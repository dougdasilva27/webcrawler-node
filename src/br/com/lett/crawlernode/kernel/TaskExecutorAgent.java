package br.com.lett.crawlernode.kernel;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.sqs.model.Message;

import br.com.lett.crawlernode.server.QueueHandler;
import br.com.lett.crawlernode.server.QueueService;
import br.com.lett.crawlernode.util.Logging;

public class TaskExecutorAgent {
	private static final Logger logger = LoggerFactory.getLogger(TaskExecutorAgent.class);
	
	/**
	 * 
	 * @param controlledTaskExecutor
	 * @param queueHandler
	 */
	public static void performTask(ControlledTaskExecutor controlledTaskExecutor, QueueHandler queueHandler) {
		
		// computing number of tasks to retrieve from SQS
		//int numTasksToRetrieve = computeNumOfTasksToRetrieve(controlledTaskExecutor);
		int numTasksToRetrieve = 1;

		if (numTasksToRetrieve > 0) { // this will prevent to make an empty request

			// request message (tasks) from the Amazon queue
			List<Message> messages = QueueService.requestMessages(queueHandler.getSQS(), numTasksToRetrieve);

			for (Message message : messages) {

				// check the message
				if ( QueueService.checkMessageIntegrity(message) ) {

					// create a crawler session from the message
					CrawlerSession session = new CrawlerSession(message);

					// create the task
					Runnable task = TaskFactory.createTask(session);

					// submit the task to the executor
					if (task != null) {
						controlledTaskExecutor.executeTask(task);
					} else {
						Logging.printLogError(logger, "Error: task could not be created. [market: " + session.getMarket().getName() + ", city: " + session.getMarket().getCity() + "]" + " deleting message from sqs...");
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
	 * @param controlledTaskExecutor
	 * @return The maximum number of messages to request for the queue.
	 */
	private static int computeNumOfTasksToRetrieve(ControlledTaskExecutor controlledTaskExecutor) {
		int numTasksToRetrieve = controlledTaskExecutor.getMaxThreadsCount() - controlledTaskExecutor.getActiveThreadsCount();
		if (numTasksToRetrieve < 0) numTasksToRetrieve = 0;
		else if (numTasksToRetrieve > QueueService.MAX_MESSAGES_REQUEST) numTasksToRetrieve = QueueService.MAX_MESSAGES_REQUEST;
		return numTasksToRetrieve;
	}

}
