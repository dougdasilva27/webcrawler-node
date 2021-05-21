package br.com.lett.crawlernode.crawlers.corecontent.argentina;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.ArgentinaCarrefoursuper;

public class ArgentinaCarrefoursupervicentelopezCrawler extends ArgentinaCarrefoursuper {

   public ArgentinaCarrefoursupervicentelopezCrawler(Session session) {
      super(session);
   }

   public static final String TOKEN = "eyJjYW1wYWlnbnMiOm51bGwsImNoYW5uZWwiOiIxIiwicHJpY2VUYWJsZXMi" +
      "Om51bGwsInJlZ2lvbklkIjoiVTFjalkyRnljbVZtYjNWeVlYSXdNREF5TzJOaGNuSmxabTkxY21GeU1EZzVPUT09Iiwi" +
      "dXRtX2NhbXBhaWduIjpudWxsLCJ1dG1fc291cmNlIjpudWxsLCJ1dG1pX2NhbXBhaWduIjpudWxsLCJjdXJyZW5jeUNv" +
      "ZGUiOiJBUlMiLCJjdXJyZW5jeVN5bWJvbCI6IiQiLCJjb3VudHJ5Q29kZSI6IkFSRyIsImN1bHR1cmVJbmZvIjoiZXMt" +
      "QVIiLCJhZG1pbl9jdWx0dXJlSW5mbyI6ImVzLUFSIiwiY2hhbm5lbFByaXZhY3kiOiJwdWJsaWMifQ";

   /**
    * Address: Av. del Libertador 215, B1638 Vicente López, Provincia de Buenos Aires, Argentina
    *
    * @return token for specified address.
    */
   @Override
   protected String getLocationToken() {
      return TOKEN;
   }

}
