package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.models.FetcherOptions.FetcherOptionsBuilder;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import br.com.lett.crawlernode.util.Logging;
import com.google.common.collect.Sets;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.Offer.OfferBuilder;
import models.Offers;
import models.pricing.BankSlip.BankSlipBuilder;
import models.pricing.CreditCard;
import models.pricing.CreditCards;
import models.pricing.Installment.InstallmentBuilder;
import models.pricing.Installments;
import models.pricing.Pricing.PricingBuilder;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class BrasilRiachueloCrawler extends Crawler {

   private static final String HOME_PAGE = "https://www.riachuelo.com.br/";
   private static final String SELLER_FULL_NAME = "Riachuelo";
   protected Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),
      Card.AURA.toString(), Card.DINERS.toString(), Card.HIPER.toString(), Card.AMEX.toString());

   public BrasilRiachueloCrawler(Session session) {
      super(session);
      super.config.setFetcher(FetchMode.APACHE);
   }

   @Override
   public boolean shouldVisit() {
      String href = this.session.getOriginalURL().toLowerCase();
      return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
   }

   @Override
   protected Document fetch() {
      return Jsoup.parse(fetchPage(session.getOriginalURL(), session));
   }

   public String fetchPage(String url, Session session) {
      Map<String, String> headers = new HashMap<>();
      headers.put("accept", "*/*");
      headers.put("accept-encoding", "no");
      headers.put("connection", "keep-alive");
      Request request = RequestBuilder.create()
         .setUrl(url)
         .setIgnoreStatusCode(false)
         .mustSendContentEncoding(false)
         .setHeaders(headers)
         .setProxyservice(Arrays.asList(
            ProxyCollection.INFATICA_RESIDENTIAL_BR,
            ProxyCollection.NETNUT_RESIDENTIAL_BR))
         .setFetcheroptions(
            FetcherOptionsBuilder.create()
               .mustUseMovingAverage(false)
               .mustRetrieveStatistics(true)
               .setForbiddenCssSelector("#px-captcha")
               .build()
         ).build();

      return this.dataFetcher.get(session, request).getBody();
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      List<Product> products = new ArrayList<>();

      if (isProductPage(doc)) {
         JSONObject jsonConfig = crawlJsonHtml(doc);

         JSONArray jsonImages = crawlJsonImageData(doc);

         String internalPid = crawlInternalPid(doc);

         String description = CrawlerUtils.scrapSimpleDescription(doc, Collections.singletonList("#jq-product-info-accordion"));

         JSONObject options = JSONUtils.getJSONValue(jsonConfig,"optionPrices");

         if (options.length() > 0) {
            Map<String, Set<String>> variationsMap = crawlVariationsMap(jsonConfig);

            for (String id : options.keySet()) {

               boolean available = crawlAvailabilityWithVariation(jsonConfig, id);

               String name = crawlNameWithVariation(doc, variationsMap, id);
               String primaryImage = crawlPrimaryImageWithVariation(jsonImages, jsonConfig, id);

               String secondaryImages = crawlSecondaryImagesWithVariation(jsonImages, jsonConfig, id, available, primaryImage);

               Integer stock = null;

               Offers offers = scrapOffers(doc, jsonConfig, id, available);

               String internalId = scrapInternalId(jsonConfig, id);

               Product product = ProductBuilder.create()
                  .setUrl(session.getOriginalURL())
                  .setInternalId(internalId)
                  .setInternalPid(internalPid)
                  .setOffers(offers)
                  .setName(name)
                  .setPrimaryImage(primaryImage)
                  .setSecondaryImages(secondaryImages)
                  .setDescription(description)
                  .setStock(stock)
                  .build();

               products.add(product);
            }
         } else {
            String internalId = crawlInternalId(doc);
            String name = crawlName(doc);

            String primaryImage = scrapSimplePrimaryImage(jsonImages);

            String secondaryImages = scrapSimpleSecondaryImage(jsonImages);

            Offers offers = scrapOffers(doc, jsonConfig, internalId, true);

            Product product = ProductBuilder.create()
               .setUrl(session.getOriginalURL())
               .setInternalId(internalId)
               .setInternalPid(internalPid)
               .setName(name)
               .setPrimaryImage(primaryImage)
               .setSecondaryImages(secondaryImages)
               .setDescription(description)
               .setOffers(offers)
               .build();

            products.add(product);
         }

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + session.getOriginalURL());
      }

      return products;
   }

   public String scrapInternalId(JSONObject jsonConfig, String id) {
      JSONObject skuHtml = jsonConfig.optJSONObject("sku-html");

      return skuHtml.getString(id);
   }
   private Offers scrapOffers(Document doc, JSONObject jsonConfig, String internalId, boolean available)
      throws MalformedPricingException, OfferException {
      Offers offers = new Offers();
      Double price;
      if (!available || doc.selectFirst("#product-addtocart-button") == null) {
         return offers;
      }

      JSONObject eachPrice = null;
      if (jsonConfig != null) {
         JSONObject optionPrices = jsonConfig.optJSONObject("optionPrices");
         if (optionPrices != null) {
            eachPrice = optionPrices.optJSONObject(internalId);
         }
      }

      if (eachPrice != null) {
         price = CrawlerUtils
            .getDoubleValueFromJSON(eachPrice.optJSONObject("finalPrice"), "amount", false, false);
      } else {
         price = CrawlerUtils
            .scrapDoublePriceFromHtml(doc, "#product-price-" + internalId, "data-price-amount", false,
               '.', session);
      }

      if (price == null) {
         return offers;
      }

      Double priceFrom = CrawlerUtils
         .scrapDoublePriceFromHtml(doc, ".old-price span[data-price-amount]", "data-price-amount",
            false, '.', session);
      if (Objects.equals(price, priceFrom)) {
         priceFrom = null;
      }

      Element saleElem = doc.selectFirst(".product-discount");
      List<String> sales = new ArrayList<>();
      if (saleElem != null && saleElem.wholeText() != null) {
         String sale = saleElem.wholeText().trim();
         if (Pattern.matches("[0-9]", sale)) {
            sales.add(sale);
         }
      }

      offers.add(OfferBuilder.create()
         .setSales(sales)
         .setPricing(PricingBuilder.create()
            .setSpotlightPrice(price)
            .setPriceFrom(priceFrom)
            .setBankSlip(BankSlipBuilder
               .create()
               .setFinalPrice(price)
               .build())
            .setCreditCards(new CreditCards(extCreditCards(price)))
            .build())
         .setIsMainRetailer(true)
         .setSellerFullName(SELLER_FULL_NAME)
         .setIsBuybox(false)
         .setUseSlugNameAsInternalSellerId(true)
         .build());

      return offers;
   }

   private List<CreditCard> extCreditCards(Double price) {
      return Stream.of(Card.VISA, Card.MASTERCARD, Card.ELO)
         .map(card -> {
            try {
               return CreditCard.CreditCardBuilder.create()
                  .setIsShopCard(false)
                  .setBrand(card.toString())
                  .setInstallments(new Installments(
                     Collections.singleton(InstallmentBuilder
                        .create()
                        .setInstallmentNumber(1)
                        .setInstallmentPrice(price)
                        .setFinalPrice(price)
                        .build())
                  ))
                  .build();
            } catch (MalformedPricingException e) {
               throw new RuntimeException(e);
            }
         })
         .collect(Collectors.toList());
   }

   private String crawlName(Document doc) {

      Element title = doc.selectFirst("h1 span[itemprop=\"name\"]");
      String name = null;

      if (title != null) {
         name = title.text();
      }

      return name;
   }

   private String crawlInternalId(Document doc) {
      Element input = doc.selectFirst("input[name=\"product\"]");
      String internalId = null;

      if (input != null) {
         internalId = input.val();
      }

      return internalId;
   }

   private boolean crawlAvailabilityWithVariation(JSONObject jsonConfig, String internalId) {

      String installment = JSONUtils.getValueRecursive(jsonConfig, "installment-html." +internalId, String.class);

      return installment != null;
   }

   private Map<String, Set<String>> crawlVariationsMap(JSONObject jsonConfig) {
      Map<String, Set<String>> variationsMap = new HashMap<>();
      JSONArray options = new JSONArray();

      if (jsonConfig.has("attributes")) {
         JSONObject attributes = jsonConfig.getJSONObject("attributes");

         for (String keyStr : attributes.keySet()) {
            JSONObject attribute = (JSONObject) attributes.get(keyStr);

            if (attribute.has("options")) {
               options = attribute.getJSONArray("options");
            }
         }
      }

      for (Object object : options) {
         JSONObject option = (JSONObject) object;
         String label = null;
         if (option.has("label")) {
            label = option.getString("label");
         }

         if (option.has("products")) {
            JSONArray products = option.getJSONArray("products");

            for (Object object2 : products) {
               String id = (String) object2;

               if (variationsMap.containsKey(id)) {
                  Set<String> names = variationsMap.get(id);
                  Set<String> newList = new HashSet<>(names);
                  newList.add(label);
                  variationsMap.put(id, newList);
               } else {
                  Set<String> newSet = new HashSet<>();
                  newSet.add(label);
                  variationsMap.put(id, newSet);
               }
            }
         }
      }

      return variationsMap;
   }

   private String crawlSecondaryImagesWithVariation(JSONArray jsonImages, JSONObject jsonConfig, String internalId, boolean available, String primaryImage) {
      String secondaryImages = null;
      JSONArray secondaryImagesArray = new JSONArray();

      if (available) {

         Object imagesObg = JSONUtils.getValue(jsonConfig, "images");

         if (imagesObg instanceof JSONObject) {
            JSONObject images = (JSONObject) imagesObg;

            if (images.has(internalId)) {
               JSONArray image = images.getJSONArray(internalId);

               for (Object object : image) {
                  JSONObject img = (JSONObject) object;

                  if (img.has("isMain") && !img.getBoolean("isMain") && img.has("img")) {
                     secondaryImagesArray.put(img.getString("img"));
                  }
               }
            }
         } else {
            secondaryImages = scrapSimpleSecondaryImage(jsonImages);
         }
      }

      if (secondaryImagesArray.length() > 0) {
         secondaryImages = secondaryImagesArray.toString();
      }

      return secondaryImages;
   }

   private String scrapSimpleSecondaryImage(JSONArray jsonImages) {
      return CrawlerUtils.scrapImagesMagento(jsonImages, false);
   }

   private String scrapSimplePrimaryImage(JSONArray jsonImages) {
      return CrawlerUtils.scrapImagesMagento(jsonImages, true);
   }

   private String crawlPrimaryImageWithVariation(JSONArray jsonImages, JSONObject jsonConfig, String internalId) {
      String primaryImage = null;

      Object imagesObg = JSONUtils.getValue(jsonConfig, "images");

      if (imagesObg instanceof JSONObject) {

         JSONObject images = (JSONObject) imagesObg;

         if (images.has(internalId)) {
            JSONArray image = images.optJSONArray(internalId);

            for (Object object : image) {
               JSONObject img = (JSONObject) object;

               if (img.optBoolean("isMain")) {
                  primaryImage = img.optString("img");
               }
            }
         }
      }
      else {
         primaryImage = scrapSimplePrimaryImage(jsonImages);
      }

      return primaryImage;
   }

   private String crawlNameWithVariation(Document doc, Map<String, Set<String>> variationsMap, String internalId) {
      Element title = doc.selectFirst("h1 span[itemprop=\"name\"]");
      String name = null;

      if (title != null) {
         name = title.text();

         if (variationsMap.containsKey(internalId)) {
            String variation = variationsMap.get(internalId).toString();

            if (variation.contains("[") && variation.contains("]")) {
               variation = variation.replace("[", "").replace("]", "");
            }

            if (variation.contains("disabled")) {
               variation = variation.replaceAll("disabled", "");
            }
            name = name.concat(" ").concat(variation);
         }
      }

      return name;
   }

   private String crawlInternalPid(Document doc) {
      String internalPid = null;
      Element div = doc.selectFirst("div[data-product-id]");

      if (div != null) {
         internalPid = div.attr("data-product-id");
      }

      return internalPid;
   }

   private JSONObject crawlJsonHtml(Document doc) {

      JSONObject jsonHtml =  doc.select("script[type='text/x-magento-init']")
         .stream()
         .filter(element -> element.html().contains("jsonConfig"))
         .map(script -> CrawlerUtils.stringToJson(script.html()))
         .findFirst()
         .orElse(new JSONObject());

      JSONObject jsonConfig = JSONUtils.getValueRecursive(jsonHtml, "[data-role=swatch-options].Magento_Swatches/js/swatch-renderer.jsonConfig", JSONObject.class);

      if (jsonConfig != null) {
         return jsonConfig;
      }

      return new JSONObject();
   }

   private JSONArray crawlJsonImageData(Document doc) {

      JSONObject jsonHtml =  doc.select("script[type='text/x-magento-init']")
         .stream()
         .filter(element -> element.html().contains("[data-gallery-role=gallery-placeholder]"))
         .map(script -> CrawlerUtils.stringToJson(script.html()))
         .findFirst()
         .orElse(new JSONObject());

      JSONArray dataGallery = JSONUtils.getValueRecursive(jsonHtml, "[data-gallery-role=gallery-placeholder].Xumulus_FastGalleryLoad/js/gallery/custom_gallery.data", JSONArray.class);

      if (dataGallery != null) {
         return dataGallery;
      }


      return new JSONArray();
   }

   private boolean isProductPage(Document doc) {
      return doc.selectFirst("h1 span[itemprop=name]") != null;
   }
}
