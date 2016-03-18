package socket;

import controller.ClientCore;
import socket.BaseConnection;

public abstract class ClientConnection extends BaseConnection {
	protected ClientCore core;

	public void setCore(ClientCore core) {
		this.core = core;
	}

	@Override
	protected void transaction() throws Exception {
		
		sendInt(core.getClientId());
		
		transaction_inner();
	}
	
	protected abstract void transaction_inner() throws Exception;
}
