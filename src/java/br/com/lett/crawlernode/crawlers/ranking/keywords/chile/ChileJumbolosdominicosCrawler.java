package br.com.lett.crawlernode.crawlers.ranking.keywords.chile;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.ChileJumboCrawler;

public class ChileJumbolosdominicosCrawler extends ChileJumboCrawler {

   public static final String CODE_LOCATE = br.com.lett.crawlernode.crawlers.corecontent.chile.ChileJumbolosdominicosCrawler.CODE_LOCATE;


   public ChileJumbolosdominicosCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getStoreCode() {
      return CODE_LOCATE;
   }
}
