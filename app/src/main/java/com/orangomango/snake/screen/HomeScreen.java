package com.orangomango.snake.screen;

import java.util.*;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.net.Uri;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.orangomango.androidbridge.geometry.TextAlignment;
import com.orangomango.snake.GameView;
import com.orangomango.snake.Player;
import com.orangomango.snake.R;
import com.orangomango.snake.game.GameDifficulty;
import com.orangomango.snake.ui.*;
import com.orangomango.account.Account;

import com.orangomango.androidbridge.*;
import com.orangomango.androidbridge.util.Pair;
import com.orangomango.androidbridge.geometry.Rectangle2D;

import static com.orangomango.snake.GameView.AUDIO;
import static com.orangomango.snake.GameView.WIDTH;
import static com.orangomango.snake.game.SnakeBody.SKIN_LAUNCH_ID;

import androidx.browser.customtabs.CustomTabsIntent;

public class HomeScreen extends Screen{
	private static int OFFSET_X;
	private static boolean LOGIN_INFO = false; // Logged in flag

	public static final int COLOR_EASY = 0xFF10B981;
	public static final int COLOR_MEDIUM = 0xFF3B82F6;
	public static final int COLOR_HARD = 0xFFEF4444;
	public static final int COLOR_EXTREME = 0xFFA855F7;

	private final ArrayList<UiElement> uielements = new ArrayList<>();
	private final Rectangle2D headerRect = new Rectangle2D(0.05, 0.04, 0.90, 0.08);
	private final Rectangle2D leaderboardRect = new Rectangle2D(0.232, 0.15, 0.285, 0.62);
	private final Rectangle2D settingsRect = new Rectangle2D(0.729, 0.15, 0.24, 0.62);
	private final Button leadEasy, leadMedium, leadHard, leadExtreme;
	private final Slider cellSlider, speedSlider;
	private final ToggleButton aiMode, wrapping;
	private final MultistateButton difficultyButton;
	private Button loginButton, signupButton, playButton;
	private final Button control_dPad, control_joystick, control_swipe;
	private Account account = null;
	private String gameMode = null;
	private final ArrayList<ArrayList<Pair<String, long[]>>> leaderboards = new ArrayList<>();
	private int currentLeadMode;
	private final InputField usernameField, passwordField;
	private final GlobalSettings globalSettings;
	private volatile ArrayList<String> friends = new ArrayList<>();
	private boolean filterFriends = false;
	private Player player;

	public static GameSettings GAME_SETTINGS = new GameSettings(GameDifficulty.HARD.getCellSize(), 1/GameDifficulty.HARD.getSpeed(), false, false, 2);
	public static PurchaseHandler PURCHASE_HANDLER;

	private static class GameSettings{
		public double cellSize;
		public double speed;
		public boolean autoplay;
		public boolean wrapping;
		public int gameMode;

		public GameSettings(double cellSize, double speed, boolean autoplay, boolean wrapping, int gameMode){
			this.cellSize = cellSize;
			this.speed = speed;
			this.autoplay = autoplay;
			this.wrapping = wrapping;
			this.gameMode = gameMode;
		}
	}

	public static class GlobalSettings{
		public double musicVolume;
		public double effectsVolume;
		public int controlMethod;
		public boolean vibrations;
		public boolean leftHanded;
		public boolean randomSkin;

		public GlobalSettings(double musicVolume, double effectsVolume, int controlMethod, boolean vibrations, boolean leftHanded, boolean randomSkin){
			this.musicVolume = musicVolume;
			this.effectsVolume = effectsVolume;
			this.controlMethod = controlMethod;
			this.vibrations = vibrations;
			this.leftHanded = leftHanded;
			this.randomSkin = randomSkin;
		}

		public void saveSettings(Context context){
			FileHelper.writeInternalFile(context, "app.settings", String.format("%s %s %d %d %d %d", this.musicVolume, this.effectsVolume, this.controlMethod, this.vibrations ? 1 : 0, this.leftHanded ? 1 : 0, this.randomSkin ? 1 : 0));
		}
	}

	public static class PurchaseHandler{
		private Player player;
		private Account account;
		private GameView gameView;
		private ArrayList<String> purchases = new ArrayList<>();

		public static final String SKIN_PREMIUM = "skin_premium";
		public static final String SKIN_LAUNCH = "skin_launch";
		public static final String MANGOCOINS_BUNDLE = "mangocoins_bundle";

		public PurchaseHandler(Player player, Account account, GameView gameView){
			this.player = player;
			this.account = account;
			this.gameView = gameView;
		}

