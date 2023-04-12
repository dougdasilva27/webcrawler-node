package br.com.lett.crawlernode.crawlers.extractionutils.ranking;

import br.com.lett.crawlernode.core.fetcher.methods.JsoupDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.List;

public class CidadeCancaoCrawlerRanking extends CrawlerRankingKeywords {
   public CidadeCancaoCrawlerRanking(Session session) {
      super(session);
   }

   private final String store_id = session.getOptions().optString("storeId", "toledo");

   @Override
   protected void processBeforeFetch() {
      BasicClientCookie cookie = new BasicClientCookie("storeSelected", "https://" + store_id + ".cidadecancao.com");
      cookie.setDomain(store_id + ".cidadecancao.com");
      cookie.setPath("/");
      this.cookies.add(cookie);
   }

   @Override
   protected Document fetchDocument(String url, List<Cookie> cookies) {
      this.currentDoc = new Document(url);

      if (this.currentPage == 1) {
         this.session.setOriginalURL(url);
      }

      Request request = Request.RequestBuilder.create()
         .setCookies(cookies)
         .setUrl(url)
         .build();

      Response response = CrawlerUtils.retryRequest(request, session, new JsoupDataFetcher(), true);

      return Jsoup.parse(response.getBody());
   }

   @Override
   protected void extractProductsFromCurrentPage() throws UnsupportedEncodingException, MalformedProductException {
      this.pageSize = 24;
      this.log("Página " + this.currentPage);

      String url = "https://www.cidadecancao.com/" + store_id + "/catalogsearch/result/?q="+this.keywordEncoded+"&p=" + this.currentPage;
      this.log("Link onde são feitos os crawlers: " + url);

      this.currentDoc = fetchDocument(url, this.cookies);

      Elements products = this.currentDoc.select(".products.list.items.product-items > li");
      if (!products.isEmpty()) {
         for (Element product : products) {
            String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(product, "form[data-role=\"tocart-form\"]", "data-product-sku");
            String internalPid = CrawlerUtils.scrapStringSimpleInfoByAttribute(product, "input[name=\"product\"]", "value");
            String productUrl = CrawlerUtils.scrapStringSimpleInfoByAttribute(product, "a.product", "href");
            String name = CrawlerUtils.scrapStringSimpleInfo(product, ".product.name", false);
            String imgUrl = CrawlerUtils.scrapStringSimpleInfoByAttribute(product, ".product-image-photo", "src");
            Integer price = CrawlerUtils.scrapPriceInCentsFromHtml(product, "span[data-price-type=\"finalPrice\"] > .price", null, false, ',', session, null);
            boolean isAvailable = price != null;

            RankingProduct productRanking = RankingProductBuilder.create()
               .setUrl(productUrl)
               .setInternalId(internalId)
               .setInternalPid(internalPid)
               .setImageUrl(imgUrl)
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
         this.log("Keyword sem resultados!");
      }

      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora "
         + this.arrayProducts.size() + " produtos crawleados");
   }

   @Override
   public boolean hasNextPage() {
      Element pager = this.currentDoc.selectFirst(".pager .pages .next");
      boolean finalPage = pager != null && pager.classNames().contains("disable");
      return !finalPage;
   }
}
