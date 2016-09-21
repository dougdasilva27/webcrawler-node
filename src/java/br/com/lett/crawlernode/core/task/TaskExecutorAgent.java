package br.com.lett.crawlernode.core.task;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class TaskExecutorAgent {
	private ScheduledExecutorService scheduledExecutorService;
	
	private static final int DEFAULT_PERIOD = 1; // 1 second
	private static final int DEFAULT_NTHREADS = 2;
	
	public TaskExecutorAgent() {
		scheduledExecutorService = Executors.newScheduledThreadPool(DEFAULT_NTHREADS);
	}
	
	public TaskExecutorAgent(int nthreads) {
		scheduledExecutorService = Executors.newScheduledThreadPool(nthreads);
	}
	
	public void executeScheduled(MessageFetcher messageFetcher) {
		scheduledExecutorService.scheduleAtFixedRate(messageFetcher, 0, DEFAULT_PERIOD, TimeUnit.SECONDS);
	}
	
	public void executeScheduled(MessageFetcher messageFetcher, int period) {
		scheduledExecutorService.scheduleAtFixedRate(messageFetcher, 0, period, TimeUnit.SECONDS);
	}

}
