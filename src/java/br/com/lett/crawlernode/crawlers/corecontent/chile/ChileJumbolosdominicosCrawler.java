package br.com.lett.crawlernode.crawlers.corecontent.chile;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.ChileJumboCrawler;

public class ChileJumbolosdominicosCrawler extends ChileJumboCrawler {

   public ChileJumbolosdominicosCrawler(Session session) {
      super(session);
   }

   public static final String CODE_LOCATE = "12";

   @Override
   protected String getCodeLocate() {
      return CODE_LOCATE;
   }
}
