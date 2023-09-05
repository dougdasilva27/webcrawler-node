package br.com.lett.crawlernode.crawlers.ranking.keywords.mexico;

import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.methods.HttpClientFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MexicoFarmarciasdelahorroCrawler extends CrawlerRankingKeywords {

   private final String idUrl = session.getOptions().optString("idUrl");
   public MexicoFarmarciasdelahorroCrawler(Session session) {
      super(session);
   }

   @Override
   protected Document fetchDocument(String url) {

      Map<String, String> headers = new HashMap<>();
      headers.put("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/116.0.0.0 Safari/537.36");

      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .setHeaders(headers)
         .setProxyservice(Arrays.asList(
            ProxyCollection.NETNUT_RESIDENTIAL_MX_HAPROXY,
            ProxyCollection.SMART_PROXY_MX_HAPROXY
         ))
         .build();

      return Jsoup.parse(CrawlerUtils.retryRequestWithListDataFetcher(request, List.of(new HttpClientFetcher()), session).getBody());
   }

   @Override
   protected void extractProductsFromCurrentPage() throws UnsupportedEncodingException, MalformedProductException {

      String url = "https://www.fahorro.com/catalogsearch/result/index/?form_key=" + idUrl + this.currentPage + "&q=" + this.keywordEncoded;
      this.currentDoc = fetchDocument(url);

      Elements products = this.currentDoc.select(".products.wrapper.grid.products-grid li");

      if (!products.isEmpty()) {

         for (Element product : products) {
            String internalPid = CrawlerUtils.scrapStringSimpleInfoByAttribute(product, ".price-box.price-final_price", "data-product-id");
            String productUrl = CrawlerUtils.scrapUrl(product, ".product-item-link", "href", "https", "www.fahorro.com");
            String productName = CrawlerUtils.scrapStringSimpleInfo(product, ".product.name.product-item-name", false);
            String imageUrl = CrawlerUtils.scrapStringSimpleInfoByAttribute(product, ".product-image-photo", "src");
            Integer price = CrawlerUtils.scrapPriceInCentsFromHtml(product, "span[data-price-type=finalPrice] .price", null, false, ',', session, null);
            boolean isAvailable = price != null;

            RankingProduct productRanking = RankingProductBuilder.create()
               .setUrl(productUrl)
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
      return !this.currentDoc.select(".pages .items.pages-items .item.pages-item-next").isEmpty();
   }


}

