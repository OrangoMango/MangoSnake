package com.orangomango.snake.game.lan;

import android.util.Log;

import java.io.*;
import java.net.Socket;
import java.util.function.Consumer;

public class Client{
	private Socket socket;
	private BufferedReader reader;
	private BufferedWriter writer;

	public Client(String host, int port){
		try {
			this.socket = new Socket(host, port);
			this.reader = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
			this.writer = new BufferedWriter(new OutputStreamWriter(this.socket.getOutputStream()));


		} catch (IOException ex){
			ex.printStackTrace();
			close();
		}
	}

	private void close(){
		try {
			if (this.socket != null) this.socket.close();
			if (this.reader != null) this.reader.close();
			if (this.writer != null) this.writer.close();
		} catch (IOException ex){
			ex.printStackTrace();
		}
	}

	public void send(String data){
		try {
			this.writer.write(data);
			this.writer.newLine();
			this.writer.flush();
		} catch (IOException ex){
			ex.printStackTrace();
			close();
		}
	}

	public void listen(Consumer<String> consumer){
		Log.d("LANDebug", "Client is listening");
		while (this.socket.isConnected()){
			try {
				String data = this.reader.readLine();
				//Log.d("LANDebug", "Client received " + data);
				consumer.accept(data);
			} catch (IOException ex){
				ex.printStackTrace();
				close();
			}
		}
	}

	public Socket getSocket(){
		return this.socket;
	}
}
