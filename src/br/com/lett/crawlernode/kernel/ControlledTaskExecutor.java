package br.com.lett.crawlernode.kernel;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ControlledTaskExecutor {
	protected static final Logger logger = LoggerFactory.getLogger(ControlledTaskExecutor.class);

	public static final int DEFAULT_NTHREADS = 100;
	public static final int DEFAULT_MAX_NTHREADS = 100;
	public static final int DEFAULT_BLOQUING_QUEUE_MAX_SIZE = 100;

	private ThreadPoolExecutor threadPoolExecutor;

	/**
	 * Maximum number of threads in the executor pool of threads
	 */
	private int maxThreads;


	public ControlledTaskExecutor(int maxThreads) {
		threadPoolExecutor = new ThreadPoolExecutor(
				maxThreads,
				maxThreads,
				0L,
				TimeUnit.MILLISECONDS,
				new LinkedBlockingQueue<Runnable>(DEFAULT_BLOQUING_QUEUE_MAX_SIZE),
				new RejectedTaskHandler()
				);

		this.maxThreads = maxThreads;
	}
	
	public long getCompletedTasksCount() {
		return threadPoolExecutor.getCompletedTaskCount();
	}

	public void executeTask(Runnable task) {
		threadPoolExecutor.execute(task);
	}

	public ThreadPoolExecutor getExecutor() {
		return threadPoolExecutor;
	}

	public int getMaxThreads() {
		return maxThreads;
	}

	public void setMaxThreads(int maxThreads) {
		this.maxThreads = maxThreads;
	}

}
