package socket;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;


import basic.ByteOps;

public abstract class BaseConnection extends Thread {
	protected Socket socket;
	protected BufferedInputStream input = null;
	protected BufferedOutputStream output = null;

	protected int readInt() throws Exception {
		byte[] head = new byte[4];
		input.read(head);
		return ByteOps.byteArrayToInt(head);
	}

	protected void sendInt(int i) throws Exception {
		byte[] head = ByteOps.intToByteArray1(i);
		output.write(head);
		output.flush();
	}

	protected String readString() throws Exception {
		return (String)readObject();
	}
	
	protected Object readObject() throws Exception{
		byte[] head = new byte[4];
		input.read(head);
		int len = ByteOps.byteArrayToInt(head);

		if (len > 100000 || len == 0)
			throw new Exception();
		byte[] content = new byte[len];
		input.read(content);
		
		ByteArrayInputStream bis = new ByteArrayInputStream(content);
		ObjectInput in = null;
		Object res=null;
		try {
		  in = new ObjectInputStream(bis);
		  res = in.readObject(); 
		} finally {
		  try {
		    bis.close();
		  } catch (Exception ex) {
		  }
		  try {
		      in.close();
		  } catch (Exception ex) {
		  }
		}
		return res;
	}
	
	protected void sendObject(Object o) throws Exception{
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		ObjectOutputStream os=null;
		try{
		os=new ObjectOutputStream(bos);
		os.writeObject(o);
		os.flush();
		bos.flush();
		byte[] content=bos.toByteArray();
		
		byte[] head = ByteOps.intToByteArray1(content.length);
		output.write(head);
		output.write(content);
		output.flush();
		}catch (Exception ex){
			throw ex;
		}
		finally{
			try {
				os.close();
			} catch (Exception e) {
			}
			try {
				bos.close();
			} catch (Exception e) {
			}
		}
	}

	protected void sendString(String data) throws Exception {
		sendObject(data);
	}

	public void setSocket(Socket socket) {
		this.socket = socket;
	}

	@Override
	public void run() {
		try {
			output = new BufferedOutputStream(socket.getOutputStream());
			input = new BufferedInputStream(socket.getInputStream());

			transaction();

		} catch (Exception e) {
		}
		try {
			socket.close();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	protected abstract void transaction() throws Exception;
}
