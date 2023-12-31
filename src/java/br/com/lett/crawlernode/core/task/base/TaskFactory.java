package br.com.lett.crawlernode.core.task.base;


import br.com.lett.crawlernode.core.session.sentinel.SentinelCrawlerSession;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.session.crawler.*;
import br.com.lett.crawlernode.core.session.ranking.EqiRankingDiscoverKeywordsSession;
import br.com.lett.crawlernode.core.session.ranking.RankingDiscoverKeywordsSession;
import br.com.lett.crawlernode.core.session.ranking.RankingKeywordsSession;
import br.com.lett.crawlernode.core.session.ranking.TestRankingKeywordsSession;
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

   private TaskFactory() {
   }

   private static final Logger logger = LoggerFactory.getLogger(TaskFactory.class);

   /**
    * @param session
    */
   public static Task createTask(Session session, String className) {

      if (session instanceof InsightsCrawlerSession || session instanceof SeedCrawlerSession || session instanceof DiscoveryCrawlerSession || session instanceof EqiCrawlerSession || session instanceof ToBuyCrawlerSession || session instanceof SentinelCrawlerSession) {
         return createCrawlerTask(session, className);
      } else if (session instanceof RankingKeywordsSession || session instanceof RankingDiscoverKeywordsSession || session instanceof EqiRankingDiscoverKeywordsSession
         || session instanceof TestRankingKeywordsSession) {
         return createCrawlerTask(session, className);
      } else if (session instanceof TestCrawlerSession) {
         return createCrawlerTask(session, className);
      } else {
         return null;
      }
   }

   /**
    * Create an instance of a crawler task for the market in the session.
    * http://stackoverflow.com/questions/5658182/initializing-a-class-with-class-forname-and-which-have-a-constructor-which-tak
    *
    * @return Controller instance
    */
   private static Task createCrawlerTask(Session session, String className) {
      try {
         // instantiating a crawler task with the given session as it's constructor parameter
         Constructor<?> constructor = Class.forName(className).getConstructor(Session.class);
         return (Task) constructor.newInstance(session);
      } catch (Exception ex) {
         Logging.printLogError(logger, session, "Error instantiating task: " + className);
         Logging.printLogError(logger, session, CommonMethods.getStackTraceString(ex));
      }

      return null;
   }

}
