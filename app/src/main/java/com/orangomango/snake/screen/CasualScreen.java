package com.orangomango.snake.screen;

import static com.orangomango.snake.GameView.AUDIO;

import android.app.Activity;

import com.orangomango.androidbridge.ICanvas;
import com.orangomango.androidbridge.PointerEvent;
import com.orangomango.androidbridge.geometry.Rectangle2D;
import com.orangomango.androidbridge.geometry.TextAlignment;
import com.orangomango.snake.GameView;
import com.orangomango.snake.ui.UiElement;

public class CasualScreen extends Screen{
	private static int SCREEN_W, SCREEN_H;

	private final Rectangle2D backButton = new Rectangle2D(0.075, 0.02, 0.25, 0.06);
	private final Rectangle2D screenView = new Rectangle2D(0.075, 0.13, 0.85, 0.80);

	public CasualScreen(GameView gameView){
		super(gameView);
	}

	@Override
	public void handleInput(PointerEvent event){
		switch (event.type){
			case PRESSED:
				if (this.backButton.scale(UiElement::rw, UiElement::rh).contains(event.x, event.y)){
					this.gameView.triggerVibration(100);
					AUDIO.playSound("gui");
					goBack(null);
				}
		}
	}

	@Override
	public void update(int screenWidth, int screenHeight){
		SCREEN_W = screenWidth;
		SCREEN_H = screenHeight;
	}

	@Override
	public void render(ICanvas canvas){
		canvas.clear(0xFF020617);

		// Render back button
		canvas.setEffect(10, 0xFFF472B6);
		canvas.fillText("< Back To Home", rsw(this.backButton.getMinX()), rsh(this.backButton.getMaxY()-this.backButton.getHeight()*0.2), 0xFFF472B6, UiElement.FONT_LARGE, TextAlignment.LEFT);
		canvas.clearEffect();
	}

	@Override
	public void goBack(Activity activity){
		HomeScreen hs = new HomeScreen(this.gameView);
		this.gameView.setScreen(hs);
	}

	private static double rsw(double x){
		return x * SCREEN_W;
	}

	private static double rsh(double y){
		return y * SCREEN_H;
	}
}
