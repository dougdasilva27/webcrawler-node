package br.com.lett.crawlernode.crawlers.ranking.keywords.argentina;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.methods.ApacheDataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.FetcherDataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.JsoupDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import br.com.lett.crawlernode.util.MathUtils;
import org.apache.http.HttpHeaders;
import org.apache.http.cookie.Cookie;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.*;

public class ArgentinaSupermercadolaanonimaonlinecipollettiCrawler extends CrawlerRankingKeywords {

   private List<Cookie> cookies = new ArrayList<>();

   public ArgentinaSupermercadolaanonimaonlinecipollettiCrawler(Session session) {

      super(session);
      super.fetchMode = FetchMode.FETCHER;

   }

   @Override
   protected Document fetchDocument(String url, List<Cookie> cookies) {
      this.currentDoc = new Document(url);

      if (this.currentPage == 1) {
         this.session.setOriginalURL(url);
      }

      Map<String, String> headers = new HashMap<>();
      headers.put("cookie", " laanonimasucursalnombre=CIPOLLETTI; laanonimasucursal=22");
      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .setHeaders(headers)
         .build();
      Response response = new ApacheDataFetcher().get(session, request);

      return Jsoup.parse(response.getBody());
   }

   @Override
   protected void extractProductsFromCurrentPage() throws MalformedProductException {
      // número de produtos por página do market
      this.pageSize = 20;

      this.log("Página " + this.currentPage);

      String url = "https://supermercado.laanonimaonline.com/buscar?pag=" + this.currentPage + "&clave=" + this.keywordWithoutAccents.replace(" ", "%20");

      this.log("Link onde são feitos os crawlers: " + url);
      this.currentDoc = fetchDocument(url, cookies);
      Elements products = this.currentDoc.select(".caja1.producto > div");

      if (!products.isEmpty()) {

         List<String> internalPids = new ArrayList<>();
         Map<String, Integer> productStock = new HashMap<>();

         for (Element e : products) {
            String internalPid = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, "input[name*=sku_item_imetrics]", "value");
            internalPids.add(internalPid);
         }
         productStock = getStock(internalPids);

         for (Element e : products) {
            String internalPid = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, "input[name*=sku_item_imetrics]", "value");
            if (productStock.get(internalPid) != null && productStock.get(internalPid) >= 0) {
               String internalId = crawlInternalId(e);
               String productUrl = crawlProductUrl(e);
               String name = CrawlerUtils.scrapStringSimpleInfo(e, "div.titulo_puntos a", true);
               Integer price = crawlPrice(e);
               String imageUrl = CrawlerUtils.scrapSimplePrimaryImage(e, "div.producto img", Arrays.asList("data-src"), "https", "supermercado.laanonimaonline.com");
               boolean isAvailable = productStock.get(internalPid) > 0;
               if (!isAvailable) {
                  price = null;
               }

               RankingProduct productRanking = RankingProductBuilder.create()
                  .setUrl(productUrl)
                  .setInternalId(internalId)
                  .setInternalPid(internalPid)
                  .setImageUrl(imageUrl)
                  .setName(name)
                  .setPriceInCents(price)
                  .setAvailability(isAvailable)
                  .build();

               saveDataProduct(productRanking);

               if (this.arrayProducts.size() == productsLimit) {
                  break;
               }
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

   private String crawlInternalId(Element document) {
      return CrawlerUtils.scrapStringSimpleInfoByAttribute(document, "input[name*=id_item_imetrics]", "value");
   }

   private String crawlProductUrl(Element e) {

      String url = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".titulo_puntos a", "href");
      String productUrl = CrawlerUtils.completeUrl(url, "https:", "supermercado.laanonimaonline.com");

      return productUrl;
   }

   private Integer crawlPrice(Element e) {
      String priceStr = CrawlerUtils.scrapStringSimpleInfo(e, ".precio.semibold", true);
      String priceDecimal = CrawlerUtils.scrapStringSimpleInfo(e, ".decimales", true);
      String priceComplete = priceStr + priceDecimal;
      Double price = MathUtils.parseDoubleWithComma(priceComplete);

      return price != null ? (int) Math.round(price * 100) : null;
   }

   protected Map<String, Integer> getStock(List<String> internalPids) {

      StringBuilder sb = new StringBuilder();
      for (String s : internalPids) {
         if (sb.length() > 0) {
            sb.append("&");
         }
         sb.append("datos%5B%5D=");
         sb.append(s);
      }

      Map<String, String> headers = new HashMap<>();
      headers.put(HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded; charset=UTF-8");
      headers.put("cookie", "_gcl_au=1.1.171006165.1663695275; laanonimaapp=0; laanonima=56264d8c6f6e4e4089d3a8c37934fe10; mostrarConfSucursal=0; _ga=GA1.2.776627427.1663695276; _gid=GA1.2.1890901966.1663695276; _fbp=fb.1.1663695276420.54648718; _hjFirstSeen=1; _hjIncludedInSessionSample=0; _hjSession_2758890=eyJpZCI6IjQ1ZmYyNGVhLWJkZWMtNDE0YS05OWQzLTA0NzZjOTU3OWQ1OSIsImNyZWF0ZWQiOjE2NjM4NjkyNjEyMjIsImluU2FtcGxlIjpmYWxzZX0=; _hjAbsoluteSessionInProgress=0; mostrar_ventana_sucursales=0; _hjSessionUser_2758890=eyJpZCI6IjY1YzRkMGQwLWFmMDUtNTA1NS1hMDE0LTY3MWMyOTFlYWQyZCIsImNyZWF0ZWQiOjE2NjM2OTUyNzYzODYsImV4aXN0aW5nIjp0cnVlfQ==; laanonimasucursalnombre=CIPOLLETTI; laanonimasucursal=22; laanonimadatos=YTo0OntzOjE2OiJjb2RpZ29fc2VndXJpZGFkIjtzOjY6IjhjeHg5ZyI7czoxMzoibnVtZXJvY2Fycml0byI7czozMjoiYjBjODExODc3YTc2YjI1MjJhMGM3YWRkMjBlNDZiNGIiO3M6MTU6InRfbnVtZXJvY2Fycml0byI7czo3OiJhbm9uaW1vIjtzOjEyOiJtb250b19taW5pbW8iO2Q6MDt9; laanonimaordenlistado=relevancia; cartelLogueado=0; _ga_W3SKYF6FJF=GS1.1.1663869260.9.1.1663869345.58.0.0;");
      Request request = Request.RequestBuilder.create()
         .setUrl("https://supermercado.laanonimaonline.com/paginas/controlStockListados.php")
         .setHeaders(headers)
         .setSendUserAgent(true)
         .setPayload(sb.toString())
         .build();
      Response response = new FetcherDataFetcher().post(session, request);
      JSONArray array = JSONUtils.stringToJsonArray(response.getBody());
      Map<String, Integer> productStock = new HashMap<>();
      for (Object obj : array) {
         JSONObject jsonObject = (JSONObject) obj;
         String codigo = jsonObject.optString("codigo");
         Integer stock = jsonObject.optInt("stock");
         productStock.put(codigo, stock);
      }

      return productStock;
   }
}
