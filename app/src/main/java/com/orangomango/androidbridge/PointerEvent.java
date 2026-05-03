package com.orangomango.androidbridge;

public class PointerEvent{
	public enum Type { PRESSED, RELEASED, DRAGGED }

	public final float x;
	public final float y;
	public final Type type;

	public PointerEvent(float x, float y, Type type){
		this.x = x;
		this.y = y;
		this.type = type;
	}
}