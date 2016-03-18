package socket;

public abstract class ServerLongConnection extends ServerConnection {

	@Override
	protected void transaction_inner() throws Exception {
		for (int i = 0;; i++) {
			core.ClientConnected(clientId);
			String data = readString();
			if (clientId.contains("557004")) continue;
			String res = onData(data);
			if (res != null)
				sendString(res);
		}
	}
	protected abstract String onData(String data);
}
