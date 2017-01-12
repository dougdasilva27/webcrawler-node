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
		int activeTasks = server.getActiveTasks();
		long succeededTasks = server.getSucceededTasks();
		long failedTasksCount = server.getFailedTasksCount();
		int taskQueueSize = server.getTaskQueueSize();
		int activeThreads = server.getActiveThreads();
				
		JSONObject metadata = new JSONObject();
		
		metadata.put("crawler_node_tasks_active", activeTasks);
		metadata.put("crawler_node_tasks_success", succeededTasks);
		metadata.put("crawler_node_tasks_fail", failedTasksCount);
		metadata.put("crawler_node_tasks_queue_size", taskQueueSize);
		metadata.put("crawler_node_threads_active", activeThreads);
		
		Logging.printLogDebug(logger, null, metadata, "Registering tasks status...");
	}

}
