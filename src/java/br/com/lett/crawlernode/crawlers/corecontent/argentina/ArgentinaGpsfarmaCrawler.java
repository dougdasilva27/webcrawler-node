package br.com.lett.crawlernode.crawlers.corecontent.argentina;

import org.apache.http.impl.cookie.BasicClientCookie;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.GpsfarmaCrawler;

/**
 * Date: 28/08/2019
 * 
 * @author João Pedro
 *
 */
public class ArgentinaGpsfarmaCrawler extends GpsfarmaCrawler {

   private static final String SELLER_FULLNAME = "gpsfarma argentina - caba san nicolas";

  public ArgentinaGpsfarmaCrawler(Session session) {
    super(session);
  }


   @Override
   protected String getSellerFullName() {
      return SELLER_FULLNAME;
   }

}
