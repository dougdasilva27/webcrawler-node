package br.com.lett.crawlernode.base;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.amazonaws.services.sqs.model.Message;

import br.com.lett.crawlernode.models.CrawlerSession;
import br.com.lett.crawlernode.queue.QueueService;

public class TaskExecutor {

	private ExecutorService executor;
	
	private int maxThreads;

	public TaskExecutor(int maxThreads) {
		executor = Executors.newFixedThreadPool(maxThreads);
		this.setMaxThreads(maxThreads);
	}
	
	/**
	 * 
	 * @param workList
	 */
	public void submitWorkList(WorkList workList) {

		// check the message fields
		while (!workList.isEmpty()) {
			
			// get one message from the work list
			Message message = workList.getMessage();
			
			if (QueueService.checkMessage(message)) { // checking message fields

				// create a crawler session from the message
				CrawlerSession session = new CrawlerSession(message);

				// create the task
				Runnable task = TaskFactory.createTask(session);

				// submit the task to the executor
				if (task != null) {
					executor.execute(task);
				}
			}
		}
	}

	public ExecutorService getExecutor() {
		return executor;
	}

	public void setExecutor(ExecutorService executor) {
		this.executor = executor;
	}

	public int getMaxThreads() {
		return maxThreads;
	}

	public void setMaxThreads(int maxThreads) {
		this.maxThreads = maxThreads;
	}


}
