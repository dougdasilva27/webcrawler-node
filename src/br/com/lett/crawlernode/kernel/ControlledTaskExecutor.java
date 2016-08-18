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
	
	public ControlledTaskExecutor() {
		threadPoolExecutor = new ThreadPoolExecutor(
				DEFAULT_MAX_NTHREADS,
				DEFAULT_MAX_NTHREADS,
				0L,
				TimeUnit.MILLISECONDS,
				new LinkedBlockingQueue<Runnable>(DEFAULT_BLOQUING_QUEUE_MAX_SIZE),
				new RejectedTaskHandler()
				);
	}

	public ControlledTaskExecutor(int maxThreads) {
		threadPoolExecutor = new ThreadPoolExecutor(
				maxThreads,
				maxThreads,
				0L,
				TimeUnit.MILLISECONDS,
				new LinkedBlockingQueue<Runnable>(DEFAULT_BLOQUING_QUEUE_MAX_SIZE),
				new RejectedTaskHandler()
				);
	}
	
	public ControlledTaskExecutor(int maxThreads, int bloquingQueueSize) {
		threadPoolExecutor = new ThreadPoolExecutor(
				maxThreads,
				maxThreads,
				0L,
				TimeUnit.MILLISECONDS,
				new LinkedBlockingQueue<Runnable>(bloquingQueueSize),
				new RejectedTaskHandler()
				);
	}
	
	public int getMaxThreadsCount() {
		return threadPoolExecutor.getMaximumPoolSize();
	}
	
	public int getBloquingQueueSize() {
		return threadPoolExecutor.getQueue().size();
	}
	
	public int getActiveThreadsCount() {
		return threadPoolExecutor.getActiveCount();
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

}
