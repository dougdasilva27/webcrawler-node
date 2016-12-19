package br.com.lett.crawlernode.main;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.com.lett.crawlernode.core.crawler.Resources;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.models.Markets;
import br.com.lett.crawlernode.core.task.MessageFetcher;
import br.com.lett.crawlernode.core.task.TaskExecutor;
import br.com.lett.crawlernode.core.task.TaskExecutorAgent;
import br.com.lett.crawlernode.database.DBCredentials;
import br.com.lett.crawlernode.database.DatabaseCredentialsSetter;
import br.com.lett.crawlernode.database.DatabaseManager;
import br.com.lett.crawlernode.database.Persistence;
import br.com.lett.crawlernode.processor.controller.ResultManager;
import br.com.lett.crawlernode.server.QueueHandler;
import br.com.lett.crawlernode.util.CommonMethods;
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
	public static ProxyCollection 		proxies;
	public static DBCredentials 		dbCredentials;
	public static DatabaseManager 		dbManager;
	public static ResultManager 		processorResultManager;
	public static QueueHandler			queueHandler;
	
	public static Markets				markets;

	private static TaskExecutor 		taskExecutor;
	private static TaskExecutorAgent 	taskExecutorAgent;
	
	public static Resources				globalResources;

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
		
		// fetch all markets information from database
		markets = new Markets(dbManager);
		
		// initialize temporary folder for images download
		Persistence.initializeImagesDirectories(markets);
		
		// create result manager for processor stage
		processorResultManager = new ResultManager(false, Main.dbManager.mongoMongoImages, Main.dbManager);

		// fetching proxies
		proxies = new ProxyCollection(markets);
		proxies.setBonanzaProxies();
		proxies.setBuyProxies();
		proxies.setStormProxies();
		proxies.setCharityProxy();
		proxies.setAzureProxy();
		
		// set global resources
		globalResources = new Resources();
		try {
			globalResources.setWebdriverExtension(downloadWebdriverExtension());
		} catch (MalformedURLException e) {
			Logging.printLogError(logger, "Error in resource URL.");
			Logging.printLogError(logger, CommonMethods.getStackTraceString(e));
		} catch (IOException e) {
			Logging.printLogError(logger, "Error during resource download.");
			Logging.printLogError(logger, CommonMethods.getStackTraceString(e));
		}

		// create a queue handler that will contain an Amazon SQS instance
		queueHandler = new QueueHandler();

		// create a task executor
		Logging.printLogDebug(logger, "Creating task executor...");
		taskExecutor = new TaskExecutor(executionParameters.getCoreThreads(), executionParameters.getNthreads());
		Logging.printLogDebug(logger, taskExecutor.toString());
		
		// schedule threads to keep fetching messages
		Logging.printLogDebug(logger, "Creating the TaskExecutorAgent...");
		taskExecutorAgent = new TaskExecutorAgent(1); // only 1 thread fetching message
		taskExecutorAgent.executeScheduled( new MessageFetcher(taskExecutor, queueHandler, markets), 1 );
		
	}
	
	private static File downloadWebdriverExtension() throws IOException {
		BufferedInputStream in = null;
	    FileOutputStream fout = null;
	    try {
	        in = new BufferedInputStream(new URL("https://s3.amazonaws.com/code-deploy-lett/crawler-node-util/modheader_2_1_1.crx").openStream());
	        File f = new File("modheader_2_1_1.crx");
	        if (!f.exists()) {
	        	f.createNewFile();
	        }
	        fout = new FileOutputStream(f);

	        final byte[] data = new byte[1024];
	        int count;
	        while ((count = in.read(data, 0, 1024)) != -1) {
	            fout.write(data, 0, count);
	        }
	        
	        return f;
	        
	    } finally {
	        if (in != null) {
	            in.close();
	        }
	        if (fout != null) {
	            fout.close();
	        }
	    }
	}

}
