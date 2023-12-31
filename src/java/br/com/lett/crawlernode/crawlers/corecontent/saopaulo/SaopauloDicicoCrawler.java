package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import models.Marketplace;
import models.prices.Prices;

public class SaopauloDicicoCrawler extends Crawler {

   private static final String HOME_PAGE = "http://www.dicico.com.br/";
   public static final String PRODUCT_API = "http://www.dicico.com.br/dicico-br/productDetail/ajax/switchSKU.jsp";

   public SaopauloDicicoCrawler(Session session) {
      super(session);
      config.setFetcher(FetchMode.FETCHER);
   }

   @Override
   public boolean shouldVisit() {
      String href = session.getOriginalURL().toLowerCase();
      return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
   }


   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      if (isProductPage(doc)) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String internalPid = crawlInternalPid(doc);
         String[] skuIDs = getJSONArray(doc);
         String description = crawlDescription(doc);
         CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".breadcrumb a:not(:first-child)");

         if (skuIDs != null) {
            for (String internalId : skuIDs) {
               Document fetchedData = fetchAPIProduct(internalId);
               // fetchedData = isProductPage(fetchedData) ? fetchedData : doc;

               String name = crawlName(doc);
               Float price = CrawlerUtils.scrapFloatPriceFromHtml(doc, ".t-black.bold.price, .mt2-price", null, false, ',', session);
               Prices prices = crawlPrices(price);
               boolean available = crawlAvailability(fetchedData) && price != null;
               Marketplace marketplace = crawlMarketplace();
               List<String> imagesArr = getImagesFromAPI(internalId);
               String primaryImage = crawlPrimaryImage(imagesArr);
               String secondaryImages = crawlSecondaryImages(imagesArr);


               Product product = ProductBuilder.create().setUrl(session.getOriginalURL()).setInternalId(internalId).setInternalPid(internalPid)
                     .setName(name).setPrice(price).setPrices(prices).setAvailable(available).setCategory1(categories.getCategory(0))
                     .setCategory2(categories.getCategory(1)).setCategory3(categories.getCategory(2)).setPrimaryImage(primaryImage)
                     .setSecondaryImages(secondaryImages).setDescription(description).setMarketplace(marketplace).build();

               products.add(product);
            }
         }

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;

   }

   private Document fetchAPIProduct(String id) {
      Document fetchedData = new Document("");

      if (id != null) {
         String payload = "?type=html&relatedProductcolorDimension=false&relatedProductsizeDimension=false&productId=" + id;
         Request request = RequestBuilder.create().setUrl(PRODUCT_API + payload).setCookies(cookies).build();
         fetchedData = Jsoup.parse(this.dataFetcher.get(session, request).getBody());
      }

      return fetchedData;
   }

   private Marketplace crawlMarketplace() {
      return new Marketplace();
   }

   private String crawlDescription(Document doc) {
      StringBuilder description = new StringBuilder();

      Element descriptionHeader = doc.selectFirst("section.prod-car > header > h3 > span");

      if (descriptionHeader != null) {
         description.append(descriptionHeader.outerHtml());
      }

      Element descriptionTecnic = doc.selectFirst("section.car-body > table.prod-ficha");

      if (descriptionTecnic != null) {
         description.append(descriptionTecnic.outerHtml());
      }

      return description.toString();
   }

   private String[] getJSONArray(Document doc) {

      Element infoDocs = doc.selectFirst("#JsonArray");
      String[] idArray = null;

      if (infoDocs != null) {
         JSONArray skuObjArr = new JSONArray(infoDocs.text());
         JSONObject skuObject = skuObjArr.getJSONObject(0);

         if (skuObject.has("pickupInStore")) {
            String valueId = skuObject.getString("pickupInStore");
            valueId = valueId.substring(1, valueId.length() - 1).replace("=true", "").replace("=false", "").replace(" ", "");
            idArray = valueId.split(",");

            return idArray;
         }

      } else {
         JSONObject skuObj = CrawlerUtils.selectJsonFromHtml(doc, ".pdp script[language=\"JavaScript\"]", "var digitalData =", "};", false, false);
         if (skuObj.has("ecom") && !skuObj.isNull("ecom")) {
            idArray = new String[1];
            JSONObject ecom = skuObj.getJSONObject("ecom");

            if (ecom.has("sku") && !ecom.isNull("sku")) {
               idArray[0] = ecom.get("sku").toString();

            }
         }
      }

      return idArray;

   }

   private boolean isProductPage(Document doc) {
      return doc.select("#productTitleDisplayContainer").first() != null;
   }


   private String crawlInternalPid(Document doc) {
      String internalId = null;

      Element id = doc.selectFirst("#currentSkuId");
      if (id != null) {
         internalId = id.attr("value");
      }

      return internalId;
   }


   private String crawlName(Document doc) {
      String name = null;

      Element nameElement = doc.selectFirst("#productTitleDisplayContainer .name span");
      if (nameElement != null) {
         name = nameElement.text();
      }
      return name;
   }


   private Prices crawlPrices(Float price) {
      Prices prices = new Prices();

      if (price != null) {
         Map<Integer, Float> installmentPriceMap = new TreeMap<>();
         installmentPriceMap.put(1, price);

         prices.insertCardInstallment(Card.HIPERCARD.toString(), installmentPriceMap);
         prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
         prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
         prices.insertCardInstallment(Card.AMEX.toString(), installmentPriceMap);
         prices.insertCardInstallment(Card.DINERS.toString(), installmentPriceMap);
      }

      return prices;
   }


   private boolean crawlAvailability(Document doc) {
      return doc.select(".btn-addToCart").first() != null;
   }

   private List<String> getImagesFromAPI(String id) {
      List<String> imagesArr = new ArrayList<>();
      String productAPI = "http://sodimac.scene7.com/is/image/SodimacBrasil/" + id + "?req=set,json";
      String imagesFetched = null;

      if (id != null) {
         Request request = RequestBuilder.create().setUrl(productAPI).setCookies(cookies).build();
         imagesFetched = this.dataFetcher.get(session, request).getBody();
         imagesFetched = imagesFetched.substring(31, imagesFetched.length() - 6);

         JSONObject imagesObj = new JSONObject(imagesFetched);
         String imageURL = null;
         Object obj = imagesObj.get("item");

         if (obj instanceof JSONArray) {
            JSONArray imagesObjArray = (JSONArray) obj;

            for (int i = 0; i < imagesObjArray.length(); i++) {
               imageURL = "http://sodimac.scene7.com/is/image/" + imagesObjArray.getJSONObject(i).getJSONObject("s").get("n").toString();
               imagesArr.add(imageURL);
            }

         } else if (obj instanceof JSONObject) {

            imagesObj = imagesObj.getJSONObject("item");
            imageURL = "http://sodimac.scene7.com/is/image/" + imagesObj.getJSONObject("s").get("n").toString();
            imagesArr.add(imageURL);

         }
      }

      return imagesArr;
   }

   private String crawlPrimaryImage(List<String> images) {
      String primaryImage = null;

      if (!images.isEmpty()) {
         primaryImage = images.get(0);
      }

      return primaryImage;

   }

   private String crawlSecondaryImages(List<String> images) {
      String secondaryImages = null;

      JSONArray secondaryImagesArray = new JSONArray();

      if (!images.isEmpty()) {
         for (int i = 1; i < images.size(); i++) {
            secondaryImagesArray.put(images.get(i));
         }

         secondaryImages = secondaryImagesArray.toString();
      }

      return secondaryImages;
   }

}
