package br.com.lett.crawlernode.crawlers.corecontent.peru;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.methods.JsoupDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.Parser;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import br.com.lett.crawlernode.util.Logging;
import models.Offers;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.*;

public class PeruFazilCrawler extends Crawler {
   public PeruFazilCrawler(Session session) {
      super(session);
      super.config.setParser(Parser.JSON);
      super.config.setFetcher(FetchMode.JSOUP);
   }

   @Override
   protected Response fetchResponse() {
      String url = "https://www.tottus.com.pe/s/product-search/v2/key/" + session.getOriginalURL() + "?returnAvailabilityKeys=true";

      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .setProxyservice(Arrays.asList(
            ProxyCollection.NETNUT_RESIDENTIAL_CO_HAPROXY
         ))
         .build();

      Response response = CrawlerUtils.retryRequest(request, session, new JsoupDataFetcher());
      return response;
   }

   @Override
   public List<Product> extractInformation(JSONObject json) throws Exception {
      super.extractInformation(json);
      List<Product> products = new ArrayList<>();

      if (json.has("data")) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());
         JSONObject productSku = JSONUtils.getValueRecursive(json, "results.0", JSONObject.class);

         String internalId = productSku.optString("productId");
         String internalPid = productSku.optString("sku");
         String name = JSONUtils.getValueRecursive(productSku, "name.es-PE", String.class);

         JSONArray arrayImages = JSONUtils.getValueRecursive(productSku, "images", JSONArray.class);
         String primaryImage = arrayImages.isEmpty() ? arrayImages.remove(0) : null;
         String description = JSONUtils.getValueRecursive(productSku, "description.es-PE", String.class);

         boolean isAvailable = productSkuPrice.get(internalPid) != null;
         Offers offers = isAvailable ? scrapOffers(productSkuPrice.get(internalPid)) : new Offers();

         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setInternalPid(internalPid)
            .setName(name)
            .setOffers(offers)
            .setPrimaryImage(primaryImage)
            .setDescription(description)
            .build();

         products.add(product);
      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }

}
