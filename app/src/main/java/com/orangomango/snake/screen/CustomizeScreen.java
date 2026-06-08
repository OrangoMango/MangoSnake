package com.orangomango.snake.screen;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.util.ArrayList;
import java.util.HashMap;

import org.json.JSONException;
import org.json.JSONObject;

import com.google.android.gms.games.PlayGames;
import com.orangomango.account.Account;
import com.orangomango.androidbridge.FileHelper;
import com.orangomango.androidbridge.ICanvas;
import com.orangomango.androidbridge.PointerEvent;
import com.orangomango.androidbridge.geometry.Rectangle2D;
import com.orangomango.androidbridge.geometry.TextAlignment;
import com.orangomango.androidbridge.util.Pair;
import com.orangomango.snake.GameView;
import com.orangomango.snake.PlayAchievement;
import com.orangomango.snake.Player;
import com.orangomango.snake.R;
import com.orangomango.snake.game.Apple;
import com.orangomango.snake.game.GameDifficulty;
import com.orangomango.snake.ui.*;

import static com.orangomango.snake.GameView.AUDIO;
import static com.orangomango.snake.GameView.HEIGHT;
import static com.orangomango.snake.GameView.WIDTH;
import static com.orangomango.snake.GameView.SNAKE_IMAGE;
import static com.orangomango.snake.screen.GameScreen.LEADERBOARD_CURRENCY;
import static com.orangomango.snake.game.SnakeBody.PI_SNAKE;

public class CustomizeScreen extends Screen{
	private static int SCREEN_W, SCREEN_H;
	private static Bitmap LOCK_IMAGE;

	private static HashMap<String, Integer> BUTTON_IMAGES = new HashMap<>();

	private final ArrayList<UiElement> uielements = new ArrayList<>();
	private JSONObject jsonData;
	private int snakeSelectedIndex = 0;
	private int appleSelectedIndex = 0;
	private Player player;
	private Button equipSnake, equipApple;
	private ArrayList<ArrayList<Pair<String, long[]>>> leaderboards;
	private int snakeColorsPage = 0, appleColorsPage = 0;
	private Account account;
	private volatile double skinsUnlockedProgress;

	private int TOTAL_SNAKE_COLORS, TOTAL_APPLE_COLORS;

	private final Rectangle2D backButton = new Rectangle2D(0.075, 0.02, 0.18, 0.06);
	private final Rectangle2D screenView = new Rectangle2D(0.075, 0.13, 0.85, 0.80);
	private final Rectangle2D shopButton = new Rectangle2D(0.615, 0.03, 0.155, 0.05);
	private final Rectangle2D preview = new Rectangle2D(this.screenView.getMinX()+this.screenView.getWidth()*0.02, this.screenView.getMinY()+this.screenView.getHeight()*0.05, this.screenView.getWidth()*0.22, this.screenView.getHeight()*0.90);
	private final Rectangle2D snakePicker = new Rectangle2D(this.screenView.getMinX()+this.screenView.getWidth()*0.27, this.screenView.getMinY()+this.screenView.getHeight()*0.05, this.screenView.getWidth()*0.28, this.screenView.getHeight()*0.42);
	private final Rectangle2D applePicker = new Rectangle2D(this.screenView.getMinX()+this.screenView.getWidth()*0.27, this.screenView.getMinY()+this.screenView.getHeight()*0.53, this.screenView.getWidth()*0.28, this.screenView.getHeight()*0.42);
	private final Rectangle2D snakeInfo = new Rectangle2D(this.screenView.getMinX()+this.screenView.getWidth()*0.58, this.screenView.getMinY()+this.screenView.getHeight()*0.05, this.screenView.getWidth()*0.43, this.screenView.getHeight()*0.42);
	private final Rectangle2D appleInfo = new Rectangle2D(this.screenView.getMinX()+this.screenView.getWidth()*0.58, this.screenView.getMinY()+this.screenView.getHeight()*0.53, this.screenView.getWidth()*0.43, this.screenView.getHeight()*0.42);

	static {
		BUTTON_IMAGES.put("button_image_12", R.drawable.button_image_12);
		BUTTON_IMAGES.put("button_image_13", R.drawable.button_image_13);
		BUTTON_IMAGES.put("button_image_14", R.drawable.button_image_14);
		BUTTON_IMAGES.put("button_image_15", R.drawable.button_image_15);
		BUTTON_IMAGES.put("button_image_16", R.drawable.button_image_16);
		BUTTON_IMAGES.put("button_image_17", R.drawable.button_image_17);
		BUTTON_IMAGES.put("button_image_18", R.drawable.button_image_18);
		BUTTON_IMAGES.put("button_image_19", R.drawable.button_image_19);
		BUTTON_IMAGES.put("button_image_20", R.drawable.button_image_20);
		BUTTON_IMAGES.put("button_image_21", R.drawable.button_image_21);
		BUTTON_IMAGES.put("button_image_22", R.drawable.button_image_22);
		BUTTON_IMAGES.put("button_image_23", R.drawable.button_image_23);
		BUTTON_IMAGES.put("button_image_24", R.drawable.button_image_24);
		BUTTON_IMAGES.put("button_image_25", R.drawable.button_image_25);
		BUTTON_IMAGES.put("button_image_26", R.drawable.button_image_26);
		BUTTON_IMAGES.put("button_image_27", R.drawable.button_image_27);
		BUTTON_IMAGES.put("button_image_28", R.drawable.button_image_28);
		BUTTON_IMAGES.put("button_image_29", R.drawable.button_image_29);
	}

	public CustomizeScreen(GameView gameView, Player player, Account account, ArrayList<ArrayList<Pair<String, long[]>>> leads){
		super(gameView);

		String jsonData = FileHelper.readRawResource(this.gameView.getContext(), R.raw.customize);
		try {
			this.jsonData = new JSONObject(jsonData);
			TOTAL_SNAKE_COLORS = this.jsonData.getJSONObject("snake").getJSONObject("colors").length();
			TOTAL_APPLE_COLORS = this.jsonData.getJSONObject("apple").getJSONObject("colors").length();
		} catch (JSONException ex){
			ex.printStackTrace();
		}

		this.player = player;
		this.account = account;
		this.leaderboards = leads;

		// Move to the correct page
		this.snakeColorsPage = this.player.getSnakeIndex() / 6;
		this.appleColorsPage = this.player.getAppleIndex() / 6;

		// Update Google Play Games currency leaderboard
		int totalMangocoins = this.player.getAppData().optInt("totalCurrency");
		if (totalMangocoins > 0){
			PlayGames.getLeaderboardsClient((Activity) this.gameView.getContext()).submitScore(LEADERBOARD_CURRENCY, totalMangocoins);
		}

		calculateSkinsProgress();
		LOCK_IMAGE = BitmapFactory.decodeResource(this.gameView.getContext().getResources(), R.drawable.lock);

		// Show newly unlocked skins
		try {
			int snakeSkins = this.jsonData.getJSONObject("snake").getJSONObject("colors").length();
			int appleSkins = this.jsonData.getJSONObject("apple").getJSONObject("colors").length();

			for (int i = 1; i < snakeSkins; i++){ // We skip default skin
				if (calculateProgress(i+1) >= 1) this.player.updateNotificationDot(i, false);
			}

			for (int i = 1; i < appleSkins; i++){ // We skip default skin
				if (calculateProgress(-(i+1)) >= 1) this.player.updateNotificationDot(-i, false);
			}

			this.player.save();
		} catch (JSONException ex){
			ex.printStackTrace();
		}
	}

