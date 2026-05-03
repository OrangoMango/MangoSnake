package com.orangomango.snake.ui;

import com.orangomango.androidbridge.ICanvas;
import com.orangomango.androidbridge.geometry.TextAlignment;
import com.orangomango.snake.GameView;

public class Container extends UiElement{
	private int titleColor, color, borderColor;
	private Double titleFontSize;
	private String titleText;

	public Container(GameView gameView, int color, int bcolor, double x, double y, double w, double h, Integer titleColor, Double titleFontSize, String titleText){
		super(gameView, x, y, w, h);
		this.color = color;
		this.borderColor = bcolor;
		if (titleColor != null) this.titleColor = titleColor;
		this.titleFontSize = titleFontSize;
		this.titleText = titleText;
	}

	@Override
	public void render(ICanvas canvas){
		canvas.fillRoundRect(rw(this.x), rh(this.y), rw(this.w), rh(this.h), rh(0.035), rh(0.035), this.color);
		canvas.strokeRoundRect(rw(this.x), rh(this.y), rw(this.w), rh(this.h), rh(0.035), rh(0.035), this.borderColor, rh(0.0035));

		if (this.titleText != null){
			canvas.fillText(this.titleText, rw(this.x+this.w*0.5), rh(this.y+0.053), this.titleColor, this.titleFontSize, TextAlignment.CENTER);
		}
	}
}