		public void handlePurchase(String id){
			this.player.syncAndSave(this.account); // First sync cloud and appdata

			if (id.equals(SKIN_PREMIUM)){
				try{
					String jsonData = FileHelper.readRawResource(this.gameView.getContext(), R.raw.customize);
					JSONObject json = new JSONObject(jsonData);
					int totalSnakes = json.getJSONObject("snake").getJSONObject("colors").length();
					int totalApples = json.getJSONObject("apple").getJSONObject("colors").length();

					this.player.unlockPremiumSkins(totalSnakes, totalApples);
					this.player.setAdBlock();
					this.player.forcePush(this.account);

					this.purchases.add(SKIN_PREMIUM);
				} catch (JSONException ex){
					ex.printStackTrace();
				}
			} else if (id.equals(SKIN_LAUNCH)){
				this.player.setPermanentSnakeIndex(SKIN_LAUNCH_ID);
				this.player.forcePush(this.account);

				this.purchases.add(SKIN_LAUNCH);
			} else if (id.equals(MANGOCOINS_BUNDLE)){
				try {
					this.player.getAppData().put("currency", this.player.getAppData().optInt("currency", 0) + 10000);
					this.player.setAdBlock();
					this.player.forcePush(this.account);
				} catch (JSONException ex){
					ex.printStackTrace();
				}

				// This item can be purchased more than once
			}
		}

		public boolean isPurchased(String id){
			return this.purchases.contains(id);
		}

		public void updateAccount(Account account){
			this.account = account;
		}

		public void updatePlayer(Player player){
			this.player = player;
		}
	}

