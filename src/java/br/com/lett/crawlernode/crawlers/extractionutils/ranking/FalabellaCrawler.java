package br.com.lett.crawlernode.crawlers.extractionutils.ranking;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.MathUtils;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class FalabellaCrawler extends CrawlerRankingKeywords {

   private final String HOME_PAGE = getHomePage();

   protected FalabellaCrawler(Session session) {
      super(session);
   }

   protected abstract String getHomePage();

   @Override
   protected void extractProductsFromCurrentPage() {
      this.pageSize = 48;

      this.log("Página " + this.currentPage);

      String url = HOME_PAGE + "search/?Ntt=" + this.keywordEncoded + "&page=" + this.currentPage;
      this.log("Link onde são feitos os crawlers: " + url);

      this.currentDoc = fetchDocument(url);

      Elements products = this.currentDoc.select(".search-results--products > div");

      if (!products.isEmpty()) {
         if (this.totalProducts == 0) {
            setTotalProducts();
         }
         for (Element e : products) {

            String internalId = scarpInternalId(e);
            String internalPId = internalId;
            String productUrl = crawlProductUrl(e);

            saveDataProduct(internalId, internalPId, productUrl);

            this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + internalPId + " - Url: " + productUrl);
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

   protected String scarpInternalId(Element e){
      String value = CrawlerUtils.scrapStringSimpleInfoByAttribute(e,"div[id*=testId-pod-]","id");
      return CommonMethods .getLast(value.split("-"));
   }

   @Override
   protected void setTotalProducts() {
      String result = CrawlerUtils.scrapStringSimpleInfo(this.currentDoc, "#search_numResults", true);

      String total = getTotal(result);

      this.totalProducts = total != null ? Integer.parseInt(total) : 0;
      this.log("Total da busca: " + this.totalProducts);
   }


   private String getTotal(String result) {
      String total = null;
      Pattern pattern = Pattern.compile("\\de([0-9]*)\\resultados");
      Matcher matcher = pattern.matcher(result);
      if (matcher.find()) {
         total = matcher.group(1);
      }
      return total;
   }

   private String crawlProductUrl(Element e) {
      return  CrawlerUtils.scrapStringSimpleInfoByAttribute(e,".pod-head a","href");
   }

}
