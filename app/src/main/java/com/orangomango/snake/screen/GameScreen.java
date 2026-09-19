package com.orangomango.snake.screen;

import java.util.*;
import org.json.JSONException;
import org.json.JSONObject;
import android.app.Activity;
import android.view.KeyEvent;

import com.google.android.gms.games.LeaderboardsClient;
import com.google.android.gms.games.PlayGames;
import com.orangomango.androidbridge.ICanvas;
import com.orangomango.androidbridge.PointerEvent;
import com.orangomango.androidbridge.geometry.Rectangle2D;
import com.orangomango.androidbridge.geometry.Side;
import com.orangomango.androidbridge.geometry.TextAlignment;

import com.orangomango.snake.GameView;
import com.orangomango.snake.game.Game;
import com.orangomango.snake.game.SnakeBody;
import com.orangomango.snake.Player;
import com.orangomango.snake.R;
import com.orangomango.snake.game.lan.Client;
import com.orangomango.snake.ui.UiElement;
import com.orangomango.account.Account;

import static com.orangomango.snake.screen.HomeScreen.COLOR_EASY;
import static com.orangomango.snake.screen.HomeScreen.COLOR_MEDIUM;
import static com.orangomango.snake.screen.HomeScreen.COLOR_HARD;
import static com.orangomango.snake.screen.HomeScreen.COLOR_EXTREME;
import static com.orangomango.snake.GameView.AUDIO;

public class GameScreen extends Screen{
	private static int OFFSET_X, SCREEN_W, SCREEN_H;
	private static final int SWIPE_THRESHOLD = 40;
	private static final int AD_COOLDOWN = 45000;
	private static final int GAME_OVER_TIME = 2000;

	// Google Play Games
	public static final String LEADERBOARD_EASY = "CgkI0ZmR74UBEAIQCw";
	public static final String LEADERBOARD_MEDIUM = "CgkI0ZmR74UBEAIQDA";
	public static final String LEADERBOARD_HARD = "CgkI0ZmR74UBEAIQDQ";
	public static final String LEADERBOARD_EXTREME = "CgkI0ZmR74UBEAIQDg";
	public static final String LEADERBOARD_CURRENCY = "CgkI0ZmR74UBEAIQIw";

	private int highscore;
	private boolean showInfo = false;
	private volatile boolean showHighscore = false;

	private Account account;
	private long lastFingerRelease;
	private int fingerTaps = 0;
	private float startX, startY;
	private Side inputDir = null;
	private int controlMethod;
	private boolean dragging;
	private final Rectangle2D gamepad = new Rectangle2D(0.87, 0.74, 0.15, 0); // centerX and centerY, HEIGHT will be adjusted later
	private double gamepadAngle, padFactor = 1;
	private Game game;

	private static long LAST_AD_TIME = System.currentTimeMillis();
	private Player player;
	private boolean leftHanded, randomSkin;

	private Client client;
	private String myPlayer = "player_1"; // TODO: init with user's ip address

	// Controller
	private final Rectangle2D controllerRect = new Rectangle2D(0.75, 0.10, 0.23,0.15);
	private final double speedHCx = 0.165, speedLCx = 0.390, pauseCx = 0.610, autoplayCx = 0.825, radius = 0.022;
	private final double homeBtnX = 0.785, homeBtnY = 0.325;
	private Polygon upControlShape, rightControlShape, downControlShape, leftControlShape;
	private volatile long gameOverStartTime; // TODO

	private class Polygon{
		public double[] xPoints;
		public double[] yPoints;
		public int nPoints;

		public Polygon(double[] xPoints, double[] yPoints, int n){
			this.xPoints = xPoints;
			this.yPoints = yPoints;
			this.nPoints = n;
		}

		public boolean contains(double x, double y){ // TODO: look into this
			boolean inside = false;
			for (int i = 0, j = this.nPoints - 1; i < this.nPoints; j = i++){
				if (((this.yPoints[i] > y) != (this.yPoints[j] > y)) && (x < (this.xPoints[j] - this.xPoints[i]) * (y - this.yPoints[i]) / (this.yPoints[j] - this.yPoints[i]) + this.xPoints[i])){
					inside = !inside;
				}
			}
			return inside;
		}

		public double getAvgX(){
			double avg = 0;
			for (int i = 0; i < this.nPoints; i++){
				avg += this.xPoints[i];
			}
			return avg/this.nPoints;
		}

		public double getAvgY(){
			double avg = 0;
			for (int i = 0; i < this.nPoints; i++){
				avg += this.yPoints[i];
			}
			return avg/this.nPoints;
		}
	}

