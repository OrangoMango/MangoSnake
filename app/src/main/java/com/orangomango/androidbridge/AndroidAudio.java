package com.orangomango.androidbridge;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.SoundPool;
import java.util.HashMap;
import java.util.Map;

public class AndroidAudio{
	private final SoundPool soundPool;
	private final Map<String, Integer> soundMap = new HashMap<>();
	private MediaPlayer mediaPlayer;
	private float soundVolume = 1.0f, musicVolume = 1.0f;

	public AndroidAudio() {
		AudioAttributes attrs = new AudioAttributes.Builder()
				.setUsage(AudioAttributes.USAGE_GAME)
				.setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
				.build();

		this.soundPool = new SoundPool.Builder()
				.setMaxStreams(10)
				.setAudioAttributes(attrs)
				.build();
	}

	public void loadSound(Context context, String name, int resId) {
		soundMap.put(name, soundPool.load(context, resId, 1));
	}

	public void setSoundVolume(float vol){
		this.soundVolume = vol;
	}

	public void setMusicVolume(float vol){
		this.musicVolume = vol;
		if (this.mediaPlayer != null){
			this.mediaPlayer.setVolume(this.musicVolume, this.musicVolume);
		}
	}

	public void playSound(String name){
		Integer soundId = soundMap.get(name);
		if (soundId != null) {
			soundPool.play(soundId, this.soundVolume, this.soundVolume, 1, 0, 1.0f);
		}
	}

	public void playBackgroundMusic(Context context, int resId){
		if (this.mediaPlayer != null){
			this.mediaPlayer.release();
		}
		this.mediaPlayer = MediaPlayer.create(context, resId);
		this.mediaPlayer.setLooping(true);
		this.mediaPlayer.setVolume(this.musicVolume*0.8f, this.musicVolume*0.8f);
		this.mediaPlayer.start();
	}

	public void stopBackgroundMusic(){
		if (this.mediaPlayer != null) {
			this.mediaPlayer.stop();
			this.mediaPlayer.release();
			this.mediaPlayer = null;
		}
	}

	public void pauseBackgroundMusic(){
		if (this.mediaPlayer != null){
			this.mediaPlayer.pause();
		}
	}

	public void resumeBackgroundMusic(){
		if (this.mediaPlayer != null){
			this.mediaPlayer.start();
		}
	}
}
