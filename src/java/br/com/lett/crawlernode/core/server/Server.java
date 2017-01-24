package br.com.lett.crawlernode.core.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.net.httpserver.HttpServer;

import br.com.lett.crawlernode.core.task.base.RejectedTaskHandler;
import br.com.lett.crawlernode.main.Main;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.Logging;

public class Server {

	private static final Logger logger = LoggerFactory.getLogger(Server.class);
	
	public static final String MSG_TASK_COMPLETED = "task completed";
	public static final String MSG_TASK_FAILED = "task failed";
	public static final String MSG_METHOD_NOT_ALLOWED = "request method not allowed";
	public static final String MSG_SERVER_HEALTH_OK = "the server is fine";
	public static final String MSG_BAD_REQUEST = "bad request";
	
	public static final int HTTP_STATUS_CODE_OK = 200;
	public static final int HTTP_STATUS_CODE_SERVER_ERROR = 500;
	public static final int HTTP_STATUS_CODE_BAD_REQUEST = 400;
	public static final int HTTP_STATUS_CODE_METHOD_NOT_ALLOWED = 402;
	public static final int HTTP_STATUS_CODE_NOT_FOUND = 404;
	
	public static final String ENDPOINT_TASK = "/crawler-task";
	public static final String ENDPOINT_HEALTH_CHECK = "/health-check";

	private static final int SERVER_PORT = 5000;
	private static final String SERVER_HOST = "localhost";

	private HttpServer httpServer;
	private PoolExecutor executor;

	private final Object lock = new Object();
	private long succeededTasks;
	private long failedTasksCount;

	public Server() {
		Logging.printLogDebug(logger, "creating executor....");
		createExecutor();
		Logging.printLogDebug(logger, "done.");
		Logging.printLogDebug(logger, executor.toString());

		Logging.printLogDebug(logger, "creating server [" + SERVER_HOST + "][" + SERVER_PORT + "]....");
		createServer(executor);
		Logging.printLogDebug(logger, "done.");
	}

	private void createExecutor() {
		executor = new PoolExecutor(
				Main.executionParameters.getCoreThreads(), 
				Main.executionParameters.getCoreThreads(),
				0L,
				TimeUnit.SECONDS,
				new LinkedBlockingQueue<Runnable>(PoolExecutor.DEFAULT_BLOQUING_QUEUE_MAX_SIZE),
				new RejectedTaskHandler());
	}

	private void createServer(Executor executor) {
		try {
			httpServer = HttpServer.create(new InetSocketAddress(SERVER_HOST, SERVER_PORT), 0);
			ServerHandler serverHandler = new ServerHandler();
			
			httpServer.createContext(Server.ENDPOINT_TASK, serverHandler);
			httpServer.createContext(Server.ENDPOINT_HEALTH_CHECK, serverHandler);

			httpServer.setExecutor(executor);
			httpServer.start();

		} catch (IOException ex) {
			Logging.printLogError(logger, "error creating server.");
			CommonMethods.getStackTraceString(ex);
		}
	}

	public int getActiveTasks() {
		return executor.getActiveTaskCount();
	}

	public void incrementSucceededTasks() {
		synchronized(lock) {
			succeededTasks++;
		}
	}
	
	public void incrementFailedTasks() {
		synchronized(lock) {
			failedTasksCount++;
		}
	}

	public long getSucceededTasks() {
		synchronized(lock) {
			return succeededTasks;
		}
	}

	public long getFailedTasksCount() {
		synchronized(lock) {
			return failedTasksCount;
		}
	}

	public int getTaskQueueSize() {
		return executor.getBloquingQueueSize();
	}

	public int getActiveThreads() {
		return executor.getActiveThreadsCount();
	}

}
