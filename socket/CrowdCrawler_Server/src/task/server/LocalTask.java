package task.server;

import java.util.Random;

public class LocalTask extends TaskServer {

	@Override
	public void initialize() {
		super.initialize();
	}

	@Override
	public String getJob(int njob) {
		String res = "";
		for (int i = 0; i < 10; i++)
			res += new Random().nextDouble() + "\t";
		return res;
	}

	@Override
	public void submitResult(String data) {
		double s=Double.valueOf(data);
		System.out.println(s);
	}

	@Override
	public void taskFail(String job) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String communicate(String msg) {
		return null;
	}

	@Override
	public int getDQsize() {
		// TODO Auto-generated method stub
		return 0;
	}

}
