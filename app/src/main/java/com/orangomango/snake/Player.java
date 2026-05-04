package com.orangomango.snake;

import android.app.Activity;
import android.content.Context;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.android.gms.games.AchievementsClient;
import com.google.android.gms.games.LeaderboardsClient;
import com.google.android.gms.games.PlayGames;
import com.orangomango.account.Account;
import com.orangomango.androidbridge.FileHelper;
import com.orangomango.snake.game.GameDifficulty;

import static com.orangomango.snake.screen.HomeScreen.PURCHASE_HANDLER;
import static com.orangomango.snake.screen.GameScreen.LEADERBOARD_EASY;
import static com.orangomango.snake.screen.GameScreen.LEADERBOARD_EXTREME;
import static com.orangomango.snake.screen.GameScreen.LEADERBOARD_HARD;
import static com.orangomango.snake.screen.GameScreen.LEADERBOARD_MEDIUM;

public class Player{
	private JSONObject json;
	private Context context;

	public Player(Context context){
		this.context = context;
		try {
			String savedData = FileHelper.readInternalFile(this.context, "usersave.json");
			if (savedData == null){
				this.json = new JSONObject();
				this.json.put("snakeIndex", 0);
				this.json.put("appleIndex", 0);
				this.json.put("appdata", new JSONObject());
			} else {
				this.json = new JSONObject(savedData);
			}
		} catch (JSONException ex){
			ex.printStackTrace();
		}

		PURCHASE_HANDLER.updatePlayer(this);
	}

	public void save(){
		FileHelper.writeInternalFile(this.context, "usersave.json", this.json.toString());
	}

	public void unlockPremiumSkins(int totalSnakes, int totalApples){
		try {
			String jsonData = FileHelper.readRawResource(this.context, R.raw.customize);
			JSONObject json = new JSONObject(jsonData);

			// Unlock snake skins
			for (int i = 0; i < totalSnakes; i++){
				String purchaseId = json.getJSONObject("snake").getJSONObject("colors").getJSONObject(String.valueOf(i)).optString("purchaseId");
				if (!purchaseId.isEmpty()){
					setPermanentSnakeIndex(i);
				}
			}

			// Unlock apple skins
			for (int i = 0; i < totalApples; i++){
				String purchaseId = json.getJSONObject("apple").getJSONObject("colors").getJSONObject(String.valueOf(i)).optString("purchaseId");
				if (!purchaseId.isEmpty()){
					setPermanentAppleIndex(i);
				}
			}
		} catch (JSONException ex){
			ex.printStackTrace();
		}
	}

	// Ad-block enabled
	public void setAdBlock(){
		FileHelper.writeInternalFile(this.context, "adblock.lock", "true");
	}

	public boolean isAdBlockEnabled(){
		String data = FileHelper.readInternalFile(this.context, "adblock.lock");
		return data != null && data.equals("true");
	}

	public void setPermanentSnakeIndex(int index){
		try {
			JSONArray array = getPermanentData().optJSONArray("permanentSnake");
			if (array == null){
				array = new JSONArray();
			}

			boolean contained = false;
			for (int i = 0; i < array.length(); i++){
				if (array.getInt(i) == index){
					contained = true;
					break;
				}
			}

			if (!contained){
				array.put(index);
				getPermanentData().put("permanentSnake", array);
			}
		} catch (JSONException ex){
			ex.printStackTrace();
		}
	}

	public boolean isPermanentSnake(int index){
		try{
			JSONArray array = getPermanentData().optJSONArray("permanentSnake");
			if (array == null){
				return false;
			}

			for (int i = 0; i < array.length(); i++){
				if (array.getInt(i) == index){
					return true;
				}
			}
		} catch (JSONException ex){
			ex.printStackTrace();
		}

		return false;
	}

