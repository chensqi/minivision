package socket;

import controller.ServerCore;
import socket.BaseConnection;

public abstract class ServerConnection extends BaseConnection {
	protected String clientId;
	protected ServerCore core;

	public void setCore(ServerCore core) {
		this.core = core;
	}

	@Override
	protected void transaction() throws Exception {
		String ip = socket.getInetAddress().toString().replace("/", "");
		clientId = ip + ":" + readInt();
		
		core.ClientConnected(clientId);
		
		transaction_inner();
	}
	
	protected abstract void transaction_inner() throws Exception;
}
