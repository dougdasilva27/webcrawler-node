package br.com.lett.crawlernode.crawlers.corecontent.chile;

import org.apache.http.impl.cookie.BasicClientCookie;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.ChileJumboCrawler;
import br.com.lett.crawlernode.util.Logging;

public class ChileJumbovinaCrawler extends ChileJumboCrawler {

  public ChileJumbovinaCrawler(Session session) {
    super(session);
  }

   public static final String CODE_LOCATE = "16";

   @Override
   protected String getCodeLocate() {
      return CODE_LOCATE;
   }
}
