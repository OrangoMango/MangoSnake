package com.orangomango.androidbridge;

public class Scheduler{
	public static void schedule(Runnable r, long time){
		Thread t = new Thread(() -> {
			try {
				Thread.sleep(time);
				r.run();
			} catch (InterruptedException ex){
				ex.printStackTrace();
			}
		});
		t.start();
	}
}
