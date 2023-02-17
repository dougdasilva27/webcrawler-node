package br.com.lett.crawlernode.crawlers.ranking.keywords.mexico;


import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import com.google.common.net.HttpHeaders;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.List;

public class MexicoFerrePatCrawler extends CrawlerRankingKeywords {
   public MexicoFerrePatCrawler(Session session) {
      super(session);
   }

   private static final String HOME_PAGE = "https://www.ferrepat.com";

   @Override
   protected JSONObject fetchJSONObject(String url) {

      String payload = "pagina=" + this.currentPage;
      HashMap<String, String> headers = new HashMap<>();
      headers.put(HttpHeaders.ORIGIN, HOME_PAGE);
      headers.put(HttpHeaders.REFERER, url);
      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .setHeaders(headers)
         .setProxyservice(List.of(
            ProxyCollection.BUY_HAPROXY,
            ProxyCollection.SMART_PROXY_MX_HAPROXY,
            ProxyCollection.NETNUT_RESIDENTIAL_MX_HAPROXY
         ))
         .setPayload(payload)
         .build();
      Response response = this.dataFetcher.post(session, request);
      if (response.getBody() != null && !response.getBody().isEmpty()) {
         return JSONUtils.stringToJson(response.getBody());
      }
      return new JSONObject();
   }

   @Override
   protected void extractProductsFromCurrentPage() throws UnsupportedEncodingException, MalformedProductException {
      String url = HOME_PAGE + "/tienda?search=" + this.keywordEncoded;
      JSONObject json = fetchJSONObject(url);
      if (json != null && !json.isEmpty() && json.has("products")) {
         for (Object object : json.optJSONArray("products")) {
            Element productHtml = Jsoup.parse(object.toString());
            String productUrl = CrawlerUtils.scrapStringSimpleInfoByAttribute(productHtml,".product-card a","href");
            String internalId = CommonMethods.getLast(productUrl.split("/"));
            String name = CrawlerUtils.scrapStringSimpleInfo(productHtml,".brand",true);
            Integer price = CrawlerUtils.scrapPriceInCentsFromHtml(productHtml,".new-price",null,true,'.',session,null);
            String image = CrawlerUtils.scrapStringSimpleInfoByAttribute(productHtml,"img","src");

            RankingProduct productRanking = RankingProductBuilder.create()
               .setInternalPid(internalId)
               .setName(name)
               .setAvailability(true)
               .setUrl(productUrl)
               .setImageUrl(image)
               .setPriceInCents(price)
               .build();
            saveDataProduct(productRanking);
            if (this.arrayProducts.size() == productsLimit) {
               break;
            }

         }
      }

   }
}
