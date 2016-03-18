package network;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import core.listener.Listenable;
import core.listener.ProxyListener;
import basic.FileOps;

public class ProxyBank extends Listenable<ProxyListener> {
	public HashMap<String, Double> proxy = new HashMap<String, Double>();
	private double decay = 0.5;
	private String filename = "proxybank.txt";

	public ProxyBank() {
	}

	public ProxyBank(String name) {
		filename = name;
	}

	public int size() {
		return proxy.size();
	}

	public void proxyLoader() {
		new Thread() {
			public void run() {
				for (;;)
					try {
						sleep(1000 * 60);
						saveProxies();
					} catch (Exception e) {
					}
			};
		}.start();
		Thread loader = new Thread() {
			int sleeptime = 5000;

			private void fetchProxies(String url, String pattern) {
				try {
					int cnt = 0;
					Client client = new Client("local");
					String content = client.getContent(url);
					Matcher matcher = Pattern.compile(pattern).matcher(content);
					for (; matcher.find(); cnt++) {
						addProxy(matcher.group(1),
								Integer.valueOf(matcher.group(2)));
					}
					System.out.println(cnt + " Proxies Fetched From :　" + url);
					sleep(sleeptime);
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}

			private void fetchProxies_wrapper(String url) {
				try {
					Client client = new Client(getProxy(1));
					LinkedList<NameValuePair> data = new LinkedList<NameValuePair>();
					data.add(new BasicNameValuePair("url", url));
					String content = client
							.sendPost(
									"http://proxy-list.org/english/extractor.php",
									data,
									"__utmt=1; __utma=68685392.2013796423.1447120542.1447120542.1447120542.1; __utmb=68685392.1.10.1447120542; __utmc=68685392; __utmz=68685392.1447120542.1.1.utmcsr=(direct)|utmccn=(direct)|utmcmd=(none); _ym_uid=1447120544209249557; _ym_visorc_24577874=w; PHPSESSID=gianjibnm8l7ar5evn8nn5pkm6",
									"UTF-8");
					Matcher matcher = Pattern.compile(
							"(\\d*\\.\\d*\\.\\d*\\.\\d*):(\\d*)").matcher(
							content);
					int cnt = 0;
					for (; matcher.find(); cnt++) {
						addProxy(matcher.group(1),
								Integer.valueOf(matcher.group(2)));
					}
					System.out.println(cnt + " Proxies Fetched From :　" + url);
					sleep(sleeptime);
				} catch (Exception e) {
					// TODO: handle exception
				}
			}

			public void run() {
				for (;;) {
					for (int i = 1; i <= 100; i++) {
						fetchProxies_wrapper("http://www.proxy.com.ru/list_"
								+ i + ".html");
					}

					fetchProxies_wrapper("http://cn-proxy.com/");
					//
					for (int i = 1; i <= 10; i++)
						fetchProxies("http://www.kuaidaili.com/proxylist/" + i
								+ "/",
								"<td>(\\d*\\.\\d*\\.\\d*\\.\\d*)</td>[\\s\\S]*?<td>(\\d*)</td>");

					// fetchProxies(
					// "http://www.site-digger.com/html/articles/20110516/proxieslist.html",
					// "<td>(\\d*\\.\\d*\\.\\d*\\.\\d*)</td>[\\s\\S]*?<td>(\\d*)</td>");
					//
					// fetchProxies("http://www.xicidaili.com/",
					// "<td>(\\d*\\.\\d*\\.\\d*\\.\\d*)</td>[\\s\\S]*?<td>(\\d*)</td>");
					// fetchProxies("http://www.xici.net.co/nn/",
					// "<td>(\\d*\\.\\d*\\.\\d*\\.\\d*)</td>[\\s\\S]*?<td>(\\d*)</td>");
					// fetchProxies("http://www.xici.net.co/nt/",
					// "<td>(\\d*\\.\\d*\\.\\d*\\.\\d*)</td>[\\s\\S]*?<td>(\\d*)</td>");
					// fetchProxies("http://www.xici.net.co/wn/",
					// "<td>(\\d*\\.\\d*\\.\\d*\\.\\d*)</td>[\\s\\S]*?<td>(\\d*)</td>");
					// fetchProxies("http://www.xici.net.co/wt/",
					// "<td>(\\d*\\.\\d*\\.\\d*\\.\\d*)</td>[\\s\\S]*?<td>(\\d*)</td>");

					fetchProxies("http://www.free-proxy-list.net",
							"<td>(\\d*\\.\\d*\\.\\d*\\.\\d*)</td><td>(\\d*)</td>");

					for (int i = 1; i <= 10; i++) {
						fetchProxies(
								"http://proxy-list.org/english/index.php?p="
										+ i,
								"<li class=\"proxy\">(\\d*\\.\\d*\\.\\d*\\.\\d*):(\\d*)</li>");
					}

					saveProxies();
				}
			}
		};
		loader.start();
	}

	public String getProxy(int limit) {
		String res = "";
		synchronized (this) {
			LinkedList<String> avp = new LinkedList<String>();
			avp.addAll(proxy.keySet());
			double s = 0;
			for (double t : proxy.values())
				s += t;
			s *= Math.random();
			for (String p : avp) {
				s -= proxy.get(p);
				if (s < 0) {
					res = p;
					break;
				}
			}

			if (res.length() == 0)
				return "No_Available_Proxy";
		}
		return res;
	}

	public void loadProxies() {
		synchronized (this) {
			proxy = new HashMap<String, Double>();
			for (String row : FileOps.LoadFilebyLine(filename)) {
				String[] sep = row.split("\t");
				if (check(sep[0])) {
					double v = 1;
					if (sep.length == 2)
						v = Double.valueOf(sep[1]);
					addProxy(sep[0], v);
				}
			}
		}
	}

	private boolean check(String string) {
		Matcher m = Pattern.compile("(\\d.*\\.\\d.*\\.\\d.*\\.\\d.*:\\d.*)")
				.matcher(string);
		if (!m.find())
			return false;
		if (m.group(1).length() != string.length())
			return false;
		return true;
	}

	public void report(String p) {
		try {
			System.out.println("Bad Proxy : " + p);
			releaseProxy(p);
			synchronized (this) {
				proxy.put(p, proxy.get(p) * decay);
				if (proxy.get(p) < 1e-6) {
					proxy.remove(p);
					for (ProxyListener listener : listeners)
						listener.removeProxy(p);
				}
			}
		} catch (Exception ex) {
		}
	}

	public void saveProxies() {
		synchronized (this) {
			LinkedList<String> content = new LinkedList<String>();
			for (String p : proxy.keySet())
				content.add(p + "\t" + proxy.get(p));
			FileOps.SaveFile(filename, content);
		}

	}

	public void addProxy(String p, double w) {
		p = p.trim();
		synchronized (this) {
			if (!proxy.containsKey(p))
				proxy.put(p, w);
		}
		for (ProxyListener listener : listeners)
			listener.addProxy(p);
	}

	public void addProxy(String p) {
		addProxy(p, 1.0);
	}

	public void addProxy(String ip, int port) {
		if (ip.length() > 18)
			return;
		if (ip.split("\\.").length != 4)
			return;
		addProxy(ip + ":" + port);
	}

	public void releaseProxy(String p) {
	}

	public void reportGood(String p) {
		System.out.println("Good Proxy : " + p);
		if (!check(p))
			return;
		synchronized (this) {
			proxy.put(p, 1.0);
		}
	}

	public void initWeight() {
		for (String p : proxy.keySet())
			proxy.put(p, 1.0);
	}

	public void removeProxy(String p) {
		proxy.remove(p);
	}
}
