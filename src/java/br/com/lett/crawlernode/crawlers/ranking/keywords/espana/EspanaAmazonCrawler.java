package br.com.lett.crawlernode.crawlers.ranking.keywords.espana;

import java.util.HashMap;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.AmazonScraperUtils;
import br.com.lett.crawlernode.util.CrawlerUtils;

public class EspanaAmazonCrawler extends CrawlerRankingKeywords {

   public EspanaAmazonCrawler(Session session) {
      super(session);
      super.fetchMode = FetchMode.FETCHER;
   }

   private AmazonScraperUtils amazonScraperUtils = new AmazonScraperUtils(logger, session);

   @Override
   protected void processBeforeFetch() {
      super.processBeforeFetch();

      this.cookies = this.amazonScraperUtils.handleCookiesBeforeFetch("https://www.amazon.es/", cookies, dataFetcher);
   }

   @Override
   protected void extractProductsFromCurrentPage() {
      this.pageSize = 20;
      this.log("Página " + this.currentPage);

      String url = "https://www.amazon.es/s?ref=sr_pg_" + this.currentPage + "&page=" + this.currentPage +
            "&k=" + this.keywordEncoded + "&qid=1591228280";
      this.log("Link onde são feitos os crawlers: " + url);

      this.currentDoc = Jsoup.parse(amazonScraperUtils.fetchPage(url, new HashMap<>(), cookies, dataFetcher));

      Elements products = this.currentDoc.select(".s-result-list .s-result-item:not([data-asin=\"\"])");
      Element result = this.currentDoc.select("#noResultsTitle").first();

      if (!products.isEmpty() && result == null) {
         if (this.totalProducts == 0) {
            setTotalProducts();
         }

         for (Element e : products) {

            String internalPid = crawlInternalPid(e);
            String internalId = internalPid;
            String productUrl = CrawlerUtils.scrapUrl(e, ".a-link-normal", "href", "https", "www.amazon.es");

            saveDataProduct(internalId, internalPid, productUrl);

            this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + internalPid + " - Url: " + productUrl);
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
      return this.currentDoc.select(".a-disabled.a-last").isEmpty();
   }

   @Override
   protected void setTotalProducts() {
      JSONObject totalJson = CrawlerUtils.selectJsonFromHtml(currentDoc, "script[data-a-state=\"{\"key\":\"s-metadata\"}\"]", null, null, true);

      if (totalJson.has("totalResultCount")) {
         Object obj = totalJson.get("totalResultCount");

         if (obj instanceof Integer) {
            this.totalProducts = (Integer) obj;
            this.log("Total da busca: " + this.totalProducts);
         }
      }
   }

   private String crawlInternalPid(Element e) {
      return e.attr("data-asin");
   }
}