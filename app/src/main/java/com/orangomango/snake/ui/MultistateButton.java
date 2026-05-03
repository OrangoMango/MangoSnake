package com.orangomango.snake.ui;

import com.orangomango.androidbridge.ICanvas;
import com.orangomango.androidbridge.geometry.Rectangle2D;
import com.orangomango.androidbridge.geometry.TextAlignment;
import com.orangomango.androidbridge.util.Pair;
import com.orangomango.snake.GameView;

import java.util.ArrayList;

import static com.orangomango.snake.GameView.AUDIO;

public class MultistateButton extends UiElement implements MouseSensible{
	private int color;
	private ArrayList<Pair<String, Integer>> states = new ArrayList<>();
	private ArrayList<Runnable> onStateChanged = new ArrayList<>();
	private int currentSelection = 0;
	private boolean disabled = false;
	private Runnable sChanged = null;

	public MultistateButton(GameView gameView, int color, double x, double y, double w, double h){
		super(gameView, x, y, w, h);
		this.color = color;
	}

	@Override
	public void onClick(double ex, double ey){
		Rectangle2D rect = new Rectangle2D(rw(this.x), rh(this.y), rw(this.w), rh(this.h));
		if (rect.contains(ex, ey)){
			this.currentSelection = (this.currentSelection + 1) % this.states.size();
			this.onStateChanged.get(this.currentSelection).run();
			if (this.sChanged != null) this.sChanged.run();
			AUDIO.playSound("gui");
			this.gameView.triggerVibration(100);
		}
	}

	public void addState(String text, int color, Runnable r){
		this.states.add(new Pair<String, Integer>(text, color));
		this.onStateChanged.add(r);
	}

	@Override
	public void render(ICanvas canvas){
		Pair<String, Integer> selected = this.states.get(this.currentSelection);
		if (this.disabled) selected = new Pair<String, Integer>(selected.getKey(), 0xFF334155);

		canvas.setEffect(10, selected.getValue());
		canvas.fillRoundRect(rw(this.x), rh(this.y), rw(this.w), rh(this.h), rh(0.035), rh(0.035), this.color);
		canvas.strokeRoundRect(rw(this.x), rh(this.y), rw(this.w), rh(this.h), rh(0.035), rh(0.035), selected.getValue(), rh(0.0035));
		canvas.clearEffect();

		canvas.fillText(selected.getKey(), rw(this.x+0.5*this.w), rh(this.y+0.45*this.h), selected.getValue(), UiElement.FONT_MEDIUM, TextAlignment.CENTER);

		final double dotRadius = 0.05*this.h;
		final double dotSpacing = 0.1*this.w;
		final double totalWidth = (this.states.size()-1)*dotSpacing;

		for (int i = 0; i < this.states.size(); i++){
			canvas.fillOval(rw(this.x + (this.w-totalWidth)*0.5 + i*dotSpacing) - rh(dotRadius), rh(this.y+0.75*this.h - dotRadius), rh(dotRadius*2), rh(dotRadius*2), this.currentSelection == i ? selected.getValue() : 0xFF1E293B);
		}
	}

	public void setState(int index){
		this.currentSelection = index % this.states.size();
		if (this.sChanged != null) this.sChanged.run();
	}

	public void setDisabled(boolean value){
		this.disabled = value;
	}

	public void setOnStateChanged(Runnable r){
		this.sChanged = r;
	}
}
