package task.client;

import controller.ClientCore;
import controller.ClientWorker;

public class LocalTask extends TaskClient {

	@Override
	public String work(String job,ClientCore core,ClientWorker worker) {
		double sum = 0;
		for (String s : job.split("\t"))
			try {
				sum += Double.valueOf(s);
			} catch (Exception e) {
			}
		return "" + sum;
	}

}
