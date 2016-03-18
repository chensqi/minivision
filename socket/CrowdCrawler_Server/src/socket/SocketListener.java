package socket;

import java.net.ServerSocket;
import java.net.Socket;

import controller.ServerCore;

public class SocketListener extends Thread {

	private int port;
	private String connector;
	private ServerCore core;

	public SocketListener(int port, String connector, ServerCore core) {
		this.port = port;
		this.connector = connector;
		this.core = core;
	}

	@Override
	public void run() {
		ServerSocket serverSocket = null;
		for (;;) {
			try {
				serverSocket = new ServerSocket(port);
				while (true) {
					Socket client = serverSocket.accept();
					ServerConnection conn = (ServerConnection) Class.forName(
							connector).newInstance();
					conn.setSocket(client);
					conn.setCore(core);
					conn.start();
				}
			} catch (Exception e) {
				System.err.println("Socket Listener Error on Port " + port);
				e.printStackTrace();
			} finally {
				try {
					serverSocket.close();
				} catch (Exception e) {
				}
			}
		}
	}
}
