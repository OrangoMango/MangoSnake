package com.orangomango.snake.game;

import static com.orangomango.snake.GameView.HEIGHT;

import java.util.*;
import java.util.function.Consumer;

import com.orangomango.androidbridge.geometry.Side;
import com.orangomango.snake.game.ai.Cycle;
import com.orangomango.snake.game.ai.Point;

public class Game{
	private String gameMode;
	private int timeInterval;
	private boolean wrap, ai;

	private Map<String, List<SnakeBody>> snake = new HashMap<>();
	private volatile Apple apple;
	private Random random = new Random();
	private Map<String, Side> direction = new HashMap<>();
	private Map<String, Integer> score = new HashMap<>();
	private GameWorld gameWorld;
	private volatile boolean paused = false, gameFinished = false, allowMovement = true;
	private volatile long lastFrameTime, pauseStartTime;
	private int steps;
	private boolean threadRunning = true;
	private long gameStartTime;

	private Runnable gameResetEvent;
	private Consumer<Integer> scoreEvent, gameOverEvent;

	public Game(String gameMode, int timeInterval, boolean ai, boolean wrap){
		this.gameMode = gameMode;
		this.timeInterval = timeInterval;
		this.ai = ai;
		this.wrap = wrap;
	}

	public void addPlayer(String playerId){
		this.snake.put(playerId, new ArrayList<SnakeBody>()); // Init the snake array
	}

	private void resetSnakeBodies(){
		int counter = 0;
		for (List<SnakeBody> snakeBody : this.snake.values()){
			synchronized (this){
				snakeBody.clear();
				snakeBody.add(new SnakeBody(7, 5+counter));
				snakeBody.add(new SnakeBody(6, 5+counter));
				snakeBody.add(new SnakeBody(5, 5+counter));
			}
			counter += 2; // TODO
		}

		for (Map.Entry<String, List<SnakeBody>> entry : this.snake.entrySet()){
			this.direction.put(entry.getKey(), Side.RIGHT);
			this.score.put(entry.getKey(), 0);
		}
	}

	public void restoreGameState(){
		this.gameWorld = new GameWorld((int) Math.floor(HEIGHT * 1.5 /SnakeBody.SIZE), HEIGHT/SnakeBody.SIZE); // WIDTH is not the same as HomeScreen! (ratio: 3:2)
		resetSnakeBodies();

		this.gameFinished = false;
		this.steps = 0;
		this.gameStartTime = System.currentTimeMillis();

		this.gameResetEvent.run();

		generateApple();
	}

