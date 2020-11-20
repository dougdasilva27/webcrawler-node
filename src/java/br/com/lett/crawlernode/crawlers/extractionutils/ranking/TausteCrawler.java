package br.com.lett.crawlernode.crawlers.extractionutils.ranking;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;

import org.apache.http.cookie.Cookie;
import org.apache.http.impl.cookie.BasicClientCookie;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;


import java.util.ArrayList;
import java.util.List;

public abstract class TausteCrawler extends CrawlerRankingKeywords {

   private static final List<Cookie> COOKIES = new ArrayList<>();

   public TausteCrawler(Session session) {
      super(session);
   }

   protected abstract String getLocation();

   private Document fetchProducts(){

      String url = "https://www.tauste.com.br/" + this.keywordEncoded.replace("+", "%20") + "?PageNumber=" + this.currentPage;

      this.log("Link onde são feitos os crawlers: " + url);

      BasicClientCookie cookie = new BasicClientCookie("VTEXSC", "sc=" + getLocation());
      cookie.setDomain("www.tauste.com.br");
      cookie.setPath("/");
      COOKIES.add(cookie);

      Request request = Request.RequestBuilder.create().setUrl(url).setCookies(COOKIES).build();

      return Jsoup.parse(this.dataFetcher.get(session, request).getBody());
   }

   @Override
   protected void extractProductsFromCurrentPage() {

      this.log("Página " + this.currentPage);

      this.currentDoc = fetchProducts();

      Elements products = this.currentDoc.select(".productCard");

      if (products.size() >= 1) {

         if(this.totalProducts == 0){
            this.totalProducts = CrawlerUtils.scrapIntegerFromHtml(this.currentDoc, "span.resultado-busca-numero span.value",true, 0);
            this.log("Total da busca: " + this.totalProducts);
         }

         for (Element product : products) {

            String internalPid = CrawlerUtils.scrapStringSimpleInfoByAttribute(product, null, "data-product-id");

            String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(product, ".productCard__addWrap", "data-id");

            String urlProduct = CrawlerUtils.scrapStringSimpleInfoByAttribute(product, "a .productCard__image", "href");

            saveDataProduct(internalId, internalPid, urlProduct);

            this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + internalPid + " - Url: " + urlProduct);
            if (this.arrayProducts.size() == productsLimit) break;

         }
      } else {
         this.result = false;
         this.log("Keyword sem resultado!");
      }

      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");
   }

   @Override
   protected boolean hasNextPage(){
      return this.arrayProducts.size() < this.totalProducts;
   }
}
