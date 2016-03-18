package task.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.sun.org.apache.bcel.internal.generic.NEW;

import controller.ClientCore;
import controller.ClientWorker;
import controller.ResultData;
import network.Client;

public class WeiboID extends TaskClient {

	@Override
	public ResultData work(String job, ClientCore core, ClientWorker worker) {
		ResultData res = new ResultData(getName(), job, null);

		String url = "http://weibo.cn/" + job;
//		Client client = worker.getHttpClient();
		Client client = new Client("049.4.129.149:80");
		
		if (client == null)
			return res;

		try {
			Thread.sleep(3000);
		} catch (Exception e) {
		}

		String content = client.getContent(url);

		try {
			if (content.contains("<div class=\"me\">失败</div>")) {
				res.data = new ArrayList<String>();
			} else {
				Matcher id_matcher = Pattern
						.compile("<a href=\"/(\\d*)/avatar").matcher(content);
				id_matcher.find();
				String id = id_matcher.group(1);
				
				Matcher ava_matcher = Pattern
						.compile("<img src=\"(.*?)\" alt=\"头像\"").matcher(content);
				ava_matcher.find();
				String avatar = ava_matcher.group(1);
				
				Matcher displayname_matcher = Pattern
						.compile("<td valign=\"top\"><div class=\"ut\"><span class=\"ctt\">(.*?)<").matcher(content);
				displayname_matcher.find();
				String displayname = displayname_matcher.group(1);

				Matcher weibo_matcher = Pattern
						.compile("微博\\[(\\d*)\\]").matcher(content);
				weibo_matcher.find();
				String weibo = weibo_matcher.group(1);

				Matcher follower_matcher = Pattern
						.compile("粉丝\\[(\\d*)\\]").matcher(content);
				follower_matcher.find();
				String follower = follower_matcher.group(1);

				Matcher followee_matcher = Pattern
						.compile("关注\\[(\\d*)\\]").matcher(content);
				followee_matcher.find();
				String followee = followee_matcher.group(1);

				ArrayList<String> curdata = new ArrayList<String>();
				curdata.add(id);
				curdata.add(job);
				curdata.add(displayname);
				curdata.add(avatar);
				curdata.add(weibo);
				curdata.add(follower);
				curdata.add(followee);
				System.err.println(id+"\t"+job+"\t"+displayname+"\t"+avatar+"\t"+weibo+"\t"+follower+"\t"+followee);
				res.data=curdata;
			}
		} catch (Exception e) {
			System.out.println(content);
			e.printStackTrace();
			worker.badHttpClient();
			return res;
		}

		return res;
	}
	
	public static void main(String[] args) {
		WeiboID cur=new WeiboID();
		cur.work("xuetucao", null, null);
	}
}
