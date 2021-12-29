package br.com.lett.crawlernode.crawlers.ranking.keywords.unitedstates;

import br.com.lett.crawlernode.core.fetcher.DynamicDataFetcher;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.Cookie;

public class UnitedstatesFlooranddecorCrawler extends CrawlerRankingKeywords {

   protected String storeId = getStoreId();

   public UnitedstatesFlooranddecorCrawler(Session session) {
      super(session);
   }

   protected String getStoreId() {
      return session.getOptions().optString("StoreID");
   }

   @Override
   protected void processBeforeFetch() {
      Cookie cookie = new Cookie.Builder("StoreID", storeId)
         .domain("www.flooranddecor.com")
         .path("/")
         .isHttpOnly(true)
         .isSecure(false)
         .build();
      this.cookiesWD.add(cookie);
   }


   @Override
   protected Document fetchDocument(String url) {
      Document doc = null;
      try {
         webdriver = DynamicDataFetcher.fetchPageWebdriver(url, ProxyCollection.NETNUT_RESIDENTIAL_US_HAPROXY, session, this.cookiesWD, "https://www.flooranddecor.com/");
         webdriver.waitForElement("div.l-plp-grid_item-wrapper", 20000);

         doc = Jsoup.parse(webdriver.getCurrentPageSource());

         webdriver.terminate();

      } catch (Exception e) {
         Logging.printLogInfo(logger, session, CommonMethods.getStackTrace(e));
         webdriver.terminate();

      }

      return doc;
   }


   @Override
   protected void extractProductsFromCurrentPage() throws MalformedProductException {
      String url = "";


      if (arrayProducts.isEmpty()) {
         url = "https://www.flooranddecor.com/search?q=sink&search-button=&lang=default&shopThisStore=" + this.storeId;
      } else {
         url = "https://www.flooranddecor.com/on/demandware.store/Sites-floor-decor-Site/default/SearchRedesign-UpdateGrid?q="
            + this.keywordEncoded
            + "&start="
            + this.arrayProducts.size() + "&sz=24&tab=search-tabs-products&shopThisStore="
            + this.storeId + "&ajax=true";
      }

      this.log("Link onde são feitos os crawlers: " + url);

      this.currentDoc = fetchDocument(url);

      Elements results = this.currentDoc.select("div.l-plp-grid_item-wrapper");

      if (results != null && !results.isEmpty()) {
         if (this.totalProducts == 0) {
            setTotalProducts();
         }
         if (currentPage == 1) {
            String productCount = CrawlerUtils.scrapStringSimpleInfoByAttribute(this.currentDoc, "li[aria-controls=search-tabs-products]", "data-count");
            this.totalProducts = Integer.parseInt(productCount);
         }

         for (Element prod : results) {
            String internalPid = CrawlerUtils.scrapStringSimpleInfoByAttribute(prod, "article.l-plp-grid_item", "data-analytics-product-impression-id");
            String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(prod, "article.l-plp-grid_item", "data-analytics-product-impression-sku");
            String name = CrawlerUtils.scrapStringSimpleInfoByAttribute(prod, "article.l-plp-grid_item", "data-analytics-product-impression-name");
            String productUrl = "https://www.flooranddecor.com" + CrawlerUtils.scrapStringSimpleInfoByAttribute(prod, "figure.b-product_tile-figure a", "href");
            String imageUrl = "https:" + CrawlerUtils.scrapStringSimpleInfoByAttribute(prod, "img.b-product_tile-figure_img.m-main-img.data-js-main-img.m-active", "data-src");
            int price = CrawlerUtils.scrapPriceInCentsFromHtml(prod, "span[data-js-product-price]", null, true, '.', session, 0);
            boolean isAvailable = price != 0;

            RankingProduct productRanking = RankingProductBuilder.create()
               .setUrl(productUrl)
               .setInternalId(internalId)
               .setInternalPid(internalPid)
               .setName(name)
               .setImageUrl(imageUrl)
               .setPriceInCents(price)
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
   protected void setTotalProducts() {
      this.totalProducts = CrawlerUtils.scrapIntegerFromHtml(this.currentDoc, ".b-sss_tabs-nav_numbers", true, 0);
   }
}
