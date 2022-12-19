package br.com.lett.crawlernode.crawlers.ranking.keywords.mexico;

import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;

public class MexicoOfficedepotCrawler extends CrawlerRankingKeywords {

   private final int COUNT_PAGE = 0;
   private final String HOME_PAGE = "https://www.officedepot.com.mx/officedepot/en/";

   public MexicoOfficedepotCrawler(Session session) {
      super(session);
   }

   @Override
   protected Document fetchDocument(String url) {
      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .setProxyservice(Arrays.asList(
            ProxyCollection.BUY,
            ProxyCollection.NETNUT_RESIDENTIAL_MX,
            ProxyCollection.NETNUT_RESIDENTIAL_MX_HAPROXY,
            ProxyCollection.SMART_PROXY_MX
         ))
         .build();

      String response = dataFetcher.get(session, request).getBody();

      return Jsoup.parse(response);
   }

   @Override
   protected void extractProductsFromCurrentPage() throws UnsupportedEncodingException, MalformedProductException {
      String keyword = this.keywordWithoutAccents.toLowerCase();
      String url = HOME_PAGE + "Categoría/Todas/c/0-0-0-0?q=" + keyword + "%3Arelevance&page=" + COUNT_PAGE;
      if (keyword.equals("bic")) {
         String bicUrl = HOME_PAGE + "Categoría/Todas/c/0-0-0-0?q=" + keyword + "%3Aprice-desc%3Abrand%3ABIC&page=" + COUNT_PAGE;
         this.currentDoc = fetchDocument(bicUrl);
      } else {
         this.currentDoc = fetchDocument(url);
      }

      Elements products = this.currentDoc.select(".product-item");

      if (!products.isEmpty()) {
         for (Element product : products) {
            String internalPid = CrawlerUtils.scrapStringSimpleInfo(product, ".product-sku > span.name-add.font-medium", false);
            String productName = CrawlerUtils.scrapStringSimpleInfo(product, ".name.description-style > h2", true);
            String productUrl = CrawlerUtils.scrapUrl(product, ".product-description", "href", "https", "www.officedepot.com.mx");
            String imageUrl = CrawlerUtils.scrapUrl(product, ".thumb.center-content-items > img", "data-src", "https", "www.officedepot.com.mx");
            Integer price = getPrice(product);
            boolean isAvailable = price != null;

            RankingProduct productRanking = RankingProductBuilder.create()
               .setUrl(productUrl)
               .setInternalId(internalPid)
               .setInternalPid(internalPid)
               .setName(productName)
               .setPriceInCents(price)
               .setAvailability(isAvailable)
               .setImageUrl(imageUrl)
               .build();

            saveDataProduct(productRanking);
            if (this.arrayProducts.size() == productsLimit)
               break;
         }
      } else {
         this.result = false;
         this.log("Keyword sem resultado!");
      }
      this.log("Finalizando Crawler de produtos da página: " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");
   }

   @Override
   protected boolean hasNextPage() {
      return !this.currentDoc.select(".js-next-pick").isEmpty();
   }

   private Integer getPrice(Element element) {
      Integer price = null;
      String priceString = CrawlerUtils.scrapStringSimpleInfoByAttribute(element, ".btn-primary-rs", "data-productdiscounted");
      if (priceString != null && !priceString.isEmpty()) {
         price = CommonMethods.stringPriceToIntegerPrice(priceString, '.', null);
      }
      return price;
   }
}
