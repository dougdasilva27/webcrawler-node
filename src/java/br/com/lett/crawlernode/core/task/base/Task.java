package br.com.lett.crawlernode.core.task.base;

import br.com.lett.crawlernode.core.session.Session;

public abstract class Task {
	
	public static final String STATUS_COMPLETED = "completed";
	public static final String STATUS_FAILED = "failed";
	
	protected Session session;

	public final void process() {
		onStart();
		processTask();
		onFinish();
	}
	
	protected abstract void processTask();
	
	protected void onStart() {
		// subclasses will override if they need to
	}
	
	protected void onFinish() {
		// subclasses will override if they need to
	}
}