	private int calculateSkinsProgress(){
		// Calculate the percentage of unlocked skins
		double count = 0;
		int payonly = 0;
		for (int i = -TOTAL_APPLE_COLORS; i <= TOTAL_SNAKE_COLORS; i++){ // i=0 is useless
			if (i == 15) continue;
			double p = calculateProgress(i);
			if (Double.isInfinite(p)){
				payonly++;
			} else {
				if ((p >= 1 && !isCoinTask(i > 0 ? i-1 : i+1)) || (i > 0 ? this.player.isPermanentSnake(i-1) : this.player.isPermanentApple(-i-1))){
					count++;
				}
			}
		}
		this.skinsUnlockedProgress = count / (TOTAL_SNAKE_COLORS+TOTAL_APPLE_COLORS-payonly-1);
		if (this.skinsUnlockedProgress == 1.0){
			PlayGames.getAchievementsClient((Activity) this.gameView.getContext()).unlock(PlayAchievement.CHAMALEON.getId());
		}

		return TOTAL_SNAKE_COLORS+TOTAL_APPLE_COLORS-payonly-1;
	}

	@Override
	public void goBack(Activity activity){
		HomeScreen hs = new HomeScreen(this.gameView);
		this.gameView.setScreen(hs);
	}

	@Override
	public void handleInput(PointerEvent event){
		this.uielements.stream().filter(el -> el instanceof MouseSensible).forEach(el -> ((MouseSensible)el).onHover(event.x, event.y));

		switch (event.type){
			case PRESSED:
				if (this.backButton.scale(UiElement::rw, UiElement::rh).contains(event.x, event.y)){
					this.gameView.triggerVibration(100);
					AUDIO.playSound("gui");
					goBack(null);
				}

				if ((new Rectangle2D(rsw(this.shopButton.getMinX()), rsh(this.shopButton.getMinY()), rsw(this.shopButton.getWidth()), rsw(this.shopButton.getHeight()))).contains(event.x, event.y)){
					this.gameView.triggerVibration(100);
					AUDIO.playSound("gui");
					ShopScreen ss = new ShopScreen(this.gameView, this.player, this.account, this.leaderboards);
					this.gameView.setScreen(ss);
					return;
				}

				Rectangle2D snakePageBack = new Rectangle2D(this.snakePicker.getMinX(), this.snakePicker.getMinY(), this.snakePicker.getWidth()*0.1, this.snakePicker.getHeight());
				Rectangle2D snakePageForward = new Rectangle2D(this.snakePicker.getMinX()+this.snakePicker.getWidth()*0.9, this.snakePicker.getMinY(), this.snakePicker.getWidth()*0.1, this.snakePicker.getHeight());
				Rectangle2D applePageBack = new Rectangle2D(this.applePicker.getMinX(), this.applePicker.getMinY(), this.applePicker.getWidth()*0.1, this.applePicker.getHeight());
				Rectangle2D applePageForward = new Rectangle2D(this.applePicker.getMinX()+this.applePicker.getWidth()*0.9, this.applePicker.getMinY(), this.applePicker.getWidth()*0.1, this.applePicker.getHeight());

				if (snakePageBack.scale(CustomizeScreen::rsw, CustomizeScreen::rsh).contains(event.x, event.y) && this.snakeColorsPage > 0){
					this.gameView.triggerVibration(100);
					this.snakeColorsPage--;
					selectSnakeColor(this.snakeSelectedIndex);
				} else if (snakePageForward.scale(CustomizeScreen::rsw, CustomizeScreen::rsh).contains(event.x, event.y) && this.snakeColorsPage < TOTAL_SNAKE_COLORS/6-1){
					this.gameView.triggerVibration(100);
					this.snakeColorsPage++;
					selectSnakeColor(this.snakeSelectedIndex);
				} else if (applePageBack.scale(CustomizeScreen::rsw, CustomizeScreen::rsh).contains(event.x, event.y) && this.appleColorsPage > 0){
					this.gameView.triggerVibration(100);
					this.appleColorsPage--;
					selectAppleColor(this.appleSelectedIndex);
				} else if (applePageForward.scale(CustomizeScreen::rsw, CustomizeScreen::rsh).contains(event.x, event.y) && this.appleColorsPage < TOTAL_APPLE_COLORS/6-1){
					this.gameView.triggerVibration(100);
					this.appleColorsPage++;
					selectAppleColor(this.appleSelectedIndex);
				}

				this.uielements.stream().filter(el -> el instanceof MouseSensible).forEach(el -> ((MouseSensible)el).onClick(event.x, event.y));
				break;
			case DRAGGED:
				this.uielements.stream().filter(el -> el instanceof MouseSensible).forEach(el -> ((MouseSensible)el).onDrag(event.x, event.y));
				break;
			case RELEASED:
				this.uielements.stream().filter(el -> el instanceof MouseSensible).forEach(el -> ((MouseSensible)el).onRelease(event.x, event.y));
				break;
		}
	}

	private void selectSnakeColor(int index){
		this.snakeSelectedIndex = index; // Can be 0

		for (int i = 0; i < 12; i += 2){ // 2*3 + 2*3
			((Button) this.uielements.get(i)).setStyle(null, index-6*this.snakeColorsPage == i/2 ? 0xFFFF0000 : 0xFFFFFFFF, null, null);
		}

		if (this.player.isNotificationDot(index)){
			this.player.updateNotificationDot(index, true);
			this.player.save();
		}

		try{
			String purchaseId = this.jsonData.getJSONObject("snake").getJSONObject("colors").getJSONObject(String.valueOf(index)).optString("purchaseId");
			int value = this.jsonData.getJSONObject("snake").getJSONObject("colors").getJSONObject(String.valueOf(index)).optInt("value", -1);

			if (this.player.getSnakeIndex() != index && (this.player.isPermanentSnake(index) || calculateProgress(index + 1) >= 1)){
				final boolean claim = !this.player.isPermanentSnake(index) && isPermanentTask(index);
				if (claim){
					this.equipSnake.setStyle(0xFFFF751A, 0xFFCC5200, "CLAIM SKIN", 0xFFFFFFFF);
				} else if (this.player.isPermanentSnake(index) || value < 0){
					this.equipSnake.setStyle(0xFF10B981, 0xFF059669, "EQUIP SKIN", 0xFFFFFFFF);
				} else {
					this.equipSnake.setStyle(0xFFFF751A, 0xFFCC5200, value+" MangoCoins", 0xFFFFFFFF); // Value must be white here (progress >= 1)
				}
			} else {
				if (this.player.getSnakeIndex() == index){
					this.equipSnake.setStyle(0xFF475569, 0xFF64748B, "EQUIPPED", 0xFFF8FAFC);
				} else if (!purchaseId.isEmpty()){
					this.gameView.getBillingManager().getProductDetails(purchaseId, pd -> {
						this.equipSnake.setStyle(0xFF3366FF, 0xFF002699, pd.getOneTimePurchaseOfferDetails().getFormattedPrice(), 0xFFFFFFFF);
					});
				} else {
					if (value < 0){
						this.equipSnake.setStyle(0xFF475569, 0xFF64748B, "LOCKED", 0xFFF8FAFC);
					} else {
						this.equipSnake.setStyle(0xFF475569, 0xFF64748B, value+" MangoCoins", 0xFFFF4D4D); // Value must be red here (progress < 1)
					}
				}
			}
		} catch (JSONException ex){
			ex.printStackTrace();
		}
	}