	public HomeScreen(GameView gameView){
		super(gameView);

		if (PURCHASE_HANDLER == null) PURCHASE_HANDLER = new PurchaseHandler(null, null, this.gameView);

		this.globalSettings = new GlobalSettings(0.5, 1, 0, true, false, false);
		String savedSettings = FileHelper.readInternalFile(this.gameView.getContext(), "app.settings");
		if (savedSettings != null){
			try{
				String[] data = savedSettings.split(" ");
				this.globalSettings.musicVolume = Double.parseDouble(data[0]);
				this.globalSettings.effectsVolume = Double.parseDouble(data[1]);
				this.globalSettings.controlMethod = Integer.parseInt(data[2]);
				this.globalSettings.vibrations = Integer.parseInt(data[3]) == 1;
				this.globalSettings.leftHanded = Integer.parseInt(data[4]) == 1;
				this.globalSettings.randomSkin = Integer.parseInt(data[5]) == 1;
			} catch (Exception ex){
				FileHelper.deleteInternalFile(this.gameView.getContext(), "app.settings"); // Reset settings file when corrupted
			}
		}

		this.player = new Player(gameView.getContext()); // Only one instance available!

		// Sliders
		cellSlider = new Slider(this.gameView, 0.548, 0.25, 0.15, 0.10, "Grid Size", "px");
		cellSlider.setInterval(25, 60, GAME_SETTINGS.cellSize);
		cellSlider.setFormatter(value -> {
			int[] gridSize = GameDifficulty.calculateGridSize(value);
			return String.format("%dx%d", gridSize[0], gridSize[1]);
		});
		speedSlider = new Slider(this.gameView, 0.548, 0.38, 0.15, 0.10, "Speed", "%"); // Percentage
		speedSlider.setInterval(1/300.0, 1/40.0, GAME_SETTINGS.speed);
		speedSlider.setBounds(0.01, 1.0); // Don't allow 0%

		// Toggle buttons
		aiMode = new ToggleButton(this.gameView, 0.548, 0.49, 0.15, 0.08, "Auto-Play");
		aiMode.setSelected(GAME_SETTINGS.autoplay);
		wrapping = new ToggleButton(this.gameView, 0.548, 0.56, 0.15, 0.08, "Wrapping");
		wrapping.setSelected(GAME_SETTINGS.wrapping);

		// Game mode button
		Button gameModeButton = new Button(this.gameView, 0xFF0099FF, 0xFF00E5FF, 0.548, 0.66, 0.15, 0.07, "Game Mode", UiElement.FONT_MEDIUM, 0xFFFFFFFF, () -> {
			CasualScreen cs = new CasualScreen(this.gameView);
			this.gameView.setScreen(cs);
		});

		// Login
		this.usernameField = new InputField(this.gameView, 0.0465, 0.231, 0.162, 0.05, "Username");
		this.passwordField = new InputField(this.gameView, 0.0465, 0.293, 0.162, 0.05, "Password");
		passwordField.setPasswordField(true);
		this.loginButton = new Button(this.gameView, 0xFF10B981, 0xFF059669, 0.0465, 0.362, 0.162, 0.05, "LOGIN", UiElement.FONT_SMALL, 0xFFFFFFFF, () -> {
			if (this.account == null){
				new Thread(() -> {
					try{
						Account account = new Account(this.usernameField.getText(), this.passwordField.getText());
						JSONObject data = account.login();
						if (data != null && data.getBoolean("success")){
							login(account);
							account.logMessage("Requested login");
						} else {
							this.loginButton.bounce();
						}
					} catch (JSONException ex){
						ex.printStackTrace();
					}
				}).start();
			} else {
				logout();
			}
		});

		// Load login data
		new Thread(() -> {
			String savedData = FileHelper.readInternalFile(this.gameView.getContext(), "credentials.data");
			if (savedData != null){
				try{
					String[] data = savedData.split(" ");
					Account account = new Account(data[0], data[1]);
					JSONObject json = account.login();
					if (json != null && json.getBoolean("success")){
						login(account);
					}
				} catch (JSONException ex){
					ex.printStackTrace();
				}
			}
		}).start();

		this.signupButton = new Button(this.gameView, 0x1AA855F7, 0x4DA855F7, 0.05, 0.82, 0.15, 0.12, "SIGNUP", UiElement.FONT_LARGE, 0xFFFFFFFF, () -> {
			CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();
			CustomTabsIntent customTabsIntent = builder.build();
			customTabsIntent.launchUrl(this.gameView.getContext(), Uri.parse("https://id.orangomango.org/account/signup.php"));
		});

		this.playButton = new Button(this.gameView, 0xFF10B981, 0xFF059669, 0.55, 0.82, 0.40, 0.12, "PLAY RANKED", UiElement.FONT_LARGE, 0xFFFFFFFF, () -> {
			GameScreen gs = new GameScreen(this.gameView, this.account, this.player, this.gameMode, (int)(1/speedSlider.getValue()), aiMode.getSelected(), wrapping.getSelected(), this.globalSettings.controlMethod, this.globalSettings.leftHanded, this.globalSettings.randomSkin);
			gs.initGame((int)cellSlider.getValue());
			this.gameView.setScreen(gs);
		});
		this.playButton.setBorderSize(0.0045);

		Button skinsButton = new Button(this.gameView, 0x1AFF007F, 0xFFF472B6, 0.28, 0.82, 0.20, 0.12, "CUSTOMIZE", UiElement.FONT_LARGE, 0xFFFFFFFF, () -> {
			CustomizeScreen cs = new CustomizeScreen(this.gameView, this.player, this.account, this.leaderboards, this.globalSettings);
			this.gameView.setScreen(cs);
		});

		difficultyButton = new MultistateButton(this.gameView, 0xFF001D27, 0.04875, 0.55, 0.1575, 0.18);
		difficultyButton.addState("EASY", COLOR_EASY, () -> setDifficulty(0));
		difficultyButton.addState("MEDIUM", COLOR_MEDIUM, () -> setDifficulty(1));
		difficultyButton.addState("HARD", COLOR_HARD, () -> setDifficulty(2));
		difficultyButton.addState("EXTREME", COLOR_EXTREME, () -> setDifficulty(3));

		// Runnable for setting different game mode
		Runnable rankedGame = () -> {
			this.playButton.setStyle(0xFF10B981, 0xFF059669, "PLAY RANKED", null);
			difficultyButton.setDisabled(false);
		};

		Runnable casualGame = () -> {
			this.playButton.setStyle(0xFF3B82F6, 0xFF4CC9F0, "PLAY CASUAL", null);
			difficultyButton.setDisabled(true);
			this.gameMode = null;

			GAME_SETTINGS.gameMode = -1;
			GAME_SETTINGS.cellSize = cellSlider.getValue();
			GAME_SETTINGS.speed = speedSlider.getValue();
			GAME_SETTINGS.autoplay = aiMode.getSelected();
			GAME_SETTINGS.wrapping = wrapping.getSelected();
		};

		difficultyButton.setOnStateChanged(rankedGame);
		cellSlider.setOnStateChanged(casualGame);
		speedSlider.setOnStateChanged(casualGame);
		aiMode.setOnStateChanged(casualGame);
		wrapping.setOnStateChanged(casualGame);

		if (GAME_SETTINGS.gameMode < 0) casualGame.run();
		else rankedGame.run();

		// Leaderboard
		this.leadEasy = new Button(this.gameView, 0xFF001D27, COLOR_EASY, this.leaderboardRect.getMinX()+0.05*this.leaderboardRect.getWidth(), this.leaderboardRect.getMinY()+0.12*this.leaderboardRect.getHeight(), 0.2175*this.leaderboardRect.getWidth(), 0.08*this.leaderboardRect.getHeight(), "EASY", UiElement.FONT_SMALL, COLOR_EASY, () -> setDifficulty(0));
		this.leadMedium = new Button(this.gameView, 0xFF001D27, COLOR_MEDIUM, this.leaderboardRect.getMinX()+0.28*this.leaderboardRect.getWidth(), this.leaderboardRect.getMinY()+0.12*this.leaderboardRect.getHeight(), 0.2175*this.leaderboardRect.getWidth(), 0.08*this.leaderboardRect.getHeight(), "MED", UiElement.FONT_SMALL, COLOR_MEDIUM, () -> setDifficulty(1));
		this.leadHard = new Button(this.gameView, 0xFF001D27, COLOR_HARD, this.leaderboardRect.getMinX()+0.51*this.leaderboardRect.getWidth(), this.leaderboardRect.getMinY()+0.12*this.leaderboardRect.getHeight(), 0.2175*this.leaderboardRect.getWidth(), 0.08*this.leaderboardRect.getHeight(), "HARD", UiElement.FONT_SMALL, COLOR_HARD, () -> setDifficulty(2));
		this.leadExtreme = new Button(this.gameView, 0xFF001D27, COLOR_EXTREME, this.leaderboardRect.getMinX()+0.74*this.leaderboardRect.getWidth(), this.leaderboardRect.getMinY()+0.12*this.leaderboardRect.getHeight(), 0.2175*this.leaderboardRect.getWidth(), 0.08*this.leaderboardRect.getHeight(), "EXT", UiElement.FONT_SMALL, COLOR_EXTREME, () -> setDifficulty(3));

		this.leadEasy.setBorderSize(0.002);
		this.leadMedium.setBorderSize(0.002);
		this.leadHard.setBorderSize(0.002);
		this.leadExtreme.setBorderSize(0.002);

		final double fw = 0.44, fh = 0.16; // ToggleButton size
		ToggleButton friendsLead = new ToggleButton(this.gameView, this.leaderboardRect.getMinX()+(0.5-fw/2)*this.leaderboardRect.getWidth(), this.leaderboardRect.getMinY()+(0.95-fh/2)*this.leaderboardRect.getHeight(), fw*this.leaderboardRect.getWidth(), fh*this.leaderboardRect.getHeight(), "Friends");
		friendsLead.setOnStateChanged(() -> this.filterFriends = friendsLead.getSelected());

		selectLeadMode(0);
		if (GAME_SETTINGS.gameMode >= 0) setDifficulty(GAME_SETTINGS.gameMode);
		fetchLeaderboards();

		// Settings
		Slider musicVolume = new Slider(this.gameView, 0.740, 0.47, 0.1, 0.10, "Music", "%"); // Percentage
		musicVolume.setInterval(0, 1, this.globalSettings.musicVolume);
		musicVolume.setOnStateChanged(() -> {
			AUDIO.setMusicVolume((float)musicVolume.getValue());
			this.globalSettings.musicVolume = musicVolume.getValue();
			this.globalSettings.saveSettings(this.gameView.getContext());
		});
		Slider effectsVolume = new Slider(this.gameView, 0.852, 0.47, 0.1, 0.10, "SFX", "%"); // Percentage
		effectsVolume.setInterval(0, 1, this.globalSettings.effectsVolume);
		effectsVolume.setOnStateChanged(() -> {
			AUDIO.setSoundVolume((float)effectsVolume.getValue());
			this.globalSettings.effectsVolume = effectsVolume.getValue();
			this.globalSettings.saveSettings(this.gameView.getContext());
		});
		ToggleButton vibrations = new ToggleButton(this.gameView, 0.744, 0.58, 0.21, 0.10, "Vibrations");
		vibrations.setSelected(this.globalSettings.vibrations);
		vibrations.setOnStateChanged(() -> {
			this.gameView.setVibrate(vibrations.getSelected());
			this.globalSettings.vibrations = vibrations.getSelected();
			this.globalSettings.saveSettings(this.gameView.getContext());
		});
		ToggleButton leftHanded = new ToggleButton(this.gameView, 0.744, 0.65, 0.21, 0.10, "Left-handed");
		leftHanded.setSelected(this.globalSettings.leftHanded);
		leftHanded.setOnStateChanged(() -> {
			this.globalSettings.leftHanded = leftHanded.getSelected();
			this.globalSettings.saveSettings(this.gameView.getContext());
		});

		// Control mode (settings)
		this.control_dPad = new Button(this.gameView, 0xFF001D27, 0xFF4CC9F0, this.settingsRect.getMinX()+0.05*this.settingsRect.getWidth(), this.settingsRect.getMinY()+0.15*this.settingsRect.getHeight(), 0.28*this.settingsRect.getWidth(), 0.20*this.settingsRect.getHeight(), null, -1, 0, () -> setControlMethod(0));
		this.control_joystick = new Button(this.gameView, 0xFF001D27, 0xFF4CC9F0, this.settingsRect.getMinX()+0.36*this.settingsRect.getWidth(), this.settingsRect.getMinY()+0.15*this.settingsRect.getHeight(), 0.28*this.settingsRect.getWidth(), 0.20*this.settingsRect.getHeight(), null, -1, 0, () -> setControlMethod(1));
		this.control_swipe = new Button(this.gameView, 0xFF001D27, 0xFF4CC9F0, this.settingsRect.getMinX()+0.67*this.settingsRect.getWidth(), this.settingsRect.getMinY()+0.15*this.settingsRect.getHeight(), 0.28*this.settingsRect.getWidth(), 0.20*this.settingsRect.getHeight(), null, -1, 0, () -> setControlMethod(2));

		this.control_dPad.setBorderSize(0.005);
		this.control_dPad.setGlow(true);
		this.control_dPad.setBitmap(BitmapFactory.decodeResource(this.gameView.getContext().getResources(), R.drawable.dpad));
		this.control_joystick.setBorderSize(0.005);
		this.control_joystick.setGlow(true);
		this.control_joystick.setBitmap(BitmapFactory.decodeResource(this.gameView.getContext().getResources(), R.drawable.joystick));
		this.control_swipe.setBorderSize(0.005);
		this.control_swipe.setGlow(true);
		this.control_swipe.setBitmap(BitmapFactory.decodeResource(this.gameView.getContext().getResources(), R.drawable.swipe));

		setControlMethod(this.globalSettings.controlMethod);

		AUDIO.setMusicVolume((float)this.globalSettings.musicVolume);
		AUDIO.setSoundVolume((float)this.globalSettings.effectsVolume);
		this.gameView.setVibrate(this.globalSettings.vibrations);

		this.uielements.add(new Container(this.gameView, 0xFF0F172A, 0xFF1E293B, 0.0375, 0.15, 0.18, 0.28, 0xFF94F7D4, UiElement.FONT_MEDIUM, "MangoGames ID")); // Authentication
		this.uielements.add(new Container(this.gameView, 0xFF0F172A, 0xFF1E293B, 0.0375, 0.45, 0.18, 0.32, 0xFF94F7D4, UiElement.FONT_MEDIUM, "Difficulty")); // Game difficulty selection
		this.uielements.add(new Container(this.gameView, 0xFF0F172A, 0xFF1E293B, this.leaderboardRect.getMinX(), this.leaderboardRect.getMinY(), this.leaderboardRect.getWidth(), this.leaderboardRect.getHeight(), 0xFF94F7D4, UiElement.FONT_MEDIUM, "Leaderboard")); // Leaderboard
		this.uielements.add(new Container(this.gameView, 0xFF0F172A, 0xFF1E293B, 0.533, 0.15, 0.18, 0.62, 0xFF94F7D4, UiElement.FONT_MEDIUM, "Custom game")); // Custom settings
		this.uielements.add(new Container(this.gameView, 0xFF0F172A, 0xFF1E293B, this.settingsRect.getMinX(), this.settingsRect.getMinY(), this.settingsRect.getWidth(), this.settingsRect.getHeight(), 0xFF94F7D4, UiElement.FONT_MEDIUM, "Settings")); // App settings
		this.uielements.add(new Container(this.gameView, 0xFF0F172A, 0xFF10B981, this.headerRect.getMinX(), this.headerRect.getMinY(), this.headerRect.getWidth(), this.headerRect.getHeight(), null, null, null)); // Game logo

		this.uielements.add(this.signupButton);
		this.uielements.add(this.playButton);
		this.uielements.add(skinsButton);
		this.uielements.add(cellSlider);
		this.uielements.add(speedSlider);
		this.uielements.add(aiMode);
		this.uielements.add(wrapping);
		this.uielements.add(gameModeButton);
		this.uielements.add(this.usernameField);
		this.uielements.add(this.passwordField);
		this.uielements.add(this.loginButton);
		this.uielements.add(difficultyButton);
		this.uielements.add(this.leadEasy);
		this.uielements.add(this.leadMedium);
		this.uielements.add(this.leadHard);
		this.uielements.add(this.leadExtreme);
		this.uielements.add(friendsLead);
		this.uielements.add(musicVolume);
		this.uielements.add(effectsVolume);
		this.uielements.add(vibrations);
		this.uielements.add(leftHanded);
		this.uielements.add(this.control_dPad);
		this.uielements.add(this.control_joystick);
		this.uielements.add(this.control_swipe);
	}

