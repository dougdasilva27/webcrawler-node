package br.com.lett.crawlernode.crawlers.ranking.keywords.belohorizonte;

import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.UnsupportedEncodingException;
import java.util.Collections;

public class BelohorizonteBernardaoCrawler extends CrawlerRankingKeywords {
   public BelohorizonteBernardaoCrawler(Session session) {
      super(session);
   }

   @Override
   public void extractProductsFromCurrentPage() throws UnsupportedEncodingException, MalformedProductException {
      this.pageSize = 20;
      this.log("Página " + this.currentPage);

      String url = "https://www.bernardaoemcasa.com.br/catalogsearch/result/index/?p=" + this.currentPage + "&q=" + this.keywordEncoded;
      this.log("Link onde são feitos os crawlers: " + url);

      this.currentDoc = fetchDocument(url, this.cookies);

      Elements products = this.currentDoc.select(".product-item-info");
      if (!products.isEmpty()) {
         for (Element product : products) {
            String internalPid = scrapInternalPid(product);
            String internalId = null;
            String productUrl = CrawlerUtils.scrapStringSimpleInfoByAttribute(product, "a.product-image", "href");
            String name = CrawlerUtils.scrapStringSimpleInfo(product, ".product-name", false);
            String imgUrl = scrapImageUrl(product);
            Integer price = scrapPrice(product);
            boolean isAvailable = product.selectFirst(".actions .out-of-stock") == null;

            RankingProduct productRanking = RankingProductBuilder.create()
               .setUrl(productUrl)
               .setInternalId(internalId)
               .setInternalPid(internalPid)
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
         this.log("Keyword sem resultados!");
      }

      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");
   }

   private String scrapImageUrl(Element product) {
      String imageUrl = CrawlerUtils.scrapSimplePrimaryImage(product, ".product-image .alternative-img", Collections.singletonList("src"), "https", "bernardaoemcasa.com.br");
      if (imageUrl != null && imageUrl.contains("placeholder")) {
         imageUrl = null;
      }
      return imageUrl;
   }

   private Integer scrapPrice(Element product) {
      Integer price = CrawlerUtils.scrapPriceInCentsFromHtml(product, ".price-box > .special-price > .price", null, false, ',', session, null);
      if (price == null) {
         price = CrawlerUtils.scrapPriceInCentsFromHtml(product, ".price-box .price", null, false, ',', session, null);
      }
      return price;
   }

   private String scrapInternalPid(Element product) {
      String internalPid = CrawlerUtils.scrapStringSimpleInfoByAttribute(product, ".img-responsive[id*=\"product-collection-image\"]", "id");
      if (internalPid != null && !internalPid.isEmpty()) {
         internalPid = CommonMethods.getLast(internalPid.split("-"));
      }
      return internalPid;
   }
}
