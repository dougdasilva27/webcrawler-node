package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import br.com.lett.crawlernode.util.Logging;
import org.eclipse.jetty.util.ajax.JSON;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

public class BrasilBebidasStoreCrawler extends CrawlerRankingKeywords {
   public BrasilBebidasStoreCrawler(Session session) {
      super(session);
      pageSize = 15;
   }

   @Override
   protected void extractProductsFromCurrentPage() throws UnsupportedEncodingException, MalformedProductException {

      JSONObject dataJson = getDataJson();
      if (dataJson != null && dataJson.length() > 0) {
         totalProducts = dataJson.optInt("siteSearchResults");
         JSONArray products = dataJson.optJSONArray("listProducts");
         for (Object p : products) {
            JSONObject product = (JSONObject) p;
            Boolean available = checkAvailibity(product.optString("availability"));
            Integer price = available ? getPrice(product.optString("price", null)) : null;
            String internalId = product.optString("idProduct", null);
            String name = product.optString("nameProduct", null);
            String url = product.optString("urlProduct", null);
            String imageUrl = product.optString("urlImage", null);
            RankingProduct productRanking = RankingProductBuilder.create()
               .setInternalId(internalId)
               .setUrl(url)
               .setName(name)
               .setAvailability(available)
               .setPriceInCents(price)
               .setImageUrl(imageUrl)
               .build();
            saveDataProduct(productRanking);
            if (this.arrayProducts.size() == productsLimit) {
               break;
            }
         }
      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }
   }


   private Boolean checkAvailibity(String available) {
      if (available != null && !available.isEmpty()) {
         return available.equals("YES");
      }
      return false;
   }

   private Integer getPrice(String price) {
      if (price != null && !price.isEmpty()) {
         return CommonMethods.stringPriceToIntegerPrice(price, '.', null);
      }
      return null;
   }

   private static String HOME_PAGE = "https://www.bebidastore.com.br/";

   private JSONObject getDataJson() {
      String query = HOME_PAGE + "loja/busca.php?loja=1055537&palavra_busca=" + this.keywordEncoded + "&pg=" + this.currentPage;
      Document docHtml = fetchDocument(query);
      if (docHtml != null) {
         Elements data = docHtml.select("script");
         if (data != null && !data.isEmpty()) {
            for (Element e : data) {
               String script = CrawlerUtils.scrapScriptFromHtml(e, "script");
               if (script != null && !script.isEmpty() && script.contains("dataLayer")) {
                  String jsonString = script.replace("dataLayer = ", "");
                  JSONArray jsonArray = CrawlerUtils.stringToJsonArray(jsonString);
                  if (jsonArray != null && !jsonArray.isEmpty()) {
                     JSONObject obj = JSONUtils.getValueRecursive(jsonArray, "0.0", JSONObject.class);
                     return obj;
                  }
               }
            }
         }

      }
      return null;
   }
}
