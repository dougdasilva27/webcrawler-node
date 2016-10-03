package br.com.lett.crawlernode.core.task;

import java.util.ArrayList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.com.lett.crawlernode.core.session.CrawlerSession;
import br.com.lett.crawlernode.core.session.CrawlerSessionError;
import br.com.lett.crawlernode.core.session.DiscoveryCrawlerSession;
import br.com.lett.crawlernode.core.session.InsightsCrawlerSession;
import br.com.lett.crawlernode.core.session.SeedCrawlerSession;
import br.com.lett.crawlernode.core.session.TestCrawlerSession;
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

		if (r instanceof Crawler) {
			Logging.printLogDebug(logger, ((Crawler)r).session, "START");
		}
		else if (r instanceof ImageCrawler) {
			Logging.printLogDebug(logger, ((ImageCrawler)r).session, "START");
		}

		synchronized(lock) {
			activeTaskCount++;
		}

	}

	@Override
	protected void afterExecute(Runnable r, Throwable t) {
		super.afterExecute(r, t);

		// get the array of errors
		ArrayList<CrawlerSessionError> errors = retrieveTaskErrors(r);

		synchronized(lock) {
			activeTaskCount--;

			if (t != null || errors.size() > 0) {
				failedTaskCount++;
			} else {
				succeededTaskCount++;
			}
		}

		// finalize the task
		finalizeTask(r, t);
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

	/**
	 * 
	 * @param r
	 * @return
	 */
	private ArrayList<CrawlerSessionError> retrieveTaskErrors(Runnable r) {
		if (r instanceof Crawler) {
			return ((Crawler)r).session.getErrors();
		}
		else if (r instanceof ImageCrawler) {
			return ((ImageCrawler)r).session.getErrors();
		}

		return new ArrayList<CrawlerSessionError>();
	}

	private void finalizeTask(Runnable r, Throwable t) {
		if (r instanceof Crawler) {
			finalizeCrawlerTask(((Crawler)r).session, t);
		}
		else if (r instanceof ImageCrawler) {
			finalizeImageTask(((ImageCrawler)r).session, t);
		}
	}

	/**
	 * Finalization routine.
	 * 
	 * @param session
	 * @param t
	 */
	private void finalizeCrawlerTask(CrawlerSession session, Throwable t) {
		ArrayList<CrawlerSessionError> errors = session.getErrors();

		Logging.printLogDebug(logger, session, "Finalizing session of type [" + session.getClass().getSimpleName() + "]");

		// in case of the thread pool get a non checked exception
		if (t != null) {
			Logging.printLogError(logger, session, "Task failed [" + session.getUrl() + "]");
			Logging.printLogError(logger, session, CommonMethods.getStackTrace(t));

			Persistence.setTaskStatusOnMongo(Persistence.MONGO_TASK_STATUS_FAILED, session, Main.dbManager.mongoBackendPanel);
		} 

		else {

			// errors collected manually
			// they can be exceptions or business logic errors
			// and are all gathered inside the session
			if (errors.size() > 0) {
				Logging.printLogError(logger, session, "Task failed [" + session.getUrl() + "]");

				// print all errors of type exceptions
				for (CrawlerSessionError error : errors) {
					if (error.getType().equals(CrawlerSessionError.EXCEPTION)) {
						Logging.printLogError(logger, session, error.getErrorContent());
					}
				}

				Persistence.setTaskStatusOnMongo(Persistence.MONGO_TASK_STATUS_FAILED, session, Main.dbManager.mongoBackendPanel);
			}

			// only remove the task from queue if it was flawless
			// and if we are not testing, because when testing there is no message processing
			else if (session instanceof InsightsCrawlerSession || session instanceof SeedCrawlerSession || session instanceof DiscoveryCrawlerSession) {
				Logging.printLogDebug(logger, session, "Task completed.");
				Logging.printLogDebug(logger, session, "Deleting task: " + session.getUrl() + " ...");

				QueueService.deleteMessage(Main.queueHandler, session.getQueueName(), session.getMessageReceiptHandle());

				Persistence.setTaskStatusOnMongo(Persistence.MONGO_TASK_STATUS_DONE, session, Main.dbManager.mongoBackendPanel);
			}
		}

		// only print statistics of void and truco if we are running an Insights session crawling
		if (session instanceof InsightsCrawlerSession) {
			Logging.printLogDebug(logger, session, "[ACTIVE_VOID_ATTEMPTS]" + session.getVoidAttempts());
			Logging.printLogDebug(logger, session, "[TRUCO_ATTEMPTS]" + session.getTrucoAttempts());
		}

		Logging.printLogDebug(logger, session, "END");
	}

	/**
	 * 
	 * @param session
	 * @param t
	 */
	private void finalizeImageTask(CrawlerSession session, Throwable t) {
		ArrayList<CrawlerSessionError> errors = session.getErrors();

		Logging.printLogDebug(logger, session, "Finalizing session of type [" + session.getClass().getSimpleName() + "]");

		// in case of the thread pool get a non checked exception
		if (t != null) {
			Logging.printLogError(logger, session, "Task failed!");
			Logging.printLogError(logger, session, CommonMethods.getStackTrace(t));
		}
		else {
			if (errors.size() > 0) {
				Logging.printLogError(logger, session, "Task failed!");

				// print all errors of type exceptions
				for (CrawlerSessionError error : errors) {
					if (error.getType().equals(CrawlerSessionError.EXCEPTION)) {
						Logging.printLogError(logger, session, error.getErrorContent());
					}
				}
			}

			// only remove the task from queue if it was flawless
			Logging.printLogDebug(logger, session, "Task completed.");
			Logging.printLogDebug(logger, session, "Deleting task: " + session.getUrl() + " ...");

			QueueService.deleteMessage(Main.queueHandler, session.getQueueName(), session.getMessageReceiptHandle());
		}
		
		// clear the session
		session.clearSession();

		Logging.printLogDebug(logger, session, "END");
	}


}
