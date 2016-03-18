package controller;

import java.util.LinkedList;
import java.util.Random;

import socket.ClientConnector;

public class ClientCore {
	private int id;

	private LinkedList<ResultData> dataPool = new LinkedList<ResultData>();
	private LinkedList<String> tasks = new LinkedList<String>();

	public int running = 0;

	public ClientCore(int nlocal, int nproxy, int nsender) {
		id = new Random().nextInt(1000000);

		for (int i = 0; i < nlocal; i++)
			new ClientWorker(true, this).start();
		for (int i = 0; i < nproxy; i++)
			new ClientWorker(false, this).start();

		for (int i = 0; i < nsender; i++)
			new ClientConnector(getHost(), getDataPort(),
					"socket.client.DataSender", 1000, 0, this).start();

		new ClientConnector(getHost(), getTaskPort(),
				"socket.client.TaskReceiver", 1000, 0, this).start();

		new ClientConnector(getHost(), getProxyPort(),
				"socket.client.ProxyReceiver", 1000, 0, this).start();

		final ClientCore core = this;

		new Thread() {
			public void run() {
				for (;;) {
					try {
						sleep(1000);
						new ClientConnector(getHost(), getHeartBeatPort(),
								"socket.client.HeartBeat", 1000, 1, core)
								.start();
					} catch (Exception e) {
					}
				}
			};
		}.start();

	}

	public String getHost() {
//		return "202.120.61.1";
		return "172.16.14.238";
	}
	
	private int deltaPort=0;

	public int getGeneralPort() {
		return 8012+deltaPort;
	}

	public int getDataPort() {
		return 8013+deltaPort;
	}

	public int getTaskPort() {
		return 8014+deltaPort;
	}

	public int getHeartBeatPort() {
		return 8015+deltaPort;
	}

	public int getCommPort() {
		return 8016+deltaPort;
	}

	public int getProxyPort() {
		return 8017+deltaPort;
	}

	public int getClientId() {
		return id;
	}

	public String getTask() {
		synchronized (tasks) {
			try {
				return tasks.removeFirst();
			} catch (Exception ex) {
			}
		}
		return "";
	}

	public void submit(ResultData data) {
		synchronized (dataPool) {
			dataPool.add(data);
		}
	}

	public ResultData fetchData() {
		try {
			synchronized (dataPool) {
				return dataPool.removeFirst();
			}
		} catch (Exception e) {
		}
		return null;
	}

	public int getTaskPoolSize() {
		return tasks.size();
	}

	public void addTasks(String data) {
		synchronized (tasks) {
			for (String t : data.split(";"))
				if (t.trim().length() > 0)
					tasks.add(t.trim());
		}
	}

	public String getStatus() {
		String status = "";
		status += "Task:" + tasks.size();
		status += ";Data:" + dataPool.size();
		status += ";Workers:" + running;
		status += ";Proxy:" + proxies.size();
		return status;
	}

	public int getDataPool() {
		return dataPool.size();
	}

	public synchronized void addRunning(int i) {
		running += i;
	}

	private LinkedList<String> proxies = new LinkedList<String>();

	public String fetchProxy() {
		try {
			synchronized (proxies) {
				return proxies.removeFirst();
			}
		} catch (Exception e) {
			return null;
		}
	}

	public void addProxies(String data) {
		synchronized (proxies) {
			for (String t : data.split(";"))
				if (t.trim().length() > 0)
					proxies.add(t.trim());
		}
	}

	public int getProxyPoolSize() {
		return proxies.size();
	}
}

