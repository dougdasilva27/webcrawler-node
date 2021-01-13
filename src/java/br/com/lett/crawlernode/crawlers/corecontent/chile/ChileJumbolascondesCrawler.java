package br.com.lett.crawlernode.crawlers.corecontent.chile;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.ChileJumboCrawler;

public class ChileJumbolascondesCrawler extends ChileJumboCrawler {
   public static final String CODE_LOCATE = "11";

   public ChileJumbolascondesCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getCodeLocate() {
      return CODE_LOCATE;
   }
}
