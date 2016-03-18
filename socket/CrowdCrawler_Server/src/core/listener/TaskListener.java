package core.listener;

public interface TaskListener {
	public void onStatusChange(String status);
	public void onLog(String log);
	public void onProgressChange(double progress);
	public void updateSpeed(double speed);
}
