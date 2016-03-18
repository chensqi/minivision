package controller;

import java.io.Serializable;

public class ResultData implements Serializable{
	private static final long serialVersionUID = 1L;
	public String task;
	public 	String job;
	public Object data;
	public ResultData(String task,String job,Object data) {
		this.task=task;
		this.job=job;
		this.data=data;
	}
	public void setData(Object data){
		this.data=data;
	}
}