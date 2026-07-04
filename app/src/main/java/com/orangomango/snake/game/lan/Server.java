package com.orangomango.snake.game.lan;

import android.util.Log;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class Server{
	private ServerSocket server;
	private String host;
	private int port;

	public Server(String host, int port){
		try {
			this.host = host;
			this.port = port;
			this.server = new ServerSocket(this.port, 10, InetAddress.getByName(this.host));
		} catch (Exception ex){
			ex.printStackTrace();
		}
	}

	public void start(){
		Log.d("LANDebug", String.format("Server started at %s:%d", this.host, this.port));
		while (!this.server.isClosed()){
			try {
				Socket socket = this.server.accept();
				Log.d("LANDebug", "Client connected");
				new Thread(new Manager(socket)).start();
			} catch (IOException ex){
				ex.printStackTrace();
			}
		}
	}
}
