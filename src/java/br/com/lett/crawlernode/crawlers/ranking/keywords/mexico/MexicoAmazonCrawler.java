package br.com.lett.crawlernode.crawlers.ranking.keywords.mexico;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.crawlers.extractionutils.core.AmazonScraperUtils;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.HashMap;

public class MexicoAmazonCrawler extends CrawlerRankingKeywords {

   public MexicoAmazonCrawler(Session session) {
      super(session);
   }

   private static final String HOME_PAGE = "https://www.amazon.com.mx";
   private String nextPageUrl;
   private String storeId;

   private AmazonScraperUtils amazonScraperUtils = new AmazonScraperUtils(logger, session);

   @Override
   protected void extractProductsFromCurrentPage() {
      this.pageSize = 20;
      this.log("Página " + this.currentPage);

      String url;

      if (this.currentPage == 1) {
         url = "https://www.amazon.com.mx/s?k=" + this.keywordEncoded.replace(" ", "+") + "&__mk_es_MX=%C3%85M%C3%85%C5%BD%C3%95%C3%91&ref=nb_sb_noss";
      } else {
         url = "https://www.amazon.com.mx/s?k=" + this.keywordEncoded.replace(" ", "+") + "&page=" + this.currentPage +"&__mk_es_MX=%C3%85M%C3%85%C5%BD%C3%95%C3%91&qid=" + this.storeId + "&ref=sr_pg_" + this.currentPage;
      }
      this.log("Link onde são feitos os crawlers: " + url);

      this.currentDoc = Jsoup.parse(amazonScraperUtils.fetchPage(url, new HashMap<>(), cookies, dataFetcher));
      this.storeId = CrawlerUtils.scrapStringSimpleInfoByAttribute(this.currentDoc, "form > input[name=qid]", "value");

      Elements products = this.currentDoc.select(".s-result-list .s-result-item");
      Element result = this.currentDoc.select("#noResultsTitle").first();

      if (!products.isEmpty() && result == null) {

         for (Element e : products) {

            String internalPid = crawlInternalPid(e);
            String internalId = internalPid;
            String productUrl = crawlProductUrl(internalPid);

            if (internalPid.isEmpty()) {
               continue;
            }

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

      return this.currentDoc.select(".s-pagination-item.s-pagination-next.s-pagination-disabled").isEmpty();

   }


   private String crawlInternalPid(Element e) {
      return e.attr("data-asin");
   }

   private String crawlProductUrl(String id) {
      return HOME_PAGE + "/dp/" + id;
   }

}
