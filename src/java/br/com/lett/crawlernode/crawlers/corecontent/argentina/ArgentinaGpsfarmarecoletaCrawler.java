package br.com.lett.crawlernode.crawlers.corecontent.argentina;

import org.apache.http.impl.cookie.BasicClientCookie;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.GpsfarmaCrawler;

public class ArgentinaGpsfarmarecoletaCrawler extends GpsfarmaCrawler {

   private static String SELLER_FULLNAME = "gpsfarma argentina - caba recoleta";

  public ArgentinaGpsfarmarecoletaCrawler(Session session) {
    super(session);
  }

   @Override
   protected String getSellerFullName() {
      return SELLER_FULLNAME;
   }
}
