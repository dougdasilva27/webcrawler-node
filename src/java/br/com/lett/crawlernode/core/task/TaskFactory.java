package br.com.lett.crawlernode.core.task;

import java.lang.reflect.Constructor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.com.lett.crawlernode.core.models.Market;
import br.com.lett.crawlernode.core.session.CrawlerSession;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.Logging;

/**
 * This class is used to instantiate crawler tasks of an arbitrary type.
 * @author Samir Leao
 *
 */

public class TaskFactory {
	
	private static Logger logger = LoggerFactory.getLogger(TaskFactory.class);
	
	/**
	 * Create an instance of a crawler task for the market in the session.
	 * http://stackoverflow.com/questions/5658182/initializing-a-class-with-class-forname-and-which-have-a-constructor-which-tak
	 * @param controllerClassName The name of the controller class
	 * @return Controller instance
	 */
	public static Runnable createTask(CrawlerSession session) {
		Logging.printLogDebug(logger, session, "Creating task for " + session.getUrl());
		
		// assembling the class name
		String taskClassName = assembleClassName(session.getMarket());
		
		try {
			
			// instantiating a crawler task with the given session as it's constructor parameter
			Constructor<?> constructor = Class.forName(taskClassName).getConstructor(CrawlerSession.class);
			Runnable task = (Runnable) constructor.newInstance(session);

			return task;
		} catch (Exception ex) {
			Logging.printLogError(logger, session, "Error instantiating task: " + taskClassName);
			Logging.printLogError(logger, session, CommonMethods.getStackTraceString(ex));
		}

		return null;
	}
	
	
	/**
	 * Assemble the name of a task class.
	 * @param the market for which we will run the task
	 * @param name The name of the market
	 * @return The name of the task class
	 */
	private static String assembleClassName(Market market) {
		String city = market.getCity();
		String name = market.getName();
		
		StringBuilder sb = new StringBuilder();
		sb.append("br.com.lett.crawlernode.crawlers." + city + ".");
		sb.append(city.substring(0, 1).toUpperCase());
		sb.append(city.substring(1).toLowerCase());
		sb.append(name.substring(0, 1).toUpperCase());
		sb.append(name.substring(1).toLowerCase());
		sb.append("Crawler");

		return sb.toString();
	}

}
