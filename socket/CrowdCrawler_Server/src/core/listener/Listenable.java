package core.listener;

import java.util.HashSet;

public class Listenable<T> {
	protected HashSet<T> listeners=new HashSet<T>();
	public void bindListener(T listener){
		listeners.add(listener);
	}
	public void unbindListener(T listener){
		listeners.remove(listener);
	}
}
