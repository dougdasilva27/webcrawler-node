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

public class BrasilMagodriveCrawler extends CrawlerRankingKeywords {

   public BrasilMagodriveCrawler(Session session) {
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() throws UnsupportedEncodingException, MalformedProductException {
      this.log("Página " + this.currentPage);
      this.pageSize = 32;
      
      String url = "https://www.magodrive.com.br/pesquisa?" + "pg=" + currentPage + "&t=" + this.keywordEncoded;
      this.log("Link onde são feitos os crawlers: " + url);
      this.currentDoc = fetchDocument(url);

      Elements products = this.currentDoc.select(".wd-browsing-grid-list ul li[class]");
      if (!products.isEmpty()) {
         for (Element product : products) {

            String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(product, ".btn-original input[name=\"Products[0].SkuID\"]", "value");
            String internalPid = scrapeInternalPid(product);
            String productUrl = CrawlerUtils.scrapUrl(product, "h3.name a", "href", "https", "www.supermago.com.br");
            String name = CrawlerUtils.scrapStringSimpleInfo(product, "h3.name", false);
            String imgUrl = CrawlerUtils.scrapSimplePrimaryImage(product, ".current-img", Collections.singletonList("data-src"), "https", "d2ng48q17pwd8f.cloudfront.net");
            Integer price = CrawlerUtils.scrapPriceInCentsFromHtml(product, ".priceContainer .sale-price", null, false, ',', session, null);
            boolean isAvailable = product.selectFirst(".bt-notifyme") == null;

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

      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora "
         + this.arrayProducts.size() + " produtos crawleados");
   }

   private String scrapeInternalPid(Element product) {
      String internalPid = CrawlerUtils.scrapStringSimpleInfoByAttribute(product, ".btn-original input[name=\"Products[0].ProductID\"]", "value");

      if (internalPid == null || internalPid.isEmpty()) {
         internalPid = CrawlerUtils.scrapStringSimpleInfoByAttribute(product, "> div", "data-product-id");
      }

      return internalPid;
   }

   @Override
   public boolean hasNextPage() {
      return this.currentDoc.selectFirst(".next-page .page-next") != null;
   }
}
