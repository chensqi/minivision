package socket.client;

import socket.ClientConnection;

public class HeartBeat extends ClientConnection {

	@Override
	protected void transaction_inner() throws Exception {
		String s = core.getStatus();
		System.out.println(s);
		sendString(s);
	}
}
