package task.local;

import java.util.LinkedList;
import java.util.Random;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import basic.FileOps;

import com.cqz.dm.CodeReader;

import network.Client;

public class DoubanBasic {
	public static Random random = new Random();

	public synchronized static String login(String proxy, String account,
			String pwd) {
		System.out.println("Login with : " + account + "\t" + pwd);
		int id = random.nextInt(10000);
		String name = "douban_" + id + ".jpg";
		try {
			Client client = new Client(proxy, "", "");
			String content = client
					.getContent("https://accounts.douban.com/login");
			for (int trial = 0; trial < 1; trial++) {
				System.out.println(content);
				if (client.getCookie().contains("dbcl2="))
					break;

				LinkedList<NameValuePair> data = new LinkedList<NameValuePair>();
				data.add(new BasicNameValuePair("source", "None"));
				data.add(new BasicNameValuePair("redir",
						"http://www.douban.com"));
				data.add(new BasicNameValuePair("form_email", account));
				data.add(new BasicNameValuePair("form_password", pwd));

				String capSol = "";
				String capId = "";
				Matcher matcher = Pattern.compile(
						"name=\"captcha-id\" value=\"(.*?)\"").matcher(content);
				if (matcher.find()) {
					capId = matcher.group(1);
					System.out.println(capId);
					System.out.println(client.saveImg(
							"https://www.douban.com/misc/captcha?id=" + capId
									+ "&amp;size=s", name));
					System.out
							.println("https://www.douban.com/misc/captcha?id="
									+ capId + "&amp;size=s");
					// Scanner in = new Scanner(System.in);
					// capSol = in.nextLine();
					int code_id = 1008;
					capSol = CodeReader.getImgCode(name, code_id)[1];
					// FileOps.remove(name);
					// in.close();

				}

				data.add(new BasicNameValuePair("captcha-solution", capSol));
				data.add(new BasicNameValuePair("captcha-id", capId));
				data.add(new BasicNameValuePair("remember", "on"));
				data.add(new BasicNameValuePair("login", "登录"));

				content = client.sendPost("https://accounts.douban.com/login",
						data, "ps=y", "gb2312");
				if (trial > 0)
					try {
						Thread.sleep(3000);
					} catch (Exception e) {
					}
			}
			String c = client.getCookie();
			if (!c.contains("dbcl2="))
				c = "";
			return c;
		} catch (Exception ex) {
			return "";
		}
	}

	public static void main(String[] args) {
		System.out.println(login("local", "qgfafm622469@163.com", "d685383795"));
	}
}
