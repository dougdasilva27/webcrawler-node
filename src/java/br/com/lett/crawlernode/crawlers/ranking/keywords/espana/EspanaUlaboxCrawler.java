package br.com.lett.crawlernode.crawlers.ranking.keywords.espana;

import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.apache.http.cookie.Cookie;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EspanaUlaboxCrawler extends CrawlerRankingKeywords {

   public EspanaUlaboxCrawler(Session session) {
      super(session);
   }

   @Override
   protected Document fetchDocument(String url) {
      this.currentDoc = new Document(url);

      if (this.currentPage == 1) {
         this.session.setOriginalURL(url);
      }

      Request request = Request.RequestBuilder.create()
         .setCookies(this.cookies)
         .setUrl(url)
         .setProxyservice(List.of(
            ProxyCollection.BUY_HAPROXY,
            ProxyCollection.NETNUT_RESIDENTIAL_ES_HAPROXY,
            ProxyCollection.NETNUT_RESIDENTIAL_ES
         ))
         .build();

      Response response = dataFetcher.get(session, request);

      return Jsoup.parse(response.getBody());
   }

   @Override
   protected void extractProductsFromCurrentPage() throws MalformedProductException {

      this.pageSize = 60;
      this.log("Página " + this.currentPage);

      String url = "https://www.ulabox.com/en/search?q=" + this.keywordEncoded + "&p=" + this.currentPage;

      this.log("Link onde são feitos os crawlers: " + url);
      this.currentDoc = fetchDocument(url);
      Elements products = this.currentDoc.select(".MuiGrid-root.jss561.MuiGrid-item.MuiGrid-grid-xs-6.MuiGrid-grid-sm-4.MuiGrid-grid-md-4.MuiGrid-grid-lg-3.MuiGrid-grid-xl-true");

      if (!products.isEmpty()) {
         for (Element e : products) {
            String productUrl = CrawlerUtils.scrapUrl(e, "a", "href", "https", "www.ulabox.com");
            String internalId = scrapInternalId(productUrl);
            String name = CrawlerUtils.scrapStringSimpleInfo(e, ".MuiTypography-root.MuiLink-root.MuiLink-underlineHover.jss567.false.MuiTypography-colorSecondary", false);
            Integer priceInCents = CrawlerUtils.scrapPriceInCentsFromHtml(e, ".MuiGrid-root.jss561.MuiGrid-item.MuiGrid-grid-xs-6.MuiGrid-grid-sm-4.MuiGrid-grid-md-4.MuiGrid-grid-lg-3.MuiGrid-grid-xl-true .jss573", null, true, ',', session, null);
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

   private String scrapInternalId(String url) {
      final String regex = "(producto|product)\\/.*\\/(\\d+)";

      final Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
      final Matcher matcher = pattern.matcher(url);

      return matcher.find() ? matcher.group(2) : null;
   }

   @Override
   protected boolean hasNextPage() {
      Element nextButton = this.currentDoc.selectFirst("a[rel=next]");
      if (nextButton != null) {
         String disableString = nextButton.attr("aria-disabled");
         return "false".equals(disableString);
      }

      return false;
   }
}
