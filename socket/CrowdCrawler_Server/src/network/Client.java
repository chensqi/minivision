package network;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Random;

import javax.imageio.ImageIO;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.NameValuePair;

import com.cqz.dm.CodeReader;

public class Client {
	public static String ERROR = "Error";
	static {
		System.setProperty("org.apache.commons.logging.Log",
				"org.apache.commons.logging.impl.SimpleLog");
		System.setProperty("org.apache.commons.logging.simplelog.showdatetime",
				"true");
		System.setProperty("org.apache.commons.logging"
				+ ".simplelog.log.org.apache.commons.httpclient", "error");
	}
	HttpClient client;
	HttpClientContext context;
	public String proxy;
	RequestConfig conf;
	String cookie;
	public String userAgent = "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/46.0.2490.86 Safari/537.36";

	public Client(String proxy) {
		init(proxy);
	}

	public Client(String proxy, String userAgent, String cookie) {
		init(proxy);
		if (userAgent.length() != 0)
			this.userAgent = userAgent;
		if (cookie.length() != 0)
			this.cookie = cookie;
	}

	public void setCookie(String c) {
		cookie = c;
	}

	public void setUserAgent(String userAgent) {
		this.userAgent = userAgent;
	}

	public void init(String proxy) {

		client = HttpClients.createDefault();
		if (proxy.startsWith("local")) {
			this.proxy = "local";
			conf = RequestConfig.custom().setSocketTimeout(10000)
					.setConnectTimeout(5000)
					.setCookieSpec(CookieSpecs.IGNORE_COOKIES).build();

		} else {
			this.proxy = proxy;
			String[] part = this.proxy.split(":");
			HttpHost p = new HttpHost(part[0], Integer.valueOf(part[1]));
			conf = RequestConfig.custom().setSocketTimeout(30000)
					.setConnectTimeout(15000)
					.setCookieSpec(CookieSpecs.IGNORE_COOKIES).setProxy(p)
					.build();
		}
		context = HttpClientContext.create();
		context.setCookieStore(new BasicCookieStore());
		cookie = "";
	}

	public boolean saveImg(String url, String path) {
		try {
			HttpGet get = new HttpGet(url);
			get.setConfig(conf);
			get.setHeader("Cookie", cookie);
			get.setHeader("User-Agent", userAgent);
			HttpResponse res = client.execute(get);
			HttpEntity entity = res.getEntity();
			if (entity != null) {
				InputStream instream = entity.getContent();
				ImageIO.write(ImageIO.read(instream), "jpg", new File(path));
				return true;
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return false;
	}

	public String getContent(String url) {
		return getContent(url, "UTF-8");
	}

	private void addCookie(String s) {
		HashMap<String, String> parse = new HashMap<String, String>();
		for (String sub : (cookie + ";" + s).trim().split(";")) {
			try {
				String cur = sub.trim();
				if (cur.length() == 0)
					continue;
				if (cur.startsWith("expires"))
					continue;
				if (cur.startsWith("Max-Age"))
					continue;
				if (cur.startsWith("path"))
					continue;
				if (cur.startsWith("domain"))
					continue;
				int pos = cur.indexOf('=');
				parse.put(cur.substring(0, pos), cur.substring(pos + 1));
			} catch (Exception ex) {
			}
		}
		cookie = "";
		for (String ss : parse.keySet()) {
			cookie += ss + "=" + parse.get(ss) + ";";
		}
		// System.out.println("Cookie : " + cookie);
	}

	public void setDoubanCookie() {
		String curcookie = "bid=\"";
		Random random = new Random();
		for (int i = 0; i < 13; i++) {
			int cur = random.nextInt(52);
			if (cur < 26)
				curcookie += (char) ('a' + cur);
			else
				curcookie += (char) ('A' + cur - 26);
		}
		curcookie += "\"";
		cookie = curcookie;
		// int id1 = random.nextInt(8000) + 2000;
		// int id2 = random.nextInt(90) + 10;
		// String lastAgent = userAgent.replace("####", "" + id1).replace("@@",
		// "" + id2);
	}

	public String getContent(String url, String encoding) {
		client = HttpClients.createDefault();
		try {
			HttpGet get = new HttpGet(url);
			get.setConfig(conf);
			get.setHeader("Cookie", cookie);
			get.setHeader("User-Agent", userAgent);
			CloseableHttpResponse res = (CloseableHttpResponse) client.execute(
					get, context);
			for (Header header : res.getAllHeaders()) {
				if (header.getName().equals("Set-Cookie"))
					addCookie(header.getValue());
			}
			BufferedReader rd = new BufferedReader(new InputStreamReader(res
					.getEntity().getContent(), encoding));
			StringBuffer result = new StringBuffer();
			String line = "";
			while ((line = rd.readLine()) != null) {
				result.append(line);
			}
			return result.toString();
		} catch (Exception ex) {
			// ex.printStackTrace();
		}
		return "ERROR";
	}

	public String sendPost(String url, LinkedList<NameValuePair> params,
			String cookie_cur, String encode) {
		client = HttpClients.createDefault();
		try {
			HttpPost login = new HttpPost(url);
			login.setConfig(conf);
			login.setHeader("User-Agent",
					"Mozilla/5.0 (Windows NT 5.1; rv:2.0.1) Gecko/20100101 Firefox/4.0.1");
			login.setHeader("Content-Type", "application/x-www-form-urlencoded");
			login.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));

			login.setHeader("Cookie", cookie + "; " + cookie_cur);
			login.setHeader("accept-language", "zh-CN,zh;q=0.8");
			login.setHeader("upgrade-insecure-requests", "1");

			HttpResponse res = client.execute(login);

			for (Header header : res.getAllHeaders()) {
				if (header.getName().equals("Set-Cookie"))
					addCookie(header.getValue());
			}
			BufferedReader rd = new BufferedReader(new InputStreamReader(res
					.getEntity().getContent(), encode));

			StringBuffer result = new StringBuffer();
			String line = "";
			while ((line = rd.readLine()) != null) {
				result.append(line + " ");
			}
			return result.toString();
		} catch (Exception ex) {
		}
		return "ERROR";
	}

	public void setAgent(String s) {
		userAgent = s;
	}

	public String getCookie() {
		return cookie;
	}

	public static void main(String[] args) {
		Client client = new Client("222.74.212.163:3128", "", "");
		// Client client=new Client("local");
		client.setDoubanCookie();
		System.out.println(client.getContent("http://www.baidu.com/"));
		// client.setCookie("_T_WM=31763addd18a502d106dffc9f841kci3");
		// System.out.println(client.saveImg("http://weibo.cn/interface/f/ttt/captcha/show.php?cpt=2_844199c695593ed5",
		// "weibo.jpg"));
		// System.out.println(client.getContent("http://weibo.cn/1742566624/info"));
		System.out.println(client
				.getContent("http://wap.douban.com/people/9893423/about"));
		// System.out.println(client.getContent("https://www.douban.com/people/3135564/"));
		// System.out.println(client.getContent("http://proxy-list.org/english/extractor.php"));
		// System.out.println(client.getContent("http://www.proxy.com.ru/list_1.html"));
		// Client client=new Client("local");
		// System.out.println(client.getContent("http://www.douban.com"));
	}
}
