package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.JSONUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import javax.print.Doc;
import java.util.*;

public class BrasilAdegaOnlineCrawler extends CrawlerRankingKeywords {

   private static final String API_URL = "https://api.ze.delivery/public-api";

   public BrasilAdegaOnlineCrawler(Session session) {
      super(session);
   }

   protected Document fetch(String url) {
      Map<String, String> headers = new HashMap<>();

      headers.put("Accept","text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9");
      headers.put("Accept-Encoding","gzip");
      headers.put("Connection","keep-alive");

      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .setHeaders(headers)
         .setSendUserAgent(false)
         .build();

      Response a = this.dataFetcher.get(session, request);

      String content = a.getBody();
      return Jsoup.parse(content);
   }

   @Override
   protected void extractProductsFromCurrentPage() throws MalformedProductException {
      this.pageSize = 20;
      this.log("Página " + this.currentPage);

      String url = "https://www.adegaonline.com.br/search?q=" + this.keywordEncoded + "&type=product&page=" + this.currentPage;

      Document doc = fetch(url);

      if (doc.selectFirst("#bold-subscriptions-platform-script") != null) {
         JSONArray scriptJson = scrapJsonFromScript(doc);

         JSONArray products = sortPositions(scriptJson);

         for (Object o: products) {
            JSONObject product = (JSONObject) o;

            String name = product.optString("title");
            Long internalId = product.optLong("id");
            Integer price = product.optInt("price");
            Boolean available = product.optBoolean("available");
            String imgUrl = scrapImg(product);
            String productUrl = scrapUrl(product);

            RankingProduct productRanking = RankingProductBuilder.create()
               .setUrl(productUrl)
               .setInternalId(internalId.toString())
               .setName(name)
               .setImageUrl(imgUrl)
               .setPriceInCents(price)
               .setAvailability(available)
               .build();

            saveDataProduct(productRanking);
         }
         this.log("Keyword com resultado!");
      } else {
         this.result = false;
         this.log("Keyword sem resultado!");
      }
      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");
   }


   private String scrapUrl(JSONObject product) {
      return "https://www.adegaonline.com.br/products/" + product.optString("handle");
   }

   private String scrapImg(JSONObject product) {
      return "https:" + JSONUtils.getValueRecursive(product, "images.0", String.class);
   }

   private JSONArray scrapJsonFromScript(Document doc) {
      Element script = doc.selectFirst("#bold-subscriptions-platform-script");
      List <String> jsonWithoutScriptFront = List.of(script.toString().split("concat\\("));
      List <String> jsonWithoutScriptBack = new ArrayList<>();
      String json = null;

      if (jsonWithoutScriptFront != null && jsonWithoutScriptFront.size() > 0) {
         jsonWithoutScriptBack = List.of(jsonWithoutScriptFront.get(1).split("\\);"));
      }

      if (jsonWithoutScriptBack != null && !jsonWithoutScriptBack.isEmpty()) {
         json = jsonWithoutScriptBack.get(0);
      }

      return new JSONArray(json);
   }

   private JSONArray sortPositions(JSONArray disorderedProducts) {
      return disorderedProducts;
   }

   @Override
   protected boolean hasNextPage() {
      return true;
   }
}
