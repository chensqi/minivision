package socket.server;

import socket.ServerLongConnection;
import task.server.TaskServer;
import controller.Scheduler;

public class TaskSender extends ServerLongConnection {
	private static String lock = "lock";

	@Override
	protected String onData(String data) {
		try {
			int cnt = Integer.valueOf(data);
			System.out.println(clientId + " : Task Request - " + cnt);

			if (cnt == 0)
				return null;

			synchronized (lock) {
				String taskstr = "";
				int DQsize = Scheduler.getDQsize();
				if (DQsize < 5000) {
					TaskServer task = Scheduler.selectTask();
					if (task != null)
						taskstr = task.getJob(cnt);
					// for (int i = 0; i < cnt; i++) {
					// try {
					// String job = task.getJob();
					// String taskdes = task.getName() + ":" + job;
					// core.assignTask(clientId, taskdes);
					// if (taskstr.length() > 0)
					// taskstr += ";";
					// taskstr += taskdes;
					// } catch (Exception ex) {
					// }
					// }
				}
				if (taskstr.length() == 0)
					taskstr = "IDLE";
				System.out.println(clientId + " : Assign - " + taskstr);
				return taskstr;
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return null;
	}

}
