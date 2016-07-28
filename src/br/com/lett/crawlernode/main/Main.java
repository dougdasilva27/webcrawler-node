package br.com.lett.crawlernode.main;

import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.MessageAttributeValue;

import br.com.lett.crawlernode.base.ExecutionParameters;
import br.com.lett.crawlernode.base.TaskFactory;

import br.com.lett.crawlernode.database.DatabaseManager;

import br.com.lett.crawlernode.fetcher.Proxies;
import br.com.lett.crawlernode.models.CrawlerSession;
import br.com.lett.crawlernode.processor.controller.ResultManager;
import br.com.lett.crawlernode.queue.QueueHandler;
import br.com.lett.crawlernode.queue.QueueService;
import br.com.lett.crawlernode.util.Logging;


/**
 * 
 * Parameters:
 * -debug : to print debug log messages on console
 * -environment [development,  production]
 * -mode [discover, normal]: yet to be defined
 * 
 * @author Samir Leão
 *
 */

public class Main {

	private static final Logger logger = LoggerFactory.getLogger(Main.class);

	public static ExecutionParameters 	executionParameters;
	public static Proxies 				proxies;
	public static DatabaseManager 		dbManager;
	public static ResultManager 		processorResultManager;
	public static AmazonSQS 			queue;
	
	private static ExecutorService 		executor;
	private static QueueHandler			queueHandler;

	public static void main(String args[]) {

		// setting execution parameters
		executionParameters = new ExecutionParameters(args);
		executionParameters.setUpExecutionParameters();

		// setting MDC for logging messages
		setLogMDC();

		// fetching proxies
		//		if (executionParameters.getEnvironment().equals(ExecutionParameters.ENVIRONMENT_PRODUCTION)) {
		//			proxies = new Proxies();
		//			proxies.fetchPremiumProxies();
		//			proxies.fetchRegularProxies();
		//		}

		// create a queue handler that will contain an Amazon SQS instance
		queueHandler = new QueueHandler();
		queue = queueHandler.getSQS();
		
//		sendTasks();

		// create an executor with a fixed number of threads
		executor = Executors.newFixedThreadPool(100);

		
		/*
		 * main task -- from time to time goes to server and takes 10 urls
		 */

		Timer mainTask = new Timer();

		mainTask.scheduleAtFixedRate(new TimerTask() {

			@Override
			public void run() {

				// request message (tasks) from the Amazon queue
				List<Message> messages = QueueService.requestMessages(queueHandler.getSQS(), 10);

				for (Message message : messages) {

					// check the message fields
					if (QueueService.checkMessage(message)) {

						// create a crawler session from the message
						CrawlerSession session = new CrawlerSession(message);

						// create the task
						Runnable task = TaskFactory.createTask(session);

						// submit the task to the executor
						if (task != null) {
							executor.execute(task);
						}
					}

				}


			} 
		} , 0, 15000); // 15 seconds

	}


	private static void setLogMDC() {
		String pid = ManagementFactory.getRuntimeMXBean().getName().replaceAll("@.*", "");
		String hostName = ManagementFactory.getRuntimeMXBean().getName().replaceAll("\\d+@", "");

		MDC.put("PID", pid);
		MDC.put("HOST_NAME", hostName);
		MDC.put("PROCESS_NAME", "java");

		if (executionParameters != null) {

			MDC.put("ENVIRONMENT", executionParameters.getEnvironment());

			if (executionParameters.getDebug()) {
				MDC.put("DEBUG_MODE", "true");
			} else {
				MDC.put("DEBUG_MODE", "false");
			}

		} else {
			Logging.printLogError(logger, "Fatal error during MDC setup: execution parameters are not ready. Please, initialize them first.");
			System.exit(0);
		}
	}
	
	private static void sendTasks() {
		Map<String, MessageAttributeValue> attrMap1 = new HashMap<String, MessageAttributeValue>();		
		Map<String, MessageAttributeValue> attrMap2 = new HashMap<String, MessageAttributeValue>();
		Map<String, MessageAttributeValue> attrMap3 = new HashMap<String, MessageAttributeValue>();
		
		attrMap1.put("city", new MessageAttributeValue().withDataType("String").withStringValue("brasil"));
		attrMap1.put("market", new MessageAttributeValue().withDataType("String").withStringValue("adias"));
		attrMap1.put("marketId", new MessageAttributeValue().withDataType("String").withStringValue("20"));
		
		attrMap2.put("city", new MessageAttributeValue().withDataType("String").withStringValue("brasil"));
		attrMap2.put("market", new MessageAttributeValue().withDataType("String").withStringValue("ambientair"));
		attrMap2.put("marketId", new MessageAttributeValue().withDataType("String").withStringValue("21"));
		
		attrMap3.put("city", new MessageAttributeValue().withDataType("String").withStringValue("brasil"));
		attrMap3.put("market", new MessageAttributeValue().withDataType("String").withStringValue("centralar"));
		attrMap3.put("marketId", new MessageAttributeValue().withDataType("String").withStringValue("24"));
		
		for (int i = 1; i < 50; i++) {
			String body = "www.adias.com.br" + i;			
			QueueService.sendMessage(queue, attrMap1, body);
		}
		for (int i = 1; i < 50; i++) {
			String body = "www.ambientair.com.br" + i;			
			QueueService.sendMessage(queue, attrMap2, body);
		}
		for (int i = 1; i < 50; i++) {
			String body = "www.centralar.com.br" + i;			
			QueueService.sendMessage(queue, attrMap3, body);
		}
		
	}

}
