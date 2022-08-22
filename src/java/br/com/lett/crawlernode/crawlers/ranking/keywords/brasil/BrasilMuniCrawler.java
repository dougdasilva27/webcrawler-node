package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.JSONUtils;
import br.com.lett.crawlernode.util.Logging;
import com.google.gson.JsonParser;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

public class BrasilMuniCrawler extends CrawlerRankingKeywords {
   public static final String PRODUCTS_API_URL = "https://gri9dmsahc-3.algolianet.com/1/indexes/*/queries?x-algolia-agent=Algolia%20for%20vanilla%20JavaScript%20(lite)%203.32.1%3Binstantsearch.js%201.12.1%3BMagento%20integration%20(1.16.0)%3BJS%20Helper%202.26.1&x-algolia-application-id=GRI9DMSAHC&x-algolia-api-key=ZWE4ZDQyOTM5YjdjNDE0NWU5NjI5NWVhNzE4ODAwNDk5OTBjMjlhY2RiMTJiYzgzMjE0Mjc5ZmM3YmZiZTYzY2ZpbHRlcnM9Jm51bWVyaWNGaWx0ZXJzPXZpc2liaWxpdHlfc2VhcmNoJTNEMQ%3D%3D";

   public BrasilMuniCrawler(Session session) {
      super(session);
   }

   protected void extractProductsFromCurrentPage() throws UnsupportedEncodingException, MalformedProductException {
      this.pageSize = 32;
      this.log("Página " + this.currentPage);

      JSONObject search = fetchProductsFromAPI();
      JSONArray arraySkus = search.has("hits") ? search.getJSONArray("hits") : new JSONArray();

      if (arraySkus.length() > 0) {

         if (this.totalProducts == 0) {
            setTotalProducts(search);
         }

         for (Object product : arraySkus) {
            JSONObject jsonSku = (JSONObject) product;
            String internalId = JSONUtils.getStringValue(jsonSku, "objectID");
            String productUrl = JSONUtils.getStringValue(jsonSku, "url");
            String imgUrl = JSONUtils.getStringValue(jsonSku, "image_url");
            String name = JSONUtils.getStringValue(jsonSku, "name");
            Integer price = crawlPrice(jsonSku);
            boolean isAvailable = JSONUtils.getIntegerValueFromJSON(jsonSku, "in_stock", 0) >= 1 ? true : false;
            RankingProduct productRanking = RankingProductBuilder.create()
               .setUrl(productUrl)
               .setInternalId(internalId)
               .setName(name)
               .setImageUrl(imgUrl)
               .setPriceInCents(price)
               .setAvailability(isAvailable)
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

   private Integer crawlPrice(JSONObject product) {
      Integer price;
      try {
         String priceStr = product.optQuery("/price/BRL/default").toString();
         Double priceDouble = Double.parseDouble(priceStr) * 100;
         price = priceDouble.intValue();
      } catch (NullPointerException e) {
         price = 0;
      }
      return price;
   }

   private void setTotalProducts(JSONObject search) {
      this.totalProducts = JSONUtils.getIntegerValueFromJSON(search, "nbHits", 0);
      this.log("Total: " + this.totalProducts);
   }

   private JSONObject fetchProductsFromAPI() {
      JSONObject products = new JSONObject();

      String payload = "{\"requests\":[{\"indexName\":\"farmadelivery_default_products\","
         + "\"params\":\"query=" + this.keywordEncoded
         + "&hitsPerPage=32&maxValuesPerFacet=30&page=" + (this.currentPage - 1)
         + "&ruleContexts=%5B%22magento_filters%22%2C%22%22%5D&facets=%5B%22brand%22%2C%22"
         + "composicao_new%22%2C%22manufacturer%22%2C%22activation_information%22%2C%22frete_gratis_dropdown%22%2C%22category_ids%22%2C%22"
         + "price.BRL.default%22%2C%22color%22%2C%22categories.level0%22%5D&tagFilters=\"}]}";

      Map<String, String> headers = new HashMap<>();
      headers.put("Content-Type", "application/x-www-form-urlencoded");
      headers.put("Accept-Encoding", "no");

      Request request = Request.RequestBuilder.create()
         .setUrl(PRODUCTS_API_URL)
         .setCookies(cookies)
         .setHeaders(headers)
         .setPayload(payload)
         .mustSendContentEncoding(false)
         .build();
      String page = this.dataFetcher.post(session, request).getBody();

      if (page.startsWith("{") && page.endsWith("}")) {
         try {
            // Using google JsonObject to get a JSONObject because this json can have a duplicate key.
            JSONObject result = new JSONObject(new JsonParser().parse(page).getAsJsonObject().toString());

            if (result.has("results") && result.get("results") instanceof JSONArray) {
               JSONArray results = result.getJSONArray("results");
               if (results.length() > 0 && results.get(0) instanceof JSONObject) {
                  products = results.getJSONObject(0);
               }
            }

         } catch (Exception e) {
            Logging.printLogWarn(logger, session, CommonMethods.getStackTrace(e));
         }
      }

      return products;
   }
}
