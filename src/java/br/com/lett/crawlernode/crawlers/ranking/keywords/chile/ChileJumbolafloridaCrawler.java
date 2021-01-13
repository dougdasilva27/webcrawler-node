package br.com.lett.crawlernode.crawlers.ranking.keywords.chile;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.ChileJumboCrawler;

public class ChileJumbolafloridaCrawler extends ChileJumboCrawler {
   public static final String CODE_LOCATE = br.com.lett.crawlernode.crawlers.corecontent.chile.ChileJumbolafloridaCrawler.CODE_LOCATE;

   public ChileJumbolafloridaCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getStoreCode() {
      return CODE_LOCATE;
   }
}
