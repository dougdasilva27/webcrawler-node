package br.com.lett.crawlernode.crawlers.corecontent.mexico;

import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.MexicoRappiCrawler;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import br.com.lett.crawlernode.util.Logging;
import models.Offers;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class MexicoRappichedrauiciudadmexicoCrawler extends MexicoRappiCrawler {

   public MexicoRappichedrauiciudadmexicoCrawler(Session session) {
      super(session);
   }

   public static final String STORE_ID = "990002982";

   @Override
   protected String getStoreId() {
      return STORE_ID;
   }

   @Override
   protected JSONObject fetch() {
      JSONObject productsInfo = new JSONObject();

      String storeId = getStoreId();

      String productUrl = session.getOriginalURL();
      String productId = null;

      if (productUrl.contains("_")) {
         productId = CommonMethods.getLast(productUrl.split("\\?")[0].split("_"));
      }

      if (productId != null && storeId != null) {
         String token = fetchToken();

         JSONObject data = JSONUtils.stringToJson(fetchProduct(productId, storeId, token));

         JSONArray components = JSONUtils.getValueRecursive(data, "data.components", JSONArray.class);

         if (components != null) {
            for (Object json : components) {
               if (json instanceof JSONObject) {
                  JSONObject nameComponents = ((JSONObject) json).optJSONObject("resource");
                  if (nameComponents.has("products")) {
                     productsInfo = JSONUtils.getValueRecursive(nameComponents, "products.0", JSONObject.class);
                     break;
                  }
               }
            }
         }
      }
      return productsInfo;
   }

   @Override
   public List<Product> extractInformation(JSONObject productJson) throws Exception {

      List<Product> products = new ArrayList<>();

      if (isProductPage(productJson)) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());


         String internalPid = crawlInternalPid(productJson);
         String internalId = newUnification ? internalPid : crawlInternalId(productJson);
         String description = crawlDescription(productJson);
         String primaryImage = crawlPrimaryImage(productJson);
        //hasn't secondary images
         CategoryCollection categories = crawlCategories(productJson);
         String name = crawlName(productJson);
         List<String> eans = scrapEan(productJson);
         boolean available = crawlAvailability(productJson);
         Offers offers = available ? scrapOffers(productJson) : new Offers();

         // Creating the product
         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setInternalPid(internalPid)
            .setName(name)
            .setPrimaryImage(primaryImage)
            .setCategories(categories)
            .setDescription(description)
            .setEans(eans)
            .setOffers(offers)
            .build();

         products.add(product);
      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }

   @Override
   protected String crawlPrimaryImage(JSONObject json) {
      String imagePart = json.optString("image");

      if (imagePart != null && !imagePart.isEmpty()) {
         return "https://images.rappi.com.mx/products/" + imagePart;
      } else {
         return null;
      }
   }


}
