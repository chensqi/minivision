package core.listener;

public interface ClientListener {
	public void ClientConnected(String clientId);

	public void ClientDisconnected(String clientId);

	public void setStatus(String clientId, String status);

	public void clientSubmit(String clientId);
}
