package br.com.lett.crawlernode.crawlers.ranking.keywords.peru;

import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.methods.ApacheDataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.FetcherDataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.JsoupDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class PeruLinioCrawler extends CrawlerRankingKeywords {
   public PeruLinioCrawler(Session session) {
      super(session);
   }

   String HOME_PAGE = "https://www.linio.com.pe";
   String url = HOME_PAGE + "/search?scroll=&q=" + this.keywordEncoded + "&page=" + this.currentPage;

   @Override
   protected Document fetchDocument(String url) {
      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .setProxyservice(Arrays.asList(
            ProxyCollection.BUY_HAPROXY,
            ProxyCollection.LUMINATI_SERVER_BR_HAPROXY,
            ProxyCollection.NETNUT_RESIDENTIAL_ES,
            ProxyCollection.SMART_PROXY_PE
         ))
         .build();

      Response response = CrawlerUtils.retryRequestWithListDataFetcher(request, List.of(new ApacheDataFetcher(), new JsoupDataFetcher(), new FetcherDataFetcher()), session);

      return Jsoup.parse(response.getBody());
   }

   @Override
   protected void extractProductsFromCurrentPage() throws UnsupportedEncodingException, MalformedProductException {
      this.currentDoc = fetchDocument(url);
      Elements products = this.currentDoc.select(".catalogue-product.row");

      if (!products.isEmpty()) {
         for (Element product : products) {
            String name = CrawlerUtils.scrapStringSimpleInfoByAttribute(product, ".col-12.pl-0.pr-0", "title");
            Integer priceInCents = CrawlerUtils.scrapPriceInCentsFromHtml(product, ".lowest-price .price-main-md",
               null, false, '.', session, null);
            String productUrl = getProductUrl(CrawlerUtils.scrapStringSimpleInfoByAttribute(product, ".catalogue-product.row a", "href"));
            String internalPid = CrawlerUtils.scrapStringSimpleInfoByAttribute(product, ".catalogue-product.row", "data-card-sku");
            String imageUrl = CrawlerUtils.scrapSimplePrimaryImage(product, ".image", List.of("data-lazy"),
               "https", "");
            RankingProduct rankingProduct = RankingProductBuilder.create()
               .setInternalId(internalPid)
               .setInternalPid(internalPid)
               .setName(name)
               .setImageUrl(imageUrl)
               .setAvailability(true)
               .setPriceInCents(priceInCents)
               .setUrl(productUrl)
               .build();

            saveDataProduct(rankingProduct);
            if (this.arrayProducts.size() == productsLimit) {
               break;
            }
         }

      }
   }

   private String getProductUrl(String url) {
      return url != null && !url.isEmpty() ? HOME_PAGE + url : null;
   }

   @Override
   protected boolean hasNextPage() {
      return this.currentDoc.selectFirst(".page-link page-link-icon") != null;
   }
}
