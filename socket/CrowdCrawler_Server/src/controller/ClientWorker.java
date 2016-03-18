package controller;

import basic.Config;
import network.Client;
import task.client.TaskClient;

public class ClientWorker extends Thread {
	private boolean local;
	private ClientCore core;

	public ClientWorker(boolean local, ClientCore core) {
		this.local = local;
		this.core = core;
	}

	@Override
	public void run() {
		for (;;) {
			try {
				ready();
			} catch (Exception e) {
			}
		}
	}

	private void ready() {
		String task = core.getTask();
		if (task.length() > 0) {
			core.addRunning(1);
			try {
				ResultData data = runTask(task);
				if (data!=null)
					core.submit(data);
			} catch (Exception ex) {

			} finally {
				core.addRunning(-1);
			}
		} else
			try {
				sleep(3000);
			} catch (Exception e2) {
			}
	}

	private ResultData runTask(String cmd) {
		if (cmd.equals("IDLE")) {
			try {
				sleep(3000);
			} catch (Exception e) {
			}
			return null;
		}
		int pos = cmd.indexOf(':');
		String taskName = cmd.substring(0, pos);
		String job = cmd.substring(pos + 1);
		try {
			TaskClient task = (TaskClient) Class.forName(
					"task.client." + taskName).newInstance();
			ResultData res = task.work(job, core, this);
			if (res.data!=null && !local)
				goodProxy();
			return res;
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return null;
	}

	private void goodProxy() {
		core.submit(new ResultData("ProxyControl", proxy, true));
	}

	private String proxy = "";
	private Client client = null;

	public Client getHttpClient() {
//		if (client != null)
//			return client;
		if (local) {
			client = new Client("local");
			client.setUserAgent(Config.getValue("useragent"));
			return client;
		} else {
			for (;;) {
				proxy = core.fetchProxy();
				client = new Client(proxy);
				if (client != null)
					return client;
				try {
					sleep(10000);
				} catch (Exception ex) {
				}
			}
		}
	}

	public void badHttpClient() {
		if (local)
			return;
		core.submit(new ResultData("ProxyControl", proxy, false));
		proxy = core.fetchProxy();
		client = new Client(proxy);
	}

}
