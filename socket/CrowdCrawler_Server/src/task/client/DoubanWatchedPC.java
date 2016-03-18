package task.client;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import controller.ClientCore;
import controller.ClientWorker;
import network.Client;

public class DoubanWatchedPC extends TaskClient {
	private static String cookie = "";

	@Override
	public String work(String job, ClientCore core, ClientWorker worker) {
		String[] sep = job.split(",");
		String url = "http://movie.douban.com/people/" + sep[0]
				+ "/collect?start=" + ((Integer.valueOf(sep[1]) - 1) * 15);
		Client client = worker.getHttpClient();
		if (client == null) {
			return "ERROR#1";
		}
		client.setCookie(cookie);

		try {
			Thread.sleep(3000);
		} catch (Exception e) {
		}
		String content = client.getContent(url);
		String res = "";

		if (content.contains("你想访问的页面不存在"))
			return "";
		if (content.contains("此应用出错了")) {
			try {
				Thread.sleep(100000);
			} catch (Exception e) {
			}
			return "ERROR#0";
		}
		if (!content.contains("看过的电影")) {
//			try {
//				Thread.sleep(5000);
//			} catch (Exception e) {
//			}
			System.out.println(url);
			System.out.println(content);
			worker.badHttpClient();
			if (content.equals("ERROR"))
				return "ERROR#2";
			else
				return "ERROR#3";
		} else {
			// System.out.println(content);
			Matcher mdiv = Pattern.compile("<div class=\"info\">(.*?)</div>")
					.matcher(content);
			for (; mdiv.find();) {
				String cur = mdiv.group(1);
				Matcher m = Pattern.compile(
						"http://movie.douban.com/subject/(\\d*)/").matcher(cur);
				if (!m.find())
					continue;
				String mid = m.group(1);

				String rate = "0";
				m = Pattern.compile("<span class=\"rating(\\d*)").matcher(cur);
				if (m.find())
					rate = m.group(1);

				m = Pattern.compile("<span class=\"date\">(.*?)</span>")
						.matcher(cur);

				if (!m.find())
					continue;
				String date = m.group(1);

				String review = "";
				m = Pattern.compile("<span class=\"comment\">(.*?)</span>")
						.matcher(cur);
				if (m.find())
					review = m.group(1);

				res += mid + "#" + rate + "#" + date + "#"
						+ review.replace('#', '$').replace("@", "$") + " @";
			}
		}
		return res;
	}
}
