package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.fetcher.CrawlerWebdriver;
import br.com.lett.crawlernode.core.fetcher.DynamicDataFetcher;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.VTEXOldNewImpl;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import cdjd.com.google.common.collect.Sets;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class BrasilDrogariapachecoCrawler extends VTEXOldNewImpl {

   protected Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),
      Card.AURA.toString(), Card.DINERS.toString(), Card.HIPER.toString(), Card.AMEX.toString());


   public BrasilDrogariapachecoCrawler(Session session) {
      super(session);
   }

   @Override
   protected Object fetch() {

      Document document = null;
      CrawlerWebdriver webdriver;
      try {
         webdriver = DynamicDataFetcher.fetchPageWebdriver(session.getOriginalURL(), ProxyCollection.BUY_HAPROXY, session);
         webdriver.waitLoad(12000);

         if (webdriver != null) {
            document = Jsoup.parse(webdriver.getCurrentPageSource());
            webdriver.terminate();
         }
      } catch (Exception e) {
         Logging.printLogInfo(logger, session, CommonMethods.getStackTrace(e));
      }
      return document;
   }

   @Override
   protected JSONObject crawlProductApi(String internalPid, String parameters) {
      JSONObject productApi = new JSONObject();

      Map<String, String> headers = new HashMap<>();
      headers.put("accept", "application/json");
      headers.put("accept-language", "pt-BR,pt;q=0.9,en-US;q=0.8,en;q=0.7");

      String url = homePage + "api/catalog_system/pub/products/search?fq=productId:" + internalPid + (parameters == null ? "" : parameters);

      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .setCookies(cookies)
         .setHeaders(headers)
         .setSendUserAgent(true)
         .build();

      JSONArray array = CrawlerUtils.stringToJsonArray(this.dataFetcher.get(session, request).getBody());

      if (!array.isEmpty()) {
         productApi = array.optJSONObject(0) == null ? new JSONObject() : array.optJSONObject(0);
      }

      return productApi;
   }
}
