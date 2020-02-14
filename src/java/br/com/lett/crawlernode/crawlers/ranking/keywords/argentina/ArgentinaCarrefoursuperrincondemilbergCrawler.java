package br.com.lett.crawlernode.crawlers.ranking.keywords.argentina;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.session.crawler.TestCrawlerSession;
import br.com.lett.crawlernode.core.session.ranking.RankingKeywordsSession;
import br.com.lett.crawlernode.crawlers.ranking.keywords.extractionutils.ArgentinaCarrefoursuper;

public class ArgentinaCarrefoursuperrincondemilbergCrawler extends ArgentinaCarrefoursuper {

   public ArgentinaCarrefoursuperrincondemilbergCrawler(Session session) {
      super(session);
   }

   private static final String DEFAULT_CEP = br.com.lett.crawlernode.crawlers.corecontent.argentina.ArgentinaCarrefoursuperrincondemilbergCrawler.CEP;
   private static final String RANKKING_CEP = "1646";

   @Override
   protected String getCep() {
      // This happen because has a business rule that on share of search we need to scrap
      // information for a specific location, but for discover products we need to use the
      // default location of core capture.
      return session instanceof RankingKeywordsSession || session instanceof TestCrawlerSession
            ? RANKKING_CEP
            : DEFAULT_CEP;
   }
}
