package br.com.lett.crawlernode.crawlers.ranking.keywords.florianopolis;

import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.crawlers.extractionutils.core.AngeloniSuperUtils;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.apache.http.cookie.Cookie;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CommonMethods;

import java.util.List;

public class FlorianopolisAngeloniCrawler extends CrawlerRankingKeywords {
   private static final String HOME_PAGE = "https://www.angeloni.com.br/";

   public FlorianopolisAngeloniCrawler(Session session) {
      super(session);
   }

   @Override
   protected List<Cookie> fetchCookies(String url) {
      return AngeloniSuperUtils.fetchLocationCookies(session, this.dataFetcher);
   }

   @Override
   protected void processBeforeFetch() {
      if(this.cookies.isEmpty()) {
         this.cookies = AngeloniSuperUtils.fetchLocationCookies(session, this.dataFetcher);
      }
   }

   @Override
   protected void extractProductsFromCurrentPage() throws MalformedProductException {
      this.pageSize = 12;
      this.log("Página " + this.currentPage);

      String url = HOME_PAGE + "super/busca?No=" + this.arrayProducts.size() + "&Nrpp=12&Ntt=" + this.keywordEncoded;
      this.log("Link onde são feitos os crawlers: " + url);

      this.currentDoc = fetchDocument(url);

      Elements products = this.currentDoc.select(".box-produto");

      if (!products.isEmpty()) {
         if (this.totalProducts == 0) {
            setTotalProducts();
         }

         for (Element e : products) {
            String productUrl = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, "a", "href");
            String internalId = crawlInternalId(productUrl);
            String name = CrawlerUtils.scrapStringSimpleInfo(e, "a > .box-produto__desc-prod", false);
            Integer price = CrawlerUtils.scrapIntegerFromHtml(e, ".box-produto__preco", false, 0);
            boolean isAvailable = price != 0;

            RankingProduct productRanking = RankingProductBuilder.create()
               .setUrl(productUrl)
               .setInternalId(internalId)
               .setInternalPid(internalId)
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
         this.log("Keyword sem resultado!");
      }
      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora "
         + this.arrayProducts.size() + " produtos crawleados");
   }

   @Override
   protected void setTotalProducts() {
      Element totalElement = this.currentDoc.select(".banner-titulo span").first();

      if (totalElement != null) {
         String text = totalElement.ownText().replaceAll("[^0-9]", "");

         if (!text.isEmpty()) {
            this.totalProducts = Integer.parseInt(text);
         }

         this.log("Total da busca: " + this.totalProducts);
      }
   }

   private String crawlInternalId(String url) {
      return CommonMethods.getLast(url.split("-"));
   }


}