	@Override
	public void handleInput(PointerEvent event){
		this.uielements.stream().filter(el -> el instanceof MouseSensible).filter(el -> this.account == null || (el != this.signupButton && el != this.usernameField && el != this.passwordField)).forEach(el -> ((MouseSensible)el).onHover(event.x-OFFSET_X, event.y));

		switch (event.type){
			case PRESSED:
				this.uielements.stream().filter(el -> el instanceof MouseSensible).filter(el -> this.account == null || (el != this.signupButton && el != this.usernameField && el != this.passwordField)).forEach(el -> ((MouseSensible)el).onClick(event.x-OFFSET_X, event.y));

				Rectangle2D leadHitBox = new Rectangle2D(this.leaderboardRect.getMinX(), this.leaderboardRect.getMinY()+this.leaderboardRect.getHeight()*0.3, this.leaderboardRect.getWidth(), this.leaderboardRect.getHeight()*0.6);
				if (leadHitBox.scale(UiElement::rw, UiElement::rh).contains(event.x, event.y)){
					setDifficulty((GAME_SETTINGS.gameMode + 1) % 4);
					AUDIO.playSound("gui");
					this.gameView.triggerVibration(100);
				}

				if (this.headerRect.scale(UiElement::rw, UiElement::rh).contains(event.x, event.y)){
					AUDIO.playSound("gui");
					this.gameView.triggerVibration(100);
					CreditsScreen cs = new CreditsScreen(this.gameView);
					this.gameView.setScreen(cs);
					return;
				}

				break;
			case DRAGGED:
				this.uielements.stream().filter(el -> el instanceof MouseSensible).filter(el -> this.account == null || (el != this.signupButton && el != this.usernameField && el != this.passwordField)).forEach(el -> ((MouseSensible)el).onDrag(event.x-OFFSET_X, event.y));
				break;
			case RELEASED:
				this.uielements.stream().filter(el -> el instanceof MouseSensible).filter(el -> this.account == null || (el != this.signupButton && el != this.usernameField && el != this.passwordField)).forEach(el -> ((MouseSensible)el).onRelease(event.x-OFFSET_X, event.y));
				break;
		}
	}

