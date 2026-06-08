package com.orangomango.snake.screen;

import static com.orangomango.snake.GameView.AUDIO;
import static com.orangomango.snake.GameView.WIDTH;
import static com.orangomango.snake.GameView.HEIGHT;
import static com.orangomango.snake.screen.HomeScreen.PURCHASE_HANDLER;
import static com.orangomango.snake.screen.HomeScreen.PurchaseHandler;
import static com.orangomango.snake.game.SnakeBody.SKIN_LAUNCH_ID;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.orangomango.account.Account;
import com.orangomango.androidbridge.ICanvas;
import com.orangomango.androidbridge.PointerEvent;
import com.orangomango.androidbridge.geometry.Rectangle2D;
import com.orangomango.androidbridge.geometry.TextAlignment;
import com.orangomango.androidbridge.util.Pair;
import com.orangomango.snake.GameView;
import com.orangomango.snake.Player;
import com.orangomango.snake.R;
import com.orangomango.snake.ui.Button;
import com.orangomango.snake.ui.MouseSensible;
import com.orangomango.snake.ui.UiElement;

import org.json.JSONException;

import java.util.ArrayList;

public class ShopScreen extends Screen{
	private static int SCREEN_W, SCREEN_H;

	private Account account;
	private ArrayList<ArrayList<Pair<String, long[]>>> leaderboards;

	private final ArrayList<UiElement> uielements = new ArrayList<>();
	private Player player;
	private boolean owned1, owned3;
	private Button button2, button4, button5;
	private Bitmap SHOP1, SHOP2, SHOP3, SHOP4;

	private final Rectangle2D backButton = new Rectangle2D(0.075, 0.02, 0.18, 0.06);
	private final Rectangle2D screenView = new Rectangle2D(0.075, 0.13, 0.85, 0.80);
	private final Rectangle2D container1 = new Rectangle2D(this.screenView.getMinX()+this.screenView.getWidth()*0.01, this.screenView.getMinY()+this.screenView.getHeight()*0.05, this.screenView.getWidth()*0.22, this.screenView.getHeight()*0.90);
	private final Rectangle2D container2 = new Rectangle2D(this.screenView.getMinX()+this.screenView.getWidth()*0.27, this.screenView.getMinY()+this.screenView.getHeight()*0.05, this.screenView.getWidth()*0.22, this.screenView.getHeight()*0.90);
	private final Rectangle2D container3 = new Rectangle2D(this.screenView.getMinX()+this.screenView.getWidth()*0.53, this.screenView.getMinY()+this.screenView.getHeight()*0.05, this.screenView.getWidth()*0.22, this.screenView.getHeight()*0.90);
	private final Rectangle2D container4 = new Rectangle2D(this.screenView.getMinX()+this.screenView.getWidth()*0.79, this.screenView.getMinY()+this.screenView.getHeight()*0.05, this.screenView.getWidth()*0.22, this.screenView.getHeight()*0.90);

	public ShopScreen(GameView gameView, Player player, Account account, ArrayList<ArrayList<Pair<String, long[]>>> leads){
		super(gameView);

		this.player = player;
		this.account = account;
		this.leaderboards = leads;

		SHOP1 = BitmapFactory.decodeResource(this.gameView.getContext().getResources(), R.drawable.shop_1);
		SHOP2 = BitmapFactory.decodeResource(this.gameView.getContext().getResources(), R.drawable.shop_2);
		SHOP3 = BitmapFactory.decodeResource(this.gameView.getContext().getResources(), R.drawable.shop_3);
		SHOP4 = BitmapFactory.decodeResource(this.gameView.getContext().getResources(), R.drawable.shop_4);
	}

