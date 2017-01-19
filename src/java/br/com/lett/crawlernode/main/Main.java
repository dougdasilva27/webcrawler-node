package br.com.lett.crawlernode.main;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.net.httpserver.HttpServer;

import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.models.Markets;
import br.com.lett.crawlernode.core.server.PoolExecutor;
import br.com.lett.crawlernode.core.server.ServerConstants;
import br.com.lett.crawlernode.core.server.ServerHandler;
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
	
	private static final int SERVER_PORT = 5000;
	private static final String SERVER_HOST = "localhost";

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
		
//		try {
//			Document ipify = Jsoup.connect("http://api.ipify.org/").get();
//			Logging.printLogDebug(logger, "Machine IP: " + ipify.select("body").text());
//		} catch (IOException e1) {
//			Logging.printLogError(logger, "Error during connection with api.ipify.org");
//		}		

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
			Logging.printLogError(logger, "error in resource URL.");
			Logging.printLogError(logger, CommonMethods.getStackTraceString(e));
		} catch (IOException e) {
			Logging.printLogError(logger, "error during resource download.");
			Logging.printLogError(logger, CommonMethods.getStackTraceString(e));
		}

		// create a queue handler that will contain an Amazon SQS instance
		queueHandler = new QueueHandler();

		// create a pool executor to be used as the http server executor
		Logging.printLogDebug(logger, "creating executor....");
		PoolExecutor executor = (PoolExecutor)createExecutor();
		Logging.printLogDebug(logger, "done.");
		Logging.printLogDebug(logger, executor.toString());
		
		// create the server
		Logging.printLogDebug(logger, "creating server [" + SERVER_HOST + "][" + SERVER_PORT + "]....");
		initServer(executor);
		Logging.printLogDebug(logger, "done.");
		
	}
	
	private static Executor createExecutor() {
		return new PoolExecutor(
				executionParameters.getCoreThreads(), 
				executionParameters.getCoreThreads(),
				0L,
				TimeUnit.SECONDS,
				new LinkedBlockingQueue<Runnable>(PoolExecutor.DEFAULT_BLOQUING_QUEUE_MAX_SIZE),
				new RejectedTaskHandler());
	}
	
	private static void initServer(Executor executor) {
		try {
			HttpServer server = HttpServer.create(new InetSocketAddress(SERVER_HOST, SERVER_PORT), 0);
			ServerHandler serverHandler = new ServerHandler();
			
			server.createContext(ServerConstants.ENDPOINT_TASK, serverHandler);
			server.createContext(ServerConstants.ENDPOINT_HEALTH_CHECK, serverHandler);
			
			server.setExecutor(executor);
			server.start();
			
		} catch (IOException ex) {
			Logging.printLogError(logger, "error creating server.");
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
