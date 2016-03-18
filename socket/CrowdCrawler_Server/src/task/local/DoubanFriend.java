package task.local;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import basic.FileOps;
import network.Client;
import network.ProxyBank;

public class DoubanFriend extends Thread {

	Connection conn = null;
	Random random = new Random();
	LinkedList<Integer> jobs = new LinkedList<Integer>();
	ArrayList<String[]> workers = new ArrayList<String[]>();
	ArrayList<String> proxies = new ArrayList<String>();
	ProxyBank proxyBank;

	private String fetch_friends(int workerid, int user_id, String proxy,
			String cookie) {
		try {
			sleep(5000 + random.nextInt(2000));
		} catch (Exception e) {
		}
		Client client = new Client(proxy);
		client.setCookie(cookie);
		String url = "http://www.douban.com/people/" + user_id + "/contacts";
		String content = client.getContent(url);
		System.out.println("Worker #" + workerid + " " + url);
		if (!content.contains("关注的人")) {
			System.out.println("Worker #" + workerid + " :　" + content);
			jobs.addFirst(user_id);
			if (content.equals("ERROR"))
				return "Proxy Fail";
			if (content.contains("手机验证码登录"))
				return "Need Login";
			if (content.contains("帐号被暂时锁定"))
				return "Account Fail";
			return "Proxy Fail";
		}

		try {
			// = Pattern.compile("<h1>(.*?关注的人(\\d*)").matcher(content);
			// matcher.find();
			// int cnt = Integer.valueOf(matcher.group(1));
			LinkedList<String[]> users = new LinkedList<String[]>();
			Matcher matcher = Pattern
					.compile(
							"<dt><a href=\"http://www.douban.com/people/(.*?)/\" class=\"nbg\"><img src=\"(.*?jpg)\" class=\"m_sub_img\" alt=\"(.*?)\"/></a></dt>")
					.matcher(content);
			for (; matcher.find();) {
				String[] cur = new String[3];
				cur[0] = matcher.group(1);
				cur[1] = matcher.group(2);
				cur[2] = matcher.group(3);
				users.add(cur);
			}
			System.err.println("Worker #" + workerid + " : " + url + "\t"
					+ users.size());
			PreparedStatement f_stat = conn
					.prepareStatement("insert ignore into friends (ida,idb) values (?,?)");
			PreparedStatement u_stat = conn
					.prepareStatement("insert ignore into user (id,active,displayid,nickname,avatar,location,intro,finished) values (?,1,?,?,?,'','',0)");
			PreparedStatement o_stat = conn
					.prepareStatement("insert ignore into friends_unknown (ida,idb,nickname) values (?,?,?)");
			for (String[] cur : users) {
				int id = 0;
				try {
					id = Integer.valueOf(cur[0]);
				} catch (Exception e) {
				}
				try {
					matcher = Pattern.compile("/icon/u(\\d*)-(\\d*).jpg")
							.matcher(cur[1]);
					if (matcher.find())
						id = Integer.valueOf(matcher.group(1));
				} catch (Exception e) {
				}
				if (id != 0) {
					f_stat.setInt(1, user_id);
					f_stat.setInt(2, id);
					f_stat.addBatch();
					u_stat.setInt(1, id);
					u_stat.setString(2, cur[0]);
					u_stat.setString(3, cur[2]);
					u_stat.setString(4, cur[1]);
					u_stat.addBatch();
				} else {
					// System.err.println("#### UNKNOWN FRIENDS ####");
					// System.out.println(cur[0] + "\t" + cur[1] + "\t" +
					// cur[2]);
					o_stat.setInt(1, user_id);
					o_stat.setString(2, cur[0]);
					o_stat.setString(3, cur[2]);
					o_stat.addBatch();
				}
			}
			f_stat.executeBatch();
			u_stat.executeBatch();
			o_stat.executeBatch();
			conn.createStatement().executeUpdate(
					"insert ignore into friends_crawled (id) values ("
							+ user_id + ")");
		} catch (Exception e) {
			e.printStackTrace();
		}
		proxyBank.reportGood(proxy);
		return "Good";
	}

