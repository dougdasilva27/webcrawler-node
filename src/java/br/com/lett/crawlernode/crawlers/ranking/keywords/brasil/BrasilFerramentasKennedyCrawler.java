package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
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

public class BrasilFerramentasKennedyCrawler extends CrawlerRankingKeywords {
   public BrasilFerramentasKennedyCrawler(Session session) {
      super(session);
      super.fetchMode = FetchMode.FETCHER;
   }

   @Override
   protected void extractProductsFromCurrentPage() throws UnsupportedEncodingException, MalformedProductException {
      this.pageSize = 32;
      this.log("Página " + this.currentPage);
      String url = "https://www.ferramentaskennedy.com.br/busca?q=" + this.keywordEncoded + "&page=" + this.currentPage;
      this.log("URL : " + url);
      this.currentDoc = fetchDocument(url);

      Elements products = this.currentDoc.select("div.col-12 div.card-product");
      pageSize = products.size();

      if (!products.isEmpty()) {
         if (this.totalProducts == 0) setTotalProducts();
         for (Element e : products) {
            String productUrl = CrawlerUtils.scrapUrl(e, "div.picture a", "href", "https", "www.ferramentaskennedy.com.br");
            String internalPid = CrawlerUtils.scrapStringSimpleInfo(e, "span.span-sku", true);
            String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(e,"div.infos div.yv-review-quickreview", "value");
            String name = CrawlerUtils.scrapStringSimpleInfo(e, "h2 a", true);
            String imgUrl = CrawlerUtils.scrapSimplePrimaryImage(e, "div.picture img", Arrays.asList("src"), "https", "static.ferramentaskennedy.com.br");
            Integer price = CrawlerUtils.scrapPriceInCentsFromHtml(e, "div.price p", null, true, ',', session, 0);
            boolean isAvailable = e.selectFirst("div.infos button[disabled]") == null;
            if (isAvailable == false) { price = null; }

            RankingProduct productRanking = RankingProductBuilder.create()
               .setUrl(productUrl)
               .setInternalId(internalId)
               .setInternalPid(internalPid)
               .setName(name)
               .setImageUrl(imgUrl)
               .setPriceInCents(price)
               .setAvailability(isAvailable)
               .build();

            saveDataProduct(productRanking);
         }
      }
      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");
   }
   @Override
   protected void setTotalProducts() {
      this.totalProducts = CrawlerUtils.scrapIntegerFromHtml(currentDoc, "small.results-count span", true, 0);
      this.log("Total: " + this.totalProducts);
   }

}
