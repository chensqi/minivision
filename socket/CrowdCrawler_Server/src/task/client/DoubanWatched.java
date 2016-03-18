package task.client;

import java.util.Calendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import socket.client.Communication;
import controller.ClientCore;
import controller.ClientWorker;
import network.Client;

public class DoubanWatched extends TaskClient {
	private static String cookie = "";

	private Long lastupdate = Long.valueOf(0);

	@Override
	public String work(String job, ClientCore core, ClientWorker worker) {
		String[] sep = job.split(",");
		String url = "http://m.douban.com/movie/people/" + sep[0]
				+ "/watched?page=" + sep[1];

		Client client = worker.getHttpClient();
		if (client == null) {
			return "ERROR#1";
		}
		client.setCookie(cookie);

		String content = client.getContent(url);
		String res = "";

		if (content.contains("页面无法打开"))
			return "";
		if (content.contains("此应用出错了")) {
			try {
				Thread.sleep(100000);
			} catch (Exception e) {
			}
			return "ERROR#0";
		}
		if (content.contains("请输入上图中的单词")) {
			boolean update = false;
			long now = Calendar.getInstance().getTimeInMillis();

			synchronized (lastupdate) {
				if (now > lastupdate + 60000) {
					update = true;
					lastupdate = now;
				}
			}
			if (update) {
				cookie = new Communication(core).communicate(getName()
						+ ":Cookie");
			} else {
				try {
					Thread.sleep(30000);
				} catch (Exception e) {
				}
			}
			return "ERROR-Switch-Cookie";
		}
		if (!content.contains("看过的电影")) {
			try {
				Thread.sleep(30000);
			} catch (Exception e) {
			}
			System.out.println(url);
			System.out.println(content);
			worker.badHttpClient();
			if (content.equals("ERROR"))
				return "ERROR#2";
			else
				return "ERROR#3";
		} else {
//			 System.out.println(content);
			Matcher mdiv = Pattern.compile("<div class=\"item\">(.*?)</div>")
					.matcher(content);
			for (; mdiv.find();) {
				String cur = mdiv.group(1);
				Matcher m = Pattern
						.compile(
								"/movie/subject/(.*?)/.*?(\\d)星.*?<br>(\\d{4}-\\d{2}-\\d{2})\\W(.*)")
						.matcher(cur);
				if (m.find()) {
					int mid = Integer.valueOf(m.group(1));
					int rate = Integer.valueOf(m.group(2));
					String date = m.group(3);
					String rest = m.group(4);
					String tag = "";
					String review = "";
					for (String seg : rest.split("<br>")) {
						String t = seg.trim();
						if (t.startsWith("标签"))
							tag = t;
						if (t.startsWith("短评"))
							review = t;
					}
					res += mid + "#" + rate + "#" + date + "#"
							+ tag.replace('#', '$').replace("@", "$") + " #"
							+ review.replace('#', '$').replace("@", "$") + " @";
				}
			}
		}
		return res;
	}
}
