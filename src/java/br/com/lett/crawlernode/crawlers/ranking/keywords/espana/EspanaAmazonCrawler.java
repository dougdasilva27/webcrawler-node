package br.com.lett.crawlernode.crawlers.ranking.keywords.espana;

import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.crawlers.extractionutils.core.AmazonScraperUtils;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.HashMap;

public class EspanaAmazonCrawler extends CrawlerRankingKeywords {
   public EspanaAmazonCrawler(Session session) {
      super(session);
   }

   private static final String HOME_PAGE = "https://www.amazon.es";
   private String nextPageUrl;
   private String storeId;

   private AmazonScraperUtils amazonScraperUtils = new AmazonScraperUtils(logger, session);

   @Override
   protected void extractProductsFromCurrentPage() throws MalformedProductException {
      this.pageSize = 20;
      this.log("Página " + this.currentPage);

      String url;

      if (this.currentPage == 1) {
         url = "https://www.amazon.es/s?k=" + this.keywordEncoded.replace(" ", "+") + "&__mk_es_ES=%C3%85M%C3%85%C5%BD%C3%95%C3%91&ref=nb_sb_noss";
      } else {
         url = "https://www.amazon.es/s?k=" + this.keywordEncoded.replace(" ", "+") + "&page=" + this.currentPage + "&__mk_es_ES=%C3%85M%C3%85%C5%BD%C3%95%C3%91&qid=" + this.storeId + "&ref=sr_pg_" + this.currentPage;
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
            String imageUrl = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, "img.s-image", "src");
            String name = CrawlerUtils.scrapStringSimpleInfo(e, ".a-color-base.a-text-normal", true);
            Integer price = CrawlerUtils.scrapPriceInCentsFromHtml(e, "span.a-price .a-offscreen", null, true, ',', session, null);
            String sponsored = CrawlerUtils.scrapStringSimpleInfo(e, ".s-label-popover-default > .a-color-secondary", true);
            boolean isSpondored = sponsored != null;
            boolean isAvailable = price != null;


            if (internalPid.isEmpty()) {
               continue;
            }

            RankingProduct objProducts = RankingProductBuilder.create()
               .setUrl(productUrl)
               .setImageUrl(imageUrl)
               .setName(name)
               .setPriceInCents(price)
               .setInternalId(internalId)
               .setAvailability(isAvailable)
               .setIsSponsored(isSpondored)
               .build();

            saveDataProduct(objProducts);

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
      return HOME_PAGE + "/dp/" + id + "?th=1&psc=1";
   }
}
