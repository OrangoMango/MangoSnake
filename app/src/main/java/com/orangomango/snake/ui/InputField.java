package com.orangomango.snake.ui;

import android.app.AlertDialog;
import android.widget.EditText;

import com.orangomango.androidbridge.ICanvas;
import com.orangomango.androidbridge.geometry.Rectangle2D;
import com.orangomango.androidbridge.geometry.TextAlignment;
import com.orangomango.snake.GameView;

public class InputField extends UiElement implements MouseSensible{
	private String placeholder;
	private boolean focus = false;
	private String text = "";
	private boolean password = false;

	public InputField(GameView gameView, double x, double y, double w, double h, String placeholder){
		super(gameView, x, y, w, h);
		this.placeholder = placeholder;
	}

	@Override
	public void onClick(double ex, double ey){
		Rectangle2D rect = new Rectangle2D(rw(this.x), rh(this.y), rw(this.w), rh(this.h));
		if (rect.contains(ex, ey)){
			this.focus = true;

			// Show input dialog
			final EditText input = new EditText(this.gameView.getContext());
			input.setText(this.text);
			new AlertDialog.Builder(this.gameView.getContext())
					.setView(input)
					.setPositiveButton("Submit", (dialog, which) -> {
						String text = input.getText().toString();
						setText(text);
					})
					.setNegativeButton("Cancel", null)
					.show();
		} else {
			this.focus = false;
		}
	}

	public void setPasswordField(boolean value){
		this.password = value;
	}

	public void setText(String value){
		this.text = value;
	}

	public String getText(){
		return this.text;
	}

	@Override
	public void render(ICanvas canvas){
		canvas.fillRoundRect(rw(this.x), rh(this.y), rw(this.w), rh(this.h), rh(0.035), rh(0.035), 0xFF001219);
		canvas.strokeRoundRect(rw(this.x), rh(this.y), rw(this.w), rh(this.h), rh(0.035), rh(0.035), this.focus ? 0xFF10B981 : 0xFF1E293B, rh(0.0035));

		final String displayText = this.password ? "*".repeat(this.text.length()) : this.text;
		canvas.fillText(this.text.equals("") ? this.placeholder : displayText, rw(this.x+0.05*this.w), rh(this.y+0.65*this.h), this.text.equals("") ?  0xFF475569 : 0xFFF8FAFC, UiElement.FONT_SMALL, TextAlignment.LEFT);

		final double textWidth = canvas.measureText(displayText, UiElement.FONT_SMALL);
		if (this.focus && System.currentTimeMillis() % 1000 < 500) {
			final double cursorX = rw(this.x+0.05*this.w) + textWidth + 2;
			canvas.strokeLine(cursorX, rh(this.y+0.25*this.h), cursorX, rh(this.y+0.75*this.h), 0xFF10B981, 2);
		}
	}
}
