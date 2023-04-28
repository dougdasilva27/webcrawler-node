package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.crawlers.extractionutils.core.TrustvoxRatingCrawler;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import br.com.lett.crawlernode.util.Logging;
import com.google.common.collect.Sets;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.Offer;
import models.Offers;
import models.RatingsReviews;
import models.pricing.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SaopauloDrogaraiaCrawler extends Crawler {

   private static final String SELLER_FULL_NAME = "drogaraia";
   protected Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),
      Card.AURA.toString(), Card.DINERS.toString(), Card.HIPER.toString(), Card.AMEX.toString());

   public SaopauloDrogaraiaCrawler(Session session) {
      super(session);
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      Element scriptHTML = doc.selectFirst("script[type=\"application/ld+json\"]");
      JSONObject json = CrawlerUtils.selectJsonFromHtml(doc, "#__NEXT_DATA__", null, " ", false, false);
      JSONObject data = JSONUtils.getValueRecursive(json, "props.pageProps.pageData.productData.productBySku", JSONObject.class);

      if (scriptHTML != null) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         JSONObject scriptJson = CrawlerUtils.stringToJson(scriptHTML.html());

         String internalId = JSONUtils.getStringValue(scriptJson, "sku");
         String internalPid = data.optString("id");
         String name = JSONUtils.getStringValue(scriptJson, "name");
         String completeName = getName(doc, name);

         String ean = JSONUtils.getStringValue(scriptJson, "gtin13");
         List<String> eans = new ArrayList<>();
         eans.add(ean);

         RatingsReviews ratingReviews = crawRating(internalId);
         List<String> categories = CrawlerUtils.crawlCategories(doc, "main ul > li > a");

         List<String> images = scrapListImages(data, doc);
         String primaryImage = !images.isEmpty() ? images.remove(0) : null;

         String description = scrapDescription(data, doc);

         Boolean available = crawlAvailability(internalId, data);

         Offers offers = available != null && available ? scrapOffers(data, doc) : new Offers();

         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setInternalPid(internalPid)
            .setName(completeName)
            .setCategories(categories)
            .setPrimaryImage(primaryImage)
            .setSecondaryImages(images)
            .setDescription(description)
            .setRatingReviews(ratingReviews)
            .setOffers(offers)
            .setEans(eans)
            .build();

         products.add(product);

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }

   private Boolean crawlAvailability(String internalId, JSONObject data) {
      String payload = "{\"operationName\":\"liveComposition\",\"variables\":{\"skuList\":[\"" + internalId + "\"],\"origin\":\"\"},\"query\":\"query liveComposition($skuList: [String!]!, $origin: String!) {\\n  liveComposition(input: {skuList: $skuList, origin: $origin}) {\\n    sku\\n    livePrice {\\n      bestPrice {\\n        valueFrom\\n        valueTo\\n        updateAt\\n        type\\n        discountPercentage\\n        lmpmValueTo\\n        lmpmQty\\n        __typename\\n      }\\n      calcule {\\n        valueFrom\\n        valueTo\\n        lmpmValueTo\\n        lmpmQty\\n        updateAt\\n        type\\n        __typename\\n      }\\n      discountPercentage\\n      sku\\n      type\\n      updateAt\\n      valueFrom\\n      valueTo\\n      lmpmValueTo\\n      lmpmQty\\n      __typename\\n    }\\n    liveStock {\\n      sku\\n      qty\\n      dt\\n      __typename\\n    }\\n    __typename\\n  }\\n}\\n\"}";
      String url = "https://bff.drogaraia.com.br/graphql";
      String object;
      try {
         HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

         HttpRequest request = HttpRequest.newBuilder()
            .POST(HttpRequest.BodyPublishers.ofString(payload))
            .uri(URI.create(url))
            .headers("Content-type", "application/json", "x-session-token-cart", "icUprn4asLubxc3Gpl9jlLoxm2fxv9cT", "authority", "bff.drogaraia.com.br")
            .build();

         HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
         object = response.body();
      } catch (IOException | InterruptedException e) {
         throw new RuntimeException("Failed to scrape API: " + url, e);
      }
      JSONObject jsonStock = JSONUtils.stringToJson(object);
      Integer stock = JSONUtils.getValueRecursive(jsonStock, "data.liveComposition.0.liveStock.qty", Integer.class);

      if (stock == null) {
         return JSONUtils.getValueRecursive(data, "extension_attributes.stock_item.is_in_stock", Boolean.class);
      }
      return stock > 0;
   }

   private List<String> scrapListImages(JSONObject data, Document doc) {
      List<String> images = new ArrayList<>();
      JSONArray imagesJson = JSONUtils.getValueRecursive(data, "media_gallery_entries", JSONArray.class);
      if (imagesJson != null) {
         for (int i = 0; i < imagesJson.length(); i++) {
            String imageFile = JSONUtils.getValueRecursive(imagesJson, i + ".file", String.class);
            if (imageFile != null) {
               String image = "https://img.drogaraia.com.br/catalog/product/" + imageFile.replace("/p/", "p/");
               images.add(image);
            } else {
               String img = CrawlerUtils.scrapSimplePrimaryImage(doc, ".swiper-lazy img", Arrays.asList("src"), "https", "");
               images.add(img);
            }

         }
      }
      return images;
   }

   private String scrapDescription(JSONObject json, Document doc) {
      JSONArray descriptionArray = json.optJSONArray("custom_attributes");

      if (descriptionArray != null) {
         for (Object attribute : descriptionArray) {

            if (JSONUtils.getValueRecursive(attribute, "attribute_code", String.class, "").equals("description")) {
               String jsonDescription = JSONUtils.getValueRecursive(attribute, "value_string.0", String.class);

               if (!doc.select("div[data-testid=\"table-features\"]").isEmpty()) {
                  String htmlDescription = CrawlerUtils.scrapElementsDescription(doc, Arrays.asList("[class*=\"ConverterHtmlstyles\"] li", "div[data-testid=\"htmlParse\"]", "[class*=\"RaiaProductDescriptionstyles\"] a"));
                  if (!htmlDescription.isEmpty()) {
                     return jsonDescription + htmlDescription;
                  }

               }

               return jsonDescription;
            }
         }
      }
      return null;
   }

   /* Brief explanation of the function
   The number of units and the size of the product must be captured in the crawler (ex: 15ml);

    1 - in some cases the number of units is already in the title but the size (15 ml) is only in the subtitle
    -> in this case, the crawler checks to make a split in "-" and checks if the first index is already in the name;
    2 - in other cases, only the quantity is given and nothing should be added to the name
    -> in this case the crawler checks through the regex if it is stated in the number of units in the name
    3 - In another case, the name does not contain anything of quantity
    -> in this case, the crawler adds the entire subtitle to the name;
    4 - In the last case, when the product is unavailable, the name is elsewhere;
    -> which is the last else, taking only the title name;

    follow the examples:

    https://www.drogaraia.com.br/pampers-premium-care-tamanho-grande-com-68-tiras.html, in this case, the crawler checks whether the quantity is listed in the name;
    https://www.drogaraia.com.br/pantene-ampola-de-tratamento-gold-com-3-unidades-15-ml-cada.html, in this case, the crawler splits the "-" and checks whether the first part repeats in the name;
    https://www.drogaraia.com.br/always-absorvente-externo-active-com-abas-leve-32-com-preco-especial.html, in this case the crawler adds the entire subtitle to the name
     */

   private String getName(Document doc, String name) {

      String quantity = CrawlerUtils.scrapStringSimpleInfo(doc, ".quantity", true);
      if (name != null && quantity != null) {

         if (quantity.contains("-")) {
            String[] quantitySplit = quantity.split(" -");
            String quantityCompare = quantitySplit[0];

            if (name.contains(quantityCompare)) {
               return quantitySplit.length > 0 ? name + " " + quantitySplit[1] : null;

            } else {
               return name + " " + quantity;
            }

         }
         Pattern r = Pattern.compile("[0-9]+");
         Matcher m = r.matcher(quantity);
         if (m.find()) {
            if (name.contains(m.group(0))) {
               return name;

            } else {
               return name + " " + quantity;
            }
         }

      }

      return name;
   }

   private List<String> scrapSales(Pricing pricing, JSONObject data) {
      List<String> sales = new ArrayList<>();
      sales.add(CrawlerUtils.calculateSales(pricing));

      Object salesQuantity = data.optQuery("/price_aux/lmpm_qty");
      Object salesPrice = data.optQuery("/price_aux/lmpm_value_to");

      if (salesQuantity instanceof Integer && salesPrice != null) {
         int quantity = (int) salesQuantity;
         Double price = CommonMethods.objectToDouble(salesPrice);
         if (quantity > 1 && price != null) {
            sales.add("Leve " + quantity + " unidades por R$ " + price + " cada");
         }
      }

      return sales;
   }

   private String isMainSeller(Document doc) {
      String isMarketPlace = CrawlerUtils.scrapStringSimpleInfo(doc, "div[class*='SoldAndDelivered'] a", true);

      if (isMarketPlace != null && !isMarketPlace.isEmpty()) {
         return isMarketPlace;
      }

      return SELLER_FULL_NAME;
   }

   private Offers scrapOffers(JSONObject data, Document doc) throws MalformedPricingException, OfferException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(data);
      List<String> sales = scrapSales(pricing, data);

      String mainSeller = isMainSeller(doc);

      offers.add(Offer.OfferBuilder.create()
         .setUseSlugNameAsInternalSellerId(true)
         .setSellerFullName(mainSeller)
         .setMainPagePosition(1)
         .setIsBuybox(false)
         .setIsMainRetailer(mainSeller.equals(SELLER_FULL_NAME))
         .setPricing(pricing)
         .setSales(sales)
         .build());


      return offers;
   }

   private Pricing scrapPricing(JSONObject data) throws MalformedPricingException {
      Object spotlightPriceObject = data.optQuery("/price_aux/value_to");
      Object priceFromObject = data.optQuery("/price_aux/value_from");

      Double spotlightPrice = CommonMethods.objectToDouble(spotlightPriceObject);
      Double priceFrom = CommonMethods.objectToDouble(priceFromObject);

      if (Objects.equals(priceFrom, spotlightPrice)) priceFrom = null;

      CreditCards creditCards = scrapCreditCards(spotlightPrice);
      BankSlip bankSlip = BankSlip.BankSlipBuilder.create()
         .setFinalPrice(spotlightPrice)
         .build();

      return Pricing.PricingBuilder.create()
         .setSpotlightPrice(spotlightPrice)
         .setPriceFrom(priceFrom)
         .setCreditCards(creditCards)
         .setBankSlip(bankSlip)
         .build();
   }

   private CreditCards scrapCreditCards(Double spotlightPrice) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();
      Installments installments = new Installments();
      installments.add(Installment.InstallmentBuilder.create()
         .setInstallmentNumber(1)
         .setInstallmentPrice(spotlightPrice)
         .build());


      for (String card : cards) {
         creditCards.add(CreditCard.CreditCardBuilder.create()
            .setBrand(card)
            .setInstallments(installments)
            .setIsShopCard(false)
            .build());
      }

      return creditCards;
   }

   private RatingsReviews crawRating(String internalId) {
      TrustvoxRatingCrawler trustVox = new TrustvoxRatingCrawler(session, "71450", logger);

      return trustVox.extractV2RatingAndReviews(internalId, this.dataFetcher);
   }
}
