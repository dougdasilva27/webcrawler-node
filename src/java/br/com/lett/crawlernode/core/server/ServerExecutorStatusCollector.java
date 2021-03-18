package br.com.lett.crawlernode.core.server;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.com.lett.crawlernode.util.Logging;

public class ServerExecutorStatusCollector implements Runnable {
	
	private static final Logger logger = LoggerFactory.getLogger(ServerExecutorStatusCollector.class);
	
	private ServerCrawler serverCrawler;
	
	public ServerExecutorStatusCollector(ServerCrawler serverCrawler) {
		this.serverCrawler = serverCrawler;
	}
	
	@Override
	public void run() {
		logExecutorStatusMessage();
	}
	
	private void logExecutorStatusMessage() {				
		JSONObject metadata = new JSONObject();
		
		metadata.put("crawler_node_tasks_active", serverCrawler.getActiveTasks());
		metadata.put("crawler_node_tasks_success", serverCrawler.getSucceededTasks());
		metadata.put("crawler_node_tasks_fail", serverCrawler.getFailedTasksCount());
		metadata.put("crawler_node_tasks_queue_size", serverCrawler.getTaskQueueSize());
		metadata.put("crawler_node_threads_active", serverCrawler.getActiveThreads());
		
		Logging.logDebug(logger, null, metadata, "Server status: ");
	}

}
