package br.com.lett.crawlernode.kernel.task;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.com.lett.crawlernode.main.Main;
import br.com.lett.crawlernode.server.QueueService;
import br.com.lett.crawlernode.util.Logging;

public class CrawlerPoolExecutor extends ThreadPoolExecutor {

	protected static final Logger logger = LoggerFactory.getLogger(CrawlerPoolExecutor.class);

	private final Object lock = new Object();

	private int activeTaskCount = 0;
	private int failedTaskCount = 0;
	private int succeededTaskCount = 0;

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
		synchronized(lock) {
			activeTaskCount++;
		}
	}

	@Override
	protected void afterExecute(Runnable r, Throwable t) {
		super.afterExecute(r, t);

		synchronized(lock) {
			Crawler task = (Crawler)r;
			if (t != null) {
				failedTaskCount++;
				Logging.printLogError(logger,task.session, "Task failed [" + task.session.getUrl() + "]");
			} else {
				succeededTaskCount++;
				Logging.printLogDebug(logger, task.session, "Deleting task: " + task.session.getUrl() + " ...");
				QueueService.deleteMessage(Main.queue, task.session.getSessionId(), task.session.getMessageReceiptHandle());
				Logging.printLogDebug(logger, task.session, "END [trucos = " + task.session.getTrucoAttempts() + "]");
			}
			
			activeTaskCount--;
		}

	}

	public int getActiveTaskCount() {
		synchronized(lock) {
			return activeTaskCount;
		}
	}
	
	public int getFailedTaskCount() {
		synchronized(lock) {
			return failedTaskCount;
		}
	}
	
	public int getSucceededTaskCount() {
		synchronized(lock) {
			return succeededTaskCount;
		}
	}


}
