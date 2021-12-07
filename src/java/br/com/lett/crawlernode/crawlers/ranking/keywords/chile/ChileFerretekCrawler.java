package br.com.lett.crawlernode.crawlers.ranking.keywords.chile;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.UnsupportedEncodingException;
import java.util.*;

public class ChileFerretekCrawler extends CrawlerRankingKeywords {
   public ChileFerretekCrawler(Session session) {
      super(session);
   }
   @Override
   protected void extractProductsFromCurrentPage() throws UnsupportedEncodingException, MalformedProductException {
      this.pageSize = 20;

      this.log("Página : " + this.currentPage);

      String url = "https://herramientas.cl/shop/page/" + currentPage + "?search=" + keywordEncoded;
      this.log("URL : " + url);
      this.currentDoc = fetchDocument(url);

      Elements products = this.currentDoc.select(".oe_product_cart");

      if(!products.isEmpty()) {
         for(Element product : products) {
            String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(product, "button", "data-product-id");
            String productUrl = "https://herramientas.cl" + CrawlerUtils.scrapStringSimpleInfoByAttribute(product, ".card-body > a", "href");
            String name = CrawlerUtils.scrapStringSimpleInfo(product, ".card-body h6", true);
            String imgUrl = CrawlerUtils.scrapSimplePrimaryImage(product, ".card-body img", Collections.singletonList("src"), "https", "herramientas.cl");
            Integer price = CrawlerUtils.scrapPriceInCentsFromHtml(product, ".oe_currency_value", null, true, ',', session, 0);
            boolean isAvailable = checkIfIsAvailable(product);

            RankingProduct productRanking = RankingProductBuilder.create()
               .setUrl(productUrl)
               .setInternalId(internalId)
               .setInternalPid(internalId)
               .setImageUrl(imgUrl)
               .setName(name)
               .setPriceInCents(price)
               .setAvailability(isAvailable)
               .build();

            saveDataProduct(productRanking);
         }

      } else {
         this.result = false;
         this.log("Keyword sem resultados!");
      }

      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora "
         + this.arrayProducts.size() + " produtos crawleados");
   }

   private boolean checkIfIsAvailable(Element product) {
      return product.select(".out-stock-msg").isEmpty();
   }

   @Override
   protected boolean hasNextPage() {
      return !this.currentDoc.select(".pagination .page-item:last-child").hasClass("disabled");
   }
}
