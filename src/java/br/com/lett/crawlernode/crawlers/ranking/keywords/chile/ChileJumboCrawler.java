package br.com.lett.crawlernode.crawlers.ranking.keywords.chile;

import br.com.lett.crawlernode.core.session.Session;

public class ChileJumboCrawler extends br.com.lett.crawlernode.crawlers.ranking.keywords.extractionutils.ChileJumboCrawler {

   public ChileJumboCrawler(Session session) {
      super(session);
      super.storeCode = br.com.lett.crawlernode.crawlers.corecontent.extractionutils.ChileJumboCrawler.JUMBO_DEHESA_ID;
   }
}