	private void selectAppleColor(int index){
		this.appleSelectedIndex = index; // Can be 0

		for (int i = 1; i < 12; i += 2){ // 2*3 + 2*3
			((Button) this.uielements.get(i)).setStyle(null, index-6*this.appleColorsPage == (i-1)/2 ? 0xFFFF0000 : 0xFFFFFFFF, null, null);
		}

		if (this.player.isNotificationDot(-index)){
			this.player.updateNotificationDot(-index, true);
			this.player.save();
		}

		try{
			String purchaseId = this.jsonData.getJSONObject("apple").getJSONObject("colors").getJSONObject(String.valueOf(index)).optString("purchaseId");
			int value = this.jsonData.getJSONObject("apple").getJSONObject("colors").getJSONObject(String.valueOf(index)).optInt("value", -1);

			if (this.player.getAppleIndex() != index && (this.player.isPermanentApple(index) || calculateProgress(-(index + 1)) >= 1)){
				final boolean claim = !this.player.isPermanentApple(index) && isPermanentTask(-index);
				if (claim){
					this.equipApple.setStyle(0xFFFF751A, 0xFFCC5200, "CLAIM MANGO", 0xFFFFFFFF);
				} else if (this.player.isPermanentApple(index) || value < 0){
					this.equipApple.setStyle(0xFF10B981, 0xFF059669, "EQUIP MANGO", 0xFFFFFFFF);
				} else {
					this.equipApple.setStyle(0xFFFF751A, 0xFFCC5200, value+" MangoCoins", 0xFFFFFFFF); // Value must be white here (progress >= 1)
				}
			} else {
				if (this.player.getAppleIndex() == index){
					this.equipApple.setStyle(0xFF475569, 0xFF64748B, "EQUIPPED", 0xFFF8FAFC);
				} else if (!purchaseId.isEmpty()){
					this.gameView.getBillingManager().getProductDetails(purchaseId, pd -> {
						this.equipApple.setStyle(0xFF3366FF, 0xFF002699, pd.getOneTimePurchaseOfferDetails().getFormattedPrice(), 0xFFFFFFFF);
					});
				} else {
					if (value < 0){
						this.equipApple.setStyle(0xFF475569, 0xFF64748B, "LOCKED", 0xFFF8FAFC);
					} else {
						this.equipApple.setStyle(0xFF475569, 0xFF64748B, value+" MangoCoins", 0xFFFF4D4D); // Value must be red here (progress < 1)
					}
				}
			}
		} catch (JSONException ex){
			ex.printStackTrace();
		}
	}

	private boolean isPermanentTask(int id){ // 0 is default skin
		try {
			return this.jsonData.getJSONObject(id > 0 ? "snake" : "apple").getJSONObject("colors").getJSONObject(String.valueOf(Math.abs(id))).optBoolean("permanent");
		} catch (JSONException ex){
			ex.printStackTrace();
		}

		return false;
	}

	public boolean isCoinTask(int id){ // 0 is default skin
		try {
			return this.jsonData.getJSONObject(id > 0 ? "snake" : "apple").getJSONObject("colors").getJSONObject(String.valueOf(Math.abs(id))).optInt("value", -1) >= 0;
		} catch (JSONException ex){
			ex.printStackTrace();
		}

		return false;
	}

