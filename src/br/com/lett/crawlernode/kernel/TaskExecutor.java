package br.com.lett.crawlernode.kernel;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.sqs.model.Message;

import br.com.lett.crawlernode.server.QueueService;
import br.com.lett.crawlernode.util.Logging;

/**
 * An encapsulation of the ExecutorService used to run the tasks from Amazon SQS
 * @author Samir Leao
 *
 */
public class TaskExecutor {
	protected static final Logger logger = LoggerFactory.getLogger(TaskExecutor.class);
	
	public static final int DEFAULT_NTHREADS = 40;
	
	/**
	 * The Executor Service, to whitch the tasks will be submited
	 */
	private ExecutorService executor;
	
	/**
	 * Maximum number of threads in the executor pool of threads
	 */
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
				} else {
					Logging.printLogError(logger, "Error: task could not be created. [market: " + session.getMarket().getName() + ", city: " + session.getMarket().getCity() + "]");
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
