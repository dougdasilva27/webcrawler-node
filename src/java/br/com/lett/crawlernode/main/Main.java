package br.com.lett.crawlernode.main;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.URL;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.net.httpserver.HttpServer;

import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.models.Markets;
import br.com.lett.crawlernode.core.server.PoolExecutor;
import br.com.lett.crawlernode.core.server.WebcrawlerServer;
import br.com.lett.crawlernode.core.task.Resources;
import br.com.lett.crawlernode.core.task.base.RejectedTaskHandler;
import br.com.lett.crawlernode.database.DBCredentials;
import br.com.lett.crawlernode.database.DatabaseCredentialsSetter;
import br.com.lett.crawlernode.database.DatabaseManager;
import br.com.lett.crawlernode.database.Persistence;
import br.com.lett.crawlernode.processor.controller.ResultManager;
import br.com.lett.crawlernode.queue.QueueHandler;
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
	
	private static final String TASK_PATH = "/test";
	private static final int TASK_PORT = 5000;
	private static final String HOST = "localhost";

	public static ExecutionParameters 	executionParameters;
	public static ProxyCollection 		proxies;
	public static DBCredentials 		dbCredentials;
	public static DatabaseManager 		dbManager;
	public static ResultManager 		processorResultManager;
	public static QueueHandler			queueHandler;
	
	public static Markets				markets;
	
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

		// create a pool executor to be used as the http server executor
		Logging.printLogDebug(logger, "Creating executor...");
		PoolExecutor executor = new PoolExecutor(
				executionParameters.getCoreThreads(), 
				executionParameters.getNthreads(),
				0L,
				TimeUnit.SECONDS,
				new LinkedBlockingQueue<Runnable>(PoolExecutor.DEFAULT_BLOQUING_QUEUE_MAX_SIZE),
				new RejectedTaskHandler());
		
		// create the server
		try {
			HttpServer server = HttpServer.create(new InetSocketAddress(HOST, TASK_PORT), 1);
			server.createContext(TASK_PATH, new WebcrawlerServer());
			server.setExecutor(executor);
			server.start();
		} catch (IOException ex) {
			Logging.printLogError(logger, "Erro ao criar servidor.");
			CommonMethods.getStackTraceString(ex);
		}
		
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
