package br.com.lett.crawlernode.main;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.MessageAttributeValue;
import com.amazonaws.services.sqs.model.SendMessageBatchRequestEntry;

import br.com.lett.crawlernode.base.ExecutionParameters;
import br.com.lett.crawlernode.base.TaskExecutor;
import br.com.lett.crawlernode.base.WorkList;
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
	public static DatabaseManager 		dbManager;
	public static ResultManager 		processorResultManager;
	public static AmazonSQS 			queue;
	
	private static TaskExecutor 		taskExecutor;
	private static QueueHandler			queueHandler;
	private static WorkList				workList;

	public static void main(String args[]) {

		// setting execution parameters
		executionParameters = new ExecutionParameters(args);
		executionParameters.setUpExecutionParameters();

		// setting MDC for logging messages
		Logging.setLogMDC(executionParameters);

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
		} , 0, 15000); // 15 seconds

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
		
		List<SendMessageBatchRequestEntry> entries = new ArrayList<SendMessageBatchRequestEntry>();
		
		for (int i = 1; i <= 500; i++) {
			String body = "www.adias.com.br" + i;
			
			SendMessageBatchRequestEntry entry = new SendMessageBatchRequestEntry();
			entry.setId(String.valueOf(i));	// the id must be unique in the batch
			entry.setMessageAttributes(attrMap1);
			entry.setMessageBody(body);
			
			entries.add(entry);
			
			if (entries.size() == 10) {
				Logging.printLogDebug(logger, "Enviando batch de " + entries.size() + " mensagens...");
				QueueService.sendBatchMessages(queue, entries);
				entries.clear();
			}
		}
		if (entries.size() > 0) { // the left over
			Logging.printLogDebug(logger, "Enviando batch de " + entries.size() + " mensagens...");
			QueueService.sendBatchMessages(queue, entries);
			entries.clear();
		}
		
		for (int i = 1; i <= 500; i++) {
			String body = "www.ambientair.com.br" + i;
			
			SendMessageBatchRequestEntry entry = new SendMessageBatchRequestEntry();
			entry.setId(String.valueOf(i));	// the id must be unique in the batch
			entry.setMessageAttributes(attrMap2);
			entry.setMessageBody(body);
			
			entries.add(entry);
			
			if (entries.size() == 10) {
				Logging.printLogDebug(logger, "Enviando batch de " + entries.size() + " mensagens...");
				QueueService.sendBatchMessages(queue, entries);
				entries.clear();
			}	
		}
		if (entries.size() > 0) { // the left over
			Logging.printLogDebug(logger, "Enviando batch de " + entries.size() + " mensagens...");
			QueueService.sendBatchMessages(queue, entries);
			entries.clear();
		}
		
		for (int i = 1; i <= 500; i++) {
			String body = "www.centralar.com.br" + i;
			
			SendMessageBatchRequestEntry entry = new SendMessageBatchRequestEntry();
			entry.setId(String.valueOf(i));	// the id must be unique in the batch
			entry.setMessageAttributes(attrMap3);
			entry.setMessageBody(body);
			
			entries.add(entry);
			
			if (entries.size() == 10) {
				Logging.printLogDebug(logger, "Enviando batch de " + entries.size() + " mensagens...");
				QueueService.sendBatchMessages(queue, entries);
				entries.clear();
			}
		}
		if (entries.size() > 0) { // the left over
			Logging.printLogDebug(logger, "Enviando batch de " + entries.size() + " mensagens...");
			QueueService.sendBatchMessages(queue, entries);
			entries.clear();
		}
		
	}

}
