package br.com.lett.crawlernode.core.task.base;


import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.session.crawler.*;
import br.com.lett.crawlernode.core.session.ranking.EqiRankingDiscoverKeywordsSession;
import br.com.lett.crawlernode.core.session.ranking.RankingDiscoverKeywordsSession;
import br.com.lett.crawlernode.core.session.ranking.RankingKeywordsSession;
import br.com.lett.crawlernode.core.session.ranking.TestRankingKeywordsSession;
import br.com.lett.crawlernode.core.task.impl.ImageCrawler;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.Logging;

import java.lang.reflect.Constructor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is used to instantiate crawler tasks of an arbitrary type.
 *
 * @author Samir Leao
 */

public class TaskFactory {

   private static Logger logger = LoggerFactory.getLogger(TaskFactory.class);

   /**
    * @param session
    * @return
    */
   public static Task createTask(Session session,String className) {

      if (session instanceof InsightsCrawlerSession || session instanceof SeedCrawlerSession || session instanceof DiscoveryCrawlerSession || session instanceof EqiCrawlerSession) {
         return createCrawlerTask(session,className);
      }

      if (session instanceof ImageCrawlerSession) {
         return createImageCrawlerTask(session,className);
      }

      if (session instanceof RankingKeywordsSession || session instanceof RankingDiscoverKeywordsSession || session instanceof EqiRankingDiscoverKeywordsSession
         || session instanceof TestRankingKeywordsSession) {
         return createCrawlerRankingKeywordsTask(session,className);
      }

      if (session instanceof TestCrawlerSession) {
         if (br.com.lett.crawlernode.test.Test.testType.equals(br.com.lett.crawlernode.test.Test.INSIGHTS_TEST)) {
            return createCrawlerTask(session,className);
         }
      }

      return null;
   }

   /**
    * Create an instance of a crawler task for the market in the session.
    * http://stackoverflow.com/questions/5658182/initializing-a-class-with-class-forname-and-which-have-a-constructor-which-tak
    *
    * @return Controller instance
    */
   private static Task createCrawlerTask(Session session,String className) {

      // assemble the class name
      String taskClassName = className;

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

   private static Task createImageCrawlerTask(Session session,String className) {
      return new ImageCrawler(session);
   }

   private static Task createCrawlerRankingKeywordsTask(Session session,String className) {

      // assemble the class name
      String taskClassName = className;

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

}
