package br.com.lett.crawlernode.crawlers.ranking.keywords.saopaulo;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.MathUtils;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CommonMethods;

import java.lang.reflect.Proxy;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;

public class SaopauloPanvelCrawler extends CrawlerRankingKeywords {

   public SaopauloPanvelCrawler(Session session) {
      super(session);
      fetchMode = FetchMode.APACHE;
      cookies.add(new BasicClientCookie("stc112189", String.valueOf(LocalDate.now().toEpochDay())));
   }

   @Override
   protected Document fetchDocument(String url) {
      Request request = Request.RequestBuilder.create().setCookies(cookies).setUrl(url)
         .mustSendContentEncoding(false)
         .setSendUserAgent(false)
         .setProxyservice(Arrays.asList(ProxyCollection.BUY, ProxyCollection.NETNUT_RESIDENTIAL_BR_HAPROXY, ProxyCollection.NETNUT_RESIDENTIAL_BR_HAPROXY))
         .build();

      Response response = CrawlerUtils.retryRequest(request, session, this.dataFetcher, true);

      return Jsoup.parse(response.getBody());
   }

   @Override
   protected void extractProductsFromCurrentPage() throws MalformedProductException {
      this.pageSize = 16;
      this.log("Página " + this.currentPage);

      String url = "https://www.panvel.com/panvel/buscarProduto.do?termoPesquisa=" + keywordEncoded + "&paginaAtual=" + currentPage;
      this.log("Link onde são feitos os crawlers: " + url);

      this.currentDoc = fetchDocument(url);
      JSONArray products = crawlProducts(currentDoc);

      if (!products.isEmpty()) {
         for (int i = 0; i < products.length(); i++) {
            JSONObject productJson = products.optJSONObject(i);

            String urlProduct = productJson.optString("link") != null ? "https://www.panvel.com" + productJson.optString("link") : null;
            String internalId = productJson.optString("panvelCode");
            String name = productJson.optString("name");
            String image = productJson.optString("image");
            Double price = productJson.optJSONObject("discount").optDouble("dealPrice");
            Integer priceInCents = price != null ? MathUtils.parseInt(price * 100) : 0;
            boolean isAvailable = priceInCents > 0;

            RankingProduct productRanking = RankingProductBuilder.create()
               .setUrl(urlProduct)
               .setInternalId(internalId)
               .setName(name)
               .setImageUrl(image)
               .setPriceInCents(priceInCents)
               .setAvailability(isAvailable)
               .build();

            saveDataProduct(productRanking);

            if (this.arrayProducts.size() == productsLimit)
               break;
         }
      } else {
         this.result = false;
         this.log("Keyword sem resultado!");
      }

      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");
   }

   private JSONArray crawlProducts(Document doc) {
      Element serverAppStateScript = doc.selectFirst("#serverApp-state");
      String serverAppState = serverAppStateScript != null && !serverAppStateScript.childNodes().isEmpty() ? serverAppStateScript.childNodes().get(0).toString() : null;

      if (serverAppState != null) {
         int startIndex = serverAppState.indexOf(",&q;items&q;:") + ",".length();
         int lastIndex = serverAppState.indexOf("}],&q;showcases&q;", startIndex);

         String productsString = serverAppState.substring(startIndex, lastIndex) + "}]";
         String productsStringSanitized = productsString.replace("&q;", "\"");

         JSONObject items = CrawlerUtils.stringToJson("{" + productsStringSanitized + "}");
         JSONArray products = items.optJSONArray("items");

         return products;
      } else {
         return new JSONArray();
      }
   }

   @Override
   protected boolean hasNextPage() {
      return (arrayProducts.size() % pageSize - currentPage) < 0;
   }
}
