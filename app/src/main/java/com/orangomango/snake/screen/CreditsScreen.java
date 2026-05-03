package com.orangomango.snake.screen;

import android.app.Activity;

import com.orangomango.androidbridge.FileHelper;
import com.orangomango.androidbridge.ICanvas;
import com.orangomango.androidbridge.PointerEvent;
import com.orangomango.androidbridge.geometry.TextAlignment;
import com.orangomango.snake.GameView;
import com.orangomango.snake.R;
import com.orangomango.snake.ui.UiElement;

public class CreditsScreen extends Screen{
	private static int SCREEN_W, SCREEN_H;

	private String creditsText;
	private double offset = 0;
	private long lastFrameTime = System.currentTimeMillis();
	private long startTime = System.currentTimeMillis();

	public CreditsScreen(GameView gameView){
		super(gameView);

		this.creditsText = FileHelper.readRawResource(this.gameView.getContext(), R.raw.credits);
		this.creditsText = this.creditsText.replace("??version??", this.gameView.getContext().getResources().getString(R.string.app_version));
	}

	@Override
	public void goBack(Activity activity){
		HomeScreen hs = new HomeScreen(this.gameView);
		this.gameView.setScreen(hs);
	}

	@Override
	public void handleInput(PointerEvent event){
		switch (event.type){
			case PRESSED:
				if (System.currentTimeMillis()-this.startTime > 1000){
					this.gameView.triggerVibration(100);
					goBack(null);
				}
				break;
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

		long now = System.currentTimeMillis();
		if (now - this.lastFrameTime > 25){
			this.offset += rsh(0.003);
			if (this.offset > rsh(2.5)){
				this.offset = 0;
			}

			this.lastFrameTime = now;
		}

		String[] lines = this.creditsText.split("\n");
		double y = Math.round(rsh(1.05) - this.offset);
		for (String line : lines){
			if (line.startsWith("&&")){
				String[] parts = line.substring(2).split("~");
				double lengthA = calculateLength(canvas, parts[0], UiElement.FONT_MEDIUM) + rsw(0.03);
				double lengthB = calculateLength(canvas, parts[1], UiElement.FONT_MEDIUM) + rsw(0.03);

				fillText(canvas, parts[0], rsw(0.5) - lengthA, y, 0xFF94F7D4, UiElement.FONT_MEDIUM, TextAlignment.LEFT);
				canvas.fillText("~", rsw(0.5), y, 0xFF94F7D4, UiElement.FONT_MEDIUM, TextAlignment.CENTER);
				fillText(canvas, parts[1], rsw(0.5) + lengthB, y, 0xFF94F7D4, UiElement.FONT_MEDIUM, TextAlignment.RIGHT);
			} else {
				fillText(canvas, line, rsw(0.5), y, 0xFF94F7D4, UiElement.FONT_MEDIUM, TextAlignment.CENTER);
			}
			y += this.gameView.getPaintDescent() - this.gameView.getPaintAscent();
		}
	}

	private void fillText(ICanvas canvas, String text, double x, double y, int color, double font, TextAlignment alignment){
		double startX = 0;
		double textLength = calculateLength(canvas, text, font);
		switch (alignment){
			case LEFT:
				startX = x;
				break;
			case CENTER:
				startX = x - textLength*0.5;
				break;
			case RIGHT:
				startX = x - textLength;
				break;
		}

		double pos = startX;
		String[] parts = text.split(" ");
		for (int i = 0; i < parts.length; i++){
			String part = parts[i];

			if (part.startsWith("{")){
				String data = part.split("\\^")[0].substring(1);
				data = data + (i == parts.length-1 ? "" : " ");
				String colorData = part.split("\\^")[1];
				colorData = colorData.substring(0, colorData.indexOf("}")); // Everything after '}' gets ignored

				int partC = Integer.parseUnsignedInt(colorData, 16);
				canvas.setEffect(15, partC);
				canvas.fillText(data, pos, y, partC, font, TextAlignment.LEFT);
				canvas.clearEffect();

				pos += canvas.measureText(data, font);
			} else {
				part = part + (i == parts.length-1 ? "" : " ");
				canvas.setEffect(15, color);
				canvas.fillText(part, pos, y, color, font, TextAlignment.LEFT);
				canvas.clearEffect();

				pos += canvas.measureText(part, font);
			}
		}
	}

	private static double calculateLength(ICanvas canvas, String text, double font){
		text = text.replaceAll("\\{([^\\^]+)\\^[^}]+\\}", "$1");
		text = text.replace("&&", "");
		return canvas.measureText(text, font);
	}

	private static double rsw(double x){
		return x * SCREEN_W;
	}

	private static double rsh(double y){
		return y * SCREEN_H;
	}
}
