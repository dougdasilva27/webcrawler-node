package br.com.lett.crawlernode.crawlers.ranking.keywords.mexico;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
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
import java.util.Map;

public class MexicoFarmarciasdelahorroCrawler extends CrawlerRankingKeywords {
   public MexicoFarmarciasdelahorroCrawler(Session session) {
      super(session);
      super.fetchMode = FetchMode.JSOUP;
   }

   @Override
   protected Document fetchDocument(String url) {

      Map<String, String> headers = new HashMap<>();
      headers.put("Connection", "keep-alive");

      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .setHeaders(headers)
         .setProxyservice(Arrays.asList(
            ProxyCollection.NETNUT_RESIDENTIAL_CO_HAPROXY,
            ProxyCollection.NETNUT_RESIDENTIAL_AR_HAPROXY,
            ProxyCollection.NETNUT_RESIDENTIAL_ES,
            ProxyCollection.NETNUT_RESIDENTIAL_BR
         ))
         .build();

      String response = dataFetcher.get(session, request).getBody();

      return Jsoup.parse(response);
   }

   @Override
   protected void extractProductsFromCurrentPage() throws UnsupportedEncodingException, MalformedProductException {

      String url = "https://www.fahorro.com/catalogsearch/result/index/?p=" + this.currentPage + "&q=" + this.keywordEncoded;
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
      this.log("Finishing page products crawler: " + this.currentPage + " - yet " + this.arrayProducts.size() + " produtos crawleados");
   }

   @Override
   protected boolean hasNextPage() {
      return !this.currentDoc.select(".pages .items.pages-items .item.pages-item-next").isEmpty();
   }


}