	private void updateFriendsList(){
		try{
			JSONArray array = this.account.listFriends();
			ArrayList<String> temp = new ArrayList<>();
			for (int i = 0; i < array.length(); i++){
				temp.add(array.getJSONObject(i).getString("username"));
			}
			this.friends = temp;
		} catch (JSONException ex){
			ex.printStackTrace();
		}
	}

	private void login(Account account){
		this.account = account;
		fetchLeaderboards();
		updateFriendsList();

		this.usernameField.setText("");
		this.passwordField.setText("");
		this.loginButton.setStyle(0xFFEF4444, 0xFF7F1D1D, "LOGOUT", null);

		FileHelper.writeInternalFile(this.gameView.getContext(), "credentials.data", this.account.getUsername().trim()+" "+this.account.getPassword().trim());

		this.player.syncAndSave(account); // Sync accounts on login

		if (!LOGIN_INFO){
			LOGIN_INFO = this.account.logMessage("Logged in");
		}
	}

	private void logout(){
		this.account.logout();
		this.account = null;
		this.loginButton.setStyle(0xFF10B981, 0xFF059669, "LOGIN", null);

		FileHelper.deleteInternalFile(this.gameView.getContext(), "credentials.data");
		FileHelper.deleteInternalFile(this.gameView.getContext(), "usersave.json");
		this.player = new Player(this.gameView.getContext());
	}

