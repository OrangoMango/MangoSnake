package com.orangomango.snake;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Vibrator;
import android.view.MotionEvent;
import android.view.View;
import android.graphics.Rect;
import android.graphics.RectF;

import androidx.core.content.res.ResourcesCompat; // Font

import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import com.orangomango.account.Account;
import com.orangomango.androidbridge.*;
import com.orangomango.androidbridge.geometry.TextAlignment;

import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.orangomango.snake.screen.HomeScreen;
import com.orangomango.snake.screen.Screen;
import com.orangomango.snake.screen.UpdateScreen;
import com.orangomango.snake.ui.UiElement;

public class GameView extends View{
	public static int WIDTH = 1200; // Ratio: 2:1, changed on update()
	public static int HEIGHT = 600;

	private Screen game;
	private InterstitialAd mInterstitialAd;
	private RewardedAd mRewardedAd;
	private boolean vibrate;
	private BillingManager billingManager;
	private int receivedWidth, receivedHeight;
	private double fps;

	public static Typeface MAIN_TYPEFACE;
	public static Bitmap[] SNAKE_IMAGE = new Bitmap[30];
	public static AndroidAudio AUDIO;

	// ICanvas data
	private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
	private final RectF destRectF = new RectF();
	private final Rect srcRect = new Rect();

	public GameView(Context context, BillingManager billingManager){
		super(context);

		// Load audio
		AUDIO = new AndroidAudio();
		AUDIO.loadSound(context, "gameover", R.raw.gameover);
		AUDIO.loadSound(context, "gamestart", R.raw.gamestart);
		AUDIO.loadSound(context, "gui", R.raw.gui);
		AUDIO.loadSound(context, "highscore", R.raw.highscore);
		AUDIO.loadSound(context, "point", R.raw.point);

		// Load font and images
		MAIN_TYPEFACE = ResourcesCompat.getFont(context, R.font.main_font);
		for (int i = 0; i < SNAKE_IMAGE.length; i++){
			SNAKE_IMAGE[i] = BitmapFactory.decodeResource(getResources(), getResources().getIdentifier("snake_" + i, "drawable", context.getPackageName()));
		}

		AUDIO.playBackgroundMusic(getContext(), R.raw.background);

		// Check for updates
		Account.HOST = "https://id.orangomango.org";
		Account.registerApplication(getResources().getString(R.string.app_uid));
		new Thread(() -> {
			String appVersion = Account.getAppVersion();
			String localAppVersion = getResources().getString(R.string.app_version);
			// Bypass update required screen while on debug mode
			if ((context.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) == 0 && appVersion != null && !appVersion.equals(localAppVersion)){
				this.game = new UpdateScreen(this, appVersion);
			}
		}).start();

		// Launch home screen
		this.billingManager = billingManager;

		MobileAds.initialize(getContext(), initializationStatus -> {
			loadInterstitial();
			loadRewardedAd();
		});
	}

	private void loadRewardedAd(){
		AdRequest adRequest = new AdRequest.Builder().build();

		// ca-app-pub-3940256099942544/5224354917
		RewardedAd.load(getContext(), getResources().getString(R.string.rewarded_ad), adRequest,
			new RewardedAdLoadCallback(){
				@Override
				public void onAdLoaded(RewardedAd ad){
					mRewardedAd = ad;
				}

				@Override
				public void onAdFailedToLoad(LoadAdError loadAdError){
					mRewardedAd = null;
				}
		});
	}

	public void showRewarded(Runnable callback){
		if (mRewardedAd != null) {
			mRewardedAd.setFullScreenContentCallback(new FullScreenContentCallback(){
				@Override
				public void onAdDismissedFullScreenContent() {
					mRewardedAd = null;
					loadRewardedAd();
				}
			});

			((Activity)getContext()).runOnUiThread(() -> mRewardedAd.show((Activity)getContext(), rewardItem -> callback.run()));
		}
	}

	private void loadInterstitial(){
		AdRequest adRequest = new AdRequest.Builder().build();

		// ca-app-pub-5753059750056945/3724851181
		InterstitialAd.load(getContext(), getResources().getString(R.string.interstitial_ad), adRequest,
			new InterstitialAdLoadCallback(){
				@Override
				public void onAdLoaded(InterstitialAd interstitialAd){
					mInterstitialAd = interstitialAd;
				}

				@Override
				public void onAdFailedToLoad(LoadAdError loadAdError){
					mInterstitialAd = null;
				}
		});
	}

	public void showIntersitial(Runnable callback){
		if (mInterstitialAd != null) {
			mInterstitialAd.setFullScreenContentCallback(new FullScreenContentCallback(){
				@Override
				public void onAdDismissedFullScreenContent(){
					mInterstitialAd = null;
					loadInterstitial();
					callback.run();
				}
			});

			((Activity)getContext()).runOnUiThread(() -> mInterstitialAd.show((Activity)getContext()));
		} else {
			callback.run();
		}
	}

