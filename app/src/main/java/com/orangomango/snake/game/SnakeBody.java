package com.orangomango.snake.game;

import com.orangomango.androidbridge.ICanvas;

import java.util.List;

import static com.orangomango.snake.GameView.SNAKE_IMAGE;

public class SnakeBody{
	public static int SIZE = 25;

	public static int PI_SNAKE = 28;
	private static final String PI_STRING = "3.1415926535897932384626433832795028841971693993751058209749445923078164062862089986280348253421170679821480865132823066470938446095505822317253594081284811174502841027019385211055596446229489549303819644288109756659334461284756482337867831652712019091456485669234603486104543266482133936072602491412737245870066063155881748815209209628292540917153643678925903600113305305488204665213841469519415116094330572703657595919530921861173819326117931051185480744623799627495673518857527248912279381830119491298336733624406566430860213949463952247371907021798609437027705392171762931767523846748184676694051320005681271452635608277857713427577896091736371787214684409012249534301465495853710507922796892589235420199561121290219608640344181598136297747713099605187072113499999983729780499510597317328160963185950244594553469083026425223082533446850352619311881710100031378387528865875332083814206171776691473035982";

	public static final int SKIN_LAUNCH_ID = 26;
	public static final int SKIN_NOTHING_ID = 29;

	public int x, y;
	private double offset;

	public SnakeBody(int x, int y) {
		this.x = x;
		this.y = y;
	}

