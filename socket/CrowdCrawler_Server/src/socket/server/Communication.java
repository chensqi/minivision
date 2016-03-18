package socket.server;

import socket.ServerShortConnection;

public class Communication extends ServerShortConnection {
	@Override
	protected String onData(String data) {
		System.out.println(clientId + " Communicate " + data);

		String res = core.communicate(data);
		System.out.println(clientId + " Communicate Result = " + res);
		return res;
	}
}
