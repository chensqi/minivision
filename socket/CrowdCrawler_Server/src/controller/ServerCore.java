package controller;

import java.util.HashSet;

import javax.swing.JOptionPane;

import network.ProxyBank;
import core.listener.ClientListener;
import socket.SocketListener;
import basic.Config;

public class ServerCore extends Thread {

	private HashSet<ClientListener> clientListener = new HashSet<ClientListener>();
	private Guardian guardian;

	private ProxyBank proxies;

	public ServerCore() {
		Config.load("Config.txt");
		proxies = new ProxyBank();
		guardian = new Guardian(this);
	}

	@Override
	public void run() {
		proxies.loadProxies();
		proxies.proxyLoader();

		Scheduler.start();
		guardian.start();

		new SocketListener(Config.getInt("DataPort"),
				"socket.server.DataReceiver", this).start();
		new SocketListener(Config.getInt("TaskPort"),
				"socket.server.TaskSender", this).start();
		new SocketListener(Config.getInt("CommPort"),
				"socket.server.Communication", this).start();
		new SocketListener(Config.getInt("HeartBeatPort"),
				"socket.server.HeartBeat", this).start();
		new SocketListener(Config.getInt("GeneralPort"),
				"socket.server.GeneralSocket", this).start();
		new SocketListener(Config.getInt("ProxyPort"),
				"socket.server.ProxySender", this).start();
	}

	public ProxyBank getProxyBank() {
		return proxies;
	}

	private HashSet<String> clients = new HashSet<String>();

	public void ClientConnected(String clientId) {
		guardian.ClientConnected(clientId);
		if (clients.contains(clientId))
			return;
		clients.add(clientId);

		System.err.println("Client Connected : " + clientId);

		for (ClientListener listener : clientListener)
			listener.ClientConnected(clientId);
	}

	public void ClientDisconnected(String clientId) {
		clients.remove(clientId);
		System.err.println("Client Disconnected : " + clientId);

		for (ClientListener listener : clientListener)
			listener.ClientDisconnected(clientId);
	}

	public void bindClientListener(ClientListener listener) {
		clientListener.add(listener);
	}

	public void unbindClientListener(ClientListener listener) {
		clientListener.remove(listener);
	}

	public void assignTask(String clientId, String taskDes) {

	}

	public void clientSubmit(String clientId) {
		for (ClientListener listener : clientListener)
			listener.clientSubmit(clientId);
	}

	public void clientStatus(String clientId, String status) {
		for (ClientListener listener : clientListener)
			listener.setStatus(clientId, status);
	}

	public void showError(String msg) {
		JOptionPane.showMessageDialog(null, msg, "Error",
				JOptionPane.ERROR_MESSAGE);
	}

	public String communicate(String data) {
		int pos = data.indexOf(":");
		String task = data.substring(0, pos);
		return Scheduler.getTask(task).communicate(data.substring(pos + 1));
	}
}
