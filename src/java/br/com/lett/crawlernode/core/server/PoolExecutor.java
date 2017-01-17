package br.com.lett.crawlernode.core.server;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is a custom implementation of a ThreadPoolExecutor.
 * It only overrides the beforeExecute and afterExecute methods from
 * ThreadPoolExecutor class. The main purpose of this class is to keep
 * track informations about the tasks being executed. This informations
 * are used mainly to do an intelligent fetching of tasks from the remote
 * queue, and prevent to flood the pool executor with tasks, and prevent the
 * use of the bloquing queue.
 * 
 * @author Samir Leao
 *
 */
public class PoolExecutor extends ThreadPoolExecutor {

	protected static final Logger logger = LoggerFactory.getLogger(PoolExecutor.class);
	
	public static final int DEFAULT_NTHREADS = 200;
	public static final int DEFAULT_CORE_NTHREADS = 200;
	public static final int DEFAULT_MAX_NTHREADS = 210;
	public static final int DEFAULT_BLOQUING_QUEUE_MAX_SIZE = 100;
	
	private int corePoolSize;
	private int maximumPoolSize;
	
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
	public PoolExecutor(
			int corePoolSize,
			int maximumPoolSize,
			long keepAliveTime,
			TimeUnit unit,
			BlockingQueue<Runnable> workQueue,
			RejectedExecutionHandler handler) {

		super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, handler);
		super.prestartAllCoreThreads();
		
		this.corePoolSize = corePoolSize;
		this.maximumPoolSize = maximumPoolSize;
	}

	@Override
	protected void beforeExecute(Thread t, Runnable r) {
		super.beforeExecute(t, r);

		synchronized(lock) {
			activeTaskCount++;
		}
	}

	@Override
	protected void afterExecute(Runnable r, Throwable t) {
		super.afterExecute(r, t);

		synchronized(lock) {
			activeTaskCount--;
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
	
	public int getMaxThreadsCount() {
		return getMaximumPoolSize();
	}
	
	public int getCoreThreadsCount() {
		return getCorePoolSize();
	}
	
	public int getBloquingQueueSize() {
		return getQueue().size();
	}
	
	public int getBloquingQueueRemainingCapacity() {
		return getQueue().remainingCapacity();
	}
	
	public int getActiveThreadsCount() {
		return getActiveCount();
	}

	public void executeTask(Runnable task) {
		execute(task);
	}
	
	@Override
	public String toString() {
		return 
				"[" +
				"corePoolSize=" + corePoolSize + ", " +
				"maximumPoolSize=" + maximumPoolSize + 
				"]";
	}

}
