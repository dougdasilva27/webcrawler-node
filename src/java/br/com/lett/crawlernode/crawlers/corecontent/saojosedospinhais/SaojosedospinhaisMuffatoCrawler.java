package br.com.lett.crawlernode.crawlers.corecontent.saojosedospinhais;

import org.json.JSONObject;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.SupermuffatodeliveryCrawler;

public class SaojosedospinhaisMuffatoCrawler extends SupermuffatodeliveryCrawler {

   public SaojosedospinhaisMuffatoCrawler(Session session) {
      super(session);
   }

   private static final String CITY_CODE = "11";

   @Override
   public String handleURLBeforeFetch(String url) {
      return super.handleURLBeforeFetch(url.split("\\?")[0] + "?sc=" + CITY_CODE);
   }

   @Override
   protected JSONObject crawlProductApi(String internalPid, String parameters) {
      return super.crawlProductApi(internalPid, "&sc=" + CITY_CODE);
   }
}