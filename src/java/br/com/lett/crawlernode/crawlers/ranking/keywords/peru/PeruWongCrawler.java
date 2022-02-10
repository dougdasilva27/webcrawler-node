package br.com.lett.crawlernode.crawlers.ranking.keywords.peru;

import br.com.lett.crawlernode.core.fetcher.methods.ApacheDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PeruWongCrawler extends CrawlerRankingKeywords {

   public PeruWongCrawler(Session session) {
      super(session);
   }

   private Document fetchPage(String url){
      Request request = Request.RequestBuilder.create().setCookies(cookies).setUrl(url).build();
      Response response = new ApacheDataFetcher().get(session, request);
      return Jsoup.parse(response.getBody());
   }

   @Override
   protected void extractProductsFromCurrentPage() throws MalformedProductException {
      this.pageSize = 18;
      this.log("Página " + this.currentPage);

      String url =
         "https://www.wong.pe/busca/?ft=" + this.keywordEncoded + "&PageNumber=" + this.currentPage;

      this.log("Link onde são feitos os crawlers: " + url);
      this.currentDoc = fetchPage(url);
      Elements products = this.currentDoc.select(".product-shelf .product-item");

      if (!products.isEmpty()) {
         if (this.totalProducts == 0) {
            setTotalProducts();
         }
         for (Element e : products) {

            String internalId = crawlInternalId(e);
            String productPid = crawlProductPid(e);
            String productUrl = crawlProductUrl(e);
            String imgUrl = CrawlerUtils.scrapSimplePrimaryImage(e, ".product-item__image-link img", Collections.singletonList("src"), "https", "wongfood.vteximg.com.br");
            String name = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".product-item__name", "title");
            Integer price = crawlPrice(e, 0);
            boolean isAvailable = price != 0;

            RankingProduct productRanking = RankingProductBuilder.create()
               .setUrl(productUrl)
               .setInternalId(internalId)
               .setInternalPid(productPid)
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

      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora "
         + this.arrayProducts.size() + " produtos crawleados");
   }

   @Override
   protected void setTotalProducts() {
      Element totalElement = this.currentDoc.select(".resultado-busca-numero .value").first();

      if (totalElement != null) {
         String text = totalElement.ownText().replaceAll("[^0-9]", "").trim();

         if (!text.isEmpty()) {
            this.totalProducts = Integer.parseInt(text);
         }

         this.log("Total da busca: " + this.totalProducts);
      }
   }

   private String crawlInternalId(Element e) { //.
      String text = CrawlerUtils.scrapStringSimpleInfo(e, ".product-item__data--sku ul li", false);
      return text != null && !text.isEmpty() ? CommonMethods.substring(text, "{\"", "\":", true) : null;

   }

   private String crawlProductPid(Element e) {
      return e.attr("data-id");
   }

   private String crawlProductUrl(Element e) {
      return e.attr("data-uri");
   }

   private Integer crawlPrice(Element e, Integer defaultValue) {
      Integer priceInCents = defaultValue;
      String priceStr = CrawlerUtils.scrapStringSimpleInfo(e, ".product-prices__value--best-price", false);

      if (priceStr != null) {
         String regex = "([0-9]+\\.[0-9]+)";
         Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
         Matcher matcher = pattern.matcher(priceStr);

         if (matcher.find()) {
            priceStr = matcher.group(0);
            Double price = priceStr != null ? Double.parseDouble(priceStr)  : null;
            price = price != null ? price * 100 : null;
            priceInCents = price != null ?  price.intValue() : defaultValue;
         }
      }

      return priceInCents;
   }
}
