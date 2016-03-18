package socket;


public abstract class ServerShortConnection extends ServerConnection {

	@Override
	protected void transaction_inner() throws Exception {
		core.ClientConnected(clientId);
		String data = readString();
		String res = onData(data);
		if (res != null)
			sendString(res);
	}
	protected abstract String onData(String data);
}
