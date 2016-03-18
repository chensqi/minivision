package socket.client;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

import socket.BaseConnection;
import controller.ClientCore;
import basic.ByteOps;

public class Communication{

	private ClientCore core;

	public Communication(ClientCore core) {
		this.core = core;
	}

	private Socket socket;
	private BufferedInputStream input = null;
	private BufferedOutputStream output = null;

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

		if (len > 10000 || len == 0)
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

	public String communicate(String data) {

		String res = "";
		try {
			socket = new Socket();
			socket.connect(
					new InetSocketAddress(core.getHost(), core.getCommPort()),
					10000);
			input = new BufferedInputStream(socket.getInputStream());
			output = new BufferedOutputStream(socket.getOutputStream());

			sendInt(core.getClientId());
			System.out.println(data);

			sendString(data);

			res = readString();

		} catch (Exception e) {
			e.printStackTrace();
		}
		try {
			input.close();
		} catch (Exception ex) {
		}
		try {
			output.close();
		} catch (Exception ex) {
		}
		try {
			socket.close();
		} catch (Exception ex) {
		}
		try {
			Thread.sleep(1000);
		} catch (Exception e) {
		}
		return res;
	}
}