	public void setPermanentAppleIndex(int index){
		try {
			JSONArray array = getPermanentData().optJSONArray("permanentApple");
			if (array == null){
				array = new JSONArray();
			}

			boolean contained = false;
			for (int i = 0; i < array.length(); i++){
				if (array.getInt(i) == index){
					contained = true;
					break;
				}
			}

			if (!contained){
				array.put(index);
				getPermanentData().put("permanentApple", array);
			}
		} catch (JSONException ex){
			ex.printStackTrace();
		}
	}

	public boolean isPermanentApple(int index){
		try{
			JSONArray array = getPermanentData().optJSONArray("permanentApple");
			if (array == null){
				return false;
			}

			for (int i = 0; i < array.length(); i++){
				if (array.getInt(i) == index){
					return true;
				}
			}
		} catch (JSONException ex){
			ex.printStackTrace();
		}

		return false;
	}

	public JSONObject getAppData(){
		try{
			return this.json.getJSONObject("appdata");
		} catch (JSONException ex){
			ex.printStackTrace();
			return null;
		}
	}

	public JSONObject getPermanentData(){
		try {
			JSONObject ob = getAppData().optJSONObject("permanentData");
			if (ob == null){
				getAppData().put("permanentData", new JSONObject());
			}

			return getAppData().getJSONObject("permanentData");
		} catch (JSONException ex){
			ex.printStackTrace();
		}

		return null;
	}

	public void syncAndSave(Account account){
		new Thread(() -> {
			if (account != null){
				JSONObject data = account.getAppData();
				if (data != null){
					if (data.length() == 0){
						account.setAppData(getAppData());
					} else {
						if (!account.getUsername().equals(getAppData().optString("username")) || data.optLong("lastSave") >= getAppData().optLong("lastSave", -1)){ // Online is more recent
							try{
								this.json.put("appdata", data);
							} catch (JSONException ex){
								ex.printStackTrace();
							}
						} else {
							account.setAppData(getAppData());
						}
					}
				}
			}

			syncPlayGames(account);
			this.save();
		}).start();
	}

	public void forcePush(Account account){
		try{
			getAppData().put("lastSave", System.currentTimeMillis());
			syncAndSave(account);
		} catch (JSONException ex){
			ex.printStackTrace();
		}
	}

	public int getSnakeIndex(){
		try {
			return this.json.getInt("snakeIndex");
		} catch (JSONException ex){
			ex.printStackTrace();
			return -1;
		}
	}

	public void updateSnakeIndex(int idx){
		try{
			this.json.put("snakeIndex", idx);
		} catch (JSONException ex){
			ex.printStackTrace();
		}
	}

	public int getAppleIndex(){
		try {
			return this.json.getInt("appleIndex");
		} catch (JSONException ex){
			ex.printStackTrace();
			return -1;
		}
	}

	public void updateAppleIndex(int idx){
		try{
			this.json.put("appleIndex", idx);
		} catch (JSONException ex){
			ex.printStackTrace();
		}
	}

	public void updateNotificationDot(int id, boolean deleted){
		try {
			JSONArray array = this.json.optJSONArray("notificationDot");
			if (array == null){
				array = new JSONArray();
			}

			int contained = -1;
			for (int i = 0; i < array.length(); i++){
				if (array.getJSONObject(i).getInt("id") == id){
					contained = i;
					break;
				}
			}

			if (contained == -1){
				array.put(new JSONObject(String.format("{'id': %d, 'deleted': %s}", id, deleted)));
			} else if (deleted){
				array.getJSONObject(contained).put("deleted", deleted); // Set to true
			}


			this.json.put("notificationDot", array);
		} catch (JSONException ex){
			ex.printStackTrace();
		}
	}

	public boolean isNotificationDot(int id){
		try {
			JSONArray array = this.json.optJSONArray("notificationDot");
			if (array == null){
				return false;
			}

			for (int i = 0; i < array.length(); i++){
				if (array.getJSONObject(i).getInt("id") == id){
					return !array.getJSONObject(i).getBoolean("deleted");
				}
			}
		} catch (JSONException ex){
			ex.printStackTrace();
		}

		return false;
	}

