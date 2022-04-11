package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.SupermuffatodeliveryCrawler;
import org.json.JSONObject;

public class BrasilMuffatoNorteShoppingCrawler extends SupermuffatodeliveryCrawler {
   private static final String CITY_CODE = "21";

   public BrasilMuffatoNorteShoppingCrawler(Session session) {
      super(session);
   }

   @Override
   public String handleURLBeforeFetch(String url) {
      return super.handleURLBeforeFetch(url.split("\\?")[0] + "?sc=" + CITY_CODE);
   }

   @Override
   protected JSONObject crawlProductApi(String internalPid, String parameters) {
      return super.crawlProductApi(internalPid, "&sc=" + CITY_CODE);
   }
}
