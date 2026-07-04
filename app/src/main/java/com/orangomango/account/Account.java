package com.orangomango.account;

import java.net.*;
import java.io.*;

import org.json.*;

import static com.orangomango.snake.screen.HomeScreen.PURCHASE_HANDLER;

public class Account{
	public static String HOST = "http://localhost:8000"; // DEFAULT values
	private static String APPLICATION_UID;

	private String uid;
	private String tag;
	private String username, password;

	public Account(String username, String password){
		this.username = username;
		this.password = password;

		PURCHASE_HANDLER.updateAccount(this);
	}

	public static void registerApplication(String appuid){
		APPLICATION_UID = appuid;
	}

	public String getUsername(){
		return this.username;
	}

	public String getPassword(){
		return this.password;
	}

	public String getTag(){
		return this.tag;
	}

	public JSONObject login(){
		try{
			ApiResponse response = sendPostRequest("/account/api/auth.php", String.format("username=%s&password=%s", URLEncoder.encode(this.username), URLEncoder.encode(this.password)));
			if (response != null && response.getCode() == 200){
				JSONObject json = new JSONObject(response.getContent());
				if (json.getBoolean("success")){
					this.uid = json.getJSONObject("data").getString("uid");
					this.tag = json.getJSONObject("data").getString("tag");
				}

				return json;
			}

			return null;
		} catch (JSONException ex){
			ex.printStackTrace();
			return null;
		}
	}

	public void logout(){
		this.uid = null;
	}

	public static String getAppVersion(){
		try{
			ApiResponse response = sendPostRequest("/account/api/version_beta_temp.php", ""); // TODO: rename endpoint, remove 2 server files (_temp)
			if (response != null && response.getCode() == 200){
				JSONObject dt = new JSONObject(response.getContent());
				return dt.getString("data");
			} else {
				return null;
			}
		} catch (JSONException ex){
			ex.printStackTrace();
			return null;
		}
	}

	public JSONArray listFriends(){
		try{
			if (this.uid == null) return null;
			ApiResponse response = sendPostRequest("/account/api/friends.php", "uid=" + this.uid);
			if (response != null && response.getCode() == 200){
				JSONObject json = new JSONObject(response.getContent());
				return json.getJSONArray("data");
			}

			return null;
		} catch (JSONException ex){
			ex.printStackTrace();
			return null;
		}
	}

	public JSONObject getAppData(){
		try{
			if (this.uid == null) return null;
			ApiResponse response = sendPostRequest("/account/api/appdata.php?method=read", "uid=" + this.uid);
			if (response != null && response.getCode() == 200){
				JSONObject json = new JSONObject(response.getContent());
				return json.getBoolean("success") ? json.getJSONObject("data") : null;
			}

			return null;
		} catch (JSONException ex){
			ex.printStackTrace();
			return null;
		}
	}

	public boolean setAppData(JSONObject json){
		try{
			if (this.uid == null) return false;
			ApiResponse response = sendPostRequest("/account/api/appdata.php?method=write", "uid=" + this.uid + "&json_data=" + URLEncoder.encode(json.toString(4)));
			if (response != null && response.getCode() == 200){
				JSONObject dt = new JSONObject(response.getContent());
				return dt.getBoolean("success");
			} else {
				return false;
			}
		} catch (JSONException ex){
			ex.printStackTrace();
			return false;
		}
	}

	public static JSONObject getLeaderboard(String leadName){
		try{
			ApiResponse response = sendPostRequest("/leaderboard/lead.php?lead=" + leadName + "&api=1", "");
			if (response != null && response.getCode() == 200){
				JSONObject dt = new JSONObject(response.getContent());
				return dt;
			} else {
				return null;
			}
		} catch (JSONException ex){
			ex.printStackTrace();
			return null;
		}
	}

	public boolean updateLeaderboard(String leadName, long[] score){
		try{
			JSONObject data = new JSONObject();
			data.put("uid", this.uid);
			data.put("lead", leadName);

			JSONArray scores = new JSONArray();
			for (int i = 0; i < score.length; i++){
				scores.put(score[i]);
			}
			data.put("score", scores);

			ApiResponse response = sendPostRequest("/leaderboard/lead.php", "json_data=" + data.toString());

			System.out.println(response.getContent());

			if (response != null && response.getCode() == 200){
				JSONObject dt = new JSONObject(response.getContent());
				return dt.getBoolean("success");
			} else {
				return false;
			}
		} catch (JSONException ex){
			ex.printStackTrace();
			return false;
		}
	}

	public boolean logMessage(String message){
		try {
			ApiResponse response = sendPostRequest("/account/api/log.php", "uid=" + this.uid + "&message="+URLEncoder.encode(message));
			if (response != null && response.getCode() == 200){
				JSONObject dt = new JSONObject(response.getContent());
				return dt.getBoolean("success");
			} else {
				return false;
			}
		} catch (JSONException ex){
			ex.printStackTrace();
			return false;
		}
	}

	private static ApiResponse sendPostRequest(String urlText, String content){
		try {
			URL url = new URL(HOST + urlText);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setDoOutput(true);
			conn.setRequestMethod("POST");

			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(conn.getOutputStream()));
			writer.write("appuid="+APPLICATION_UID + "&" + content);
			writer.close();

			int responseCode = conn.getResponseCode();

			StringBuilder builder = new StringBuilder();
			InputStream is = (responseCode >= 200 && responseCode < 300) ? conn.getInputStream() : conn.getErrorStream();

			if (is != null){
				BufferedReader reader = new BufferedReader(new InputStreamReader(is));
				String line;
				while ((line = reader.readLine()) != null){
					builder.append(line);
				}
			}

			return new ApiResponse(responseCode, builder.toString());
		} catch (Exception ex){
			ex.printStackTrace();
			return null;
		}
	}
}
