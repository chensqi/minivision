package socket.client;

import socket.ClientConnection;

public class ProxyReceiver extends ClientConnection {

	@Override
	protected void transaction_inner() throws Exception {
		for (;;) {
			if (core.getDataPool() < 1000) {
				int size = core.getProxyPoolSize();
				size = Math.max(0, 100 - size);
				if (size > 0) {
					socket.setSoTimeout(10000);
					sendString("" + size);
					String data = readString();
					if (data.equals("IDLE"))
						try {
							sleep(1000);
						} catch (Exception e) {
						}
					else
						core.addProxies(data);
				}
			}
			try {
				sleep(100);
			} catch (Exception e) {
			}
		}
	}
}
