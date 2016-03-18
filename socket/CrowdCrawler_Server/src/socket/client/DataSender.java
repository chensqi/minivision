package socket.client;

import controller.ResultData;
import socket.ClientConnection;

public class DataSender extends ClientConnection {
	@Override
	protected void transaction_inner() throws Exception {
		for (;;) {
			ResultData data = core.fetchData();
			if (data == null) {
				try {
					sleep(1000);
				} catch (Exception e) {
				}
				continue;
			}
			socket.setSoTimeout(10000);
			sendObject(data);
		}
	}
}
