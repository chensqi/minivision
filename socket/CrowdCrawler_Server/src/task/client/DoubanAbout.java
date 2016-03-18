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

public class DoubanAbout extends TaskClient {

	@Override
	public ResultData work(String job, ClientCore core, ClientWorker worker) {
//		System.out.println(job);
		ResultData res = new ResultData(getName(), job, null);
		String[] sep = job.split("\t");

		String url = "http://wap.douban.com/people/" + sep[0] + "/about";
		Client client = worker.getHttpClient();
		client.setAgent(sep[1].replace("@", ";"));
		client.setDoubanCookie();

		if (client == null)
			return res;

		try {
			Thread.sleep(3000);
		} catch (Exception e) {
		}
		String content = client.getContent(url);

		if (content.contains("你要访问的页面不存在")) {
			res.setData(new ArrayList<String>());
			return res;
		}
		if (!content.contains("的个人信息")) {
			System.out.println(url);
//			System.out.println(content);
			worker.badHttpClient();
			if (content.equals("ERROR"))
				return res;
			else
				return res;
		} else {
			try {
				Matcher m_name = Pattern.compile("class=\"founder\">(.*?)</a>")
						.matcher(content);
				m_name.find();
				String name = m_name.group(1);

				Matcher m_pic = Pattern.compile(
						"<img src=\"(.*?)\" alt=\"头像\"/>").matcher(content);
				String pic = "";
				if (m_pic.find())
					pic = m_pic.group(1);

				Matcher m_id = Pattern.compile("id：</span>(.*?)<br />")
						.matcher(content);
				String id = "";
				m_id.find();
				id = m_id.group(1).trim();

				Matcher m_loc = Pattern.compile("常居地：</span>(.*?)<br />")
						.matcher(content);
				String loc = "";
				if (m_loc.find())
					loc = m_loc.group(1).trim();

				Matcher m_intro = Pattern.compile(
						"<div class=\"intro\">(.*?)</div>").matcher(content);
				String intro = "";
				if (m_intro.find())
					intro = m_intro.group(1).trim();
				System.err.println(job);
				System.err.println(name);
				System.err.println(pic);
				System.err.println(id);
				System.err.println(loc);
//				System.err.println(intro);
				ArrayList<String> infos = new ArrayList<String>();
				infos.add(id);
				infos.add(name);
				infos.add(pic);
				infos.add(loc);
				infos.add(intro);
				res.setData(infos);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return res;
	}
}
