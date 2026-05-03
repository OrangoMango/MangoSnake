package com.orangomango.snake.game;

import static com.orangomango.snake.GameView.HEIGHT;

public enum GameDifficulty{
	EASY(60, 250, "easy"),
	MEDIUM(30, 150, "medium"),
	HARD(40, 80, "hard"),
	EXTREME(50, 60, "extreme");

	private double cellSize;
	private double speed;
	private String name;

	private GameDifficulty(double cellSize, double speed, String name){
		this.cellSize = cellSize;
		this.speed = speed;
		this.name = name;
	}

	public double getCellSize(){
		return this.cellSize;
	}

	public double getSpeed(){
		return this.speed;
	}

	public String getName(){
		return this.name;
	}

	public static int[] calculateGridSize(double value){
		int size = (int) Math.floor(value * (HEIGHT / 600.0));
		int w = (int) Math.floor(HEIGHT * 1.5 / size);
		int h = HEIGHT / size;

		return new int[]{w, h};
	}
}