	public void initGame(int size){
		// WIDTH is not the same as HomeScreen! (ratio: 3:2)
		SnakeBody.SIZE = Apple.SIZE = (int) Math.floor(size * (HEIGHT / 600.0)); // Fix size due to screen_size
		this.gameWorld = new GameWorld((int) Math.floor(HEIGHT * 1.5 / SnakeBody.SIZE), HEIGHT/SnakeBody.SIZE);

		resetSnakeBodies();
		generateApple();
		this.gameStartTime = System.currentTimeMillis();

		Thread gameThread = new Thread(() -> {
			while (this.threadRunning){
				try {
					if (this.paused || this.gameFinished){
						Thread.sleep(10);
						continue;
					}

					if (System.nanoTime() - this.lastFrameTime >= (long) this.timeInterval*1000000){
						for (Map.Entry<String, List<SnakeBody>> entry : this.snake.entrySet()){
							List<SnakeBody> snakeBody = entry.getValue();

							SnakeBody head;
							synchronized (this){
								head = snakeBody.get(0);
							}

							if (this.ai){
								final Cycle cycle = this.gameWorld.getCycle();
								final Point nextPoint = cycle.getNextPoint(head.x, head.y);
								if (this.apple == null){
									setDirection(nextPoint, head); // The game has finished so just follow the tail
								} else {
									Point bestPoint = getBestPoint(head);
									if ((bestPoint.equals(nextPoint) && snakeBody.stream().filter(sb -> sb.x == bestPoint.x && sb.y == bestPoint.y).findAny().isEmpty()) || isSafe(snakeBody, bestPoint)){
										setDirection(bestPoint, head);
									} else {
										setDirection(nextPoint, head);
									}
								}
							}

							SnakeBody next = getNext(entry.getKey(), head);
							boolean dead = false;
							for (int i = 0; i < snakeBody.size(); i++){
								SnakeBody body = snakeBody.get(i);
								if (head != body && head.x == body.x && head.y == body.y){
									dead = true;
									break;
								}
							}

							if (this.wrap){
								next.wrap(this.gameWorld.getWidth(), this.gameWorld.getHeight());
							} else if (next.outside(this.gameWorld.getWidth(), this.gameWorld.getHeight())){
								dead = true;
							}

							if (dead){
								this.gameFinished = true;
								this.gameOverEvent.accept(this.score.get(entry.getKey()));
								continue;
							}

							boolean appleFlag = false;

							synchronized (this){
								if (this.apple != null && next.x == this.apple.x && next.y == this.apple.y){
									appleFlag = true;
								} else {
									snakeBody.remove(snakeBody.size() - 1);
								}
								next.setMove(0);
								snakeBody.add(0, next);
							}

							if (appleFlag){ // Outside of the synchronized block
								this.score.put(entry.getKey(), this.score.get(entry.getKey())+1);
								this.scoreEvent.accept(this.score.get(entry.getKey()));
								generateApple();
							}
						}

						this.lastFrameTime = System.nanoTime();

						if (this.apple != null) this.steps++;
						this.allowMovement = true;
					}

					double moveFactor = 0;
					if (!this.paused && !this.gameFinished){
						long lastTick = System.nanoTime() - this.lastFrameTime;
						moveFactor = Math.min(1.0, (double) lastTick / (this.timeInterval * 1000000));
					} else if (this.paused){
						long lastTick = this.pauseStartTime - this.lastFrameTime;
						moveFactor = Math.min(1.0, (double) lastTick / (this.timeInterval * 1000000));
					}

					for (List<SnakeBody> snakeBody : this.snake.values()){ // Move each snake
						synchronized (this){
							snakeBody.get(0).setMove(Math.max(0, moveFactor));
							snakeBody.get(snakeBody.size()-1).setMove(Math.max(0, moveFactor));
						}
					}

					Thread.sleep(5);
				} catch (InterruptedException ex){
					ex.printStackTrace();
				}
			}
		});
		gameThread.setDaemon(true);
		gameThread.start();
	}

	private SnakeBody getNext(String playerId, SnakeBody head){
		switch (this.direction.get(playerId)){
			case TOP:
				return new SnakeBody(head.x, head.y-1);
			case BOTTOM:
				return new SnakeBody(head.x, head.y+1);
			case LEFT:
				return new SnakeBody(head.x-1, head.y);
			case RIGHT:
				return new SnakeBody(head.x+1, head.y);
			default:
				return null;
		}
	}

	private void generateApple(){
		Apple apple = new Apple(random.nextInt(this.gameWorld.getWidth()), random.nextInt(this.gameWorld.getHeight()));
		for (List<SnakeBody> snakeBody : this.snake.values()){
			for (int i = 0; i < snakeBody.size(); i++){
				SnakeBody sb = snakeBody.get(i);
				if ((sb.x == apple.x && sb.y == apple.y)){
					if (snakeBody.size() < this.gameWorld.getWidth()*this.gameWorld.getHeight()){
						generateApple();
						return;
					} else {
						apple = null;
						/*new Thread(() -> {
							try {
								Thread.sleep(2200);
								this.gameFinished = true;
								resetGame(true);
							} catch (InterruptedException ex){
								ex.printStackTrace();
							}
						}).start();*/
						break;
					}
				}
			}
		}
		this.apple = apple;
	}

	private void setDirection(Point point, SnakeBody head){ // TODO: hardcoded
		if (point.x > head.x && this.direction.get("player_1") != Side.LEFT){
			this.direction.put("player_1", Side.RIGHT);
		}
		if (point.x < head.x && this.direction.get("player_1") != Side.RIGHT){
			this.direction.put("player_1", Side.LEFT);
		}
		if (point.y > head.y && this.direction.get("player_1") != Side.TOP){
			this.direction.put("player_1", Side.BOTTOM);
		}
		if (point.y < head.y && this.direction.get("player_1") != Side.BOTTOM){
			this.direction.put("player_1", Side.TOP);
		}
	}

