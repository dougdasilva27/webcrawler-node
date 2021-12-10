package br.com.lett.crawlernode.crawlers.ranking.keywords.unitedstates;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class UnitedstatesFlooranddecorCrawler extends CrawlerRankingKeywords {

   protected String storeId = getStoreId();

   public UnitedstatesFlooranddecorCrawler(Session session) {
      super(session);
    //  super.fetchMode = FetchMode.JSOUP;
   }

   protected String getStoreId() {
      return session.getOptions().optString("StoreID");
   }

   @Override
   protected void processBeforeFetch() {
      BasicClientCookie cookie = new BasicClientCookie("StoreID", storeId);
      cookie.setDomain("www.flooranddecor.com");
      cookie.setPath("/");
      this.cookies.add(cookie);
   }


   @Override
   protected Document fetchDocument(String url) {
      Map<String, String> headers = new HashMap<>();

      headers.put("sec-ch-ua", "\"Google Chrome\";v=\"93\", \" Not;A Brand\";v=\"99\", \"Chromium\";v=\"93\"");
      headers.put("accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9");
      headers.put("authority", "www.flooranddecor.com");
      headers.put("user-agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/93.0.4577.82 Safari/537.36");

      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .setHeaders(headers)
         .setCookies(cookies)
         .setProxyservice(
            Arrays.asList(
               ProxyCollection.NETNUT_RESIDENTIAL_US_HAPROXY,
               ProxyCollection.NETNUT_RESIDENTIAL_ES_HAPROXY,
               ProxyCollection.BUY_HAPROXY,
               ProxyCollection.NETNUT_RESIDENTIAL_AR_HAPROXY,
               ProxyCollection.NETNUT_RESIDENTIAL_MX_HAPROXY,
               ProxyCollection.NETNUT_RESIDENTIAL_DE_HAPROXY))
         .build();

      return Jsoup.parse(this.dataFetcher.get(session, request).getBody());
   }

   @Override
   protected void extractProductsFromCurrentPage() throws MalformedProductException {
      String url = "";


      if(arrayProducts.isEmpty()){
         url = "https://www.flooranddecor.com/search?q=sink&search-button=&lang=default&shopThisStore="+ this.storeId;
      }else{
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
   protected boolean hasNextPage(){
      return true;
   }
}
