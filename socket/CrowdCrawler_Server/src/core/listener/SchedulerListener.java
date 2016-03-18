package core.listener;

import task.server.TaskServer;


public interface SchedulerListener {
	public void AddTask(String taskName,TaskServer task);
	public void RemoveTask(String taskName);
}
