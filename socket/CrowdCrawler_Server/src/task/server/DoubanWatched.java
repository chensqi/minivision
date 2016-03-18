package task.server;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import core.listener.TaskListener;
import basic.format.Pair;

public class DoubanWatched extends TaskServer {

	private Connection conn = null;
	private int laid = 1987643;

	private LinkedList<Pair<Integer, Integer>> Q;

	private LinkedList<String> dataQ;

	@Override
	public void initialize() {
		new Thread() {
			public void run() {
				for (;;) {
					try {
						sleep(1000);
						for (TaskListener listener : listeners)
							listener.onStatusChange("JQ:" + Q.size() + " DQ:"
									+ dataQ.size());
					} catch (Exception e) {
					}
				}
			};
		}.start();
		synchronized (this) {
			updateStatus("Initializing");
			try {
				Class.forName("com.mysql.jdbc.Driver");
				conn = DriverManager
						.getConnection(
								"jdbc:mysql://172.16.7.85:3306/douban?characterEncoding=utf8",
								"crawler", "crawler");
			} catch (Exception ex) {
				ex.printStackTrace();
			}

			for (; nextpage(laid) == -1; laid++)
				updateStatus("Checking Previous Data " + laid);

			Q = new LinkedList<Pair<Integer, Integer>>();

			log("Initialized, Starting from " + laid);

			dataQ = new LinkedList<String>();

			Thread[] dataworkers = new Thread[30];
			for (int i = 0; i < dataworkers.length; i++) {
				dataworkers[i] = new Thread() {
					Connection conn_worker;

					private int getcurpage(int uid) {
						try {
							String cmd = "select * from user_movie_watched_crawled where uid="
									+ uid;
							Statement stat = conn_worker.createStatement();
							ResultSet res = stat.executeQuery(cmd);
							if (!res.next())
								return 0;
							return res.getInt("page");
						} catch (Exception e) {
							return 1;
						}
					}

					private void setnextpage(int uid, int page, boolean fin) {
						try {
							if (!fin) {
								synchronized (Q) {
									Q.add(new Pair<Integer, Integer>(uid,
											page + 1));
								}
							}
							int lap = getcurpage(uid);
							if (page <= lap) {
								System.err.println("Out-Date Job Submitted "
										+ uid + "," + page + "," + lap);
								return;
							}
							Statement stat = conn_worker.createStatement();
							if (lap == 0) {
								String cmd = "INSERT INTO `user_movie_watched_crawled`(`uid`, `page`, `finished`) VALUES ("
										+ uid + "," + page + "," + fin + ")";
								stat.executeUpdate(cmd);
							} else {
								String cmd = "update user_movie_watched_crawled set page="
										+ page
										+ ",finished="
										+ fin
										+ " where uid=" + uid;
								stat.executeUpdate(cmd);
							}

						} catch (Exception e) {
							e.printStackTrace();
						}
					}

					private int insertBatch(int uid, String data) {
						int cnt = 0;
						try {
							PreparedStatement stat = conn_worker
									.prepareStatement("INSERT INTO `user_movie_watched`(`uid`, `mid`, `rate`, `date`, `tag`, `review`) VALUES (?,?,?,?,?,?)");
							for (String s : data.split("@")) {
								String[] sep = s.split("#");
								if (sep.length == 5) {
									stat.setInt(1, uid);
									stat.setInt(2, Integer.valueOf(sep[0]));
									stat.setInt(3, Integer.valueOf(sep[1]));
									stat.setString(4, sep[2]);
									stat.setString(5, sep[3].trim());
									stat.setString(6, sep[4].trim());
									stat.addBatch();
									cnt++;
								}
							}
							stat.executeBatch();
							// conn_worker.commit();
						} catch (Exception e) {
							// e.printStackTrace();
						}
						return cnt;
					}

					@Override
					public void run() {
						try {
							Class.forName("com.mysql.jdbc.Driver");
							conn_worker = DriverManager
									.getConnection(
											"jdbc:mysql://172.16.7.85:3306/douban?characterEncoding=utf8&rewriteBatchedStatements=true",
											"crawler", "crawler");
						} catch (Exception ex) {
							ex.printStackTrace();
						}

						for (;;) {
							String data = null;
							synchronized (dataQ) {
								if (!dataQ.isEmpty())
									data = dataQ.removeFirst();
							}
							if (data == null) {
								try {
									sleep(3000);
								} catch (Exception e) {
								}
								continue;
							}

							int pos = data.indexOf(':');
							String seg0 = data.substring(0, pos);
							String seg1 = data.substring(pos + 1);
							String[] header = seg0.split(",");
							int uid = Integer.valueOf(header[0]);
							int page = Integer.valueOf(header[1]);

							int cnt = insertBatch(uid, seg1);
							setnextpage(uid, page, cnt == 0);
						}
					}
				};
				dataworkers[i].start();
			}
		}
		super.initialize();
	}

