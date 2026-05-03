package com.orangomango.androidbridge;

import 	android.content.Context;
import java.io.*;
import java.nio.charset.StandardCharsets;

public class FileHelper{
	public static String readRawResource(Context context, int resourceId){
		StringBuilder sb = new StringBuilder();
		try {
			InputStream is = context.getResources().openRawResource(resourceId);
			BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
			String line;
			while ((line = reader.readLine()) != null) {
				sb.append(line).append("\n");
			}
		} catch (IOException e){
			e.printStackTrace();
		}
		return sb.toString();
	}

	public static void writeInternalFile(Context context, String fileName, String content){
		File file = new File(context.getFilesDir(), fileName);

		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(file));
			writer.write(content);
			writer.close();
		} catch (IOException ex){
			ex.printStackTrace();
		}
	}

	public static String readInternalFile(Context context, String fileName){
		File file = new File(context.getFilesDir(), fileName);
		if (!file.exists()) return null;

		StringBuilder sb = new StringBuilder();
		
		try {
			BufferedReader reader = new BufferedReader(new FileReader(file));
			String line = null;
			int lineCount = 0;
			while ((line = reader.readLine()) != null){
				if (lineCount > 0) sb.append("\n");
				sb.append(line);
				lineCount++;
			}
			reader.close();
		} catch (IOException ex){
			ex.printStackTrace();
		}
		
		return sb.toString();
	}

	public static void deleteInternalFile(Context context, String fileName){
		File file = new File(context.getFilesDir(), fileName);
		if (file.exists()){
			file.delete();
		}
	}
}