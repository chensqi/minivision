package task.local;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.omg.CORBA.PRIVATE_MEMBER;

import basic.FileOps;
import basic.format.Pair;
import network.Client;
import network.ProxyBank;

public class WeiboFriend extends Thread {

	Connection wei_conn = null;
	Connection dou_conn = null;
	Random random = new Random();
	LinkedList<Pair<Long, Integer>> jobs = new LinkedList<Pair<Long, Integer>>();
	ArrayList<String[]> workers = new ArrayList<String[]>();
	ProxyBank proxyBank;

	private String fetch_friend(int workerid, long user_id, int page,
			String proxy, String cookie) {
		for (;;) {
			try {
				sleep(10000 + random.nextInt(2000));
			} catch (Exception e) {
			}
			Client client = new Client(proxy);
			client.setCookie(cookie);
			String url = "http://weibo.cn/attgroup/change?rl=0&cat=user&uid="
					+ user_id + "&page=" + page;
			String content = client.getContent(url);
			if (!content.contains("所有关注人") && !content.contains("用户状态异常") &&! content.contains("User does not exists")) {
//				System.out.println("Worker #" + workerid + " " + url);
//				System.out.println("Worker #" + workerid + " :　" + content);
				synchronized (jobs) {
					jobs.addFirst(new Pair<Long, Integer>(user_id, page));
				}
				if (content.equals("ERROR"))
					return "Proxy Fail";
				if (content.contains("记住登录状态"))
					return "Need Login";
				if (content.contains("帐号存在异常"))
					return "Account Fail";
				return "";
			}

			try {
				Matcher matcher = Pattern.compile("<tr>(.*?)</tr>").matcher(
						content);
				LinkedList<String> follows = new LinkedList<String>();
				for (; matcher.find();) {
					String t = matcher.group(1);
					if (t.contains("推荐给粉丝"))
						continue;
					follows.add(t);
				}
				System.out.println("Worker #" + workerid + " : " + url + "\t"
						+ follows.size());
				synchronized (wei_conn) {
					if (follows.size() > 0) {
						PreparedStatement stat = wei_conn
								.prepareStatement("insert ignore into follow (source,target) values (?,?)");
						PreparedStatement u_stat = wei_conn
								.prepareStatement("insert ignore into user (id,alias,displayname,avatar,gender,location,weibo,follower,followee,finished) values (?,?,?,?,'','','',?,'',0)");
						for (String cur : follows) {
							try {
								matcher = Pattern
										.compile(
												"<td.*?><a href=\"(.*?)\"><img src=\"(.*?)\".*?></a></td>"
														+ "<td.*?><a href=.*?>(.*?)</a>.*?粉丝(\\d*)人.*?<br/><a href=\".*?/attention/add\\?uid=(\\d*)")
										.matcher(cur);

								if (!matcher.find()) {
									matcher = Pattern
											.compile(
													"<td.*?><a href=\"(.*?)\"><img src=\"(.*?)\".*?></a></td>"
															+ "<td.*?><a href=.*?>(.*?)</a>.*?粉丝(\\d*)人.*?<br/>")
											.matcher(cur);

									if (!matcher.find())
										System.err.println(cur);
								}
								String t_alias[] = matcher.group(1).split("/");
								String alias = t_alias[t_alias.length - 1];
								String avatar = matcher.group(2);
								String displayname = matcher.group(3);
								int follower = Integer
										.valueOf(matcher.group(4));

								Long id = Long.valueOf(0);
								try {
									Long.valueOf(matcher.group(5));
								} catch (Exception ex) {
								}
								if (id == 0) {
									matcher = Pattern.compile(
											"http://.*?/(\\d*)")
											.matcher(avatar);
									if (matcher.find())
										id = Long.valueOf(matcher.group(1));
								}
								// System.out.println(id + " : " + alias + "\t"
								// + avatar + "\t" + displayname + "\t"
								// + follower);
								if (id == 0)
									continue;
								stat.setLong(1, user_id);
								stat.setLong(2, id);
								stat.addBatch();
								u_stat.setLong(1, id);
								u_stat.setString(2, alias);
								u_stat.setString(3, displayname);
								u_stat.setString(4, avatar);
								u_stat.setInt(5, follower);
								u_stat.addBatch();
							} catch (Exception ex) {
								ex.printStackTrace();
							}
						}
						stat.executeBatch();
						u_stat.executeBatch();
						if (page == 1)
							wei_conn.createStatement().executeUpdate(
									"insert into follow_tag (userid,page,finished) values ("
											+ user_id + ",1,0)");
						else
							wei_conn.createStatement().executeUpdate(
									"update follow_tag set page=" + (page)
											+ " where userid=" + user_id);
						page++;
					} else {
						if (page == 1)
							wei_conn.createStatement().executeUpdate(
									"insert into follow_tag (userid,page,finished) values ("
											+ user_id + ",0,1)");
						else
							wei_conn.createStatement().executeUpdate(
									"update follow_tag set page=" + (page - 1)
											+ ",finished=1 where userid="
											+ user_id);
						break;
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return "Good";
	}

	@Override
	public void run() {
		proxyBank = new ProxyBank("weiboproxy_f.txt");
		proxyBank.loadProxies();
		proxyBank.proxyLoader();
		proxyBank.initWeight();

		try {
			Class.forName("com.mysql.jdbc.Driver");
			wei_conn = DriverManager
					.getConnection(
							"jdbc:mysql://172.16.2.33:3306/weibo?characterEncoding=utf8&rewriteBatchedStatements=true",
							"root", "apex5879");
			dou_conn = DriverManager
					.getConnection(
							"jdbc:mysql://172.16.2.33:3306/douban?characterEncoding=utf8&rewriteBatchedStatements=true",
							"root", "apex5879");

			Thread fetcher = new Thread() {
				HashSet<Long> fetched = new HashSet<Long>();
				public boolean init = true;

				public void run() {
					for (;;) {
						try {
							if (!init)
								sleep(100000);
							else {
								ResultSet rs = wei_conn.createStatement()
										.executeQuery(
												"select * from follow_tag");
								synchronized (jobs) {
									for (; rs.next();) {
										long id = rs.getLong("userid");
										int page = rs.getInt("page");
										int finished = rs.getInt("finished");
										if (fetched.contains(id))
											continue;
										fetched.add(id);
										if (finished == 1)
											continue;
										jobs.add(new Pair<Long, Integer>(id,
												page + 1));
									}
								}
								System.out.println("Follow_Tag Fetched : "+fetched.size());
							}
							ResultSet rs = dou_conn.createStatement().executeQuery(
									"select weiboid from intro_weiboid where weiboid>0");
							synchronized (jobs) {
								for (; rs.next();) {
									long id = rs.getLong("weiboid");
									if (!fetched.contains(id)) {
										jobs.add(new Pair<Long, Integer>(id, 1));
										fetched.add(id);
									}
								}
							}

							System.out.println("Fetched = " + fetched.size()
									+ " , Jobs = " + jobs.size());

							if (init) {
								startworkers();
							}
							init = false;
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				}
				
				private void startworkers() {
					for (final String s : FileOps.LoadFilebyLine("weibo_f.txt")) {
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
									if (workers.get(wid)[3]
											.equals("ACCOUNTFAIL"))
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
											if (proxy
													.equals("No_Available_Proxy")) {
												proxy = "";
												break;
											}
											System.out.println("Trying "
													+ proxy);
											long time = new Date().getTime();
											String content = new Client(proxy,
													"Mozilla/5.0 (Windows; U; Windows NT 6.1; en-us) AppleWebKit/534.50 (KHTML, like Gecko) Version/5.1 Safari/534.50", "")
													.getContent("http://weibo.cn/");
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
										String cookie = WeiboBasic.login(
												workers.get(wid)[0],
												workers.get(wid)[1]);
										if (cookie.length() == 0) {
											System.err.println("Worker #" + wid
													+ " : Account Fail");
											workers.get(wid)[3] = "ACCOUNTFAIL";
											break;
										}
										workers.get(wid)[3] = cookie;
										System.err.println("Worker #" + wid
												+ " : " + cookie);
									}
									Pair<Long, Integer> cur = null;
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
									String res = fetch_friend(wid,
											cur.getFirst(), cur.getSecond(),
											workers.get(wid)[2],
											workers.get(wid)[3]);
									if (res.equals("Proxy Fail")) {
										proxyfail++;
										if (proxyfail > 10)
											workers.get(wid)[2] = "";
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
						try {
							Thread.sleep(200);
						} catch (Exception ex) {
						}
					}
					new Thread() {
						public void run() {
							for (;;) {
								try {
									sleep(10000);
									LinkedList<String> out = new LinkedList<String>();
									for (String[] t : workers)
										out.add(t[0] + "\t" + t[1] + "\t"
												+ t[2] + "\t" + t[3]);
									FileOps.SaveFile("weibo_f.txt", out);
								} catch (Exception e) {
								}
							}
						};
					}.start();
				}
			};

			fetcher.start();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		try {
			sleep(60000);
		} catch (Exception e) {
		}
	}

	public static void main(String[] args) {
		new WeiboFriend().start();
	}
}
