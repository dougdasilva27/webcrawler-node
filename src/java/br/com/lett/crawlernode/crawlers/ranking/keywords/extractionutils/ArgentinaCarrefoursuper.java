package br.com.lett.crawlernode.crawlers.ranking.keywords.extractionutils;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.methods.ApacheDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.HashMap;
import java.util.Map;

public abstract class ArgentinaCarrefoursuper extends CrawlerRankingKeywords {

   public ArgentinaCarrefoursuper(Session session) {
      super(session);
      super.fetchMode = FetchMode.FETCHER;
   }

   private static final String PRODUCTS_SELECTOR = ".home-product-cards .product-card .producto-info .open-modal[title]";
   private static final String HOST = "supermercado.carrefour.com.ar";
   private String storeId;


   /**
    * This function might return a cep from specific store
    * 
    * @return
    */
   protected abstract String getCep();


   @Override
   public void processBeforeFetch() {
      String url = "https://supermercado.carrefour.com.ar/envios/ajax/validatePostcode?postcode="+getCep();

      Request request = RequestBuilder.create().setUrl(url).build();

      JSONObject body = JSONUtils.stringToJson(new ApacheDataFetcher().get(session, request).getBody());

      storeId = body.optString("store");
   }

   @Override
   protected Document fetchDocument(String url) {
      this.currentDoc = new Document(url);

      if (this.currentPage == 1) {
         this.session.setOriginalURL(url);
      }

      Map<String, String> headers = new HashMap<>();

      headers.put("cookie", "sucursal_id="+storeId+";");
      headers.put("authority", "supermercado.carrefour.com.ar");

      Request request = RequestBuilder.create()
         .setUrl(url)
         .setHeaders(headers)
         .build();

      Response response = dataFetcher.get(session, request);

      Document doc = Jsoup.parse(response.getBody());

      takeAScreenshot(url, cookies);
      return doc;
   }

   @Override
   protected void extractProductsFromCurrentPage() {
      this.pageSize = 10;
      this.log("Página " + this.currentPage);

      String url = "https://" + HOST + "/catalogsearch/result/?q=" + this.keywordEncoded + "&p=" + this.currentPage;

      this.log("Link onde são feitos os crawlers: " + url);
      this.currentDoc = fetchDocument(url);
      Elements products = this.currentDoc.select(PRODUCTS_SELECTOR);

      if (!products.isEmpty()) {
         if (this.totalProducts == 0) {
            setTotalProductsCarrefour();
         }
         for (Element e : products) {

            String internalId = e.attr("data-id");
            String productUrl = CrawlerUtils.completeUrl(e.attr("href"), "https", HOST);

            saveDataProduct(internalId, null, productUrl);

            Element nameEl = e.selectFirst(".title.title-food");
            this.log("Position: " + this.position + " - InternalId: " + internalId + " - Url: " + productUrl + " - Name: " + (nameEl != null ? nameEl.text() : ""));
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

   protected void setTotalProductsCarrefour() {
      this.totalProducts = CrawlerUtils.scrapIntegerFromHtml(this.currentDoc, ".resultados-count", false, 0);
      this.log("Total da busca: " + this.totalProducts);
   }
}
