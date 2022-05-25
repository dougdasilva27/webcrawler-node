package br.com.lett.crawlernode.crawlers.ranking.keywords.chile;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
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
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class ChileLidersuperCrawler extends CrawlerRankingKeywords {

   public ChileLidersuperCrawler(Session session) {
      super(session);
      super.fetchMode = FetchMode.JSOUP;
   }

   @Override
   protected Document fetchDocument(String url) {

      Map<String, String> headers = new HashMap<>();

      Request request = Request.RequestBuilder.create().setUrl(url)
         .setHeaders(headers)
         .setProxyservice(Arrays.asList(
            ProxyCollection.NETNUT_RESIDENTIAL_CO_HAPROXY))
         .setCookies(cookies)
         .build();

      Response response = CrawlerUtils.retryRequest(request, session, dataFetcher, true);

      return Jsoup.parse(response.getBody());
   }

   @Override
   protected void extractProductsFromCurrentPage() throws MalformedProductException {
      this.pageSize = 15;
      this.log("Página " + this.currentPage);

      String url = "https://www.lider.cl/supermercado/search?No=" + this.arrayProducts.size() + "&Ntt=" + this.keywordEncoded
         + "&isNavRequest=Yes&Nrpp=40&page=" + this.currentPage;

      this.log("Link onde são feitos os crawlers: " + url);
      this.currentDoc = fetchDocument(url);

      Elements products = this.currentDoc.select("#content-prod-boxes div[prod-number]");

      if (!products.isEmpty()) {
         if (this.totalProducts == 0) {
            setTotalProducts();
         }

         for (Element e : products) {
            String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, "div", "prod-number");
            String productUrl = CrawlerUtils.completeUrl(CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".product-details a", "href"), "https:", "www.lider.cl");
            String name = CrawlerUtils.scrapStringSimpleInfo(e, ".product-description", true);
            String imageUrl = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".photo-container img", "src");
            int price = CommonMethods.doublePriceToIntegerPrice(CrawlerUtils.scrapDoublePriceFromHtml(e, ".price-sell b", null, true, ',', session), 0);

            RankingProduct productRanking = RankingProductBuilder.create()
               .setUrl(productUrl)
               .setInternalId(internalId)
               .setName(name)
               .setPriceInCents(price)
               .setAvailability(isAvailable(e))
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

   protected boolean setAvailability(JSONObject jsonObject) {
      String available = jsonObject.optString("stockLevel");
      return available != null && available.contains("1");
   }

   protected JSONArray fetchAvaibility(StringBuilder payload) {
      String url = "https://www.lider.cl/supermercado/includes/inventory/inventoryInformation.jsp";
      payload.append("&useProfile=true&consolidate=true");
      Map<String, String> headers = new HashMap<>();
      headers.put("content-type", "application/x-www-form-urlencoded; charset=UTF-8");
      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .setHeaders(headers)
         .setPayload(payload.toString())
         .build();

      String content = dataFetcher.post(session, request).getBody();

      return CrawlerUtils.stringToJsonArray(content);
   }


   @Override
   protected void setTotalProducts() {
      Element total = this.currentDoc.selectFirst(".result-departments a:last-child span");
      if (total != null) {
         String text = total.ownText().replaceAll("[^0-9]", "");

         if (!text.isEmpty()) {
            this.totalProducts = Integer.parseInt(text);
            this.log("Total da busca: " + this.totalProducts);
         }
      }
   }

   private Boolean isAvailable(Element doc) {
      String noStock = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "button.btn-agotado", "class");

      if (noStock.equals("btn btn-info btn-block btn-agotado")) {
         return false;
      }
      return true;
   }
}