	@Override
	public void update(int screenWidth, int screenHeight){
		SCREEN_W = screenWidth;
		SCREEN_H = screenHeight;

		// All relative coordinates are calculated relative to the total SCREEN_W and SCREEN_H instead of WIDTH/HEIGHT.
		if (this.uielements.size() == 0){
			int count = 0;

			for (int i = 0; i < 2; i++){
				for (int j = 0; j < 3; j++){
					final int indexId = count;
					double w = rsw(0.20 * this.snakePicker.getWidth());
					double h = w;
					double px = rsw(this.snakePicker.getMinX() + 0.17 * this.snakePicker.getWidth()) + j * rsw(0.055);
					double py = rsh(this.snakePicker.getMinY() + 0.155 * this.snakePicker.getHeight()) + i * (h + rsh(0.02)) + rsh(0.07 * this.snakePicker.getHeight());
					Button colorButton = new Button(this.gameView, 0, 0xFFFFFFFF, px / WIDTH, py / HEIGHT, w / WIDTH, h / HEIGHT, null, -1, 0, () -> {
						selectSnakeColor(indexId+6*this.snakeColorsPage);
					});
					colorButton.setGlow(true);
					this.uielements.add(colorButton);

					double w2 = rsw(0.20 * this.applePicker.getWidth());
					double h2 = w2;
					double px2 = rsw(this.applePicker.getMinX() + 0.17 * this.applePicker.getWidth()) + j * rsw(0.055);
					double py2 = rsh(this.applePicker.getMinY() + 0.155 * this.applePicker.getHeight()) + i * (h + rsh(0.02)) + rsh(0.07 * this.applePicker.getHeight());
					Button colorButton2 = new Button(this.gameView, 0, 0xFFFFFFFF, px2 / WIDTH, py2 / HEIGHT, w2 / WIDTH, h2 / HEIGHT, null, -1, 0, () -> {
						selectAppleColor(indexId+6*this.appleColorsPage);
					});
					colorButton2.setGlow(true);
					this.uielements.add(colorButton2);

					count++;
				}
			}

			// Add equip buttons
			this.equipSnake = new Button(this.gameView, 0xFF10B981, 0xFF059669, rsw(this.snakeInfo.getMinX()+this.snakeInfo.getWidth()*0.05) / WIDTH, rsh(this.snakeInfo.getMinY()+this.snakeInfo.getHeight()*0.7) / HEIGHT, rsw(this.snakeInfo.getWidth()*0.9) / WIDTH, rsh(this.snakeInfo.getHeight()*0.2) / HEIGHT, "EQUIP SKIN", UiElement.FONT_LARGE, 0xFFFFFFFF, () -> {
				try{
					String purchaseId = this.jsonData.getJSONObject("snake").getJSONObject("colors").getJSONObject(String.valueOf(this.snakeSelectedIndex)).optString("purchaseId");
					double taskProgress = calculateProgress(this.snakeSelectedIndex + 1);
					if (this.snakeSelectedIndex == this.player.getSnakeIndex() || (taskProgress >= 0 && taskProgress < 1 && !this.player.isPermanentSnake(this.snakeSelectedIndex))){
						this.equipSnake.bounce();
					} else if (!purchaseId.isEmpty() && !this.player.isPermanentSnake(this.snakeSelectedIndex)){
						this.gameView.getBillingManager().purchaseProduct(purchaseId, () -> {
							selectSnakeColor(this.snakeSelectedIndex);
							selectAppleColor(this.appleSelectedIndex);
							calculateSkinsProgress();
						});
					} else {
						this.player.updateSnakeIndex(this.snakeSelectedIndex);
						this.player.save();
						selectSnakeColor(this.snakeSelectedIndex);
						if (!this.player.isPermanentSnake(this.snakeSelectedIndex)){
							if (isPermanentTask(this.snakeSelectedIndex) || isCoinTask(this.snakeSelectedIndex)){
								if (isCoinTask(this.snakeSelectedIndex)){
									int value = this.jsonData.getJSONObject("snake").getJSONObject("colors").getJSONObject(String.valueOf(this.snakeSelectedIndex)).getInt("value");
									this.player.getAppData().put("currency", this.player.getAppData().optInt("currency")-value);
								}
								this.player.setPermanentSnakeIndex(this.snakeSelectedIndex);
								this.player.forcePush(this.account);
								calculateSkinsProgress();
							}
						}
					}
				} catch (JSONException ex){
					ex.printStackTrace();
				}
			});
			this.equipApple = new Button(this.gameView, 0xFF10B981, 0xFF059669, rsw(this.appleInfo.getMinX()+this.appleInfo.getWidth()*0.05) / WIDTH, rsh(this.appleInfo.getMinY()+this.appleInfo.getHeight()*0.7) / HEIGHT, rsw(this.appleInfo.getWidth()*0.9) / WIDTH, rsh(this.appleInfo.getHeight()*0.2) / HEIGHT, "EQUIP MANGO", UiElement.FONT_LARGE, 0xFFFFFFFF, () -> {
				try{
					String purchaseId = this.jsonData.getJSONObject("apple").getJSONObject("colors").getJSONObject(String.valueOf(this.appleSelectedIndex)).optString("purchaseId");
					double taskProgress = calculateProgress(-(this.appleSelectedIndex + 1));
					if (this.appleSelectedIndex == this.player.getAppleIndex() || (taskProgress >= 0 && taskProgress < 1 && !this.player.isPermanentApple(this.appleSelectedIndex))){
						this.equipApple.bounce();
					} else if (!purchaseId.isEmpty() && !this.player.isPermanentApple(this.appleSelectedIndex)){
						this.gameView.getBillingManager().purchaseProduct(purchaseId, () -> {
							selectSnakeColor(this.snakeSelectedIndex);
							selectAppleColor(this.appleSelectedIndex);
							calculateSkinsProgress();
						});
					} else {
						this.player.updateAppleIndex(this.appleSelectedIndex);
						this.player.save();
						selectAppleColor(this.appleSelectedIndex);
						if (!this.player.isPermanentApple(this.appleSelectedIndex)){
							if (isPermanentTask(-this.appleSelectedIndex) || isCoinTask(-this.appleSelectedIndex)){
								if (isCoinTask(-this.appleSelectedIndex)){
									int value = this.jsonData.getJSONObject("apple").getJSONObject("colors").getJSONObject(String.valueOf(this.appleSelectedIndex)).getInt("value");
									this.player.getAppData().put("currency", this.player.getAppData().optInt("currency")-value);
								}
								this.player.setPermanentAppleIndex(this.appleSelectedIndex);
								this.player.forcePush(this.account);
								calculateSkinsProgress();
							}
						}
					}
				} catch (JSONException ex){
					ex.printStackTrace();
				}
			});

			Button statsButton = new Button(this.gameView, 0xFF4CC9F0, 0xFF0C6F8D, rsw(0.79) / WIDTH, rsh(0.03) / HEIGHT, rsw(0.05) / WIDTH, rsw(0.05) / HEIGHT, null, -1, 0, () -> {
				StatsScreen ss = new StatsScreen(this.gameView, this.player, this.account, this.leaderboards, this.skinsUnlockedProgress, calculateSkinsProgress());
				this.gameView.setScreen(ss);
			});
			statsButton.setGlow(true);
			statsButton.setBitmap(BitmapFactory.decodeResource(this.gameView.getContext().getResources(), R.drawable.stats));

			Button googlePlayGamesButton = new Button(this.gameView, 0xFF80FF80, 0xFF006345, rsw(0.85) / WIDTH, rsh(0.03) / HEIGHT, rsw(0.05) / WIDTH, rsw(0.05) / HEIGHT, null, -1, 0, () -> {
				PlayGames.getAchievementsClient((Activity) this.gameView.getContext())
						.getAchievementsIntent()
						.addOnSuccessListener(intent -> {
							((Activity) this.gameView.getContext()).startActivityForResult(intent, 9003); // Some arbitrary code for the intent
						});
			});
			googlePlayGamesButton.setGlow(true);
			googlePlayGamesButton.setBitmap(BitmapFactory.decodeResource(this.gameView.getContext().getResources(), R.drawable.gpg));

			selectSnakeColor(this.player.getSnakeIndex());
			selectAppleColor(this.player.getAppleIndex());

			this.uielements.add(this.equipSnake);
			this.uielements.add(this.equipApple);
			this.uielements.add(statsButton);
			this.uielements.add(googlePlayGamesButton);
		} else {
			for (int i = 0; i < 12; i++){
				Button button = (Button) this.uielements.get(i);
				try {
					if (i % 2 == 0){
						JSONObject snakeData = this.jsonData.getJSONObject("snake").getJSONObject("colors").getJSONObject(String.valueOf(i/2+this.snakeColorsPage*6));
						if ((calculateProgress(i/2 + 1 + 6*this.snakeColorsPage) < 1 || isCoinTask(i/2+this.snakeColorsPage*6)) && !this.player.isPermanentSnake(i/2+this.snakeColorsPage*6)){
							button.setBitmap(LOCK_IMAGE);
						} else {
							button.setBitmap(null);
						}
						button.setStyle(Integer.parseUnsignedInt(snakeData.getString("color"), 16), null, null, null);

						if (snakeData.optBoolean("button_image")){
							button.setOverlayImage(BitmapFactory.decodeResource(this.gameView.getContext().getResources(), BUTTON_IMAGES.get("button_image_" + (i/2 + 6*this.snakeColorsPage))));
						} else {
							button.setOverlayImage(null);
						}
					} else {
						JSONObject appleData = this.jsonData.getJSONObject("apple").getJSONObject("colors").getJSONObject(String.valueOf((i-1)/2+this.appleColorsPage*6));
						if ((calculateProgress(-(i/2 + 1 + 6*this.appleColorsPage)) < 1 || isCoinTask(-((i-1)/2+this.appleColorsPage*6))) && !this.player.isPermanentApple((i-1)/2+this.appleColorsPage*6)){
							button.setBitmap(LOCK_IMAGE);
						} else {
							button.setBitmap(null);
						}
						button.setStyle(Integer.parseUnsignedInt(appleData.getString("color"), 16), null, null, null);

						if (appleData.optBoolean("button_image")){
							button.setOverlayImage(BitmapFactory.decodeResource(this.gameView.getContext().getResources(), BUTTON_IMAGES.get("button_image_" + ((i-1)/2 + 6*this.appleColorsPage))));
						} else {
							button.setOverlayImage(null);
						}
					}
				} catch (JSONException ex){
					ex.printStackTrace();
				}
			}
		}
	}

