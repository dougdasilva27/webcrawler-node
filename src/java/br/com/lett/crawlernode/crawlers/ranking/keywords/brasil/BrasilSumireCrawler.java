package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BrasilSumireCrawler extends CrawlerRankingKeywords {
   public BrasilSumireCrawler(Session session) {
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() throws UnsupportedEncodingException, MalformedProductException {
      this.pageSize = 36;
      this.log("Página " + this.currentPage);
      String url = "https://www.perfumariasumire.com.br/catalogsearch/result/index/?p=" + this.currentPage + "&q=" + this.keywordEncoded;
      this.log("Link onde são feitos os crawlers: " + url);

      this.currentDoc = fetchDocument(url);
      Elements products = this.currentDoc.select(".products.wrapper.grid.products-grid > ol > li");

      if (products.size() > 0) {

         if (this.totalProducts == 0) {
            this.totalProducts = CrawlerUtils.scrapSimpleInteger(this.currentDoc, "#toolbar-amount > span.toolbar-amount-top > span:nth-child(3)", false);
            this.log("Total da busca: " + this.totalProducts);
         }

         for (Element e : products) {

            String internalId = crawlId(e);
            Boolean isAvailable = CrawlerUtils.scrapStringSimpleInfo(e, ".product-item-inner > div > div.actions-primary > div > span", false) == null;
            String urlProduct = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".product.details.product-item-details > .product.name.product-item-name > a", "href");
            String name = CrawlerUtils.scrapStringSimpleInfo(e, ".product.details.product-item-details > .product.name.product-item-name > a", false);
            String imgUrl = CrawlerUtils.scrapSimplePrimaryImage(e, " .product-item-info > a > span > .product-image-wrapper > img", Arrays.asList("src"), "https", "://www.perfumariasumire.com.br/");
            Integer price = null;
            if (isAvailable == true) {
               price = CrawlerUtils.scrapPriceInCentsFromHtml(e, "#product-price-" + internalId, null, false, ',', session, null);
            }
            RankingProduct productRanking = RankingProductBuilder.create()
               .setUrl(urlProduct)
               .setInternalId(internalId)
               .setName(name)
               .setImageUrl(imgUrl)
               .setPriceInCents(price)
               .setAvailability(isAvailable)
               .build();
            saveDataProduct(productRanking);

            if (this.arrayProducts.size() == productsLimit) break;
         }

      } else {
         this.result = false;
         this.log("Keyword sem resultado!");
      }

      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");
   }

   private String crawlId(Element e) {
      String finalId = null;
      String holder = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".product-item-inner > div > div.actions-secondary > a", "data-post");
      String regex = "product\\\":(.*),\\\"uenc";
      final Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
      final Matcher matcher = pattern.matcher(holder);
      while (matcher.find()) {
         finalId = matcher.group(1);
      }
      return finalId;
   }

   @Override
   protected boolean hasNextPage() {
      boolean finalResponse = false;
      String nextPage = CrawlerUtils.scrapStringSimpleInfo(currentDoc, "#amasty-shopby-product-list > div:first-child > div.pages > ul > li.item.pages-item-next > a", false);
      if (nextPage != null) {
         finalResponse = true;
      }
      return finalResponse;
   }
}
