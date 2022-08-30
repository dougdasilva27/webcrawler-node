package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
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

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BrasilPalacioDasFerramentas extends CrawlerRankingKeywords {
   protected Integer PRODUCTS_PER_PAGE = 24;
   private static final String HOME_PAGE = "https://palaciodasferramentas.com.br";

   public BrasilPalacioDasFerramentas(Session session) {
      super(session);
      super.fetchMode = FetchMode.JSOUP;
   }

   @Override
   protected void extractProductsFromCurrentPage() throws MalformedProductException {
      this.log("Página " + this.currentPage);

      String url = HOME_PAGE + "/catalogsearch/result/index/?" + "p=" + this.currentPage + "&q=" + this.keywordEncoded;

      this.log("Link onde são feitos os crawlers: " + url);
      this.currentDoc = fetchDocument(url);

      Elements products = this.currentDoc.select(".filterproducts.products.list.items.product-items li.item.product.product-item");

      if (!products.isEmpty()) {
         if (this.totalProducts == 0) {
            this.totalProducts = products.size();
         }

         for (Element e : products) {
            String productUrl = CrawlerUtils.scrapUrl(e, ".product.photo.product-item-photo a", "href", "", "");
            String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".price-box.price-final_price", "data-product-id");
            String name = CrawlerUtils.scrapStringSimpleInfo(e, ".product-item-link", false);
            String imgUrl = scrapImgUrl(e);
            Integer price = scrapPrice(e);
            boolean isAvailable = price != null;

            RankingProduct productRanking = RankingProductBuilder.create()
               .setUrl(productUrl)
               .setInternalId(internalId)
               .setName(name)
               .setImageUrl(imgUrl)
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

   private String scrapImgUrl(Element e) {
      return CrawlerUtils.scrapUrl(e, ".product.photo.product-item-photo a img", "data-src", "", "");
   }


   private Integer scrapPrice(Element e) {
      String priceDescription = CrawlerUtils.scrapStringSimpleInfo(e, "span .price", false);
      Integer price;

      if (priceDescription != null && !priceDescription.isEmpty()) {
         priceDescription = priceDescription.replaceAll("[^0-9]", "");
         price = !priceDescription.equals("") ? Integer.parseInt(priceDescription) : null;

         return price;
      }

      return null;
   }

   @Override
   protected boolean hasNextPage() {
      return true;
   }

   @Override
   protected Document fetchDocument(String url) {
      Map<String, String> headers = new HashMap<>();

      headers.put("Accept", "*/*");
      headers.put("Accept-Encoding", "gzip, deflate, br");
      headers.put("Connection", "keep-alive");
      headers.put("authority", "www.palaciodasferramentas.com.br");

      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .setHeaders(headers)
         .setProxyservice(Arrays.asList(
            ProxyCollection.BUY,
            ProxyCollection.NETNUT_RESIDENTIAL_BR_HAPROXY,
            ProxyCollection.LUMINATI_SERVER_BR_HAPROXY,
            ProxyCollection.NETNUT_RESIDENTIAL_BR))
         .setSendUserAgent(false)
         .build();

      Response response = CrawlerUtils.retryRequestWithListDataFetcher(request, List.of(new ApacheDataFetcher(), new JsoupDataFetcher(), new FetcherDataFetcher()), session );

      return Jsoup.parse(response.getBody());
   }

}
