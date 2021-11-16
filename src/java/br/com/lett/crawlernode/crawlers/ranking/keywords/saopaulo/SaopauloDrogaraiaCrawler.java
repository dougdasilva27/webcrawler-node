package br.com.lett.crawlernode.crawlers.ranking.keywords.saopaulo;

import br.com.lett.crawlernode.core.fetcher.methods.FetcherDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class SaopauloDrogaraiaCrawler extends CrawlerRankingKeywords {

   public SaopauloDrogaraiaCrawler(Session session) {
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() throws MalformedProductException {
      this.pageSize = 12;

      this.log("Página " + this.currentPage);
      String url = "https://busca.drogaraia.com.br/search?w=" + this.keywordEncoded + "&cnt=150&srt=" + this.arrayProducts.size();

      Map<String, String> headers = new HashMap<>();
      headers.put("upgrade-insecure-requests", "1");
      headers.put("accept", "text/html");
      headers.put("accept-language", "pt-BR");

      this.log("Link onde são feitos os crawlers: " + url);

      Request request = RequestBuilder.create().setUrl(url).setCookies(cookies).setHeaders(headers).build();
      String response = new FetcherDataFetcher().get(session, request).getBody();

      if (response != null && !response.isEmpty()) {
         this.currentDoc = Jsoup.parse(response);
      } else {
         this.currentDoc = fetchDocument(url);
      }


      Elements products = this.currentDoc.select(".item div.container:not(.min-limit)");

      if (!products.isEmpty()) {
         if (this.totalProducts == 0) {
            setTotalProducts();
         }

         for (Element e : products) {
            String internalId = crawlInternalId(e);
            String productUrl = crawlProductUrl(e);
            String name = CrawlerUtils.scrapStringSimpleInfo(e, "h2.product-name a", true);
            String imgUrl = CrawlerUtils.scrapSimplePrimaryImage(e, ".product-image img", Collections.singletonList("data-src"), "https", "");
            Integer price = CrawlerUtils.scrapIntegerFromHtml(e, ".price-box .price-text", true, 0);
            boolean isAvailable = price != 0;

            RankingProduct productRanking = RankingProductBuilder.create()
               .setUrl(productUrl)
               .setInternalId(internalId)
               .setInternalPid(internalId)
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

   @Override
   protected void setTotalProducts() {
      Element totalElement = this.currentDoc.selectFirst("p.amount");

      if (totalElement != null && this.currentDoc.selectFirst("#pantene") == null) {
         String token = totalElement.text().replaceAll("[^0-9]", "").trim();

         if (!token.isEmpty()) {
            this.totalProducts = Integer.parseInt(token);
         }

         this.log("Total da busca: " + this.totalProducts);
      }
   }

   private String crawlInternalId(Element e) {
      String internalId = null;
      Element id = e.selectFirst(".trustvox-shelf-container[data-trustvox-product-code]");

      if (id != null) {
         internalId = id.attr("data-trustvox-product-code");
      }

      return internalId;
   }

   private String crawlProductUrl(Element e) {
      String urlProduct = null;
      Element urlElement = e.selectFirst(".product-name.sli_title a");

      if (urlElement != null) {
         urlProduct = urlElement.attr("title");
      } else {
         urlElement = e.selectFirst(".product-name a");

         if (urlElement != null) {
            urlProduct = urlElement.attr("href");
         }
      }

      return urlProduct;
   }
}
