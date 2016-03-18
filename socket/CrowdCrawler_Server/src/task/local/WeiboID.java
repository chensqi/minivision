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

public class WeiboID extends Thread {

	Connection dou_conn = null;
	Connection wei_conn = null;
	Random random = new Random();
	LinkedList<String[]> jobs = new LinkedList<String[]>();
	ArrayList<String[]> workers = new ArrayList<String[]>();
	ArrayList<String> proxies = new ArrayList<String>();

	private String fetch_weiboid(int workerid, int id, String rawid,
			String url, String proxy, String cookie) {
		try {
			sleep(5000 + random.nextInt(2000));
		} catch (Exception e) {
		}
		String c1 = "", c2 = "", c3 = "";
		Client client = new Client(proxy);
		client.setCookie(cookie);
		String content = client.getContent("http://weibo.cn/" + rawid);
		c1=content;
		// System.out.println("Worker #" + workerid + " :　http://weibo.cn/"
		// + rawid);
		// System.out.println("Worker #" + workerid + " :　" + content);
		if (content.equals("ERROR"))
			return "Proxy Fail";
		if (content.contains("记住登录状态")) {
			return "Need Login";
		}
		if (content.contains("帐号存在异常"))
			return "Account Fail";
		try {
			Matcher id_matcher = Pattern.compile("<a href=\"/(\\d*)/avatar")
					.matcher(content);
			id_matcher.find();
			String wid = id_matcher.group(1);

			Matcher ava_matcher = Pattern.compile(
					"avatar(.*?)<img src=\"(.*?)\" alt=\"头像\"")
					.matcher(content);
			ava_matcher.find();
			String avatar = ava_matcher.group(2);

			Matcher displayname_matcher = Pattern
					.compile(
							"<td valign=\"top\"><div class=\"ut\"><span class=\"ctt\">(.*?)<")
					.matcher(content);
			displayname_matcher.find();
			String[] sep = displayname_matcher.group(1).split("&nbsp;");
			String displayname = sep[0].trim();
			String gender = "";
			String location = "";
			try {
				String[] tsep = sep[1].split("/");
				gender = tsep[0].trim();
				location = tsep[1].trim();
			} catch (Exception e) {
			}

			Matcher weibo_matcher = Pattern.compile("微博\\[(\\d*)\\]").matcher(
					content);
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

			System.err.println("Worker #" + workerid + " :　" + wid + "\t"
					+ rawid + "\t" + displayname + "\t" + avatar + "\t"
					+ gender + "\t" + location + "\t" + weibo + "\t" + follower
					+ "\t" + followee);
			synchronized (wei_conn) {
				PreparedStatement stat = wei_conn
						.prepareStatement("insert ignore into user (id,alias,displayname,avatar,gender,location,weibo,follower,followee,finished) values (?,?,?,?,?,?,?,?,?,1)");
				stat.setLong(1, Long.valueOf(wid));
				stat.setString(2, rawid);
				stat.setString(3, displayname);
				stat.setString(4, avatar);
				stat.setString(5, gender);
				stat.setString(6, location);
				stat.setInt(7, Integer.valueOf(weibo));
				stat.setInt(8, Integer.valueOf(follower));
				stat.setInt(9, Integer.valueOf(followee));
				stat.executeUpdate();
				stat = wei_conn
						.prepareStatement("update user set alias=?,displayname=?,avatar=?,gender=?,location=?,weibo=?,follower=?,followee=?,finished=1 where id="
								+ wid);
				stat.setString(1, rawid);
				stat.setString(2, displayname);
				stat.setString(3, avatar);
				stat.setString(4, gender);
				stat.setString(5, location);
				stat.setInt(6, Integer.valueOf(weibo));
				stat.setInt(7, Integer.valueOf(follower));
				stat.setInt(8, Integer.valueOf(followee));
				stat.executeUpdate();
			}
			synchronized (dou_conn) {
				dou_conn.createStatement().executeUpdate(
						"update intro_weiboid set weiboid=" + wid
								+ " where raw='" + rawid + "'");
			}
			return "Good";
		} catch (Exception e) {
		}
		try {
			content = client.getContent("http://" + url);
			c2 = content;
			Matcher matcher = Pattern.compile(
					"href=\"http://weibo.com/u/(\\d*)").matcher(content);
			matcher.find();
			String wid = matcher.group(1);
			content = client.getContent("http://weibo.cn/" + wid);
			c3 = content;
			Matcher ava_matcher = Pattern.compile(
					"avatar(.*?)<img src=\"(.*?)\" alt=\"头像\"")
					.matcher(content);
			ava_matcher.find();
			String avatar = ava_matcher.group(2);

			Matcher displayname_matcher = Pattern
					.compile(
							"<td valign=\"top\"><div class=\"ut\"><span class=\"ctt\">(.*?)<")
					.matcher(content);
			displayname_matcher.find();
			String[] sep = displayname_matcher.group(1).split("&nbsp;");
			String displayname = sep[0].trim();
			String gender = "";
			String location = "";
			try {
				String[] tsep = sep[1].split("/");
				gender = tsep[0].trim();
				location = tsep[1].trim();
			} catch (Exception e) {
			}

			Matcher weibo_matcher = Pattern.compile("微博\\[(\\d*)\\]").matcher(
					content);
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

			System.err.println("Worker #" + workerid + " :　" + wid + "\t"
					+ rawid + "\t" + displayname + "\t" + avatar + "\t"
					+ gender + "\t" + location + "\t" + weibo + "\t" + follower
					+ "\t" + followee);
			synchronized (wei_conn) {
				PreparedStatement stat = wei_conn
						.prepareStatement("insert ignore into user (id,alias,displayname,avatar,gender,location,weibo,follower,followee) values (?,?,?,?,?,?,?,?,?)");
				stat.setLong(1, Long.valueOf(wid));
				stat.setString(2, rawid);
				stat.setString(3, displayname);
				stat.setString(4, avatar);
				stat.setString(5, gender);
				stat.setString(6, location);
				stat.setInt(7, Integer.valueOf(weibo));
				stat.setInt(8, Integer.valueOf(follower));
				stat.setInt(9, Integer.valueOf(followee));
				stat.executeUpdate();
				stat = wei_conn
						.prepareStatement("update user set alias=?,displayname=?,avatar=?,gender=?,location=?,weibo=?,follower=?,followee=? where id="
								+ wid);
				stat.setString(1, rawid);
				stat.setString(2, displayname);
				stat.setString(3, avatar);
				stat.setString(4, gender);
				stat.setString(5, location);
				stat.setInt(6, Integer.valueOf(weibo));
				stat.setInt(7, Integer.valueOf(follower));
				stat.setInt(8, Integer.valueOf(followee));
				stat.executeUpdate();
			}
			synchronized (dou_conn) {
				dou_conn.createStatement().executeUpdate(
						"update intro_weiboid set weiboid=" + wid
								+ " where id='" + id + "'");
			}
			return "Good";
		} catch (Exception e) {
		}
		System.err.println("Worker #" + workerid + " : " + rawid + "\t" + url
				+ "\tNot Found");
//		 System.err.println(c1);
		// System.err.println(c2);
		// System.err.println(c3);
		try {
			synchronized (dou_conn) {
				dou_conn.createStatement().executeUpdate(
						"update intro_weiboid set weiboid=-1 where raw='"
								+ rawid + "'");
			}
		} catch (Exception e2) {
		}
		return "Good";
	}

