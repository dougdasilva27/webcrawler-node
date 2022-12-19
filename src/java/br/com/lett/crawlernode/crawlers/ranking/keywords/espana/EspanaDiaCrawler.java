package br.com.lett.crawlernode.crawlers.ranking.keywords.espana;

import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;

public class EspanaDiaCrawler extends CrawlerRankingKeywords {

   public EspanaDiaCrawler(Session session) {
      super(session);
   }

   private static final String HOST = "www.dia.es";

   @Override
   protected void extractProductsFromCurrentPage() throws MalformedProductException {

      this.pageSize = 24;
      this.log("Página " + this.currentPage);

      String url = "https://" + HOST + "/compra-online/search?q=" + this.keywordWithoutAccents + "%3Arelevance&page=" + (this.currentPage - 1) + "&disp=";

      this.log("Link onde são feitos os crawlers: " + url);
      this.currentDoc = fetchDocument(url);
      Elements products = this.currentDoc.select("#productgridcontainer .product-list__item .prod_grid");

      if (!products.isEmpty()) {
         for (Element e : products) {
            String internalId = e.attr("data-productcode");
            String productUrl = CrawlerUtils.scrapUrl(e, "a", "href", "https://", HOST);
            String name = CrawlerUtils.scrapStringSimpleInfo(e, ".details", true);
            Integer priceInCents = e.selectFirst(".price_container > .price > span") != null ?
               CrawlerUtils.scrapPriceInCentsFromHtml(e, ".price_container > .price > span", null, true, ',', session, null) :
               CrawlerUtils.scrapPriceInCentsFromHtml(e, ".price_container > .price", null, true, ',', session, null);
            boolean isAvailable = priceInCents != null;

            RankingProduct productRanking = RankingProductBuilder.create()
               .setUrl(productUrl)
               .setInternalId(internalId)
               .setName(name)
               .setPriceInCents(priceInCents)
               .setAvailability(isAvailable)
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

   @Override
   protected boolean hasNextPage() {
      Element nextButton = this.currentDoc.selectFirst(".btn-pager--next");

      return nextButton != null && !nextButton.hasClass("disabled");
   }

}
