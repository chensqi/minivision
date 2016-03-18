package socket;

import java.net.InetSocketAddress;
import java.net.Socket;

import controller.ClientCore;

public class ClientConnector extends Thread {
	private String host;
	private int port;
	private int timeout;
	private int trails;
	private String connection;
	private ClientCore core;

	public ClientConnector(String host, int port, String connection, int timeout,
			int trails,ClientCore core) {
		this.host = host;
		this.port = port;
		this.connection = connection;
		this.timeout = timeout;
		if (trails == 0)
			trails = -1;
		this.trails = trails;
		this.core=core;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}

	public void setTrails(int trails) {
		if (trails == 0)
			trails = -1;
		this.trails = trails;
	}

	public void setConnection(String connection) {
		this.connection = connection;
	}

	private Socket socket = null;

	@Override
	public void run() {
		for (int i = 0; i < 0 || i != trails; i++) {
			try {
				socket = new Socket();
				socket.connect(new InetSocketAddress(host, port), timeout);
				System.out.println("Connected "+socket.getLocalPort()+" "+socket.getPort());
				ClientConnection conn = (ClientConnection) Class.forName(
						connection).newInstance();
				conn.setSocket(socket);
				conn.setCore(core);
				conn.run();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}
