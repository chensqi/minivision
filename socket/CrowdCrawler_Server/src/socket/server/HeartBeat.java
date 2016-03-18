package socket.server;

import socket.ServerShortConnection;

public class HeartBeat extends ServerShortConnection {
	@Override
	protected String onData(String data) {
		core.clientStatus(clientId, data);
		return null;
	}

}