	public GameScreen(GameView gameView, Account account, Player player, String gameMode, int timeInterval, boolean ai, boolean wrap, int controlMethod, boolean leftHanded, boolean randomSkin){
		super(gameView);

		this.game = new Game(gameMode, timeInterval, ai, wrap);
		this.game.addPlayer(this.myPlayer);

		// Game handling events
		this.game.setOnScore(score -> {
			new Thread(() -> {
				this.gameView.triggerVibration(new long[]{0, 50, 40, 50});
				AUDIO.playSound("point");
				if (this.game.getGameMode() != null) this.player.triggerMidGamePlayAchievement(this.game.getGameMode(), score, this.player.getAppData().optInt("mangoes", 0) + score);
			}).start();
		});

		this.game.setOnGameReset(() -> {
			this.showHighscore = false;
			this.inputDir = null;

			if (this.randomSkin){
				CustomizeScreen.selectRandomPlayer(this.gameView.getContext(), this.player);
			}

			AUDIO.playSound("gamestart");
		});

		this.game.setOnGameOver(score -> {
			this.gameView.triggerVibration(new long[]{0, 150, 100, 500});
			AUDIO.playSound("gameover");

			if (this.account != null && this.game.getGameMode() != null && score > this.highscore){
				this.account.updateLeaderboard(this.game.getGameMode(), new long[]{score, -System.currentTimeMillis()});
			}

			if (System.currentTimeMillis()-LAST_AD_TIME >= AD_COOLDOWN && !this.player.isAdBlockEnabled()){
				LAST_AD_TIME = System.currentTimeMillis();
				resetGame(score, false);
				try {
					Thread.sleep(500);
				} catch (InterruptedException ex){
					ex.printStackTrace();
				}
				this.gameView.showIntersitial(() -> {});
			} else {
				resetGame(score, false);
			}
		});

		this.game.setOnGameWon(score -> {
			resetGame(score, true);
		});

		this.controlMethod = controlMethod;
		this.leftHanded = leftHanded;
		this.randomSkin = randomSkin;

		this.account = account;
		this.player = player;

		if (this.randomSkin){
			CustomizeScreen.selectRandomPlayer(this.gameView.getContext(), this.player);
		}

		if (gameMode != null){
			new Thread(() -> {
				JSONObject data = this.account == null ? null : this.account.getAppData();
				if (data != null){
					this.highscore = data.optInt("high_" + gameMode, 0);
				} else {
					this.highscore = this.player.getAppData().optInt("high_" + gameMode, 0);
				}
			}).start();
		}

		if (gameMode == null){
			AUDIO.playBackgroundMusic(this.gameView.getContext(), R.raw.background_game_3);
		} else if (gameMode.equals("easy") || gameMode.equals("medium")){
			AUDIO.playBackgroundMusic(this.gameView.getContext(), R.raw.background_game);
		} else if (gameMode.equals("hard") || gameMode.equals("extreme")){
			AUDIO.playBackgroundMusic(this.gameView.getContext(), R.raw.background_game_2);
		}
	}

	public void initGame(int size){
		AUDIO.playSound("gameStart");
		this.game.initGame(size);
	}

	public void connect(Client client){
		this.client = client;
	}

	// Listen for data incoming from the server
	public void handleConnectionData(String data){
		// TODO: get game events here (like snake movement, gameover screen, etc)
		/*try {
			JSONObject json = new JSONObject(data);
			String eventType = json.getString("event");
			Log.d("LANDebug", "event json: " + json);
			if (eventType.equals("playerCount")){
				int count = json.getInt("count"); // Note: every player other than "player_1" will be controlled by the server
				String ip = json.getString("host");
				Log.d("LANDebug", "PlayerCount: " + count + " host: " + ip);
				if (count == 2){
					this.snake.put(ip, new ArrayList<SnakeBody>());
					this.direction.put(ip, Side.RIGHT);
					resetSnakeBodies();
				} else if (count > 2){
					// TODO: not supported for > 2 players (add rooms etc..)
				}
			} else if (eventType.equals("movement")){
				Log.d("LANDebug", String.format("Here btw (%s and my is %s)", json.getString("host"), this.client.getMyIp()));
				if (!json.getString("host").equals(this.client.getMyIp())){
					String direction = json.getString("direction");
					if (direction.equals("n")){
						this.direction.put(json.getString("host"), Side.TOP);
					} else if (direction.equals("e")){
						this.direction.put(json.getString("host"), Side.RIGHT);
					} else if (direction.equals("s")){
						this.direction.put(json.getString("host"), Side.BOTTOM);
					} else if (direction.equals("w")){
						this.direction.put(json.getString("host"), Side.LEFT);
					}
					Log.d("LANDebug", this.direction.toString());
				}
			} else {
				Log.d("LANDebug", json.toString());
			}
		} catch (JSONException ex){
			ex.printStackTrace();
		}*/
	}

	private void resetGame(int score, boolean gameWon){
		this.gameOverStartTime = System.currentTimeMillis();

		if (score > this.highscore){
			this.highscore = score;
			this.showHighscore = true;
			AUDIO.playSound("highscore");

			if (this.game.getGameMode() != null){
				LeaderboardsClient leaderboardsClient = PlayGames.getLeaderboardsClient((Activity) this.gameView.getContext());
				if (this.game.getGameMode().equals("easy")){
					leaderboardsClient.submitScore(LEADERBOARD_EASY, score);
				} else if (this.game.getGameMode().equals("medium")){
					leaderboardsClient.submitScore(LEADERBOARD_MEDIUM, score);
				} else if (this.game.getGameMode().equals("hard")){
					leaderboardsClient.submitScore(LEADERBOARD_HARD, score);
				} else if (this.game.getGameMode().equals("extreme")){
					leaderboardsClient.submitScore(LEADERBOARD_EXTREME, score);
				}
			}
		}

		// Save data
		final long startTime = this.game.getStartTime();
		new Thread(() -> {
			if (this.game.getGameMode() != null){
				try{
					// First save offline
					final int totalMangoes = this.player.getAppData().optInt("mangoes", 0) + score;
					final int totalRounds = this.player.getAppData().optInt("rounds", 0) + 1;

					long elapsedTime = System.currentTimeMillis() - startTime;

					if (score > 0){
						this.player.syncPlayAchievements(this.account, totalMangoes, totalRounds, this.game.getGameMode(), score, gameWon);
						this.player.getAppData().put("rounds", totalRounds);

						// Reset AVG after 30 rounds
						if (this.player.getAppData().optInt("totalRounds_"+this.game.getGameMode(), 0) >= 30){
							this.player.getAppData().put("totalRounds_"+this.game.getGameMode(), 0);
							this.player.getAppData().put("totalScore_"+this.game.getGameMode(), 0);
						}

						this.player.getAppData().put("totalScore_"+this.game.getGameMode(), this.player.getAppData().optInt("totalScore_"+this.game.getGameMode(), 0) + score);
						this.player.getAppData().put("totalRounds_"+this.game.getGameMode(), this.player.getAppData().optInt("totalRounds_"+this.game.getGameMode(), 0) + 1);
						this.player.getAppData().put("timePlayed", this.player.getAppData().optInt("timePlayed", 0) + (elapsedTime / 1000));
						this.player.getAppData().put("currency", this.player.getAppData().optInt("currency", 0) + score*getMultiplier(this.game.getGameMode()));
						this.player.getAppData().put("totalCurrency", this.player.getAppData().optInt("totalCurrency", this.player.getAppData().optInt("currency", 0)) + score*getMultiplier(this.game.getGameMode()));

						// Save longest game time
						if (elapsedTime/1000 > this.player.getAppData().optInt("longestGameTime_"+this.game.getGameMode(), 0)){
							this.player.getAppData().put("longestGameTime_"+this.game.getGameMode(), elapsedTime / 1000);
						}
					}

					this.player.getAppData().put("high_" + this.game.getGameMode(), this.highscore);
					this.player.getAppData().put("mangoes", totalMangoes);
					this.player.getAppData().put("lastSave", System.currentTimeMillis());
					if (this.account != null) this.player.getAppData().put("username", this.account.getUsername());

					this.player.syncAndSave(this.account);
				} catch (JSONException ex){
					ex.printStackTrace();
				}
			}
		}).start();
	}



