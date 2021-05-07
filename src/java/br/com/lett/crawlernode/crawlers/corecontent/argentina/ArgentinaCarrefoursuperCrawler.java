package br.com.lett.crawlernode.crawlers.corecontent.argentina;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.ArgentinaCarrefoursuper;

/**
 * Date: 2019-07-12
 *
 * @author gabriel
 */
public class ArgentinaCarrefoursuperCrawler extends ArgentinaCarrefoursuper {

   public ArgentinaCarrefoursuperCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getHomePage() {
      return "https://supermercado.carrefour.com.ar/";
   }

   @Override
   protected String getLocationToken() {
      return "eyJjYW1wYWlnbnMiOm51bGwsImNoYW5uZWwiOiIxIiwicHJpY2VUYWJsZXMiOm51bGwsInJlZ2lvbklkIjoiVTFjalkyRnljbVZtYjNWeVlYSXdNREF5TzJOaGN" +
         "uSmxabTkxY21GeU1EZzVPUT09IiwidXRtX2NhbXBhaWduIjpudWxsLCJ1dG1fc291cmNlIjpudWxsLCJ1dG1pX2NhbXBhaWduIjpudWxsLCJjdXJyZW5jeUNvZGUiOi" +
         "JBUlMiLCJjdXJyZW5jeVN5bWJvbCI6IiQiLCJjb3VudHJ5Q29kZSI6IkFSRyIsImN1bHR1cmVJbmZvIjoiZXMtQVIiLCJjaGFubmVsUHJpdmFjeSI6InB1YmxpYyJ9";
   }
}
