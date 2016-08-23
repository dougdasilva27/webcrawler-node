package br.com.lett.crawlernode.kernel.task;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TaskExecutor {
	protected static final Logger logger = LoggerFactory.getLogger(TaskExecutor.class);

	public static final int DEFAULT_NTHREADS = 200;
	public static final int DEFAULT_MAX_NTHREADS = 200;
	public static final int DEFAULT_BLOQUING_QUEUE_MAX_SIZE = 100;

	private CrawlerPoolExecutor crawlerPoolExecutor;
	
	public TaskExecutor() {
		crawlerPoolExecutor = new CrawlerPoolExecutor(
				DEFAULT_MAX_NTHREADS,
				DEFAULT_MAX_NTHREADS,
				0L,
				TimeUnit.MILLISECONDS,
				new LinkedBlockingQueue<Runnable>(DEFAULT_BLOQUING_QUEUE_MAX_SIZE),
				new RejectedTaskHandler()
				);
		
		crawlerPoolExecutor.prestartAllCoreThreads();
	}

	public TaskExecutor(int maxThreads) {
		crawlerPoolExecutor = new CrawlerPoolExecutor(
				maxThreads,
				maxThreads,
				0L,
				TimeUnit.MILLISECONDS,
				new LinkedBlockingQueue<Runnable>(DEFAULT_BLOQUING_QUEUE_MAX_SIZE),
				new RejectedTaskHandler()
				);
		
		crawlerPoolExecutor.prestartAllCoreThreads();
	}
	
	public TaskExecutor(int maxThreads, int bloquingQueueSize) {
		crawlerPoolExecutor = new CrawlerPoolExecutor(
				maxThreads,
				maxThreads,
				0L,
				TimeUnit.MILLISECONDS,
				new LinkedBlockingQueue<Runnable>(bloquingQueueSize),
				new RejectedTaskHandler()
				);
		
		crawlerPoolExecutor.prestartAllCoreThreads();
	}
	
	public int getMaxThreadsCount() {
		return crawlerPoolExecutor.getMaximumPoolSize();
	}
	
	public int getCoreThreadsCount() {
		return crawlerPoolExecutor.getCorePoolSize();
	}
	
	public int getBloquingQueueSize() {
		return crawlerPoolExecutor.getQueue().size();
	}
	
	public int getBloquingQueueRemainingCapacity() {
		return crawlerPoolExecutor.getQueue().remainingCapacity();
	}
	
	public int getActiveThreadsCount() {
		return crawlerPoolExecutor.getActiveCount();
	}
	
	public long getCompletedTasksCount() {
		return crawlerPoolExecutor.getCompletedTaskCount();
	}

	public void executeTask(Runnable task) {
		crawlerPoolExecutor.execute(task);
	}

	public CrawlerPoolExecutor getExecutor() {
		return crawlerPoolExecutor;
	}
	
	public int getActiveTasksCount() {
		return crawlerPoolExecutor.getActiveTaskCount();
	}
	
	public int getFailedTasksCount() {
		return crawlerPoolExecutor.getFailedTaskCount();
	}

}
