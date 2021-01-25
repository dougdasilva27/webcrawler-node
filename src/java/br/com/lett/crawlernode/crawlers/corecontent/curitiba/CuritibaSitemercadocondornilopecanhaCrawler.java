package br.com.lett.crawlernode.crawlers.corecontent.curitiba;

import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.BrasilSitemercadoCrawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import br.com.lett.crawlernode.util.Logging;
import models.Offers;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author gabriel date: 2020-11-04
 */
public class CuritibaSitemercadocondornilopecanhaCrawler extends BrasilSitemercadoCrawler {
   public CuritibaSitemercadocondornilopecanhaCrawler(Session session) {
      super(session);
   }

   public static final String HOME_PAGE = "https://www.sitemercado.com.br/condor/curitiba-loja-hiper-condor-nilo-pecanha-ahu-r-nilo-pecanha";

   public static final int IDLOJA = 5264;
   public static final int IDREDE = 2553;

   @Override
   protected String getHomePage() {
      return HOME_PAGE;
   }

   @Override
   protected Map<String, Integer> getLojaInfo() {
      Map<String, Integer> lojaInfo = new HashMap<>();
      lojaInfo.put("IdLoja", IDLOJA);
      lojaInfo.put("IdRede", IDREDE);
      return lojaInfo;
   }

   @Override
   public List<Product> extractInformation(JSONObject jsonSku) throws Exception {
      super.extractInformation(jsonSku);
      List<Product> products = new ArrayList<>();

      if (jsonSku.has("idLojaProduto")) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         Integer internalIdInt = JSONUtils.getIntegerValueFromJSON(jsonSku, "idLojaProduto", 0);
         String internalId = internalIdInt != null ? internalIdInt.toString() : null;
         Integer internalPidInt = JSONUtils.getIntegerValueFromJSON(jsonSku, "idProduct", 0);
         String internalPid = internalPidInt != null ? internalPidInt.toString() : null;
         CategoryCollection categories = crawlCategories(jsonSku);
         String description = JSONUtils.getStringValue(jsonSku, "description");
         Integer stock = jsonSku.has("quantityStock") && jsonSku.get("quantityStock") instanceof Integer ? jsonSku.getInt("quantityStock") : null;
         boolean available = stock != null && stock > 0;
         Offers offers = available ? scrapOffers(jsonSku) : new Offers();
         JSONArray imagensFromArray = JSONUtils.getValueRecursive(jsonSku, "images", JSONArray.class);
         List<String> images = CrawlerUtils.scrapImagesListFromJSONArray(imagensFromArray, "img", null, "https", "img.sitemercado.com.br", session);
         String primaryImage = images != null && !images.isEmpty() ? images.remove(0) : null;
         String name = JSONUtils.getStringValue(jsonSku, "excerpt");

         // Creating the product
         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setInternalPid(internalPid)
            .setName(name)
            .setCategory1(categories.getCategory(0))
            .setCategory2(categories.getCategory(1))
            .setCategory3(categories.getCategory(2))
            .setPrimaryImage(primaryImage)
            .setSecondaryImages(images)
            .setDescription(description)
            .setStock(stock)
            .setOffers(offers)
            .build();

         products.add(product);


      } else {
         Logging.printLogDebug(logger, session, "Not a product page:   " + this.session.getOriginalURL());
      }

      return products;
   }
}
