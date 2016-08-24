package br.com.lett.crawlernode.kernel;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.sqs.model.Message;

import br.com.lett.crawlernode.kernel.task.TaskExecutor;
import br.com.lett.crawlernode.kernel.task.TaskFactory;
import br.com.lett.crawlernode.server.QueueHandler;
import br.com.lett.crawlernode.server.QueueService;
import br.com.lett.crawlernode.util.Logging;

public class MessageFetcher implements Runnable {
	
	private static final Logger logger = LoggerFactory.getLogger(MessageFetcher.class);
	
	private TaskExecutor taskExecutor;
	private QueueHandler queueHandler;
	
	public MessageFetcher(TaskExecutor taskExecutor, QueueHandler queueHandler) {
		this.taskExecutor = taskExecutor;
		this.queueHandler = queueHandler;
	}

	@Override
	public void run() {

		// computing number of tasks to retrieve from SQS
		int numTasksToRetrieve = computeNumOfTasksToRetrieve();

		if (numTasksToRetrieve > 0) { // this will prevent to make an empty request

			// request messages from the Amazon queue
			Logging.printLogDebug(logger, "Requesting for a maximum of " + numTasksToRetrieve + " tasks on queue...");
			List<Message> messages = QueueService.requestMessages(queueHandler.getSQS(), numTasksToRetrieve);
			Logging.printLogDebug(logger, "Request returned with " + messages.size() + " tasks");

			for (Message message : messages) {

				// check the message
				if ( QueueService.checkMessageIntegrity(message) ) {

					// create a crawler session from the message
					CrawlerSession session = new CrawlerSession(message);

					// create the task
					Runnable task = TaskFactory.createTask(session);

					// submit the task to the executor
					if (task != null) {
						taskExecutor.executeTask(task);
					} else {
						Logging.printLogError(logger, session, "Error: task could not be created...deleting message from sqs...");
						QueueService.deleteMessage(queueHandler.getSQS(), message);						
					}
				}

				// something is wrong with the message content
				else {
					Logging.printLogError(logger, "Message refused [failed on integrity checking]. Will delete it from the queue...");
					QueueService.deleteMessage(queueHandler.getSQS(), message);
				}

			}
		}
		else {
			Logging.printLogDebug(logger, "Won't ask for any message because the pool is at it's maximum!");
		}
	}

	/**
	 * Computes the number of tasks to request for from AmazonSQS, based
	 * on the number of active threads on the ThreadPool. Note that this is the maximum
	 * number. In some cases the request can return less than the maximum, because of the long
	 * pooling policy.
	 * @return The maximum number of messages to request for the queue.
	 */
	private int computeNumOfTasksToRetrieve() {
		int activeTasks = taskExecutor.getActiveTasksCount();
		long succeededTasks = taskExecutor.getSucceededTasksCount();
		long failedTasksCount = taskExecutor.getFailedTasksCount();
		int taskQueueSize = taskExecutor.getBloquingQueueSize();
		int coreThreadPoolSize = taskExecutor.getCoreThreadsCount();
		int activeThreads = taskExecutor.getActiveThreadsCount();
		
		int diff = coreThreadPoolSize - activeTasks;
		
		Logging.printLogDebug(logger, 
				"[TASKS_ACTIVE]" + activeTasks +
				" [TASKS_SUCCESS]" + succeededTasks +
				" [TASKS_FAIL]" + failedTasksCount +
				" [TASKS_QUEUE_SIZE]" + taskQueueSize + 
				" [THREADS_ACTIVE]" + activeThreads
				);
		
		if (diff > QueueService.MAX_MESSAGES_REQUEST) {
			if (taskQueueSize > 0) return 0;
			return QueueService.MAX_MESSAGES_REQUEST;
		} 
		else {
			if (taskQueueSize > 0) return 0;
			return diff;
		}
	}
	
}
