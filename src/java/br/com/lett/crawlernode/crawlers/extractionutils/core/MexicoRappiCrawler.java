package br.com.lett.crawlernode.crawlers.extractionutils.core;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.JSONUtils;
import br.com.lett.crawlernode.util.Logging;
import models.Offers;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public abstract class MexicoRappiCrawler extends RappiCrawler {

   private static final String HOME_PAGE = "https://www.rappi.com.mx/";

   @Override
   protected String getHomeDomain() {
      return "mxgrability.rappi.com";
   }

   @Override
   protected String getImagePrefix() {
      return "images.rappi.com.mx/products";
   }

   public MexicoRappiCrawler(Session session) {
      super(session);
      this.config.setFetcher(FetchMode.APACHE);
   }

   @Override
   public boolean shouldVisit() {
      String href = this.session.getOriginalURL().toLowerCase();
      return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
   }

   @Override
   public List<Product> extractInformation(JSONObject jsonSku) throws Exception {

      List<Product> products = new ArrayList<>();

      JSONObject productJson = JSONUtils.getJSONValue(jsonSku, "product");

      if (isProductPage(productJson)) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());


         String internalPid = crawlInternalPid(productJson);
         String internalId = newUnification ? internalPid : crawlInternalId(productJson);
         String description = crawlDescription(productJson);
         String primaryImage = crawlPrimaryImage(jsonSku);
         List<String> secondaryImages = crawlSecondaryImages(jsonSku, primaryImage);
         CategoryCollection categories = crawlCategories(productJson);
         String name = crawlName(productJson);
         List<String> eans = scrapEan(productJson);
         boolean available = crawlAvailability(productJson);
         Offers offers = available ? scrapOffers(productJson) : new Offers();

         // Creating the product
         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL().replace("product", "producto"))
            .setInternalId(internalId)
            .setInternalPid(internalPid)
            .setName(name)
            .setPrimaryImage(primaryImage)
            .setSecondaryImages(secondaryImages)
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
   protected String crawlDescription(JSONObject json) {
      StringBuilder description = new StringBuilder();

      if (json.has("description") && json.get("description") instanceof String) {
         String desc = json.getString("description");

         if (desc.replace(" ", "").contains("-PLU")) {
            String descFinal = desc.replace(CommonMethods.getLast(desc.split("-")), "").trim();
            description.append(descFinal.substring(0, descFinal.length() - 2).trim());
         } else {
            description.append(desc);
         }
      }

      return description.toString();
   }
}
