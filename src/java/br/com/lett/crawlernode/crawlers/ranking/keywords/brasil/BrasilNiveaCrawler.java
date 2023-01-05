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
import java.util.Arrays;

public class BrasilNiveaCrawler extends CrawlerRankingKeywords {
   public BrasilNiveaCrawler(Session session) {
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() throws UnsupportedEncodingException, MalformedProductException {
      String url = "https://www.nivea.com.br/catalogsearch/result/?q=" + this.keywordEncoded;
      // site não possui paginação
      this.currentDoc = fetchDocument(url);
      Elements products = this.currentDoc.select(".item.product.product-item");
      if (!products.isEmpty()) {
         if (totalProducts == 0) {
            setTotalProducts();
         }
         for (Element e : products) {
            String productUrl = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".product.photo.product-item-photo", "href");
            String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(e,".price-box.price-final_price", "data-product-id");
            String name = scraperName(e);
            String imgUrl = CrawlerUtils.scrapSimplePrimaryImage(e, ".product-image-photo.lazyload", Arrays.asList("data-src"), "", "");
            Integer price = CrawlerUtils.scrapPriceInCentsFromHtml(e, ".price", null, false, ',', session, 0);
            boolean isAvailable = CrawlerUtils.scrapStringSimpleInfo(e, ".stock.unavailable", false) == null;
            RankingProduct productRanking = RankingProductBuilder.create()
               .setUrl(productUrl)
               .setInternalId(internalId)
               .setName(name)
               .setImageUrl(imgUrl)
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

   }

   private String scraperName(Element e) {
      String name = CrawlerUtils.scrapStringSimpleInfo(e, ".product-item-link", false);
      String description = CrawlerUtils.scrapStringSimpleInfo(e, ".inner-wrapper", false);
      if(description != null){
         return  name + " " + description;
      }
      return name;
   }

}
