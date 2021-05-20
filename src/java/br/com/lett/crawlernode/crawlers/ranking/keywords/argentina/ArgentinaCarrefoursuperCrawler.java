package br.com.lett.crawlernode.crawlers.ranking.keywords.argentina;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.ArgentinaCarrefoursuper;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.CarrefourCrawler;

public class ArgentinaCarrefoursuperCrawler extends CarrefourCrawler {

   public ArgentinaCarrefoursuperCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getHomePage() {
      return "https://supermercado.carrefour.com.ar/";
   }

   @Override
   protected String getLocation() {
      return "eyJjYW1wYWlnbnMiOm51bGwsImNoYW5uZWwiOiIxIiwicHJpY2VUYWJsZXMiOm51bGwsInJlZ2lvbklkIjoiVTFjalkyRnljbVZtYjNWeVlYSXdNREF5TzJOaGN" +
         "uSmxabTkxY21GeU1EZzVPUT09IiwidXRtX2NhbXBhaWduIjpudWxsLCJ1dG1fc291cmNlIjpudWxsLCJ1dG1pX2NhbXBhaWduIjpudWxsLCJjdXJyZW5jeUNvZGUiOi" +
         "JBUlMiLCJjdXJyZW5jeVN5bWJvbCI6IiQiLCJjb3VudHJ5Q29kZSI6IkFSRyIsImN1bHR1cmVJbmZvIjoiZXMtQVIiLCJjaGFubmVsUHJpdmFjeSI6InB1YmxpYyJ9";
   }
}