	public int getSnakeColor(int id){
		try {
			String jsonData = FileHelper.readRawResource(this.context, R.raw.customize);
			JSONObject json = new JSONObject(jsonData);
			return Integer.parseUnsignedInt(json.getJSONObject("snake").getJSONObject("colors").getJSONObject(String.valueOf(id)).getString("color"), 16);
		} catch (JSONException ex){
			ex.printStackTrace();
			return -1;
		}
	}

	public int getAppleColor(int id){
		try {
			String jsonData = FileHelper.readRawResource(this.context, R.raw.customize);
			JSONObject json = new JSONObject(jsonData);
			return Integer.parseUnsignedInt(json.getJSONObject("apple").getJSONObject("colors").getJSONObject(String.valueOf(id)).getString("color"), 16);
		} catch (JSONException ex){
			ex.printStackTrace();
			return -1;
		}
	}

	public int getAppleInternalColor(int id){
		try {
			String jsonData = FileHelper.readRawResource(this.context, R.raw.customize);
			JSONObject json = new JSONObject(jsonData);
			return Integer.parseUnsignedInt(json.getJSONObject("apple").getJSONObject("colors").getJSONObject(String.valueOf(id)).getString("internalColor"), 16);
		} catch (JSONException ex){
			ex.printStackTrace();
			return -1;
		}
	}

	public void triggerMidGamePlayAchievement(String gameMode, int currentScore, int totalMangoes){
		AchievementsClient achievementsClient = PlayGames.getAchievementsClient((Activity) this.context);

		achievementsClient.setSteps(PlayAchievement.MANGOEATER_1.getId(), totalMangoes);
		achievementsClient.setSteps(PlayAchievement.MANGOEATER_2.getId(), totalMangoes);
		achievementsClient.setSteps(PlayAchievement.MANGOEATER_3.getId(), totalMangoes);
		achievementsClient.setSteps(PlayAchievement.MANGOEATER_4.getId(), totalMangoes);
		achievementsClient.setSteps(PlayAchievement.MANGOEATER_5.getId(), totalMangoes);

		if (gameMode.equals("easy")){
			if (currentScore >= 40) achievementsClient.unlock(PlayAchievement.EASY_1.getId());
			if (currentScore >= 70) achievementsClient.unlock(PlayAchievement.EASY_2.getId());
			if (currentScore >= 110) achievementsClient.unlock(PlayAchievement.EASY_3.getId());
		}
		if (gameMode.equals("medium")){
			if (currentScore >= 50) achievementsClient.unlock(PlayAchievement.MEDIUM_1.getId());
			if (currentScore >= 100) achievementsClient.unlock(PlayAchievement.MEDIUM_2.getId());
			if (currentScore >= 150) achievementsClient.unlock(PlayAchievement.MEDIUM_3.getId());
		}
		if (gameMode.equals("hard")){
			if (currentScore >= 20) achievementsClient.unlock(PlayAchievement.HARD_1.getId());
			if (currentScore >= 40) achievementsClient.unlock(PlayAchievement.HARD_2.getId());
			if (currentScore >= 60) achievementsClient.unlock(PlayAchievement.HARD_3.getId());
		}
		if (gameMode.equals("extreme")){
			if (currentScore >= 10) achievementsClient.unlock(PlayAchievement.EXTREME_1.getId());
			if (currentScore >= 20) achievementsClient.unlock(PlayAchievement.EXTREME_2.getId());
			if (currentScore >= 30) achievementsClient.unlock(PlayAchievement.EXTREME_3.getId());
		}
	}

