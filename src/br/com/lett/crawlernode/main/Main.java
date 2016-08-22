package br.com.lett.crawlernode.main;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.sqs.AmazonSQS;

import br.com.lett.crawlernode.database.DBCredentials;
import br.com.lett.crawlernode.database.DatabaseCredentialsSetter;
import br.com.lett.crawlernode.database.DatabaseManager;
import br.com.lett.crawlernode.kernel.TaskExecutor;
import br.com.lett.crawlernode.kernel.ExecutionParameters;
import br.com.lett.crawlernode.kernel.MessageFetcher;
import br.com.lett.crawlernode.kernel.TaskExecutorAgent;
import br.com.lett.crawlernode.kernel.fetcher.Proxies;
import br.com.lett.crawlernode.processor.controller.ResultManager;
import br.com.lett.crawlernode.server.QueueHandler;
import br.com.lett.crawlernode.util.Logging;


/**
 * 
 * Parameters:
 * -debug : to print debug log messages on console
 * -environment [development,  production]
 * -mode [insights, discovery, dead]
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

	public static ExecutionParameters 	executionParameters;
	public static Proxies 				proxies;
	public static DBCredentials 		dbCredentials;
	public static DatabaseManager 		dbManager;
	public static AmazonSQS 			queue;
	public static ResultManager 		processorResultManager;

	private static TaskExecutor 		taskExecutor;
	private static TaskExecutorAgent 	taskExecutorAgent;
	private static QueueHandler			queueHandler;

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
		
		// create result manager for processor stage
		processorResultManager = new ResultManager(false, Main.dbManager.mongoMongoImages, Main.dbManager);

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

		// create a task executor
		Logging.printLogDebug(logger, "Creating task executor with a maximum of " + executionParameters.getNthreads() + " threads...");
		taskExecutor = new TaskExecutor(executionParameters.getNthreads());
		
		// schedule threads to keep fethcing messages
		Logging.printLogDebug(logger, "Creating the TaskExecutorAgent...");
		taskExecutorAgent = new TaskExecutorAgent(5); // only 1 thread fetching message
		taskExecutorAgent.executeScheduled( new MessageFetcher(taskExecutor, queueHandler), 5 );

	}

}
