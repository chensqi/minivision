package task.server;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.LinkedList;

import basic.format.Pair;
import controller.ResultData;
import core.listener.TaskListener;

public class Weibo extends TaskServer {

	private LinkedList<String> jobQ = new LinkedList<String>();
	private LinkedList<String> fetched = new LinkedList<String>();
	private LinkedList<Pair<Long, String>> assignedQ = new LinkedList<Pair<Long, String>>();
	private LinkedList<ResultData> dataQ = new LinkedList<ResultData>();
	private HashSet<String> finished = new HashSet<String>();

	@Override
	public void initialize() {
		jobQ.clear();
		assignedQ.clear();
		dataQ.clear();
		fetched.clear();

		new Thread() {
			public void run() {
				for (;;) {
					Connection conn = null;
					try {
						Class.forName("com.mysql.jdbc.Driver");
						conn = DriverManager
								.getConnection(
										"jdbc:mysql://172.16.2.33:3306/douban?characterEncoding=utf8&rewriteBatchedStatements=true",
										"root", "apex5879");
						ResultSet rs = conn.createStatement().executeQuery(
								"select * from intro_weiboid where weiboid=''");
						for (; rs.next();) {
							String did = rs.getString("id");
							if (fetched.contains(did))
								continue;
							fetched.add(did);
							synchronized (jobQ) {
								jobQ.add(rs.getString("id") + "\t"
										+ rs.getString("raw") + "\t"
										+ rs.getString("url"));
							}
						}
						conn.close();
					} catch (Exception ex) {
						ex.printStackTrace();
					}
					try {
						sleep(60000);
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
						LinkedList<String> failed = new LinkedList<String>();
						synchronized (assignedQ) {
							Long thres = Calendar.getInstance()
									.getTimeInMillis() - 600000;
							for (; !assignedQ.isEmpty()
									&& assignedQ.getFirst().getFirst() < thres;)
								failed.add(assignedQ.removeFirst().getSecond());
						}
						for (String job : failed) {
							taskFail(job);
						}
					} catch (Exception e) {
					}
				}
			};
		}.start();

		for (int i = 0; i < 10; i++)
			new Thread() {
				Connection wei_conn = null;
				Connection dou_conn = null;

				public void run() {
					for (;;) {
						try {
							sleep(10000);
						} catch (Exception e) {
						}
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
						} catch (Exception ex) {
							ex.printStackTrace();
						}
						if (wei_conn == null || dou_conn == null)
							continue;

						for (int i = 0; i < 1000; i++) {
							try {
								ResultData cur = null;
								synchronized (dataQ) {
									if (dataQ.size() > 0)
										cur = dataQ.removeFirst();
								}
								if (cur != null) {
									PreparedStatement stat = wei_conn
											.prepareStatement("insert ignore into user (id,alias,displayname,avatar,gender,location,weibo,follower,followee) values (?,?,?,?,?,?,?,?,?)");
									ArrayList<String> data = (ArrayList<String>) cur.data;
									String[] jobsep = cur.job.split("\t");
									if (data.size() == 0) {
										dou_conn.createStatement()
												.executeUpdate(
														"update intro_weiboid set weiboid=-1 where id="
																+ jobsep[0]);
									} else {
										stat.setLong(1,
												Long.valueOf(data.get(0)));
										stat.setString(2, data.get(1));
										stat.setString(3, data.get(2));
										stat.setString(4, data.get(3));
										stat.setString(5, data.get(4));
										stat.setString(6, data.get(5));
										stat.setInt(7,
												Integer.valueOf(data.get(6)));
										stat.setInt(8,
												Integer.valueOf(data.get(7)));
										stat.setInt(9,
												Integer.valueOf(data.get(8)));
										stat.executeUpdate();
										dou_conn.createStatement()
												.executeUpdate(
														"update intro_weiboid set weiboid="
																+ data.get(0)
																+ " where id="
																+ jobsep[0]);
									}
									finished.add(cur.job);
								}
							} catch (Exception e) {
								e.printStackTrace();
								break;
							}
						}

						try {
							wei_conn.close();
						} catch (Exception ex) {
						}
						try {
							dou_conn.close();
						} catch (Exception ex) {
						}
						wei_conn = null;
						dou_conn = null;
					}
				}
			}.start();

		new Thread() {
			public void run() {
				for (;;) {
					try {
						sleep(1000);

						for (TaskListener listener : listeners)
							listener.onStatusChange("JQ:" + jobQ.size()
									+ " DQ:" + getDQsize() + " AQ:"
									+ assignedQ.size() + " AJQ:"
									+ (jobQ.size() + assignedQ.size()));
					} catch (Exception e) {
					}
				}
			};
		}.start();

		super.initialize();
	}

	private void assign(String s) {
		synchronized (assignedQ) {
			assignedQ.add(new Pair<Long, String>(Calendar.getInstance()
					.getTimeInMillis(), s));
		}
	}

	@Override
	public String getJob(int cnt) {
		String tres = "";
		for (int i = 0; i < cnt; i++) {
			synchronized (jobQ) {
				if (jobQ.isEmpty())
					break;
				String id = jobQ.removeFirst();
				assign(id);
				if (tres.length() > 0)
					tres += ";";
				tres += getName() + ":" + id;
			}
		}
		return tres;
	}

	@Override
	public void submitResult(ResultData data) {
		if (data.data == null) {
			return;
		}
		synchronized (dataQ) {
			dataQ.add(data);
			speeder.trigger();
		}
	}

	@Override
	public void taskFail(String job) {
		synchronized (finished) {
			if (finished.contains(job))
				return;
		}

		synchronized (jobQ) {
			jobQ.add(job);
		}
	}

	@Override
	public String communicate(String msg) {
		if (msg.equals("Cookie"))
			return "_T_WM=31763addd18a502d106dffc9f841fea8; SUB=_2A257VFQfDeTxGeNJ61QY8ibPzjmIHXVYt3xXrDV6PUJbrdANLWPMkW1WwQWj6B0wFKDxXyVilpN7a2emLw..; gsid_CTandWM=4uzt5ab81V4bIjgDdjyj9nWDe5J";
		return null;
	}

	@Override
	public int getDQsize() {
		return dataQ.size();
	}
}
