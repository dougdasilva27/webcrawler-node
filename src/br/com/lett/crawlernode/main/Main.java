package br.com.lett.crawlernode.main;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import br.com.lett.crawlernode.base.Crawler;
import br.com.lett.crawlernode.base.ExecutionParameters;
import br.com.lett.crawlernode.crawlers.BrasilAdiasCrawler;
import br.com.lett.crawlernode.crawlers.BrasilAmericanasCrawler;
import br.com.lett.crawlernode.crawlers.BrasilXulambisCrawler;
import br.com.lett.crawlernode.fetcher.Proxies;

/**
 * 
 * @author Samir Leão
 *
 */

public class Main {

	public static ExecutionParameters 	executionParameters;
	public static Proxies 				proxies;
	public static ExecutorService 		executor;
	public static Timer mainTask = new Timer();

	public static void main(String args[]) {

		// setting execution parameters
		executionParameters = new ExecutionParameters(args);
		executionParameters.setUpExecutionParameters();

		// fetching proxies
		//		proxies = new Proxies();
		//		proxies.fetchPremiumProxies();
		//		proxies.fetchRegularProxies();

		// creating the executor service with a limited size thread pool
		executor = Executors.newFixedThreadPool(2);

		/*
		 * main task -- from time to time goes to server and takes 10 urls
		 */
		mainTask.scheduleAtFixedRate(new TimerTask() {

			@Override
			public void run() {

				System.out.println("\nIndo lá pegar 10 tarefas...");

				// creating tasks
				ArrayList<String> tasks = createTasks(10);

				if (!executor.isShutdown()) {

					// executing each task
					System.out.println("mandando as 10 tarefas para o executor...");
					for (String url : tasks) {
						Runnable task = null;

						if (url.contains("adias")) task = new BrasilAdiasCrawler(url);
						else if(url.contains("americanas")) task = new BrasilAmericanasCrawler(url);
						else if(url.contains("xulambis")) task = new BrasilXulambisCrawler(url);

						executor.execute(task);
					}	
				} else {
					System.out.println("Executor foi desligado.");
				}
			}

		}, 0, 10000); // 10 seconds



	}

	/**
	 * Test function to create n tasks
	 * 
	 * @return ArrayList with tasks (URLs)
	 */
	private static ArrayList<String> createTasks(int n) {
		ArrayList<String> tasks = new ArrayList<String>();
		for (int i = 0; i < n; i++) {
			if (i >= 0 && i <= 3) tasks.add("http://www.americanas.com.br/");
			else if (i >= 4 && i <= 7) tasks.add("http://www.adias.com.br/");
			else tasks.add("http://www.xulambisworks.com.br");
		} 

		return tasks;
	}

}