	private boolean isSafe(List<SnakeBody> snakeBody, Point point){
		List<SnakeBody> temp = new ArrayList<>(snakeBody);
		if (temp.stream().noneMatch(sb -> sb.x == point.x && sb.y == point.y)){
			temp.remove(temp.size()-1);
			temp.add(0, new SnakeBody(point.x, point.y));
			for (int i = 0; i < snakeBody.size()+5; i++){
				SnakeBody pseudoHead = temp.get(0);
				Point pseudoPoint = this.gameWorld.getCycle().getNextPoint(pseudoHead.x, pseudoHead.y);
				if (temp.stream().anyMatch(sb -> sb.x == pseudoPoint.x && sb.y == pseudoPoint.y)){
					return false;
				} else {
					temp.remove(temp.size()-1);
					temp.add(0, new SnakeBody(pseudoPoint.x, pseudoPoint.y));
				}
			}
			return true;
		} else {
			return false;
		}
	}

	private Point getBestPoint(SnakeBody head){
		int[][] dirs = new int[][]{{0, -1}, {1, 0}, {0, 1}, {-1, 0}};
		Point[] options = new Point[4];
		for (int i = 0; i < 4; i++){
			Point newPoint = new Point(head.x+dirs[i][0], head.y+dirs[i][1]);
			options[i] = this.gameWorld.isInsideMap(newPoint.x, newPoint.y) ? newPoint : null;
		}

		final Cycle cycle = this.gameWorld.getCycle();
		final int appleIndex = cycle.getIndex(this.apple.x, this.apple.y);
		int minDistance = Integer.MAX_VALUE;
		Point bestPoint = null;
		for (int i = 0; i < 4; i++){
			Point opt = options[i];
			if (opt != null){
				int distance = cycle.getCost(cycle.getIndex(opt.x, opt.y), appleIndex);
				if (distance < minDistance){
					minDistance = distance;
					bestPoint = opt;
				}
			}
		}

		return bestPoint;
	}

	public void setOnScore(Consumer<Integer> c){
		this.scoreEvent = c;
	}

	public void setOnGameReset(Runnable r){
		this.gameResetEvent = r;
	}

	public void setOnGameOver(Consumer<Integer> c){
		this.gameOverEvent = c;
	}

	public String getGameMode(){
		return this.gameMode;
	}

	public boolean isPaused(){
		return this.paused;
	}

	public void setPaused(boolean v){
		this.paused = v;
	}

	public void togglePause(){
		if (this.gameFinished)
			return;

		if (!this.paused){
			this.pauseStartTime = System.nanoTime();
		} else {
			long pausedDuration = System.nanoTime() - this.pauseStartTime;
			this.lastFrameTime += pausedDuration;
		}

		this.paused = !this.paused;
	}

	public boolean isFinished(){
		return this.gameFinished;
	}

	public void setThreadRunning(boolean v){
		this.threadRunning = v;
	}

	public boolean isAiRunning(){
		return this.ai;
	}

	public void toggleAi(){
		this.ai = !this.ai;
	}

	public GameWorld getGameWorld(){
		return this.gameWorld;
	}

	public int getSteps(){
		return this.steps;
	}

	public long getStartTime(){
		return this.gameStartTime;
	}

	public boolean isAllowMovement(){
		return this.allowMovement;
	}

	public void setAllowMovement(boolean v){
		this.allowMovement = v;
	}

	public int getTimeInterval(){
		return this.timeInterval;
	}

	public void setTimeInterval(int t){
		this.timeInterval = t;
	}

	public Map<String, List<SnakeBody>> getSnake(){
		return this.snake;
	}

	public Map<String, Side> getDirection(){
		return this.direction;
	}

	public int getScore(String playerId){
		return this.score.get(playerId);
	}

	public Apple getApple(){
		return this.apple;
	}
}
