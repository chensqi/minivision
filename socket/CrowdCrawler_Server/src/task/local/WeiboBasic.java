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
import com.sun.swing.internal.plaf.synth.resources.synth;

import network.Client;

public class WeiboBasic {
	public static Random random = new Random();

	public synchronized static String login(String account, String pwd) {
		int id = random.nextInt(10000);
//		if (account.length()>0) return "";
		if (id>=0) return "";
		String name = "weibo_" + id + ".jpg";
		try {
			Client client = new Client("local","","");
			client.setAgent("Mozilla/4.0 (compatible; MSIE 6.0; ) Opera/UCWEB7.0.2.37/28/999");
			String content = client.getContent("http://login.weibo.cn/login/");
			for (int trial = 0; trial < 5; trial++) {
				System.out.println(content);
				if (content.contains("帐号存在异常"))
					return "";
				if (content.length() == 0
						&& client.getCookie().contains("delet"))
					return "";
				if (!content.contains("记住登录状态"))
					break;
				Matcher matcher = Pattern.compile("rand=(\\d*)").matcher(
						content);
				matcher.find();
				String rand = matcher.group(1);
				matcher = Pattern.compile("password_\\d*").matcher(content);
				matcher.find();
				String pwd_name = matcher.group(0);
				matcher = Pattern.compile("name=\"vk\" value=\"(.*?)\"")
						.matcher(content);
				matcher.find();
				String vk = matcher.group(1);
				String capId = "";
				String code = "";

				if (content.contains("请输入图片中的字符")) {
					matcher = Pattern.compile("name=\"capId\" value=\"(.*?)\"")
							.matcher(content);
					matcher.find();
					capId = matcher.group(1);
					String url = "http://weibo.cn/interface/f/ttt/captcha/show.php?cpt="
							+ capId;
					System.err.println("capId : "+capId);
					System.out.println(client.saveImg(url, name));
//					 Scanner in = new Scanner(System.in);
//					 code = in.nextLine();
					int code_id = 1004;
					if (capId.startsWith("4"))
						code_id = 2103;
					
					for (int i=0;i<4;i++){
						String[] res=CodeReader.getImgCode(name, code_id);
						code=res[1];
						if (!res[0].startsWith("-")) break;
					}
					 FileOps.remove(name);
					// in.close();
				}
				System.out.println(account);
				System.out.println(client.getCookie());
				System.out.println(rand);
				System.out.println(pwd_name);
				System.out.println(vk);
				System.out.println(capId);
				System.out.println(code);
				System.out.println(name);

				LinkedList<NameValuePair> data = new LinkedList<NameValuePair>();
				data.add(new BasicNameValuePair("mobile", account));
				data.add(new BasicNameValuePair(pwd_name, pwd));
				data.add(new BasicNameValuePair("code", code));
				data.add(new BasicNameValuePair("remember", "on"));
				data.add(new BasicNameValuePair("backurl",
						"http%3A%2F%2Fweibo.cn"));
				data.add(new BasicNameValuePair("backTitle", "手机新浪网"));
				data.add(new BasicNameValuePair("vk", vk));
				data.add(new BasicNameValuePair("capId", capId));
				data.add(new BasicNameValuePair("submit", "登录"));

				content = client.sendPost("http://login.weibo.cn/login/", data,
						"","UTF-8");
				 System.out.println(content);
				if (trial > 0)
					try {
						Thread.sleep(3000);
					} catch (Exception e) {
					}
			}
			return client.getCookie();
		} catch (Exception ex) {
			return "";
		}
	}

	public static void main(String[] args) {
//		System.out.println(CodeReader.getImgCode("weibo_2097.jpg", 1004)[0]);
		System.out.println(login("nieew07470@163.com", "a123456"));
	}
}
