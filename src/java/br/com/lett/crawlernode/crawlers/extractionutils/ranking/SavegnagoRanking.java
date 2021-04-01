package br.com.lett.crawlernode.crawlers.extractionutils.ranking;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class SavegnagoRanking extends CrawlerRankingKeywords {

   private static final String BASE_URL = "www.savegnago.com.br";
   private String urlModel;
   private final String storeId = getStoreId();


   public String getStoreId() {
      return storeId;
   }

   public SavegnagoRanking(Session session) {
      super(session);
      super.fetchMode = FetchMode.FETCHER;
   }

   @Override
   protected void processBeforeFetch() {
      super.processBeforeFetch();

      Logging.printLogDebug(logger, session, "Adding cookie...");

      BasicClientCookie cookie = new BasicClientCookie("VTEXSC", "sc=" + storeId);
      cookie.setDomain(".savegnago.com.br");
      cookie.setPath("/");
      this.cookies.add(cookie);

   }


   private String buildUrl(Document doc) {

      if (urlModel == null) {
         String script = doc.selectFirst(".vitrine script[type='text/javascript']").toString();

         String[] firstSplit = script.split("load\\('");
         if (firstSplit.length > 0) {
            String url = firstSplit[1].split("' \\+ pageclickednumber")[0];
            urlModel = BASE_URL + url;
            return urlModel + this.currentPage;
         }
      } else {
         return urlModel + this.currentPage;
      }

      return null;
   }

   private Document getHtml(String url) {
      Request request = Request.RequestBuilder.create().setUrl(url).setCookies(cookies).build();
      Response response = dataFetcher.get(session, request);

      return Jsoup.parse(response.getBody());

   }

   @Override
   protected void extractProductsFromCurrentPage() {
      this.pageSize = 32;
      this.log("Página " + this.currentPage);

      String urlFirst = "https://" + BASE_URL + "/" + this.keywordEncoded;

      Document doc = getHtml(urlFirst);
      String url = "https://" + buildUrl(doc);

      this.currentDoc = fetchDocument(url);

      if (currentDoc.selectFirst(".product-card") != null) {
         //Get from the html
         this.log("Link onde são feitos os crawlers: " + url);
         Elements products = this.currentDoc.select(".n4colunas li[layout]");

         if (products != null && !products.isEmpty()) {
            if (totalProducts == 0) {
               setTotalProducts(doc);
            }
            for (Element product : products) {
               String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(product, ".product-card", "item");
               String internalPid = internalId;
               String productUrl = CrawlerUtils.scrapUrl(product, ".prod-acc > a", "href", "https", BASE_URL);
               saveDataProduct(internalId, internalPid, productUrl);

               log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + internalPid + " - Url: " + productUrl);
               if (arrayProducts.size() == productsLimit) {
                  break;
               }
            }
         }
      } else {
         result = false;
         log("Keyword sem resultado!");
      }

      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora "
         + this.arrayProducts.size() + " produtos crawleados");

   }

   protected void setTotalProducts(Document doc) {
      this.totalProducts = CrawlerUtils.scrapIntegerFromHtml(doc, ".resultado-busca-numero .value", true, 0);
      this.log("Total de produtos: " + this.totalProducts);
   }

}
