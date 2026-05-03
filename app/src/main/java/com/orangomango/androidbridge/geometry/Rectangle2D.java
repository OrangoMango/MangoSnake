package com.orangomango.androidbridge.geometry;

import java.util.function.DoubleFunction;

public class Rectangle2D{
	private double x, y, width, height;

	public Rectangle2D(double x, double y, double w, double h){
		this.x = x;
		this.y = y;
		this.width = w;
		this.height = h;
	}

	public boolean contains(double x, double y){
		return x >= getMinX() && y >= getMinY() && x <= getMaxX() && y <= getMaxY();
	}

	public boolean intersects(Rectangle2D other){
		return other.getMaxX() >= this.getMinX() && other.getMaxY() >= this.getMinY() && other.getMinX() <= this.getMaxX() && other.getMinY() <= this.getMaxY();
	}

	public Rectangle2D scale(DoubleFunction<Double> fX, DoubleFunction<Double> fY){
		return new Rectangle2D(fX.apply(this.x), fY.apply(this.y), fX.apply(this.width), fY.apply(this.height));
	}

	public Rectangle2D translate(double x, double y){
		return new Rectangle2D(this.x + x, this.y + y, this.width, this.height);
	}

	public double getMinX(){
		return this.x;
	}

	public double getMinY(){
		return this.y;
	}

	public double getMaxX(){
		return this.x + getWidth();
	}

	public double getMaxY(){
		return this.y + getHeight();
	}

	public double getWidth(){
		return this.width;
	}

	public double getHeight(){
		return this.height;
	}

	public void setWidth(double width){
		this.width = width;
	}

	public void setHeight(double height){
		this.height = height;
	}
}
