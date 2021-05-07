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

   @Override
   protected String getLocationToken() {
      return "eyJjYW1wYWlnbnMiOm51bGwsImNoYW5uZWwiOiIxIiwicHJpY2VUYWJsZXMiOm51bGwsInJlZ2lvbklkIjoiVTFjalkyRnljbVZtYjNWeVlYSXdNREkxTzJOaG" +
         "NuSmxabTkxY21GeU1EZzVPUT09IiwidXRtX2NhbXBhaWduIjpudWxsLCJ1dG1fc291cmNlIjpudWxsLCJ1dG1pX2NhbXBhaWduIjpudWxsLCJjdXJyZW5jeUNvZGUi" +
         "OiJBUlMiLCJjdXJyZW5jeVN5bWJvbCI6IiQiLCJjb3VudHJ5Q29kZSI6IkFSRyIsImN1bHR1cmVJbmZvIjoiZXMtQVIiLCJhZG1pbl9jdWx0dXJlSW5mbyI6ImVzLU" +
         "FSIiwiY2hhbm5lbFByaXZhY3kiOiJwdWJsaWMifQ";
   }

}
