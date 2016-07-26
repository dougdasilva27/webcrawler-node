package br.com.lett.crawlernode.main;

import java.lang.management.ManagementFactory;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import br.com.lett.crawlernode.base.Crawler;
import br.com.lett.crawlernode.base.ExecutionParameters;
import br.com.lett.crawlernode.crawlers.brasil.BrasilAdiasCrawler;
import br.com.lett.crawlernode.crawlers.brasil.BrasilAmericanasCrawler;
import br.com.lett.crawlernode.fetcher.Proxies;
import br.com.lett.crawlernode.models.CrawlerSession;
import br.com.lett.crawlernode.util.Logging;

/**
 * 
 * @author Samir Leão
 *
 */

public class Main {
	
	private static final Logger logger = LoggerFactory.getLogger(Main.class);

	public static ExecutionParameters 	executionParameters;
	public static Proxies 				proxies;
	public static ExecutorService 		executor;
	public static Timer mainTask = new Timer();

	public static void main(String args[]) {

		// setting execution parameters
		executionParameters = new ExecutionParameters(args);
		executionParameters.setUpExecutionParameters();
		
		// setting MDC for logging messages
		//setLogMDC();

		// fetching proxies
		if (executionParameters.getEnvironment().equals(ExecutionParameters.ENVIRONMENT_PRODUCTION)) {
			proxies = new Proxies();
			proxies.fetchPremiumProxies();
			proxies.fetchRegularProxies();
		}

		// creating the executor service with a limited size thread pool
		executor = Executors.newFixedThreadPool(2);

		/*
		 * main task -- from time to time goes to server and takes 10 urls
		 */
		//		mainTask.scheduleAtFixedRate(new TimerTask() {
		//
		//			@Override
		//			public void run() {

		// creating tasks
		ArrayList<String> tasks = createTasks();

		if (!executor.isShutdown()) {

			// executing each task
			for (String url : tasks) {
				Runnable task = null;

				CrawlerSession session = new CrawlerSession();
				session.setUrl(url);

				if (session.getUrl().contains("adias")) task = new BrasilAdiasCrawler(session);
				else if (session.getUrl().contains("americanas")) task = new BrasilAmericanasCrawler(session);

				executor.execute(task);
			}	
		} else {
			System.out.println("Executor foi desligado.");
		}
		//			}
		//
		//		}, 0, 10000); // 10 seconds



	}

	/**
	 * Test function to create n tasks
	 * 
	 * @return ArrayList with tasks (URLs)
	 */
	private static ArrayList<String> createTasks() {
		ArrayList<String> tasks = new ArrayList<String>();

		// adding 2 for Adias
		tasks.add("http://www.adias.com.br/produto/ar-condicionado-split-12000-btus-philco-220v-frio-ph12000fm5-68338");
		tasks.add("http://www.adias.com.br/produto/ar-condicionado-split-inverter-18000-btus-fujitsu-220v-quente-e-frio-asba18lec-68273");

		// adding 2 for Americanas
		tasks.add("http://www.americanas.com.br/produto/124797411/console-xbox-one-1tb-game-halo-5-guardians-via-download-headset-com-fio-controle-wireless?chave=HM_DT13");
		tasks.add("http://www.americanas.com.br/produto/128366870/smartphone-lenovo-vibe-c2-dual-chip-android-6.0-tela-5-16gb-4g-camera-8mp-preto?chave=pm_hm_bg_0_0_acom_lenovovibe_2507");

		return tasks;
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

}
