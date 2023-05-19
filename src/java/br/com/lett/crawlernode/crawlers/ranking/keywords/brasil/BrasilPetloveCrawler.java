package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.fetcher.DynamicDataFetcher;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.util.List;

public class BrasilPetloveCrawler extends CrawlerRankingKeywords {

   public BrasilPetloveCrawler(Session session) {
      super(session);
   }

   @Override
   protected Document fetchDocument(String url) {
      List<String> proxies = List.of(ProxyCollection.BUY_HAPROXY, ProxyCollection.NETNUT_RESIDENTIAL_BR_HAPROXY, ProxyCollection.NETNUT_RESIDENTIAL_ANY_HAPROXY);
      int attempt = 0;
      boolean succes = false;
      Document doc = new Document("");
      do {
         try {
            webdriver = DynamicDataFetcher.fetchPageWebdriver(url, proxies.get(attempt), session);
            if (webdriver != null) {
               doc = Jsoup.parse(webdriver.getCurrentPageSource());
               succes = !doc.select("ul.catalog-items").isEmpty();
               webdriver.terminate();
            }
         } catch (Exception e) {
            Logging.printLogDebug(logger, session, CommonMethods.getStackTrace(e));
            Logging.printLogWarn(logger, "Page not captured");
         }
      } while (!succes && attempt++ <= (proxies.size() - 1));
      return doc;
   }

   @Override
   protected void extractProductsFromCurrentPage() throws MalformedProductException {
      this.pageSize = 20;

      this.log("Página " + this.currentPage);

      String url = "https://www.petlove.com.br/busca?q=" + this.keywordEncoded + "&page=" + this.currentPage;
      this.log("Link onde são feitos os crawlers: " + url);

      Document doc = fetchDocument(url);

      Element catalogJSON = doc.selectFirst("script:containsData(window.catalogJSON)");
      String aux = catalogJSON.html().replace(" ", "").replace("\n", "");
      String jsonString = CommonMethods.substring(aux, "window.catalogJSON=", ";window.catalogJSON.pageType=null;", true);
      JSONObject productJson = CrawlerUtils.stringToJSONObject(jsonString);


      if (!productJson.isEmpty()) {
         if (this.totalProducts == 0) {
            this.totalProducts = productJson.optInt("max_produtos");
         }

         JSONArray products = productJson.optJSONArray("produtos");
         for (Object productObj : products) {
            JSONObject product = (JSONObject) productObj;

            String internalId = product.optString("sku");
            String productUrl = product.optString("link");
            String name = scrapName(product);
            String imgUrl = product.optString("figura");
            boolean isAvailable = product.optInt("disponivel") != 0;
            Integer price = isAvailable ? scrapPrice(product) : null;

            RankingProduct productRanking = RankingProductBuilder.create()
               .setUrl(productUrl)
               .setInternalId(internalId)
               .setInternalPid(null)
               .setImageUrl(imgUrl)
               .setName(name)
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

   private Integer scrapPrice(JSONObject product) {
      Integer pricePor = CommonMethods.stringPriceToIntegerPrice(product.optString("por"), '.', null);
      if (pricePor == null) {
         Integer priceDe = CommonMethods.stringPriceToIntegerPrice(product.optString("de"), '.', null);
         return priceDe;
      }

      return pricePor;
   }

   private String scrapName(JSONObject product) {
      String name = product.optString("nome");
      if (name != null) {
         String corrected = name.replaceAll("([a-z])([A-Z])", "$1 $2");
         return corrected.replaceAll("-", " - ");
      }

      return null;
   }
}
