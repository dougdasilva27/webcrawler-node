package br.com.lett.crawlernode.crawlers.corecontent.extractionutils;

import java.text.Normalizer;
import java.util.HashMap;
import java.util.Map;
import org.json.JSONObject;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.Logging;

public abstract class MexicoRappiCrawler extends RappiCrawler {

   private static final String HOME_PAGE = "https://www.rappi.com.mx/";
   private static final String IMAGES_DOMAIN = "images.rappi.com.mx/products";
   private final String storeId = setStoreId();

   protected abstract String setStoreId();

   @Override
   protected String getImagesDomain() {
      return IMAGES_DOMAIN;
   }

   public MexicoRappiCrawler(Session session) {
      super(session);
   }

   @Override
   public boolean shouldVisit() {
      String href = this.session.getOriginalURL().toLowerCase();
      return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
   }

   @Override
   protected JSONObject fetch() {
      JSONObject productsInfo = new JSONObject();

      String productId = null;
      String productUrl = session.getOriginalURL();

      if (productUrl.contains("_")) {
         String ids = productUrl.split("\\?")[0];
         productId = CommonMethods.getLast(ids.split("_")).replaceAll("[^0-9]", "");
      }

      if (productId != null && storeId != null) {
         Map<String, String> headers = new HashMap<>();

         String url = "https://services.mxgrability.rappi.com/windu/products/store/" + storeId + "/product/" + productId;
         Request request = RequestBuilder.create().setUrl(url).setCookies(cookies).setHeaders(headers).mustSendContentEncoding(false).build();

         String page = this.dataFetcher.get(session, request).getBody();

         page = Normalizer.normalize(page, Normalizer.Form.NFD);
         if (page.startsWith("{") && page.endsWith("}")) {
            try {
               JSONObject apiResponse = new JSONObject(page);

               if (apiResponse.has("product") && apiResponse.get("product") instanceof JSONObject) {
                  productsInfo = apiResponse.getJSONObject("product");
               }

            } catch (Exception e) {
               Logging.printLogWarn(logger, session, CommonMethods.getStackTrace(e));
            }
         }
      }

      return productsInfo;
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
