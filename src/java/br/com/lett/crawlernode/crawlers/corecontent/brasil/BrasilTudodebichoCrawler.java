package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.VTEXNewImpl;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import br.com.lett.crawlernode.util.MathUtils;
import models.RatingsReviews;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONObject;
import org.jsoup.nodes.Document;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Locale;

public class BrasilTudodebichoCrawler extends VTEXNewImpl {

   public BrasilTudodebichoCrawler(@NotNull Session session) {
      super(session);
   }

   @Override
   protected String scrapName(Document doc, JSONObject productJson, JSONObject jsonSku) {
      String name = null;

      if(jsonSku != null && jsonSku.has("nameComplete")){
         name = jsonSku.optString("nameComplete");
      }

      if (jsonSku == null && productJson.has("productName")) {
         name = productJson.optString("productName");
      }

      if (name != null && !name.isEmpty() && productJson.has("brand")) {
         String brand = productJson.optString("brand");
         if (brand != null && !brand.isEmpty() && !checkIfNameHasBrand(brand, name)) {
            name = name + " " + brand;
         }
      }

      if (name != null && !name.isEmpty() && jsonSku.has("Tamanhos")) {
         String length = JSONUtils.getValueRecursive(jsonSku, "Tamanhos.0", String.class);
         if (length != null && !length.isEmpty() && !checkIfNameHasLength(length, name)) {
            name = name + " " + length;
         }
      }

      return name;
   }

   protected boolean checkIfNameHasLength(String brand, String name) {
      String brandStripAccents = StringUtils.stripAccents(brand);
      String nameStripAccents = StringUtils.stripAccents(name);
      return nameStripAccents.toLowerCase(Locale.ROOT).contains(brandStripAccents.toLowerCase(Locale.ROOT));
   }

   @Nullable
   @Override
   protected RatingsReviews scrapRating(@NotNull String internalId, @NotNull String internalPid, @NotNull Document doc, @NotNull JSONObject jsonSku) {
      String apiURL = "https://trustvox.com.br/widget/root?code=" + internalPid + "&store_id=117067&url=" + session.getOriginalURL();
      RatingsReviews ratingReviews = new RatingsReviews();
      HttpResponse<String> response;

      try {
         HttpClient client = HttpClient.newBuilder().build();
         HttpRequest request = HttpRequest.newBuilder()
            .GET()
            .header("Accept", "application/vnd.trustvox-v2+json")
            .header("Referer", "https://www.tudodebicho.com.br/")
            .uri(URI.create(apiURL))
            .build();
         response = client.send(request, HttpResponse.BodyHandlers.ofString());

      } catch (Exception e) {
         throw new RuntimeException("Failed in load document: " + session.getOriginalURL(), e);
      }

      JSONObject json = CrawlerUtils.stringToJson(response.body());
      JSONObject storeRate = json.optJSONObject("rate");

      if (storeRate != null) {
         Double avgRating = MathUtils.parseDoubleWithDot(storeRate.optString("average"));
         int count = storeRate.optInt("count");
         ratingReviews.setAverageOverallRating(avgRating);
         ratingReviews.setTotalRating(count);
         ratingReviews.setTotalWrittenReviews(count);
      }

      return ratingReviews;
   }
}
