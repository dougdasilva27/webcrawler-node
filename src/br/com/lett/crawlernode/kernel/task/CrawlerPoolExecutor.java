package br.com.lett.crawlernode.kernel.task;

import java.util.ArrayList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.com.lett.crawlernode.database.Persistence;
import br.com.lett.crawlernode.main.Main;
import br.com.lett.crawlernode.server.QueueService;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.Logging;

/**
 * This class is a custom implementation of a ThreadPoolExecutor.
 * It only overrides the beforeExecute and afterExecute methods from
 * ThreadPoolExecutor class. The main purpose of this class is to keep
 * track informations about the tasks being executed. This informations
 * are used mainly to do an intelligent fetching of tasks from the remote
 * queue, and prevent to flood the pool executor with tasks, and prevent the
 * use of the bloquing queue.
 * @author Samir Leao
 *
 */
public class CrawlerPoolExecutor extends ThreadPoolExecutor {

	protected static final Logger logger = LoggerFactory.getLogger(CrawlerPoolExecutor.class);

	/** Object to be used as a mutex to access the task counters*/
	private final Object lock = new Object();

	private int activeTaskCount = 0;
	private long failedTaskCount = 0;
	private long succeededTaskCount = 0;

	/**
	 * Creates a new {@code CrawlerPoolExecutor} with the given initial
	 * parameters and default thread factory.
	 *
	 * @param corePoolSize the number of threads to keep in the pool, even
	 *        if they are idle, unless {@code allowCoreThreadTimeOut} is set
	 * @param maximumPoolSize the maximum number of threads to allow in the
	 *        pool
	 * @param keepAliveTime when the number of threads is greater than
	 *        the core, this is the maximum time that excess idle threads
	 *        will wait for new tasks before terminating.
	 * @param unit the time unit for the {@code keepAliveTime} argument
	 * @param workQueue the queue to use for holding tasks before they are
	 *        executed.  This queue will hold only the {@code Runnable}
	 *        tasks submitted by the {@code execute} method.
	 * @param handler the handler to use when execution is blocked
	 *        because the thread bounds and queue capacities are reached
	 * @throws IllegalArgumentException if one of the following holds:<br>
	 *         {@code corePoolSize < 0}<br>
	 *         {@code keepAliveTime < 0}<br>
	 *         {@code maximumPoolSize <= 0}<br>
	 *         {@code maximumPoolSize < corePoolSize}
	 * @throws NullPointerException if {@code workQueue}
	 *         or {@code handler} is null
	 */
	public CrawlerPoolExecutor(
			int corePoolSize,
			int maximumPoolSize,
			long keepAliveTime,
			TimeUnit unit,
			BlockingQueue<Runnable> workQueue,
			RejectedExecutionHandler handler) {

		super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, handler);
	}

	@Override
	protected void beforeExecute(Thread t, Runnable r) {
		super.beforeExecute(t, r);

		Crawler task = (Crawler)r;
		Logging.printLogDebug(logger, task.session, "START");

		synchronized(lock) {
			activeTaskCount++;
		}

	}

	@Override
	protected void afterExecute(Runnable r, Throwable t) {
		super.afterExecute(r, t);

		Crawler task = (Crawler)r;
		ArrayList<CrawlerSessionError> errors = task.session.getErrors();

		synchronized(lock) {
			activeTaskCount--;

			if (t != null || errors.size() > 0) {
				failedTaskCount++;
			} else {
				succeededTaskCount++;
			}
		}

		// in case of the thread pool get a non checked exception
		if (t != null) {
			Logging.printLogError(logger, task.session, "Task failed [" + task.session.getUrl() + "]");
			Logging.printLogError(logger, task.session, CommonMethods.getStackTrace(t));
			
			Persistence.setTaskStatusOnMongo(Persistence.MONGO_TASK_STATUS_FAILED, task.session, Main.dbManager.mongoBackendPanel);
		} 
		
		else {
			
			// errors collected manually
			// they can be exceptions or business logic errors
			// and are all gathered inside the session
			if (errors.size() > 0) {
				Logging.printLogError(logger, task.session, "Task failed [" + task.session.getUrl() + "]");

				// print all errors of type exceptions
				for (CrawlerSessionError error : errors) {
					if (error.getType().equals(CrawlerSessionError.EXCEPTION)) {
						Logging.printLogError(logger, task.session, error.getErrorContent());
					}
				}
				
				Persistence.setTaskStatusOnMongo(Persistence.MONGO_TASK_STATUS_FAILED, task.session, Main.dbManager.mongoBackendPanel);

			}
			
			// only remove the task from queue if it was flawless
			else {
				Logging.printLogDebug(logger, task.session, "Task completed.");
				Logging.printLogDebug(logger, task.session, "Deleting task: " + task.session.getUrl() + " ...");
				QueueService.deleteMessage(Main.queueHandler, task.session.getQueueName(), task.session.getMessageReceiptHandle());
				
				Persistence.setTaskStatusOnMongo(Persistence.MONGO_TASK_STATUS_DONE, task.session, Main.dbManager.mongoBackendPanel);
			}
		}

		Logging.printLogDebug(logger, task.session, "[ACTIVE_VOID_ATTEMPTS]" + task.session.getVoidAttempts());
		Logging.printLogDebug(logger, task.session, "[TRUCO_ATTEMPTS]" + task.session.getTrucoAttempts());
		Logging.printLogDebug(logger, task.session, "END");

	}

	public int getActiveTaskCount() {
		synchronized(lock) {
			return activeTaskCount;
		}
	}

	public long getFailedTaskCount() {
		synchronized(lock) {
			return failedTaskCount;
		}
	}

	public long getSucceededTaskCount() {
		synchronized(lock) {
			return succeededTaskCount;
		}
	}


}
