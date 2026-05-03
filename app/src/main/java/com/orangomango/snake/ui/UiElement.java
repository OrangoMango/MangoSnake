package com.orangomango.snake.ui;

import com.orangomango.androidbridge.ICanvas;
import com.orangomango.androidbridge.geometry.Rectangle2D;
import com.orangomango.snake.GameView;

import static com.orangomango.snake.GameView.WIDTH;
import static com.orangomango.snake.GameView.HEIGHT;

public abstract class UiElement{
	protected double x, y, w, h;
	protected GameView gameView;

	public static double FONT_SMALL, FONT_MEDIUM, FONT_LARGE, FONT_LARGELARGE, FONT_MEDIUMLARGE, FONT_EXTRALARGE, FONT_EXTRAEXTRALARGE;

	public UiElement(GameView gameView, double x, double y, double w, double h){
		this.gameView = gameView;
		this.x = x;
		this.y = y;
		this.w = w;
		this.h = h;
	}

	public Rectangle2D getBounds(){
		return new Rectangle2D(this.x, this.y, this.w, this.h);
	}

	public abstract void render(ICanvas canvas);

	public static double rw(double x){
		return x * WIDTH;
	}

	public static double rh(double y){
		return y * HEIGHT;
	}
}