	public void render(ICanvas canvas, boolean effect, int pos, List<SnakeBody> snake, int snakeIndex, int neonColor){
		final SnakeBody next = pos == snake.size()-1 ? null : snake.get(pos+1);
		final SnakeBody prev = pos == 0 ? null : snake.get(pos-1);

		int rotation = 0;
		int imgIndex = 0;
		int yIndex = 0;

		// Set image index
		if (snakeIndex == PI_SNAKE){ // PI-snake
			if (pos == 0){
				yIndex = 4;
			} else if (next == null){
				yIndex = 3;
			} else {
				boolean straight = (getWrappedDist(prev.x, next.x) == 0 || getWrappedDist(prev.y, next.y) == 0);
				yIndex = straight ? 0 : 1;
			}

			imgIndex = 0;
			if (pos != 0){
				imgIndex = Integer.parseInt(String.valueOf(PI_STRING.charAt(pos+1)));
			}
		} else {
			if (pos == 0){ // head
				imgIndex = 4;
			} else if (next == null){ // tail
				imgIndex = 3;
			} else {
				boolean straight = (getWrappedDist(prev.x, next.x) == 0 || getWrappedDist(prev.y, next.y) == 0);
				imgIndex = straight ? 0 : 1;
			}
		}

		// Set rotation
		if ((snakeIndex != PI_SNAKE && imgIndex == 1) || yIndex == 1){ // Curve
			int dx1 = getWrappedDist(prev.x, this.x);
			int dy1 = getWrappedDist(prev.y, this.y);
			int dx2 = getWrappedDist(next.x, this.x);
			int dy2 = getWrappedDist(next.y, this.y);

			boolean flag = false;

			if (dx1 == -1 && dy2 == -1){ // LEFT & UP
				rotation = 0;
			} else if (dx2 == -1 && dy1 == -1){
				rotation = 0;
				flag = true;
			} else if (dy1 == -1 && dx2 == 1){ // UP & RIGHT
				rotation = 90;
			} else if (dy2 == -1 && dx1 == 1){
				rotation = 90;
				flag = true;
			} else if (dx1 == 1 && dy2 == 1){ // RIGHT & DOWN
				rotation = 180;
			} else if (dx2 == 1 && dy1 == 1){
				rotation = 180;
				flag = true;
			} else if (dy1 == 1 && dx2 == -1){ // DOWN & LEFT
				rotation = 270;
			} else if (dy2 == 1 && dx1 == -1){
				rotation = 270;
				flag = true;
			}

			if (flag){
				if (snakeIndex == PI_SNAKE){
					yIndex = 2;
				} else {
					imgIndex = 2;
				}
			}
		} else {
			SnakeBody target = (next != null) ? next : prev;
			int dx = getWrappedDist(target.x, this.x);
			int dy = getWrappedDist(target.y, this.y);

			if (dx == -1) rotation = 0;
			else if (dy == -1) rotation = 90;
			else if (dx == 1) rotation = 180;
			else if (dy == 1) rotation = 270;
		}

		canvas.save();
		double renderX = this.x * SIZE;
		double renderY = this.y * SIZE;

		canvas.translate(renderX + SIZE / 2.0, renderY + SIZE / 2.0);
		canvas.rotate(rotation);

		// Interpolate the translation between two tiles
		double trX = 0;
		double trY = 0;
		if (pos == 0 || pos == snake.size()-1){
			double dx = 0;
			double dy = 0;

			if (pos == 0){
				dx = getWrappedDist(next.x, this.x);
				dy = getWrappedDist(next.y, this.y);
			} else if (pos == snake.size()-1){
				dx = getWrappedDist(this.x, prev.x);
				dy = getWrappedDist(this.y, prev.y);
			}

			if (dx == -1){
				trX = this.offset;
			} else if (dx == 1){
				trX = -this.offset;
			} else if (dy == -1){
				trY = this.offset;
			} else if (dy == 1){
				trY = -this.offset;
			}
		}

		if (effect && Math.abs((next == null ? prev.x : next.x)-this.x) <= 1){
			if (pos == 0){
				canvas.setEffect(SIZE*0.8*this.offset, neonColor);
				canvas.fillRect(-SIZE*0.60/2.0 - (1-this.offset)*SIZE, -SIZE*0.60/2.0, SIZE*0.60, SIZE*0.60, snakeIndex == SKIN_NOTHING_ID || snakeIndex == SKIN_LAUNCH_ID ? neonColor : 0);
			} else if (pos == snake.size()-1){
				canvas.setEffect(SIZE*0.8*(1-this.offset), neonColor);
				canvas.fillRect(-SIZE*0.60/2.0 - this.offset*SIZE, -SIZE*0.60/2.0, SIZE*0.60, SIZE*0.60, snakeIndex == SKIN_NOTHING_ID || snakeIndex == SKIN_LAUNCH_ID ? neonColor : 0);
			} else {
				canvas.setEffect(SIZE*0.8, neonColor);
				canvas.fillRect(-SIZE*0.60/2.0, -SIZE*0.60/2.0, SIZE*0.60, SIZE*0.60, snakeIndex == SKIN_NOTHING_ID || snakeIndex == SKIN_LAUNCH_ID ? neonColor : 0);
			}

			canvas.fillRect(0, 0, 0, 0, neonColor); // Reset color for image drawing
			canvas.clearEffect();
		}

		if (trX != 0 || trY != 0){
			if (pos == 0){
				if (this.offset > 0.5 || Math.abs(next.x-this.x) > 1) canvas.drawImage(SNAKE_IMAGE[snakeIndex], 1+imgIndex*34+(1-this.offset)*32, 1+yIndex*34, 32-(1-this.offset)*32, 32, -SIZE/2.0, -SIZE/2.0, SIZE-(1-this.offset)*SIZE, SIZE);
				else canvas.drawImage(SNAKE_IMAGE[snakeIndex], 1+imgIndex*34+16, 1+yIndex*34, 16, 32, -SIZE/2.0-(1-this.offset)*SIZE + SIZE*0.5, -SIZE/2.0, SIZE*0.5, SIZE);

				//if (this.offset > 0.5 || Math.abs(next.x-this.x) > 1) canvas.strokeRect(-SIZE/2.0, -SIZE/2.0, SIZE-(1-this.offset)*SIZE, SIZE, 0xFF0000FF, 3);
				//else canvas.strokeRect(-SIZE/2.0-(1-this.offset)*SIZE + SIZE*0.5, -SIZE/2.0, SIZE*0.5, SIZE, 0xFF0000FF, 3);
			} else if (pos == snake.size()-1){
				if (this.offset < 0.5 || Math.abs(prev.x-this.x) > 1) canvas.drawImage(SNAKE_IMAGE[snakeIndex], 1+imgIndex*34+this.offset*32, 1+yIndex*34, 32-this.offset*32, 32, -SIZE/2.0, -SIZE/2.0, SIZE-this.offset*SIZE, SIZE);
				else canvas.drawImage(SNAKE_IMAGE[snakeIndex], 1+imgIndex*34+16, 1+yIndex*34, 16, 32, -SIZE/2.0-this.offset*SIZE + SIZE*0.5, -SIZE/2.0, SIZE*0.5, SIZE);

				//if (this.offset < 0.5 || Math.abs(prev.x-this.x) > 1) canvas.strokeRect(-SIZE/2.0, -SIZE/2.0, SIZE-this.offset*SIZE, SIZE, 0xFF00FF00, 3);
				//else if (renderX-this.offset*SIZE+SIZE*0.5 + SIZE*0.5 < 15*SIZE) canvas.strokeRect(-SIZE/2.0-this.offset*SIZE + SIZE*0.5, -SIZE/2.0, SIZE*0.5, SIZE, 0xFF00FF00, 3);
			}
		} else {
			canvas.drawImage(SNAKE_IMAGE[snakeIndex], 1+imgIndex*34, 1+yIndex*34, 32, 32, -SIZE/2.0 - 0.5, -SIZE/2.0 - 0.5, SIZE + 1, SIZE + 1);
			//canvas.strokeRect(-SIZE/2.0, -SIZE/2.0, SIZE, SIZE, 0xFFFF0000, 3);
		}

		canvas.restore();
	}

	private static int getWrappedDist(int a, int b){
		int d = a - b;
		if (Math.abs(d) > 1){
			return d > 0 ? -1 : 1;
		}
		return d;
	}

	public void setMove(double v){
		this.offset = v;
	}

	public void wrap(int w, int h){
		if (this.x >= w) this.x = 0;
		if (this.x < 0) this.x = w - 1;
		if (this.y < 0) this.y = h - 1;
		if (this.y >= h) this.y = 0;
	}

	public boolean outside(int w, int h){
		return (this.x >= w || this.x < 0 || this.y < 0 || this.y >= h);
	}
}
