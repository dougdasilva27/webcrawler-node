package br.com.lett.crawlernode.crawlers.corecontent.curitiba;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.SupermuffatodeliveryCrawler;

public class CuritibaMuffatoCrawler extends SupermuffatodeliveryCrawler {

   public CuritibaMuffatoCrawler(Session session) {
      super(session);
   }

   private static final String CITY_CODE = "14";

   @Override
   public String handleURLBeforeFetch(String url) {
      return super.handleURLBeforeFetch(url.split("\\?")[0] + "?sc=" + CITY_CODE);
   }
}
