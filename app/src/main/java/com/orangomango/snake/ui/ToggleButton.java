package com.orangomango.snake.ui;

import com.orangomango.androidbridge.ICanvas;
import com.orangomango.androidbridge.geometry.Rectangle2D;
import com.orangomango.androidbridge.geometry.TextAlignment;
import com.orangomango.snake.GameView;

import static com.orangomango.snake.GameView.AUDIO;

public class ToggleButton extends UiElement implements MouseSensible{
	private String label;
	private boolean selected = false;
	private Runnable sChanged = null;

	public ToggleButton(GameView gameView, double x, double y, double w, double h, String label){
		super(gameView, x, y, w, h);
		this.label = label;
	}

	@Override
	public void onClick(double ex, double ey){
		Rectangle2D rect = new Rectangle2D(rw(this.x), rh(this.y), rw(this.w), rh(this.h));
		if (rect.contains(ex, ey)){
			this.selected = !this.selected;
			if (this.sChanged != null) this.sChanged.run();
			AUDIO.playSound("gui");
			this.gameView.triggerVibration(100);
		}
	}

	@Override
	public void render(ICanvas canvas){
		// Label text
		canvas.fillText(this.label, rw(this.x+0.02*this.w), rh(this.y+0.60*this.h), 0xFF94F7D4, UiElement.FONT_MEDIUM, TextAlignment.LEFT);

		// Toggle
		Rectangle2D rect = getToggleRect();
		canvas.fillRoundRect(rect.getMinX(), rect.getMinY(), rect.getWidth(), rect.getHeight(), rh(0.017), rh(0.017), this.selected ? 0xFF064E3B : 0xFF1E293B);

		if (this.selected){
			canvas.fillOval(rw(this.x+0.90*this.w-0.06*this.h), rh(this.y+0.50*this.h)-rw(0.06*this.h), rw(0.12*this.h), rw(0.12*this.h), 0xFF10B981);
		} else {
			canvas.fillOval(rw(this.x+0.80*this.w-0.06*this.h), rh(this.y+0.50*this.h)-rw(0.06*this.h), rw(0.12*this.h), rw(0.12*this.h), 0xFF94A3B8);
		}
	}

	public boolean getSelected(){
		return this.selected;
	}

	public void setSelected(boolean v){
		this.selected = v;
	}

	private Rectangle2D getToggleRect(){
		return new Rectangle2D(rw(this.x+0.75*this.w), rh(this.y+0.35*this.h), rw(0.20*this.w), rh(0.30*this.h));
	}

	public void setOnStateChanged(Runnable r){
		this.sChanged = r;
	}
}
