package br.com.lett.crawlernode.kernel.task;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.com.lett.crawlernode.main.Main;

public class TaskExecutor {
	protected static final Logger logger = LoggerFactory.getLogger(TaskExecutor.class);

	public static final int DEFAULT_NTHREADS = 200;
	public static final int DEFAULT_CORE_NTHREADS = 200;
	public static final int DEFAULT_MAX_NTHREADS = 210;
	public static final int DEFAULT_BLOQUING_QUEUE_MAX_SIZE = 100;
	
	public int maxTasks = Main.executionParameters.getCoreThreads();

	private CrawlerPoolExecutor crawlerPoolExecutor;
	
	public TaskExecutor() {
		crawlerPoolExecutor = new CrawlerPoolExecutor(
				DEFAULT_CORE_NTHREADS,
				DEFAULT_MAX_NTHREADS,
				0L,
				TimeUnit.MILLISECONDS,
				new LinkedBlockingQueue<Runnable>(DEFAULT_BLOQUING_QUEUE_MAX_SIZE),
				new RejectedTaskHandler()
				);
		
		crawlerPoolExecutor.prestartAllCoreThreads();
	}
	
	/**
	 * Constructor create a new task executor with
	 * a maximum pool size.
	 * @param maxThreads the maximum and core pool size for the thread pool
	 */
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
	
	/**
	 * Constructor create a new task executor with
	 * the desired core and maximum pool size.
	 * 
	 * @param coreThreads
	 * @param maxThreads
	 */
	public TaskExecutor(int coreThreads, int maxThreads) {
		crawlerPoolExecutor = new CrawlerPoolExecutor(
				coreThreads,
				maxThreads,
				0L,
				TimeUnit.MILLISECONDS,
				new LinkedBlockingQueue<Runnable>(DEFAULT_BLOQUING_QUEUE_MAX_SIZE),
				new RejectedTaskHandler()
				);
		
		crawlerPoolExecutor.prestartAllCoreThreads();
	}
	
	public TaskExecutor(int coreThreads, int maxThreads, int bloquingQueueSize) {
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
	
	public int getMaximumNumberOfTasks() {
		return this.maxTasks;
	}
	
	public void shutDown() {
		crawlerPoolExecutor.shutdown();
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
	
	public long getSucceededTasksCount() {
		return crawlerPoolExecutor.getSucceededTaskCount();
	}
	
	public long getFailedTasksCount() {
		return crawlerPoolExecutor.getFailedTaskCount();
	}
	
	@Override
	public String toString() {
		StringBuilder stringBuilder = new StringBuilder();
		
		stringBuilder.append("Core number of threads: " + getCoreThreadsCount() + "\n");
		stringBuilder.append("Maximum number of threads: " + getMaxThreadsCount() + "\n");
		stringBuilder.append("Maximum number of tasks: " + getMaximumNumberOfTasks() + "\n");
		
		return stringBuilder.toString();
	}

}
