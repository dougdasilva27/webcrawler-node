package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import models.Offers;
import org.eclipse.jetty.util.ajax.JSON;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.util.*;

public class BrasilShopeeHariboCrawler extends Crawler {
   public BrasilShopeeHariboCrawler(Session session) {
      super(session);
   }

   public List<Product> extractInformation(Document document) throws Exception {
      JSONObject productObj = getProduct(this.session.getOriginalURL());
      List<Product> products = new ArrayList<>();
      if (productObj != null) {
         JSONObject data = productObj.optJSONObject("data");
         String internalId = data.optString("itemid");
         String name = data.optString("name");
         String primaryImage = "https://cf.shopee.com.br/file/" + data.optString("image");
         String description = data.getString("description");
         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setName(name)
//            .setOffers(offers)
            .setPrimaryImage(primaryImage)
//            .setSecondaryImages(secondaryImages)
            .setDescription(description)
//            .setCategories(categories)
            .build();

         products.add(product);
      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return new ArrayList<>();
   }

   private JSONObject getProduct(String url) {
      JSONObject data = null;
      String[] arr = url.split("\\.");
      String itemId = arr[arr.length - 1];
      String shopId = arr[arr.length - 2];
      String newUrl = "https://shopee.com.br/api/v4/item/get?itemid=" + itemId + "&shopid=" + shopId;
      Request request = Request.RequestBuilder.create()
         .setUrl(newUrl)
         .build();

      Response response = this.dataFetcher.get(session, request);
      if(!response.getBody().isEmpty()){
         return CrawlerUtils.stringToJson(response.getBody());
      }
      return data;

   }
}
