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
import java.util.*;

public class PeruLinioCrawler extends CrawlerRankingKeywords {
   public PeruLinioCrawler(Session session) {
      super(session);
   }

   String HOME_PAGE = "https://www.linio.com.pe";
   String url = HOME_PAGE + "/search?scroll=&q=" + this.keywordEncoded + "&page=" + this.currentPage;

   @Override
   protected Document fetchDocument(String url) {

      Map<String, String> headers = new HashMap<>();
      headers.put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9");
      headers.put("Accept-Language", "en-US,en;q=0.9,pt;q=0.8,pt-PT;q=0.7");
      headers.put("Cache-Control", "max-age=0");
      headers.put("authority", "www.linio.com.pe");
      headers.put("referer", "https://www.linio.com.pe/");

      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .setHeaders(headers)
         .setProxyservice(Arrays.asList(
            ProxyCollection.SMART_PROXY_PE_HAPROXY,
            ProxyCollection.NETNUT_RESIDENTIAL_ES_HAPROXY,
            ProxyCollection.SMART_PROXY_PE,
            ProxyCollection.SMART_PROXY_MX_HAPROXY
         ))
         .build();

      Response response = CrawlerUtils.retryRequestWithListDataFetcher(request, List.of( new FetcherDataFetcher(),new ApacheDataFetcher(), new JsoupDataFetcher()), session);

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
