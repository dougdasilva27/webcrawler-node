package br.com.lett.crawlernode.core.server;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ServerExecutorStatusAgent {

	private ScheduledExecutorService scheduledExecutorService;

	private static final int DEFAULT_PERIOD = 1; // 1 second

	public ServerExecutorStatusAgent() {
		scheduledExecutorService = Executors.newScheduledThreadPool(1);
	}

	public void executeScheduled(ServerExecutorStatusCollector statusCollector) {
		scheduledExecutorService.scheduleAtFixedRate(statusCollector, 0, DEFAULT_PERIOD, TimeUnit.SECONDS);
	}

	public void executeScheduled(ServerExecutorStatusCollector statusCollector, int period) {
		scheduledExecutorService.scheduleAtFixedRate(statusCollector, 0, period, TimeUnit.SECONDS);
	}

}
