package br.com.lett.crawlernode.crawlers.corecontent.brasil;


import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.*;
import models.*;
import models.prices.Prices;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.*;
import java.util.Map.Entry;

/**
 * date: 27/03/2018
 *
 * @author gabriel
 */

public class BrasilCentauroCrawler extends Crawler {

   private static final String MAIN_SELLER_NAME_LOWER = "centauro";

   //This token is hardcoded because contains information about location and store id.
   private static final String BEARER_TOKEN = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1bmlxdWVfbmFtZSI6ImZyb250LWVuZCBjZW50YXVybyIsIm5iZiI6MTU4OTkxOTgxMywiZXhwIjoxOTA1NDUyNjEzLCJpYXQiOjE1ODk5MTk4MTN9.YeCTBYcWozaQb4MnILtfeKTeyCwApNgLSOfGeVVM8D0";

   public BrasilCentauroCrawler(Session session) {
      super(session);
   }

   @Override
   protected Object fetch() {
      String url = "https://apigateway.centauro.com.br/ecommerce/v4.3/produtos?codigoModelo=/";
      String slug = CommonMethods.getLast(session.getOriginalURL().split("/"));

      if (slug.contains("?")) {
         url += slug.split("\\?")[0];
      } else {
         url += slug;
      }

      Map<String, String> headers = new HashMap<>();
      headers.put("authorization", "Bearer " + BEARER_TOKEN);

      Request request = Request.RequestBuilder.create()
         .setHeaders(headers)
         .setUrl(url)
         .build();
      Response response = this.dataFetcher.get(session, request);

      return CrawlerUtils.stringToJson(response.getBody());
   }

   @Override
   public List<Product> extractInformation(JSONObject json) throws Exception {
      super.extractInformation(json);
      List<Product> products = new ArrayList<>();

      if (json.has("informacoes")) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String internalPid = JSONUtils.getValueRecursive(json, "informacoes.codigo", String.class);
         String name = JSONUtils.getValueRecursive(json, "informacoes.nome", String.class);
//         CategoryCollection categories = scrapCategories(jsonProduct);
//         List<String> images = scrapImages(jsonProduct);
//         String primaryImage = !images.isEmpty() ? images.remove(0) : null;
//         String description = jsonProduct.optString("FullDescription") + "\n" + jsonProduct.optString("BrandDescription");
//         RatingsReviews ratings = scrapRatings(internalPid);
//         Offers offers = scrapOffers(jsonProduct);

         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
//            .setInternalId(internalId)
//            .setInternalPid(internalPid)
//            .setName(name)
//               .setCategories(categories)
//               .setPrimaryImage(primaryImage)
//               .setSecondaryImages(images)
//               .setDescription(description)
//               .setRatingReviews(ratings)
//               .setOffers(offers)
            .build();

         products.add(product);
      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }

   protected CategoryCollection scrapCategories(JSONObject json){
      CategoryCollection categories = new CategoryCollection();

      categories.add(JSONUtils.getValueRecursive(json, "informacoes.grupo", String.class));
      categories.add(JSONUtils.getValueRecursive(json, "informacoes.subGrupo", String.class));
      categories.add(JSONUtils.getValueRecursive(json, "informacoes.genero", String.class));

      return categories;
   }

   protected CategoryCollection scrapImages(JSONObject json){
      CategoryCollection categories = new CategoryCollection();

      categories.add(JSONUtils.getValueRecursive(json, "informacoes.grupo", String.class));
      categories.add(JSONUtils.getValueRecursive(json, "informacoes.subGrupo", String.class));
      categories.add(JSONUtils.getValueRecursive(json, "informacoes.genero", String.class));

      return categories;
   }

}
