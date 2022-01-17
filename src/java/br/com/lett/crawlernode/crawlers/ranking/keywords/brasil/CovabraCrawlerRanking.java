package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.UnsupportedEncodingException;
import java.util.Collections;

public class CovabraCrawlerRanking extends CrawlerRankingKeywords {

   public CovabraCrawlerRanking(Session session) {
      super(session);
   }

   @Override
   protected void processBeforeFetch() {
      String store = session.getOptions().optString("website", "");
      BasicClientCookie cookie = new BasicClientCookie("website", store);
      cookie.setDomain("www.covabra.com.br");
      cookie.setPath("/");
      this.cookies.add(cookie);
   }

   @Override
   protected void extractProductsFromCurrentPage() throws UnsupportedEncodingException, MalformedProductException {
      this.pageSize = 36;
      this.log("Página " + this.currentPage);

      String url = "https://www.covabra.com.br/catalogsearch/result/index/?p="+this.currentPage+"&q="+this.keywordEncoded;
      this.log("Link onde são feitos os crawlers: " + url);

      this.currentDoc = fetchDocument(url, this.cookies);

      Elements products = this.currentDoc.select(".product-item-info");
      if (!products.isEmpty()) {
         for (Element product : products) {
            String internalPid = CrawlerUtils.scrapStringSimpleInfoByAttribute(product, "input[name=product]", "value");
            String internalId = internalPid;
            String productUrl = CrawlerUtils.scrapStringSimpleInfoByAttribute(product, ".product-item-link", "href");
            String name = CrawlerUtils.scrapStringSimpleInfo(product, ".product-item-name", false);
            String imgUrl = CrawlerUtils.scrapSimplePrimaryImage(product, ".product-image-photo", Collections.singletonList("src"), "https", "covabra.com.br");
            Integer price = CrawlerUtils.scrapPriceInCentsFromHtml(product, "span[data-price-type=finalPrice]", null, false, ',', session, 0);
            boolean isAvailable = price != 0;

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

   @Override
   public boolean hasNextPage() {
      return this.currentDoc.selectFirst(".pages-item-next")  != null;
   }
}
