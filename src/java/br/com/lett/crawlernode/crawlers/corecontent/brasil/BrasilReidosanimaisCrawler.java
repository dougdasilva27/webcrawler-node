package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import br.com.lett.crawlernode.util.Logging;
import models.prices.Prices;

public class BrasilReidosanimaisCrawler extends Crawler {

   private static final String HOST = "petlazer.com.br";

   public BrasilReidosanimaisCrawler(Session session) {
      super(session);
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      if (isProductPage(doc)) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         JSONObject json = CrawlerUtils.selectJsonFromHtml(doc, ".product-options script", "Product.Config(", ");", false, true);

         String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, ".product-view [name=product]", "value");
         String internalPid = internalId;
         String name = CrawlerUtils.scrapStringSimpleInfo(doc, ".prod__name h1, .prod__name h2", true);
         Float price = CrawlerUtils.scrapFloatPriceFromHtml(doc, "[id*=product-price] .price", null, false, ',', session);
         Prices prices = scrapPrices(doc, price);
         CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".breadcrumbs > ul > li", true);
         String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, ".product-image-gallery img", Arrays.asList("data-zoom-image", "data-src"), "https", HOST);
         String secondaryImages = CrawlerUtils.scrapSimpleSecondaryImages(doc, ".product-image-gallery img", Arrays.asList("data-zoom-image", "data-src"), "https", HOST, primaryImage);
         String description = CrawlerUtils.scrapElementsDescription(doc, Arrays.asList(".product-view .tabs"));
         boolean available = doc.selectFirst(".availability.in-stock") != null;
         List<String> eans = scrapEans(doc);

         // Creating the product
         Product product = ProductBuilder.create()
               .setUrl(session.getOriginalURL())
               .setInternalId(internalId)
               .setInternalPid(internalPid)
               .setName(name)
               .setPrice(price)
               .setPrices(prices)
               .setAvailable(available)
               .setCategory1(categories.getCategory(0))
               .setCategory2(categories.getCategory(1))
               .setCategory3(categories.getCategory(2))
               .setPrimaryImage(primaryImage)
               .setSecondaryImages(secondaryImages)
               .setDescription(description)
               .setEans(eans)
               .build();

         if (json != null && !json.keySet().isEmpty()) {
            String idAttr = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, ".product-options li[attr-id]", "attr-id");
            if (idAttr != null) {
               Float basePrice = JSONUtils.getFloatValueFromJSON(json, "basePrice", true);

               json = json.has("attributes") && json.get("attributes") instanceof JSONObject ? json.getJSONObject("attributes") : new JSONObject();
               json = json.has(idAttr) && json.get(idAttr) instanceof JSONObject ? json.getJSONObject(idAttr) : new JSONObject();

               String code = "";
               if (json.has("code") && json.get("code") instanceof String) {
                  code = json.getString("code");
               }

               JSONArray variations = json.has("options") && json.get("options") instanceof JSONArray ? json.getJSONArray("options") : new JSONArray();
               for (Object o : variations) {
                  if (o instanceof JSONObject) {
                     JSONObject variationJson = (JSONObject) o;
                     Product clone = product.clone();

                     if (variationJson.has("label") && variationJson.get("label") instanceof String) {
                        clone.setName(product.getName() + " - " + variationJson.getString("label") + " " + code);
                     }

                     if (basePrice != null && variationJson.has("price")) {
                        Float priceSum = JSONUtils.getFloatValueFromJSON(variationJson, "price", true);
                        priceSum = priceSum != null ? priceSum : 0.0f;

                        clone.setPrice(basePrice + priceSum);
                        clone.setPrices(scrapPrices(doc, clone.getPrice()));
                     }

                     if (variationJson.has("products") && variationJson.get("products") instanceof JSONArray) {
                        for (Object obj : variationJson.getJSONArray("products")) {
                           if (obj instanceof String) {
                              Product cloneClone = clone.clone();

                              Document docImages = crawlApiInfo(obj.toString(), internalPid);
                              String primaryImageVariation = CrawlerUtils.scrapSimplePrimaryImage(docImages, ".product-image-gallery img",
                                    Arrays.asList("data-zoom-image", "data-src"), "https", HOST);
                              String secondaryImagesVariation = CrawlerUtils.scrapSimpleSecondaryImages(docImages, ".product-image-gallery img",
                                    Arrays.asList("data-zoom-image", "data-src"), "https", HOST, primaryImageVariation);

                              cloneClone.setInternalId((String) obj);
                              cloneClone.setPrimaryImage(primaryImageVariation);
                              cloneClone.setSecondaryImages(secondaryImagesVariation);

                              products.add(cloneClone);
                           }
                        }
                     }
                  }
               }
            }
         } else {
            products.add(product);
         }
      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }

   private boolean isProductPage(Document doc) {
      return doc.selectFirst(".catalog-product-view") != null;
   }

   private List<String> scrapEans(Document doc) {
      List<String> eans = null;

      String ean = CrawlerUtils.scrapStringSimpleInfo(doc, ".prod__name .sku", true);
      if (ean != null) {
         ean = CommonMethods.getLast(ean.split(":")).trim().split("/")[0];

         if (!ean.isEmpty()) {
            eans = Arrays.asList(ean);
         }
      }

      return eans;
   }

   private Document crawlApiInfo(String internalId, String internalPid) {
      Document doc = new Document("");

      String url = "https://www.reidosanimais.com.br//extendedconfigurableswatches/media/loadMedia?cId="
            + internalId + "&pId=" + internalPid;
      Request request = RequestBuilder.create()
            .setUrl(url)
            .setCookies(cookies)
            .build();

      JSONObject apiInfo = JSONUtils.stringToJson(this.dataFetcher.get(session, request).getBody());
      if (apiInfo.has("data") && !apiInfo.isNull("data")) {
         doc = Jsoup.parse(apiInfo.get("data").toString());
      }

      return doc;
   }

   private Prices scrapPrices(Document doc, Float price) {
      Prices prices = new Prices();

      if (price != null) {
         Map<Integer, Float> installmentPriceMap = new TreeMap<>();
         installmentPriceMap.put(1, price);

         prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
         prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
         prices.insertCardInstallment(Card.ELO.toString(), installmentPriceMap);
         prices.insertCardInstallment(Card.HIPERCARD.toString(), installmentPriceMap);
         prices.insertCardInstallment(Card.AMEX.toString(), installmentPriceMap);
         prices.insertCardInstallment(Card.HIPER.toString(), installmentPriceMap);
         prices.insertCardInstallment(Card.DINERS.toString(), installmentPriceMap);
      }

      return prices;
   }
}