	@Override
	public void render(ICanvas canvas){
		canvas.clear(0xFF020617);

		// Render back button
		canvas.setEffect(10, 0xFFF472B6);
		canvas.fillText("< Back To Home", rsw(this.backButton.getMinX()), rsh(this.backButton.getMaxY()-this.backButton.getHeight()*0.2), 0xFFF472B6, UiElement.FONT_LARGE, TextAlignment.LEFT);
		canvas.clearEffect();

		// Preview area
		canvas.fillRoundRect(rsw(this.preview.getMinX()), rsh(this.preview.getMinY()), rsw(this.preview.getWidth()), rsh(this.preview.getHeight()), rsw(0.02), rsh(0.04), 0xFF0F172A);
		canvas.strokeRoundRect(rsw(this.preview.getMinX()), rsh(this.preview.getMinY()), rsw(this.preview.getWidth()), rsh(this.preview.getHeight()), rsw(0.02), rsh(0.04), 0xFF7D98CF, rsh(0.002));

		// Control area
		canvas.fillRoundRect(rsw(this.snakePicker.getMinX()), rsh(this.snakePicker.getMinY()), rsw(this.snakePicker.getWidth()), rsh(this.snakePicker.getHeight()), rsw(0.02), rsh(0.04), 0xFF0F172A);
		canvas.strokeRoundRect(rsw(this.snakePicker.getMinX()), rsh(this.snakePicker.getMinY()), rsw(this.snakePicker.getWidth()), rsh(this.snakePicker.getHeight()), rsw(0.02), rsh(0.04), 0xFF7D98CF, rsh(0.002));
		canvas.fillText(String.format("Snake (%d/%d)", this.snakeColorsPage+1, TOTAL_SNAKE_COLORS/6), rsw(this.snakePicker.getMinX()+this.snakePicker.getWidth()*0.5), rsh(this.snakePicker.getMinY()+this.snakePicker.getHeight()*0.135), 0xFF94F7D4, UiElement.FONT_MEDIUM, TextAlignment.CENTER);
		canvas.fillRoundRect(rsw(this.applePicker.getMinX()), rsh(this.applePicker.getMinY()), rsw(this.applePicker.getWidth()), rsh(this.applePicker.getHeight()), rsw(0.02), rsh(0.04), 0xFF0F172A);
		canvas.strokeRoundRect(rsw(this.applePicker.getMinX()), rsh(this.applePicker.getMinY()), rsw(this.applePicker.getWidth()), rsh(this.applePicker.getHeight()), rsw(0.02), rsh(0.04), 0xFF7D98CF, rsh(0.002));
		canvas.fillText(String.format("Mango (%d/%d)", this.appleColorsPage+1, TOTAL_APPLE_COLORS/6), rsw(this.applePicker.getMinX()+this.applePicker.getWidth()*0.5), rsh(this.applePicker.getMinY()+this.applePicker.getHeight()*0.135), 0xFF94F7D4, UiElement.FONT_MEDIUM, TextAlignment.CENTER);

		// Snake
		canvas.fillRoundRect(rsw(this.snakeInfo.getMinX()), rsh(this.snakeInfo.getMinY()), rsw(this.snakeInfo.getWidth()), rsh(this.snakeInfo.getHeight()), rsw(0.02), rsh(0.04), 0xFF0F172A);
		canvas.strokeRoundRect(rsw(this.snakeInfo.getMinX()), rsh(this.snakeInfo.getMinY()), rsw(this.snakeInfo.getWidth()), rsh(this.snakeInfo.getHeight()), rsw(0.02), rsh(0.04), 0xFF7D98CF, rsh(0.002));
		final double snakeBarWidth = rsw(this.snakeInfo.getWidth()*0.65);
		final double snakeProgress = calculateProgress(this.snakeSelectedIndex+1);
		if (Double.isFinite(snakeProgress)){
			if (snakeProgress < 1 && !this.player.isPermanentSnake(this.snakeSelectedIndex)){
				canvas.fillRoundRect(rsw(this.snakeInfo.getMinX() + this.snakeInfo.getWidth() * 0.1), rsh(this.snakeInfo.getMinY() + this.snakeInfo.getHeight() * 0.47), snakeBarWidth, rsh(this.snakeInfo.getHeight() * 0.1), rsw(0.02), rsh(0.04), 0xFF0C6F8D);
				canvas.setEffect(15, 0xFF4CC9F0);
				canvas.fillRoundRect(rsw(this.snakeInfo.getMinX() + this.snakeInfo.getWidth() * 0.1), rsh(this.snakeInfo.getMinY() + this.snakeInfo.getHeight() * 0.47), snakeBarWidth * Math.max(0, Math.min(1, snakeProgress)), rsh(this.snakeInfo.getHeight() * 0.1), rsw(0.02), rsh(0.04), 0xFF4CC9F0);
				canvas.fillText(String.format("%.0f%%", Math.max(0, Math.min(1, snakeProgress)) * 100), rsw(this.snakeInfo.getMinX() + this.snakeInfo.getWidth() * 0.9), rsh(this.snakeInfo.getMinY() + this.snakeInfo.getHeight() * 0.54), 0xFF94F7D4, UiElement.FONT_MEDIUM, TextAlignment.RIGHT);
				canvas.clearEffect();
			} else {
				canvas.fillText("Task Completed!", rsw(this.snakeInfo.getMinX() + this.snakeInfo.getWidth() * 0.5), rsh(this.snakeInfo.getMinY() + this.snakeInfo.getHeight() * 0.53), 0xFF00E5FF, UiElement.FONT_MEDIUM, TextAlignment.CENTER);
			}
		}
		canvas.fillText(String.format("ID: %d", this.snakeSelectedIndex), rsw(this.snakeInfo.getMaxX() - this.snakeInfo.getWidth() * 0.05), rsh(this.snakeInfo.getMinY() + this.snakeInfo.getHeight() * 0.1), 0xFF64748B, UiElement.FONT_SMALL*0.85, TextAlignment.RIGHT);

		// Apple
		canvas.fillRoundRect(rsw(this.appleInfo.getMinX()), rsh(this.appleInfo.getMinY()), rsw(this.appleInfo.getWidth()), rsh(this.appleInfo.getHeight()), rsw(0.02), rsh(0.04), 0xFF0F172A);
		canvas.strokeRoundRect(rsw(this.appleInfo.getMinX()), rsh(this.appleInfo.getMinY()), rsw(this.appleInfo.getWidth()), rsh(this.appleInfo.getHeight()), rsw(0.02), rsh(0.04), 0xFF7D98CF, rsh(0.002));
		final double appleBarWidth = rsw(this.appleInfo.getWidth()*0.65);
		final double appleProgress = calculateProgress(-(this.appleSelectedIndex+1));
		if (Double.isFinite(appleProgress)){
			if (appleProgress < 1 && !this.player.isPermanentApple(this.appleSelectedIndex)){
				canvas.fillRoundRect(rsw(this.appleInfo.getMinX() + this.appleInfo.getWidth() * 0.1), rsh(this.appleInfo.getMinY() + this.appleInfo.getHeight() * 0.47), appleBarWidth, rsh(this.appleInfo.getHeight() * 0.1), rsw(0.02), rsh(0.04), 0xFF0C6F8D);
				canvas.setEffect(15, 0xFF4CC9F0);
				canvas.fillRoundRect(rsw(this.appleInfo.getMinX() + this.appleInfo.getWidth() * 0.1), rsh(this.appleInfo.getMinY() + this.appleInfo.getHeight() * 0.47), appleBarWidth * Math.max(0, Math.min(1, appleProgress)), rsh(this.appleInfo.getHeight() * 0.1), rsw(0.02), rsh(0.04), 0xFF4CC9F0);
				canvas.fillText(String.format("%.0f%%", Math.max(0, Math.min(1, appleProgress)) * 100), rsw(this.appleInfo.getMinX() + this.appleInfo.getWidth() * 0.9), rsh(this.appleInfo.getMinY() + this.appleInfo.getHeight() * 0.54), 0xFF94F7D4, UiElement.FONT_MEDIUM, TextAlignment.RIGHT);
				canvas.clearEffect();
			} else {
				canvas.fillText("Task Completed!", rsw(this.appleInfo.getMinX() + this.appleInfo.getWidth() * 0.5), rsh(this.appleInfo.getMinY() + this.appleInfo.getHeight() * 0.53), 0xFF00E5FF, UiElement.FONT_MEDIUM, TextAlignment.CENTER);
			}
		}
		canvas.fillText(String.format("ID: %d", this.appleSelectedIndex), rsw(this.appleInfo.getMaxX() - this.appleInfo.getWidth() * 0.05), rsh(this.appleInfo.getMinY() + this.appleInfo.getHeight() * 0.1), 0xFF64748B, UiElement.FONT_SMALL*0.85, TextAlignment.RIGHT);

		// Render page indicators
		if (this.snakeColorsPage > 0) canvas.fillText("<", rsw(this.snakePicker.getMinX()+0.08*this.snakePicker.getWidth()), rsh(this.snakePicker.getMinY()+0.57*this.snakePicker.getHeight()), 0xFF94F7D4, UiElement.FONT_EXTRALARGE*0.8, TextAlignment.CENTER);
		if (this.snakeColorsPage < TOTAL_SNAKE_COLORS/6-1) canvas.fillText(">", rsw(this.snakePicker.getMinX()+0.92*this.snakePicker.getWidth()), rsh(this.snakePicker.getMinY()+0.57*this.snakePicker.getHeight()), 0xFF94F7D4, UiElement.FONT_EXTRALARGE*0.8, TextAlignment.CENTER);
		if (this.appleColorsPage > 0) canvas.fillText("<", rsw(this.applePicker.getMinX()+0.08*this.applePicker.getWidth()), rsh(this.applePicker.getMinY()+0.57*this.applePicker.getHeight()), 0xFF94F7D4, UiElement.FONT_EXTRALARGE*0.8, TextAlignment.CENTER);
		if (this.appleColorsPage < TOTAL_APPLE_COLORS/6-1) canvas.fillText(">", rsw(this.applePicker.getMinX()+0.92*this.applePicker.getWidth()), rsh(this.applePicker.getMinY()+0.57*this.applePicker.getHeight()), 0xFF94F7D4, UiElement.FONT_EXTRALARGE*0.8, TextAlignment.CENTER);

		try{
			final String prefix1 = "Snake | ";
			final String prefix2 = "Mango | ";
			canvas.setEffect(7.5, 0xFF00FF00);
			canvas.fillText(prefix1, rsw(this.snakeInfo.getMinX() + 0.05 * this.snakeInfo.getWidth()), rsh(this.snakeInfo.getMinY() + 0.2 * this.snakeInfo.getHeight()), 0xFF00FF00, UiElement.FONT_LARGE, TextAlignment.LEFT);
			canvas.setEffect(7.5, 0xFFFF8533);
			canvas.fillText(prefix2, rsw(this.appleInfo.getMinX() + 0.05 * this.appleInfo.getWidth()), rsh(this.appleInfo.getMinY() + 0.2 * this.appleInfo.getHeight()), 0xFFFF8533, UiElement.FONT_LARGE, TextAlignment.LEFT);
			canvas.setEffect(7.5, 0xFFF8FAFC);
			canvas.fillText(this.jsonData.getJSONObject("snake").getJSONObject("colors").getJSONObject(String.valueOf(this.snakeSelectedIndex)).getString("name"), canvas.measureText(prefix1, UiElement.FONT_LARGE) + rsw(this.snakeInfo.getMinX() + 0.05 * this.snakeInfo.getWidth()), rsh(this.snakeInfo.getMinY() + 0.2 * this.snakeInfo.getHeight()), 0xFFF8FAFC, UiElement.FONT_LARGE, TextAlignment.LEFT);
			canvas.fillText(this.jsonData.getJSONObject("apple").getJSONObject("colors").getJSONObject(String.valueOf(this.appleSelectedIndex)).getString("name"), canvas.measureText(prefix2, UiElement.FONT_LARGE) + rsw(this.appleInfo.getMinX() + 0.05 * this.appleInfo.getWidth()), rsh(this.appleInfo.getMinY() + 0.2 * this.appleInfo.getHeight()), 0xFFF8FAFC, UiElement.FONT_LARGE, TextAlignment.LEFT);
			canvas.setEffect(7.5, 0xFF64748B);

			final double textSize1 = calculateTextSize(canvas, this.jsonData.getJSONObject("snake").getJSONObject("colors").getJSONObject(String.valueOf(this.snakeSelectedIndex)).getString("message"), rsw(this.snakeInfo.getWidth())*0.85);
			final double textSize2 = calculateTextSize(canvas, this.jsonData.getJSONObject("apple").getJSONObject("colors").getJSONObject(String.valueOf(this.appleSelectedIndex)).getString("message"), rsw(this.appleInfo.getWidth())*0.85);

			canvas.fillText(this.jsonData.getJSONObject("snake").getJSONObject("colors").getJSONObject(String.valueOf(this.snakeSelectedIndex)).getString("message"), rsw(this.snakeInfo.getMinX() + 0.05 * this.snakeInfo.getWidth()), rsh(this.snakeInfo.getMinY() + 0.345 * this.snakeInfo.getHeight()), 0xFF64748B, textSize1, TextAlignment.LEFT);
			canvas.fillText(this.jsonData.getJSONObject("apple").getJSONObject("colors").getJSONObject(String.valueOf(this.appleSelectedIndex)).getString("message"), rsw(this.appleInfo.getMinX() + 0.05 * this.appleInfo.getWidth()), rsh(this.appleInfo.getMinY() + 0.345 * this.appleInfo.getHeight()), 0xFF64748B, textSize2, TextAlignment.LEFT);
			canvas.clearEffect();
		} catch (JSONException ex){
			ex.printStackTrace();
		}

		for (int i = 0; i < this.uielements.size(); i++){
			UiElement element = this.uielements.get(i);
			element.render(canvas);

			if (i < 12){
				final int taskId = i % 2 == 0 ? i/2+this.snakeColorsPage*6 : -((i-1)/2+this.appleColorsPage*6);

				boolean claim;
				if (i % 2 == 0){ // Snake
					claim = !this.player.isPermanentSnake(taskId) && (isCoinTask(taskId) || isPermanentTask(taskId)) && calculateProgress(taskId + 1) >= 1;
				} else { // Apple
					claim = !this.player.isPermanentApple(-taskId) && (isCoinTask(taskId) || isPermanentTask(taskId)) && calculateProgress(taskId - 1) >= 1;
				}

				try {
					String purchaseId = this.jsonData.getJSONObject(i % 2 == 0 ? "snake" : "apple").getJSONObject("colors").getJSONObject(String.valueOf(Math.abs(taskId))).optString("purchaseId");

					if (claim || (!isCoinTask(taskId) && purchaseId.isEmpty() && this.player.isNotificationDot(i % 2 == 0 ? i/2+this.snakeColorsPage*6 : -((i-1)/2+this.appleColorsPage*6)))){
						Rectangle2D bounds = element.getBounds();
						final double ballRadius = bounds.getWidth()*0.2;
						canvas.fillOval(UiElement.rw(bounds.getMaxX()-ballRadius*1.3), UiElement.rh(bounds.getMinY())-UiElement.rw(ballRadius*0.7), UiElement.rw(ballRadius*2), UiElement.rw(ballRadius*2), claim ? 0xFFFF751A : 0xFF0000FF);
					}
				} catch (JSONException ex){
					ex.printStackTrace();
				}
			}
		}

		// Render preview snake
		canvas.fillText("Preview", rsw(this.preview.getMinX()+this.preview.getWidth()*0.5), rsh(this.preview.getMinY()+this.preview.getHeight()*0.08), 0xFF94F7D4, UiElement.FONT_MEDIUM, TextAlignment.CENTER);
		final double startX = rsw(this.preview.getMinX()+this.preview.getWidth()*0.15);
		final double startY = rsh(this.preview.getMinY()+this.preview.getHeight()*0.25);
		final int size = (int) rsw(0.2*this.preview.getWidth());

		for (int i = 0; i < 2; i++){
			if (this.snakeSelectedIndex == PI_SNAKE){ // PI-snake
				drawSnakeBody(canvas, startX, startY, size, 1, 0, 0, 4, 0, i == 0);
				drawSnakeBody(canvas, startX, startY, size, 0, 0, 1, 1, 180, i == 0);
				drawSnakeBody(canvas, startX, startY, size, 0, 1, 4, 0, 270, i == 0);
				drawSnakeBody(canvas, startX, startY, size, 0, 2, 1, 3,90, i == 0);
			} else {
				drawSnakeBody(canvas, startX, startY, size, 1, 0, 4, 0, 0, i == 0);
				drawSnakeBody(canvas, startX, startY, size, 0, 0, 1, 0, 180, i == 0);
				drawSnakeBody(canvas, startX, startY, size, 0, 1, 0, 0, 270, i == 0);
				drawSnakeBody(canvas, startX, startY, size, 0, 2, 3, 0,90, i == 0);
			}
		}

		// Render preview apple
		final double centerX = startX + size * 3;
		final double centerY = startY + size * 2;
		Apple.renderApple(canvas, centerX, centerY, size, this.player.getAppleColor(this.appleSelectedIndex), this.player.getAppleInternalColor(this.appleSelectedIndex));

		canvas.setEffect(10, 0xFF00BAFF);
		canvas.fillRoundRect(rsw(this.preview.getMinX()+0.1*this.preview.getWidth()), rsh(this.preview.getMinY()+0.9*this.preview.getHeight()), rsw(this.preview.getWidth()*0.8) * this.skinsUnlockedProgress, rsh(this.preview.getHeight()*0.05), rsw(0.02), rsh(0.04), 0xFF00BAFF);
		canvas.strokeRoundRect(rsw(this.preview.getMinX()+0.1*this.preview.getWidth()), rsh(this.preview.getMinY()+0.9*this.preview.getHeight()), rsw(this.preview.getWidth()*0.8), rsh(this.preview.getHeight()*0.05), rsw(0.02), rsh(0.04), 0xFF00E5FF, rsh(0.004));
		canvas.clearEffect();

		canvas.fillText("Total Progess", rsw(this.preview.getMinX()+0.1*this.preview.getWidth()), rsh(this.preview.getMinY()+0.87*this.preview.getHeight()), 0xFF80DEEA, UiElement.FONT_MEDIUM*0.8, TextAlignment.LEFT);
		canvas.fillText(String.format("%.0f%%", this.skinsUnlockedProgress * 100), rsw(this.preview.getMinX()+0.9*this.preview.getWidth()), rsh(this.preview.getMinY()+0.87*this.preview.getHeight()), 0xFFE0F7FA , UiElement.FONT_MEDIUM*0.8, TextAlignment.RIGHT);

		// Render currency indicator
		canvas.fillRoundRect(rsw(this.shopButton.getMinX()), rsh(this.shopButton.getMinY()), rsw(this.shopButton.getWidth()), rsw(this.shopButton.getHeight()), rsw(0.02), rsh(0.04), 0x33FFAB00);
		canvas.strokeRoundRect(rsw(this.shopButton.getMinX()), rsh(this.shopButton.getMinY()), rsw(this.shopButton.getWidth()), rsw(this.shopButton.getHeight()), rsw(0.02), rsh(0.04), 0xFFFFD600, rsh(0.002));
		Apple.renderApple(canvas, rsw(0.638), rsh(this.shopButton.getMinY())+rsw(this.shopButton.getHeight())*0.5, (int)rsw(0.035), this.player.getAppleColor(this.player.getAppleIndex()), this.player.getAppleInternalColor(this.player.getAppleIndex()));
		canvas.fillText("SHOP", rsw(this.shopButton.getMinX())+rsw(this.shopButton.getWidth())*0.5, rsh(this.shopButton.getMinY())+rsw(this.shopButton.getHeight())*0.235, 0xFFFFAB00, UiElement.FONT_SMALL, TextAlignment.CENTER);
		canvas.setEffect(10, 0xFFE0F7FA);
		canvas.fillText(Integer.toString(this.player.getAppData().optInt("currency", 0)), rsw(0.76), rsh(this.shopButton.getMinY())+rsw(this.shopButton.getHeight())*0.7, 0xFFE0F7FA, UiElement.FONT_LARGELARGE, TextAlignment.RIGHT);
		canvas.clearEffect();
	}

