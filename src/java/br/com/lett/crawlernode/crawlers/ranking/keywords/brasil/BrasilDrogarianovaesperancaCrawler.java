package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.apache.http.cookie.Cookie;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BrasilDrogarianovaesperancaCrawler extends CrawlerRankingKeywords {

   private static final int PAGE_SIZE = 36;

   public BrasilDrogarianovaesperancaCrawler(Session session) {
      super(session);
      super.fetchMode = FetchMode.FETCHER;
   }
   @Override
   protected Document fetchDocument(String url) {
      this.currentDoc = new Document(url);

      if (this.currentPage == 1) {
         this.session.setOriginalURL(url);
      }
      Map<String, String> headers = new HashMap<>();
     // headers.put("cookie", "CookiesPO=A2A69A90-00D4-453C-9331-9B4B86A7865A; .Fw.Session=CfDJ8JJw1TdO6PdLuY5U4fn%2BbyYEm0FXHTe2DbN3M7KXsa8oOkbs5nT1DHD5c91tdaTzHDemcBfuFEpUGLZscaznWYnmEmd%2Ba3LiHUp9onaQhbNpXQps07i7TnlzGb%2FpnDA4iUstI0h6v5qQ05lQCH0nyIA%2BNpSXRWNdzwRApF71nHmC; DNE.Antiforgery=CfDJ8JJw1TdO6PdLuY5U4fn-byZ6nsq4vaWsYlOHWigwZjxPTeNJZaXBK3FDR--pB4SEr2Tqpl8WoNKlftPvcHo1FsPvMHdaoyU1WyOekY35d8wuzVS6ZSzfOhuv_mWCz6E3d1-75Fcxv2NjPHuYZjeqxP0");

      Request request = Request.RequestBuilder.create()
      //   .setHeaders(headers)
         .setUrl(url)
         .setProxyservice(Arrays.asList(
         ProxyCollection.NETNUT_RESIDENTIAL_BR_HAPROXY, ProxyCollection.NETNUT_RESIDENTIAL_BR, ProxyCollection.BUY_HAPROXY,
            ProxyCollection.BUY, ProxyCollection.LUMINATI_RESIDENTIAL_BR)).build();
      Response response = CrawlerUtils.retryRequest(request, session, dataFetcher, true);

      return Jsoup.parse(response.getBody());
   }
   @Override
   protected void extractProductsFromCurrentPage() throws MalformedProductException {
      this.pageSize = PAGE_SIZE;
      this.log("Página " + this.currentPage);

      String url = "https://www.drogarianovaesperanca.com.br/busca/?Pagina=" + this.currentPage +
         "&q=" + keywordWithoutAccents.replaceAll(" ", "+") + "&ULTP=S";
      this.log("Link onde são feitos os crawlers: " + url);

      this.currentDoc = fetchDocument(url);
      Elements products = this.currentDoc.select("div[class*=boxproduto-lista]");

      // In this scraper we need scrap products total first
      // because we need this information for verify if the site returns products for the keyword we send
      // or returns suggestions (in this case we don't have total Products),
      // we don't scrap suggestions products
      if (this.totalProducts == 0) {
         this.setTotalProducts();
      }

      if (!products.isEmpty() && this.totalProducts > 0) {
         for (Element e : products) {
            String productUrl = CrawlerUtils.scrapUrl(e, "h3.dg-boxproduto-titulo a", "href", "https", "www.drogarianovaesperanca.com.br");
            String internalId = crawlInternalId(productUrl);
            String name = CrawlerUtils.scrapStringSimpleInfo(e, "h3.dg-boxproduto-titulo a", false);
            String imgUrl = scrapFullImage(e);
            Integer price = CrawlerUtils.scrapPriceInCentsFromHtml(e,"span[class*=preco-por]", null, false, ',', session, null);

            boolean isAvailable = price != 0;

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

      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");
   }

   @Override
   protected void setTotalProducts() {
      this.totalProducts = CrawlerUtils.scrapIntegerFromHtml(currentDoc, "h1.dg-categoria-desc", true, 0);
      this.log("Total: " + this.totalProducts);
   }

   /**
    * For scrap internal Id we need the url, is the only "safe" place
    * <p>
    * Ex: Internal Id will be found in the end of url
    * <p>
    * Url:
    * https://www.drogarianovaesperanca.com.br/naturais/fitoterapicos/comprar-prevelip-com-60-capsulas-16970/
    * <p>
    * InternalId: 16970
    *
    * @param url
    * @return
    */
   private String crawlInternalId(String url) {
      String internalId = null;

      String lastStringUrl = CommonMethods.getLast(url.split("\\?")[0].split("-"));

      if (lastStringUrl.contains("/")) {
         internalId = lastStringUrl.split("/")[0];
      }

      return internalId;
   }

   private String scrapFullImage (Element e) {
      String miniImage = CrawlerUtils.scrapStringSimpleInfoByAttribute(e,  "a[class*=boxproduto-img] img", "data-src");

      return miniImage.replaceAll("imagens/200x200/", "imagens-complete/445x445/");
   }
}
