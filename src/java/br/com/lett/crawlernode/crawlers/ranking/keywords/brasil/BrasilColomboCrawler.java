package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CommonMethods;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class BrasilColomboCrawler extends CrawlerRankingKeywords {

   public BrasilColomboCrawler(Session session) {
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() throws MalformedProductException {
      this.pageSize = 24;

      this.log("Página " + this.currentPage);

      String url = "https://pesquisa.colombo.com.br/busca?q=" + this.keywordWithoutAccents.replace(" ", "%20") + "&televendas=&page=" + this.currentPage;

      this.log("Link onde são feitos os crawlers: " + url);

      this.currentDoc = fetchDocument(url);

      Elements products = this.currentDoc.select(".neemu-products-container .nm-product-item");

      HashMap<String, String> productsPrices = crawlProductsPrices(products);

      if (!products.isEmpty()) {
         for (Element e : products) {
            String internalPid = crawlInternalPid(e.attr("id"));
            String urlProduct = CrawlerUtils.completeUrl(CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".nm-product-info", "href"), "https", "www.colombo.com");
            String name = CrawlerUtils.scrapStringSimpleInfo(e, ".nm-product-name", true);
            String imgUrl = CrawlerUtils.scrapSimplePrimaryImage(e, ".nm-product-img", Collections.singletonList("src"), "https", "images.colombo.com.br");
            Integer price = 0;

            if(productsPrices.containsKey(internalPid)) {
               price = CommonMethods.stringPriceToIntegerPrice(productsPrices.get(internalPid), ',', 0);
            }
            boolean isAvailable = price != 0;

            RankingProduct productRanking = RankingProductBuilder.create()
               .setUrl(urlProduct)
               .setInternalId(null)
               .setInternalPid(internalPid)
               .setImageUrl(imgUrl)
               .setName(name)
               .setPriceInCents(price)
               .setAvailability(isAvailable)
               .build();

            saveDataProduct(productRanking);
            if (this.arrayProducts.size() == productsLimit)
               break;
         }
      } else {
         this.result = false;
         this.log("Keyword sem resultado!");
      }

      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");
   }

   private HashMap<String, String> crawlProductsPrices(Elements products) {
      HashMap<String, String> productsPrices = new HashMap<>();
      List<String> productsIds = products.stream().map(e ->  crawlInternalPid(e.attr("id"))).collect(Collectors.toList());
      String productsIdsQuery = String.join(",", productsIds);

      Map<String, String> headers = new HashMap<>();
      headers.put("authority", "apipreco.colombo.com.br");

      Request request = Request.RequestBuilder.create().setUrl("https://apipreco.colombo.com.br/api/precoEstoque/produto?codigosProdutos=" + productsIdsQuery).setHeaders(
            headers).build();
      String content = this.dataFetcher.get(session, request).getBody();

      try {
         JSONArray productJSON = CrawlerUtils.stringToJsonArray(content);
         for(Object product : productJSON) {
            if (product instanceof JSONObject) {
               JSONObject productObject = (JSONObject) product;
               String internalPid = productObject.optString("codigo");
               String price = productObject.optString("precoPor");
               productsPrices.put(internalPid, price);
            }
         }
      } catch (Exception e) {
         e.printStackTrace();
      }

      return productsPrices;
   }

   private String crawlInternalPid(String text) {
      String internalPid = "";

      if(text != null && !text.isEmpty()) {
         try {
            internalPid = text.split("-")[2];
         } catch(Exception e) {
            internalPid = text.replaceAll("[^0-9]+", "");
         }
      }
      return internalPid;
   }

   @Override
   protected boolean hasNextPage() {
      return this.currentDoc.select(".neemu-pagination .neemu-pagination-inner a") != null;
   }

}
