package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.CarrefourCrawler;

public class BrasilCarrefourvilaregfeijoCrawler extends CarrefourCrawler {

   public BrasilCarrefourvilaregfeijoCrawler(Session session) {
      super(session);
   }


   public static final String HOME_PAGE = "https://mercado.carrefour.com.br/";
   public static final String LOCATION = "03342-900";
   public static final String LOCATION_TOKEN = "eyJjYW1wYWlnbnMiOm51bGwsImNoYW5uZWwiOiIyIiwicHJpY2VUYWJsZXMiOm51bGwsInJlZ2lvbklkIjoiVTFjalkyRnljbVZtYjNWeVluSTVOelE9IiwidXRtX2NhbXBhaWduIjpudWxsLCJ1dG1fc291cmNlIjpudWxsLCJ1dG1pX2NhbXBhaWduIjpudWxsLCJjdXJyZW5jeUNvZGUiOiJCUkwiLCJjdXJyZW5jeVN5bWJvbCI6IlIkIiwiY291bnRyeUNvZGUiOiJCUkEiLCJjdWx0dXJlSW5mbyI6InB0LUJSIiwiY2hhbm5lbFByaXZhY3kiOiJwdWJsaWMifQ";

   @Override
   protected String getLocationToken() {
      return LOCATION_TOKEN;
   }

   @Override
   protected String getHomePage() {
      return HOME_PAGE;
   }

   @Override
   protected String getCep() {
      return LOCATION;
   }
}
