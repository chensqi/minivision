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
import java.util.Random;

import basic.FileOps;
import basic.format.Pair;
import controller.ResultData;
import core.listener.TaskListener;

public class DoubanAbout extends TaskServer {

	private int lastId = 90000000;
	private LinkedList<Integer> jobQ = new LinkedList<Integer>();
	private LinkedList<Pair<Long, Integer>> assignedQ = new LinkedList<Pair<Long, Integer>>();
	private LinkedList<ResultData> dataQ = new LinkedList<ResultData>();
	private HashSet<Integer> finished = new HashSet<Integer>();
	private HashMap<String, Double> agents = new HashMap<String, Double>();
	private HashSet<Integer> fetched = new HashSet<Integer>();
	private HashSet<Integer> update = new HashSet<Integer>();

	@Override
	public void initialize() {
		jobQ.clear();
		assignedQ.clear();
		dataQ.clear();

		for (String s : FileOps.LoadFilebyLine("agents.txt")) {
			s = s + "\t1.0";
			agents.put(s.split("\t")[0].replace(";", "@"),
					Double.valueOf(s.split("\t")[1]));
		}

		new Thread() {
			public void run() {
				for (;;) {
					try {
						sleep(10000);
						LinkedList<String> out = new LinkedList<String>();
						for (String s : agents.keySet())
							out.add(s + "\t" + agents.get(s));
						FileOps.SaveFile("agents.txt", out);
					} catch (Exception e) {
						// TODO: handle exception
					}
				}
			};

		}.start();

		new Thread() {
			public void run() {
				for (;;) {
					Connection conn = null;
					try {
						sleep(1000);
					} catch (Exception e) {
					}
					try {
						Class.forName("com.mysql.jdbc.Driver");
						conn = DriverManager
								.getConnection(
										"jdbc:mysql://172.16.2.33:3306/douban?characterEncoding=utf8&rewriteBatchedStatements=true",
										"root", "apex5879");
					} catch (Exception ex) {
						ex.printStackTrace();
					}
					if (conn == null)
						continue;

					for (;;) {
						try {
							sleep(1000);
							int PoolSize = 10000;
							if (jobQ.size() < PoolSize) {
								HashSet<Integer> cur = new HashSet<Integer>();
								Statement stat = conn.createStatement();
								HashSet<Integer> req = new HashSet<Integer>();
								ResultSet rs = stat
										.executeQuery("select id from user where finished=0");
								for (; rs.next();) {
									int id = rs.getInt("id");
									if (!fetched.contains(id)) {
										req.add(id);
										update.add(id);
									}
								}

								if (req.size() + jobQ.size() < PoolSize) {
									rs = stat
											.executeQuery("select id from user where id>="
													+ lastId
													+ " and id<"
													+ (lastId + PoolSize));
									for (; rs.next();) {
										int id = rs.getInt("id");
										if (!fetched.contains(id))
											cur.add(id);
									}
									for (int i = 0; i < PoolSize; i++)
										if (!cur.contains(lastId + i))
											req.add(lastId + i);
									lastId += PoolSize;
									log("lastId : " + lastId);
								}
								rs.close();
								synchronized (jobQ) {
									jobQ.addAll(req);
								}
								fetched.addAll(req);
							}
						} catch (Exception e) {
							e.printStackTrace();
							break;
						}
					}

					try {
						conn.close();
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
						LinkedList<Integer> failed = new LinkedList<Integer>();
						synchronized (assignedQ) {
							Long thres = Calendar.getInstance()
									.getTimeInMillis() - 600000;
							for (; !assignedQ.isEmpty()
									&& assignedQ.getFirst().getFirst() < thres;)
								failed.add(assignedQ.removeFirst().getSecond());
						}
						for (int job : failed) {
							taskFail("" + job);
						}
					} catch (Exception e) {
					}
				}
			};
		}.start();

		for (int i = 0; i < 10; i++)
			new Thread() {
				Connection conn = null;

				private void insert(LinkedList<ResultData> datas)
						throws Exception {
					PreparedStatement stat = null;
					stat = conn
							.prepareStatement("INSERT IGNORE INTO `user"
									+ "`(`id`, `active`,`displayid`, `nickname`"
									+ ", `avatar`, `location`, `intro`) VALUES (?,?,?,?,?,?,?)");
					PreparedStatement u_stat = conn
							.prepareStatement("update user set `location`=?, `intro`=?,`finished`=1 where id=?");
					for (ResultData data : datas) {
						int id = Integer.valueOf(data.job.split("\t")[0]);
						ArrayList<String> infos = (ArrayList<String>) data.data;
						if (update.contains(id)) {
							u_stat.setString(1, infos.get(3));
							u_stat.setString(2, infos.get(4));
							u_stat.setInt(3, id);
							u_stat.addBatch();
							continue;
						}
						stat.setInt(1, id);

						if (infos.size() == 0) {
							stat.setInt(2, 0);
							stat.setString(3, "");
							stat.setString(4, "");
							stat.setString(5, "");
							stat.setString(6, "");
							stat.setString(7, "");

						} else {
							stat.setInt(2, 1);
							stat.setString(3, infos.get(0));
							stat.setString(4, infos.get(1));
							stat.setString(5, infos.get(2));
							stat.setString(6, infos.get(3));
							stat.setString(7, infos.get(4));
						}
						stat.addBatch();
					}
					stat.executeBatch();
					u_stat.executeBatch();

					HashSet<Integer> fin = new HashSet<Integer>();
					for (ResultData data : datas)
						fin.add(Integer.valueOf(data.job.split("\t")[0]));
					synchronized (finished) {
						finished.addAll(fin);
					}
					try {
						stat.close();
					} catch (Exception e2) {
					}

				}

				public void run() {
					for (;;) {
						try {
							sleep(10000);
						} catch (Exception e) {
						}
						try {
							Class.forName("com.mysql.jdbc.Driver");
							conn = DriverManager
									.getConnection(
											"jdbc:mysql://172.16.2.33:3306/douban?characterEncoding=utf8&rewriteBatchedStatements=true",
											"root", "apex5879");
						} catch (Exception ex) {
							ex.printStackTrace();
						}
						if (conn == null)
							continue;

						for (int i = 0; i < 1000; i++) {
							try {
								LinkedList<ResultData> cur = new LinkedList<ResultData>();
								synchronized (dataQ) {
									for (; cur.size() < 1000
											&& !dataQ.isEmpty();)
										cur.add(dataQ.removeFirst());
								}
								if (cur.size() == 0) {
									sleep(3000);
									continue;
								}
								insert(cur);
							} catch (Exception e) {
								e.printStackTrace();
								break;
							}
						}

						try {
							conn.close();
						} catch (Exception ex) {
						}
						conn = null;
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

	private void assign(int id) {
		synchronized (assignedQ) {
			assignedQ.add(new Pair<Long, Integer>(Calendar.getInstance()
					.getTimeInMillis(), id));
		}
	}

	Random random = new Random();

	private String getAgent() {
		synchronized (agents) {
			double t = 0;
			LinkedList<Pair<String, Double>> list = new LinkedList<Pair<String, Double>>();
			for (String s : agents.keySet()) {
				list.add(new Pair<String, Double>(s, agents.get(s)));
				t += agents.get(s);
			}
			t *= random.nextDouble();
			for (Pair<String, Double> cur : list) {
				t -= cur.getSecond();
				if (t < 1e-8)
					return cur.getFirst();
			}
			return list.getLast().getFirst();
		}
	}

	private void badAgent(String s) {
		agents.put(s, agents.get(s) / 2);
	}

	private void goodAgent(String s) {
		agents.put(s, 1.0);
	}

	@Override
	public String getJob(int cnt) {
		String tres = "";
		for (int i = 0; i < cnt; i++) {
			synchronized (jobQ) {
				if (jobQ.isEmpty())
					break;
				int id = jobQ.removeFirst();
				assign(id);
				if (tres.length() > 0)
					tres += ";";
				tres += getName() + ":" + id + "\t" + getAgent();
			}
		}
		return tres;
	}

	@Override
	public void submitResult(ResultData data) {
		if (data.data == null) {
			badAgent(data.job.split("\t")[1]);
			return;
		}
		synchronized (dataQ) {
			dataQ.add(data);
			goodAgent(data.job.split("\t")[1]);
			speeder.trigger();
		}
	}

	@Override
	public void taskFail(String job) {
		int id = Integer.valueOf(job.split("\t")[0]);
		synchronized (finished) {
			if (finished.contains(id))
				return;
		}

		synchronized (jobQ) {
			jobQ.add(id);
		}
	}

	@Override
	public String communicate(String msg) {
		return null;
	}

	@Override
	public int getDQsize() {
		return dataQ.size();
	}
}