	@Override
	public void update(int screenWidth, int screenHeight){
		OFFSET_X = (screenWidth-WIDTH)/2;

		// Once the list is empty, this method does nothing
		this.gameView.getBillingManager().emptyPendingList();
	}

	@Override
	public void render(ICanvas canvas){
		canvas.clear(0xFF020617);

		canvas.save();
		canvas.translate(OFFSET_X, 0);

		for (UiElement element : this.uielements){
			if (this.account != null && (element == this.signupButton || element == this.usernameField || element == this.passwordField)) continue;
			element.render(canvas);
		}

		// Render login text
		if (this.account != null){
			canvas.fillText("LOGGED IN AS", UiElement.rw(0.1275), UiElement.rh(0.265), 0xFF94F7D4, UiElement.FONT_SMALL, TextAlignment.CENTER);
			canvas.fillText(this.account.getUsername(), UiElement.rw(0.1275), UiElement.rh(0.320), 0xFFFFFFFF, UiElement.FONT_MEDIUM, TextAlignment.CENTER);
		}

		// Render leaderboard
		if (this.currentLeadMode < this.leaderboards.size()){
			ArrayList<Pair<String, long[]>> data = this.leaderboards.get(this.currentLeadMode);
			renderLeaderboard(canvas, data);
		} else {
			canvas.fillText("Logging in...", UiElement.rw(this.leaderboardRect.getMinX()+this.leaderboardRect.getWidth()*0.5), UiElement.rh(this.leaderboardRect.getMinY()+this.leaderboardRect.getHeight()*0.5), 0xFFFFFFFF, UiElement.FONT_MEDIUM, TextAlignment.CENTER);
		}

		// Render game name and version
		canvas.setEffect(20, 0xFF10B981);
		canvas.fillText(this.gameView.getContext().getResources().getString(R.string.app_name)+" v"+this.gameView.getContext().getResources().getString(R.string.app_version)+" [android] - www.orangomango.org - Tap for credits", UiElement.rw(this.headerRect.getMinX()+this.headerRect.getWidth()*0.5), UiElement.rh(this.headerRect.getMinY()+this.headerRect.getHeight()*0.65), 0xFF10B981, UiElement.FONT_MEDIUM, TextAlignment.CENTER);
		canvas.clearEffect();

		// Render control method text
		canvas.setEffect(10, 0xFF4CC9F0);
		String controlMethod = null;
		switch (this.globalSettings.controlMethod){
			case 0:
				controlMethod = "D-PAD";
				break;
			case 1:
				controlMethod = "JOYSTICK";
				break;
			case 2:
				controlMethod = "SWIPE TO MOVE";
				break;
		}
		canvas.fillText(controlMethod, UiElement.rw(this.settingsRect.getMinX()+0.5*this.settingsRect.getWidth()), UiElement.rh(this.settingsRect.getMinY()+0.42*this.settingsRect.getHeight()), 0xFF4CC9F0, UiElement.FONT_SMALL, TextAlignment.CENTER);
		canvas.clearEffect();

		canvas.restore();
	}

