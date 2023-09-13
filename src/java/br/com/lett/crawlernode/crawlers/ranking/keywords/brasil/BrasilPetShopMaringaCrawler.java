package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BrasilPetShopMaringaCrawler extends CrawlerRankingKeywords {
   public BrasilPetShopMaringaCrawler(Session session) {
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() throws UnsupportedEncodingException, MalformedProductException {
      this.pageSize = 30;
      this.log("Página " + this.currentPage);

      String url = "https://www.petshopmaringa.net/index.php?route=product/search&search=" + this.keywordEncoded + "&page=" + this.currentPage;

      this.log("Link onde são feitos os crawlers: " + url);
      this.currentDoc = fetchDocument(url);
      Elements products = this.currentDoc.select(".product-layout");

      if (!products.isEmpty()) {
         if (this.totalProducts == 0)
            setTotalProducts();
         for (Element e : products) {
            String productUrl = CrawlerUtils.scrapUrl(e, ".image a", "href", "https", "www.petshopmaringa.net");
            String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".product-thumb", "data-product");
            String name = CrawlerUtils.scrapStringSimpleInfo(e, "h4.product-name a", true);
            String imageUrl = CrawlerUtils.scrapSimplePrimaryImage(e, ".image .img-responsive", Collections.singletonList("data-original"), "https", "www.petshopmaringa.net");
            Integer price = crawlPrice(e);
            boolean isAvailable = price != null;

            RankingProduct productRanking = RankingProductBuilder.create()
               .setUrl(productUrl)
               .setInternalId(internalId)
               .setName(name)
               .setPriceInCents(price)
               .setAvailability(isAvailable)
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

   private Integer crawlPrice(Element e) {
      Integer price = CrawlerUtils.scrapPriceInCentsFromHtml(e, ".caption .price", null, true, ',', session, null);
      if (price == null) {
         return CrawlerUtils.scrapPriceInCentsFromHtml(e, ".caption .price-new", null, true, ',', session, null);
      }
      return price;
   }

   @Override
   protected void setTotalProducts() {
      String regex = "de ([0-9]*) \\(";
      String totalProduct = CrawlerUtils.scrapStringSimpleInfo(this.currentDoc, "#content > div:nth-child(9) > div.col-sm-6.text-right", true);
      if (totalProduct != null) {
         Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
         Matcher matcher = pattern.matcher(totalProduct);
         if (matcher.find()) {
            this.totalProducts = Integer.parseInt(matcher.group(1));
         } else {
            this.totalProducts = 0;
         }
      }
      this.log("Total: " + this.totalProducts);
   }
}