	private static int getMultiplier(String gameMode){
		if (gameMode.equals("easy")){
			return 1;
		} else if (gameMode.equals("medium")){
			return 1;
		} else if (gameMode.equals("hard")){
			return 2;
		} else if (gameMode.equals("extreme")){
			return 3;
		} else {
			return 0;
		}
	}


	@Override
	public void handleKeyDown(int keyCode){
		switch (keyCode){
			case KeyEvent.KEYCODE_W:
			case KeyEvent.KEYCODE_DPAD_UP:
				this.inputDir = Side.TOP;
				break;

			case KeyEvent.KEYCODE_S:
			case KeyEvent.KEYCODE_DPAD_DOWN:
				this.inputDir = Side.BOTTOM;
				break;


			case KeyEvent.KEYCODE_A:
			case KeyEvent.KEYCODE_DPAD_LEFT:
				this.inputDir = Side.LEFT;
				break;

			case KeyEvent.KEYCODE_D:
			case KeyEvent.KEYCODE_DPAD_RIGHT:
				this.inputDir = Side.RIGHT;
				break;


			// TODO: debug keys
			/*case KeyEvent.KEYCODE_T:
				this.direction.put("player_2", Side.TOP);
				break;
			case KeyEvent.KEYCODE_F:
				this.direction.put("player_2", Side.LEFT);
				break;
			case KeyEvent.KEYCODE_G:
				this.direction.put("player_2", Side.BOTTOM);
				break;
			case KeyEvent.KEYCODE_H:
				this.direction.put("player_2", Side.RIGHT);
				break;*/
		}
	}

