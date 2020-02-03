package br.com.lett.crawlernode.core.task.base;

import br.com.lett.crawlernode.core.models.Market;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.session.crawler.*;
import br.com.lett.crawlernode.core.session.ranking.*;
import br.com.lett.crawlernode.core.task.impl.ImageCrawler;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.Logging;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;

/**
 * This class is used to instantiate crawler tasks of an arbitrary type.
 * 
 * @author Samir Leao
 *
 */

public class TaskFactory {

  private static Logger logger = LoggerFactory.getLogger(TaskFactory.class);

  /**
   * 
   * @param session
   * @return
   */
  public static Task createTask(Session session) {
    if (session instanceof InsightsCrawlerSession || session instanceof SeedCrawlerSession || session instanceof DiscoveryCrawlerSession) {

      return createCrawlerTask(session);
    }

    if (session instanceof RatingReviewsCrawlerSession) {
      return createRateReviewCrawlerTask(session);
    }

    if (session instanceof ImageCrawlerSession) {
      return createImageCrawlerTask(session);
    }

    if (session instanceof RankingKeywordsSession || session instanceof RankingDiscoverKeywordsSession
        || session instanceof TestRankingKeywordsSession) {
      return createCrawlerRankingKeywordsTask(session);
    }

    if (session instanceof RankingCategoriesSession || session instanceof RankingDiscoverCategoriesSession
        || session instanceof TestRankingCategoriesSession) {
      return createCrawlerRankingCategoriesTask(session);
    }

    if (session instanceof TestCrawlerSession) {
      if (br.com.lett.crawlernode.test.Test.testType.equals(br.com.lett.crawlernode.test.Test.INSIGHTS_TEST)) {
        return createCrawlerTask(session);
      } else if (br.com.lett.crawlernode.test.Test.testType.equals(br.com.lett.crawlernode.test.Test.RATING_TEST)) {
        return createRateReviewCrawlerTask(session);
      }
    }

    return null;
  }

  /**
   * Create an instance of a crawler task for the market in the session.
   * http://stackoverflow.com/questions/5658182/initializing-a-class-with-class-forname-and-which-have-a-constructor-which-tak
   * 
   * @param controllerClassName The name of the controller class
   * @return Controller instance
   */
  private static Task createCrawlerTask(Session session) {

    // assemble the class name
    String taskClassName = assembleCrawlerClassName(session.getMarket());

    try {
      // instantiating a crawler task with the given session as it's constructor parameter
      Constructor<?> constructor = Class.forName(taskClassName).getConstructor(Session.class);

      return (Task) constructor.newInstance(session);
    } catch (Exception ex) {
      Logging.printLogError(logger, session, "Error instantiating task: " + taskClassName);
      Logging.printLogError(logger, session, CommonMethods.getStackTraceString(ex));
    }

    return null;
  }

  private static Task createRateReviewCrawlerTask(Session session) {

    // assemble the class name
    String taskClassName = assembleRateReviewCrawlerClassName(session.getMarket());

    try {

      // instantiating a crawler task with the given session as it's constructor parameter
      Constructor<?> constructor = Class.forName(taskClassName).getConstructor(Session.class);
      return (Task) constructor.newInstance(session);
    } catch (Exception ex) {
      Logging.printLogError(logger, session, "Error instantiating task: " + taskClassName);
      Logging.printLogError(logger, session, CommonMethods.getStackTraceString(ex));
    }

    return null;
  }

  private static Task createImageCrawlerTask(Session session) {
    return new ImageCrawler(session);
  }

  private static Task createCrawlerRankingKeywordsTask(Session session) {

    // assemble the class name
    String taskClassName = assembleRankingClassName(session.getMarket(), "keywords");

    try {

      // instantiating a crawler task with the given session as it's constructor parameter
      Constructor<?> constructor = Class.forName(taskClassName).getConstructor(Session.class);
      return (Task) constructor.newInstance(session);
    } catch (Exception ex) {
      Logging.printLogError(logger, session, "Error instantiating task: " + taskClassName);
      Logging.printLogError(logger, session, CommonMethods.getStackTraceString(ex));
    }

    return null;
  }

  private static Task createCrawlerRankingCategoriesTask(Session session) {

    // assemble the class name
    String taskClassName = assembleRankingClassName(session.getMarket(), "categories");

    try {

      // instantiating a crawler task with the given session as it's constructor parameter
      Constructor<?> constructor = Class.forName(taskClassName).getConstructor(Session.class);
      return (Task) constructor.newInstance(session);
    } catch (Exception ex) {
      Logging.printLogError(logger, session, "Error instantiating task: " + taskClassName);
      Logging.printLogError(logger, session, CommonMethods.getStackTraceString(ex));
    }

    return null;
  }

  /**
   * Assemble the name of a task class.
   * 
   * @param the market for which we will run the task
   * @param name The name of the market
   * @return The name of the task class
   */
  private static String assembleCrawlerClassName(Market market) {
    String city = market.getCity();
    String name = market.getName();

    StringBuilder sb = new StringBuilder();
    sb.append("br.com.lett.crawlernode.crawlers.corecontent." + city + ".");
    sb.append(city.substring(0, 1).toUpperCase());
    sb.append(city.substring(1).toLowerCase());
    sb.append(name.substring(0, 1).toUpperCase());
    sb.append(name.substring(1).toLowerCase());
    sb.append("Crawler");

    return sb.toString();
  }

  /**
   * Assemble the name of a task class.
   * 
   * @param the market for which we will run the task
   * @param name The name of the market
   * @return The name of the task class
   */
  private static String assembleRateReviewCrawlerClassName(Market market) {
    String city = market.getCity();
    String name = market.getName();

    StringBuilder sb = new StringBuilder();
    sb.append("br.com.lett.crawlernode.crawlers.ratingandreviews." + city + ".");
    sb.append(city.substring(0, 1).toUpperCase());
    sb.append(city.substring(1).toLowerCase());
    sb.append(name.substring(0, 1).toUpperCase());
    sb.append(name.substring(1).toLowerCase());
    sb.append("RatingReviewCrawler");

    return sb.toString();
  }

  /**
   * Assemble the name of a CrawlerRanking class.
   * 
   * @param city The city corresponding to this controller
   * @param name The name of the market
   * @return The name of the crawler class
   */
  private static String assembleRankingClassName(Market market, String rankType) {
    String crawlerClassName = "br.com.lett.crawlernode.crawlers.ranking." + rankType + "." + market.getCity() + ".";
    crawlerClassName += market.getCity().substring(0, 1).toUpperCase();
    crawlerClassName += market.getCity().substring(1);
    crawlerClassName += market.getName().substring(0, 1).toUpperCase();
    crawlerClassName += market.getName().substring(1);
    crawlerClassName += "Crawler";
    return crawlerClassName;
  }


}