	private int nextpage(int uid) {
		try {
			String cmd = "select * from user_movie_watched_crawled where uid="
					+ uid;
			Statement stat = conn.createStatement();
			ResultSet res = stat.executeQuery(cmd);
			if (!res.next())
				return 1;
			if (res.getBoolean("finished"))
				return -1;
			return res.getInt("page") + 1;
		} catch (Exception e) {
			return 1;
		}
	}

	@Override
	public synchronized String getJob(int njob) {
		try {
			Pair<Integer, Integer> nxt = Q.removeFirst();
			return "" + nxt.getFirst() + "," + nxt.getSecond();
		} catch (Exception ex) {
		}

		for (; nextpage(laid) == -1; laid++)
			;
		int page = nextpage(laid);
		// int page = 1;

		log("Assigning " + laid + "," + page);
		String job = laid + "," + page;
		laid++;
		return job;
	}

	@Override
	public void submitResult(String data) {
		int pos = data.indexOf(':');
		String seg0 = data.substring(0, pos);
		String seg1 = data.substring(pos + 1);

		if (seg1.startsWith("ERROR"))
			taskFail(seg0);
		else {
			speeder.trigger();
			synchronized (dataQ) {
				dataQ.add(data);
			}
		}
	}

	@Override
	public void taskFail(String job) {
		String[] sep = job.split(",");
		Q.add(new Pair<Integer, Integer>(Integer.valueOf(sep[0]), Integer
				.valueOf(sep[1])));
	}

	public static String cookieGen(String username, String password) {
		network.Client client = new network.Client("localhost");
		try {
			System.out
					.println(client
							.getContent("http://m.douban.com/movie/people/1445796/watched?page=9"));
			String content = client.getContent("http://m.douban.com/login");
			Matcher matcher = Pattern.compile("/captcha/(.*?)/").matcher(
					content);
			String capId = "";
			if (matcher.find())
				capId = matcher.group(1);
			else {
				System.err.println(content);
				return "";
			}
			String capsol[];
			String imgdir = "http://m.douban.com/captcha/" + capId + "/?size=m";
			if (!client.saveImg(imgdir, "imgcode.jpg"))
				return "";
			capsol = com.cqz.dm.CodeReader.getImgCode("imgcode.jpg", 3008);
			String qqq = "";
			// try {
			// System.out.println("Please Enter Capsol:");
			// qqq = new BufferedReader(new InputStreamReader(System.in))
			// .readLine();
			// } catch (Exception e) {
			// }
			qqq = capsol[1];

			LinkedList<NameValuePair> params = new LinkedList<NameValuePair>();
			params.add(new BasicNameValuePair("form_email", username));
			params.add(new BasicNameValuePair("form_password", password));
			params.add(new BasicNameValuePair("captcha-solution", qqq));
			params.add(new BasicNameValuePair("captcha-id", capId));
			params.add(new BasicNameValuePair("user_login", "登录"));

			client.sendPost("http://m.douban.com/login", params);

			return client.getCookie();

		} catch (Exception ex) {
			ex.printStackTrace();
			return null;
		}
	}

	public static void main(String[] args) {
		// System.out.println(cookieGen("binzai5814@126.com", "vuycpb715744"));
		// String[] capsol = com.cqz.dm.CodeReader.getImgCode("imgcode.jpg",
		// 3008);
		// System.out.println(capsol[0]);
		// System.out.println(capsol[1]);
	}

	private String cookie;

	private Long lastupdate = Long.valueOf(0);

	@Override
	public synchronized String communicate(String substring) {
		long now = Calendar.getInstance().getTimeInMillis();
		if (now > lastupdate + 600000) {
			cookie = cookieGen("binzai5814@126.com", "vuycpb715744");
			lastupdate = now;
		}
		return cookie;
	}

	@Override
	public int getDQsize() {
		// TODO Auto-generated method stub
		return 0;
	}
}
