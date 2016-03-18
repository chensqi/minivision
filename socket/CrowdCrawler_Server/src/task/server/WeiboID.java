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

public class WeiboID extends TaskServer {

	private LinkedList<String> jobQ = new LinkedList<String>();
	private LinkedList<String> fetched=new LinkedList<String>();
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
						ResultSet rs=conn.createStatement().executeQuery("select ")
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

				private void insert(LinkedList<ResultData> datas) throws Exception{
					PreparedStatement stat = null;
					stat = conn
								.prepareStatement("INSERT IGNORE INTO `user"
										+ "`(`id`, `active`,`displayid`, `nickname`"
										+ ", `avatar`, `location`, `intro`) VALUES (?,?,?,?,?,?,?)");
						for (ResultData data : datas) {
							stat.setInt(1, Integer.valueOf(data.job));
							ArrayList<String> infos = (ArrayList<String>) data.data;

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
					
						HashSet<Integer> fin = new HashSet<Integer>();
						for (ResultData data : datas)
							fin.add(Integer.valueOf(data.job));
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

						for (int i=0;i<1000;i++) {
							try {
								LinkedList<ResultData> cur = new LinkedList<ResultData>();
								synchronized (dataQ) {
									for (; cur.size() < 1000 && !dataQ.isEmpty();)
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
									+ assignedQ.size()+" AJQ:"+(jobQ.size()+assignedQ.size()));
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
				tres += getName() + ":" + id;
			}
		}
		return tres;
	}

	@Override
	public void submitResult(ResultData data) {
		if (data.data == null) {
//			taskFail(data.job);
			return;
		}
		synchronized (dataQ) {
			dataQ.add(data);
			speeder.trigger();
		}
	}

	@Override
	public void taskFail(String job) {
		int id = Integer.valueOf(job);
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
