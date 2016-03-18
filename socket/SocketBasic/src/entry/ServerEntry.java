package entry;

import socket.BaseSocketListener;

public class ServerEntry {
	public static void main(String[] args) {
		new BaseSocketListener(8012, "socket.StrReceiver").start();
	}
}
