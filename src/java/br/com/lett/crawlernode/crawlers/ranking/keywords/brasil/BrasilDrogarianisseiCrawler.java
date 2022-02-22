package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.methods.FetcherDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class BrasilDrogarianisseiCrawler extends CrawlerRankingKeywords {

   private static final String API = "https://www.farmaciasnissei.com.br/pesquisa/pesquisar";

   private static final String API_TOKEN = "csrfmiddlewaretoken=VXmeDdpaXJzt9jO6h0qDrG3bSI3oSKjeyLKtQTWw7vN9j9hyMec1vTY1BYvQCILK";

   private static final String API_COOKIE = "csrftoken=n3tYeFuItzXK77qs76BV2Che4gHeyyNU0RRdrl14DlbqhXTUCknj6Pc4Nw9Giwfq;";

   public BrasilDrogarianisseiCrawler(Session session) {
      super(session);
      super.dataFetcher = new FetcherDataFetcher();
   }

   @Override
   protected void extractProductsFromCurrentPage() throws MalformedProductException {
      JSONObject productsJson = fetchJSONObject(API);

      if (this.totalProducts == 0) {
         this.totalProducts = productsJson.optInt("quantidade");
      }

      JSONArray products = JSONUtils.getJSONArrayValue(productsJson, "produtos");

      if (!products.isEmpty()) {
         HashMap<String, Integer> productPrices = fetchProductPrices(products);

         for (Object e : products) {
            JSONObject product = (JSONObject) e;
            JSONObject productInfo = product.optJSONObject("_source");

            String internalId = product.optString("_id");
            String productUrl = scrapProductUrl(product);
            String imageUrl = scrapImageUrl(productInfo);
            String name = productInfo.optString("nm_produto");
            int price = productPrices.getOrDefault(internalId, 0);
            boolean isAvailable = productInfo.optBoolean("is_disponivel");

            RankingProduct productRanking = RankingProductBuilder.create()
               .setUrl(productUrl)
               .setInternalId(internalId)
               .setInternalPid(null)
               .setImageUrl(imageUrl)
               .setName(name)
               .setPriceInCents(price)
               .setAvailability(isAvailable)
               .build();

            saveDataProduct(productRanking);

            if (this.arrayProducts.size() == productsLimit) break;
         }
      } else {
         this.result = false;
         this.log("Keyword sem resultado!");
      }

      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");
   }

   private String scrapImageUrl(JSONObject productInfo) {
      String path = productInfo.optString("imagem_produto");
      if (path.isEmpty()) return null;
      return "https://www.farmaciasnissei.com.br/media" + path;
   }

   protected JSONObject fetchJSONObject(String url) {
      String payload = API_TOKEN + "&termo=pesquisa%2F" + this.keywordEncoded + "&pagina=" + this.currentPage + "&is_termo=true";

      HashMap<String, String> headers = new HashMap<>();
      headers.put("content-type", "application/x-www-form-urlencoded");
      headers.put("referer", "https://www.farmaciasnissei.com.br/pesquisa/absorvente");
      headers.put("cookie", API_COOKIE);

      Request request = Request.RequestBuilder.create().setPayload(payload).setHeaders(headers).setCookies(cookies).setUrl(url).build();
      String response = dataFetcher.post(session, request).getBody();

      return CrawlerUtils.stringToJson(response);
   }

   private HashMap<String, Integer> fetchProductPrices(JSONArray products) {
      HashMap<String, Integer> productPrices = new HashMap<>();

      List<String> productIds = IntStream.range(0, products.length()).mapToObj(i -> products.getJSONObject(i).optString("_id")).collect(Collectors.toList());
      String payload = API_TOKEN + "&produtos_ids%5B%5D=" + String.join("&produtos_ids%5B%5D=", productIds);

      HashMap<String, String> headers = new HashMap<>();
      headers.put("content-type", "application/x-www-form-urlencoded; charset=UTF-8");
      headers.put("referer", "https://www.farmaciasnissei.com.br/pesquisa/" + this.keywordEncoded);
      headers.put("cookie", API_COOKIE);

      Request request = Request.RequestBuilder.create()
         .setPayload(payload)
         .setHeaders(headers)
         .setCookies(cookies)
         .setUrl("https://www.farmaciasnissei.com.br/pegar/preco")
         .build();
      String response = dataFetcher.post(session, request).getBody();

      JSONObject result = CrawlerUtils.stringToJson(response);
      JSONObject pricesJSON = result.optJSONObject("precos");

      if (pricesJSON != null && !pricesJSON.isEmpty()) {
         pricesJSON.keys().forEachRemaining(key -> {
            JSONObject product = (JSONObject) pricesJSON.get(key);
            JSONObject publicPrice = product.optJSONObject("publico");
            if (publicPrice != null) {
               String id = publicPrice.optString("produto_id");
               int price = JSONUtils.getPriceInCents(publicPrice, "valor_fim_somado");
               productPrices.put(id, price);
            }
         });
      }
      return productPrices;
   }


   private String scrapProductUrl(JSONObject product) {
      String url = null;
      JSONObject source = product.optJSONObject("_source");

      if (source != null) {
         String urlPart = source.optString("url_produto");

         if (urlPart != null) {
            url = CrawlerUtils.completeUrl(urlPart, "https", "www.farmaciasnissei.com.br");
         }
      }
      return url;
   }

}
