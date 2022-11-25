package br.com.lett.crawlernode.crawlers.ranking.keywords.peru;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class PeruMetroCrawler extends CrawlerRankingKeywords {

   private String getVtex() {return session.getOptions().optString("vtex_segment");}
   private String getLocation(){return session.getOptions().optString("VTEXSC");}

   private String getHomePage(){return session.getOptions().optString("homePage");}
   public PeruMetroCrawler(Session session) {
      super(session);
      super.fetchMode = FetchMode.JSOUP;
   }

   @Override
   protected Document fetchDocument(String url) {
      BasicClientCookie cookie = new BasicClientCookie("VTEXSC",getLocation());
      cookie.setDomain(".www.metro.pe");
      cookie.setPath("/");
      this.cookies.add(cookie);

      BasicClientCookie cookie2 = new BasicClientCookie("vtex_segment",getVtex());
      cookie2.setDomain("www.metro.pe");
      cookie2.setPath("/");
      this.cookies.add(cookie2);

      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .setCookies(cookies)
         .build();

      String response = dataFetcher.get(session, request).getBody();

      return Jsoup.parse(response);
   }

   @Override
   protected void extractProductsFromCurrentPage() throws MalformedProductException {
      this.pageSize = 18;
      this.log("Página " + this.currentPage);

      String url =
         getHomePage() + "busca/?ft=" + this.keywordEncoded + "&PageNumber=" + this.currentPage;

      this.log("Link onde são feitos os crawlers: " + url);
      this.currentDoc = fetchDocument(url);
      Elements products = this.currentDoc.select(".product-shelf .product-item");

      if (!products.isEmpty()) {
         if (this.totalProducts == 0) {
            setTotalProducts();
         }
         for (Element e : products) {
            String internalId = crawlInternalId(e);
            String internalPid = crawlProductPid(e);
            String productName = e.attr("data-name");
            String productUrl = crawlProductUrl(e);
            String imageUrl = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, "div > noscript > img", "src");
            Integer price = getPrice(e);
            boolean isAvailable = price != null;

            RankingProduct productRanking = RankingProductBuilder.create()
               .setUrl(productUrl)
               .setInternalId(internalId)
               .setInternalPid(internalPid)
               .setName(productName)
               .setPriceInCents(price)
               .setAvailability(isAvailable)
               .setImageUrl(imageUrl)
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

   @Override
   protected void setTotalProducts() {
      Element totalElement = this.currentDoc.select(".resultado-busca-numero .value").first();

      if (totalElement != null) {
         String text = totalElement.ownText().trim();

         if (!text.isEmpty()) {
            this.totalProducts = Integer.parseInt(text);
         }

         this.log("Total da busca: " + this.totalProducts);
      }
   }

   private String crawlInternalId(Element e) {
      return e.attr("data-sku");
   }

   private String crawlProductPid(Element e) {
      return e.attr("data-id");
   }

   private String crawlProductUrl(Element e) {
      return e.attr("data-uri");
   }

   private Integer getPrice(Element element) {
      String extractPriceFrom = CrawlerUtils.scrapStringSimpleInfo(element, ".product-prices__value.product-prices__value--best-price", false).replace("S/. ", "");
      if (extractPriceFrom != null && !extractPriceFrom.isEmpty()) {
         return CommonMethods.stringPriceToIntegerPrice(extractPriceFrom, '.', 0);
      }
      return null;
   }
}
