package br.com.lett.crawlernode.crawlers.extractionutils.ranking;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Documentação oficial LinxImpulseAPI: (https://docs.linximpulse.com/v3-search/docs/search)
 */
public class LinxImpulseRanking extends CrawlerRankingKeywords {
   protected String apiKey;
   protected String homePage;
   protected List<String> salesChannel;
   protected String sortBy = "relevance";
   protected boolean showOnlyAvailable = false;

   public LinxImpulseRanking(Session session) {
      super(session);
      validateOptions(session.getOptions());
   }

   protected void validateOptions(JSONObject options) {
      this.apiKey = options.optString("apiKey");
      this.homePage = options.optString("homePage");
      this.sortBy = options.optString("sortBy", "relevance");
      this.showOnlyAvailable = options.optBoolean("showOnlyAvailable", false);
      this.pageSize = options.optInt("resultsPerPage", 20);
      Object salesChannelObject = options.opt("salesChannel");
      if (salesChannelObject instanceof JSONArray) {
         JSONArray salesChannelArray = (JSONArray) salesChannelObject;
         this.salesChannel = salesChannelArray.toList().stream().map(Object::toString).collect(Collectors.toList());
      } else if (salesChannelObject instanceof String) {
         this.salesChannel = List.of(salesChannelObject.toString());
      }
   }

   @Override
   protected void processBeforeFetch() {
      JSONObject cookies = session.getOptions().optJSONObject("cookies");
      if (cookies != null && !cookies.isEmpty()) {
         cookies.toMap().forEach((key, value) -> this.cookies.add(new BasicClientCookie(key, value.toString())));
      }
   }

   @Override
   protected void extractProductsFromCurrentPage() throws UnsupportedEncodingException, MalformedProductException {
      String url = mountURL();
      JSONObject data = fetchPage(url);
      JSONArray products = data.optJSONArray("products");

      if (products != null && !products.isEmpty()) {
         this.totalProducts = data.optInt("size");
         for (Object object : products) {
            JSONObject product = (JSONObject) object;
            String productUrl = crawlProductUrl(product);
            String internalPid = crawlInternalPid(product);
            String internalId = crawlInternalId(product, internalPid);
            String name = product.optString("name");
            String image = crawlImage(product);
            int priceInCents = crawlPrice(product);
            boolean isAvailable = crawlAvailability(product);


            try {
               RankingProduct rankingProduct = RankingProductBuilder.create()
                  .setUrl(productUrl)
                  .setInternalId(internalId)
                  .setName(name)
                  .setImageUrl(image)
                  .setPriceInCents(priceInCents)
                  .setAvailability(isAvailable)
                  .build();

               saveDataProduct(rankingProduct);
            } catch (MalformedProductException e) {
               this.log(e.getMessage());
            }
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

   protected boolean crawlAvailability(JSONObject product) {
      return product.optString("status", "").equalsIgnoreCase("AVAILABLE");
   }

   protected String mountURL() {
      URIBuilder uriBuilder = new URIBuilder()
         .setScheme("https")
         .setHost("api.linximpulse.com")
         .setPath("/engage/search/v3/search")
         .addParameter("apiKey", this.apiKey)
         .addParameter("page", String.valueOf(this.currentPage))
         .addParameter("resultsPerPage", String.valueOf(this.pageSize))
         .addParameter("terms", this.keywordEncoded)
         .addParameter("sortBy", this.sortBy)
         .addParameter("showOnlyAvailable", String.valueOf(this.showOnlyAvailable));

      if (this.salesChannel != null && !this.salesChannel.isEmpty()) {
         salesChannel.forEach(channel -> uriBuilder.addParameter("salesChannel", channel));
      }

      return uriBuilder.toString();
   }

   protected JSONObject fetchPage(String url) {
      Map<String, String> headers = new HashMap<>();
      headers.put("origin", this.homePage);

      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .setHeaders(headers)
         .build();

      Response response = dataFetcher.get(session, request);

      return CrawlerUtils.stringToJSONObject(response.getBody());
   }

   protected String crawlInternalPid(JSONObject product) {
      String internalPid = product.optString("id");
      Object sanitizedInternalPid = product.optQuery("/collectInfo/productId");
      if (sanitizedInternalPid != null) {
         internalPid = sanitizedInternalPid.toString();
      }
      return internalPid;
   }

   protected String crawlInternalId(JSONObject product, String internalPid) {
      String internalId = internalPid;
      JSONArray skus = product.optJSONArray("skus");

      if (skus != null && !skus.isEmpty()) {
         JSONObject sku = skus.getJSONObject(0);
         String alternativeId = sku.optString("sku");
         if (!alternativeId.equals(internalPid)) {
            internalId = alternativeId;
         }
      }

      if (salesChannel != null && !salesChannel.isEmpty()) {
         String finalInternalId = internalId;
         String matchChannel = salesChannel.stream().filter(channel -> finalInternalId.startsWith(channel + "_")).findFirst().orElse(null);
         if (matchChannel != null && !matchChannel.isEmpty()) {
            internalId = internalId.replaceAll(matchChannel + "_", "");
         }
      }

      return internalId;
   }

   protected String crawlProductUrl(JSONObject product) {
      String url = product.optString("url");
      if (url.startsWith("//")) {
         url = "https:" + url;
      } else if (url.startsWith("www.")) {
         url = "https://" + url;
      } else if (!url.startsWith(this.homePage)) {
         url = this.homePage + url;
      }
      return url;
   }

   protected int crawlPrice(JSONObject product) {
      int priceInCents = 0;
      Object price = product.opt("price");
      if (price instanceof Double) {
         priceInCents = (int) Math.round((Double) price * 100);
      } else if (price instanceof Integer) {
         priceInCents = (int) price * 100;
      }
      return priceInCents;
   }

   protected String crawlImage(JSONObject product) {
      String image = "";
      Object imageObject = product.optQuery("/images/default");
      if (imageObject instanceof String) {
         image = imageObject.toString();
         if (image.startsWith("//")) {
            image = "https:" + image;
         }
      }
      return image;
   }
}
