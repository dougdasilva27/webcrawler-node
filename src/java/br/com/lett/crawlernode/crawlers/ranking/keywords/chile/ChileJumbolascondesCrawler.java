package br.com.lett.crawlernode.crawlers.ranking.keywords.chile;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.ChileJumboCrawler;

public class ChileJumbolascondesCrawler extends ChileJumboCrawler {

   public ChileJumbolascondesCrawler(Session session) {
      super(session);
   }

   public static final String CODE_LOCATE = br.com.lett.crawlernode.crawlers.corecontent.chile.ChileJumbolascondesCrawler.CODE_LOCATE;

   @Override
   protected String getStoreCode() {
      return CODE_LOCATE;
   }

}
