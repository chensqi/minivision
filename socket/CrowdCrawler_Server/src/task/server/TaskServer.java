package task.server;

import controller.ResultData;
import basic.tools.Speeder;
import core.listener.Listenable;
import core.listener.TaskListener;

public abstract class TaskServer extends Listenable<TaskListener> {

	protected Speeder speeder;

	public void initialize() {
		speeder = new Speeder(60000);
		new Thread(){
			public void run() {
				for (;;){
					try {
						sleep(1000);
						for (TaskListener listener:listeners)
							listener.updateSpeed(getSpeed());
					} catch (Exception e) {
					}
				}
			};
		}.start();
	}

	public abstract String getJob(int cnt);

	public abstract void submitResult(ResultData data);

	public abstract void taskFail(String job);


	private String log;
	private String status;
	private double progress;

	protected void log(String log) {
		System.out.println("Tasklog : "+log);
		this.log = log;
		for (TaskListener listener : listeners)
			listener.onLog(log);
	}

	protected void updateStatus(String status) {
		this.status = status;
		for (TaskListener listener : listeners)
			listener.onStatusChange(status);
	}

	protected void updateProgress(double progress) {
		this.progress = progress;
		for (TaskListener listener : listeners)
			listener.onProgressChange(progress);
	}

	public String getLog() {
		return log;
	}

	public String getStatus() {
		return status;
	}

	public double getProgress() {
		return progress;
	}

	public String getName() {
		String[] t = getClass().getName().split("\\.");
		return t[t.length - 1];
	}

	public double getSpeed() {
		return speeder.getSpeed() * 60;
	}

	public abstract String communicate(String msg);

	public abstract int getDQsize();
}
