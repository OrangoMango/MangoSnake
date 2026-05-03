package com.orangomango.snake.ui;

import com.orangomango.androidbridge.ICanvas;
import com.orangomango.androidbridge.geometry.Rectangle2D;
import com.orangomango.androidbridge.geometry.TextAlignment;
import com.orangomango.androidbridge.util.Pair;

import java.util.function.Function;

import com.orangomango.snake.GameView;

import static com.orangomango.snake.GameView.AUDIO;

public class Slider extends UiElement implements MouseSensible{
	private double min, max, def;
	private String label, unit;
	private double value;
	private boolean selected = false;
	private Runnable sChanged = null;
	private Pair<Double, Double> bounds = new Pair<Double, Double>(0.0, 1.0);
	private Function<Double, String> formatter;

	public Slider(GameView gameView, double x, double y, double w, double h, String label, String unit){
		super(gameView, x, y, w, h);
		this.label = label;
		this.unit = unit;
	}

	public void setInterval(double min, double max, double def){
		this.min = min;
		this.max = max;
		this.def = def;

		this.value = (this.def-this.min) / (this.max-this.min);
	}

	public void setBounds(double min, double max){
		// 0 <= min, max <= 1
		this.bounds = new Pair<Double, Double>(min, max);
	}

	@Override
	public void onClick(double ex, double ey){
		Rectangle2D rect = getBallBounds();

		// Scale it by 20%
		rect = rect.translate(-rect.getWidth()*0.1, -rect.getHeight()*0.1);
		rect = new Rectangle2D(rect.getMinX(), rect.getMinY(), rect.getWidth()*1.2, rect.getHeight()*1.2);

		if (rect.contains(ex, ey)){
			this.selected = true;
			AUDIO.playSound("gui");
			this.gameView.triggerVibration(100);
		}
	}

	@Override
	public void onRelease(double ex, double ey){
		this.selected = false;
	}

	@Override
	public void onDrag(double ex, double ey){
		if (this.selected){
			double newValue = (ex-rw(this.x)) / rw(this.w);
			this.value = Math.min(this.bounds.getValue(), Math.max(newValue, this.bounds.getKey()));
			if (this.sChanged != null) this.sChanged.run();
		}
	}

	@Override
	public void render(ICanvas canvas){
		// Label text
		canvas.fillText(this.label, rw(this.x+0.02*this.w), rh(this.y+0.25*this.h), 0xFF94F7D4, UiElement.FONT_MEDIUM*0.9, TextAlignment.LEFT);

		// Value text
		final String text = this.unit.equals("%") ? String.format("%.0f%%", this.value * 100) : String.format("%.0f%s", this.value*(this.max-this.min)+this.min, this.unit);
		canvas.fillText(this.formatter == null ? text : this.formatter.apply(this.value*(this.max-this.min)+this.min), rw(this.x+0.98*this.w), rh(this.y+0.25*this.h), 0xFF10B981, UiElement.FONT_MEDIUM*0.8, TextAlignment.RIGHT);

		// Slider bar
		canvas.fillRect(rw(this.x+0.05*this.w), rh(this.y+0.70*this.h), rw(0.90*this.w), rh(0.08*this.h), 0xFF1E293B);

		// Slider ball
		Rectangle2D ball = getBallBounds();
		canvas.fillOval(ball.getMinX(), ball.getMinY(), ball.getWidth(), ball.getHeight(), 0xFF10B981);
	}

	public double getValue(){
		return this.min + this.value * (this.max-this.min);
	}

	public void setValue(double v){
		if (v < this.min || v > this.max){
			throw new IllegalStateException("Input value out of bounds");
		}

		this.value = (v-this.min) / (this.max-this.min);
	}

	public void setFormatter(Function<Double, String> f){
		this.formatter = f;
	}

	private Rectangle2D getBallBounds(){
		return new Rectangle2D(rw((this.x+0.05*this.w)+(this.value*0.90*this.w))-rh(0.015), rh(this.y+0.73*this.h-0.015), rh(0.03), rh(0.03));
	}

	public void setOnStateChanged(Runnable r){
		this.sChanged = r;
	}
}
