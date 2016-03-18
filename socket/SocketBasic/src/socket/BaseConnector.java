package socket;

import java.net.InetSocketAddress;
import java.net.Socket;

public class BaseConnector extends Thread {
	private String host;
	private int port;
	private int timeout;
	private int trails;
	private String connection;

	public BaseConnector(String host, int port, String connection, int timeout,
			int trails) {
		this.host = host;
		this.port = port;
		this.connection = connection;
		this.timeout = timeout;
		if (trails == 0)
			trails = -1;
		this.trails = trails;
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
				BaseConnection conn = (BaseConnection) Class.forName(
						connection).newInstance();
				conn.setSocket(socket);
				conn.run();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}
