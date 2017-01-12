package br.com.lett.crawlernode.core.task.base;

public abstract class Task {

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
