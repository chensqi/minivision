package task.server;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import com.sun.org.apache.bcel.internal.generic.Select;
import com.sun.org.apache.xpath.internal.operations.And;
import com.sun.swing.internal.plaf.synth.resources.synth;

import core.listener.TaskListener;
import basic.format.Pair;

public class DoubanMovieRated extends TaskServer {

	private Connection conn = null;

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
								"jdbc:mysql://172.16.8.186:3306/douban?characterEncoding=utf8",
								"douban", "douban");
			} catch (Exception ex) {
				ex.printStackTrace();
			}

			Q = new LinkedList<Pair<Integer, Integer>>();
			getJob(1);

			dataQ = new LinkedList<String>();

			Thread[] dataworkers = new Thread[100];
			for (int i = 0; i < dataworkers.length; i++) {
				dataworkers[i] = new Thread() {
					Connection conn_worker;

					private int getcurpage(int mid) {
						int page = 0;
						try {
							String cmd = "select page from movie_crawltag where mid="
									+ mid;
							Statement stat = conn_worker.createStatement();
							ResultSet res = stat.executeQuery(cmd);
							if (res.next())
								page = res.getInt("page");
							res.close();
							stat.close();
						} catch (Exception e) {
						}
						return page;
					}

					private void setnextpage(int mid, int page, boolean fin) {
						Statement stat = null;
						try {
							if (!fin) {
								synchronized (Q) {
									Q.add(new Pair<Integer, Integer>(mid,
											page + 1));
								}
							}
							int lap = getcurpage(mid);
							if (page <= lap) {
								System.err.println("Out-Date Job Submitted "
										+ mid + "," + page + "," + lap);
								return;
							}
							stat = conn_worker.createStatement();
							if (lap == 0) {
								String cmd = "INSERT INTO `movie_crawltag` (`mid`, `page`, `finished`) VALUES ("
										+ mid + "," + page + "," + fin + ")";
								stat.executeUpdate(cmd);
							} else {
								String cmd = "update user_movie_watched_crawled set page="
										+ page
										+ ",finished="
										+ fin
										+ " where mid=" + mid;
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

					private int insertBatch(int uid, String data) {
						int cnt = 0;
						PreparedStatement stat = null;
						try {
							stat = conn_worker
									.prepareStatement("INSERT INTO `rated` (`movieId`, `userId`, `rate`, `date`) VALUES (?,?,?,?,?)");
							for (String s : data.split("@")) {
								String[] sep = s.split("#");
								if (sep.length == 3) {
									stat.setInt(1, uid);
									stat.setInt(2, Integer.valueOf(sep[0]));
									stat.setInt(3, Integer.valueOf(sep[1]));
									stat.setString(4, sep[2]);
									stat.addBatch();
									cnt++;
								}
							}
							stat.executeBatch();
						} catch (Exception e) {
						} finally {
							try {
								stat.close();
							} catch (Exception e2) {
							}
						}
						return cnt;
					}

					@Override
					public void run() {
						try {
							Class.forName("com.mysql.jdbc.Driver");
							conn_worker = DriverManager
									.getConnection(
											"jdbc:mysql://172.16.8.186/:3306/doubanpc?characterEncoding=utf8&rewriteBatchedStatements=true",
											"douban", "douban");
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

	@Override
	public synchronized String getJob(int njob) {
		synchronized (this) {
			for (;;) {
				try {
					synchronized (Q) {
						Pair<Integer, Integer> nxt = Q.removeFirst();
						return "" + nxt.getFirst() + "," + nxt.getSecond();
					}
				} catch (Exception ex) {
				}

				try {
					LinkedList<Pair<Integer, Integer>> jobs = new LinkedList<Pair<Integer, Integer>>();

					int step = 1000;
					Statement stat = conn.createStatement();
					ResultSet res = stat
							.executeQuery("Select * from movie_crawltag limit "
									+ step);
					for (; res.next();) {
						if (res.getBoolean("finished"))
							continue;
						else
							jobs.add(new Pair<Integer, Integer>(res
									.getInt("mid"), res.getInt("page") + 1));
					}

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
		synchronized (Q) {
			Q.add(new Pair<Integer, Integer>(Integer.valueOf(sep[0]), Integer
					.valueOf(sep[1])));
		}
	}

	@Override
	public int getDQsize() {
		return dataQ.size();
	}

	@Override
	public String communicate(String msg) {
		return null;
	}
}
