package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
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
import br.com.lett.crawlernode.util.MathUtils;
import br.com.lett.crawlernode.util.Pair;
import exceptions.IllegalSellerValueException;
import models.Marketplace;
import models.Seller;
import models.Seller.SellerBuilder;
import models.prices.Prices;

/**
 * Date: 26/03/2019
 * 
 * @author Gabriel Dornelas
 *
 */
public class BrasilBuscapeCrawler extends Crawler {

   private static final String HOST = "www.buscape.com.br";
   private static final String PROTOCOL = "https";
   private static final String HOME_PAGE = "https://www.buscape.com.br/";

   public BrasilBuscapeCrawler(Session session) {
      super(session);
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

         JSONObject pageInfo = CrawlerUtils.selectJsonFromHtml(doc, "script[type=\"text/javascript\"]", "window.__INITIAL_STATE__ =", ";", false, true);
         JSONObject productInfo = pageInfo.has("product") ? pageInfo.getJSONObject("product") : new JSONObject();

         String internalId = productInfo.has("id") ? productInfo.get("id").toString() : null;
         String name = productInfo.has("name") ? productInfo.get("name").toString() : null;
         CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".bui-breadcrumb__list .bui-breadcrumb__item:not(:first-child) a");
         String primaryImage = crawlPrimaryImage(productInfo);
         String secondaryImages = crawlSecondaryImages(productInfo);
         String description = CrawlerUtils.scrapSimpleDescription(doc, Arrays.asList("#product_details"));
         Marketplace marketplace = scrapMarketplaces(pageInfo);

         // Creating the product
         Product product = ProductBuilder.create()
               .setUrl(session.getOriginalURL())
               .setInternalId(internalId)
               .setName(name)
               .setPrices(new Prices())
               .setAvailable(false)
               .setCategory1(categories.getCategory(0))
               .setCategory2(categories.getCategory(1))
               .setCategory3(categories.getCategory(2))
               .setPrimaryImage(primaryImage)
               .setSecondaryImages(secondaryImages)
               .setDescription(description)
               .setMarketplace(marketplace)
               .build();

