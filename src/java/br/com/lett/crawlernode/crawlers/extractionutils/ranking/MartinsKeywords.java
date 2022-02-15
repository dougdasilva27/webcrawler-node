package br.com.lett.crawlernode.crawlers.extractionutils.ranking;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.models.Request;
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

import java.util.HashMap;
import java.util.Map;

public class MartinsKeywords extends CrawlerRankingKeywords {

   public MartinsKeywords(Session session) {
      super(session);
      super.fetchMode = FetchMode.JSOUP;
   }

   protected String password = getPassword();
   protected String login = getLogin();

   protected String getPassword() {
      return session.getOptions().optString("pass");
   }

   protected String getLogin() {
      return session.getOptions().optString("login");
   }

   @Override
   public void extractProductsFromCurrentPage() throws MalformedProductException {
      this.pageSize = 12;
      this.log("Página " + this.currentPage);

      // monta a url com a keyword, page size e a página
      String url = "https://www.martinsatacado.com.br/busca/?text=/engage/search/v3/search?terms=" + this.keywordWithoutAccents.replace(" ", "%20") + "&resultsperpage=" + this.pageSize + "&saleschannel=default&page=" + this.currentPage;

      this.log("Link onde são feitos os crawlers: " + url);
      this.currentDoc = fetchDocument(url);

      Elements products = this.currentDoc.select(".product[data-sku]");

      if (!products.isEmpty()) {
         if (this.totalProducts == 0) {
            setTotalProducts();
         }

         for (Element e : products) {
            String internalId = CommonMethods.getLast(e.attr("data-sku").split("_"));
            String urlProduct = CrawlerUtils
               .scrapUrl(e, "a", "href", "https", "www.martinsatacado.com.br");

            String name = CrawlerUtils.scrapStringSimpleInfo(e, ".product > a > p ", true);
            String imageUrl = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".containertImg > img", "src");
            Integer price = CrawlerUtils.scrapPriceInCentsFromHtml(e, ".value > span", null, true, ',', session, 0);
            boolean isAvailable = price != 0;

            RankingProduct productRanking = RankingProductBuilder.create()
               .setUrl(urlProduct)
               .setInternalId(internalId)
               .setName(name)
               .setPriceInCents(price)
               .setAvailability(isAvailable)
               .setImageUrl(imageUrl)
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
   protected Document fetchDocument(String url) {
      Map<String, String> headers = new HashMap<>();
      headers.put("content-type", "application/x-www-form-urlencoded");
      headers.put("referer", url);
      headers.put("authority", "www.martinsatacado.com.br");
      headers.put("accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9");
      String payload = "j_username=" + login.replace("@", "%40") + "&j_password=" + password;

      Request request = Request.RequestBuilder.create()
         .setUrl("https://www.martinsatacado.com.br/j_spring_security_check")
         .setPayload(payload)
         .setHeaders(headers)
         .build();

      return Jsoup.parse(this.dataFetcher.post(session, request).getBody());
   }

   @Override
   protected void setTotalProducts() {
      this.totalProducts = CrawlerUtils.scrapIntegerFromHtml(currentDoc, ".fr.obs1.reslts", null, null, true, true, 0);
      this.log("Total da busca: " + this.totalProducts);
   }
}
