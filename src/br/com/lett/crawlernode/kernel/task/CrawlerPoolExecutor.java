package br.com.lett.crawlernode.kernel.task;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.com.lett.crawlernode.kernel.CrawlerSession;
import br.com.lett.crawlernode.main.Main;
import br.com.lett.crawlernode.server.QueueService;
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
	public CrawlerPoolExecutor(int corePoolSize,
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

		if (!task.session.getType().equals(CrawlerSession.TEST_TYPE)) {
			synchronized(lock) {
				activeTaskCount++;
			}
		}
	}

	@Override
	protected void afterExecute(Runnable r, Throwable t) {
		super.afterExecute(r, t);

		Crawler task = (Crawler)r;

		if (task.session.getType().equals(CrawlerSession.TEST_TYPE)) {
			Logging.printLogDebug(logger, task.session, "END");
		} else {
			synchronized(lock) {
				if (t != null) {
					failedTaskCount++;
					Logging.printLogError(logger,task.session, "Task failed [" + task.session.getUrl() + "]");
				} else {
					succeededTaskCount++;
					Logging.printLogDebug(logger, task.session, "Deleting task: " + task.session.getUrl() + " ...");
					QueueService.deleteMessage(Main.queue, task.session.getSessionId(), task.session.getMessageReceiptHandle());
					Logging.printLogDebug(logger, task.session, "[trucos = " + task.session.getTrucoAttempts() + "]");
					Logging.printLogDebug(logger, task.session, "END");
				}

				activeTaskCount--;
			}
		}

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