	@Override
	public void goBack(Activity activity){
		CustomizeScreen cs = new CustomizeScreen(this.gameView, this.player, this.account, this.leaderboards);
		this.gameView.setScreen(cs);
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

				this.uielements.stream().filter(el -> el instanceof MouseSensible).forEach(el -> ((MouseSensible)el).onClick(event.x, event.y));
				break;

			case RELEASED:
				this.uielements.stream().filter(el -> el instanceof MouseSensible).forEach(el -> ((MouseSensible)el).onDrag(event.x, event.y));
				break;

			case DRAGGED:
				this.uielements.stream().filter(el -> el instanceof MouseSensible).forEach(el -> ((MouseSensible)el).onRelease(event.x, event.y));
				break;
		}
	}

	@Override
	public void update(int screenWidth, int screenHeight){
		SCREEN_W = screenWidth;
		SCREEN_H = screenHeight;

		if (this.uielements.size() == 0){
			Button button1 = new Button(this.gameView, 0xFF10B981, 0xFF059669, rsw(this.container1.getMinX()+this.container1.getWidth()*0.1) / WIDTH, rsh(this.container1.getMinY()+this.container1.getHeight()*0.85) / HEIGHT, rsw(this.container1.getWidth()*0.8) / WIDTH, rsh(this.container1.getHeight()*0.1) / HEIGHT, "Watch AD", UiElement.FONT_LARGE, 0xFFFFFFFF, () -> {
				this.gameView.showRewarded(() -> {
					try {
						this.player.getAppData().put("currency", this.player.getAppData().optInt("currency", 0) + 300); // 300 MangoCoins
						this.player.forcePush(this.account);
						goBack(null);
					} catch (JSONException ex){
						ex.printStackTrace();
					}
				});
			});

			this.button2 = new Button(this.gameView, 0xFF3366FF, 0xFF002699, rsw(this.container2.getMinX()+this.container2.getWidth()*0.1) / WIDTH, rsh(this.container2.getMinY()+this.container2.getHeight()*0.85) / HEIGHT, rsw(this.container2.getWidth()*0.8) / WIDTH, rsh(this.container2.getHeight()*0.1) / HEIGHT, "Loading...", UiElement.FONT_LARGE, 0xFFFFFFFF, () -> {
				if (!this.owned1){
					this.gameView.getBillingManager().purchaseProduct(PurchaseHandler.SKIN_PREMIUM, () -> {
						goBack(null);
					});
				} else {
					this.button2.bounce();
				}
			});

			Button button3 = new Button(this.gameView, 0xFF3366FF, 0xFF002699, rsw(this.container3.getMinX()+this.container3.getWidth()*0.1) / WIDTH, rsh(this.container3.getMinY()+this.container3.getHeight()*0.85) / HEIGHT, rsw(this.container3.getWidth()*0.8) / WIDTH, rsh(this.container3.getHeight()*0.1) / HEIGHT, "Loading...", UiElement.FONT_LARGE, 0xFFFFFFFF, () -> {
				this.gameView.getBillingManager().purchaseProduct(PurchaseHandler.MANGOCOINS_BUNDLE, () -> {
					goBack(null);
				});
			});

			this.button4 = new Button(this.gameView, 0xFFFF751A, 0xFFCC5200, rsw(this.container4.getMinX()+this.container4.getWidth()*0.1) / WIDTH, rsh(this.container4.getMinY()+this.container4.getHeight()*0.72) / HEIGHT, rsw(this.container4.getWidth()*0.8) / WIDTH, rsh(this.container4.getHeight()*0.1) / HEIGHT, "5000", UiElement.FONT_LARGE, this.player.getAppData().optInt("currency") >= 5000 || this.player.isPermanentSnake(SKIN_LAUNCH_ID) ? 0xFFFFFFFF : 0xFFFF0000, () -> {
				try {
					int mangocoins = this.player.getAppData().optInt("currency", 0);
					if (mangocoins >= 5000 && !this.player.isPermanentSnake(SKIN_LAUNCH_ID)){
						this.player.getAppData().put("currency", mangocoins-5000);
						this.player.setPermanentSnakeIndex(SKIN_LAUNCH_ID); // Skid id is 26
						this.player.forcePush(this.account);
						goBack(null);
					} else {
						this.button4.bounce();
					}
				} catch (JSONException ex){
					ex.printStackTrace();
				}
			});

			this.button5 = new Button(this.gameView, 0xFF3366FF, 0xFF002699, rsw(this.container4.getMinX()+this.container4.getWidth()*0.1) / WIDTH, rsh(this.container4.getMinY()+this.container4.getHeight()*0.85) / HEIGHT, rsw(this.container4.getWidth()*0.8) / WIDTH, rsh(this.container4.getHeight()*0.1) / HEIGHT, "Loading...", UiElement.FONT_LARGE, 0xFFFFFFFF, () -> {
				if (!this.owned3){
					this.gameView.getBillingManager().purchaseProduct(PurchaseHandler.SKIN_LAUNCH, () -> {
						this.player.updateSnakeIndex(SKIN_LAUNCH_ID); // Skin id is 26
						this.player.save();
						goBack(null);
					});
				} else {
					this.button5.bounce();
				}
			});

			this.uielements.add(button1);
			this.uielements.add(this.button2);
			this.uielements.add(button3);
			this.uielements.add(this.button4);
			this.uielements.add(this.button5);

			// Update prices
			this.gameView.getBillingManager().getProductDetails(PurchaseHandler.SKIN_PREMIUM, pd -> {
				if (PURCHASE_HANDLER.isPurchased(PurchaseHandler.SKIN_PREMIUM)){
					button2.setStyle(0xFF475569, 0xFF64748B, "OWNED", null);
					this.owned1 = true;
				} else {
					button2.setStyle(null, null, pd.getOneTimePurchaseOfferDetails().getFormattedPrice(), null);
				}
			});

			this.gameView.getBillingManager().getProductDetails(PurchaseHandler.MANGOCOINS_BUNDLE, pd -> {
				button3.setStyle(null, null, pd.getOneTimePurchaseOfferDetails().getFormattedPrice(), null);
			});

			if (this.player.isPermanentSnake(SKIN_LAUNCH_ID)){
				button4.setStyle(0xFF475569, 0xFF64748B, "OWNED", null);
			}

			this.gameView.getBillingManager().getProductDetails(PurchaseHandler.SKIN_LAUNCH, pd -> {
				if (PURCHASE_HANDLER.isPurchased(PurchaseHandler.SKIN_LAUNCH) || this.player.isPermanentSnake(SKIN_LAUNCH_ID)){
					button5.setStyle(0xFF475569, 0xFF64748B, "OWNED", null);
					this.owned3 = true;
				} else {
					button5.setStyle(null, null, pd.getOneTimePurchaseOfferDetails().getFormattedPrice(), null);
				}
			});
		}
	}

	@Override
	public void render(ICanvas canvas){
		canvas.clear(0xFF020617);

		// Render back button
		canvas.setEffect(10, 0xFFF472B6);
		canvas.fillText("< Back To Customization", rsw(this.backButton.getMinX()), rsh(this.backButton.getMaxY()-this.backButton.getHeight()*0.2), 0xFFF472B6, UiElement.FONT_LARGE, TextAlignment.LEFT);
		canvas.clearEffect();

		// Render containers
		canvas.fillRoundRect(rsw(this.container1.getMinX()), rsh(this.container1.getMinY()), rsw(this.container1.getWidth()), rsh(this.container1.getHeight()), rsw(0.02), rsh(0.04), 0xFF0F172A);
		canvas.strokeRoundRect(rsw(this.container1.getMinX()), rsh(this.container1.getMinY()), rsw(this.container1.getWidth()), rsh(this.container1.getHeight()), rsw(0.02), rsh(0.04), 0xFF7D98CF, rsh(0.002));
		canvas.fillRoundRect(rsw(this.container2.getMinX()), rsh(this.container2.getMinY()), rsw(this.container2.getWidth()), rsh(this.container2.getHeight()), rsw(0.02), rsh(0.04), 0xFF0F172A);
		canvas.strokeRoundRect(rsw(this.container2.getMinX()), rsh(this.container2.getMinY()), rsw(this.container2.getWidth()), rsh(this.container2.getHeight()), rsw(0.02), rsh(0.04), 0xFF7D98CF, rsh(0.002));
		canvas.fillRoundRect(rsw(this.container3.getMinX()), rsh(this.container3.getMinY()), rsw(this.container3.getWidth()), rsh(this.container3.getHeight()), rsw(0.02), rsh(0.04), 0xFF0F172A);
		canvas.strokeRoundRect(rsw(this.container3.getMinX()), rsh(this.container3.getMinY()), rsw(this.container3.getWidth()), rsh(this.container3.getHeight()), rsw(0.02), rsh(0.04), 0xFF7D98CF, rsh(0.002));
		canvas.fillRoundRect(rsw(this.container4.getMinX()), rsh(this.container4.getMinY()), rsw(this.container4.getWidth()), rsh(this.container4.getHeight()), rsw(0.02), rsh(0.04), 0xFF0F172A);
		canvas.strokeRoundRect(rsw(this.container4.getMinX()), rsh(this.container4.getMinY()), rsw(this.container4.getWidth()), rsh(this.container4.getHeight()), rsw(0.02), rsh(0.04), 0xFF7D98CF, rsh(0.002));

		canvas.fillText("Reward AD", rsw(this.container1.getMinX()+this.container1.getWidth()*0.5), rsh(this.container1.getMinY()+this.container1.getHeight()*0.10), 0xFF94F7D4, UiElement.FONT_MEDIUM, TextAlignment.CENTER);
		canvas.fillText("Premium Package", rsw(this.container2.getMinX()+this.container2.getWidth()*0.5), rsh(this.container2.getMinY()+this.container2.getHeight()*0.10), 0xFF94F7D4, UiElement.FONT_MEDIUM, TextAlignment.CENTER);
		canvas.fillText("MangoCoins Bundle", rsw(this.container3.getMinX()+this.container3.getWidth()*0.5), rsh(this.container3.getMinY()+this.container3.getHeight()*0.10), 0xFF94F7D4, UiElement.FONT_MEDIUM, TextAlignment.CENTER);
		canvas.fillText("Game Launch Skin", rsw(this.container4.getMinX()+this.container4.getWidth()*0.5), rsh(this.container4.getMinY()+this.container4.getHeight()*0.10), 0xFF94F7D4, UiElement.FONT_MEDIUM, TextAlignment.CENTER);

		final double size = rsw(this.container1.getWidth() * 0.5);
		canvas.drawImage(SHOP1, rsw(this.container1.getMinX()+this.container1.getWidth()*0.5) - size/2, rsh(this.container1.getMinY()+this.container1.getHeight()*0.32)-size/2, size, size);
		canvas.drawImage(SHOP2, rsw(this.container2.getMinX()+this.container1.getWidth()*0.5) - size/2, rsh(this.container2.getMinY()+this.container2.getHeight()*0.32)-size/2, size, size);
		canvas.drawImage(SHOP3, rsw(this.container3.getMinX()+this.container1.getWidth()*0.5) - size/2, rsh(this.container3.getMinY()+this.container3.getHeight()*0.32)-size/2, size, size);
		canvas.drawImage(SHOP4, rsw(this.container4.getMinX()+this.container4.getWidth()*0.5) - size/2, rsh(this.container4.getMinY()+this.container4.getHeight()*0.32)-size/2, size, size);

		canvas.setEffect(10, 0xFF00E5FF);
		canvas.fillText("Watch an AD to\nget 300 MangoCoins\nfor free", rsw(this.container1.getMinX()+this.container1.getWidth()*0.5), rsh(this.container1.getMinY()+this.container1.getHeight()*0.56), 0xFF00E5FF, UiElement.FONT_MEDIUM*0.9, TextAlignment.CENTER);
		canvas.fillText("Unlock all premium\nskins + no ADS", rsw(this.container2.getMinX()+this.container2.getWidth()*0.5), rsh(this.container2.getMinY()+this.container2.getHeight()*0.56), 0xFF00E5FF, UiElement.FONT_MEDIUM*0.9, TextAlignment.CENTER);
		canvas.fillText("10000 MangoCoins\n+ no ADS", rsw(this.container3.getMinX()+this.container3.getWidth()*0.5), rsh(this.container3.getMinY()+this.container3.getHeight()*0.56), 0xFF00E5FF, UiElement.FONT_MEDIUM*0.9, TextAlignment.CENTER);
		canvas.fillText("Visit the game page to\nget it for FREE!", rsw(this.container4.getMinX()+this.container4.getWidth()*0.5), rsh(this.container4.getMinY()+this.container4.getHeight()*0.56), 0xFF00E5FF, UiElement.FONT_MEDIUM*0.9, TextAlignment.CENTER);
		canvas.clearEffect();

		canvas.fillText("And you support my work\nof course, thanks :)", rsw(this.container2.getMinX()+this.container2.getWidth()*0.5), rsh(this.container2.getMinY()+this.container2.getHeight()*0.77), 0xFF64748B, UiElement.FONT_SMALL*1.15, TextAlignment.CENTER);
		canvas.fillText("And you support my work\nof course, thanks :)", rsw(this.container3.getMinX()+this.container3.getWidth()*0.5), rsh(this.container3.getMinY()+this.container3.getHeight()*0.77), 0xFF64748B, UiElement.FONT_SMALL*1.15, TextAlignment.CENTER);

		for (UiElement element : this.uielements){
			element.render(canvas);
		}
	}

	private static double rsw(double x){
		return x * SCREEN_W;
	}

	private static double rsh(double y){
		return y * SCREEN_H;
	}
}