	@Override
	public void handleInput(PointerEvent event){
		float tx = event.x;
		float ty = event.y;

		Rectangle2D homeButton = new Rectangle2D(rsw_reverse(this.homeBtnX)-rsw(this.radius), rsh(this.homeBtnY)-rsw(this.radius), rsw(this.radius)*2, rsw(this.radius)*2);
		if (event.type == PointerEvent.Type.PRESSED && homeButton.contains(tx, ty) && (this.game.isPaused() || this.game.isFinished())){
			AUDIO.playSound("gui");
			this.gameView.triggerVibration(65);
			if (System.currentTimeMillis()-LAST_AD_TIME >= AD_COOLDOWN && !this.player.isAdBlockEnabled()){
				LAST_AD_TIME = System.currentTimeMillis();
				this.gameView.showIntersitial(() -> {
					goBack(null);
				});
			} else {
				goBack(null);
			}
		}

		if (this.game.isFinished()){
			if (System.currentTimeMillis()-this.gameOverStartTime > GAME_OVER_TIME){
				this.gameView.triggerVibration(100);
				this.game.restoreGameState();
			}
			return;
		}

		switch (event.type) {
			case PRESSED:
				this.startX = tx;
				this.startY = ty;

				// Check controller buttons
				int controllerButton = controllerButtonPressed(tx, ty);
				if (this.game.getGameMode() == null){
					if (controllerButton == 0){
						this.game.setTimeInterval(Math.max(this.game.getTimeInterval() - 5, 5));
						this.gameView.triggerVibration(65);
					} else if (controllerButton == 1){
						this.game.setTimeInterval(this.game.getTimeInterval() + 5);
						this.gameView.triggerVibration(65);
					} else if (controllerButton == 3){
						this.game.toggleAi();
						this.gameView.triggerVibration(65);
					} else {
						if (System.currentTimeMillis() - this.lastFingerRelease < 100){ // The player tapped three times
							if ((!this.leftHanded && event.x < SCREEN_W * 0.3) || (this.leftHanded && event.x > SCREEN_W * 0.7)){ // Left side of the screen
								this.fingerTaps++;
								if (this.fingerTaps == 2){
									this.showInfo = !this.showInfo;
									this.gameView.triggerVibration(65);
									this.fingerTaps = 0;
								}
							}
						} else {
							this.fingerTaps = 0;
						}
					}
				}

				if (controllerButton == 2){
					togglePause();
					this.gameView.triggerVibration(65);
				}

				if (!this.game.isAiRunning()){
					if (this.controlMethod == 0 && !this.game.isPaused()){ // D-pad
						if (this.upControlShape.contains(tx, ty) && this.game.getDirection().get(this.myPlayer) != Side.TOP){
							this.inputDir = Side.TOP;
							this.gameView.triggerVibration(35);
						} else if (this.rightControlShape.contains(tx, ty) && this.game.getDirection().get(this.myPlayer) != Side.RIGHT){
							this.inputDir = Side.RIGHT;
							this.gameView.triggerVibration(35);
						} else if (this.downControlShape.contains(tx, ty) && this.game.getDirection().get(this.myPlayer) != Side.BOTTOM){
							this.inputDir = Side.BOTTOM;
							this.gameView.triggerVibration(35);
						} else if (this.leftControlShape.contains(tx, ty) && this.game.getDirection().get(this.myPlayer) != Side.LEFT){
							this.inputDir = Side.LEFT;
							this.gameView.triggerVibration(35);
						}
					}
				}

				break;

			case RELEASED:
				this.dragging = false;
				this.lastFingerRelease = System.currentTimeMillis();

				if (!this.game.isAiRunning()){
					if (this.controlMethod == 2 && !this.game.isPaused()){ // Swipe to move
						float deltaX = tx - this.startX;
						float deltaY = ty - this.startY;

						if (Math.abs(deltaX) > SWIPE_THRESHOLD || Math.abs(deltaY) > SWIPE_THRESHOLD){
							if (Math.abs(deltaX) > Math.abs(deltaY)){
								if (deltaX > 0) this.inputDir = Side.RIGHT;
								else this.inputDir = Side.LEFT;
							} else {
								if (deltaY > 0) this.inputDir = Side.BOTTOM;
								else this.inputDir = Side.TOP;
							}
						}
					}
				}

				break;

			case DRAGGED:
				if (!this.game.isAiRunning()){
					if (this.controlMethod == 1 && !this.game.isPaused()){ // Joystick
						Rectangle2D rect = (new Rectangle2D(rsw_reverse(this.gamepad.getMinX()), rsh(this.gamepad.getMinY()), rsw(this.gamepad.getWidth()), rsh(this.gamepad.getHeight()))).translate(rsw(-this.gamepad.getWidth()/2), rsh(-this.gamepad.getHeight()/2));
						if (rect.contains(tx, ty) || this.dragging){
							double relX = (tx - rect.getMinX()) / rect.getWidth();
							double relY = (ty - rect.getMinY()) / rect.getHeight();

							this.gamepadAngle = Math.atan2(relY - 0.5, relX - 0.5);
							this.padFactor = Math.sqrt(Math.pow((relX - 0.5) / 0.5, 2) + Math.pow((relY - 0.5) / 0.5, 2));
							this.dragging = true;

							if (relX > relY && relX > 1 - relY && this.game.getDirection().get(this.myPlayer) != Side.RIGHT){
								this.inputDir = Side.RIGHT;
								this.gameView.triggerVibration(35);
							} else if (relX < relY && relX < 1 - relY && this.game.getDirection().get(this.myPlayer) != Side.LEFT){
								this.inputDir = Side.LEFT;
								this.gameView.triggerVibration(35);
							} else if (relX > relY && relX < 1 - relY && this.game.getDirection().get(this.myPlayer) != Side.TOP){
								this.inputDir = Side.TOP;
								this.gameView.triggerVibration(35);
							} else if (relX < relY && relX > 1 - relY && this.game.getDirection().get(this.myPlayer) != Side.BOTTOM){
								this.inputDir = Side.BOTTOM;
								this.gameView.triggerVibration(35);
							}
						}
					}
				}

				break;
		}
	}

	private void sendPlayerMovement(String direction){
		/*if (this.client != null){
			new Thread(() -> {
				String data = String.format("{'event' : 'movement', 'direction' : '%s', 'host' : '%s'}", direction, this.client.getMyIp());
				this.client.send(data);
			}).start();
		}*/
	}

	public void pauseGame(){
		this.game.setPaused(false);
		togglePause();
	}

	public void togglePause(){
		this.game.togglePause();
	}

	@Override
	public void goBack(Activity activity){
		this.game.setThreadRunning(false);
		AUDIO.playBackgroundMusic(this.gameView.getContext(), R.raw.background);
		HomeScreen hs = new HomeScreen(this.gameView);
		this.gameView.setScreen(hs);
	}

	@Override
	public void update(int screenWidth, int screenHeight){
		SCREEN_W = screenWidth;
		SCREEN_H = screenHeight;
		OFFSET_X = (int)(screenWidth*0.03);

		// Fix gamepad size
		this.gamepad.setHeight(rsw(this.gamepad.getWidth())/SCREEN_H);

		// D-pad
		if (this.upControlShape == null){
			final double centerX = rsw_reverse(0.86);
			final double centerY = rsh(0.69);

			final double rx = rsw(0.068);
			final double ry = rx;

			this.upControlShape = new Polygon(new double[]{centerX, centerX+rx, centerX, centerX-rx}, new double[]{centerY-2*ry, centerY-ry, centerY, centerY-ry}, 4);
			this.rightControlShape = new Polygon(new double[]{centerX+rx, centerX+2*rx, centerX+rx, centerX}, new double[]{centerY-ry, centerY, centerY+ry, centerY}, 4);
			this.downControlShape = new Polygon(new double[]{centerX, centerX+rx, centerX, centerX-rx}, new double[]{centerY, centerY+ry, centerY+2*ry, centerY+ry}, 4);
			this.leftControlShape = new Polygon(new double[]{centerX-rx, centerX, centerX-rx, centerX-2*rx}, new double[]{centerY-ry, centerY, centerY+ry, centerY}, 4);
		}
	}

