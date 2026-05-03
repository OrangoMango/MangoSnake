package com.orangomango.snake.screen;

import static com.orangomango.snake.GameView.HEIGHT;
import static com.orangomango.snake.GameView.WIDTH;

import android.app.Activity;

import com.orangomango.androidbridge.ICanvas;
import com.orangomango.androidbridge.PointerEvent;
import com.orangomango.androidbridge.geometry.TextAlignment;
import com.orangomango.snake.GameView;
import com.orangomango.snake.R;
import com.orangomango.snake.ui.Button;
import com.orangomango.snake.ui.UiElement;

public class UpdateScreen extends Screen{
	private static int SCREEN_W, SCREEN_H;

	private double size, px, py;
	private Button updateButton, exitButton;
	private String latestVersion;

	public UpdateScreen(GameView gameView, String latestVersion){
		super(gameView);

		this.latestVersion = latestVersion;
	}

	@Override
	public void handleInput(PointerEvent event){
		switch (event.type){
			case PRESSED:
				this.updateButton.onClick(event.x, event.y);
				this.exitButton.onClick(event.x, event.y);
				break;
		}
	}

	@Override
	public void update(int screenWidth, int screenHeight){
		SCREEN_W = screenWidth;
		SCREEN_H = screenHeight;

		this.size = rsh(0.85);
		this.px = (SCREEN_W - size) / 2;
		this.py = (SCREEN_H - size) / 2;

		if (this.updateButton == null){
			this.updateButton = new Button(this.gameView, 0xFF10B981, 0xFF059669, (this.px + this.size*0.15) / WIDTH, (this.py + this.size*0.55) / HEIGHT, this.size*0.70 / WIDTH, this.size*0.15 / HEIGHT, "UPDATE", UiElement.FONT_LARGELARGE, 0xFFFFFFFF, () -> {
				this.gameView.openPlayStore();
			});
			this.updateButton.setGlow(true);

			this.exitButton = new Button(this.gameView, 0x00000000, 0xFFF8A0CD, (this.px + this.size*0.15) / WIDTH, (this.py + this.size*0.75) / HEIGHT, this.size*0.70 / WIDTH, this.size*0.12 / HEIGHT, "EXIT", UiElement.FONT_LARGELARGE, 0xFFFFFFFF, () -> {
				goBack((Activity) this.gameView.getContext());
			});
			this.exitButton.setGlow(true);
		}
	}

	@Override
	public void render(ICanvas canvas){
		canvas.clear(0xFF020617);

		canvas.strokeRoundRect(this.px-this.size*0.1, this.py, this.size*1.2, this.size, rsw(0.03), rsw(0.03), 0xFF4CC9F0, rsh(0.003));

		canvas.fillText("UPDATE REQUIRED", this.px + this.size*0.5, this.py + this.size*0.20, 0xFF4CC9F0, UiElement.FONT_MEDIUMLARGE, TextAlignment.CENTER);
		canvas.fillText("A new version of " + this.gameView.getContext().getResources().getString(R.string.app_name) + " is available!\nPlease update to the latest version\nto continue playing :)", this.px + this.size*0.5, this.py + this.size*0.35, 0xFFFFFFFF, UiElement.FONT_LARGE, TextAlignment.CENTER);

		canvas.fillText(String.format("%s v%s, Latest: v%s", this.gameView.getContext().getResources().getString(R.string.app_name), this.gameView.getContext().getResources().getString(R.string.app_version), this.latestVersion), SCREEN_W-rsw(0.05), SCREEN_H-rsw(0.02), 0xFFFFFFFF, UiElement.FONT_SMALL, TextAlignment.RIGHT);

		this.updateButton.render(canvas);
		this.exitButton.render(canvas);
	}

	private static double rsw(double x){
		return x * SCREEN_W;
	}

	private static double rsh(double y){
		return y * SCREEN_H;
	}
}
