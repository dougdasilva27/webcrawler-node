package br.com.lett.crawlernode.crawlers.corecontent.argentina;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.ArgentinaCarrefoursuper;

/**
 * Date: 2019-07-12
 * 
 * @author gabriel
 *
 */
public class ArgentinaCarrefoursuperrincondemilbergCrawler extends ArgentinaCarrefoursuper {

   public ArgentinaCarrefoursuperrincondemilbergCrawler(Session session) {
      super(session);
   }

   public static final String TOKEN = "eyJjYW1wYWlnbnMiOm51bGwsImNoYW5uZWwiOiIxIiwicHJpY2VUYWJsZXMiOm51bGwsInJlZ2lvbklkI" +
      "joiVTFjalkyRnljbVZtYjNWeVlYSXdNREEyTzJOaGNuSmxabTkxY21GeU1EZzVPUT09IiwidXRtX2NhbXBhaW" +
      "duIjpudWxsLCJ1dG1fc291cmNlIjpudWxsLCJ1dG1pX2NhbXBhaWduIjpudWxsLCJjdXJyZW5jeUNvZGUiOiJ" +
      "BUlMiLCJjdXJyZW5jeVN5bWJvbCI6IiQiLCJjb3VudHJ5Q29kZSI6IkFSRyIsImN1bHR1cmVJbmZvIjoiZXMt" +
      "QVIiLCJhZG1pbl9jdWx0dXJlSW5mbyI6ImVzLUFSIiwiY2hhbm5lbFByaXZhY3kiOiJwdWJsaWMifQ";

   /**
    * Address: Avenida Pueyrredón, 1428, Recoleta - Capital Federal 1118, Argentina
    * @return token for specified address.
    */
   @Override
   protected String getLocationToken() {
      return TOKEN;
   }

}
