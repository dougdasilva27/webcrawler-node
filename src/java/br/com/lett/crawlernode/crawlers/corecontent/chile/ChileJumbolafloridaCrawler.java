package br.com.lett.crawlernode.crawlers.corecontent.chile;

import org.apache.http.impl.cookie.BasicClientCookie;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.ChileJumboCrawler;
import br.com.lett.crawlernode.util.Logging;

public class ChileJumbolafloridaCrawler extends ChileJumboCrawler {

  public ChileJumbolafloridaCrawler(Session session) {
    super(session);
  }

   public static final String CODE_LOCATE = "18";

   @Override
   protected String getCodeLocate() {
      return CODE_LOCATE;
   }
}