	@Override
	public void run() {
		for (String s : FileOps.LoadFilebyLine("proxybank.txt"))
			proxies.add(s.split("\t")[0]);
		try {
			Class.forName("com.mysql.jdbc.Driver");
			dou_conn = DriverManager
					.getConnection(
							"jdbc:mysql://172.16.2.33:3306/douban?characterEncoding=utf8&rewriteBatchedStatements=true",
							"root", "apex5879");
			wei_conn = DriverManager
					.getConnection(
							"jdbc:mysql://172.16.2.33:3306/weibo?characterEncoding=utf8&rewriteBatchedStatements=true",
							"root", "apex5879");
			ResultSet rs = dou_conn.createStatement().executeQuery(
					"select * from intro_weiboid where weiboid=''");
			for (; rs.next();) {
				String wid = rs.getString("weiboid");
				String[] cur = new String[3];
				cur[0] = "" + rs.getInt("id");
				cur[1] = rs.getString("raw");
				cur[2] = rs.getString("url");
				jobs.add(cur);
			}
			System.out.println(jobs.size());

			for (final String s : FileOps.LoadFilebyLine("weibo.txt")) {
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
									synchronized (proxies) {
										if (proxies.isEmpty()) {
											break;
										}
										proxy = proxies.remove(random
												.nextInt(proxies.size()));
									}
									System.out.println("Trying " + proxy);
									long time = new Date().getTime();
									String content = new Client(proxy, "", "")
											.getContent("http://weibo.cn/");
									System.out.println(content);
									if (!content.contains("登录"))
										continue;
									time = new Date().getTime() - time;
									if (time < 5000)
										break;
								}
								if (proxy.length() == 0) {
									System.err.println("Worker #" + wid
											+ " : No Proxy Available");
									break;
								}
								workers.get(wid)[2] = proxy;
							}
							if (workers.get(wid)[3].length() == 0) {
								System.err.println("Worker #" + wid
										+ " : Try Login");
								String cookie =""; 
//										WeiboBasic.login(
//										workers.get(wid)[0],
//										workers.get(wid)[1]);
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
							String[] cur = null;
							synchronized (jobs) {
								if (!jobs.isEmpty())
									cur = jobs.removeFirst();
							}
							if (cur == null) {
								try {
									sleep(5000);
									continue;
								} catch (Exception e) {
								}
							}
							int id = Integer.valueOf(cur[0]);
							String res = fetch_weiboid(wid, id, cur[1], cur[2],
									workers.get(wid)[2], workers.get(wid)[3]);
							if (!res.equals("Good"))
								synchronized (jobs) {
									jobs.add(cur);
								}
							if (res.equals("Proxy Fail")) {
								proxyfail++;
								if (proxyfail > 5)
									workers.get(wid)[2] = "";
							} else if (res.equals("Need Login"))
								workers.get(wid)[3] = "";
							else if (res.equals("Account Fail")) {
								System.err.println("Worker #" + wid
										+ " : Account Fail");
								workers.get(wid)[3] = "ACCOUNTFAIL";
								break;
							} else {
								proxyfail = 0;
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
							FileOps.SaveFile("weibo.txt", out);
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
		new WeiboID().start();
	}
}
