package com.orangomango.snake.game;

import com.orangomango.androidbridge.ICanvas;

public class Apple{
	public static int SIZE = SnakeBody.SIZE;

	public int x, y;

	public Apple(int x, int y){
		this.x = x;
		this.y = y;
	}

	public void render(ICanvas canvas, int appleColor, int internalColor){
		double centerX = (x + 0.5) * SIZE;
		double centerY = (y + 0.5) * SIZE;
		Apple.renderApple(canvas, centerX, centerY, SIZE, appleColor, internalColor);
	}

	public static void renderApple(ICanvas canvas, double centerX, double centerY, int size, int fillColor, int internalColor){
		double pulse = 1.0 + 0.15 * Math.sin(System.currentTimeMillis() / 200.0);
		double currentSize = (size * 0.7) * pulse;

		canvas.save();
		canvas.setEffect(currentSize * 0.4 * pulse, fillColor);

		double[] xPoints = {centerX, centerX+currentSize/2, centerX, centerX-currentSize/2};
		double[] yPoints = {centerY-currentSize/2, centerY, centerY+currentSize/2, centerY};
		canvas.fillPolygon(xPoints, yPoints, 4, fillColor);

		canvas.clearEffect();
		double sparkSize = currentSize * 0.3;
		double[] xSpark = {centerX, centerX+sparkSize/2, centerX, centerX-sparkSize/2};
		double[] ySpark = {centerY-sparkSize/2, centerY, centerY+sparkSize/2, centerY};
		canvas.fillPolygon(xSpark, ySpark, 4, internalColor);

		canvas.restore();
	}
}