	private static double calculateTextSize(ICanvas canvas, String text, double width){
		double baseSize = UiElement.FONT_MEDIUM;
		double measuredWidth = canvas.measureText(text.split("\n")[0], baseSize);

		if (measuredWidth > width) {
			return (width / measuredWidth) * baseSize;
		}

		return baseSize;
	}

	private void drawSnakeBody(ICanvas canvas, double startX, double startY, int size, int x, int y, int frameIndexX, int frameIndexY, int rotation, boolean effect){
		canvas.save();
		canvas.translate(startX+size/2.0 + x*(size-3), startY+size/2.0 + y*(size-3));
		canvas.rotate(rotation);

		if (effect){
			canvas.setEffect(size, this.player.getSnakeColor(this.snakeSelectedIndex));
			canvas.fillRect(-size*0.3, -size*0.3, size*0.6, size*0.6, this.player.getSnakeColor(this.snakeSelectedIndex));
			canvas.clearEffect();
		}

		canvas.drawImage(SNAKE_IMAGE[this.snakeSelectedIndex], 1+34*frameIndexX, 1+34*frameIndexY, 32, 32, -size/2.0, -size/2.0, size, size);
		canvas.restore();
	}

	// Snake tasks: 1, 2, 3, ...
	// Apple tasks: -1, -2, -3, ...
	private double calculateProgress(int taskId){ // 0 is excluded here, Infinite for pay-only tasks
		if (taskId == 0) return 0;

		if (taskId == 1 || taskId == -1){
			return 1.0;
		} else {
			final int highEasy = this.player.getAppData().optInt("high_easy", 0);
			final int highMedium = this.player.getAppData().optInt("high_medium", 0);
			final int highHard = this.player.getAppData().optInt("high_hard", 0);
			final int highExtreme = this.player.getAppData().optInt("high_extreme", 0);
			final int roundsPlayed = this.player.getAppData().optInt("rounds", 0);
			final int mangoes = this.player.getAppData().optInt("mangoes", 0);
			final int timePlayed = this.player.getAppData().optInt("timePlayed", 0);

			int[] easySize = GameDifficulty.calculateGridSize(GameDifficulty.EASY.getCellSize());
			int[] mediumSize = GameDifficulty.calculateGridSize(GameDifficulty.MEDIUM.getCellSize());
			int[] hardSize = GameDifficulty.calculateGridSize(GameDifficulty.HARD.getCellSize());
			int[] extremeSize = GameDifficulty.calculateGridSize(GameDifficulty.EXTREME.getCellSize());

			switch (taskId){
				case 2:
					return highEasy / 40.0;
				case 3:
					return highExtreme / 20.0;
				case 4:
					return highHard / 30.0;
				case 5:
					return highMedium / 100.0;
				case 6:
					return mangoes / 200.0;
				case 7:
					return this.account != null ? 1.0 : 0.0;
				case 8:
					return highMedium / 150.0;
				case 10:
					return roundsPlayed / 200.0;
				case 12:
					return mangoes / 4000.0;
				case 14:
					return highExtreme / 30.0;
				case 15:
					return this.skinsUnlockedProgress;
				case 16:
					double easyProgress = (highEasy+3.0) / (easySize[0]*easySize[1]);
					double mediumProgress = (highMedium+3.0) / (mediumSize[0]*mediumSize[1]);
					double hardProgress = (highHard+3.0) / (hardSize[0]*hardSize[1]);
					double extremeProgress = (highEasy+3.0) / (extremeSize[0]*extremeSize[1]);
					return  Math.max(Math.max(easyProgress, mediumProgress), Math.max(hardProgress, extremeProgress));
				case 19:
					return this.player.getAppData().optInt("longestGameTime_easy", 0) / 480.0; // 8 min
				case 20:
					return this.player.getAppData().optInt("longestGameTime_medium", 0) / 360.0; // 6 min
				case 21:
					return this.player.getAppData().optInt("longestGameTime_hard", 0) / 210.0; // 3.5 min
				case 22:
					return this.player.getAppData().optInt("longestGameTime_extreme", 0) / 90.0; // 1.5 min
				case 25:
					return timePlayed / 3600.0; // 1h
				case 26:
					return timePlayed / 7200.0; // 2h


				case -2:
					return roundsPlayed / 100.0;
				case -3:
					return highEasy / 70.0;
				case -4:
					return Math.min(Math.min(getBestRank(0), getBestRank(1)), Math.min(getBestRank(2), getBestRank(3))) <= 5 ? 1.0 : 0.0;
				case -5:
					return mangoes / 500.0;
				case -6:
					return Math.min(Math.min(getBestRank(0), getBestRank(1)), Math.min(getBestRank(2), getBestRank(3))) <= 3 ? 1.0 : 0.0;
				case -7:
					return roundsPlayed / 400.0;
				case -11:
					return Math.min(Math.min(getBestRank(0), getBestRank(1)), Math.min(getBestRank(2), getBestRank(3))) <= 2 ? 1.0 : 0.0;
				case -15:
					return mangoes / 1000.0;
				case -17:
					return highExtreme / 10.0;
				case -19:
					return highHard / 60.0;
				case -20:
					return mangoes / 2000.0;
				case -21:
					return highEasy / 110.0;

			}
		}

		// MangoCoins tasks
		if (isCoinTask(taskId > 0 ? taskId-1 : taskId+1)){
			try{
				int value = this.jsonData.getJSONObject(taskId > 0 ? "snake" : "apple").getJSONObject("colors").getJSONObject(String.valueOf(Math.abs(taskId)-1)).getInt("value");
				return (double) this.player.getAppData().optInt("currency") / value;
			} catch (JSONException ex){
				ex.printStackTrace();
			}
		}

		// Pay-only tasks
		try {
			String purchaseId = this.jsonData.getJSONObject(taskId > 0 ? "snake" : "apple").getJSONObject("colors").getJSONObject(String.valueOf(Math.abs(taskId)-1)).optString("purchaseId");
			if (!purchaseId.isEmpty()){
				return Double.NEGATIVE_INFINITY;
			}
		} catch (JSONException ex){
			ex.printStackTrace();
		}

		return 0.0;
	}

	private int getBestRank(int lead){
		if (lead >= this.leaderboards.size()) return Integer.MAX_VALUE;

		int count = 1;
		for (int i = 0; i < this.leaderboards.get(lead).size(); i++){
			if (this.leaderboards.get(lead).get(i).getKey().equals(this.account == null ? null : this.account.getUsername())){
				return count;
			}
			count++;
		}

		return Integer.MAX_VALUE;
	}

	private static double rsw(double x){
		return x * SCREEN_W;
	}

	private static double rsh(double y){
		return y * SCREEN_H;
	}
}
