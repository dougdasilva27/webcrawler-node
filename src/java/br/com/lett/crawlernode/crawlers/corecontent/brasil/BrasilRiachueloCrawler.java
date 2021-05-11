package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.models.FetcherOptions.FetcherOptionsBuilder;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.fetcher.models.Response;
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
import org.apache.http.cookie.Cookie;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class BrasilRiachueloCrawler extends Crawler {

   private static final String HOME_PAGE = "https://www.riachuelo.com.br/";
   private static final String SELLER_FULL_NAME = "Riachuelo";
   protected Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),
      Card.AURA.toString(), Card.DINERS.toString(), Card.HIPER.toString(), Card.AMEX.toString());

   private static String x_app_token = "";

   public BrasilRiachueloCrawler(Session session) {
      super(session);
      super.config.setFetcher(FetchMode.JAVANET);
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
      headers.put("accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9");
      headers.put("accept-encoding", "gzip, deflate, br");
      headers.put("accept-language", "pt-BR,pt;q=0.9,en-US;q=0.8,en;q=0.7");
      headers.put("connection", "keep-alive");
      headers.put("pragma", "no-cache");
      headers.put("cache-control", "no-cache");
      headers.put("upgrade-insecure-requests", "1");
      headers.put("sec-fetch-dest", "document");
      headers.put("sec-fetch-mode", "navigate");
      headers.put("sec-fetch-site", "same-origin");
      headers.put("sec-fetch-user", "?1");
      headers.put("user-agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/88.0.4324.182 Safari/537.36");
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

      Response response = this.dataFetcher.get(session, request);

      List<Cookie> cookies = response.getCookies();

      if(cookies.contains("token-aws")){
         x_app_token = "cookies.";
      }


      return response.getBody();
   }

   private JSONObject fetchVariations(){
      String url = "https://api-dc-rchlo-prd.riachuelo.com.br/ecommerce-web-catalog/v2/products/variations?slug=";
      url += session.getOriginalURL().substring(28);

      Map<String, String> headers = new HashMap<>();
      headers.put("accept", "*/*");
      headers.put("accept-encoding", "no");
      headers.put("connection", "keep-alive");
      headers.put("x-api-key", "KhMO3jH1hsjvSRzQXNfForv5FrnfSpX6StdqMmjncjGivPBj3MS4kFzRWn2j7MPn");
      headers.put("x-app-token", "eyJraWQiOiJLZ1NcLytSZFlwVWJYTkJzbUs0NXNJS0poZjQwUmVoNndhQWtYSW1COGNVZz0iLCJhbGciOiJSUzI1NiJ9.eyJzdWIiOiI5MzE4NzZhZS0yN2NhLTQ2ODMtOTFkNy1hZjIzMzExYTI4M2EiLCJhdWQiOiIzZms4aGg1aHA4NTRyaGNiaG1wMjQxbGRwMSIsImV2ZW50X2lkIjoiMDQwNjJiN2EtMTBjMi00OWUyLWFkMWEtMDZjMjI5NjdhODZjIiwidG9rZW5fdXNlIjoiaWQiLCJhdXRoX3RpbWUiOjE2MjAzMjI3MzgsImlzcyI6Imh0dHBzOlwvXC9jb2duaXRvLWlkcC51cy1lYXN0LTEuYW1hem9uYXdzLmNvbVwvdXMtZWFzdC0xX1V1ZkUySFlXayIsIm5hbWUiOiJFLWNvbW1lcmNlIFdFQiAtIFBSRCIsIm5pY2tuYW1lIjoiYmY2MGNiOTEtYTg2ZC00YTY4LTg2ZWItNDY4NTViNDczOGM4IiwiY29nbml0bzp1c2VybmFtZSI6IjV0cVoyS0p2YlRld1dsOGpCYVU4OFVXeEJxZU5DR01RIiwiZXhwIjoxNjIwMzI2MzM4LCJpYXQiOjE2MjAzMjI3Mzh9.Q0QvYhKqXYImK61XNxsxGBMQRxP5fV4fiZizuLcCIqIRZTNWBpmRu2ZYvYWUhkAn_dnMBBimiksy6gM2lUd7c9gIoTLk_Ixdza4TCXXUZr4z4Kaefa456yjtHjV9a7b9H74ZsUGNRnUZbrWFG2jaHuSbI3glYaynOvQ8eRXFBwRkpS00hlYVWhZGKWFTGtyn5ioBNYRSMeUC-UIvF9yrsE40lU9M4Hb5MmGTl9eklEqf2iysDZkPbQojE4tmJM960dbgXCR-ctzj4aof_QGN66BGtPOG5Kw6-_MZCPTx2I039yqu39cqy526ri9zWeGOXU9sCC3rMgd_Pa4FCqznww");

      Request request = RequestBuilder.create()
         .setUrl(url)
         .setIgnoreStatusCode(false)
         .mustSendContentEncoding(false)
         .setHeaders(headers)
         .setProxyservice(Arrays.asList(
            ProxyCollection.INFATICA_RESIDENTIAL_BR,
            ProxyCollection.NETNUT_RESIDENTIAL_BR))
         .build();
      String jsonStr = this.dataFetcher.get(session, request).getBody();

      return JSONUtils.stringToJson(jsonStr);
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      List<Product> products = new ArrayList<>();

      if (isProductPage(doc)) {
         JSONObject jsonConfig = crawlJsonHtml(doc);

         JSONObject json = CrawlerUtils.selectJsonFromHtml(doc, "script[id=__NEXT_DATA__]", null, null, false, false);

         JSONArray jsonImages = crawlJsonImageData(doc);

         String internalPid = crawlInternalPid(doc);

         String description = CrawlerUtils.scrapSimpleDescription(doc, Collections.singletonList("#jq-product-info-accordion"));

         JSONObject options = JSONUtils.getJSONValue(jsonConfig,"optionPrices");

         JSONObject jsonVariations = fetchVariations();

         if (jsonVariations != null && !jsonVariations.isEmpty()) {
            Map<String, Set<String>> variationsMap = crawlVariationsMap(jsonConfig);

            if(true){

//               boolean available = crawlAvailabilityWithVariation(jsonConfig, id);
//
//               String name = crawlNameWithVariation(doc, variationsMap, id);
//               String primaryImage = crawlPrimaryImageWithVariation(jsonImages, jsonConfig, id);
//
//               String secondaryImages = crawlSecondaryImagesWithVariation(jsonImages, jsonConfig, id, available, primaryImage);
//
//               Integer stock = null;
//
//               Offers offers = scrapOffers(doc, jsonConfig, id, available);
//
//               String internalId = scrapInternalId(jsonConfig, id);

               Product product = ProductBuilder.create()
                  .setUrl(session.getOriginalURL())
//                  .setInternalId(internalId)
//                  .setInternalPid(internalPid)
//                  .setOffers(offers)
//                  .setName(name)
//                  .setPrimaryImage(primaryImage)
//                  .setSecondaryImages(secondaryImages)
//                  .setDescription(description)
//                  .setStock(stock)
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

      JSONObject jsonHtml = CrawlerUtils.selectJsonFromHtml(doc, "script[id=__NEXT_DATA__]", null, null, false, false);
//         doc.select("script[type='text/x-magento-init']")
//         .stream()
//         .filter(element -> element.html().contains("jsonConfig"))
//         .map(script -> CrawlerUtils.stringToJson(script.html()))
//         .findFirst()
//         .orElse(new JSONObject());

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
      return doc.selectFirst("h1.MuiTypography-root.jss90.MuiTypography-h1.MuiTypography-gutterBottom") != null;
   }
}
