package socket;

import java.net.ServerSocket;
import java.net.Socket;

public class BaseSocketListener extends Thread {
	private int port;
	private String connector;

	public BaseSocketListener(int port, String connector) {
		this.port = port;
		this.connector = connector;
	}

	@Override
	public void run() {
		ServerSocket serverSocket = null;
		for (;;) {
			try {
				serverSocket = new ServerSocket(port);
				while (true) {
					Socket client = serverSocket.accept();
					BaseConnection conn = (BaseConnection) Class.forName(
							connector).newInstance();
					System.out.println("Socket Listener Connected on Port "
							+ port);
					conn.setSocket(client);
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
