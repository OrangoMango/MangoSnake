package com.orangomango.snake.screen;

import java.util.*;
import org.json.JSONException;
import org.json.JSONObject;
import android.app.Activity;

import com.google.android.gms.games.LeaderboardsClient;
import com.google.android.gms.games.PlayGames;
import com.orangomango.androidbridge.ICanvas;
import com.orangomango.androidbridge.PointerEvent;
import com.orangomango.androidbridge.geometry.Rectangle2D;
import com.orangomango.androidbridge.geometry.Side;
import com.orangomango.androidbridge.geometry.TextAlignment;

import com.orangomango.snake.GameView;
import com.orangomango.snake.game.Apple;
import com.orangomango.snake.game.GameWorld;
import com.orangomango.snake.game.SnakeBody;
import com.orangomango.snake.Player;
import com.orangomango.snake.R;
import com.orangomango.snake.ui.UiElement;
import com.orangomango.snake.game.ai.*;
import com.orangomango.account.Account;

import static com.orangomango.snake.GameView.HEIGHT;
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

	private List<SnakeBody> snake = new ArrayList<>();
	private volatile Apple apple;
	private Random random = new Random();
	private volatile Side direction = Side.RIGHT;
	private int score = 0, highscore;
	private GameWorld gameWorld;
	private int timeInterval;
	private boolean showInfo = false;
	private boolean ai, wrap;
	private boolean threadRunning = true;
	private volatile boolean paused = false, allowMovement = true, gameFinished = false, showHighscore = false;
	private int steps;
	private Account account;
	private String gameMode;
	private long lastFingerRelease;
	private int fingerTaps = 0;
	private float startX, startY;
	private Side inputDir = null;
	private int controlMethod;
	private boolean dragging;
	private final Rectangle2D gamepad = new Rectangle2D(0.87, 0.74, 0.15, 0); // centerX and centerY, HEIGHT will be adjusted later
	private double gamepadAngle, padFactor = 1;
	private volatile long lastFrameTime, pauseStartTime;
	private static long LAST_AD_TIME = System.currentTimeMillis();
	private Player player;
	private long gameStartTime;
	private boolean leftHanded;

	// Controller
	private final Rectangle2D controllerRect = new Rectangle2D(0.75, 0.10, 0.23,0.15);
	private final double speedHCx = 0.165, speedLCx = 0.390, pauseCx = 0.610, autoplayCx = 0.825, radius = 0.022;
	private final double homeBtnX = 0.785, homeBtnY = 0.325;
	private Polygon upControlShape, rightControlShape, downControlShape, leftControlShape;
	private volatile long gameOverStartTime;

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

	public GameScreen(GameView gameView, Account account, Player player, String gameMode, int timeInterval, boolean ai, boolean wrap, int controlMethod, boolean leftHanded){
		super(gameView);

		this.timeInterval = timeInterval;
		this.ai = ai;
		this.wrap = wrap;
		this.controlMethod = controlMethod;
		this.leftHanded = leftHanded;

		this.account = account;
		this.gameMode = gameMode;
		this.player = player;

		if (this.gameMode != null){
			new Thread(() -> {
				JSONObject data = this.account == null ? null : this.account.getAppData();
				if (data != null){
					this.highscore = data.optInt("high_" + this.gameMode, 0);
				} else {
					this.highscore = this.player.getAppData().optInt("high_" + this.gameMode, 0);
				}
			}).start();
		}

		if (this.gameMode == null){
			AUDIO.playBackgroundMusic(this.gameView.getContext(), R.raw.background_game_3);
		} else if (this.gameMode.equals("easy") || this.gameMode.equals("medium")){
			AUDIO.playBackgroundMusic(this.gameView.getContext(), R.raw.background_game);
		} else if (this.gameMode.equals("hard") || this.gameMode.equals("extreme")){
			AUDIO.playBackgroundMusic(this.gameView.getContext(), R.raw.background_game_2);
		}
	}

	public void initGame(int size){
		// WIDTH is not the same as HomeScreen! (ratio: 3:2)
		SnakeBody.SIZE = Apple.SIZE = (int) Math.floor(size * (HEIGHT / 600.0)); // Fix size due to screen_size
		this.gameWorld = new GameWorld((int) Math.floor(HEIGHT * 1.5 / SnakeBody.SIZE), HEIGHT/SnakeBody.SIZE);
		this.snake.add(new SnakeBody(7, 5));
		this.snake.add(new SnakeBody(6, 5));
		this.snake.add(new SnakeBody(5, 5));

		generateApple();
		AUDIO.playSound("gameStart");
		this.gameStartTime = System.currentTimeMillis();

		Thread gameThread = new Thread(() -> {
			while (this.threadRunning){
				try {
					if (this.paused || this.gameFinished){
						Thread.sleep(10);
						continue;
					}

					if (System.nanoTime() - this.lastFrameTime >= (long) this.timeInterval*1000000){
						SnakeBody head;
						synchronized (this){
							head = this.snake.get(0);
						}

						if (this.ai){
							final Cycle cycle = this.gameWorld.getCycle();
							final Point nextPoint = cycle.getNextPoint(head.x, head.y);
							if (this.apple == null){
								setDirection(nextPoint, head); // The game has finished so just follow the tail
							} else {
								Point bestPoint = getBestPoint(head);
								if ((bestPoint.equals(nextPoint) && this.snake.stream().filter(sb -> sb.x == bestPoint.x && sb.y == bestPoint.y).findAny().isEmpty()) || isSafe(bestPoint)){
									setDirection(bestPoint, head);
								} else {
									setDirection(nextPoint, head);
								}
							}
						}

						SnakeBody next = getNext(head);
						boolean dead = false;
						for (int i = 0; i < snake.size(); i++){
							SnakeBody body = snake.get(i);
							if (head != body && head.x == body.x && head.y == body.y){
								dead = true;
								break;
							}
						}

						if (this.wrap){
							next.wrap(this.gameWorld.getWidth(), this.gameWorld.getHeight());
						} else if (next.outside(this.gameWorld.getWidth(), this.gameWorld.getHeight())){
							dead = true;
						}

						if (dead){
							this.gameView.triggerVibration(new long[]{0, 150, 100, 500});
							AUDIO.playSound("gameover");

							if (this.account != null && this.gameMode != null && this.score > this.highscore){
								this.account.updateLeaderboard(this.gameMode, new long[]{this.score, -System.currentTimeMillis()});
							}

							this.gameFinished = true;

							if (System.currentTimeMillis()-LAST_AD_TIME >= AD_COOLDOWN && !this.player.isAdBlockEnabled()){
								LAST_AD_TIME = System.currentTimeMillis();
								resetGame(false);
								Thread.sleep(500);
								this.gameView.showIntersitial(() -> {});
							} else {
								resetGame(false);
							}

							continue;
						}

						boolean appleFlag = false;

						synchronized (this){
							if (this.apple != null && next.x == this.apple.x && next.y == this.apple.y){
								appleFlag = true;
							} else {
								this.snake.remove(this.snake.size() - 1);
							}
							next.setMove(0);
							this.snake.add(0, next);
						}

						if (appleFlag){ // Outside of the synchronized block
							this.score++;
							new Thread(() -> {
								this.gameView.triggerVibration(new long[]{0, 50, 40, 50});
								AUDIO.playSound("point");
								if (this.gameMode != null) this.player.triggerMidGamePlayAchievement(this.gameMode, this.score, this.player.getAppData().optInt("mangoes", 0) + this.score);
							}).start();
							generateApple();
						}

						this.lastFrameTime = System.nanoTime();

						if (this.apple != null) this.steps++;
						this.allowMovement = true;
					}

					double moveFactor = 0;
					if (!this.paused && !this.gameFinished){
						long lastTick = System.nanoTime() - this.lastFrameTime;
						moveFactor = Math.min(1.0, (double) lastTick / (this.timeInterval * 1000000));
					} else if (this.paused){
						long lastTick = this.pauseStartTime - this.lastFrameTime;
						moveFactor = Math.min(1.0, (double) lastTick / (this.timeInterval * 1000000));
					}

					synchronized (this){
						this.snake.get(0).setMove(Math.max(0, moveFactor));
						this.snake.get(this.snake.size()-1).setMove(Math.max(0, moveFactor));
					}

					Thread.sleep(5);
				} catch (InterruptedException ex){
					ex.printStackTrace();
				}
			}
		});
		gameThread.setDaemon(true);
		gameThread.start();
	}

	private boolean isSafe(Point point){
		List<SnakeBody> temp = new ArrayList<>(this.snake);
		if (temp.stream().noneMatch(sb -> sb.x == point.x && sb.y == point.y)){
			temp.remove(temp.size()-1);
			temp.add(0, new SnakeBody(point.x, point.y));
			for (int i = 0; i < this.snake.size()+5; i++){
				SnakeBody pseudoHead = temp.get(0);
				Point pseudoPoint = this.gameWorld.getCycle().getNextPoint(pseudoHead.x, pseudoHead.y);
				if (temp.stream().anyMatch(sb -> sb.x == pseudoPoint.x && sb.y == pseudoPoint.y)){
					return false;
				} else {
					temp.remove(temp.size()-1);
					temp.add(0, new SnakeBody(pseudoPoint.x, pseudoPoint.y));
				}
			}
			return true;
		} else {
			return false;
		}
	}

	private Point getBestPoint(SnakeBody head){
		int[][] dirs = new int[][]{{0, -1}, {1, 0}, {0, 1}, {-1, 0}};
		Point[] options = new Point[4];
		for (int i = 0; i < 4; i++){
			Point newPoint = new Point(head.x+dirs[i][0], head.y+dirs[i][1]);
			options[i] = this.gameWorld.isInsideMap(newPoint.x, newPoint.y) ? newPoint : null;
		}

		final Cycle cycle = this.gameWorld.getCycle();
		final int appleIndex = cycle.getIndex(this.apple.x, this.apple.y);
		int minDistance = Integer.MAX_VALUE;
		Point bestPoint = null;
		for (int i = 0; i < 4; i++){
			Point opt = options[i];
			if (opt != null){
				int distance = cycle.getCost(cycle.getIndex(opt.x, opt.y), appleIndex);
				if (distance < minDistance){
					minDistance = distance;
					bestPoint = opt;
				}
			}
		}

		return bestPoint;
	}

	private SnakeBody getNext(SnakeBody head){
		switch (this.direction){
			case TOP:
				return new SnakeBody(head.x, head.y-1);
			case BOTTOM:
				return new SnakeBody(head.x, head.y+1);
			case LEFT:
				return new SnakeBody(head.x-1, head.y);
			case RIGHT:
				return new SnakeBody(head.x+1, head.y);
			default:
				return null;
		}
	}

	private void resetGame(boolean gameWon){
		this.gameOverStartTime = System.currentTimeMillis();

		if (this.score > this.highscore){
			this.highscore = this.score;
			this.showHighscore = true;
			AUDIO.playSound("highscore");

			if (this.gameMode != null){
				LeaderboardsClient leaderboardsClient = PlayGames.getLeaderboardsClient((Activity) this.gameView.getContext());
				if (this.gameMode.equals("easy")){
					leaderboardsClient.submitScore(LEADERBOARD_EASY, this.score);
				} else if (this.gameMode.equals("medium")){
					leaderboardsClient.submitScore(LEADERBOARD_MEDIUM, this.score);
				} else if (this.gameMode.equals("hard")){
					leaderboardsClient.submitScore(LEADERBOARD_HARD, this.score);
				} else if (this.gameMode.equals("extreme")){
					leaderboardsClient.submitScore(LEADERBOARD_EXTREME, this.score);
				}
			}
		}

		// Save data
		final int tempScore = this.score;
		final long startTime = this.gameStartTime;
		new Thread(() -> {
			if (this.gameMode != null){
				try{
					// First save offline
					final int totalMangoes = this.player.getAppData().optInt("mangoes", 0) + tempScore;
					final int totalRounds = this.player.getAppData().optInt("rounds", 0) + 1;

					long elapsedTime = System.currentTimeMillis() - startTime;

					if (tempScore > 0){
						this.player.syncPlayAchievements(this.account, totalMangoes, totalRounds, this.gameMode, tempScore, gameWon);
						this.player.getAppData().put("rounds", totalRounds);

						// Reset AVG after 30 rounds
						if (this.player.getAppData().optInt("totalRounds_"+this.gameMode, 0) >= 30){
							this.player.getAppData().put("totalRounds_"+this.gameMode, 0);
							this.player.getAppData().put("totalScore_"+this.gameMode, 0);
						}

						this.player.getAppData().put("totalScore_"+this.gameMode, this.player.getAppData().optInt("totalScore_"+this.gameMode, 0) + tempScore);
						this.player.getAppData().put("totalRounds_"+this.gameMode, this.player.getAppData().optInt("totalRounds_"+this.gameMode, 0) + 1);
						this.player.getAppData().put("timePlayed", this.player.getAppData().optInt("timePlayed", 0) + (elapsedTime / 1000));
						this.player.getAppData().put("currency", this.player.getAppData().optInt("currency", 0) + tempScore*getMultiplier(this.gameMode));
						this.player.getAppData().put("totalCurrency", this.player.getAppData().optInt("totalCurrency", this.player.getAppData().optInt("currency", 0)) + tempScore*getMultiplier(this.gameMode));

						// Save longest game time
						if (elapsedTime/1000 > this.player.getAppData().optInt("longestGameTime_"+this.gameMode, 0)){
							this.player.getAppData().put("longestGameTime_"+this.gameMode, elapsedTime / 1000);
						}
					}

					this.player.getAppData().put("high_" + this.gameMode, this.highscore);
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

	private void restoreGameState(){
		this.gameWorld = new GameWorld((int) Math.floor(HEIGHT * 1.5 /SnakeBody.SIZE), HEIGHT/SnakeBody.SIZE); // WIDTH is not the same as HomeScreen! (ratio: 3:2)
		synchronized (this){
			this.snake.clear();
			this.snake.add(new SnakeBody(7, 5));
			this.snake.add(new SnakeBody(6, 5));
			this.snake.add(new SnakeBody(5, 5));
		}
		this.direction = Side.RIGHT;

		this.gameFinished = false;
		this.showHighscore = false;
		this.score = 0;
		this.steps = 0;
		this.gameStartTime = System.currentTimeMillis();

		AUDIO.playSound("gamestart");
		generateApple();
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

	private void generateApple(){
		Apple apple = new Apple(random.nextInt(this.gameWorld.getWidth()), random.nextInt(this.gameWorld.getHeight()));
		for (int i = 0; i < this.snake.size(); i++){
			SnakeBody sb = this.snake.get(i);
			if ((sb.x == apple.x && sb.y == apple.y)){
				if (this.snake.size() < this.gameWorld.getWidth()*this.gameWorld.getHeight()){
					generateApple();
					return;
				} else {
					apple = null;
					new Thread(() -> {
						try {
							Thread.sleep(2200);
							this.gameFinished = true;
							resetGame(true);
						} catch (InterruptedException ex){
							ex.printStackTrace();
						}
					}).start();
					break;
				}
			}
		}
		this.apple = apple;
	}

	private void setDirection(Point point, SnakeBody head){
		if (point.x > head.x && this.direction != Side.LEFT){
			this.direction = Side.RIGHT;
		}
		if (point.x < head.x && this.direction != Side.RIGHT){
			this.direction = Side.LEFT;
		}
		if (point.y > head.y && this.direction != Side.TOP){
			this.direction = Side.BOTTOM;
		}
		if (point.y < head.y && this.direction != Side.BOTTOM){
			this.direction = Side.TOP;
		}
	}

	@Override
	public void handleInput(PointerEvent event){
		float tx = event.x;
		float ty = event.y;

		if (this.gameFinished){
			if (System.currentTimeMillis()-this.gameOverStartTime > GAME_OVER_TIME){
				this.gameView.triggerVibration(100);
				restoreGameState();
			}
			return;
		}

		switch (event.type) {
			case PRESSED:
				this.startX = tx;
				this.startY = ty;

				// Check controller buttons
				int controllerButton = controllerButtonPressed(tx, ty);
				if (this.gameMode == null){
					if (controllerButton == 0){
						this.timeInterval = Math.max(this.timeInterval - 5, 5);
						this.gameView.triggerVibration(65);
					} else if (controllerButton == 1){
						this.timeInterval += 5;
						this.gameView.triggerVibration(65);
					} else if (controllerButton == 3){
						this.ai = !this.ai;
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

				Rectangle2D homeButton = new Rectangle2D(rsw_reverse(this.homeBtnX)-rsw(this.radius), rsh(this.homeBtnY)-rsw(this.radius), rsw(this.radius)*2, rsw(this.radius)*2);
				if (homeButton.contains(tx, ty)){
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

				if (!this.ai){
					if (this.controlMethod == 0 && !this.paused){ // D-pad
						if (this.upControlShape.contains(tx, ty) && this.direction != Side.TOP){
							this.inputDir = Side.TOP;
							this.gameView.triggerVibration(35);
						} else if (this.rightControlShape.contains(tx, ty) && this.direction != Side.RIGHT){
							this.inputDir = Side.RIGHT;
							this.gameView.triggerVibration(35);
						} else if (this.downControlShape.contains(tx, ty) && this.direction != Side.BOTTOM){
							this.inputDir = Side.BOTTOM;
							this.gameView.triggerVibration(35);
						} else if (this.leftControlShape.contains(tx, ty) && this.direction != Side.LEFT){
							this.inputDir = Side.LEFT;
							this.gameView.triggerVibration(35);
						}
					}
				}

				break;

			case RELEASED:
				this.dragging = false;
				this.lastFingerRelease = System.currentTimeMillis();

				if (!this.ai){
					if (this.controlMethod == 2 && !this.paused){ // Swipe to move
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
				if (!this.ai){
					if (this.controlMethod == 1 && !this.paused){ // Joystick
						Rectangle2D rect = (new Rectangle2D(rsw_reverse(this.gamepad.getMinX()), rsh(this.gamepad.getMinY()), rsw(this.gamepad.getWidth()), rsh(this.gamepad.getHeight()))).translate(rsw(-this.gamepad.getWidth()/2), rsh(-this.gamepad.getHeight()/2));
						if (rect.contains(tx, ty) || this.dragging){
							double relX = (tx - rect.getMinX()) / rect.getWidth();
							double relY = (ty - rect.getMinY()) / rect.getHeight();

							this.gamepadAngle = Math.atan2(relY - 0.5, relX - 0.5);
							this.padFactor = Math.sqrt(Math.pow((relX - 0.5) / 0.5, 2) + Math.pow((relY - 0.5) / 0.5, 2));
							this.dragging = true;

							if (relX > relY && relX > 1 - relY && this.direction != Side.RIGHT){
								this.inputDir = Side.RIGHT;
								this.gameView.triggerVibration(35);
							} else if (relX < relY && relX < 1 - relY && this.direction != Side.LEFT){
								this.inputDir = Side.LEFT;
								this.gameView.triggerVibration(35);
							} else if (relX > relY && relX < 1 - relY && this.direction != Side.TOP){
								this.inputDir = Side.TOP;
								this.gameView.triggerVibration(35);
							} else if (relX < relY && relX > 1 - relY && this.direction != Side.BOTTOM){
								this.inputDir = Side.BOTTOM;
								this.gameView.triggerVibration(35);
							}
						}
					}
				}

				break;
		}
	}

	public void pauseGame(){
		this.paused = false;
		togglePause();
	}

	public void togglePause(){
		if (this.gameFinished)
			return;

		if (!this.paused){
			this.pauseStartTime = System.nanoTime();
		} else {
			long pausedDuration = System.nanoTime() - this.pauseStartTime;
			this.lastFrameTime += pausedDuration;
		}
		this.paused = !this.paused;
	}

	@Override
	public void goBack(Activity activity){
		this.threadRunning = false;
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
		canvas.translate(this.leftHanded ? SCREEN_W-OFFSET_X-this.gameWorld.getWidth()*SnakeBody.SIZE : OFFSET_X, 0);

		for (int i = 0; i <= this.gameWorld.getWidth(); i++){
			canvas.strokeLine(i * SnakeBody.SIZE, 0, i * SnakeBody.SIZE, this.gameWorld.getHeight()*SnakeBody.SIZE, 0xFF1E293B, 1.2);
		}
		for (int i = 0; i <= this.gameWorld.getHeight(); i++){
			canvas.strokeLine(0, i*SnakeBody.SIZE, this.gameWorld.getWidth()*SnakeBody.SIZE, i*SnakeBody.SIZE, 0xFF1E293B, 1.2);
		}

		if (this.allowMovement){
			if (this.inputDir == Side.TOP && this.direction != Side.BOTTOM){
				this.direction = Side.TOP;
				this.allowMovement = false;
				this.inputDir = null;
			} else if (this.inputDir == Side.BOTTOM && this.direction != Side.TOP){
				this.direction = Side.BOTTOM;
				this.allowMovement = false;
				this.inputDir = null;
			} else if (this.inputDir == Side.RIGHT && this.direction != Side.LEFT){
				this.direction = Side.RIGHT;
				this.allowMovement = false;
				this.inputDir = null;
			} else if (this.inputDir == Side.LEFT && this.direction != Side.RIGHT){
				this.direction = Side.LEFT;
				this.allowMovement = false;
				this.inputDir = null;
			}
		}

		if (this.apple != null) this.apple.render(canvas, this.player.getAppleColor(this.player.getAppleIndex()), this.player.getAppleInternalColor(this.player.getAppleIndex()));

		final int snakeIndex = this.player.getSnakeIndex();
		final int snakeColor = this.player.getSnakeColor(this.player.getSnakeIndex());
		synchronized (this){
			for (int count = 0; count < 2; count++){ // Draw a second time to avoid internal dropshadow effects
				final int snakeSize = this.snake.size();
				for (int i = snakeSize - 2; i >= 0; i--){
					SnakeBody sb = this.snake.get(i);
					sb.render(canvas, count == 0, i, this.snake, snakeIndex, snakeColor);
				}
				this.snake.get(snakeSize - 1).render(canvas, count == 0, snakeSize - 1, this.snake, snakeIndex, snakeColor);
			}
		}

		if (this.showInfo){
			this.gameWorld.getCycle().render(canvas, SnakeBody.SIZE);
			canvas.fillText(String.format("FPS: %.2f Direction: %s, TimeInterval: %s, Paused: %s, Steps: %s", this.gameView.getFPS(), this.direction, this.timeInterval, this.paused, this.steps), 40, 85, 0xFF22D3EE, UiElement.FONT_SMALL, TextAlignment.LEFT);
		}

		// Draw grid outline border
		canvas.setEffect(UiElement.rh(0.0010), 0xFFA855F7);
		final double dynamicWidth = 0.0015 + (Math.sin((System.currentTimeMillis() % 1500 / 1500.0) * 2.0*Math.PI) * 0.0010);
		int difficultyColor = 0xFF4CC9F0; // Default value
		if (this.gameMode != null){
			switch (this.gameMode){
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
		canvas.strokeRect(SnakeBody.SIZE*0.05, SnakeBody.SIZE*0.05, this.gameWorld.getWidth()*SnakeBody.SIZE-SnakeBody.SIZE*0.1, this.gameWorld.getHeight()*SnakeBody.SIZE-SnakeBody.SIZE*0.1, difficultyColor, UiElement.rw(dynamicWidth));
		canvas.clearEffect();

		canvas.setEffect(20, 0xFFFFFFFF);
		canvas.fillText(String.format("Score: %d, Highscore: %d"+(this.ai ? " | AI" : ""), this.score, this.highscore), this.leftHanded ? this.gameWorld.getWidth()*SnakeBody.SIZE-40 : 40, 60, 0xFFFFFFFF, UiElement.FONT_MEDIUM, this.leftHanded ? TextAlignment.RIGHT : TextAlignment.LEFT);

		if (this.gameMode != null){
			int textColor = 0;
			switch (this.gameMode){
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

			canvas.fillText(this.gameMode, this.leftHanded ? rsw(1-0.865)-(SCREEN_W-OFFSET_X-this.gameWorld.getWidth()*SnakeBody.SIZE) : rsw(0.865)-OFFSET_X, rsh(0.07), textColor, UiElement.FONT_LARGE, TextAlignment.CENTER);
		}

		canvas.clearEffect();
		canvas.translate(this.leftHanded ? -(SCREEN_W-OFFSET_X-this.gameWorld.getWidth()*SnakeBody.SIZE) : -OFFSET_X, 0);

		if (this.controlMethod == 0){
			canvas.setEffect(40, 0x884CC9F0);
			if (this.direction == Side.TOP) canvas.fillPolygon(this.upControlShape.xPoints, this.upControlShape.yPoints, this.upControlShape.nPoints, 0x884CC9F0);
			if (this.direction == Side.RIGHT) canvas.fillPolygon(this.rightControlShape.xPoints, this.rightControlShape.yPoints, this.rightControlShape.nPoints, 0x884CC9F0);
			if (this.direction == Side.BOTTOM) canvas.fillPolygon(this.downControlShape.xPoints, this.downControlShape.yPoints, this.downControlShape.nPoints, 0x884CC9F0);
			if (this.direction == Side.LEFT) canvas.fillPolygon(this.leftControlShape.xPoints, this.leftControlShape.yPoints, this.leftControlShape.nPoints, 0x884CC9F0);
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
			switch (this.direction){
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

		if (this.paused || this.gameFinished){
			canvas.fillRect(0, 0, SCREEN_W, SCREEN_H, 0xCC000000);
			canvas.fillText(this.gameFinished ? (this.apple == null ? "YOU WIN!" : "GAMEOVER") : "GAME PAUSED", SCREEN_W/2.0, this.gameFinished ? SCREEN_H*0.25 : SCREEN_H*0.4, 0xFFFFFFFF, UiElement.FONT_EXTRALARGE, TextAlignment.CENTER);
			if (this.gameFinished){
				if (this.score > 0) canvas.fillText(this.gameMode != null ? String.format("You collected %d MangoCoins!", this.score * getMultiplier(this.gameMode)) : "There are no rewards for casual games!", SCREEN_W/2.0, SCREEN_H*0.35, 0xFFFFD600, UiElement.FONT_LARGE, TextAlignment.CENTER);
				canvas.setEffect(30, 0xFF4CC9F0);
				canvas.fillText(Integer.toString(this.score), SCREEN_W/2.0, SCREEN_H*0.5, 0xFF4CC9F0, UiElement.FONT_EXTRAEXTRALARGE, TextAlignment.CENTER);
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

				if (this.showHighscore && this.gameMode != null){
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
		canvas.fillOval(cx-rsw(this.radius), cy-rsw(this.radius), rsw(this.radius)*2, rsw(this.radius)*2, this.paused ? 0xFFFDA4AF : 0x08FFFFFF);
		canvas.fillOval(minX+this.autoplayCx*width-rsw(this.radius), minY+0.5*height-rsw(this.radius), rsw(this.radius)*2, rsw(this.radius)*2, this.ai ? 0xFF2DD4BF : 0x08FFFFFF);

		canvas.setEffect(10, 0xFF2DD4BF);
		canvas.strokeOval(minX+this.speedHCx*width-rsw(this.radius), minY+0.5*height-rsw(this.radius), rsw(this.radius)*2, rsw(this.radius)*2, 0x33FFFFFF, UiElement.rh(0.0025));
		canvas.strokeOval(minX+this.speedLCx*width-rsw(this.radius), minY+0.5*height-rsw(this.radius), rsw(this.radius)*2, rsw(this.radius)*2, 0x33FFFFFF, UiElement.rh(0.0025));
		canvas.strokeOval(cx-rsw(this.radius), cy-rsw(this.radius), rsw(this.radius)*2, rsw(this.radius)*2, 0x33FFFFFF, UiElement.rh(0.0025));
		canvas.strokeOval(minX+this.autoplayCx*width-rsw(this.radius), minY+0.5*height-rsw(this.radius), rsw(this.radius)*2, rsw(this.radius)*2, 0x33FFFFFF, UiElement.rh(0.0025));
		canvas.clearEffect();

		canvas.fillText("<<", minX+this.speedHCx*width, minY+0.585*height, this.gameMode != null ? 0x6694A3B8 : 0xFFFFFFFF, UiElement.FONT_LARGELARGE, TextAlignment.CENTER);
		canvas.fillText(">>", minX+this.speedLCx*width, minY+0.585*height, this.gameMode != null ? 0x6694A3B8 : 0xFFFFFFFF, UiElement.FONT_LARGELARGE, TextAlignment.CENTER);
		canvas.fillText("A", minX+this.autoplayCx*width, minY+0.585*height, this.gameMode != null ? 0x6694A3B8 : 0xFFFFFFFF, UiElement.FONT_LARGELARGE, TextAlignment.CENTER);

		// Pause button
		if (this.paused){
			double[] xPoints = new double[]{cx-rsw(0.006), cx-rsw(0.006), cx+rsw(0.012)};
			double[] yPoints = new double[]{cy-rsh(0.020), cy+rsh(0.020), cy};
			canvas.fillPolygon(xPoints, yPoints, 3, 0xFFF43F5E);
		} else {
			canvas.strokeLine(cx-rsw(0.005), cy-rsh(0.015), cx-rsw(0.005), cy+rsh(0.015), 0xFF22D3EE, UiElement.rh(0.005));
			canvas.strokeLine(cx+rsw(0.005), cy-rsh(0.015), cx+rsw(0.005), cy+rsh(0.015), 0xFF22D3EE, UiElement.rh(0.005));
		}

		// Home button
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
