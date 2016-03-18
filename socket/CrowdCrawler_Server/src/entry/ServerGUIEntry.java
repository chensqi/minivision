package entry;

import java.awt.Dimension;
import java.awt.EventQueue;

import javax.swing.JFrame;

import javax.swing.JTable;

import java.awt.BorderLayout;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JButton;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.ListSelectionModel;
import javax.swing.border.LineBorder;

import java.awt.Color;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.JScrollPane;

import basic.tools.Speeder;
import controller.ServerCore;
import controller.Scheduler;
import core.listener.ClientListener;
import core.listener.ProxyListener;
import core.listener.SchedulerListener;
import core.listener.TaskListener;
import core.listener.SystemListener;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

import javax.swing.JTextField;

import task.server.TaskServer;

import java.awt.Component;
import java.awt.GridLayout;

public class ServerGUIEntry {

	private JFrame frmClientList;
	private JPanel taskPanel;
	private JLabel clientLabel;
	private JTable clientTable;
	private JTable systemTable;
	private JScrollPane scrollPane;

	private ServerCore core;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					ServerGUIEntry window = new ServerGUIEntry();
					window.frmClientList.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the application.
	 */
	public ServerGUIEntry() {
		initialize();
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		frmClientList = new JFrame();
		frmClientList.setTitle("CrowdCrawler");
		frmClientList.setBounds(100, 100, 813, 658);
		frmClientList.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frmClientList.getContentPane().setLayout(new GridLayout(0, 2, 0, 0));

		JPanel systemPanel = new JPanel();
		frmClientList.getContentPane().add(systemPanel);
		systemPanel.setBorder(new LineBorder(Color.GRAY, 1, true));
		systemPanel.setLayout(new BorderLayout(0, 0));

		lblNewLabel_1 = new JLabel("System Summary");
		lblNewLabel_1.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createLineBorder(Color.GRAY),
				BorderFactory.createEmptyBorder(4, 30, 4, 30)));
		lblNewLabel_1.setHorizontalAlignment(SwingConstants.CENTER);
		systemPanel.add(lblNewLabel_1, BorderLayout.NORTH);

		scrollPane_2 = new JScrollPane();
		// scrollPane_2.setPreferredSize(new Dimension(200, 200));
		systemPanel.add(scrollPane_2, BorderLayout.CENTER);

		systemTable = new JTable();
		systemTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		scrollPane_2.setViewportView(systemTable);

		taskPanel = new JPanel();
		frmClientList.getContentPane().add(taskPanel);
		taskPanel.setLayout(new BorderLayout(0, 0));

		taskActionPanel = new JPanel();
		taskPanel.add(taskActionPanel, BorderLayout.SOUTH);

		btnRemoveTask = new JButton("Remove Task");
		taskActionPanel.add(btnRemoveTask);

		txtTaskName = new JTextField();
		taskActionPanel.add(txtTaskName);
		txtTaskName.setColumns(10);

		btnAddNewTask = new JButton("Add New Task");
		btnAddNewTask.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				String taskName = txtTaskName.getText();
				Scheduler.addTask(taskName);
			}
		});
		taskActionPanel.add(btnAddNewTask);

		scrollPane_1 = new JScrollPane();
		taskPanel.add(scrollPane_1, BorderLayout.CENTER);

		taskTable = new JTable();
		scrollPane_1.setViewportView(taskTable);

		taskLabel = new JLabel("Task List");
		taskPanel.add(taskLabel, BorderLayout.NORTH);
		taskLabel.setHorizontalAlignment(SwingConstants.CENTER);
		taskLabel.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createLineBorder(Color.GRAY),
				BorderFactory.createEmptyBorder(4, 30, 4, 30)));

		clientPanel = new JPanel();
		clientPanel.setAlignmentY(Component.TOP_ALIGNMENT);
		clientPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		frmClientList.getContentPane().add(clientPanel);
		clientPanel.setLayout(new BorderLayout(0, 0));

		clientLabel = new JLabel("Client List");
		clientPanel.add(clientLabel, BorderLayout.NORTH);
		clientLabel.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createLineBorder(Color.GRAY),
				BorderFactory.createEmptyBorder(4, 30, 4, 30)));

		clientLabel.setHorizontalAlignment(SwingConstants.CENTER);

		scrollPane = new JScrollPane();
		clientPanel.add(scrollPane, BorderLayout.CENTER);

		clientTable = new JTable();
		clientTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		scrollPane.setPreferredSize(new Dimension(200, 200));
		scrollPane.setViewportView(clientTable);

		proxyPanel = new JPanel();
		proxyPanel.setAlignmentY(0.0f);
		proxyPanel.setAlignmentX(0.0f);
		frmClientList.getContentPane().add(proxyPanel);
		proxyPanel.setLayout(new BorderLayout(0, 0));

		lblProxyList = new JLabel("Proxy List");
		lblProxyList.setHorizontalAlignment(SwingConstants.CENTER);
		lblProxyList.setBorder(BorderFactory.createCompoundBorder(

		BorderFactory.createLineBorder(Color.GRAY),

		BorderFactory.createEmptyBorder(4, 30, 4, 30)));
		proxyPanel.add(lblProxyList, BorderLayout.NORTH);

		scrollPane_3 = new JScrollPane();
		scrollPane_3.setPreferredSize(new Dimension(200, 200));
		proxyPanel.add(scrollPane_3, BorderLayout.CENTER);

		proxyTable = new JTable();
		proxyTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		// proxyPanel.add(proxyTable, BorderLayout.SOUTH);
		scrollPane_3.setViewportView(proxyTable);

		core = new ServerCore();
		// for (;core.isAlive(););
		initializeGUI();
		core.start();
	}

	private JPanel taskActionPanel;
	private JButton btnAddNewTask;
	private JButton btnRemoveTask;
	private JScrollPane scrollPane_1;
	private JLabel taskLabel;
	private JTextField txtTaskName;
	private JTable taskTable;

	private DefaultTableModel clientTableModel;
	private DefaultTableModel proxyTableModel;
	private DefaultTableModel systemTableModel;

	private SystemListener systemListener = new SystemListener() {

		@Override
		public void setTaskSize(int ntask) {
			systemTableModel.setValueAt(ntask, 0, 2);
		}

		@Override
		public void setProxySize(int nproxy) {
			systemTableModel.setValueAt(nproxy, 0, 1);
		}

		@Override
		public void setClientSize(int nclient) {
			systemTableModel.setValueAt(nclient, 0, 0);
		}
	};

	private ClientListener clientListener = new ClientListener() {
		private HashMap<String, Integer> rid = new HashMap<String, Integer>();
		private HashMap<String, Integer> cnt = new HashMap<String, Integer>();
		private HashMap<String, Speeder> speeder = new HashMap<String, Speeder>();

		private Thread speedview = null;

		@Override
		public synchronized void ClientDisconnected(String clientId) {
			try {
				clientTableModel.removeRow(rid.get(clientId));
				rid.clear();
				for (int i = 0; i < clientTableModel.getRowCount(); i++)
					rid.put((String) clientTableModel.getValueAt(i, 0), i);
				systemListener.setClientSize(clientTableModel.getRowCount());
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}

		@Override
		public synchronized void ClientConnected(String clientId) {
			try {
				if (rid.containsKey(clientId))
					return;
				clientTableModel.addRow(new Object[] { clientId, "", 0, 0 });
				rid.put(clientId, clientTableModel.getRowCount() - 1);
				cnt.put(clientId, 0);
				speeder.put(clientId, new Speeder(60000));
			} catch (Exception ex) {
				ex.printStackTrace();
			}
			systemListener.setClientSize(clientTableModel.getRowCount());
		}

		@Override
		public void setStatus(String clientId, String status) {
			try {
				SimpleDateFormat bartDateFormat = new SimpleDateFormat(
						"HH:mm:ss");
				Date date = new Date();
				clientTableModel.setValueAt(
						status + "  " + bartDateFormat.format(date),
						rid.get(clientId), 1);
			} catch (Exception e) {
			}
		}

		@Override
		public void clientSubmit(String clientId) {
			try {
				cnt.put(clientId, cnt.get(clientId) + 1);
				speeder.get(clientId).trigger();
				if (speedview == null) {
					speedview = new Thread() {
						public void run() {
							for (;;) {
								try {
									sleep(1000);
									for (String clientId : rid.keySet())
										clientTableModel.setValueAt(speeder
												.get(clientId).getSpeed() * 60,
												rid.get(clientId), 3);
								} catch (Exception e) {
								}
							}
						};
					};
					speedview.start();
				}
				if (cnt.get(clientId) % 100 == 0)
					clientTableModel.setValueAt(cnt.get(clientId),
							rid.get(clientId), 2);
			} catch (Exception e) {
				// e.printStackTrace();
			}
		}
	};

	private DefaultTableModel taskTableModel;

	private SchedulerListener schedulerListener = new SchedulerListener() {

		private HashMap<String, TaskListener> taskListeners = new HashMap<String, TaskListener>();

		@Override
		public synchronized void RemoveTask(String taskName) {

		}

		@Override
		public synchronized void AddTask(String taskName, TaskServer task) {
			for (int i = 0; i < taskTableModel.getRowCount(); i++)
				if (taskTableModel.getValueAt(i, 0).equals(taskName))
					return;
			taskTableModel.addRow(new Object[] { taskName, task.getProgress(),
					task.getStatus(), task.getLog(), task.getSpeed() });

			systemListener.setTaskSize(taskTableModel.getRowCount());

			taskListeners.put(taskName, new TaskListener() {
				public int rowid = taskTableModel.getRowCount() - 1;

				@Override
				public void onStatusChange(String status) {
					taskTableModel.setValueAt(status, rowid, 2);
				}

				@Override
				public void onProgressChange(double progress) {
					taskTableModel.setValueAt(progress, rowid, 1);
				}

				@Override
				public void onLog(String log) {
					taskTableModel.setValueAt(log, rowid, 3);
				}

				@Override
				public void updateSpeed(double speed) {
					taskTableModel.setValueAt(speed, rowid, 4);
				}
			});
			task.bindListener(taskListeners.get(taskName));
		}
	};
	private ProxyListener proxyListener = new ProxyListener() {
		private HashMap<String, Integer> rowid = new HashMap<String, Integer>();

		@Override
		public synchronized void updateReport(String proxy, int nreport) {
			try {
				proxyTableModel.setValueAt(nreport, rowid.get(proxy), 1);
			} catch (Exception e) {
			}
		}

		@Override
		public synchronized void updateAssign(String proxy, int nassign) {
			try {
				proxyTableModel.setValueAt(nassign, rowid.get(proxy), 2);
			} catch (Exception e) {
			}
		}

		@Override
		public synchronized void removeProxy(String proxy) {
			try {
				proxyTableModel.removeRow(rowid.get(proxy));
				rowid.clear();
				for (int i = 0; i < proxyTableModel.getRowCount(); i++)
					rowid.put((String) proxyTableModel.getValueAt(i, 0), i);
			} catch (Exception e) {
			}
			systemListener.setProxySize(proxyTableModel.getRowCount());
		}

		@Override
		public synchronized void addProxy(String proxy) {
			if (rowid.containsKey(proxy))
				return;
			rowid.put(proxy, proxyTableModel.getRowCount());
			proxyTableModel.addRow(new String[] { proxy, "0", "0" });
			systemListener.setProxySize(proxyTableModel.getRowCount());
		}
	};
	private JPanel clientPanel;
	private JLabel lblNewLabel_1;
	private JScrollPane scrollPane_2;
	private JPanel proxyPanel;
	private JLabel lblProxyList;
	private JTable proxyTable;
	private JScrollPane scrollPane_3;

	private void initializeGUI() {
		initializeClientTable();
		initializeTaskTable();
		initializeProxyTable();
		initializeSystemTable();

		Scheduler.bindListener(schedulerListener);
		core.bindClientListener(clientListener);
		core.getProxyBank().bindListener(proxyListener);
	}

	private void initializeClientTable() {
		clientTable.setModel(clientTableModel = new DefaultTableModel(
				new Object[][] {}, new String[] { "ClientId", "Status",
						"# Submit", "Avg Speed" }));

		DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
		centerRenderer.setHorizontalAlignment(JLabel.CENTER);
		for (int i = 0; i < clientTableModel.getColumnCount(); i++)
			clientTable.getColumnModel().getColumn(i)
					.setCellRenderer(centerRenderer);
		clientTable.getColumnModel().getColumn(0).setPreferredWidth(200);
		clientTable.getColumnModel().getColumn(1).setPreferredWidth(300);
		clientTable.getColumnModel().getColumn(2).setPreferredWidth(80);
		clientTable.getColumnModel().getColumn(3).setPreferredWidth(80);
	}

	private void initializeProxyTable() {
		proxyTable.setModel(proxyTableModel = new DefaultTableModel(
				new Object[][] {}, new String[] { "Proxy", "# Reports",
						"# Assignment" }));

		DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
		centerRenderer.setHorizontalAlignment(JLabel.CENTER);
		for (int i = 0; i < proxyTableModel.getColumnCount(); i++)
			proxyTable.getColumnModel().getColumn(i)
					.setCellRenderer(centerRenderer);
	}

	private void initializeSystemTable() {
		systemTable.setModel(systemTableModel = new DefaultTableModel(
				new Object[][] { { 0, 0, 0 }, }, new String[] { "# Clients",
						"# Proxies", "# Tasks" }));

		DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
		centerRenderer.setHorizontalAlignment(JLabel.CENTER);
		for (int i = 0; i < systemTableModel.getColumnCount(); i++)
			systemTable.getColumnModel().getColumn(i)
					.setCellRenderer(centerRenderer);
	}

	private void initializeTaskTable() {
		taskTable.setModel(taskTableModel = new DefaultTableModel(
				new Object[][] {}, new String[] { "TaskName", "Progress",
						"Status", "Log", "Speed" }));

		DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
		centerRenderer.setHorizontalAlignment(JLabel.CENTER);
		for (int i = 0; i < taskTableModel.getColumnCount(); i++)
			taskTable.getColumnModel().getColumn(i)
					.setCellRenderer(centerRenderer);
	}
}
