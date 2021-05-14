package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.CarrefourCrawler;

/**
 * 18/04/2020
 *
 * @author Fabrício
 */
public class SaopauloCarrefourbrooklinCrawler extends CarrefourCrawler {

   public SaopauloCarrefourbrooklinCrawler(Session session) {
      super(session);
   }

   public static final String HOME_PAGE = "https://mercado.carrefour.com.br/";
   public static final String USER_LOCATION = "04555-002";
   public static final String LOCATION_TOKEN = "eyJjYW1wYWlnbnMiOm51bGwsImNoYW5uZWwiOiIyIiwicHJpY2VUYWJsZXMiOm51bGwsInJlZ2lvbklkIjoiVTFjalkyRnljbVZtYjNWeVluSTVNakk9IiwidXRtX2NhbXBhaWduIjoiYnJhbmRpbmdfZm9vZF9ub3ZvcyIsInV0bV9zb3VyY2UiOiJnb29nbGVfYnJhbmRpbmdfZm9vZCIsInV0bWlfY2FtcGFpZ24iOm51bGwsImN1cnJlbmN5Q29kZSI6IkJSTCIsImN1cnJlbmN5U3ltYm9sIjoiUiQiLCJjb3VudHJ5Q29kZSI6IkJSQSIsImN1bHR1cmVJbmZvIjoicHQtQlIiLCJjaGFubmVsUHJpdmFjeSI6InB1YmxpYyJ9";

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
      return USER_LOCATION;
   }
}
