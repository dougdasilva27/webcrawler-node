package br.com.lett.crawlernode.crawlers.ranking.keywords.argentina;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;

public class ArgentinaSupermercadolaanonimaonlinecipollettiCrawler extends CrawlerRankingKeywords {

   private List<Cookie> cookies = new ArrayList<>();

   public ArgentinaSupermercadolaanonimaonlinecipollettiCrawler(Session session) {

      super(session);
      super.fetchMode = FetchMode.FETCHER;

   }

   @Override
   protected void processBeforeFetch() {
      // Criando cookie da cidade CABA
      BasicClientCookie cookie = new BasicClientCookie("laanonimasucursalnombre", "CIPOLLETTI");
      cookie.setDomain("www.laanonimaonline.com");
      cookie.setPath("/");
      this.cookies.add(cookie);

      // Criando cookie da regiao sao nicolas
      BasicClientCookie cookie2 = new BasicClientCookie("laanonimasucursal", "22");
      cookie2.setDomain("www.laanonimaonline.com");
      cookie2.setPath("/");
      this.cookies.add(cookie2);
   }

   @Override
   protected void extractProductsFromCurrentPage() {
      // número de produtos por página do market
      this.pageSize = 20;

      this.log("Página " + this.currentPage);

      String url = "https://supermercado.laanonimaonline.com/buscar?pag=" + this.currentPage + "&clave=" + this.keywordWithoutAccents.replace(" ", "%20");
      System.err.println(url);

      this.log("Link onde são feitos os crawlers: " + url);
      this.currentDoc = fetchDocument(url, cookies);
      Elements products = this.currentDoc.select(".caja1.producto > div");

      if (!products.isEmpty()) {

         for (Element e : products) {
            String productUrl = crawlProductUrl(e);
            String internalId = crawlInternalId(productUrl);

            saveDataProduct(internalId, null, productUrl);

            this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + null + " - Url: " + productUrl);
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
   protected boolean hasNextPage() {
      return true;
   }

   private String crawlInternalId(String url) {
      String internalId = null;

      if (url.contains("art_")) {
         internalId = CommonMethods.getLast(url.split("art_"));

         if (internalId.contains("?")) {
            internalId = internalId.split("\\?")[0];
         }

         if (internalId.contains("/")) {
            internalId = internalId.split("\\/")[0];
         }
      }

      return internalId;
   }

   private String crawlProductUrl(Element e) {

      String url = CrawlerUtils.scrapStringSimpleInfoByAttribute(e,".titulo_puntos a","href");
      String productUrl = CrawlerUtils.completeUrl(url,"https:","supermercado.laanonimaonline.com");

      return productUrl;
   }
}
