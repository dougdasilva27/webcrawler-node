package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import br.com.lett.crawlernode.util.Logging;

public class BrasilPetcenterexpressCrawler extends CrawlerRankingKeywords {

   private static final String HOST = "www.petcenterexpress.com.br";

   public BrasilPetcenterexpressCrawler(Session session) {
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() {
      this.pageSize = 15;
      this.log("Página " + this.currentPage);

      String url = "https://www.petcenterexpress.com.br/produtos?busca=" + CommonMethods.encondeStringURLToISO8859(this.location, logger, session)
            + "&pagina=" + this.currentPage;

      this.log("Link onde são feitos os crawlers: " + url);
      this.currentDoc = fetchDocument(url);
      JSONArray productsArray = crawlProductsArray(this.currentDoc);

      if (productsArray.length() > 1) {
         if (this.totalProducts == 0) {
            setTotalProducts();
         }

         for (Object o : productsArray) {
            JSONObject skuJson = o instanceof JSONObject ? (JSONObject) o : new JSONObject();

            if (skuJson.has("sku")) {
               String internalPid = JSONUtils.getStringValue(skuJson, "sku");
               String productUrl = CrawlerUtils.completeUrl(JSONUtils.getStringValue(skuJson, "url"), "https", HOST);

               saveDataProduct(null, internalPid, productUrl);

               this.log(
                     "Position: " + this.position +
                           " - InternalId: " + null +
                           " - InternalPid: " + internalPid +
                           " - Url: " + productUrl);

               if (this.arrayProducts.size() == productsLimit) {
                  break;
               }
            }
         }

      } else {
         this.result = false;
         this.log("Keyword sem resultado!");
      }

      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora "
            + this.arrayProducts.size() + " produtos crawleados");

   }


   @Override
   protected void setTotalProducts() {
      this.totalProducts = CrawlerUtils.scrapIntegerFromHtml(this.currentDoc, ".pagination  .itens-found", true, 0);
      this.log("Total de produtos: " + this.totalProducts);
   }

   private JSONArray crawlProductsArray(Document doc) {
      JSONArray productsArray = new JSONArray();

      Elements scripts = doc.select("script[type=\"application/ld+json\"]");

      for (Element e : scripts) {
         String script = e.html().trim();

         if (script.contains("sku") && script.startsWith("[") && script.endsWith("]")) {
            try {
               productsArray = new JSONArray(script);
            } catch (Exception e1) {
               Logging.printLogWarn(logger, session, CommonMethods.getStackTrace(e1));
            }

            break;
         }
      }

      return productsArray;
   }
}
