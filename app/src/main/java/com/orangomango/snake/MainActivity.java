package com.orangomango.snake;

import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.games.GamesSignInClient;
import com.google.android.gms.games.PlayGames;
import com.google.android.gms.games.PlayGamesSdk;

import com.orangomango.snake.screen.GameScreen;
import static com.orangomango.snake.GameView.AUDIO;


public class MainActivity extends AppCompatActivity{
	private GameView gameView;
	private BillingManager billingManager;

	@Override
	protected void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);

		makeFullScreen();
		PlayGamesSdk.initialize(this);

		this.billingManager = new BillingManager(this);
		this.gameView = new GameView(this, this.billingManager);

		setContentView(this.gameView);

		getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
			@Override
			public void handleOnBackPressed(){
				MainActivity.this.gameView.handleBackPressed(MainActivity.this);
			}
		});

		GamesSignInClient gamesSignInClient = PlayGames.getGamesSignInClient(this);
		gamesSignInClient.isAuthenticated().addOnCompleteListener(task -> {
			boolean isAuthenticated = (task.isSuccessful() && task.getResult().isAuthenticated());
			if (!isAuthenticated){
				gamesSignInClient.signIn().addOnCompleteListener(signInTask -> {
					if (!(signInTask.isSuccessful() && signInTask.getResult().isAuthenticated())){
						Toast.makeText(this, "Play Games authentication failed", Toast.LENGTH_LONG).show();
					}
				});
			}
		});
	}

	private void makeFullScreen(){
		if (getSupportActionBar() != null){
			getSupportActionBar().hide();
		}

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P){
			getWindow().getAttributes().layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
		}

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R){
			final Window window = getWindow();
			window.setDecorFitsSystemWindows(false);
			WindowInsetsController controller = window.getInsetsController();
			if (controller != null) {
				controller.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
				controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
			}
		} else {
			getWindow().getDecorView().setSystemUiVisibility(
					View.SYSTEM_UI_FLAG_FULLSCREEN
							| View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
							| View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
							| View.SYSTEM_UI_FLAG_LAYOUT_STABLE
							| View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
							| View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
			);
		}
	}

	@Override
	public void onPause(){
		super.onPause();
		AUDIO.pauseBackgroundMusic();
		if (this.gameView.getScreen() instanceof GameScreen){
			((GameScreen)this.gameView.getScreen()).pauseGame();
		}
	}

	@Override
	public void onResume(){
		super.onResume();
		AUDIO.resumeBackgroundMusic();
	}

	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		super.onWindowFocusChanged(hasFocus);
		if (hasFocus){
			makeFullScreen();
		}
	}
}