package br.com.lett.crawlernode.crawlers.extractionutils.ranking;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.Collections;
import java.util.Locale;

public abstract class HomecenterCrawlerRanking extends CrawlerRankingKeywords {

   private static final String HOME_PAGE = "www.homecenter.com.co";

   protected HomecenterCrawlerRanking(Session session) {
      super(session);
      super.fetchMode = FetchMode.FETCHER;
   }

   public abstract String getCity();

   public abstract String getCityComuna();

   @Override
   protected void processBeforeFetch() {
      String city = getCity();
      String comuna = getCityComuna();
      if (city != null) {
         cookies.add(new BasicClientCookie("ZONE_NAME", city.toUpperCase(Locale.ROOT)));
      }
      if (comuna != null) {
         cookies.add(new BasicClientCookie("comuna", comuna));
      }
   }

   @Override
   protected void extractProductsFromCurrentPage() throws MalformedProductException {
      this.pageSize = 28;
      this.log("Página " + this.currentPage);

      String url = "https://www.homecenter.com.co/homecenter-co/search/?Ntt=" + this.keywordEncoded + "&currentpage=" + this.currentPage;

      this.log("Link onde são feitos os crawlers: " + url);
      this.currentDoc = fetchDocument(url);
      Elements products = this.currentDoc.select("[data-category='']");

      if (!products.isEmpty()) {
         for (Element e : products) {
            String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, null, "data-key");
            String productUrl = CrawlerUtils.scrapUrl(e, ".product.ie11-product-container .link-with-wrapper a", Collections.singletonList("href"), "https", HOME_PAGE);

            String name = CrawlerUtils.scrapStringSimpleInfo(e, ".product > div > a > .product-title", true);
            String imageUrl = CrawlerUtils.scrapSimplePrimaryImage(e, ".product-image > div > img", Collections.singletonList("data-src"), "https", "https://www.homecenter.com.co");
            Integer price = null;
            boolean isAvailable = getAvaliability(e);
            if (isAvailable) {
               price = CrawlerUtils.scrapPriceInCentsFromHtml(e, ".phablet-desktop > .product-price-and-logo > .main.gridView > .price > span:first-child", null, true, ',', session, null);
            }

            RankingProduct productRanking = RankingProductBuilder.create()
               .setUrl(productUrl)
               .setInternalId(internalId)
               .setName(name)
               .setPriceInCents(price)
               .setAvailability(isAvailable)
               .setImageUrl(imageUrl)
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

   private boolean getAvaliability(Element e) {
      String firstValidation = CrawlerUtils.scrapStringSimpleInfo(e, ".dispatch-and-withdrawl-info > .dispatch-info", false);
      String secondValidation = CrawlerUtils.scrapStringSimpleInfo(e, ".dispatch-and-withdrawl-info > .withdrawl-info", false);

      return firstValidation.contains("Disponible para despacho") || secondValidation.contains("Disponible para retiro");
   }

   @Override
   protected boolean hasNextPage() {
      Element page = this.currentDoc.selectFirst("#bottom-pagination-next-page");
      return page != null;
   }
}
