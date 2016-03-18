package core.listener;

public interface ProxyListener {
	public void addProxy(String proxy);
	public void removeProxy(String proxy);
	public void updateReport(String proxy,int nreport);
	public void updateAssign(String proxy,int nassign);
}
