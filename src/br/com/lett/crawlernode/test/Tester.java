package br.com.lett.crawlernode.test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.com.lett.crawlernode.database.DBCredentials;
import br.com.lett.crawlernode.database.DatabaseManager;
import br.com.lett.crawlernode.kernel.ExecutionParameters;
import br.com.lett.crawlernode.kernel.fetcher.Proxies;
import br.com.lett.crawlernode.kernel.task.TaskExecutor;
import br.com.lett.crawlernode.kernel.task.TaskExecutorAgent;
import br.com.lett.crawlernode.main.Main;
import br.com.lett.crawlernode.processor.controller.ResultManager;

/**
 * Class used to test crawlers logic with any desired number of
 * URLs of any desired market.
 * @author Samir Leao
 *
 */
public class Tester {
	
	private static final Logger logger = LoggerFactory.getLogger(Main.class);

	public static ExecutionParameters 	executionParameters;
	public static Proxies 				proxies;
	public static DBCredentials 		dbCredentials;
	public static DatabaseManager 		dbManager;
	public static ResultManager 		processorResultManager;

	private static TaskExecutor 		taskExecutor;
	private static TaskExecutorAgent 	taskExecutorAgent;
	
	public static void main(String args[]) {
		
	}
	
	private static void test(String url, String marketName, String city) {
		
	}

}