	@Override
	public void render(ICanvas canvas){
		canvas.clear(0xFF020617);
		canvas.translate(this.leftHanded ? SCREEN_W-OFFSET_X-this.game.getGameWorld().getWidth()*SnakeBody.SIZE : OFFSET_X, 0);

		for (int i = 0; i <= this.game.getGameWorld().getWidth(); i++){
			canvas.strokeLine(i * SnakeBody.SIZE, 0, i * SnakeBody.SIZE, this.game.getGameWorld().getHeight()*SnakeBody.SIZE, 0xFF1E293B, 1.2);
		}
		for (int i = 0; i <= this.game.getGameWorld().getHeight(); i++){
			canvas.strokeLine(0, i*SnakeBody.SIZE, this.game.getGameWorld().getWidth()*SnakeBody.SIZE, i*SnakeBody.SIZE, 0xFF1E293B, 1.2);
		}

		if (this.game.isAllowMovement()){ // Control the player's snake
			if (this.inputDir == Side.TOP && this.game.getDirection().get(this.myPlayer) != Side.BOTTOM){
				this.game.getDirection().put(this.myPlayer, Side.TOP);
				this.game.setAllowMovement(false);
				this.inputDir = null;
				sendPlayerMovement("n");
			} else if (this.inputDir == Side.BOTTOM && this.game.getDirection().get(this.myPlayer) != Side.TOP){
				this.game.getDirection().put(this.myPlayer, Side.BOTTOM);
				this.game.setAllowMovement(false);
				this.inputDir = null;
				sendPlayerMovement("s");
			} else if (this.inputDir == Side.RIGHT && this.game.getDirection().get(this.myPlayer) != Side.LEFT){
				this.game.getDirection().put(this.myPlayer, Side.RIGHT);
				this.game.setAllowMovement(false);
				this.inputDir = null;
				sendPlayerMovement("e");
			} else if (this.inputDir == Side.LEFT && this.game.getDirection().get(this.myPlayer) != Side.RIGHT){
				this.game.getDirection().put(this.myPlayer, Side.LEFT);
				this.game.setAllowMovement(false);
				this.inputDir = null;
				sendPlayerMovement("w");
			}
		}

		if (this.game.getApple() != null) this.game.getApple().render(canvas, this.player.getAppleColor(this.player.getAppleIndex()), this.player.getAppleInternalColor(this.player.getAppleIndex()));

		final int snakeIndex = this.player.getSnakeIndex();
		final int snakeColor = this.player.getSnakeColor(snakeIndex);
		synchronized (this){
			for (List<SnakeBody> snakeBody : this.game.getSnake().values()){
				for (int count = 0; count < 2; count++){ // Draw a second time to avoid internal dropshadow effects
					final int snakeSize = snakeBody.size();
					if (snakeSize == 0) continue; // The list has not been filled yet
					for (int i = snakeSize - 2; i >= 0; i--){
						SnakeBody sb = snakeBody.get(i);
						sb.render(canvas, count == 0, i, snakeBody, snakeIndex, snakeColor);
					}
					snakeBody.get(snakeSize-1).render(canvas, count == 0, snakeSize-1, snakeBody, snakeIndex, snakeColor);
				}
			}
		}

		if (this.showInfo){
			this.game.getGameWorld().getCycle().render(canvas, SnakeBody.SIZE);
			canvas.fillText(String.format("FPS: %.2f Direction: %s, TimeInterval: %s, Paused: %s, Steps: %s", this.gameView.getFPS(), this.game.getDirection(), this.game.getTimeInterval(), this.game.isPaused(), this.game.getSteps()), 40, 85, 0xFF22D3EE, UiElement.FONT_SMALL, TextAlignment.LEFT);
		}

		// Draw grid outline border
		canvas.setEffect(UiElement.rh(0.0010), 0xFFA855F7);
		final double dynamicWidth = 0.0015 + (Math.sin((System.currentTimeMillis() % 1500 / 1500.0) * 2.0*Math.PI) * 0.0010);
		int difficultyColor = 0xFF4CC9F0; // Default value
		if (this.game.getGameMode() != null){
			switch (this.game.getGameMode()){
				case "easy":
					difficultyColor = COLOR_EASY;
					break;
				case "medium":
					difficultyColor = COLOR_MEDIUM;
					break;
				case "hard":
					difficultyColor = COLOR_HARD;
					break;
				case "extreme":
					difficultyColor = COLOR_EXTREME;
					break;
			}
		}
		canvas.strokeRect(SnakeBody.SIZE*0.05, SnakeBody.SIZE*0.05, this.game.getGameWorld().getWidth()*SnakeBody.SIZE-SnakeBody.SIZE*0.1, this.game.getGameWorld().getHeight()*SnakeBody.SIZE-SnakeBody.SIZE*0.1, difficultyColor, UiElement.rw(dynamicWidth));
		canvas.clearEffect();

		canvas.setEffect(20, 0xFFFFFFFF);
		canvas.fillText(String.format("Score: %d, Highscore: %d"+(this.game.isAiRunning() ? " | AI" : ""), this.game.getScore(this.myPlayer), this.highscore), this.leftHanded ? this.game.getGameWorld().getWidth()*SnakeBody.SIZE-40 : 40, 60, 0xFFFFFFFF, UiElement.FONT_MEDIUM, this.leftHanded ? TextAlignment.RIGHT : TextAlignment.LEFT);

		if (this.game.getGameMode() != null){
			int textColor = 0;
			switch (this.game.getGameMode()){
				case "easy":
					textColor = COLOR_EASY;
					canvas.setEffect(25, 0xFF34D399);
					break;
				case "medium":
					textColor = COLOR_MEDIUM;
					canvas.setEffect(25, 0xFF60A5FA);
					break;
				case "hard":
					textColor = COLOR_HARD;
					canvas.setEffect(25, 0xFFF87171);
					break;
				case "extreme":
					textColor = COLOR_EXTREME;
					canvas.setEffect(25, 0xFFC084FC);
					break;
			}

			canvas.fillText(this.game.getGameMode(), this.leftHanded ? rsw(1-0.865)-(SCREEN_W-OFFSET_X-this.game.getGameWorld().getWidth()*SnakeBody.SIZE) : rsw(0.865)-OFFSET_X, rsh(0.07), textColor, UiElement.FONT_LARGE, TextAlignment.CENTER);
		}

		canvas.clearEffect();
		canvas.translate(this.leftHanded ? -(SCREEN_W-OFFSET_X-this.game.getGameWorld().getWidth()*SnakeBody.SIZE) : -OFFSET_X, 0);

		if (this.controlMethod == 0){
			canvas.setEffect(40, 0x884CC9F0);
			if (this.game.getDirection().get(this.myPlayer) == Side.TOP) canvas.fillPolygon(this.upControlShape.xPoints, this.upControlShape.yPoints, this.upControlShape.nPoints, 0x884CC9F0);
			if (this.game.getDirection().get(this.myPlayer) == Side.RIGHT) canvas.fillPolygon(this.rightControlShape.xPoints, this.rightControlShape.yPoints, this.rightControlShape.nPoints, 0x884CC9F0);
			if (this.game.getDirection().get(this.myPlayer) == Side.BOTTOM) canvas.fillPolygon(this.downControlShape.xPoints, this.downControlShape.yPoints, this.downControlShape.nPoints, 0x884CC9F0);
			if (this.game.getDirection().get(this.myPlayer) == Side.LEFT) canvas.fillPolygon(this.leftControlShape.xPoints, this.leftControlShape.yPoints, this.leftControlShape.nPoints, 0x884CC9F0);
			canvas.clearEffect();

			canvas.strokePolygon(this.upControlShape.xPoints, this.upControlShape.yPoints, this.upControlShape.nPoints, 0xFF083344, rsh(0.01));
			canvas.strokePolygon(this.rightControlShape.xPoints, this.rightControlShape.yPoints, this.rightControlShape.nPoints, 0xFF083344, rsh(0.01));
			canvas.strokePolygon(this.downControlShape.xPoints, this.downControlShape.yPoints, this.downControlShape.nPoints, 0xFF083344, rsh(0.01));
			canvas.strokePolygon(this.leftControlShape.xPoints, this.leftControlShape.yPoints, this.leftControlShape.nPoints, 0xFF083344, rsh(0.01));

			canvas.fillText("↑", this.upControlShape.getAvgX(), this.upControlShape.getAvgY()+rsh(0.015), 0xFFFFFFFF, UiElement.FONT_EXTRALARGE, TextAlignment.CENTER);
			canvas.fillText("→", this.rightControlShape.getAvgX()+rsw(0.005), this.rightControlShape.getAvgY()+rsh(0.015), 0xFFFFFFFF, UiElement.FONT_EXTRALARGE, TextAlignment.CENTER);
			canvas.fillText("↓", this.downControlShape.getAvgX(), this.downControlShape.getAvgY()+rsh(0.017), 0xFFFFFFFF, UiElement.FONT_EXTRALARGE, TextAlignment.CENTER);
			canvas.fillText("←", this.leftControlShape.getAvgX()-rsw(0.005), this.leftControlShape.getAvgY()+rsh(0.015), 0xFFFFFFFF, UiElement.FONT_EXTRALARGE, TextAlignment.CENTER);
		} else if (this.controlMethod == 1){
			canvas.strokeOval(rsw_reverse(gamepad.getMinX())-rsw(gamepad.getWidth()/2), rsh(gamepad.getMinY()-gamepad.getHeight()/2), rsw(gamepad.getWidth()), rsh(gamepad.getHeight()), 0x332DD4BF, rsh(0.01));

			double snakeDirectionAngle = 0;
			switch (this.game.getDirection().get(this.myPlayer)){
				case TOP:
					snakeDirectionAngle = 270;
					break;
				case RIGHT:
					snakeDirectionAngle = 0;
					break;
				case BOTTOM:
					snakeDirectionAngle = 90;
					break;
				case LEFT:
					snakeDirectionAngle = 180;
					break;
			}

			// Draw arrow
			canvas.save();
			canvas.translate(rsw_reverse(gamepad.getMinX()), rsh(gamepad.getMinY()));
			canvas.rotate(snakeDirectionAngle);
			canvas.strokeLine(rsw(-0.030), 0, rsw(0.030), 0, 0x992DD4BF, rsh(0.012));
			canvas.strokeLine(rsw(-0.010), rsw(-0.018), rsw(0.030), 0, 0x992DD4BF, rsh(0.012));
			canvas.strokeLine(rsw(-0.010), rsw(0.018), rsw(0.030), 0, 0x992DD4BF, rsh(0.012));
			canvas.restore();

			// Draw pointer
			canvas.save();
			canvas.translate(rsw_reverse(gamepad.getMinX())+rsw(gamepad.getWidth()/2*Math.cos(this.gamepadAngle)*Math.min(this.padFactor, 1)), rsh(gamepad.getMinY()+gamepad.getHeight()/2*Math.sin(this.gamepadAngle)*Math.min(this.padFactor, 1)));
			canvas.rotate(Math.toDegrees(this.gamepadAngle));
			canvas.translate(rsw(0.002), 0);
			double[] xPoints = new double[]{rsw(0.020), rsw(-0.015), rsw(-0.008), rsw(-0.015)};
			double[] yPoints = new double[]{0, rsw(-0.016), 0, rsw(0.016)};
			canvas.setEffect(15, 0xFFF43F5E);
			canvas.fillPolygon(xPoints, yPoints, 4, 0xFFF43F5E);
			canvas.clearEffect();
			canvas.restore();
		}

		if (this.game.isPaused() || this.game.isFinished()){
			canvas.fillRect(0, 0, SCREEN_W, SCREEN_H, 0xCC000000);
			canvas.fillText(this.game.isFinished() ? (this.game.getApple() == null ? "YOU WIN!" : "GAMEOVER") : "GAME PAUSED", SCREEN_W/2.0, this.game.isFinished() ? SCREEN_H*0.25 : SCREEN_H*0.4, 0xFFFFFFFF, UiElement.FONT_EXTRALARGE, TextAlignment.CENTER);
			if (this.game.isFinished()){
				if (this.game.getScore(this.myPlayer) > 0) canvas.fillText(this.game.getGameMode() != null ? String.format("You collected %d MangoCoins!", this.game.getScore(this.myPlayer) * getMultiplier(this.game.getGameMode())) : "There are no rewards for casual games!", SCREEN_W/2.0, SCREEN_H*0.35, 0xFFFFD600, UiElement.FONT_LARGE, TextAlignment.CENTER);
				canvas.setEffect(30, 0xFF4CC9F0);
				canvas.fillText(Integer.toString(this.game.getScore(this.myPlayer)), SCREEN_W/2.0, SCREEN_H*0.5, 0xFF4CC9F0, UiElement.FONT_EXTRAEXTRALARGE, TextAlignment.CENTER);
				canvas.clearEffect();

				long diff = System.currentTimeMillis()-this.gameOverStartTime;
				double progress = Math.min(1, (double) diff / GAME_OVER_TIME);

				canvas.fillText("Tap to continue", SCREEN_W/2.0, SCREEN_H*0.9, progress < 1 ? 0xFF667878 : 0xFFFFFFFF, UiElement.FONT_MEDIUM, TextAlignment.CENTER);
				if (progress < 1){
					canvas.fillRect(rsw(0.4), rsh(0.935), rsw(0.2), rsh(0.035), 0xFF1A2222);
					canvas.setEffect(rsh(0.0335), 0xFF00F2FF);
					canvas.fillRect(rsw(0.4), rsh(0.935), rsw(0.2) * (1-progress), rsh(0.035), 0xFF00F2FF);
					canvas.clearEffect();
				}

				if (this.showHighscore && this.game.getGameMode() != null){
					canvas.setEffect(15, 0xFFFF0000);
					canvas.fillText("NEW HIGHSCORE!", SCREEN_W/2.0, SCREEN_H*0.57, 0xFFFF0000, UiElement.FONT_MEDIUM, TextAlignment.CENTER);
					canvas.clearEffect();
				}
			}
		}

		// Draw controller buttons
		drawController(canvas);
	}