	public void openPlayStore(){
		final String packageName = getContext().getPackageName();
		Uri uri = Uri.parse("market://details?id=" + packageName);
		Intent goToMarket = new Intent(Intent.ACTION_VIEW, uri);

		goToMarket.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_NEW_DOCUMENT | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
		try {
			getContext().startActivity(goToMarket);
		} catch (ActivityNotFoundException e) {
			getContext().startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://play.google.com/store/apps/details?id=" + packageName)));
		}
	}

	public void setVibrate(boolean v){
		this.vibrate = v;
	}

	public void triggerVibration(long time){
		triggerVibration(new long[]{0, time});
	}

	public void triggerVibration(long[] time){
		if (!this.vibrate) return;
		Vibrator v = (Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE);

		if (v != null && v.hasVibrator()){
			v.vibrate(time, -1);
		}
	}

	public void handleBackPressed(Activity activity){
		this.game.goBack(activity);
	}

	@Override
	public boolean onTouchEvent(MotionEvent event){
		float tx = event.getX();
		float ty = event.getY();

		PointerEvent.Type type;
		switch (event.getAction()){
			case MotionEvent.ACTION_DOWN:
				type = PointerEvent.Type.PRESSED;
				break;
			case MotionEvent.ACTION_MOVE:
				type = PointerEvent.Type.DRAGGED;
				break;
			case MotionEvent.ACTION_UP:
				type = PointerEvent.Type.RELEASED;
				break;
			default: return false;
		}

		game.handleInput(new PointerEvent(tx, ty, type));
		invalidate();
		return true;
	}

