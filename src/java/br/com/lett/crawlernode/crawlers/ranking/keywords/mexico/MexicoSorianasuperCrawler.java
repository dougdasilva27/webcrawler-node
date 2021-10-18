package br.com.lett.crawlernode.crawlers.ranking.keywords.mexico;

import br.com.lett.crawlernode.core.fetcher.DynamicDataFetcher;
import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import br.com.lett.crawlernode.util.Logging;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.WebDriverException;

public class MexicoSorianasuperCrawler extends CrawlerRankingKeywords {

   public MexicoSorianasuperCrawler(Session session) {
      super(session);
      super.fetchMode = FetchMode.APACHE;
   }

   private static final String PROTOCOL = "https://";
   private static int pages = 0;
   private static final String DOMAIN = "superentucasa.soriana.com/Default.aspx";


   @Override
   protected void extractProductsFromCurrentPage() throws MalformedProductException {
      this.log("Página " + this.currentPage);


      String url = "https://www.soriana.com/on/demandware.store/Sites-Soriana-Site/default/Search-UpdateGrid?q="+this.keywordEncoded+"&start=" + (this.currentPage - 1) * 12 + "&sz=12";

      this.log("Link onde são feitos os crawlers: " + url);
      this.currentDoc = fetchDocument(url);

      Elements products = this.currentDoc.select("body > div[class*=product-tile]");

      if (!products.isEmpty()) {
         for (Element e : products) {


            String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".product", "data-pid");
            String internalPid = internalId;
            String productUrl = CrawlerUtils.scrapUrl(e, ".product a", "href", "https", "www.soriana.com/");
            String name = CrawlerUtils.scrapStringSimpleInfo(e, ".product .product-tile--link", true);
            int price = CommonMethods.doublePriceToIntegerPrice(CrawlerUtils.scrapDoublePriceFromHtml(e, " .product .value", "content", true, '.', session), 0);
            boolean isAvailable = price != 0;

            //New way to send products to save data product
            RankingProduct productRanking = RankingProductBuilder.create()
               .setUrl(productUrl)
               .setInternalId(internalId)
               .setInternalPid(internalPid)
               .setName(name)
               .setPriceInCents(price)
               .setAvailability(isAvailable)
               .build();

            saveDataProduct(productRanking);

         }
      } else {
         this.result = false;
         this.log("Keyword sem resultado!");
      }

      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");
   }

   @Override
   protected boolean hasNextPage() {

      return this.currentDoc.selectFirst(".show-more") != null;

   }

   private String crawlInternalId(Element e) {
      String internalId = null;
      Element id = e.selectFirst("input[type=hidden][name=s]");

      if (id != null) {
         internalId = id.val();
      }

      return internalId;
   }
}
