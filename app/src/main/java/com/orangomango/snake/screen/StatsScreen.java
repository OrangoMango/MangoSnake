package com.orangomango.snake.screen;

import static com.orangomango.snake.GameView.AUDIO;

import android.app.Activity;
import android.graphics.LinearGradient;
import android.graphics.Shader;

import com.orangomango.account.Account;
import com.orangomango.androidbridge.ICanvas;
import com.orangomango.androidbridge.PointerEvent;
import com.orangomango.androidbridge.geometry.Rectangle2D;
import com.orangomango.androidbridge.geometry.TextAlignment;

import com.orangomango.androidbridge.util.Pair;
import com.orangomango.snake.GameView;
import com.orangomango.snake.Player;
import com.orangomango.snake.game.Apple;
import com.orangomango.snake.game.GameDifficulty;
import com.orangomango.snake.ui.UiElement;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class StatsScreen extends Screen{
	private static int SCREEN_W, SCREEN_H;

	private final Rectangle2D backButton = new Rectangle2D(0.075, 0.02, 0.25, 0.06);
	private String headers = "Loading...", data = "Loading...";
	private int appleColor, appleInternalColor;

	private ArrayList<ArrayList<Pair<String, long[]>>> leaderboards;
	private HomeScreen.GlobalSettings globalSettings;
	private Account account;
	private Player player;
	private double dragOffset, lastDragY;

	public StatsScreen(GameView gameView, Player player, Account account, ArrayList<ArrayList<Pair<String, long[]>>> leads, HomeScreen.GlobalSettings gset, double skinsUnlockedProgress, int totalSkins){
		super(gameView);

		this.account = account;
		this.player = player;
		this.leaderboards = leads;
		this.globalSettings = gset;
		this.appleColor = this.player.getAppleColor(player.getAppleIndex());
		this.appleInternalColor = this.player.getAppleInternalColor(player.getAppleIndex());

		new Thread(() -> buildDataString(skinsUnlockedProgress, totalSkins)).start();
	}

	private void buildDataString(double skinsUnlockedProgress, int totalSkins){
		StringBuilder builder = new StringBuilder();
		StringBuilder dataBuilder = new StringBuilder();
		JSONObject userData = player.getAppData();

		builder.append("Username:\n");
		builder.append("Tag:\n");
		builder.append("MangoGames ID friends:\n");
		builder.append("Server sync:\n");
		builder.append("Local sync:\n\n");

		builder.append("Games played:\n");
		builder.append("Time played:\n");
		builder.append("Mangoes eaten:\n");
		builder.append("Skins owned:\n");
		builder.append("Longest snake length:\n\n");

		builder.append("Easy mode highscore:\n");
		builder.append("Medium mode highscore:\n");
		builder.append("Hard mode highscore:\n");
		builder.append("Extreme mode highscore:\n\n");

		builder.append("Longest survived in easy:\n");
		builder.append("Longest survived in medium:\n");
		builder.append("Longest survived in hard:\n");
		builder.append("Longest survived in extreme:\n\n");

		builder.append("Easy avg, 30 games:\n");
		builder.append("Medium avg, 30 games:\n");
		builder.append("Hard avg, 30 games:\n");
		builder.append("Extreme avg, 30 games:\n\n");

		builder.append("Easy mode completed:\n");
		builder.append("Medium mode completed:\n");
		builder.append("Hard mode completed:\n");
		builder.append("Extreme mode completed:\n\n");

		// Part 1
		SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yy HH:mm:ss", Locale.getDefault());
		String username = account == null ? null : account.getUsername();
		dataBuilder.append(String.format("%s\n", username == null ? "--" : username));
		String tag = account == null ? null : account.getTag();
		dataBuilder.append(String.format("%s\n", tag == null ? "--" : "#"+tag));
		JSONArray friends = account == null ? null : account.listFriends();
		dataBuilder.append(String.format("%d\n", friends == null ? 0 : friends.length()));
		if (account != null){
			JSONObject appData = account.getAppData();
			if (appData != null){
				dataBuilder.append(sdf.format(new Date(appData.optLong("lastSave")))).append("\n");
			} else {
				dataBuilder.append("--\n");
			}
		} else {
			dataBuilder.append("--\n");
		}
		dataBuilder.append(sdf.format(new Date(userData.optLong("lastSave")))).append("\n\n");

		// Part 2
		dataBuilder.append(String.format("%d\n", userData.optInt("rounds")));
		int seconds = userData.optInt("timePlayed");
		dataBuilder.append(String.format("%02dh %02dmin %02ds\n", seconds / 3600, (seconds / 60) % 60, seconds % 60));
		dataBuilder.append(String.format("%d\n", userData.optInt("mangoes")));
		dataBuilder.append(String.format("%d/%d\n", (int)Math.ceil(skinsUnlockedProgress*totalSkins), totalSkins));
		dataBuilder.append(String.format("%d\n\n", Math.max(Math.max(userData.optInt("high_easy"), userData.optInt("high_medium")), Math.max(userData.optInt("high_hard"), userData.optInt("high_extreme")))+3));

		// Part 3
		dataBuilder.append(String.format("%d\n", userData.optInt("high_easy")));
		dataBuilder.append(String.format("%d\n", userData.optInt("high_medium")));
		dataBuilder.append(String.format("%d\n", userData.optInt("high_hard")));
		dataBuilder.append(String.format("%d\n\n", userData.optInt("high_extreme")));

		// Part 4
		int easyTime = userData.optInt("longestGameTime_easy");
		int mediumTime = userData.optInt("longestGameTime_medium");
		int hardTime = userData.optInt("longestGameTime_hard");
		int extremeTime = userData.optInt("longestGameTime_extreme");

		dataBuilder.append(String.format("%02dmin %02ds\n", (easyTime / 60) % 60, easyTime % 60));
		dataBuilder.append(String.format("%02dmin %02ds\n", (mediumTime / 60) % 60, mediumTime % 60));
		dataBuilder.append(String.format("%02dmin %02ds\n", (hardTime / 60) % 60, hardTime % 60));
		dataBuilder.append(String.format("%02dmin %02ds\n\n", (extremeTime / 60) % 60, extremeTime % 60));

		double avgEasy = (double) userData.optInt("totalScore_easy")/userData.optInt("totalRounds_easy");
		double avgMedium = (double) userData.optInt("totalScore_medium")/userData.optInt("totalRounds_medium");
		double avgHard = (double) userData.optInt("totalScore_hard")/userData.optInt("totalRounds_hard");
		double avgExtreme = (double) userData.optInt("totalScore_extreme")/userData.optInt("totalRounds_extreme");

		// Part 5
		dataBuilder.append(Double.isFinite(avgEasy) ? String.format("%.2f (%d games)\n", avgEasy, userData.optInt("totalRounds_easy")) : "-- (0 games)\n");
		dataBuilder.append(Double.isFinite(avgMedium) ? String.format("%.2f (%d games)\n", avgMedium, userData.optInt("totalRounds_medium")) : "-- (0 games)\n");
		dataBuilder.append(Double.isFinite(avgHard) ? String.format("%.2f (%d games)\n", avgHard, userData.optInt("totalRounds_hard")) : "-- (0 games)\n");
		dataBuilder.append(Double.isFinite(avgExtreme) ? String.format("%.2f (%d games)\n\n", avgExtreme, userData.optInt("totalRounds_extreme")) : "-- (0 games)\n\n");

		// Part 6
		int[] easySize = GameDifficulty.calculateGridSize(GameDifficulty.EASY.getCellSize());
		int[] mediumSize = GameDifficulty.calculateGridSize(GameDifficulty.MEDIUM.getCellSize());
		int[] hardSize = GameDifficulty.calculateGridSize(GameDifficulty.HARD.getCellSize());
		int[] extremeSize = GameDifficulty.calculateGridSize(GameDifficulty.EXTREME.getCellSize());

		double easyProgress = (userData.optInt("high_easy")+3.0) / (easySize[0]*easySize[1]);
		double mediumProgress = (userData.optInt("high_medium")+3.0) / (mediumSize[0]*mediumSize[1]);
		double hardProgress = (userData.optInt("high_hard")+3.0) / (hardSize[0]*hardSize[1]);
		double extremeProgress = (userData.optInt("high_extreme")+3.0) / (extremeSize[0]*extremeSize[1]);

		dataBuilder.append(String.format("%s (%.2f%%)\n", easyProgress == 1.0, easyProgress*100));
		dataBuilder.append(String.format("%s (%.2f%%)\n", mediumProgress == 1.0, mediumProgress*100));
		dataBuilder.append(String.format("%s (%.2f%%)\n", hardProgress == 1.0, hardProgress*100));
		dataBuilder.append(String.format("%s (%.2f%%)\n", extremeProgress == 1.0, extremeProgress*100));

		this.headers = builder.toString();
		this.data = dataBuilder.toString();
	}

	@Override
	public void goBack(Activity activity){
		CustomizeScreen cs = new CustomizeScreen(this.gameView, this.player, this.account, this.leaderboards, this.globalSettings);
		this.gameView.setScreen(cs);
	}

	@Override
	public void handleInput(PointerEvent event){
		switch (event.type){
			case PRESSED:
				if (this.backButton.scale(UiElement::rw, UiElement::rh).contains(event.x, event.y)){
					this.gameView.triggerVibration(100);
					AUDIO.playSound("gui");
					goBack(null);
				}

				this.lastDragY = event.y;
				break;

			case DRAGGED:
				double delta = event.y - this.lastDragY;
				delta = Math.clamp(delta, -rsh(0.23), rsh(0.23));
				this.dragOffset = Math.min(rsh(0.46), Math.max(this.dragOffset - delta * 0.19, 0));
				break;

			case RELEASED:
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

		// Render back button
		canvas.setEffect(10, 0xFFF472B6);
		canvas.fillText("< Back To Customization", rsw(this.backButton.getMinX()), rsh(this.backButton.getMaxY()-this.backButton.getHeight()*0.2), 0xFFF472B6, UiElement.FONT_LARGE, TextAlignment.LEFT);
		canvas.clearEffect();

		canvas.translate(0, -this.dragOffset);
		canvas.setEffect(10, 0xFF00FF00);
		canvas.fillText(this.headers, rsw(0.41), rsh(0.075), 0xFF00FF00, UiElement.FONT_LARGE, TextAlignment.LEFT);
		canvas.clearEffect();
		canvas.setEffect(10, 0xFFFFFFFF);
		canvas.fillText(this.data, rsw(0.85), rsh(0.075), 0xFFFFFFFF, UiElement.FONT_LARGE, TextAlignment.CENTER);
		canvas.clearEffect();
		canvas.translate(0, this.dragOffset);

		if (this.dragOffset < rsh(0.46)){ // Bottom (update rsh of handleInput too!)
			double fadeHeight = rsh(0.3);
			LinearGradient gradient = new LinearGradient(0, SCREEN_H - (float) fadeHeight, 0, SCREEN_H - (float) fadeHeight*0.15f, 0x00020617, 0xFF020617, Shader.TileMode.CLAMP);
			canvas.setShader(gradient);
			canvas.fillRect(rsw(0.4), SCREEN_H - fadeHeight, rsw(0.6), fadeHeight, 0xFF000000);
			canvas.setShader(null);
		}

		if (this.dragOffset > 0){ // Top
			double fadeHeight = rsh(0.3);
			LinearGradient gradient = new LinearGradient(0, (float) fadeHeight*0.15f, 0, (float) fadeHeight, 0xFF020617, 0x00020617, Shader.TileMode.CLAMP);
			canvas.setShader(gradient);
			canvas.fillRect(rsw(0.4), 0, rsw(0.6), fadeHeight, 0xFF000000);
			canvas.setShader(null);
		}

		Apple.renderApple(canvas, rsw(0.20), rsh(0.5), (int)rsw(0.20), this.appleColor, this.appleInternalColor);
	}

	private static double rsw(double x){
		return x * SCREEN_W;
	}

	private static double rsh(double y){
		return y * SCREEN_H;
	}
}
