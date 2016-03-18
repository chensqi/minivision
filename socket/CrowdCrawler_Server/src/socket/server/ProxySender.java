package socket.server;

import basic.Config;
import socket.ServerLongConnection;

public class ProxySender extends ServerLongConnection {
	private static String lock = "lock";

	@Override
	protected String onData(String data) {
		try {
			int cnt = Integer.valueOf(data);
			System.out.println(clientId + " : Proxy Request - " + cnt);

			if (cnt == 0)
				return null;

			synchronized (lock) {
				String proxies = "";
				int limit = Config.getInt("ProxyLimit");
				for (int i = 0; i < cnt; i++) {
					try {
						String p = core.getProxyBank().getProxy(limit);
						if (proxies.length() > 0)
							proxies += ";";
						proxies += p;
					} catch (Exception ex) {
					}
				}
				System.out.println(clientId + " : Proxies - " + proxies);
				return proxies;
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return null;
	}

}
