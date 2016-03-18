package socket.server;

import socket.ServerShortConnection;

public class GeneralSocket extends ServerShortConnection {
	@Override
	protected String onData(String data) {
		System.out.println(clientId + " : " + data);
		
		return null;
	}
}
