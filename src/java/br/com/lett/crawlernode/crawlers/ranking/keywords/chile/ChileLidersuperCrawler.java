package br.com.lett.crawlernode.crawlers.ranking.keywords.chile;

import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.methods.ApacheDataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.DataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.FetcherDataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.JsoupDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.FetcherOptions;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChileLidersuperCrawler extends CrawlerRankingKeywords {

   public ChileLidersuperCrawler(Session session) {
      super(session);
   }

   private final List<String> proxies = Arrays.asList(
      ProxyCollection.LUMINATI_SERVER_BR,
      ProxyCollection.BUY,
      ProxyCollection.NETNUT_RESIDENTIAL_AR_HAPROXY,
      ProxyCollection.NETNUT_RESIDENTIAL_BR_HAPROXY,
      ProxyCollection.NETNUT_RESIDENTIAL_CO_HAPROXY,
      ProxyCollection.NETNUT_RESIDENTIAL_ES_HAPROXY);

   @Override
   protected void processBeforeFetch() {
      String url = "https://www.lider.cl/supermercado";
      Map<String, String> headers = new HashMap<>();
      headers.put("accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9");
      headers.put("authority", "www.pedidosya.com.ar");
      headers.put("accept-language", "pt-BR,pt;q=0.9,en-US;q=0.8,en;q=0.7");
      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .setProxyservice(proxies)
         .setFetcheroptions(FetcherOptions.FetcherOptionsBuilder.create().setForbiddenCssSelector("#px-captcha").build())
         .setSendUserAgent(false)
         .build();
      Response response = this.dataFetcher.get(session, request);

      if (!response.isSuccess()) {
         response = retryRequest(request, List.of(new ApacheDataFetcher(), new JsoupDataFetcher(), this.dataFetcher));
      }

      this.cookies = response.getCookies();
   }

   @Override
   protected Document fetchDocument(String url) {
      Map<String, String> headers = new HashMap<>();
      headers.put("accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9");
      headers.put("authority", "www.lider.cl");
      headers.put("referer", "https://www.lider.cl/supermercado/");
      headers.put("cookie", "cookieSearchTerms=" + keywordEncoded + ";" + CommonMethods.cookiesToString(cookies));
      headers.put("accept-language", "pt-BR,pt;q=0.9,en-US;q=0.8,en;q=0.7");

      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .setSendUserAgent(false)
         .setProxyservice(proxies)
         .setHeaders(headers)
         .build();
      Response response = new JsoupDataFetcher().get(session, request);

      if (!response.isSuccess()) {
         response = retryRequest(request, List.of(new ApacheDataFetcher(), new FetcherDataFetcher(), this.dataFetcher));
      }

      return Jsoup.parse(response.getBody());

   }

   @Override
   protected void extractProductsFromCurrentPage() throws MalformedProductException {
      this.pageSize = 15;
      this.log("Página " + this.currentPage);

      String url = "https://www.lider.cl/supermercado/search?Ntt=" + keywordEncoded + "&ost=" + keywordEncoded + "";

      this.log("Link onde são feitos os crawlers: " + url);
      this.currentDoc = fetchDocument(url);

      StringBuilder payload = new StringBuilder();
      payload.append("productNumbers=");

      Elements products = this.currentDoc.select(".product-item-box");

      products.forEach(p -> {
         String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(p, ".box-product.product-item-box", "prod-number");
         if (internalId != null) {
            payload.append(internalId).append("%2C");
         }
      });

      JSONArray jsonArray = fetchAvaibility(payload);

      if (!products.isEmpty()) {
         if (this.totalProducts == 0) {
            setTotalProducts();
         }

         for (Object o : jsonArray) {
            JSONObject product = (JSONObject) o;
            String internalId = product.optString("productNumber");
            Element e = this.currentDoc.selectFirst("." + internalId);
            String productUrl = CrawlerUtils.completeUrl(CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".product-details a", "href"), "https:", "www.lider.cl");
            String name = CrawlerUtils.scrapStringSimpleInfo(e, ".product-description", true);
            String imageUrl = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".photo-container img", "src");
            int price = CommonMethods.doublePriceToIntegerPrice(CrawlerUtils.scrapDoublePriceFromHtml(e, ".price-sell b", null, true, ',', session), 0);

            RankingProduct productRanking = RankingProductBuilder.create()
               .setUrl(productUrl)
               .setInternalId(internalId)
               .setName(name)
               .setPriceInCents(price)
               .setAvailability(setAvailability(product))
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

   protected boolean setAvailability(JSONObject jsonObject) {
      String available = jsonObject.optString("stockLevel");
      return available != null && available.contains("1");
   }

   protected JSONArray fetchAvaibility(StringBuilder payload) {

      String url = "https://www.lider.cl/supermercado/includes/inventory/inventoryInformation.jsp";
      payload.append("&useProfile=true&consolidate=true");
      Map<String, String> headers = new HashMap<>();
      headers.put("accept", "application/json, text/javascript, */*; q=0.01");
      headers.put("authority", "www.lider.cl");
      headers.put("referer", "https://www.lider.cl/supermercado/search?Ntt=" + keywordEncoded + "&ost=" + keywordEncoded + "");
      headers.put("cookie", CommonMethods.cookiesToString(cookies));
      headers.put("accept-language", "pt-BR,pt;q=0.9,en-US;q=0.8,en;q=0.7");

      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .setHeaders(headers)
         .setProxyservice(proxies)
         .setPayload(payload.toString())
         .build();

      Response response = new JsoupDataFetcher().post(session, request);

      if (!response.isSuccess()) {
         response = retryRequest(request, List.of(new ApacheDataFetcher(), new JsoupDataFetcher(), this.dataFetcher));
      }


      return CrawlerUtils.stringToJsonArray(response.getBody());
   }

   private Response retryRequest(Request request, List<DataFetcher> dataFetcherList) {
      Response response = dataFetcherList.get(0).get(session, request);

      if (!response.isSuccess()) {
         int tries = 0;
         while (!response.isSuccess() && tries < 3) {
            tries++;
            if (tries % 2 == 0) {
               response = dataFetcherList.get(1).get(session, request);
            } else {
               response = dataFetcherList.get(2).get(session, request);
            }
         }
      }

      return response;
   }

   @Override
   protected void setTotalProducts() {
      Element total = this.currentDoc.selectFirst(".result-departments a:last-child span");
      if (total != null) {
         String text = total.ownText().replaceAll("[^0-9]", "");

         if (!text.isEmpty()) {
            this.totalProducts = Integer.parseInt(text);
            this.log("Total da busca: " + this.totalProducts);
         }
      }
   }

}
