package br.com.lett.crawlernode.crawlers.extractionutils.ranking;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.session.Session;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class PeruRappiCrawler extends RappiCrawlerRanking {

   public PeruRappiCrawler(Session session) {
      super(session);
      PRODUCT_BASE_URL = "https://www." + getProductDomain() + "/produto/";
   }

   public static final String API_DOMAIN = "rappi.pe";
   public static final String PRODUCT_DOMAIN = "rappi.com.pe";
   public final String storeName = session.getOptions().optString("storeName");

   @Override
   protected String getApiDomain() {
      return API_DOMAIN;
   }

   @Override
   protected String getProductDomain() {
      return PRODUCT_DOMAIN;
   }


}
