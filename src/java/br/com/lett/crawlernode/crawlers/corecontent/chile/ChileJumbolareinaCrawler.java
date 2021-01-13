package br.com.lett.crawlernode.crawlers.corecontent.chile;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.ChileJumboCrawler;

public class ChileJumbolareinaCrawler extends ChileJumboCrawler {

  public ChileJumbolareinaCrawler(Session session) {
    super(session);
  }

   public static final String CODE_LOCATE = "11";

   @Override
   protected String getCodeLocate() {
      return CODE_LOCATE;
   }
}
