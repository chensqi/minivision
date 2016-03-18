package entry;

import socket.BaseConnector;

public class ClientEntry {
	public static void main(String[] args) {
		for (int i = 0; i < Integer.valueOf(args[0]); i++)
			new BaseConnector("202.120.61.1", 8012, "socket.StrSender", 1000, 0).start();
	}
}
