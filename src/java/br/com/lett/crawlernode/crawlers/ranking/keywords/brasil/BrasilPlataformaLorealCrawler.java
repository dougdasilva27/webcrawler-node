package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import models.Offers;
import models.RatingsReviews;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.jsoup.nodes.Element;

import java.io.UnsupportedEncodingException;
import java.util.List;

public class BrasilPlataformaLorealCrawler extends CrawlerRankingKeywords {
   public BrasilPlataformaLorealCrawler(Session session) {
      super(session);
   }

   private String host = this.session.getOptions().optString("host");

   @Override
   protected void extractProductsFromCurrentPage() throws UnsupportedEncodingException, MalformedProductException {
      String url = "https://" + host + "/procurar?q=" + this.keywordEncoded + "&start=0&sz=" + this.productsLimit;
      this.currentDoc = fetchDocument(url);

      Elements products = this.currentDoc.select(".c-product-grid .c-product-tile__wrapper");
      if (!products.isEmpty()) {
         if (this.totalProducts == 0) setTotalProducts();
         for (Element product : products) {
            String internalPid = CommonMethods.getLast(CrawlerUtils.scrapStringSimpleInfoByAttribute(product, ".c-product-tile .c-product-image", "href").split("/")).replaceAll(".html", "");
            String productUrl = CrawlerUtils.scrapUrl(product, ".c-product-tile .c-product-tile__figure .c-product-image__link", "href", "https", host);
            String productName = getNameWithDetail(
               CrawlerUtils.scrapStringSimpleInfo(product, ".c-product-tile .c-product-tile__caption .c-product-tile__name", false),
               CrawlerUtils.scrapStringSimpleInfo(product, ".c-product-tile__variations-group .c-product-tile__variations-single-text span", false)
            );
            if (price == null) {
               Element spanSelected = product.selectFirst(".c-product-price span:nth-child(4)");
               price = CrawlerUtils.scrapPriceInCentsFromHtml(spanSelected, ".c-product-price__value", null, false, ',', session, null);
            }
            String disable = CrawlerUtils.scrapStringSimpleInfoByAttribute(product, ".c-product-tile .c-product-tile__caption .c-product-tile__variations-group .c-carousel ul li a", "aria-disabled");
            boolean isAvailable = disable == null;

            RankingProduct productRanking = RankingProductBuilder.create()
               .setUrl(productUrl)
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

   private String getNameWithDetail(String name, String info) {
      if (info == null) return name;
      if (name.matches("(?i).*\\d+(ml|g|mg).*")) {
         return name.replaceAll("\\d+(ml|g|mg|ML|G|MG)", info);
      } else {
         return name + " " + info;
      }
}
