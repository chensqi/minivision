package task.server;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import com.sun.org.apache.bcel.internal.generic.RETURN;
import com.sun.swing.internal.plaf.synth.resources.synth;

import core.listener.TaskListener;
import basic.format.Pair;

public class DoubanWatchedPC extends TaskServer {

	private Connection conn = null;
	private Pair<Integer, Integer> laid = new Pair<Integer, Integer>(115000000, 0);

	private HashSet<Pair<Integer, Integer>> Q;

	private ArrayList<LinkedList<String>> dataQ;

	private HashMap<Integer, Integer> curpage = new HashMap<Integer, Integer>();

	@Override
	public void initialize() {

		dataQ = new ArrayList<LinkedList<String>>();
		for (int i = 0; i < 100; i++)
			dataQ.add(new LinkedList<String>());
		new Thread() {
			public void run() {
				for (;;) {
					try {
						sleep(1000);

						for (TaskListener listener : listeners)
							listener.onStatusChange("JQ:" + Q.size() + " DQ:"
									+ getDQsize() + " AQ:"
									+ assignedJobs.size());
					} catch (Exception e) {
					}
				}
			};
		}.start();
		new Thread() {
			public void run() {
				for (;;) {
					try {
						sleep(1000);
						if (getDQsize() > 5000)
							continue;
						System.out.println(assignedJobs.size());
						LinkedList<String> failed = new LinkedList<String>();
						synchronized (assignedJobs) {
							Long thres = Calendar.getInstance()
									.getTimeInMillis() - 180000;
							for (; !assignedJobs.isEmpty()
									&& assignedJobs.getFirst().getSecond() < thres;)
								failed.add(assignedJobs.removeFirst()
										.getFirst());
						}
						for (String s : failed) {
							System.out.println("Fail : " + s);
							taskFail(s);
						}
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
								"jdbc:mysql://localhost:3316/doubanpcseg?characterEncoding=utf8",
								"crawler", "crawler");
			} catch (Exception ex) {
				ex.printStackTrace();
			}

			// for (; nextpage(laid) == -1; laid++)
			// updateStatus("Checking Previous Data " + laid);

			Q = new HashSet<Pair<Integer, Integer>>();
			// getJob();

			log("Initialized, Starting from " + laid.toString());

			Thread[] dataworkers = new Thread[100];
			for (int i = 0; i < dataworkers.length; i++) {
				final int id = i;
				dataworkers[i] = new Thread() {
					Connection conn_worker;

					private void setnextpage(int uid, int page, boolean fin) {
						Statement stat = null;
						int sid = uid % 100;
						try {
							if (!fin) {
								synchronized (Q) {
									Q.add(new Pair<Integer, Integer>(uid,
											page + 1));
								}
							}
							int lap = 0;

							synchronized (curpage) {
								if (curpage.containsKey(uid))
									lap = curpage.get(uid);
								if (page <= lap) {
									System.err
											.println("Out-Date Job Submitted "
													+ uid + "," + page + ","
													+ lap);
									return;
								}
								curpage.put(uid, page);
							}
							stat = conn_worker.createStatement();
							if (lap == 0) {
								String cmd = "INSERT INTO `user_movie_watched_crawled_"
										+ sid
										+ "`(`uid`, `page`, `finished`) VALUES ("
										+ uid + "," + page + "," + fin + ")";
								stat.executeUpdate(cmd);
							} else {
								String cmd = "update user_movie_watched_crawled_"
										+ sid
										+ " set page="
										+ page
										+ ",finished="
										+ fin
										+ " where uid="
										+ uid;
								stat.executeUpdate(cmd);
							}

						} catch (Exception e) {
							e.printStackTrace();
						} finally {
							try {
								stat.close();
							} catch (Exception e2) {
							}
						}
					}

					private void insertBatch(
							LinkedList<Pair<Integer, String>> data) {
						int sid = 0;
						PreparedStatement stat = null;
						try {
							for (Pair<Integer, String> cur : data) {
								int uid = cur.getFirst();
								sid = uid % 100;
								stat = conn_worker
										.prepareStatement("INSERT IGNORE INTO `user_movie_watched_"
												+ sid
												+ "`(`uid`, `mid`, `rate`, `date`, `review`) VALUES (?,?,?,?,?)");
								for (String s : cur.getSecond().split("@")) {
									String[] sep = s.split("#");
									if (sep.length == 4) {
										stat.setInt(1, uid);
										stat.setInt(2, Integer.valueOf(sep[0]));
										stat.setInt(3, Integer.valueOf(sep[1]));
										stat.setString(4, sep[2]);
										stat.setString(5, sep[3].trim());
										stat.addBatch();
									}
								}
							}
							stat.executeBatch();
						} catch (Exception e) {
							e.printStackTrace();
						} finally {
							try {
								stat.close();
							} catch (Exception e2) {
							}
						}
					}

					@Override
					public void run() {
						for (;;) {
							try {
								sleep(10000);
							} catch (Exception e) {
							}
							try {
								Class.forName("com.mysql.jdbc.Driver");
								conn_worker = DriverManager
										.getConnection(
												"jdbc:mysql://localhost:3316/doubanpcseg?characterEncoding=utf8&rewriteBatchedStatements=true",
												"crawler", "crawler");
							} catch (Exception ex) {
								ex.printStackTrace();
							}
							if (conn_worker == null)
								continue;

							for (;;) {
								try {
									LinkedList<String> cur = new LinkedList<String>();
									synchronized (dataQ.get(id)) {
										for (; !dataQ.get(id).isEmpty()
												&& cur.size() < 100;)
											cur.add(dataQ.get(id).removeFirst());
									}
									if (cur.size() == 0) {
										try {
											sleep(1000);
										} catch (Exception e) {
										}
										continue;
									}

									LinkedList<Pair<Integer, String>> D = new LinkedList<Pair<Integer, String>>();

									for (String data : cur) {
										int pos = data.indexOf(':');
										String seg0 = data.substring(0, pos);
										String seg1 = data.substring(pos + 1);
										String[] header = seg0.split(",");
										int uid = Integer.valueOf(header[0]);
										D.add(new Pair<Integer, String>(uid,
												seg1));
									}

									insertBatch(D);
									for (String data : cur) {
										int pos = data.indexOf(':');
										String seg0 = data.substring(0, pos);
										String seg1 = data.substring(pos + 1);
										String[] header = seg0.split(",");
										int uid = Integer.valueOf(header[0]);
										int page = Integer.valueOf(header[1]);
										setnextpage(uid, page,
												seg1.length() == 0);
									}
								} catch (Exception ex) {
									ex.printStackTrace();
									break;
								}
							}
							try {
								conn_worker.close();
							} catch (Exception e) {
							}
						}
					}
				};
				dataworkers[i].start();
			}
		}
		Thread jobloader=
		new Thread() {
			@Override
			public void run() {
				for (;;) {
					if (Q.size() > 5000) {
						try {
							sleep(1000);
							continue;
						} catch (Exception e) {
						}
					}
					try {
						log("Fetching Job " + laid.toString());
						int step = 1000;
						Statement stat = conn.createStatement();
						ResultSet res = stat
								.executeQuery("Select * from user_movie_watched_crawled_"
										+ laid.getSecond()
										+ " where uid>"
										+ laid.getFirst()
										+ " and uid<"
										+ (laid.getFirst() + step + 1));
						LinkedList<Pair<Integer, Integer>> jobs = new LinkedList<Pair<Integer, Integer>>();

						int cnt = 0;
						synchronized (curpage) {
							for (; res.next();) {
								cnt++;
								if (res.getBoolean("finished")){
									curpage.put(res.getInt("uid"), -1);
								}
								else
									curpage.put(res.getInt("uid"),
											res.getInt("page"));
							}

							System.out.println("ResultSet : " + cnt);
							for (int i = laid.getFirst() + laid.getSecond(); i < laid
									.getFirst() + step; i += 100) {
								int page = 0;
								if (curpage.containsKey(i))
									page = curpage.get(i);
								if (page == -1)
									continue;
								jobs.add(new Pair<Integer, Integer>(i, page + 1));
							}
						}
						if (laid.getSecond() == 99)
							laid = new Pair<Integer, Integer>(laid.getFirst()
									+ step, 0);
						else
							laid = new Pair<Integer, Integer>(laid.getFirst(),
									laid.getSecond() + 1);
						synchronized (Q) {
							Q.addAll(jobs);
						}
						res.close();
						stat.close();
					} catch (Exception ex) {
						ex.printStackTrace();
					}
				}
			}
		};
		jobloader.setPriority(Thread.MAX_PRIORITY);
		jobloader.start();
		super.initialize();
	}

	private LinkedList<Pair<String, Long>> assignedJobs = new LinkedList<Pair<String, Long>>();

	@Override
	public String getJob(int njob) {
		LinkedList<String> fjobs = new LinkedList<String>();
		for (int r = 0;; r++) {
			try {
				if (r < 1000)
					synchronized (Q) {
						for (; fjobs.size() < njob;) {
							Pair<Integer, Integer> nxt = Q.iterator().next();
							Q.remove(nxt);
							fjobs.add(nxt.getFirst() + "," + nxt.getSecond());
						}
					}
				if (fjobs.size() >= njob || r >= 1000) {
					synchronized (assignedJobs) {
						for (String job : fjobs)
							assignedJobs.add(new Pair<String, Long>(job,
									Calendar.getInstance().getTimeInMillis()));
					}
					String job = "";
					for (String cur : fjobs)
						job += getName() + ":" + cur + ";";
					if (job.length() == 0)
						return "IDLE";
					return job.substring(0, job.length() - 1);
				}
			} catch (Exception ex) {
			}
			// try {
			// log("Fetching Job " + laid.toString());
			// int step = 1000;
			// Statement stat = conn.createStatement();
			// ResultSet res = stat
			// .executeQuery("Select * from user_movie_watched_crawled_"
			// + laid.getSecond()
			// + " where uid>"
			// + laid.getFirst()
			// + " and uid<"
			// + (laid.getFirst() + step + 1));
			//
			// // HashMap<Integer, Integer> curpage = new HashMap<Integer,
			// // Integer>();
			// LinkedList<Pair<Integer, Integer>> jobs = new
			// LinkedList<Pair<Integer, Integer>>();
			//
			// int cnt = 0;
			// synchronized (curpage) {
			// for (; res.next();) {
			// cnt++;
			// if (res.getBoolean("finished"))
			// curpage.put(res.getInt("uid"), -1);
			// else
			// curpage.put(res.getInt("uid"), res.getInt("page"));
			// }
			//
			// System.out.println("ResultSet : " + cnt);
			// for (int i = laid.getFirst() + laid.getSecond(); i < laid
			// .getFirst() + step; i += 100) {
			// int page = 0;
			// if (curpage.containsKey(i))
			// page = curpage.get(i);
			// if (page == -1)
			// continue;
			// jobs.add(new Pair<Integer, Integer>(i, page + 1));
			// }
			// }
			// if (laid.getSecond() == 99)
			// laid = new Pair<Integer, Integer>(laid.getFirst() + step, 0);
			// else
			// laid = new Pair<Integer, Integer>(laid.getFirst(),
			// laid.getSecond() + 1);
			// synchronized (Q) {
			// Q.addAll(jobs);
			// }
			// res.close();
			// stat.close();
			// } catch (Exception ex) {
			// ex.printStackTrace();
			// }

		}
	}

	@Override
	public void submitResult(String data) {
		int pos = data.indexOf(':');
		String seg0 = data.substring(0, pos);
		String seg1 = data.substring(pos + 1);
		String[] header = seg0.split(",");
		int uid = Integer.valueOf(header[0]);
		int sid = uid % 100;

		if (seg1.startsWith("ERROR"))
			taskFail(seg0);
		else {
			speeder.trigger();
			synchronized (dataQ.get(sid)) {
				dataQ.get(sid).add(data);
			}
		}
	}

	@Override
	public void taskFail(String job) {
		String[] sep = job.split(",");
		int uid = Integer.valueOf(sep[0]);
		int page = Integer.valueOf(sep[1]);
		synchronized (curpage) {
			if (curpage.containsKey(uid)) {
				int p = curpage.get(uid);
				if (p >= page || p == -1)
					return;
			}
		}
		synchronized (Q) {
			Q.add(new Pair<Integer, Integer>(uid, page));
		}
	}

	public static String cookieGen(String username, String password) {
		network.Client client = new network.Client("localhost");
		try {
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
		DoubanWatchedPC task = new DoubanWatchedPC();
		task.initialize();
		// task.submitResult("0,4:0#1#2010-01-01# ");
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
		int ndq = 0;
		for (LinkedList<String> td : dataQ)
			ndq += td.size();
		return ndq;
	}

}