	@Override
	public void run() {
		proxyBank = new ProxyBank("doubanproxy.txt");
		proxyBank.loadProxies();
		proxyBank.proxyLoader();
		proxyBank.initWeight();

		for (String s : FileOps.LoadFilebyLine("proxybank.txt"))
			proxies.add(s.split("\t")[0]);
		try {
			Class.forName("com.mysql.jdbc.Driver");
			conn = DriverManager
					.getConnection(
							"jdbc:mysql://172.16.2.33:3306/douban?characterEncoding=utf8&rewriteBatchedStatements=true",
							"root", "apex5879");

			ResultSet rs = conn
					.createStatement()
					.executeQuery(
							"SELECT id FROM `intro_weiboid` WHERE id not in (select id from friends_crawled)");
			for (; rs.next();)
				jobs.add(rs.getInt("id"));

			System.out.println(jobs.size());

			for (final String s : FileOps.LoadFilebyLine("douban.txt")) {
				System.out.println("Starting : " + s);
				Thread worker = new Thread() {
					@Override
					public void run() {
						int wid = 0;
						String[] sep = s.split("\t");
						synchronized (workers) {
							wid = workers.size();
							workers.add(new String[4]);
							workers.get(wid)[0] = sep[0];
							workers.get(wid)[1] = sep[1];
							workers.get(wid)[2] = "";
							workers.get(wid)[3] = "";
							if (sep.length > 2)
								workers.get(wid)[2] = sep[2];
							if (sep.length > 3)
								workers.get(wid)[3] = sep[3];
							if (workers.get(wid)[3].equals("ACCOUNTFAIL"))
								return;
						}
						int proxyfail = 0;
						for (; wid < 1000;) {
							if (workers.get(wid)[2].length() == 0) {
								System.err.println("Worker #" + wid
										+ " : Switching Proxy");
								String proxy = "";
								for (;;) {
									proxy = "";
									synchronized (proxyBank) {
										proxy = proxyBank.getProxy(1);
									}
									if (proxy.equals("No_Available_Proxy")) {
										proxy = "";
										break;
									}
									System.out.println("Trying " + proxy);
									long time = new Date().getTime();
									String content = new Client(proxy)
											.getContent("http://www.douban.com/");
									if (!content.contains("豆瓣")) {
										proxyBank.report(proxy);
										continue;
									}
									time = new Date().getTime() - time;
									 if (time < 5000)
									 break;
								}
								if (proxy.length() == 0) {
									System.err.println("Worker #" + wid
											+ " : No Proxy Available");
									break;
								}

								proxyBank.removeProxy(proxy);
								workers.get(wid)[2] = proxy;
								System.err.println("Worker #" + wid + " : "
										+ proxy);
							}
							if (workers.get(wid)[3].length() == 0) {
								System.err.println("Worker #" + wid
										+ " : Try Login");
								String cookie = DoubanBasic.login("local",
										workers.get(wid)[0],
										workers.get(wid)[1]);
								if (cookie.length() == 0) {
									System.err.println("Worker #" + wid
											+ " : Account Fail");
									workers.get(wid)[3] = "ACCOUNTFAIL";
									break;
								}
								workers.get(wid)[3] = cookie;
								System.err.println("Worker #" + wid + " : "
										+ cookie);
							}
							int cur = 0;
							synchronized (jobs) {
								if (!jobs.isEmpty())
									cur = jobs.removeFirst();
							}
							if (cur == 0) {
								try {
									sleep(5000);
									continue;
								} catch (Exception e) {
								}
							}
							String res = fetch_friends(wid, cur,
									workers.get(wid)[2], workers.get(wid)[3]);
							if (res.equals("Proxy Fail")) {
								proxyfail++;
								if (proxyfail > 10) {
									workers.get(wid)[2] = "";
								}
							} else
								proxyfail = 0;
							if (res.equals("Need Login"))
								workers.get(wid)[3] = "";
							if (res.equals("Account Fail")) {
								System.err.println("Worker #" + wid
										+ " : Account Fail");
								workers.get(wid)[3] = "ACCOUNTFAIL";
								break;
							}
						}
					}
				};
				worker.start();
				Thread.sleep(200);
			}

			new Thread() {
				public void run() {
					for (;;) {
						try {
							sleep(10000);
							LinkedList<String> out = new LinkedList<String>();
							for (String[] t : workers)
								out.add(t[0] + "\t" + t[1] + "\t" + t[2] + "\t"
										+ t[3]);
							FileOps.SaveFile("douban.txt", out);
						} catch (Exception e) {
						}
					}
				};
			}.start();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		try {
			sleep(60000);
		} catch (Exception e) {
		}
	}

	public static void main(String[] args) {
		new DoubanFriend().start();
	}
}