	@Override
	protected void onDraw(Canvas canvas){
		super.onDraw(canvas);

		long startTime = System.currentTimeMillis();

		ICanvas bridge = new ICanvas(){
			@Override
			public void clear(int rgb){
				canvas.drawColor(rgb);
			}

			@Override
			public void fillRect(double x, double y, double w, double h, int rgb){
				paint.setColor(rgb);
				paint.setStyle(Paint.Style.FILL);
				canvas.drawRect((float) x, (float) y, (float) (x + w), (float) (y + h), paint);
			}

			@Override
			public void fillRoundRect(double x, double y, double w, double h, double arcWidth, double arcHeight, int rgb){
				paint.setColor(rgb);
				paint.setStyle(Paint.Style.FILL);
				canvas.drawRoundRect((float) x, (float) y, (float) (x + w), (float) (y + h), (float) arcWidth, (float) arcHeight, paint);
			}

			@Override
			public void strokeRect(double x, double y, double w, double h, int rgb, double lw){
				paint.setColor(rgb);
				paint.setStyle(Paint.Style.STROKE);
				paint.setStrokeWidth((float) lw);
				canvas.drawRect((float) x, (float) y, (float) (x + w), (float) (y + h), paint);
			}

			@Override
			public void strokeRoundRect(double x, double y, double w, double h, double arcWidth, double arcHeight, int rgb, double lw){
				paint.setColor(rgb);
				paint.setStyle(Paint.Style.STROKE);
				paint.setStrokeWidth((float) lw);
				canvas.drawRoundRect((float) x, (float) y, (float) (x + w), (float) (y + h), (float) arcWidth, (float) arcHeight, paint);
			}

			@Override
			public void fillOval(double x, double y, double w, double h, int rgb){
				paint.setColor(rgb);
				paint.setStyle(Paint.Style.FILL);
				canvas.drawOval((float) x, (float) y, (float) (x + w), (float) (y + h), paint);
			}

			@Override
			public void strokeOval(double x, double y, double w, double h, int rgb, double lw){
				paint.setColor(rgb);
				paint.setStyle(Paint.Style.STROKE);
				paint.setStrokeWidth((float) lw);
				canvas.drawOval((float) x, (float) y, (float) (x + w), (float) (y + h), paint);
			}

			@Override
			public void strokeLine(double x1, double y1, double x2, double y2, int rgb, double lw){
				paint.setColor(rgb);
				paint.setStyle(Paint.Style.STROKE);
				paint.setStrokeWidth((float) lw);
				canvas.drawLine((float) x1, (float) y1, (float) x2, (float) y2, paint);
			}

			@Override
			public void fillPolygon(double[] xPoints, double[] yPoints, int nPoints, int rgb){
				Path path = new Path();
				path.moveTo((float) xPoints[0], (float) yPoints[0]);

				for (int i = 1; i < nPoints; i++){
					path.lineTo((float) xPoints[i], (float) yPoints[i]);
				}

				path.close();

				paint.setColor(rgb);
				paint.setStyle(Paint.Style.FILL);
				canvas.drawPath(path, paint);
			}

			@Override
			public void strokePolygon(double[] xPoints, double[] yPoints, int nPoints, int rgb, double lw){
				Path path = new Path();
				path.moveTo((float) xPoints[0], (float) yPoints[0]);

				for (int i = 1; i < nPoints; i++){
					path.lineTo((float) xPoints[i], (float) yPoints[i]);
				}

				path.close();

				paint.setColor(rgb);
				paint.setStyle(Paint.Style.STROKE);
				paint.setStrokeWidth((float) lw);
				canvas.drawPath(path, paint);
			}

			@Override
			public void fillText(String text, double x, double y, int rgb, double size, TextAlignment align){
				paint.setColor(rgb);
				paint.setTextSize((float) size);
				paint.setTypeface(MAIN_TYPEFACE);
				paint.setStyle(Paint.Style.FILL);

				switch (align) {
					case CENTER: paint.setTextAlign(Paint.Align.CENTER); break;
					case RIGHT:  paint.setTextAlign(Paint.Align.RIGHT);  break;
					default:     paint.setTextAlign(Paint.Align.LEFT);   break;
				}

				String[] lines = text.split("\n");
				for (String line : lines) {
					canvas.drawText(line, (float) x, (float) y, paint);
					y += paint.descent() - paint.ascent();
				}
			}

			@Override
			public void drawImage(Object img, double dx, double dy, double dw, double dh){
				if (img instanceof Bitmap){
					destRectF.set((float) dx, (float) dy, (float) (dx + dw), (float) (dy + dh));
					canvas.drawBitmap((Bitmap) img, null, destRectF, paint);
				}
			}

			@Override
			public void drawImage(Object img, double sx, double sy, double sw, double sh, double dx, double dy, double dw, double dh){
				if (img instanceof Bitmap){
					srcRect.set((int) sx, (int) sy, (int) (sx + sw), (int) (sy + sh));
					destRectF.set((float) dx, (float) dy, (float) (dx + dw), (float) (dy + dh));
					canvas.drawBitmap((Bitmap) img, srcRect, destRectF, paint);
				}
			}

			@Override
			public void drawRoundImage(Object img, double x, double y, double w, double h, double arcWidth, double arcHeight){
				if (img instanceof Bitmap) {
					Bitmap bitmap = (Bitmap) img;
					canvas.save();
					canvas.translate((float) x, (float) y);

					float scaleX = (float) w / bitmap.getWidth();
					float scaleY = (float) h / bitmap.getHeight();
					BitmapShader shader = new BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
					Matrix matrix = new Matrix();
					matrix.setScale(scaleX, scaleY);
					shader.setLocalMatrix(matrix);

					paint.setShader(shader);
					destRectF.set(0, 0, (float) w, (float) h);
					canvas.drawRoundRect(destRectF, (float) arcWidth, (float) arcHeight, paint);
					paint.setShader(null);
					canvas.restore();
				}
			}

			@Override
			public void save(){
				canvas.save();
			}

			@Override
			public void restore(){
				canvas.restore();
			}

			@Override
			public void translate(double x, double y){
				canvas.translate((float) x, (float) y);
			}

			@Override
			public void rotate(double deg){
				canvas.rotate((float) deg);
			}

			@Override
			public void scale(double x, double y){
				canvas.scale((float) x, (float) y);
			}

			@Override
			public void setEffect(double radius, int color){
				paint.setShadowLayer((float) radius, 0f, 0f, color);
			}

			@Override
			public void setShader(LinearGradient gradient){
				paint.setShader(gradient);
			}

			public void setLineDashes(double line, double gap){
				paint.setPathEffect(new DashPathEffect(new float[]{(float)line, (float)gap}, 0));
			}

			@Override
			public void clearEffect(){
				paint.clearShadowLayer();
				paint.setPathEffect(null);
			}

			@Override
			public int getWidth(){
				return GameView.this.receivedWidth;
			}

			@Override
			public int getHeight(){
				return GameView.this.receivedHeight;
			}

			@Override
			public double measureText(String text, double size) {
				paint.setTypeface(MAIN_TYPEFACE);
				paint.setTextSize((float)size);
				return paint.measureText(text);
			}
		};

		if (this.receivedWidth == 0 && this.receivedHeight == 0){ // Device screen size is only remembered once
			this.receivedWidth = getWidth();
			this.receivedHeight = getHeight();
			HEIGHT = this.receivedHeight;
			WIDTH = (int)(this.receivedWidth*0.88); //HEIGHT * 2;

			UiElement.FONT_SMALL = 0.015*HEIGHT;
			UiElement.FONT_MEDIUM = 0.025*HEIGHT;
			UiElement.FONT_LARGE = 0.035*HEIGHT;
			UiElement.FONT_LARGELARGE = 0.045*HEIGHT;
			UiElement.FONT_MEDIUMLARGE = 0.070*HEIGHT;
			UiElement.FONT_EXTRALARGE = 0.100*HEIGHT;
			UiElement.FONT_EXTRAEXTRALARGE = 0.155*HEIGHT;

			this.game = new HomeScreen(this);
		}

		if (this.game != null){
			this.game.update(this.receivedWidth, this.receivedHeight);
			this.game.render(bridge);
		}

		long duration = System.currentTimeMillis() - startTime;
		this.fps = 1000.0 / duration;

		invalidate();
	}

	public Screen getScreen(){
		return this.game;
	}

	public void setScreen(Screen screen){
		this.game = screen;
	}

	public BillingManager getBillingManager(){
		return this.billingManager;
	}

	public double getFPS(){
		return this.fps;
	}

	public float getPaintDescent(){
		return this.paint.descent();
	}

	public float getPaintAscent(){
		return this.paint.ascent();
	}
}