	private void drawController(ICanvas canvas){ // Speed/Pause/Auto-Play
		final double width = rsw(this.controllerRect.getWidth());
		final double height = rsh(this.controllerRect.getHeight());
		final double minX = rsw_reverse(this.controllerRect.getMinX()) + (this.leftHanded ? -width : 0);
		final double minY = rsh(this.controllerRect.getMinY());

		// Pause button
		final double cx = minX+this.pauseCx*width;
		final double cy = minY+0.5*height;

		canvas.fillRoundRect(minX, minY, width, height, UiElement.rh(0.035), UiElement.rh(0.035), 0xB20F172A);
		canvas.strokeRoundRect(minX, minY, width, height, UiElement.rh(0.035), UiElement.rh(0.035), 0x33FFFFFF, UiElement.rh(0.0035));

		canvas.fillOval(minX+this.speedHCx*width-rsw(this.radius), minY+0.5*height-rsw(this.radius), rsw(this.radius)*2, rsw(this.radius)*2, 0x08FFFFFF);
		canvas.fillOval(minX+this.speedLCx*width-rsw(this.radius), minY+0.5*height-rsw(this.radius), rsw(this.radius)*2, rsw(this.radius)*2, 0x08FFFFFF);
		canvas.fillOval(cx-rsw(this.radius), cy-rsw(this.radius), rsw(this.radius)*2, rsw(this.radius)*2, this.game.isPaused() ? 0xFFFDA4AF : 0x08FFFFFF);
		canvas.fillOval(minX+this.autoplayCx*width-rsw(this.radius), minY+0.5*height-rsw(this.radius), rsw(this.radius)*2, rsw(this.radius)*2, this.game.isAiRunning() ? 0xFF2DD4BF : 0x08FFFFFF);

		canvas.setEffect(10, 0xFF2DD4BF);
		canvas.strokeOval(minX+this.speedHCx*width-rsw(this.radius), minY+0.5*height-rsw(this.radius), rsw(this.radius)*2, rsw(this.radius)*2, 0x33FFFFFF, UiElement.rh(0.0025));
		canvas.strokeOval(minX+this.speedLCx*width-rsw(this.radius), minY+0.5*height-rsw(this.radius), rsw(this.radius)*2, rsw(this.radius)*2, 0x33FFFFFF, UiElement.rh(0.0025));
		canvas.strokeOval(cx-rsw(this.radius), cy-rsw(this.radius), rsw(this.radius)*2, rsw(this.radius)*2, 0x33FFFFFF, UiElement.rh(0.0025));
		canvas.strokeOval(minX+this.autoplayCx*width-rsw(this.radius), minY+0.5*height-rsw(this.radius), rsw(this.radius)*2, rsw(this.radius)*2, 0x33FFFFFF, UiElement.rh(0.0025));
		canvas.clearEffect();

		canvas.fillText("<<", minX+this.speedHCx*width, minY+0.585*height, this.game.getGameMode() != null ? 0x6694A3B8 : 0xFFFFFFFF, UiElement.FONT_LARGELARGE, TextAlignment.CENTER);
		canvas.fillText(">>", minX+this.speedLCx*width, minY+0.585*height, this.game.getGameMode() != null ? 0x6694A3B8 : 0xFFFFFFFF, UiElement.FONT_LARGELARGE, TextAlignment.CENTER);
		canvas.fillText("A", minX+this.autoplayCx*width, minY+0.585*height, this.game.getGameMode() != null ? 0x6694A3B8 : 0xFFFFFFFF, UiElement.FONT_LARGELARGE, TextAlignment.CENTER);

		// Pause button
		if (this.game.isPaused()){
			double[] xPoints = new double[]{cx-rsw(0.006), cx-rsw(0.006), cx+rsw(0.012)};
			double[] yPoints = new double[]{cy-rsh(0.020), cy+rsh(0.020), cy};
			canvas.fillPolygon(xPoints, yPoints, 3, 0xFFF43F5E);
		} else {
			canvas.strokeLine(cx-rsw(0.005), cy-rsh(0.015), cx-rsw(0.005), cy+rsh(0.015), 0xFF22D3EE, UiElement.rh(0.005));
			canvas.strokeLine(cx+rsw(0.005), cy-rsh(0.015), cx+rsw(0.005), cy+rsh(0.015), 0xFF22D3EE, UiElement.rh(0.005));
		}

		// Home button
		if (this.game.isPaused() || this.game.isFinished()){
			canvas.fillRoundRect(rsw_reverse(this.homeBtnX)-rsw(this.radius), rsh(this.homeBtnY)-rsw(this.radius), rsw(this.radius)*2, rsw(this.radius)*2, UiElement.rh(0.035), UiElement.rh(0.035), 0x08FFFFFF);
			canvas.setEffect(10, 0xFF2DD4BF);
			canvas.strokeRoundRect(rsw_reverse(this.homeBtnX)-rsw(this.radius), rsh(this.homeBtnY)-rsw(this.radius), rsw(this.radius)*2, rsw(this.radius)*2, UiElement.rh(0.035), UiElement.rh(0.035), 0x33FFFFFF, UiElement.rh(0.0025));
			canvas.clearEffect();
		
			double[] roofX = new double[]{rsw_reverse(this.homeBtnX), rsw_reverse(this.homeBtnX)-rsw(0.013), rsw_reverse(this.homeBtnX)+rsw(0.013)};
			double[] roofY = new double[]{rsh(this.homeBtnY-0.022), rsh(this.homeBtnY-0.004), rsh(this.homeBtnY-0.004)};
			canvas.fillPolygon(roofX, roofY, 3, 0x4D22D3EE);

			double[] bodyX = new double[]{rsw_reverse(this.homeBtnX)-rsw(0.009), rsw_reverse(this.homeBtnX)-rsw(0.009), rsw_reverse(this.homeBtnX)+rsw(0.009), rsw_reverse(this.homeBtnX)+rsw(0.009)};
			double[] bodyY = new double[]{rsh(this.homeBtnY-0.002), rsh(this.homeBtnY+0.020), rsh(this.homeBtnY+0.020), rsh(this.homeBtnY-0.002)};
			canvas.fillPolygon(bodyX, bodyY, 4, 0x4D22D3EE);
		}
	}

