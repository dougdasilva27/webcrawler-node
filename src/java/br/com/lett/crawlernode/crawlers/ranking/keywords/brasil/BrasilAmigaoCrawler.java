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

public class BrasilAmigaoCrawler extends CrawlerRankingKeywords {
   public BrasilAmigaoCrawler(Session session) {
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() throws UnsupportedEncodingException, MalformedProductException {
      String url = "https://www.amigao.com/" + session.getOptions().optString("locate") + "/catalogsearch/result/index/?q=" + this.currentPage + "&q=" + this.keywordEncoded;
      this.currentDoc = fetchDocument(url);

      Elements products = this.currentDoc.select(".item.product.product-item");

      if (!products.isEmpty()) {

         for (Element product : products) {
            String internalId = extractProductId(product);
            String productUrl = CrawlerUtils.scrapUrl(product, "a.product-item-link", "href", "https", "www.amigao.com");
            String productName = CrawlerUtils.scrapStringSimpleInfo(product, ".product-item-name", false);
            String imageUrl = CrawlerUtils.scrapStringSimpleInfoByAttribute(product, "img.product-image-photo", "src");
            Integer price = CrawlerUtils.scrapPriceInCentsFromHtml(product, ".price-wrapper .price", null, false, ',', session, null);
            Integer priceFrom = CrawlerUtils.scrapPriceInCentsFromHtml(product, ".old-price .price-wrapper .price", null, false, ',', session, price);

            boolean isAvailable = price != null;

            RankingProduct productRanking = RankingProductBuilder.create()
               .setUrl(productUrl)
               .setInternalId(internalId)
               .setName(productName)
               .setPriceInCents(price)
               .setPriceInCents(priceFrom)
               .setAvailability(isAvailable)
               .setImageUrl(imageUrl)
               .build();

            saveDataProduct(productRanking);
            if (this.arrayProducts.size() == productsLimit)
               break;
         }
      }

      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");
   }

   @Override
   protected boolean hasNextPage() {
      return !this.currentDoc.select(".action.next").isEmpty();
   }
   protected String extractProductId(Element product) {
      Element productBox = product.selectFirst(".price-box.price-final_price");
      if (productBox != null) {
         return productBox.attr("data-product-id");
      }
      return null;
   }
}
