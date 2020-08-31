package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.HashMap;
import java.util.Map;

public class BrasilKalungaCrawler extends CrawlerRankingKeywords {

   private static String token = null;

   public BrasilKalungaCrawler(Session session) {
      super(session);
      super.fetchMode = FetchMode.APACHE;
   }

   @Override
   protected void extractProductsFromCurrentPage() {

      this.log("Página " + this.currentPage);

      // monta a url com a keyword e a página
      String url = "https://www.kalunga.com.br/busca/" + this.keywordWithoutAccents + "/1";

      this.currentDoc = fetchDocument(url);
      Elements products = this.currentDoc.select(".blocoproduto .col-4 a:not(.small):first-child");

      if (!products.isEmpty()) {
         if (this.totalProducts == 0)
            setTotalProducts();

         for (Element e : products) {
            // InternalPid
            String internalPid = crawlInternalPid(e);

            // InternalId
            String internalId = internalPid;

            // monta a url
            String productUrl = crawlProductUrl(e);

            saveDataProduct(internalId, internalPid, productUrl);

            this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + internalPid + " - Url: " + productUrl);
            if (this.arrayProducts.size() == productsLimit) {
               break;
            }
         }
      } else {
         this.result = false;
         this.log("Keyword sem resultados!");
      }

      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");

   }


   protected void setTotalBusca(JSONObject apiSearch) {
      if (apiSearch.has("quantidade")) {
         try {
            this.totalProducts = Integer.parseInt(apiSearch.getString("quantidade"));
         } catch (Exception e) {
            this.logError(CommonMethods.getStackTraceString(e));
         }
      }

      this.log("Total da busca: " + this.totalProducts);
   }


   private String crawlInternalPid(Element e) {
      String internalPid;

      String[] tokens = e.attr("href").split("/");
      internalPid = tokens[tokens.length - 1].split("\\?")[0].replaceAll("[^0-9]", "");

      return internalPid;
   }

   private String crawlProductUrl(Element e) {
      String productUrl;
      productUrl = e.attr("href");

      if (!productUrl.contains("kalunga")) {
         productUrl = ("https://www.kalunga.com.br/" + productUrl).replace("br//", "br/");
      }

      if (productUrl.contains("?")) {
         productUrl = productUrl.split("\\?")[0];
      }

      return productUrl;
   }

   @Override
   protected void setTotalProducts() {
      Element totalElement = this.currentDoc.selectFirst(".text-primary.spnQtdeRegistros");

      try {
         if (totalElement != null)
            this.totalProducts = Integer.parseInt(totalElement.text());
      } catch (Exception e) {
         this.logError(e.getMessage());
      }

      this.log("Total da busca: " + this.totalProducts);
   }
}
