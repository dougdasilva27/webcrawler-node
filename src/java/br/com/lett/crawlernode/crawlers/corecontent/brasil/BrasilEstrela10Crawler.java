package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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
import br.com.lett.crawlernode.crawlers.ratingandreviews.extractionutils.TrustvoxRatingCrawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
import br.com.lett.crawlernode.util.Pair;
import models.Marketplace;
import models.RatingsReviews;
import models.prices.Prices;


public class BrasilEstrela10Crawler extends Crawler {

   private static final String HOME_PAGE = "https://www.estrela10.com.br/";
   private static final String IMAGES_HOST = "d2figssdufzycg.cloudfront.net";
   private static final String IMAGES_PROTOCOL = "https";

   public BrasilEstrela10Crawler(Session session) {
      super(session);
      super.config.setMustSendRatingToKinesis(true);
   }

   @Override
   public boolean shouldVisit() {
      String href = this.session.getOriginalURL().toLowerCase();
      return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      if (isProductPage(doc)) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String mainPageName = CrawlerUtils.scrapStringSimpleInfo(doc, "h1[itemprop=\"name\"]", true);
         CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".wd-browsing-breadcrumbs li:not(.last) a:not([href=\"/\"]) span");
         String description = CrawlerUtils.scrapElementsDescription(doc, Arrays.asList("#informations > .descriptions"));
         Map<String, String> colorsMap = scrapColorsIDs(doc);
         Map<String, Map<String, String>> imagesMap = fetchImageColors(colorsMap, session.getOriginalURL());
         JSONArray productsArray = CrawlerUtils.selectJsonArrayFromHtml(doc, "script", "var variants = ", ";", false, true);
         boolean hasVariations = doc.select(".sku-option").size() > 2;
         String internalPid = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "input[name=ProductID]", "value");

         // if product has variations, the first product is a product default, so is not crawled
         // then if is not, the last product is not crawled, because is a invalid product
         int indexStart = 0;
         int indexFinished = productsArray.length();

         if (hasVariations) {
            indexStart++;
         } else {
            indexFinished--;
         }

         for (int i = indexStart; i < indexFinished; i++) {
            JSONObject jsonSku = productsArray.getJSONObject(i);

            String name = scrapName(jsonSku, mainPageName);
            String internalId = JSONUtils.getStringValue(jsonSku, "productID");
            Integer stock = crawlStock(jsonSku);
            boolean available = stock != null && stock > 0;
            Float price = JSONUtils.getFloatValueFromJSON(jsonSku, "price", false);
            String primaryImage = crawlPrimaryImage(doc, jsonSku, imagesMap);
            String secondaryImages = crawlSecondaryImages(doc, jsonSku, primaryImage, imagesMap);
            Prices prices = crawlPrices(internalPid, internalId, price, jsonSku);
            RatingsReviews ratingReviews = scrapRating(internalId, doc);
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
                  .setStock(stock)
                  .setMarketplace(new Marketplace())
                  .setRatingReviews(ratingReviews)
                  .build();

            products.add(product);
         }

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }

   private boolean isProductPage(Document doc) {
      return !doc.select("h1[itemprop=\"name\"]").isEmpty();
   }

   private String scrapName(JSONObject jsonSku, String mainPageName) {
      StringBuilder name = new StringBuilder();
      name.append(mainPageName);

      if (jsonSku.has("options") && jsonSku.get("options") instanceof JSONArray) {
         JSONArray jsonOptions = jsonSku.getJSONArray("options");

         for (int i = 0; i < jsonOptions.length(); i++) {
            JSONObject option = jsonOptions.getJSONObject(i);

            if (option.has("title") && !option.isNull("title")) {
               String nameVariation = option.get("title").toString().trim();

               if (!nameVariation.isEmpty() && !name.toString().toLowerCase().contains(nameVariation.toLowerCase())) {
                  name.append(" ").append(nameVariation);
               }
            }
         }
      }


      return name.toString();
   }

   /**
    * 
    * @param doc
    * @return Map of <ColorId, ColorName>
    */
   private Map<String, String> scrapColorsIDs(Document doc) {
      Map<String, String> colorsMap = new HashMap<>();
      Elements variationTypes = doc.select(".variation-group");

      Element colorElement = null;

      for (Element e : variationTypes) {
         String variationName = CrawlerUtils.scrapStringSimpleInfo(e, ".title", true);
         if (variationName.equalsIgnoreCase("cor")) {
            colorElement = e;
            break;
         }
      }

      if (colorElement != null) {
         Elements colors = colorElement.select("option[id]");

         for (Element e : colors) {
            colorsMap.put(e.val(), e.text().trim());
         }
      }

      return colorsMap;
   }


   private Map<String, Map<String, String>> fetchImageColors(Map<String, String> colors, String url) {
      Map<String, Map<String, String>> colorsMap = new HashMap<>();

      for (Entry<String, String> entry : colors.entrySet()) {
         Map<String, String> imageMap = new HashMap<>();
         String urlColor = url + "?pp=/" + entry.getKey() + "/";

         Request request = RequestBuilder.create().setUrl(urlColor).setCookies(cookies).build();
         Document doc = Jsoup.parse(this.dataFetcher.get(session, request).getBody());

         String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, "li.image.selected:not([style]) img", Arrays.asList("data-image-large",
               "data-image-big", "src"), IMAGES_PROTOCOL, IMAGES_HOST);

         imageMap.put("primary", primaryImage);
         imageMap.put("secondary", CrawlerUtils.scrapSimpleSecondaryImages(doc, "li.image:not([style]) img",
               Arrays.asList("data-image-large", "data-image-big", "src"), IMAGES_PROTOCOL, IMAGES_HOST, primaryImage));

         colorsMap.put(entry.getKey(), imageMap);
      }

      return colorsMap;
   }

   private String crawlPrimaryImage(Document doc, JSONObject jsonSku, Map<String, Map<String, String>> colorsMap) {
      String primaryImage = null;

      if (colorsMap.isEmpty()) {
         primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, "li.image.selected:not([style]) img", Arrays.asList("data-image-large",
               "data-image-big", "src"), IMAGES_PROTOCOL, IMAGES_HOST);
      } else if (jsonSku.has("options") && jsonSku.get("options") instanceof JSONArray) {
         JSONArray jsonOptions = jsonSku.getJSONArray("options");

         for (int i = 0; i < jsonOptions.length(); i++) {
            JSONObject option = jsonOptions.getJSONObject(i);

            String variationType = JSONUtils.getStringValue(option, "name");
            if (variationType != null && variationType.equalsIgnoreCase("cor")) {
               String id = JSONUtils.getValue(option, "id").toString();
               String value = JSONUtils.getValue(option, "value").toString();

               if (id != null && value != null) {
                  String colorId = id + "." + value;

                  if (colorsMap.containsKey(colorId)) {
                     Map<String, String> imagesMap = colorsMap.get(colorId);

                     if (imagesMap.containsKey("primary")) {
                        primaryImage = imagesMap.get("primary");
                     }
                  }
               }
            }
         }
      }

      return primaryImage;

   }

   private String crawlSecondaryImages(Document doc, JSONObject jsonSku, String primaryImage, Map<String, Map<String, String>> colorsMap) {
      String secondaryImages = null;

      if (colorsMap.isEmpty()) {
         secondaryImages = CrawlerUtils.scrapSimpleSecondaryImages(doc, "li.image:not([style]) img", Arrays.asList("data-image-large",
               "data-image-big", "src"), IMAGES_PROTOCOL, IMAGES_HOST, primaryImage);
      } else if (jsonSku.has("options") && jsonSku.get("options") instanceof JSONArray) {
         JSONArray jsonOptions = jsonSku.getJSONArray("options");

         for (int i = 0; i < jsonOptions.length(); i++) {
            JSONObject option = jsonOptions.getJSONObject(i);

            String variationType = JSONUtils.getStringValue(option, "name");
            if (variationType != null && variationType.equalsIgnoreCase("cor")) {
               String id = JSONUtils.getValue(option, "id").toString();
               String value = JSONUtils.getValue(option, "value").toString();

               if (id != null && value != null) {
                  String colorId = id + "." + value;

                  if (colorsMap.containsKey(colorId)) {
                     Map<String, String> imagesMap = colorsMap.get(colorId);

                     if (imagesMap.containsKey("secondary")) {
                        secondaryImages = imagesMap.get("secondary");
                     }
                  }
               }
            }
         }
      }

      return secondaryImages;
   }

   private Integer crawlStock(JSONObject jsonSku) {
      Integer stock = null;

      Double stockDouble = JSONUtils.getDoubleValueFromJSON(jsonSku, "StockBalance", false);
      if (stockDouble != null) {
         stock = stockDouble.intValue();
      }

      return stock;
   }

   private Prices crawlPrices(String priceId, String internalPid, Float price, JSONObject jsonSku) {
      Prices prices = new Prices();

      if (price != null) {

         String priceDescription = JSONUtils.getStringValue(jsonSku, "priceDescription");
         if (priceDescription != null) {
            String html = priceDescription.replace("&lt;", "<").replace("&gt;", ">").replace("&#39;", "\"").replace("&quot;", "");

            prices.setBankTicketPrice(CrawlerUtils.scrapDoublePriceFromHtml(Jsoup.parse(html), ".sale-price span[itemprop=price]", null, true, ',',
                  session));
         }

         prices.setPriceFrom(JSONUtils.getDoubleValueFromJSON(jsonSku, "priceBase", false));

         String url = "https://www.estrela10.com.br/widget/product_payment_options?SkuID=" + internalPid + "&ProductID=" + priceId
               + "&Template=wd.product.payment.options.result.template&ForceWidgetToRender=true";

         Request request = RequestBuilder.create().setUrl(url).setCookies(cookies).build();
         Document docPrices = Jsoup.parse(this.dataFetcher.get(session, request).getBody());

         Elements cards = docPrices.select(".grid table th img");
         for (int i = 0; i < cards.size(); i++) {
            Element card = cards.get(i);

            String cardName = card.attr("title").toLowerCase();

            if (cardName.contains("visa")) {
               Map<Integer, Float> installmentPriceMap = getInstallmentsForCard(docPrices, i);
               prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);

            } else if (cardName.contains("mastercard")) {
               Map<Integer, Float> installmentPriceMap = getInstallmentsForCard(docPrices, i);
               prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);

            } else if (cardName.contains("diners")) {
               Map<Integer, Float> installmentPriceMap = getInstallmentsForCard(docPrices, i);
               prices.insertCardInstallment(Card.DINERS.toString(), installmentPriceMap);

            } else if (cardName.contains("american") || cardName.contains("amex")) {
               Map<Integer, Float> installmentPriceMap = getInstallmentsForCard(docPrices, i);
               prices.insertCardInstallment(Card.AMEX.toString(), installmentPriceMap);

            } else if (cardName.contains("hipercard")) {
               Map<Integer, Float> installmentPriceMap = getInstallmentsForCard(docPrices, i);
               prices.insertCardInstallment(Card.HIPERCARD.toString(), installmentPriceMap);

            } else if (cardName.contains("credicard")) {
               Map<Integer, Float> installmentPriceMap = getInstallmentsForCard(docPrices, i);
               prices.insertCardInstallment(Card.CREDICARD.toString(), installmentPriceMap);

            } else if (cardName.contains("elo")) {
               Map<Integer, Float> installmentPriceMap = getInstallmentsForCard(docPrices, i);
               prices.insertCardInstallment(Card.ELO.toString(), installmentPriceMap);

            } else if (cardName.contains("aura")) {
               Map<Integer, Float> installmentPriceMap = getInstallmentsForCard(docPrices, i);
               prices.insertCardInstallment(Card.AURA.toString(), installmentPriceMap);

            } else if (cardName.contains("discover")) {
               Map<Integer, Float> installmentPriceMap = getInstallmentsForCard(docPrices, i);
               prices.insertCardInstallment(Card.DISCOVER.toString(), installmentPriceMap);

            }
         }
      }

      return prices;
   }

   private Map<Integer, Float> getInstallmentsForCard(Document doc, int idCard) {
      Map<Integer, Float> mapInstallments = new HashMap<>();

      Elements installmentsCards = doc.select(".modal-wd-product-payment-options > .grid:not(:last-child) table");

      Element cardInstallment = installmentsCards.get(idCard);
      Elements installments = cardInstallment.select("tbody tr td");

      for (Element e : installments) {
         String text = e.text().toLowerCase();

         if (text.contains("vista")) {
            Float value = MathUtils.parseFloatWithComma(text);
            mapInstallments.put(1, value);
         } else {
            Pair<Integer, Float> installment = CrawlerUtils.crawlSimpleInstallment(null, e, false);

            if (!installment.isAnyValueNull()) {
               mapInstallments.put(installment.getFirst(), installment.getSecond());
            }
         }
      }

      return mapInstallments;
   }

   private RatingsReviews scrapRating(String internalId, Document doc) {
      TrustvoxRatingCrawler trustVox = new TrustvoxRatingCrawler(session, "30085", logger);
      return trustVox.extractRatingAndReviews(internalId, doc, dataFetcher);
   }

}
