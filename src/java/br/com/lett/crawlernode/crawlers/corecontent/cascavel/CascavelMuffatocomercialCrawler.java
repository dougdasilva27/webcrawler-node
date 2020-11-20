package br.com.lett.crawlernode.crawlers.corecontent.cascavel;

import org.json.JSONObject;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.SupermuffatodeliveryCrawler;

public class CascavelMuffatocomercialCrawler extends SupermuffatodeliveryCrawler {

   public CascavelMuffatocomercialCrawler(Session session) {
      super(session);
   }

   private static final String CITY_CODE = "17";

   @Override
   public String handleURLBeforeFetch(String url) {
      return super.handleURLBeforeFetch(url.split("\\?")[0] + "?sc=" + CITY_CODE);
   }

   @Override
   protected JSONObject crawlProductApi(String internalPid, String parameters) {
      return super.crawlProductApi(internalPid, "&sc=" + CITY_CODE);
   }
}