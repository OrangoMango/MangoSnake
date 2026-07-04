package com.orangomango.snake.screen;

import static com.orangomango.snake.GameView.AUDIO;

import android.app.Activity;
import android.util.Log;

import com.orangomango.account.Account;
import com.orangomango.androidbridge.ICanvas;
import com.orangomango.androidbridge.PointerEvent;
import com.orangomango.androidbridge.geometry.Rectangle2D;
import com.orangomango.androidbridge.geometry.TextAlignment;

import com.orangomango.snake.GameView;
import com.orangomango.snake.Player;
import com.orangomango.snake.game.lan.Client;
import com.orangomango.snake.game.lan.Server;
import com.orangomango.snake.ui.Button;
import com.orangomango.snake.ui.InputField;
import com.orangomango.snake.ui.MouseSensible;
import com.orangomango.snake.ui.UiElement;

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;

public class CasualScreen extends Screen{
	private static int SCREEN_W, SCREEN_H;

	private final ArrayList<UiElement> uielements = new ArrayList<>();
	private Account account;
	private Player player;
	private Button connectClient;

	private volatile String localAddress = "127.0.0.1";

	private final Rectangle2D backButton = new Rectangle2D(0.075, 0.02, 0.25, 0.06);
	//private final Rectangle2D screenView = new Rectangle2D(0.075, 0.13, 0.85, 0.80);

	public CasualScreen(GameView gameView, Account account, Player player){
		super(gameView);

		this.account = account;
		this.player = player;

		new Thread(() -> {
			try(final DatagramSocket socket = new DatagramSocket()){
				socket.connect(InetAddress.getByName("8.8.8.8"), 10002);
				this.localAddress = socket.getLocalAddress().getHostAddress();
			} catch (Exception ex){
				ex.printStackTrace();
			}
		}).start();
	}

	@Override
	public void handleInput(PointerEvent event){
		this.uielements.stream().filter(el -> el instanceof MouseSensible).forEach(el -> ((MouseSensible)el).onHover(event.x, event.y));

		switch (event.type){
			case PRESSED:
				if (this.backButton.scale(UiElement::rw, UiElement::rh).contains(event.x, event.y)){
					this.gameView.triggerVibration(100);
					AUDIO.playSound("gui");
					goBack(null);
				}

				this.uielements.stream().filter(el -> el instanceof MouseSensible).forEach(el -> ((MouseSensible)el).onClick(event.x, event.y));
				break;
			case DRAGGED:
				this.uielements.stream().filter(el -> el instanceof MouseSensible).forEach(el -> ((MouseSensible)el).onDrag(event.x, event.y));
				break;
			case RELEASED:
				this.uielements.stream().filter(el -> el instanceof MouseSensible).forEach(el -> ((MouseSensible)el).onRelease(event.x, event.y));
				break;
		}
	}

	@Override
	public void update(int screenWidth, int screenHeight){
		SCREEN_W = screenWidth;
		SCREEN_H = screenHeight;

		if (this.uielements.size() == 0){
			InputField field = new InputField(this.gameView, 0.3, 0.7, 0.15, 0.07, "192.168.141.231");
			this.uielements.add(field);

			Button startServer = new Button(this.gameView, 0xFF0099FF, 0xFF00E5FF, 0.3, 0.50, 0.15, 0.07, "Start Server", UiElement.FONT_MEDIUM, 0xFFFFFFFF, () -> {
				Server server = new Server(this.localAddress, 1234);
				Thread serverThread = new Thread(() -> server.start());
				serverThread.setDaemon(true);
				serverThread.start();
			});
			this.uielements.add(startServer);

			this.connectClient = new Button(this.gameView, 0xFF0099FF, 0xFF00E5FF, 0.55, 0.50, 0.15, 0.07, "Connect Client", UiElement.FONT_MEDIUM, 0xFFFFFFFF, () -> {
				GameScreen gs = new GameScreen(this.gameView, this.account, this.player, null, 250, false, true, 0, false, false);
				gs.initGame(35);

				Thread clientThread = new Thread(() -> {
					Client client = new Client(field.getText().equals("") ? this.localAddress : field.getText(), 1234);
					gs.connect(client);
					if (client.getSocket() != null){
						this.gameView.setScreen(gs);
						client.listen(gs::handleConnectionData);
					} else {
						Log.d("LANDebug", "Error");
						this.connectClient.bounce();
					}
				});
				clientThread.setDaemon(true);
				clientThread.start();
			});
			this.uielements.add(this.connectClient);
		}
	}

	@Override
	public void render(ICanvas canvas){
		canvas.clear(0xFF020617);

		// Render back button
		canvas.setEffect(10, 0xFFF472B6);
		canvas.fillText("< Back To Home", rsw(this.backButton.getMinX()), rsh(this.backButton.getMaxY()-this.backButton.getHeight()*0.2), 0xFFF472B6, UiElement.FONT_LARGE, TextAlignment.LEFT);
		canvas.clearEffect();

		for (int i = 0; i < this.uielements.size(); i++){
			UiElement element = this.uielements.get(i);
			element.render(canvas);
		}

		canvas.fillText(this.localAddress, rsw(0.3), rsh(0.65), 0xFFFFFFFF, UiElement.FONT_MEDIUM, TextAlignment.CENTER);
	}

	@Override
	public void goBack(Activity activity){
		HomeScreen hs = new HomeScreen(this.gameView);
		this.gameView.setScreen(hs);
	}

	private static double rsw(double x){
		return x * SCREEN_W;
	}

	private static double rsh(double y){
		return y * SCREEN_H;
	}
}
