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

import basic.FileOps;
import basic.format.Pair;
import network.Client;
import network.ProxyBank;

public class WeiboContent extends Thread {

	Connection wei_conn = null;
	Connection dou_conn = null;
	Random random = new Random();
	LinkedList<Pair<Long, Integer>> jobs = new LinkedList<Pair<Long, Integer>>();
	ArrayList<String[]> workers = new ArrayList<String[]>();
	int pagelimit = 100;
	ProxyBank proxyBank;

	private String fetch_weibo(int workerid, long user_id, int page,
			String proxy, String cookie) {
		for (;;) {
			if (page > pagelimit)
				break;
			try {
				sleep(10000 + random.nextInt(2000));
			} catch (Exception e) {
			}
			Client client = new Client(proxy);
			client.setCookie(cookie);
			String content = client.getContent("http://weibo.cn/" + user_id
					+ "?page=" + page);
			// System.out.println("Worker #" + workerid + " http://weibo.cn/"
			// + user_id + "?page=" + page);
			// System.out.println("Worker #" + workerid + " :　" + content);
			if (!content.contains("的微博") && !content.contains("用户状态异常")) {
				System.out.println("Worker #" + workerid + " http://weibo.cn/"
						+ user_id + "?page=" + page);
				System.out.println("Worker #" + workerid + " :　" + content);
				synchronized (content) {
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
				Matcher matcher = Pattern
						.compile(
								"<div class=\"c\" id=\"(.*?)\".*?<div class=\"s\"></div>")
						.matcher(content);
				LinkedList<Pair<String, String>> weibos = new LinkedList<Pair<String, String>>();
				int totpage = 0;
				for (; matcher.find();) {
					weibos.add(new Pair<String, String>(matcher.group(1),
							matcher.group()));
					// System.out.println(matcher.group());
				}
				try {
					matcher = Pattern.compile("/(\\d*)页").matcher(content);
					matcher.find();
					totpage = Integer.valueOf(matcher.group(1));
				} catch (Exception ex) {
				}
				totpage = Math.max(totpage, page);
				System.out.println("Worker #" + workerid + " http://weibo.cn/"
						+ user_id + "?page=" + page + " : " + weibos.size());
				synchronized (wei_conn) {
					if (weibos.size() > 0 || totpage > page) {
						PreparedStatement stat = wei_conn
								.prepareStatement("insert ignore into weibo (userid,weiboid,raw) values (?,?,?)");
						for (Pair<String, String> cur : weibos) {
							stat.setLong(1, Long.valueOf(user_id));
							stat.setString(2, cur.getFirst());
							stat.setString(3, cur.getSecond());
							stat.addBatch();
						}
						stat.executeBatch();
						if (page == 1)
							wei_conn.createStatement().executeUpdate(
									"insert into weibo_tag (userid,page,finished) values ("
											+ user_id + ",1,0)");
						else
							wei_conn.createStatement().executeUpdate(
									"update weibo_tag set page=" + (page)
											+ " where userid=" + user_id);
						page++;
					} else {
						if (page == 1)
							wei_conn.createStatement().executeUpdate(
									"insert into weibo_tag (userid,page,finished) values ("
											+ user_id + ",0,1)");
						else
							wei_conn.createStatement().executeUpdate(
									"update weibo_tag set page=" + (page - 1)
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
		proxyBank = new ProxyBank("weiboproxy.txt");
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

			new Thread(){
				HashSet<Long> fetched = new HashSet<Long>();
				public void run() {
					boolean init=true;
					for (;;){
						try {
							if (!init) sleep(100000);
							if (init){
								ResultSet rs = wei_conn.createStatement().executeQuery(
									"select * from weibo_tag");
								synchronized (jobs) {
									for (; rs.next();) {
										long id = rs.getLong("userid");
										int page = rs.getInt("page");
										int finished = rs.getInt("finished");
										fetched.add(id);
										if (finished == 1)
											continue;
										if (page != -1 && page < pagelimit)
											jobs.add(new Pair<Long, Integer>(id, page + 1));
									}									
								}
							}
							ResultSet rs = dou_conn.createStatement().executeQuery(
									"select weiboid from intro_weiboid where weiboid>0");
							synchronized (jobs) {
								for (; rs.next();) {
									long id = rs.getLong("weiboid");
									if (!fetched.contains(id)){
										jobs.add(new Pair<Long, Integer>(id, 1));
										fetched.add(id);
									}
								}
							}
							System.out.println("Fetched : "+fetched.size()+" , Jobs : "+jobs.size());
							if (init){
								startWorkers();
							}
							init=false;
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
					
				};
				private void startWorkers(){
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
											synchronized (proxyBank) {
												proxy = proxyBank.getProxy(1);
											}
											if (proxy.equals("No_Available_Proxy")) {
												proxy = "";
												break;
											}
											System.out.println("Trying " + proxy);
											long time = new Date().getTime();
											String content = new Client(proxy, "", "")
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
										System.err.println("Worker #" + wid + " : "
												+ cookie);
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
									String res = fetch_weibo(wid, cur.getFirst(),
											cur.getSecond(), workers.get(wid)[2],
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
						} catch (InterruptedException e) {
						}
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
					}
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
		new WeiboContent().start();
	}
}
