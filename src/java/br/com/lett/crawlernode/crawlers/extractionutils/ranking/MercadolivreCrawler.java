package br.com.lett.crawlernode.crawlers.extractionutils.ranking;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.methods.ApacheDataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.FetcherDataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.JsoupDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.List;

public class MercadolivreCrawler extends CrawlerRankingKeywords {

   private String nextUrlHost;
   protected String nextUrl;
   private String productUrlHost;
   protected String url;
   private String getCep() {return session.getOptions().optString("cp");}
   private String getDomain() {return session.getOptions().optString("domain");}

   protected MercadolivreCrawler(Session session) {
      super(session);
      super.fetchMode = FetchMode.FETCHER;
   }

   public void setNextUrlHost(String nextUrlHost) {
      this.nextUrlHost = nextUrlHost;
   }

   public void setUrl(String url) {
      this.url = url;
   }

   public void setProductUrlHost(String productUrlHost) {
      this.productUrlHost = productUrlHost;
   }

   private static final String PRODUCTS_SELECTOR = ".ui-search-layout__item div.andes-card";
   protected Integer meliPageSize = 64;

   @Override
   protected void extractProductsFromCurrentPage() throws MalformedProductException {
      this.pageSize = meliPageSize;
      this.log("Página " + this.currentPage);

      String searchUrl = getNextPageUrl();

      this.currentDoc = fetch(searchUrl);
      this.nextUrl = CrawlerUtils.scrapUrl(currentDoc, ".andes-pagination__button--next > a", "href", "https:", nextUrlHost);
      Elements products = this.currentDoc.select(PRODUCTS_SELECTOR);

      if (!products.isEmpty()) {
         if (this.totalProducts == 0) {
            setTotalProducts();
         }

         for (Element e : products) {
            String productUrl = CrawlerUtils.scrapUrl(e, "a.ui-search-result__content, .ui-search-item__group--title a[title]", "href", "https", productUrlHost);

            String internalPid = null;
            if (productUrl != null) {
               if (productUrl.startsWith("https://www.mercadolivre.com.br/") || productUrl.startsWith("https://www.mercadolibre.com")) {
                  productUrl = productUrl != null ? productUrl.split("\\?")[0] : null;
                  internalPid = CommonMethods.getLast(productUrl.split("/"));
               } else {
                  internalPid = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, "input[name=itemId]", "value");
               }
            }

            String name = CrawlerUtils.scrapStringSimpleInfo(e, ".ui-search-item__title", true);
            String imageUrl = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".slick-slide.slick-active img", "data-src");
            int price = CommonMethods.doublePriceToIntegerPrice(CrawlerUtils.scrapDoublePriceFromHtml(e, ".ui-search-price__second-line .price-tag-amount", null, false, ',', session), 0);
            boolean isAvailable = price > 0;

            //New way to send products to save data product
            RankingProduct productRanking = RankingProductBuilder.create()
               .setUrl(productUrl)
               .setInternalId(null)
               .setInternalPid(internalPid)
               .setName(name)
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

      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");
   }

   private int getPrice(Element e) {
      int price = 0;
      Double a = CrawlerUtils.scrapDoublePriceFromHtml(e, ".ui-search-price__second-line .price-tag-amount", null, false, ',', session);

      if (a == null) {
         a = CrawlerUtils.scrapDoublePriceFromHtml(e, ".ui-search-price__second-line .price-tag-amount", null, false, ',', session);

      }
      return CommonMethods.doublePriceToIntegerPrice(a, 0);
   }

   private Document fetch(String url) {
      // This user agent is used because some of ours user agents doesn't work on this market
      if (getCep() != null && !getCep().isEmpty()) {
         BasicClientCookie cookie = new BasicClientCookie("cp", getCep());
         cookie.setDomain(getDomain());
         cookie.setPath("/");
         this.cookies.add(cookie);
      }

      Request request = RequestBuilder.create()
         .setCookies(cookies)
         .setUrl(url)
         .setFollowRedirects(false)
         .build();

      Response response = CrawlerUtils.retryRequestWithListDataFetcher(request, List.of(new ApacheDataFetcher(), new JsoupDataFetcher()), session, "get");

      return Jsoup.parse(response.getBody());
   }

   protected String getNextPageUrl() {
      return this.currentPage > 1 ? this.nextUrl : this.url;
   }

   @Override
   protected boolean hasNextPage() {
      return super.hasNextPage() && this.nextUrl != null;
   }

   @Override
   protected void setTotalProducts() {
      this.totalProducts = CrawlerUtils.scrapIntegerFromHtml(currentDoc, ".ui-search-search-result__quantity-results", true, 0);
      this.log("Total da busca: " + this.totalProducts);
   }

}