	private void renderLeaderboard(ICanvas canvas, ArrayList<Pair<String, long[]>> data){
		final double bx = this.leaderboardRect.getMinX();
		final double by = this.leaderboardRect.getMinY();
		final double bw = this.leaderboardRect.getWidth();
		final double bh = this.leaderboardRect.getHeight();
		final double headerY = UiElement.rh(by + 0.26 * bh);

		canvas.fillText("#", UiElement.rw(bx+0.08*bw), headerY, 0xFF94F7D4, UiElement.FONT_SMALL, TextAlignment.LEFT);
		canvas.fillText("PLAYER", UiElement.rw(bx+0.22*bw), headerY, 0xFF94F7D4, UiElement.FONT_SMALL, TextAlignment.LEFT);
		canvas.fillText("SCORE", UiElement.rw(bx+0.92*bw), headerY,  0xFF94F7D4, UiElement.FONT_SMALL, TextAlignment.RIGHT);

		int counter = 0;
		for (int i = 0; i < data.size(); i++) {
			double rowY = by + (0.33 + (counter * 0.075)) * bh;
			Pair<String, long[]> entry = data.get(i);

			if (this.account != null){
				if (this.filterFriends && !entry.getKey().equals(this.account.getUsername()) && !this.friends.contains(entry.getKey())){
					continue;
				}
			}

			canvas.fillText(String.valueOf(counter+1), UiElement.rw(bx+0.08*bw), UiElement.rh(rowY), counter < 3 ? 0xFF10B981 : 0xFFCBD5E1, UiElement.FONT_MEDIUM, TextAlignment.LEFT);
			canvas.fillText(entry.getKey(), UiElement.rw(bx+0.22*bw), UiElement.rh(rowY), counter < 3 ? 0xFF10B981 : 0xFFCBD5E1, UiElement.FONT_MEDIUM, TextAlignment.LEFT);
			canvas.fillText(String.valueOf(entry.getValue()[0]), UiElement.rw(bx+0.92*bw), UiElement.rh(rowY), counter < 3 ? 0xFF10B981 : 0xFFCBD5E1, UiElement.FONT_MEDIUM, TextAlignment.RIGHT);
			counter++;

			if (counter == 6) break;
		}

		if (this.account != null){
			int userRank = -1;
			long userScore = 0;
			int count = 1;

			for (Pair<String, long[]> p : data){
				if (this.filterFriends && !p.getKey().equals(this.account.getUsername()) && !this.friends.contains(p.getKey())){
					continue;
				}

				if (p.getKey().equals(this.account.getUsername())){
					userScore = p.getValue()[0];
					userRank = count;
					break;
				}
				count++;
			}

			if (userRank != -1){
				final double userRowY = UiElement.rh(by + 0.86 * bh);
				canvas.setLineDashes(UiElement.rw(0.005), UiElement.rw(0.005));
				canvas.strokeRoundRect(UiElement.rw(bx + 0.05 * bw), UiElement.rh(by + 0.75 * bh), UiElement.rw(0.90 * bw), UiElement.rh(0.14 * bh), UiElement.rh(0.020), UiElement.rh(0.020), 0xFF10B981, UiElement.rh(0.0030));
				canvas.clearEffect();

				canvas.fillText("PERSONAL STATUS", UiElement.rw(bx + 0.08 * bw), UiElement.rh(by + 0.80 * bh), 0xFF94F7D4, UiElement.FONT_SMALL, TextAlignment.LEFT);

				canvas.fillText("#" + userRank, UiElement.rw(bx + 0.08 * bw), userRowY, 0xFF10B981, UiElement.FONT_MEDIUM, TextAlignment.LEFT);
				canvas.fillText(this.account.getUsername(), UiElement.rw(bx + 0.22 * bw), userRowY, 0xFF10B981, UiElement.FONT_MEDIUM, TextAlignment.LEFT);
				canvas.fillText(String.valueOf(userScore), UiElement.rw(bx + 0.92 * bw), userRowY, 0xFF10B981, UiElement.FONT_MEDIUM, TextAlignment.RIGHT);
			}
		}
	}

