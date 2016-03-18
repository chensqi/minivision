package controller;

import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;

import core.listener.ClientListener;

public class Guardian extends Thread implements ClientListener {
	private HashMap<String, Long> last = new HashMap<String, Long>();

	private ServerCore core;

	public Guardian(ServerCore core) {
		this.core = core;
	}

	@Override
	public void run() {
		for (;;) {
			long time = Calendar.getInstance().getTimeInMillis();
			time -= 30000;
			for (String s : new HashSet<String>(last.keySet()))
				if (last.containsKey(s) && last.get(s) < time) {
					core.ClientDisconnected(s);
					last.remove(s);
				}
			try {
				Thread.sleep(30000);
			} catch (Exception e) {
			}
		}
	}

	@Override
	public void ClientConnected(String clientId) {
		long time = Calendar.getInstance().getTimeInMillis();
		last.put(clientId, time);
	}

	@Override
	public void ClientDisconnected(String clientId) {
		last.remove(clientId);
	}

	@Override
	public void setStatus(String clientId, String status) {
	}

	@Override
	public void clientSubmit(String clientId) {
	}
}
