package br.com.lett.crawlernode.main;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.Message;

import br.com.lett.crawlernode.base.ExecutionParameters;
import br.com.lett.crawlernode.base.TaskExecutor;
import br.com.lett.crawlernode.base.WorkList;
import br.com.lett.crawlernode.database.DBCredentials;
import br.com.lett.crawlernode.database.DatabaseCredentialsSetter;
import br.com.lett.crawlernode.database.DatabaseManager;
import br.com.lett.crawlernode.fetcher.Proxies;
import br.com.lett.crawlernode.processor.controller.ResultManager;
import br.com.lett.crawlernode.queue.QueueHandler;
import br.com.lett.crawlernode.queue.QueueService;
import br.com.lett.crawlernode.util.Logging;


/**
 * 
 * Parameters:
 * -debug : to print debug log messages on console
 * -environment [development,  production]
 * 
 * @author Samir Leao
 *
 */

public class Main {

	private static final Logger logger = LoggerFactory.getLogger(Main.class);

	public static ExecutionParameters 	executionParameters;
	public static Proxies 				proxies;
	public static DBCredentials 		dbCredentials;
	public static DatabaseManager 		dbManager;
	public static ResultManager 		processorResultManager;
	public static AmazonSQS 			queue;

	private static TaskExecutor 		taskExecutor;
	private static QueueHandler			queueHandler;
	private static WorkList				workList;

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
		if (executionParameters.getEnvironment().equals(ExecutionParameters.ENVIRONMENT_PRODUCTION)) {
			proxies = new Proxies();
			proxies.fetchPremiumProxies();
			proxies.fetchRegularProxies();
			proxies.fetchBonanzaProxies();
		}

		// create a queue handler that will contain an Amazon SQS instance
		queueHandler = new QueueHandler();
		queue = queueHandler.getSQS();

		// create the work list
		workList = new WorkList(WorkList.DEFAULT_MAX_SIZE);

		// create a task executor
		taskExecutor = new TaskExecutor(TaskExecutor.DEFAULT_NTHREADS);


		/*
		 * main task -- from time to time goes to server and takes 10 urls
		 */

		Timer mainTask = new Timer();

		mainTask.scheduleAtFixedRate(new TimerTask() {

			@Override
			public void run() {

				// request message (tasks) from the Amazon queue
				List<Message> messages = QueueService.requestMessages(queueHandler.getSQS(), workList.maxMessagesToFetch());

				// add the retrieved messages on the work list
				workList.addMessages(messages);

				// submit the tasks to the task executor
				taskExecutor.submitWorkList(workList);

			} 
		} , 0, 5000); // 5 seconds

	}

}
