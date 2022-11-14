package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.methods.JsoupDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class BrasilAlthoffSupermercadosCrawler extends CrawlerRankingKeywords {
   public BrasilAlthoffSupermercadosCrawler(Session session) {
      super(session);
   }

   private String endCursor = null;

   protected String getStoreId() {
      return session.getOptions().optString("storeId");
   }

   protected JSONObject fetchDocument() {
      Map<String, String> headers = new HashMap<>();
      headers.put("content-type", "application/json");

      String payload = "{\"accountId\":50,\"storeId\":" + getStoreId() + ",\"categoryName\":null,\"first\":12,\"promotion\":null,\"after\":"
         + endCursor + ",\"search\":\"" + this.keywordEncoded + "\",\"brands\":[],\"categories\":[],\"tags\":[],\"personas\":[],\"sort\":" +
         "{\"field\":\"_score\",\"order\":\"desc\"},\"pricingRange\":{},\"highlightEnabled\":false}";

      Request request = Request.RequestBuilder.create()
         .setUrl("https://search.osuper.com.br/ecommerce_products_production/_search")
         .setPayload(payload)
         .setHeaders(headers)
         .setProxyservice(Arrays.asList(
            ProxyCollection.LUMINATI_SERVER_BR,
            ProxyCollection.NETNUT_RESIDENTIAL_BR))
         .build();

      Response response = new JsoupDataFetcher().post(session, request);

      return JSONUtils.stringToJson(response.getBody());
   }

   @Override
   protected void extractProductsFromCurrentPage() throws MalformedProductException {
      this.pageSize = 12;
      this.log("Página " + this.currentPage);

      JSONObject json = fetchDocument();
      endCursor = JSONUtils.getValueRecursive(json, "pageInfo.endCursor", String.class);


      if (!products.isEmpty()) {

         JSONArray products = JSONUtils.getJSONArrayValue(json, "edges");
         for (Element e : products) {
            String internalPid = e.attr("data-id");
            String productUrl = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, "a", "href");

            String name = CrawlerUtils.scrapStringSimpleInfo(e, "span > h3 > a", true);
            String imageUrl = CrawlerUtils.scrapSimplePrimaryImage(e, ".span > a > img", Arrays.asList("src"), "https", "www.farmaonline.com");
            Integer price = CrawlerUtils.scrapPriceInCentsFromHtml(e, "span > .price > a > .best-price", null, true, ',', session, null);
            boolean isAvailable = price != null;

            RankingProduct productRanking = RankingProductBuilder.create()
               .setUrl(productUrl)
               .setInternalPid(internalPid)
               .setName(name)
               .setPriceInCents(price)
               .setAvailability(isAvailable)
               .setImageUrl(imageUrl)
               .build();

            saveDataProduct(productRanking);

            if (this.arrayProducts.size() == productsLimit) {
               break;
            }

         }
      } else {
         this.result = false;
         this.log("Keyword sem resultado!");
      }

      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");
   }

   @Override
   protected void setTotalProducts() {
      String url = getHomePage() + keywordWithoutAccents.replace(" ", "%20");
      Document doc = fetchDocument(url);

      totalProducts = CrawlerUtils.scrapIntegerFromHtml(doc, ".resultado-busca-numero .value", false, 0);
   }


}
