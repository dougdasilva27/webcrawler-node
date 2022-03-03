package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;

import java.util.Collections;

public class BrasilPetloveCrawler extends CrawlerRankingKeywords {

   public BrasilPetloveCrawler(Session session) {
      super(session);
   }

   private static final String HOME_PAGE = "https://www.petlove.com.br/";

   @Override
   protected void extractProductsFromCurrentPage() throws MalformedProductException {
      this.pageSize = 20;

      this.log("Página " + this.currentPage);

      String url = "https://www.petlove.com.br/busca?q=" + this.keywordEncoded + "&page=" + this.currentPage;
      this.log("Link onde são feitos os crawlers: " + url);

      this.currentDoc = fetchDocument(url);

      Elements products = this.currentDoc.select("#shelf-loop .catalog-list-item");

      if (!products.isEmpty()) {
         if (this.totalProducts == 0) {
            setTotalProducts();
         }

         for (Element e : products) {
            String internalId = scrapInternalId(e);
            String productUrl = crawlProductUrl(e);
            String name = CrawlerUtils.scrapStringSimpleInfo(e, ".product-name", true);
            String imgUrl = scrapImage(e);
            Integer price = CrawlerUtils.scrapPriceInCentsFromHtml(e, ".catalog-list-price:not(.catalog-list-price-subscription)", null, true, ',', session, 0);
            boolean isAvailable = price != 0;

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

   private String scrapImage(Element e) {
      String image = CrawlerUtils.scrapSimplePrimaryImage(e, ".catalog-list-image img", Collections.singletonList("src"), "https", "petlove.com.br");

      if(image != null && !image.isEmpty()) {
         image = image.replace("/small/", "/large/");
      }

      return image;
   }

   @Override
   protected void setTotalProducts() {
      Element totalElement = this.currentDoc.select(".sort-results").first();

      if (totalElement != null) {
         String total = totalElement.ownText().replaceAll("[^0-9]", "").trim();

         if (!total.isEmpty()) {
            this.totalProducts = Integer.parseInt(total);
         }

         this.log("Total da busca: " + this.totalProducts);
      }
   }

   private String crawlInternalPid(Element e) {
      String internalPid = null;

      Element sku = e.select("span[itemprop=sku]").first();
      if (sku != null) {
         internalPid = sku.ownText();
      }

      return internalPid;
   }

   private String crawlProductUrl(Element e) {
      Element url = e.selectFirst(".catalog-info-product > a");
      if (url != null) {
         String productUrl = url.attr("href");

         if (!productUrl.contains("petlove.com")) {
            productUrl = (HOME_PAGE + productUrl).replace("br//", "br/");
         }

         return productUrl;
      }
      return null;
   }


   private String scrapInternalId(Element e) {
      Element url = e.selectFirst(".catalog-info-product > a");
      if (url != null) {
         String productURL = url.attr("href");
         return CommonMethods.getLast(productURL.split("sku="));
      }
      return null;
   }
}
