package br.com.lett.crawlernode.kernel;

import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.com.lett.crawlernode.util.Logging;

/**
 * This class is an implementation of RejectedExecutionHandler interface
 * to be used by the ControlledTaskExecutor, which uses, internally, a
 * ThreadPoolExecutor. The RejectedTaskHandler tells the ThreadPoolExecutor what
 * to do when a new task is submited to execution, but the blocking queue is
 * already full.
 * @author Samir Leao
 *
 */
public class RejectedTaskHandler implements RejectedExecutionHandler {
	protected static final Logger logger = LoggerFactory.getLogger(RejectedExecutionHandler.class);
	
	@Override
	public void rejectedExecution(Runnable task, ThreadPoolExecutor executor) {
		Logging.printLogDebug(logger, "Rejected task: " + task.toString());
	}
}