	private int controllerButtonPressed(double ex, double ey){
		final double width = rsw(this.controllerRect.getWidth());
		final double height = rsh(this.controllerRect.getHeight());
		final double minX = rsw_reverse(this.controllerRect.getMinX()) + (this.leftHanded ? -width : 0);
		final double minY = rsh(this.controllerRect.getMinY());

		Rectangle2D speedL = new Rectangle2D(minX+this.speedLCx*width-rsw(this.radius), minY+0.5*height-rsw(this.radius), rsw(this.radius)*2, rsw(this.radius)*2);
		Rectangle2D speedH = new Rectangle2D(minX+this.speedHCx*width-rsw(this.radius), minY+0.5*height-rsw(this.radius), rsw(this.radius)*2, rsw(this.radius)*2);
		Rectangle2D pause = new Rectangle2D(minX+this.pauseCx*width-rsw(this.radius), minY+0.5*height-rsw(this.radius), rsw(this.radius)*2, rsw(this.radius)*2);
		Rectangle2D autoplay = new Rectangle2D(minX+this.autoplayCx*width-rsw(this.radius), minY+0.5*height-rsw(this.radius), rsw(this.radius)*2, rsw(this.radius)*2);

		if (speedL.contains(ex, ey)) return 0;
		else if (speedH.contains(ex, ey)) return 1;
		else if (pause.contains(ex, ey)) return 2;
		else if (autoplay.contains(ex, ey)) return 3;
		else return -1;
	}

	private static double rsw(double x){
		return x * SCREEN_W;
	}

	private static double rsh(double y){
		return y * SCREEN_H;
	}

	private double rsw_reverse(double x){
		return this.leftHanded ? rsw(1-x) : rsw(x);
	}
}
