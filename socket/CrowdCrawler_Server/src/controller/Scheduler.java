package controller;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.HashSet;

import core.listener.SchedulerListener;
import basic.Config;
import basic.format.Pair;
import task.server.TaskServer;

public class Scheduler {
	private static HashMap<String, TaskServer> tasks = new HashMap<String, TaskServer>();
	private static HashMap<String, Pair<String, String>> assignment = new HashMap<String, Pair<String, String>>();

	private static HashSet<SchedulerListener> listeners = new HashSet<SchedulerListener>();

	public static void start() {
	}

	public static TaskServer selectTask() {
		for (TaskServer task : tasks.values())
			return task;
		return null;
	}

	public static void addTask(String taskName) {
		if (tasks.containsKey(taskName)) {
			System.err.println("Task Already Exists.");
			return;
		}
		try {
			URLClassLoader loader = new URLClassLoader(new URL[] { new URL(
					Config.getString("classpath")) });
			TaskServer task = (TaskServer) loader.loadClass(
					"task.server." + taskName).newInstance();
			task.initialize();
			tasks.put(taskName, task);
			loader.close();
			for (SchedulerListener listener : listeners)
				listener.AddTask(taskName, task);
		} catch (Exception ex) {
		}
		if (!tasks.containsKey(taskName))
			System.err
					.println("Error Loading Task, Please Check TaskName Again.");
	}

	public static void jobFinished(String clientId, ResultData data) {
			String task = data.task;
			if (tasks.containsKey(task)) {
				TaskServer taskins = tasks.get(task);
				taskins.submitResult(data);
			}
		}

	public static void removeTask(String taskName) {
		tasks.remove(taskName);
		for (SchedulerListener listener : listeners)
			listener.RemoveTask(taskName);
	}

	public static void bindListener(SchedulerListener listener) {
		listeners.add(listener);
	}

	public static void unbindListener(SchedulerListener listener) {
		listeners.remove(listener);
	}

	public static void removeClient(String clientId) {
		try {
			if (assignment.containsKey(clientId))
				tasks.get(assignment.get(clientId).getFirst()).taskFail(
						assignment.get(clientId).getSecond());
		} catch (Exception ex) {
		}
	}

	public static TaskServer getTask(String task) {
		try {
			return tasks.get(task);
		} catch (Exception e) {
			return null;
		}
	}

	public static int getDQsize() {
		int s=0;
		for (TaskServer task:tasks.values())
			s+=task.getDQsize();
		return s;
	}
}
