package br.com.lett.crawlernode.main;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.Message;

import br.com.lett.crawlernode.database.DBCredentials;
import br.com.lett.crawlernode.database.DatabaseCredentialsSetter;
import br.com.lett.crawlernode.database.DatabaseManager;
import br.com.lett.crawlernode.kernel.ControlledTaskExecutor;
import br.com.lett.crawlernode.kernel.ExecutionParameters;
import br.com.lett.crawlernode.kernel.TaskExecutor;
import br.com.lett.crawlernode.kernel.TaskExecutorAgent;
import br.com.lett.crawlernode.kernel.WorkList;
import br.com.lett.crawlernode.kernel.fetcher.Proxies;
import br.com.lett.crawlernode.processor.controller.ResultManager;
import br.com.lett.crawlernode.server.QueueHandler;
import br.com.lett.crawlernode.server.QueueService;
import br.com.lett.crawlernode.util.Logging;


/**
 * 
 * Parameters:
 * -debug : to print debug log messages on console
 * -environment [development,  production]
 * -mode [insights, discovery]
 * 
 * <p>Environments:</p>
 * <ul>
 * <li> development: in this mode we use a testing Amazon SQS queue, named crawler-development;
 * We stil use proxies when running in development mode, because we must test for website blocking and 
 * crawling informations the way it's going be running in the server. The classes in which this mode has some influence, are: 
 * <ul>
 * <li>DataFetcher</li>
 * <li>QueueHandler</li>
 * <li>QueueService</li>
 * <li>DatabaseManager</li>
 * </ul>
 * </li>
 * <li> production: in this mode the Amazon SQS used is the crawler-insights, and the crawler-node can run
 * whatever ecommerce crawler it finds on the queue. Besides, the information inside the message in this mode
 * is expected to be complete, differing from the development, where the only vital information is url, market and city.</li> 
 * </ul>
 * <p>Modes:</p>
 * <ul>
 * <li>insights:</li>
 * <li>discovery:</li>
 * </ul>
 * @author Samir Leao
 *
 */

public class Main {

	private static final Logger logger = LoggerFactory.getLogger(Main.class);

	public static ExecutionParameters 		executionParameters;
	public static Proxies 					proxies;
	public static DBCredentials 			dbCredentials;
	public static DatabaseManager 			dbManager;
	public static ResultManager 			processorResultManager;
	public static AmazonSQS 				queue;

	private static TaskExecutor 			taskExecutor;
	private static ControlledTaskExecutor 	controlledTaskExecutor;
	private static QueueHandler				queueHandler;
	private static WorkList					workList;

	private static int FETCH_TASK_PERIOD = 5000;	// milisseconds
	
	private static int BLOQUING_QUEUE_SIZE = 10; // the max size of the queue used by the TaskExecutor

	public static void main(String args[]) {
		Logging.printLogDebug(logger, "Starting webcrawler-node...");

		// setting execution parameters
		executionParameters = new ExecutionParameters(args);
		executionParameters.setUpExecutionParameters();

		// setting MDC for logging messages
		Logging.setLogMDC(executionParameters);

		// setting database credentials
		DatabaseCredentialsSetter dbCredentialsSetter = new DatabaseCredentialsSetter("crawler");
		dbCredentials = dbCredentialsSetter.setDatabaseCredentials();

		// creating the database manager
		dbManager = new DatabaseManager(dbCredentials);
		dbManager.connect();

		// creating processor result manager
		processorResultManager = new ResultManager(false, dbManager.mongoMongoImages, dbManager);

		// fetching proxies
		proxies = new Proxies();
		proxies.fetchLuminatiProxies();
		proxies.fetchBonanzaProxies();
		proxies.fetchShaderProxies();
		proxies.fetchBuyProxies();
		proxies.fetchStormProxies();

		// create a queue handler that will contain an Amazon SQS instance
		queueHandler = new QueueHandler();
		queue = queueHandler.getSQS();

		// create the work list
		workList = new WorkList(WorkList.DEFAULT_MAX_SIZE);

		// create a task executor
		Logging.printLogDebug(logger, "Creating task executor with a maximum of " + executionParameters.getNthreads() + " threads...");
		//taskExecutor = new TaskExecutor(executionParameters.getNthreads());
		controlledTaskExecutor = new ControlledTaskExecutor(executionParameters.getNthreads(), BLOQUING_QUEUE_SIZE);

		/*
		 * main task
		 */

		Timer mainTask = new Timer();

		//for(int i=0; i<5; i++) {

			mainTask.scheduleAtFixedRate(new TimerTask() {

				@Override
				public void run() {

					//mainTaskWithDefaultTaskExecutor();
					TaskExecutorAgent.performTask(controlledTaskExecutor, queueHandler);

				}
			} , 0, FETCH_TASK_PERIOD);
			//} , 1000*i, FETCH_TASK_PERIOD);

		//}

	}

	/**
	 * Task performed using the Executor from Executors Framework
	 */
	private static void mainTaskWithDefaultTaskExecutor() {

		// request message (tasks) from the Amazon queue
		List<Message> messages = QueueService.requestMessages(queueHandler.getSQS(), workList.maxMessagesToFetch());

		// add the retrieved messages on the work list
		workList.addMessages(messages);

		// submit the tasks to the task executor
		taskExecutor.submitWorkList(workList);
	}

}
