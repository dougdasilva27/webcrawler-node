package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class BrasilCaroneCrawler extends CrawlerRankingKeywords {

   private static final String HOME_PAGE = "https://www.carone.com.br/";
   private static final String API_LINK = "https://www.carone.com.br/carone/index/ajaxCheckPostcode/";
   private final String cep = getCep();

   public BrasilCaroneCrawler(Session session) {
      super(session);
   }

   private String getCep(){
      return session.getOptions().optString("cep");
   }

   @Override
   public void processBeforeFetch() {

      //If the market is Carone (id 1214): We don't need to set any location.
      if (cep != null && !cep.equals("")) {
         Request request = Request.RequestBuilder.create()
            .setUrl(HOME_PAGE)
            .build();
         Response response = dataFetcher.get(session, request);
         Document document = Jsoup.parse(response.getBody());
         String key = CrawlerUtils.scrapStringSimpleInfoByAttribute(document, "input[name=form_key]", "value");

         String payload = "form_key=" + key + "&postcode=" + cep;

         Map<String, String> headers = new HashMap<>();
         headers.put("content-type", "application/x-www-form-urlencoded; charset=UTF-8");

         Request requestApi = Request.RequestBuilder.create()
            .setUrl(API_LINK)
            .setPayload(payload)
            .setHeaders(headers)
            .build();

         Response responseApi = dataFetcher.post(session, requestApi);
         this.cookies.addAll(responseApi.getCookies());
      }
   }

   @Override
   protected void extractProductsFromCurrentPage() throws MalformedProductException {
      this.pageSize = 48;
      this.log("Página " + this.currentPage);

      String url = crawlUrl();

      this.log("Link onde são feitos os crawlers: " + url);
      this.currentDoc = fetchDocument(url);
      Elements products = this.currentDoc.select("div.category-products > ul > li");

      if (!products.isEmpty()) {
         if (this.totalProducts == 0) {
            setTotalProducts(this.currentDoc);
         }
         for (Element e : products) {
            String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".add-to-list a", "data-id");
            String productUrl = CrawlerUtils.scrapUrl(e, ".product-image a", Arrays.asList("href"), "https:", HOME_PAGE);
            String name = CrawlerUtils.scrapStringSimpleInfo(e, ".product-name", false);
            String imgUrl = CrawlerUtils.scrapSimplePrimaryImage(e, ".product-image img", Collections.singletonList("src"), "https", "carone.com.br");
            Integer price = CrawlerUtils.scrapPriceInCentsFromHtml(e, ".special-price .price", null, false, ',', session, 0);
            boolean isAvailable = price != 0;

            RankingProduct productRanking = RankingProductBuilder.create()
               .setUrl(productUrl)
               .setInternalId(internalId)
               .setInternalPid(null)
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
         this.log("Keyword sem resultado!");
      }

      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora "
         + this.arrayProducts.size() + " produtos crawleados");

   }

   private String crawlUrl() {
      String link;
      if (this.currentPage == 1) {
         link = "https://www.carone.com.br/catalogsearch/result/?q=" + this.keywordEncoded;
      } else {
         link = "https://www.carone.com.br/search/page/" + this.currentPage + "?q=" + this.keywordEncoded;
      }
      return link;
   }

   private void setTotalProducts(Document doc) {
      this.totalProducts = CrawlerUtils.scrapIntegerFromHtml(doc, ".amount .show > span", true, 0);
      this.log("Total da busca: " + this.totalProducts);
   }
}