         products.add(product);

      } else {
         products = scrapProductsOnNewSite(doc);
      }

      return products;

   }

   private boolean isProductPage(Document doc) {
      return !doc.select(".product #tipo_pagina").isEmpty();
   }


   private String crawlPrimaryImage(JSONObject product) {
      String primaryImage = null;

      if (product.has("images")) {
         JSONArray images = product.getJSONArray("images");

         if (images.length() > 0) {
            JSONObject image = images.getJSONObject(0);

            if (image.has("large") && image.get("large") instanceof String) {
               primaryImage = CrawlerUtils.completeUrl(image.get("large").toString().trim(), PROTOCOL, HOST);
            } else if (image.has("small") && image.get("small") instanceof String) {
               primaryImage = CrawlerUtils.completeUrl(image.get("small").toString().trim(), PROTOCOL, HOST);
            } else if (image.has("T100x100") && image.get("T100x100") instanceof String) {
               primaryImage = CrawlerUtils.completeUrl(image.get("T100x100").toString().trim(), PROTOCOL, HOST);
            }

         }
      }

      return primaryImage;
   }

   private String crawlSecondaryImages(JSONObject product) {
      String secondaryImages = null;
      JSONArray secondaryImagesArray = new JSONArray();

      if (product.has("images")) {
         JSONArray images = product.getJSONArray("images");

         for (int i = 1; i < images.length(); i++) { // first index is the primary Image
            JSONObject image = images.getJSONObject(i);

            String imageStr = null;

            if (image.has("large") && image.get("large") instanceof String) {
               imageStr = CrawlerUtils.completeUrl(image.get("large").toString().trim(), PROTOCOL, HOST);
            } else if (image.has("small") && image.get("small") instanceof String) {
               imageStr = CrawlerUtils.completeUrl(image.get("small").toString().trim(), PROTOCOL, HOST);
            } else if (image.has("T100x100") && image.get("T100x100") instanceof String) {
               imageStr = CrawlerUtils.completeUrl(image.get("T100x100").toString().trim(), PROTOCOL, HOST);
            }

            if (imageStr != null) {
               secondaryImagesArray.put(imageStr);
            }
         }
      }

      if (secondaryImagesArray.length() > 0) {
         secondaryImages = secondaryImagesArray.toString();
      }

      return secondaryImages;
   }

   private Marketplace scrapMarketplaces(JSONObject pageInfo) {
      Marketplace mkt = new Marketplace();
      List<String> sellers = new ArrayList<>();

      if (pageInfo.has("offers")) {
         JSONArray offers = pageInfo.getJSONArray("offers");

         for (Object object : offers) {
            JSONObject offer = (JSONObject) object;

            if (offer.has("seller") && offer.has("prices")) {
               Prices prices = crawlPrices(offer.getJSONObject("prices"));
               Double price = MathUtils.normalizeTwoDecimalPlaces(CrawlerUtils.extractPriceFromPrices(prices, Card.VISA).doubleValue());

               JSONObject sellerJson = offer.getJSONObject("seller");
               String sellerName = sellerJson.has("name") ? sellerJson.get("name").toString().toLowerCase().trim() : null;

               if (!prices.isEmpty() && sellerName != null && price != null && !sellers.contains(sellerName)) {
                  try {
                     sellers.add(sellerName);
                     Seller seller = new SellerBuilder().setName(sellerName).setPrice(price).setPrices(prices).build();
                     mkt.add(seller);
                  } catch (IllegalSellerValueException e) {
                     Logging.printLogError(logger, session, CommonMethods.getStackTrace(e));
                  }
               }
            }
         }
      }

      return mkt;
   }


   /**
    * @param doc
    * @param price
    * @return
    */
   private Prices crawlPrices(JSONObject pricesJson) {
      Prices prices = new Prices();

      if (pricesJson.has("CPC") && !pricesJson.isNull("CPC")) {
         JSONObject cpc = pricesJson.getJSONObject("CPC");
         Float price = CrawlerUtils.getFloatValueFromJSON(cpc, "value", true, false);

         if (price != null) {
            Map<Integer, Float> installmentPriceMap = new TreeMap<>();
            installmentPriceMap.put(1, price);
            prices.setBankTicketPrice(price);

            if (cpc.has("installment") && !cpc.isNull("installment")) {
               JSONObject installment = cpc.getJSONObject("installment");

               Integer installmentNumber = CrawlerUtils.getIntegerValueFromJSON(installment, "total", null);
               Float value = CrawlerUtils.getFloatValueFromJSON(installment, "value", true, false);

               if (installmentNumber != null && value != null) {
                  installmentPriceMap.put(installmentNumber, value);
               }
            }

            prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
            prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
         }
      }

      return prices;
   }

   /**
    *
    * New Site
    * 
    */

   private List<Product> scrapProductsOnNewSite(Document doc) {
      List<Product> products = new ArrayList<>();

      if (!doc.select(".prod-page").isEmpty()) {
         JSONObject productJson = CrawlerUtils.selectJsonFromHtml(doc, "script", ";Zoom.Context.load(", ");", true, false);

         String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "input[name=prodid]", "value");
         String name = CrawlerUtils.scrapStringSimpleInfo(doc, ".product-name", false);
         CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".zbreadcrumb li:not(:first-child) a");
         String description = CrawlerUtils.scrapSimpleDescription(doc, Arrays.asList(".section.details"));
         String primaryImage = scrapPrimaryImageNewSite(productJson);
         String secondaryImages = scrapSecondaryImagesNewSite(productJson, primaryImage);
         List<Document> offersHtmls = getSellersHtmls(doc, internalId);
         Marketplace marketplace = scrapMarketplacesOnNewSite(offersHtmls);

         // Creating the product
         Product product = ProductBuilder.create()
               .setUrl(session.getOriginalURL())
               .setInternalId(internalId)
               .setName(name)
               .setPrices(new Prices())
               .setAvailable(false)
               .setCategory1(categories.getCategory(0))
               .setCategory2(categories.getCategory(1))
               .setCategory3(categories.getCategory(2))
               .setPrimaryImage(primaryImage)
               .setSecondaryImages(secondaryImages)
               .setDescription(description)
               .setMarketplace(marketplace)
               .build();

         products.add(product);

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }

   private List<Document> getSellersHtmls(Document doc, String internalId) {
      List<Document> htmls = new ArrayList<>();
      htmls.add(doc);

      Integer offersNumber = CrawlerUtils.scrapIntegerFromHtmlAttr(doc, ".offers-list[data-showing-total]", "data-showing-total", 0);
      int offersNumberCount = doc.select(".offers-list__offer").size();

      if (offersNumber > offersNumberCount) {
         int currentPage = 1;

         while (offersNumber > offersNumberCount) {
            String offerUrl = "https://www.buscape.com.br/ajax/product_desk?__pAct_=getmoreoffers"
                  + "&prodid=" + internalId
                  + "&pagenumber=" + currentPage
                  + "&highlightedItemID=0"
                  + "&resultorder=7";

            Request request = RequestBuilder.create()
                  .setUrl(offerUrl)
                  .setCookies(cookies)
                  .build();

            Document offersDoc = Jsoup.parse(this.dataFetcher.get(session, request).getBody());
            Elements offersElements = offersDoc.select(".offers-list__offer");
            if (!offersElements.isEmpty()) {
               offersNumberCount += offersElements.size();
               htmls.add(offersDoc);
            } else {
               break;
            }

            currentPage++;
         }
      }

      return htmls;
   }

   private Marketplace scrapMarketplacesOnNewSite(List<Document> docuements) {
      Map<String, Prices> offersMap = new HashMap<>();

      for (Document doc : docuements) {
         Elements offers = doc.select(".offers-list__offer");
         for (Element e : offers) {
            String sellerName = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".col-store a", "title");

            if (sellerName != null) {
               String sellerNameLower = sellerName.replace("na", "").trim().toLowerCase();
               Prices prices = scrapPricesOnNewSite(doc);

               offersMap.put(sellerNameLower, prices);
            }
         }
      }

      return CrawlerUtils.assembleMarketplaceFromMap(offersMap, Arrays.asList("buscapé"), Card.VISA, session);
   }

   private Prices scrapPricesOnNewSite(Element doc) {
      Prices prices = new Prices();

      Float price = CrawlerUtils.scrapFloatPriceFromHtml(doc, ".price__total", null, true, ',', session);
      if (price != null) {
         Map<Integer, Float> installmentPriceMap = new TreeMap<>();
         installmentPriceMap.put(1, price);
         prices.setBankTicketPrice(price);

         Pair<Integer, Float> installments = CrawlerUtils.crawlSimpleInstallment(".price__parceled__parcels", doc, false);
         if (!installments.isAnyValueNull()) {
            installmentPriceMap.put(installments.getFirst(), installments.getSecond());
         }

         prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
         prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
      }

      return prices;
   }

   private String scrapPrimaryImageNewSite(JSONObject product) {
      String primaryImage = null;

      JSONObject superZoom = JSONUtils.getJSONValue(product, "SuperZoom");
      if (superZoom.has("img") && superZoom.get("img") instanceof JSONArray) {
         JSONArray images = superZoom.getJSONArray("img");

         if (images.length() > 0) {
            primaryImage = getImageFromJSONOfNewSite(images.getJSONObject(0));
         }
      }

      return primaryImage;
   }

   private String scrapSecondaryImagesNewSite(JSONObject product, String primaryImage) {
      String secondaryImages = null;
      JSONArray secondaryImagesArray = new JSONArray();

      JSONObject superZoom = JSONUtils.getJSONValue(product, "SuperZoom");
      if (superZoom.has("img") && superZoom.get("img") instanceof JSONArray) {
         JSONArray images = superZoom.getJSONArray("img");

         for (Object o : images) {
            if (o instanceof JSONObject) {
               String image = getImageFromJSONOfNewSite((JSONObject) o);

               if (!image.equalsIgnoreCase(primaryImage)) {
                  secondaryImagesArray.put(image);
               }
            }
         }
      }

      if (secondaryImagesArray.length() > 0) {
         secondaryImages = secondaryImagesArray.toString();
      }

      return secondaryImages;
   }

   private String getImageFromJSONOfNewSite(JSONObject imageJson) {
      String image = null;

      if (imageJson.has("l") && imageJson.get("l") instanceof String) {
         image = CrawlerUtils.completeUrl(imageJson.get("l").toString().trim(), PROTOCOL, HOST);
      } else if (imageJson.has("m") && imageJson.get("m") instanceof String) {
         image = CrawlerUtils.completeUrl(imageJson.get("m").toString().trim(), PROTOCOL, HOST);
      } else if (imageJson.has("t") && imageJson.get("t") instanceof String) {
         image = CrawlerUtils.completeUrl(imageJson.get("t").toString().trim(), PROTOCOL, HOST);
      }

      return image;
   }
}
