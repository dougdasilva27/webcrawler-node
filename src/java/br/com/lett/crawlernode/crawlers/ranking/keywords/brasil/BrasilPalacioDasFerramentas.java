package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.Arrays;

public class BrasilPalacioDasFerramentas extends CrawlerRankingKeywords {

   public BrasilPalacioDasFerramentas(Session session) {
      super(session);
      super.fetchMode = FetchMode.JSOUP;
   }

   @Override
   protected void extractProductsFromCurrentPage() throws MalformedProductException {
      this.log("Página " + this.currentPage);

      String url = "https://palaciodasferramentas.com.br/catalogsearch/result/index/?p=" + this.currentPage + "&q=" + this.keywordEncoded;
      this.log("Link onde são feitos os crawlers: " + url);
      Document doc = fetchDocument(url);

      Elements products = doc.select(".item.product.product-item");

      if (!products.isEmpty()) {
         if (this.totalProducts == 0) {
            this.totalProducts = CrawlerUtils.scrapIntegerFromHtml(doc, "#toolbar-amount .toolbar-number:nth-of-type(3)", true, 0);
         }

         for (Element e : products) {
            String productUrl = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".product-item-photo a", "href");
            String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".price-box.price-final_price", "data-product-id");
            String name = CrawlerUtils.scrapStringSimpleInfo(e, ".product-item-link", false);
            String imgUrl = CrawlerUtils.scrapSimplePrimaryImage(e, "img.product-image-photo", Arrays.asList("srcset"), "https", "palaciodasferramentas.com.br");
            Integer price = CrawlerUtils.scrapPriceInCentsFromHtml(e, "ul > li > div > strong > span.price", null, true, ',', session, null);
            boolean isAvailable = e.selectFirst(".stock.unavailable") == null;

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

      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");
   }

   @Override
   protected boolean hasNextPage() {
      return this.totalProducts > this.arrayProducts.size();
   }
}
