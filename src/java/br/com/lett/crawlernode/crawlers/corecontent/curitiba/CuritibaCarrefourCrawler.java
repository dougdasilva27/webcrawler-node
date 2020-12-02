package br.com.lett.crawlernode.crawlers.corecontent.curitiba;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.CarrefourCrawler;

/**
 * 18/04/2020
 * 
 * @author Fabrício
 *
 */
public class CuritibaCarrefourCrawler extends CarrefourCrawler {

   public static final String HOME_PAGE = "https://mercado.carrefour.com.br/";
   public static final String LOCATION = "80220-000";
   public static final String LOCATION_TOKEN = "eyJjYW1wYWlnbnMiOm51bGwsImNoYW5uZWwiOiIyIiwicHJpY2VUYWJsZXMiOm51bGwsInJlZ2lvbklkIjoiVTFjalkyRnljbVZtYjNWeVluSTVOalU9IiwidXRtX2NhbXBhaWduIjpudWxsLCJ1dG1fc291cmNlIjpudWxsLCJ1dG1pX2NhbXBhaWduIjpudWxsLCJjdXJyZW5jeUNvZGUiOiJCUkwiLCJjdXJyZW5jeVN5bWJvbCI6IlIkIiwiY291bnRyeUNvZGUiOiJCUkEiLCJjdWx0dXJlSW5mbyI6InB0LUJSIiwiY2hhbm5lbFByaXZhY3kiOiJwdWJsaWMifQ";

   public CuritibaCarrefourCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getLocationToken() {
      return LOCATION_TOKEN;
   }

   @Override
   protected String getHomePage() {
      return HOME_PAGE;
   }
}
