package task.client;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import controller.ClientCore;
import controller.ClientWorker;
import controller.ResultData;
import network.Client;

public class DoubanMovieRated extends TaskClient {
	private static String cookie = "";

	@Override
	public ResultData work(String job, ClientCore core, ClientWorker worker) {
		String[] sep = job.split(",");
		String url = "http://movie.douban.com/subject/" + sep[0]
				+ "/collections?start=" + ((Integer.valueOf(sep[1]) - 1) * 20);
		Client client = worker.getHttpClient();
		if (client == null) {
			return "ERROR#1";
		}
		client.setCookie(cookie);

		String content = client.getContent(url);
		String res = "";

		if (!content.contains("的豆瓣成员")) {
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
			// System.out.println(content);
//			Matcher mdiv = Pattern.compile("<table width=\"100%\" class=\"\">(.*?)</table>")
//					.matcher(content);
//			for (; mdiv.find();) {
//				String cur = mdiv.group(1);
//				Matcher m = Pattern.compile(
//						"http://movie.douban.com/people/(.*?)/\" class=\"\">(.*?)<").matcher(cur);
//				if (!m.find())
//					continue;
//				String uid = m.group(1);
//				String username=m.group(2).trim();
//
//				String location=" ";
//				m = Pattern.compile("<span style=\"font-size:12px;\">\\((.*?)\\)</span>").matcher(cur);
//				if (m.find())
//					location=m.group(1);
//				
//				
//
//				m = Pattern.compile("<span class=\"date\">(.*?)</span>")
//						.matcher(cur);
//
//				if (!m.find())
//					continue;
//				String date = m.group(1);
//
//				String review = "";
//				m = Pattern.compile("<span class=\"comment\">(.*?)</span>")
//						.matcher(cur);
//				if (m.find())
//					review = m.group(1);
//
//				res += mid + "#" + rate + "#" + date + "#"
//						+ review.replace('#', '$').replace("@", "$") + " @";
//			}
		}
		return res;
	}
}
