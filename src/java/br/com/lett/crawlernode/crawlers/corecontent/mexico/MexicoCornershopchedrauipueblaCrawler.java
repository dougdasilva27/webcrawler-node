package br.com.lett.crawlernode.crawlers.corecontent.mexico;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.CornershopCrawler;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.json.JSONObject;

public class MexicoCornershopchedrauipueblaCrawler extends CornershopCrawler {

   public MexicoCornershopchedrauipueblaCrawler(Session session) {
      super(session);
   }

    public static final String STORE_ID = "457";
   public static final String SELLER_FULL_NAME = "Cornershop Chedraui Puebla";


   @Override
   protected String getStoreId() {
      return STORE_ID;
   }

   @Override
   protected String getSellerName() {
      return SELLER_FULL_NAME;
   }


}
