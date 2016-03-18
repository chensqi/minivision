package task.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import socket.client.Communication;

import com.sun.org.apache.bcel.internal.generic.NEW;

import controller.ClientCore;
import controller.ClientWorker;
import controller.ResultData;
import network.Client;

public class Weibo extends TaskClient {

	static String cookie = "";

	@Override
	public ResultData work(String job, ClientCore core, ClientWorker worker) {
		ResultData res = new ResultData(getName(), job, null);

		String[] sep = job.split("\t");
		String url = "http://weibo.cn/" + sep[1];
		Client client = worker.getHttpClient();
		client.setCookie(cookie);
		System.out.println("Working : " + url);
		System.out.println("Cookie : " + cookie);
		// Client client = new Client("049.4.129.149:80");

		if (client == null)
			return res;

		try {
			Thread.sleep(3000);
		} catch (Exception e) {
		}

		String content = client.getContent(url);
		System.out.println(content);

		if (content.equals("ERROR")) {
			worker.badHttpClient();
			return res;
		}
		if (content.contains("手机号/电子邮箱/会员帐号")) {
			System.out.println("Query Cookie");
			cookie = new Communication(core).communicate(getName() + ":Cookie");
			return res;
		}

		try {
			if (content.contains("<div class=\"me\">失败</div>")) {
				res.data = new ArrayList<String>();
			} else {
				Matcher id_matcher = Pattern
						.compile("<a href=\"/(\\d*)/avatar").matcher(content);
				id_matcher.find();
				String id = id_matcher.group(1);

				Matcher ava_matcher = Pattern.compile(
						"avatar(.*?)<img src=\"(.*?)\" alt=\"头像\"").matcher(
						content);
				ava_matcher.find();
				String avatar = ava_matcher.group(2);

				Matcher displayname_matcher = Pattern
						.compile(
								"<td valign=\"top\"><div class=\"ut\"><span class=\"ctt\">(.*?)<")
						.matcher(content);
				displayname_matcher.find();
				String[] sepd = displayname_matcher.group(1).split("&nbsp;");
				String displayname = sepd[0].trim();
				String gender = "";
				String location = "";
				try {
					String[] tsep = sepd[1].split("/");
					gender = tsep[0].trim();
					location = tsep[1].trim();
				} catch (Exception e) {
				}

				Matcher weibo_matcher = Pattern.compile("微博\\[(\\d*)\\]")
						.matcher(content);
				weibo_matcher.find();
				String weibo = weibo_matcher.group(1);

				Matcher follower_matcher = Pattern.compile("粉丝\\[(\\d*)\\]")
						.matcher(content);
				follower_matcher.find();
				String follower = follower_matcher.group(1);

				Matcher followee_matcher = Pattern.compile("关注\\[(\\d*)\\]")
						.matcher(content);
				followee_matcher.find();
				String followee = followee_matcher.group(1);

				ArrayList<String> curdata = new ArrayList<String>();
				curdata.add(id);
				curdata.add(sep[1]);
				curdata.add(displayname);
				curdata.add(avatar);
				curdata.add(gender);
				curdata.add(location);
				curdata.add(weibo);
				curdata.add(follower);
				curdata.add(followee);
				System.err.println(id + "\t" + job + "\t" + displayname + "\t"
						+ avatar + "\t" + weibo + "\t" + follower + "\t"
						+ followee);
				res.data = curdata;
			}
		} catch (Exception e) {
			System.out.println(content);
			e.printStackTrace();
			worker.badHttpClient();
			return res;
		}
		return res;
	}

}
