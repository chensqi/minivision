package socket.server;

import socket.ServerConnection;
import socket.ServerLongConnection;
import controller.ResultData;
import controller.Scheduler;

public class DataReceiver extends ServerConnection {

	@Override
	protected void transaction_inner() throws Exception {
		for (int i = 0;; i++) {
			core.ClientConnected(clientId);
			ResultData data = (ResultData)readObject();
			if (clientId.contains("557004")) continue;
			onData(data);
		}
	}
	private void onData(ResultData data) {
		System.out.println(clientId + " Data Received : " + data);
		core.clientSubmit(clientId);
//		core.clientStatus(clientId, "Submitted");
		if (data.task.equals("ProxyControl")) {
			String proxy = data.job;
			boolean isgood=(Boolean)data.data;
			if (isgood)
				core.getProxyBank().reportGood(proxy);
			else 
				core.getProxyBank().report(proxy);
		}
		else 
			Scheduler.jobFinished(clientId, data);
	}
}
