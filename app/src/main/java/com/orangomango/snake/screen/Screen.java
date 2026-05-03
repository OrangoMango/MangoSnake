package com.orangomango.snake.screen;

import android.app.Activity;

import com.orangomango.androidbridge.ICanvas;
import com.orangomango.androidbridge.PointerEvent;
import com.orangomango.snake.GameView;

public abstract class Screen{
	protected GameView gameView;

	public Screen(GameView gameView){
		this.gameView = gameView;
	}

	public void goBack(Activity activity){
		activity.finish();
	}

	public abstract void handleInput(PointerEvent event);
	public abstract void update(int screenWidth, int screenHeight);
	public abstract void render(ICanvas canvas);
}
