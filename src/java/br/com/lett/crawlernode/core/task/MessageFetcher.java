package br.com.lett.crawlernode.core.task;

import java.util.List;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.sqs.model.Message;

import br.com.lett.crawlernode.core.models.Markets;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.session.SessionFactory;
import br.com.lett.crawlernode.server.QueueHandler;
import br.com.lett.crawlernode.server.QueueService;
import br.com.lett.crawlernode.server.SQSRequestResult;
import br.com.lett.crawlernode.util.Logging;

public class MessageFetcher implements Runnable {
	
	private static final Logger logger = LoggerFactory.getLogger(MessageFetcher.class);
	
	private TaskExecutor taskExecutor;
	private QueueHandler queueHandler;
	private Markets markets;
	
	public MessageFetcher(TaskExecutor taskExecutor, QueueHandler queueHandler, Markets markets) {
		this.taskExecutor = taskExecutor;
		this.queueHandler = queueHandler;
		this.markets = markets;
	}

	@Override
	public void run() {

		// computing number of tasks to retrieve from SQS
		int numTasksToRetrieve = computeNumOfTasksToRetrieve();

		if (numTasksToRetrieve > 0) { // this will prevent to make an empty request
			
			Logging.printLogDebug(logger, "Requesting for a maximum of " + numTasksToRetrieve + " message.");
			
			// request messages from the Amazon queue
			SQSRequestResult result = QueueService.requestMessages(queueHandler, numTasksToRetrieve);
			List<Message> messages = result.getMessages();
			
			if (messages.size() == 0) {
				Logging.printLogDebug(logger, "Request returned with 0 messages.");
			} else {
				Logging.printLogDebug(logger, "Request returned with " + messages.size() + " tasks from queue " + result.getQueueName());
			}

			for (Message message : messages) {

				// check the message
				if ( QueueService.checkMessageIntegrity(message, result.getQueueName()) ) {
					
					// create a crawler session from the message
					Logging.printLogDebug(logger, "Creating session...");
					Session session = SessionFactory.createSession(message, result.getQueueName(), markets);
					
					Logging.printLogDebug(logger, session, "Created a session of type: [" + session.getClass().getSimpleName() + "]");
					Logging.printLogDebug(logger, session, session.toString());

					// create the task
					Runnable task = TaskFactory.createTask(session);

					// submit the task to the executor
					if (task != null) {
						taskExecutor.executeTask(task);
					} else {
						Logging.printLogError(logger, session, "Error: task could not be created.");					
					}
				}

				// something is wrong with the message content
				else {
					Logging.printLogError(logger, "Message refused [failed on integrity checking].");
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
		int maxTasks = taskExecutor.getMaximumNumberOfTasks();
		long succeededTasks = taskExecutor.getSucceededTasksCount();
		long failedTasksCount = taskExecutor.getFailedTasksCount();
		int taskQueueSize = taskExecutor.getBloquingQueueSize();
		//int coreThreadPoolSize = taskExecutor.getCoreThreadsCount();
		int activeThreads = taskExecutor.getActiveThreadsCount();
		
		int diff = maxTasks - activeTasks;
		
		JSONObject metadata = new JSONObject();
		
		metadata.put("crawler_node_tasks_active", activeTasks);
		metadata.put("crawler_node_tasks_success", succeededTasks);
		metadata.put("crawler_node_tasks_fail", failedTasksCount);
		metadata.put("crawler_node_tasks_queue_size", taskQueueSize);
		metadata.put("crawler_node_threads_active", activeThreads);
		
		Logging.printLogDebug(logger, null, metadata, "Registering tasks status...");
		
		if (diff <= 0) return 0;
		if (taskQueueSize > 0) return 0;
		
		if (diff > QueueService.MAX_MESSAGES_REQUEST) {	
			return QueueService.MAX_MESSAGES_REQUEST;
		} else {
			return diff;
		}
	}
	
}