	private void setControlMethod(int value){
		final int activeColor = 0xFF4CC9F0;
		this.globalSettings.controlMethod = value;
		this.globalSettings.saveSettings(this.gameView.getContext());

		switch (value){
			case 0:
				this.control_dPad.setStyle(null, activeColor, null, activeColor);
				this.control_joystick.setStyle(null, 0xFF1E293B, null, 0xFF64748B);
				this.control_swipe.setStyle(null, 0xFF1E293B, null, 0xFF64748B);
				break;
			case 1:
				this.control_dPad.setStyle(null, 0xFF1E293B, null, 0xFF64748B);
				this.control_joystick.setStyle(null, activeColor, null, activeColor);
				this.control_swipe.setStyle(null, 0xFF1E293B, null, 0xFF64748B);
				break;
			case 2:
				this.control_dPad.setStyle(null, 0xFF1E293B, null, 0xFF64748B);
				this.control_joystick.setStyle(null, 0xFF1E293B, null, 0xFF64748B);
				this.control_swipe.setStyle(null, activeColor, null, activeColor);
				break;
		}
	}

	private void setDifficulty(int difficulty){ // [0,3]
		GAME_SETTINGS.gameMode = difficulty;
		cellSlider.setValue(GameDifficulty.values()[difficulty].getCellSize());
		speedSlider.setValue(1/GameDifficulty.values()[difficulty].getSpeed());
		aiMode.setSelected(false);
		wrapping.setSelected(false);
		difficultyButton.setState(difficulty);
		this.gameMode = GameDifficulty.values()[difficulty].getName();
		selectLeadMode(difficulty);
	}

	private void selectLeadMode(int index){
		this.currentLeadMode = index;

		switch (index){
			case 0:
				this.leadEasy.setStyle(null, COLOR_EASY, null, COLOR_EASY);
				this.leadMedium.setStyle(null, 0xFF1E293B, null, 0xFF64748B);
				this.leadHard.setStyle(null, 0xFF1E293B, null, 0xFF64748B);
				this.leadExtreme.setStyle(null, 0xFF1E293B, null, 0xFF64748B);
				break;
			case 1:
				this.leadEasy.setStyle(null, 0xFF1E293B, null, 0xFF64748B);
				this.leadMedium.setStyle(null, COLOR_MEDIUM, null, COLOR_MEDIUM);
				this.leadHard.setStyle(null, 0xFF1E293B, null, 0xFF64748B);
				this.leadExtreme.setStyle(null, 0xFF1E293B, null, 0xFF64748B);
				break;
			case 2:
				this.leadEasy.setStyle(null, 0xFF1E293B, null, 0xFF64748B);
				this.leadMedium.setStyle(null, 0xFF1E293B, null, 0xFF64748B);
				this.leadHard.setStyle(null, COLOR_HARD, null, COLOR_HARD);
				this.leadExtreme.setStyle(null, 0xFF1E293B, null, 0xFF64748B);
				break;
			case 3:
				this.leadEasy.setStyle(null, 0xFF1E293B, null, 0xFF64748B);
				this.leadMedium.setStyle(null, 0xFF1E293B, null, 0xFF64748B);
				this.leadHard.setStyle(null, 0xFF1E293B, null, 0xFF64748B);
				this.leadExtreme.setStyle(null, COLOR_EXTREME, null, COLOR_EXTREME);
				break;
		}
	}

	private void fetchLeaderboards(){
		new Thread(() -> {
			ArrayList<JSONObject> objects = new ArrayList<>();
			objects.add(Account.getLeaderboard("easy"));
			objects.add(Account.getLeaderboard("medium"));
			objects.add(Account.getLeaderboard("hard"));
			objects.add(Account.getLeaderboard("extreme"));

			synchronized (this){
				this.leaderboards.clear();
			}

			try {
				for (JSONObject ob : objects){
					if (ob == null) continue;
					ArrayList<Pair<String, long[]>> scores = new ArrayList<>();
					JSONArray array = ob.getJSONArray("data");
					for (int i = 0; i < array.length(); i++){
						JSONObject pl = (JSONObject) array.get(i);
						long[] arr = new long[pl.getJSONArray("score").length()];
						for (int j = 0; j < arr.length; j++){
							arr[j] = pl.getJSONArray("score").getLong(j);
						}

						scores.add(new Pair<String, long[]>(pl.getString("name"), arr));
					}

					synchronized (this){
						this.leaderboards.add(scores);
					}
				}
			} catch (JSONException ex){
				ex.printStackTrace();
			}

			synchronized (this){
				this.leaderboards.stream().forEach(l -> l.sort((p1, p2) -> {
					long[] l1 = p1.getValue();
					long[] l2 = p2.getValue();
					int len = Math.max(l1.length, l2.length);
					for (int i = 0; i < len; i++){
						long a = i < l1.length ? l1[i] : 0;
						long b = i < l2.length ? l2[i] : 0;
						if (a != b) return Long.compare(b, a);
					}
					return 0;
				}));
			}
		}).start();
	}
}
