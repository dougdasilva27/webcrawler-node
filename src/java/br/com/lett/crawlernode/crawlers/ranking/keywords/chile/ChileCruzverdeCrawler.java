package br.com.lett.crawlernode.crawlers.ranking.keywords.chile;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.methods.JavanetDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;


import java.util.*;

public class ChileCruzverdeCrawler extends CrawlerRankingKeywords {

   public ChileCruzverdeCrawler(Session session) {
      super(session);
      super.fetchMode = FetchMode.JSOUP;
   }

   protected void extractProductsFromCurrentPage() throws MalformedProductException {
      this.pageSize = 12;
      this.log("Página " + this.currentPage);

      String url = "https://www.cruzverde.cl/search?query=" + this.keywordWithoutAccents + "&search-button=&lang=es_CL";
      String urlWithoutSpaces = url.replaceAll(" ", "+");

      this.log("Link onde são feitos os crawlers: " + urlWithoutSpaces);
      this.currentDoc = fetchDocument(urlWithoutSpaces);
      Elements products = this.currentDoc.select(".product.product-wrapper");
      String internalId = getProductList();

      if (!products.isEmpty()) {
         if (this.totalProducts == 0) {
            setTotalProducts();
         }

         for (Element e : products) {
            String productUrl = "https://www.cruzverde.cl" + CrawlerUtils.scrapStringSimpleInfoByAttribute(e, " .image-container a", "href");
            String name = CrawlerUtils.scrapStringSimpleInfo(e, ".pdp-link", false);
            String imgUrl = CrawlerUtils.scrapSimplePrimaryImage(e, ".tile-image", Collections.singletonList("src"), "https", "cruzverde.cl");
            Integer price = crawlPrice(e);
            boolean isAvailable = CrawlerUtils.scrapStringSimpleInfo(e, ".add-to-cart:not([disabled])", false) != null;

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

   private Integer crawlPrice(Element e) {
      int price = 0;
      String priceString = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".sales .value", "content");
      if (priceString != null && !priceString.isEmpty() && !priceString.equals("null")) {
         price = Integer.parseInt(priceString) * 100;
      } else {
         price = CrawlerUtils.scrapPriceInCentsFromHtml(e, ".d-block .large-price .value", null, true, ',', session, 0);
      }
      return price;
   }

   @Override
   protected boolean hasNextPage() {
      return currentDoc.selectFirst(".pagination.pagination-footer button:not(first-child)") != null;
   }

   private String getProductList() {
      int pageOff = 0;
      String url = "https://api.cruzverde.cl/product-service/products/search?limit=" + pageSize + "&offset=" + pageOff + "&sort=&q=" + keywordEncoded;
      //Map<String, String> headers = new HashMap<>();
      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         //.setHeaders(headers)
         .mustSendContentEncoding(true)
         .setProxyservice(List.of(ProxyCollection.NETNUT_RESIDENTIAL_MX_HAPROXY, ProxyCollection.LUMINATI_SERVER_BR, ProxyCollection.NETNUT_RESIDENTIAL_CO_HAPROXY, ProxyCollection.BUY_HAPROXY))
         .build();
      Response response = this.dataFetcher.get(session, request);
      if (response != null){
         JSONObject reponseJson = CrawlerUtils.stringToJson(response.getBody());
      }
      return response.getBody();
   }
}
