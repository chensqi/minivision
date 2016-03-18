package socket;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;

import basic.ByteOps;

public class SocketBasics {

	public static int readInt(BufferedInputStream input) throws Exception {
		byte[] head = new byte[4];
		input.read(head);
		return ByteOps.byteArrayToInt(head);
	}

	public static String readString(BufferedInputStream input)
			throws Exception {
		byte[] head = new byte[4];
		input.read(head);
		int len = ByteOps.byteArrayToInt(head);
		byte[] content = new byte[len];
		input.read(content);
		return new String(content, "UTF-8");
	}

	public static void sendInt(int i, BufferedOutputStream output)
			throws Exception {
		byte[] head = ByteOps.intToByteArray1(i);
		output.write(head);
		output.flush();
	}

	public static void sendString(String data, BufferedOutputStream output)
			throws Exception {
		byte[] content = data.getBytes("UTF-8");
		byte[] head = ByteOps.intToByteArray1(content.length);
		output.write(head);
		output.write(content);
		output.flush();
	}

}
