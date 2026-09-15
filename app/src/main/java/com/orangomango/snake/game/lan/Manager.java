package com.orangomango.snake.game.lan;

import android.util.Log;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;

public class Manager implements Runnable{
	private Socket socket;
	private BufferedReader reader;
	private BufferedWriter writer;

	public static final ArrayList<Manager> managers = new ArrayList<>();

	public Manager(Socket socket){
		try {
			this.socket = socket;
			this.reader = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
			this.writer = new BufferedWriter(new OutputStreamWriter(this.socket.getOutputStream()));

			String ip = this.reader.readLine();

			// Add this manager to the list
			Manager.managers.add(this);

			broadcast(String.format("{'event': 'playerCount', 'count': %d, 'host': '%s'}", Manager.managers.size(), ip));
		} catch (IOException ex){
			ex.printStackTrace();
			close();
		}
	}

	private void close(){
		// TODO: notifiy client disconnected

		try {
			if (this.socket != null) this.socket.close();
			if (this.reader != null) this.reader.close();
			if (this.writer != null) this.writer.close();
		} catch (IOException ex){
			ex.printStackTrace();
		}
	}

	private void broadcast(String data){
		Log.d("LANDebug", "Broadcasting: " + data);
		for (Manager manager : Manager.managers){
			try {
				manager.writer.write(data);
				manager.writer.newLine();
				manager.writer.flush();
			} catch (IOException ex){
				ex.printStackTrace();
				manager.close();
			}
		}
	}

	@Override
	public void run(){
		while (this.socket.isConnected()){
			try {
				String data = this.reader.readLine();
				Log.d("LANDebug", "Client received: " + data);
				if (data == null){
					throw new IOException("Client disconnected");
				}

				// TODO: the server has received 'data'
				broadcast(data);
			} catch (IOException ex){
				ex.printStackTrace();
				close();
			}
		}
	}
}