	public void syncPlayAchievements(Account account, int totalMangoes, int totalRounds, String gameMode, int currentScore, boolean gameWon){
		AchievementsClient achievementsClient = PlayGames.getAchievementsClient((Activity) this.context);

		if (currentScore == -1){
			triggerMidGamePlayAchievement("easy", getAppData().optInt("high_easy"), totalMangoes);
			triggerMidGamePlayAchievement("medium", getAppData().optInt("high_medium"), totalMangoes);
			triggerMidGamePlayAchievement("hard", getAppData().optInt("high_hard"), totalMangoes);
			triggerMidGamePlayAchievement("extreme", getAppData().optInt("high_extreme"), totalMangoes);
		} else {
			triggerMidGamePlayAchievement(gameMode, currentScore, totalMangoes);
		}

		int[] easySize = GameDifficulty.calculateGridSize(GameDifficulty.EASY.getCellSize());
		int[] mediumSize = GameDifficulty.calculateGridSize(GameDifficulty.MEDIUM.getCellSize());
		int[] hardSize = GameDifficulty.calculateGridSize(GameDifficulty.HARD.getCellSize());
		int[] extremeSize = GameDifficulty.calculateGridSize(GameDifficulty.EXTREME.getCellSize());

		double easyProgress = (getAppData().optInt("high_easy")+3.0) / (easySize[0]*easySize[1]);
		double mediumProgress = (getAppData().optInt("high_medium")+3.0) / (mediumSize[0]*mediumSize[1]);
		double hardProgress = (getAppData().optInt("high_hard")+3.0) / (hardSize[0]*hardSize[1]);
		double extremeProgress = (getAppData().optInt("high_extreme")+3.0) / (extremeSize[0]*extremeSize[1]);

		if (gameWon || easyProgress == 1.0 || mediumProgress == 1.0 || hardProgress == 1.0 || extremeProgress == 1.0){
			achievementsClient.unlock(PlayAchievement.WORLD_EATER.getId());
		}

		achievementsClient.setSteps(PlayAchievement.GAMEOVER_1.getId(), totalRounds);
		achievementsClient.setSteps(PlayAchievement.GAMEOVER_2.getId(), totalRounds);
		achievementsClient.setSteps(PlayAchievement.GAMEOVER_3.getId(), totalRounds);

		if (account != null){
			achievementsClient.unlock(PlayAchievement.CONNECTED.getId());

			JSONArray friends = account.listFriends();
			if (friends != null){
				achievementsClient.setSteps(PlayAchievement.FRIENDS_1.getId(), friends.length());
				achievementsClient.setSteps(PlayAchievement.FRIENDS_2.getId(), friends.length());
			}
		}
	}

	private void syncPlayGames(Account account){
		int easy = getAppData().optInt("high_easy");
		int medium = getAppData().optInt("high_medium");
		int hard = getAppData().optInt("high_hard");
		int extreme = getAppData().optInt("high_extreme");

		// Update local leaderboards
		if (account != null){
			if (easy != 0) account.updateLeaderboard("easy", new long[]{easy, -System.currentTimeMillis()});
			if (medium != 0) account.updateLeaderboard("medium", new long[]{medium, -System.currentTimeMillis()});
			if (hard != 0) account.updateLeaderboard("hard", new long[]{hard, -System.currentTimeMillis()});
			if (extreme != 0) account.updateLeaderboard("extreme", new long[]{extreme, -System.currentTimeMillis()});
		}

		LeaderboardsClient leaderboardsClient = PlayGames.getLeaderboardsClient((Activity) this.context);
		if (easy != 0) leaderboardsClient.submitScore(LEADERBOARD_EASY, easy);
		if (medium != 0) leaderboardsClient.submitScore(LEADERBOARD_MEDIUM, medium);
		if (hard != 0) leaderboardsClient.submitScore(LEADERBOARD_HARD, hard);
		if (extreme != 0) leaderboardsClient.submitScore(LEADERBOARD_EXTREME, extreme);

		int totalMangoes = getAppData().optInt("mangoes");
		int totalRounds = getAppData().optInt("rounds");
		syncPlayAchievements(account, totalMangoes, totalRounds, null, -1, false);
	}
}
