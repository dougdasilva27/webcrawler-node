package br.com.lett.crawlernode.core.server;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.com.lett.crawlernode.util.Logging;

public class ServerExecutorStatusCollector implements Runnable {
	
	private static final Logger logger = LoggerFactory.getLogger(ServerExecutorStatusCollector.class);
	
	private Server server;
	
	public ServerExecutorStatusCollector(Server server) {
		this.server = server;
	}
	
	@Override
	public void run() {
		logExecutorStatusMessage();
	}
	
	private void logExecutorStatusMessage() {				
		JSONObject metadata = new JSONObject();
		
		metadata.put("crawler_node_tasks_active", server.getActiveTasks());
		metadata.put("crawler_node_tasks_success", server.getSucceededTasks());
		metadata.put("crawler_node_tasks_fail", server.getFailedTasksCount());
		metadata.put("crawler_node_tasks_queue_size", server.getTaskQueueSize());
		metadata.put("crawler_node_threads_active", server.getActiveThreads());
		//metadata.put("crawler_node_webdriver_instances", server.getWebdriverInstances());
		
		Logging.printLogDebug(logger, null, metadata, "Registering tasks status...");
	}

}
