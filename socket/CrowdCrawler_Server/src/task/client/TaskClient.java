package task.client;

import controller.ClientCore;
import controller.ClientWorker;
import controller.ResultData;

public abstract class TaskClient {
	public abstract ResultData work(String job, ClientCore core, ClientWorker worker);

	public String getName() {
		String[] t = getClass().getName().split("\\.");
		return t[t.length - 1];
	}
}
