package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.CategoryCollection;
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
import org.jsoup.nodes.Document;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BrasilAlthoffSupermercadosCrawler extends Crawler {

   protected BrasilAlthoffSupermercadosCrawler(Session session) {
      super(session);
      super.config.setParser(Parser.JSON);
   }

   private final String storeId = getStoreId();

   protected String getStoreId() {
      return session.getOptions().optString("storeId");
   }


   @Override
   protected Response fetchResponse() {
      String url = "https://api.emcasa.althoff.com.br/graphql";

      Map<String, String> headers = new HashMap<>();
      headers.put("origin", "https://emcasa.althoff.com.br");
      headers.put("content-type", "application/json");

      String internalId = getUrlInternalId();

      String payload = "{\"operationName\":\"ProductDetailQuery\",\"variables\":{\"productId\":\"" + internalId + "\",\"storeId\":\"" + storeId + "\"},\"query\":\"fragment SimilarDetails on PublicViewerProduct {\\n  id\\n  name\\n  description\\n  content\\n  saleUnit\\n  contentUnit\\n  type\\n  slug\\n  tags\\n  brand {\\n    id\\n    name\\n    __typename\\n  }\\n  image {\\n    url\\n    thumborized\\n    __typename\\n  }\\n  imagesGallery {\\n    name\\n    url\\n    __typename\\n  }\\n  productPromotion(storeId: $storeId) {\\n    promotionPrice\\n    discountType\\n    gift\\n    buy\\n    isGift\\n    promotion {\\n      id\\n      name\\n      endDate\\n      startDate\\n      displayExclusivePriceOnline\\n      progressiveDiscount {\\n        asFrom\\n        gift\\n        __typename\\n      }\\n      qtyToGift\\n      updatedAt\\n      type\\n      benefitType\\n      __typename\\n    }\\n    giftOfThisProduct {\\n      productId\\n      name\\n      image {\\n        url\\n        thumborized\\n        __typename\\n      }\\n      normalPrice\\n      promotionPrice\\n      gift\\n      __typename\\n    }\\n    __typename\\n  }\\n  quantity(storeId: $storeId) {\\n    min\\n    max\\n    maxPromotion\\n    fraction\\n    inStock\\n    sellByWeightAndUnit\\n    __typename\\n  }\\n  pricing(storeId: $storeId) {\\n    id\\n    promotion\\n    price\\n    promotionalPrice\\n    __typename\\n  }\\n  personas(storeId: $storeId) {\\n    personaPrice\\n    personaId\\n    __typename\\n  }\\n  level1Category(storeId: $storeId) {\\n    id\\n    name\\n    slug\\n    __typename\\n  }\\n  level2Category(storeId: $storeId) {\\n    id\\n    name\\n    slug\\n    parent {\\n      id\\n      name\\n      slug\\n      __typename\\n    }\\n    __typename\\n  }\\n  level3Category(storeId: $storeId) {\\n    id\\n    name\\n    slug\\n    level2Category: parent {\\n      level1Category: parent {\\n        id\\n        name\\n        slug\\n        __typename\\n      }\\n      id\\n      name\\n      slug\\n      __typename\\n    }\\n    __typename\\n  }\\n  __typename\\n}\\n\\nfragment ProductDetails on PublicViewerProduct {\\n  id\\n  name\\n  description\\n  content\\n  saleUnit\\n  contentUnit\\n  type\\n  slug\\n  tags\\n  brand {\\n    id\\n    name\\n    __typename\\n  }\\n  image {\\n    url\\n    thumborized(width: 321, height: 321, fitIn: true)\\n    thumbLarge: thumborized(width: 321, height: 321, fitIn: true)\\n    __typename\\n  }\\n  imagesGallery {\\n    name\\n    url\\n    thumborized(width: 321, height: 321, fitIn: true)\\n    thumbLarge: thumborized(width: 321, height: 321)\\n    __typename\\n  }\\n  productPromotion(storeId: $storeId) {\\n    promotionPrice\\n    discountType\\n    gift\\n    buy\\n    isGift\\n    promotion {\\n      id\\n      name\\n      endDate\\n      startDate\\n      displayExclusivePriceOnline\\n      progressiveDiscount {\\n        asFrom\\n        gift\\n        __typename\\n      }\\n      qtyToGift\\n      updatedAt\\n      type\\n      benefitType\\n      __typename\\n    }\\n    giftOfThisProduct {\\n      productId\\n      name\\n      image {\\n        url\\n        thumborized\\n        __typename\\n      }\\n      normalPrice\\n      promotionPrice\\n      gift\\n      __typename\\n    }\\n    __typename\\n  }\\n  quantity(storeId: $storeId) {\\n    min\\n    max\\n    maxPromotion\\n    fraction\\n    inStock\\n    sellByWeightAndUnit\\n    __typename\\n  }\\n  pricing(storeId: $storeId) {\\n    id\\n    promotion\\n    price\\n    promotionalPrice\\n    __typename\\n  }\\n  personas(storeId: $storeId) {\\n    personaPrice\\n    personaId\\n    __typename\\n  }\\n  level1Category(storeId: $storeId) {\\n    id\\n    name\\n    slug\\n    __typename\\n  }\\n  level2Category(storeId: $storeId) {\\n    id\\n    name\\n    slug\\n    description\\n    slug\\n    image {\\n      url\\n      name\\n      thumborized\\n      __typename\\n    }\\n    parent {\\n      id\\n      name\\n      slug\\n      __typename\\n    }\\n    __typename\\n  }\\n  level3Category(storeId: $storeId) {\\n    id\\n    name\\n    slug\\n    level2Category: parent {\\n      level1Category: parent {\\n        id\\n        name\\n        slug\\n        __typename\\n      }\\n      id\\n      name\\n      slug\\n      __typename\\n    }\\n    __typename\\n  }\\n  __typename\\n}\\n\\nfragment BoughtTogether on PublicViewerProduct {\\n  id\\n  name\\n  description\\n  content\\n  saleUnit\\n  contentUnit\\n  type\\n  slug\\n  tags\\n  brand {\\n    id\\n    name\\n    __typename\\n  }\\n  image {\\n    url\\n    thumborized(width: 321, height: 321, fitIn: true)\\n    __typename\\n  }\\n  imagesGallery {\\n    name\\n    url\\n    thumborized(width: 321, height: 321, fitIn: true)\\n    __typename\\n  }\\n  productPromotion(storeId: $storeId) {\\n    promotionPrice\\n    discountType\\n    gift\\n    buy\\n    isGift\\n    promotion {\\n      id\\n      name\\n      endDate\\n      startDate\\n      displayExclusivePriceOnline\\n      progressiveDiscount {\\n        asFrom\\n        gift\\n        __typename\\n      }\\n      qtyToGift\\n      updatedAt\\n      type\\n      benefitType\\n      __typename\\n    }\\n    giftOfThisProduct {\\n      productId\\n      name\\n      image {\\n        url\\n        thumborized\\n        __typename\\n      }\\n      normalPrice\\n      promotionPrice\\n      gift\\n      __typename\\n    }\\n    __typename\\n  }\\n  quantity(storeId: $storeId) {\\n    min\\n    max\\n    maxPromotion\\n    fraction\\n    inStock\\n    sellByWeightAndUnit\\n    __typename\\n  }\\n  pricing(storeId: $storeId) {\\n    id\\n    promotion\\n    price\\n    promotionalPrice\\n    __typename\\n  }\\n  personas(storeId: $storeId) {\\n    personaPrice\\n    personaId\\n    __typename\\n  }\\n  level1Category(storeId: $storeId) {\\n    id\\n    name\\n    slug\\n    __typename\\n  }\\n  level2Category(storeId: $storeId) {\\n    id\\n    name\\n    slug\\n    parent {\\n      id\\n      name\\n      slug\\n      __typename\\n    }\\n    __typename\\n  }\\n  level3Category(storeId: $storeId) {\\n    id\\n    name\\n    slug\\n    level2Category: parent {\\n      level1Category: parent {\\n        id\\n        name\\n        slug\\n        __typename\\n      }\\n      id\\n      name\\n      slug\\n      __typename\\n    }\\n    __typename\\n  }\\n  __typename\\n}\\n\\nquery ProductDetailQuery($storeId: ID!, $productId: ID!) {\\n  publicViewer(storeId: $storeId) {\\n    id\\n    product(id: $productId, storeId: $storeId) {\\n      ...ProductDetails\\n      similar(storeId: $storeId) {\\n        ...SimilarDetails\\n        __typename\\n      }\\n      boughtTogether(storeId: $storeId) {\\n        ...BoughtTogether\\n        __typename\\n      }\\n      __typename\\n    }\\n    __typename\\n  }\\n}\"}";

      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .setHeaders(headers)
         .setPayload(payload)
         .setProxyservice(Arrays.asList(
            ProxyCollection.BUY,
            ProxyCollection.NETNUT_RESIDENTIAL_BR))
         .build();

      return this.dataFetcher.get(session, request);
   }

   private String getUrlInternalId() {
      String regex = "produtos\\/([0-9]+)\\/";

      Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
      Matcher matcher = pattern.matcher(session.getOriginalURL());

      if (matcher.find()) {
         return matcher.group(1);
      }

      return null;
   }

   @Override
   public List<Product> extractInformation(JSONObject jsonSku) throws Exception {
      super.extractInformation(jsonSku);

      List<Product> products = new ArrayList<>();

      JSONObject jsonProduct = JSONUtils.getValueRecursive(jsonSku, "data.publicViewer.product", JSONObject.class);

      if (jsonProduct!= null && !jsonProduct.isEmpty()) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String internalId = getUrlInternalId();
         String name = JSONUtils.getValueRecursive(jsonProduct, "name", String.class);
//         String internalPid = crawlInternalPid(jsonSku);
         CategoryCollection categories = crawlCategories(internalId);
         String description = JSONUtils.getValueRecursive(jsonProduct,"description", String.class);

         String primaryImage = crawlPrimaryImage(jsonSku);
         String secondaryImages = crawlSecondaryImages(jsonSku, primaryImage);

         boolean available = crawlAvailability(jsonSku);
         Offers offers = available ? scrapOffers(jsonSku) : new Offers();

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
            .setSecondaryImages(secondaryImages)
            .setDescription(description)
            .setOffers(offers)
            .build();

         products.add(product);


      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }


}
