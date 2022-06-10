package br.com.lett.crawlernode.crawlers.extractionutils.ranking;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
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
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MercadoShopCrawler extends CrawlerRankingKeywords {

   protected String nextUrl;

   public MercadoShopCrawler(Session session) {
      super(session);
      super.fetchMode = FetchMode.FETCHER;
   }

   private final String homePage = getHomePage();

   protected String getHomePage() {
      return session.getOptions().optString("HomePage");
   }

   protected String url = homePage + this.keywordWithoutAccents.replace(" ", "-");

   public void setUrl(String url) {
      this.url = url;
   }

   private static final String PRODUCTS_SELECTOR = ".ui-search-layout__item div.andes-card";

   @Override
   protected void extractProductsFromCurrentPage() throws MalformedProductException {
      this.pageSize = 50;
      this.log("Página " + this.currentPage);

      String searchUrl = getNextPageUrl();

      this.currentDoc = fetch(searchUrl);
      this.nextUrl = CrawlerUtils.scrapUrl(currentDoc, ".andes-pagination__button--next > a", "href", "https:", homePage);
      Elements products = this.currentDoc.select(PRODUCTS_SELECTOR);

      if (!products.isEmpty()) {
         if (this.totalProducts == 0) {
            setTotalProducts();
         }

         for (Element e : products) {
            String productUrl = CrawlerUtils.scrapUrl(e, "a.ui-search-result__content, .ui-search-item__group--title a[title]", "href", "https", homePage);

            String internalPid = crawPid(productUrl);
//            String internalPid = null;
//            if (productUrl != null) {
//               if (productUrl.startsWith(homePage)) {
//                  productUrl = productUrl != null ? productUrl.split("\\?")[0] : null;
//                  internalPid = CommonMethods.getLast(productUrl.split("/"));
//               } else {
//                  internalPid = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, "input[name=itemId]", "value");
//               }
//            }

            String name = CrawlerUtils.scrapStringSimpleInfo(e, ".ui-search-item__title", true);
            String imageUrl = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".slick-slide.slick-active img", "data-src");
            int price = CommonMethods.doublePriceToIntegerPrice(CrawlerUtils.scrapDoublePriceFromHtml(e, ".ui-search-price__second-line .price-tag-amount", null, false, ',', session), 0);
            boolean isAvailable = price > 0;

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

      Request request = Request.RequestBuilder.create()
         .setCookies(cookies)
         .setUrl(url)
         .setFollowRedirects(true)
         .build();

      Response response = dataFetcher.get(session, request);

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

   private String crawPid(String url) {
      String regex = "p/([A-Z0-9]*)?";
      Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
      Matcher matcher = pattern.matcher(url);

      if (matcher.find()) {
         return matcher.group(1);
      } else {
         regex = "/([A-Z]*-[0-9]*)-";
         pattern = Pattern.compile(regex, Pattern.MULTILINE);
         matcher = pattern.matcher(url);
         if(matcher.find()){
            String aux = matcher.group(1).replaceAll("-","");
            return aux;
         }
      }
   return null;
   }

}

