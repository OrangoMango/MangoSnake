package com.orangomango.snake.ui;

import com.orangomango.androidbridge.ICanvas;
import com.orangomango.androidbridge.geometry.Rectangle2D;
import com.orangomango.androidbridge.geometry.TextAlignment;
import com.orangomango.snake.GameView;

import static com.orangomango.snake.GameView.AUDIO;

import android.graphics.Bitmap;

public class Button extends UiElement implements MouseSensible{
	private Runnable onClick;
	private String text;
	private Bitmap resource, overlayImage;
	private int textColor, color, borderColor;
	private double fontSize;
	private boolean onHover = false;
	private double borderSize = 0.0035;
	private boolean glow = false;

	private boolean bouncing;
	private long lastBounce;
	private int bounceCount, bouncingDir;
	private double bouncingX;

	public Button(GameView gameView, int color, int bcolor, double x, double y, double w, double h, String text, double bFont, int tColor, Runnable onClick){
		super(gameView, x, y, w, h);
		this.onClick = onClick;
		this.text = text;
		this.textColor = tColor;
		this.fontSize = bFont;
		this.color = color;
		this.borderColor = bcolor;
	}

	@Override
	public void onClick(double ex, double ey){
		Rectangle2D rect = new Rectangle2D(rw(this.x), rh(this.y), rw(this.w), rh(this.h));
		if (rect.contains(ex, ey)){
			this.onClick.run();
			AUDIO.playSound("gui");
			this.gameView.triggerVibration(100);
		}
	}

	@Override
	public void onHover(double ex, double ey){
		Rectangle2D rect = new Rectangle2D(rw(this.x), rh(this.y), rw(this.w), rh(this.h));
		this.onHover = rect.contains(ex, ey);
	}

	@Override
	public void onRelease(double ex, double ey){
		this.onHover = false;
	}

	@Override
	public void render(ICanvas canvas){
		final double sizeFactor = this.onHover ? 1.05 : 1;
		final double adj = this.onHover ? 0.025 : 0; // 0.05 / 2 = 0.025

		canvas.save();
		canvas.translate(rw(this.bouncingX), 0);

		long now = System.currentTimeMillis();
		if (this.bouncing && now-this.lastBounce > 10){
			this.bouncingX += 0.003 * this.bouncingDir;
			if (Math.abs(this.bouncingX) >= 0.009){
				this.bouncingDir *= -1;
				this.bounceCount++;
			}
			this.lastBounce = now;

			if (this.bounceCount == 2){
				this.bouncing = false;
				this.bouncingX = 0;
			}
		}

		if (this.glow) canvas.setEffect(10, this.borderColor);
		canvas.fillRoundRect(rw(this.x-adj*this.w), rh(this.y-adj*this.h), rw(this.w*sizeFactor), rh(this.h*sizeFactor), rh(0.035), rh(0.035), this.color);
		canvas.strokeRoundRect(rw(this.x-adj*this.w), rh(this.y-adj*this.h), rw(this.w*sizeFactor), rh(this.h*sizeFactor), rh(0.035), rh(0.035), this.borderColor, rh(this.borderSize));
		canvas.clearEffect();

		if (this.overlayImage != null){
			canvas.drawImage(this.overlayImage, rw(this.x+0.075*this.w-adj*this.w), rh(this.y+0.075*this.h-adj*this.h), rw(this.w*0.85*sizeFactor), rh(this.h*0.85*sizeFactor));
		}

		if (this.text == null){
			double imageSize = rw(this.w*0.4*sizeFactor);
			canvas.drawImage(this.resource, rw(this.x)+(rw(this.w)-imageSize)/2-rw(adj*this.w), rh(this.y)+(rh(this.h)-imageSize)/2-rh(adj*this.h), imageSize, imageSize);
		} else {
			canvas.fillText(this.text, rw(this.x-adj*this.w+this.w*0.5*sizeFactor), rh(this.y-adj*this.h+this.h*0.64*sizeFactor), this.textColor, this.fontSize, TextAlignment.CENTER);
		}

		canvas.restore();
	}

	public void bounce(){
		this.bouncing = true;
		this.bouncingX = 0;
		this.bounceCount = 0;
		this.bouncingDir = -1;
		this.lastBounce = System.currentTimeMillis();
	}

	public void setBorderSize(double v){
		this.borderSize = v;
	}

	public void setBitmap(Bitmap b){
		this.resource = b;
	}

	public void setOverlayImage(Bitmap b){
		this.overlayImage = b;
	}

	public void setGlow(boolean value){
		this.glow = value;
	}

	public void setStyle(Integer color, Integer bColor, String text, Integer tColor){
		if (color != null) this.color = color;
		if (bColor != null) this.borderColor = bColor;
		if (text != null) this.text = text;
		if (tColor != null) this.textColor = tColor;
	}
}
