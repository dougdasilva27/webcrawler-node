package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.*;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
import com.google.common.collect.Sets;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.Offer;
import models.Offers;
import models.pricing.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class BrasilLojinhaBabyMeCrawler extends Crawler {
   public BrasilLojinhaBabyMeCrawler(Session session) {
      super(session);
      super.config.setParser(Parser.HTML);
   }

   @Override
   protected Response fetchResponse() {
      try {
         HttpClient client = HttpClient.newBuilder().build();
         HttpRequest request = HttpRequest.newBuilder()
            .GET()
            .uri(URI.create(session.getOriginalURL()))
            .build();
         HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
         return new Response.ResponseBuilder()
            .setBody(response.body())
            .setLastStatusCode(response.statusCode())
            .build();
      } catch (Exception e) {
         throw new RuntimeException("Faleid In load document: " + session.getOriginalURL(), e);
      }
   }

   private static final String SELLER_FULL_NAME = "Lojinha Baby e Me";
   private static final Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(), Card.ELO.toString(),
      Card.DINERS.toString(), Card.DISCOVER.toString(), Card.AMEX.toString(), Card.JCB.toString(), Card.AURA.toString(), Card.HIPERCARD.toString());

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      if (isProductPage(doc)) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, ".product-info-price  div.price-final_price", "data-product-id");
         String name = CrawlerUtils.scrapStringSimpleInfo(doc, "h1.page-title span", true);
         CategoryCollection categories = CrawlerUtils.crawlCategories(doc, "ul.items li.item a");
         List<String> secondaryImages = crawImagesArray(doc);
         String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, ".gallery-placeholder__image", Arrays.asList("src"), "https", "www.lojinhababyandme.com.br");
         String description = CrawlerUtils.scrapElementsDescription(doc, Arrays.asList(".product.attribute.overview .value", ".product.description p"));
         boolean availableToBuy = doc.selectFirst(".product-info-stock-sku .available") != null;
         Offers offers = availableToBuy ? scrapOffer(doc) : new Offers();

         // Creating the product
         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setInternalPid(internalId)
            .setName(name)
            .setCategories(categories)
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

   private List<String> crawImagesArray(Document doc) {
      Element imageScript = doc.selectFirst("script:containsData(mage/gallery/gallery)");
      if (imageScript != null) {
         JSONObject imageToJson = CrawlerUtils.stringToJson(imageScript.html());
         JSONArray imageArray = JSONUtils.getValueRecursive(imageToJson, "[data-gallery-role=gallery-placeholder].mage/gallery/gallery.data", JSONArray.class, new JSONArray());
         List<String> imagesList = new ArrayList<>();
         for (int i = 0; i < imageArray.length(); i++) {
            Boolean isMainImage = JSONUtils.getValueRecursive(imageArray, i + ".isMain", Boolean.class);
            if (!isMainImage) {
               String imageList = JSONUtils.getValueRecursive(imageArray, i + ".img", String.class);
               imagesList.add(imageList);
            }
         }
         return imagesList;
      }
      return null;
   }

   private boolean isProductPage(Document doc) {
      return doc.selectFirst(".product-info-main") != null;
   }

   private Offers scrapOffer(Document doc) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(doc);
      List<String> sales = scrapSales(doc);

      offers.add(Offer.OfferBuilder.create()
         .setUseSlugNameAsInternalSellerId(true)
         .setSellerFullName(SELLER_FULL_NAME)
         .setMainPagePosition(1)
         .setIsBuybox(false)
         .setIsMainRetailer(true)
         .setPricing(pricing)
         .setSales(sales)
         .build());

      return offers;
   }

   private Pricing scrapPricing(Document doc) throws MalformedPricingException {
      Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".product-info-price [id*=product-price] .price", null, true, ',', session);
      if (spotlightPrice == null) {
         spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, "[data-price-type=\"minPrice\"] span", null, true, ',', session);
      }
      Double priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".product-info-price .old-price .price", null, true, ',', session);

      if (spotlightPrice != null && spotlightPrice.equals(priceFrom)) {
         priceFrom = null;
      }

      CreditCards creditCards = scrapCreditCards(doc, spotlightPrice);

      return Pricing.PricingBuilder.create()
         .setPriceFrom(priceFrom)
         .setSpotlightPrice(spotlightPrice)
         .setCreditCards(creditCards)
         .build();
   }

   private List<String> scrapSales(Document doc) {
      List<String> sales = new ArrayList<>();

      Element salesOneElement = doc.selectFirst(".product.attribute.overview strong");
      String firstSales = salesOneElement != null ? salesOneElement.text() : null;

      if (firstSales != null && !firstSales.isEmpty()) {
         sales.add(firstSales);
      }

      Element salesOneComboElement = doc.selectFirst(".label .product-name");
      String firstSalesCombo = salesOneComboElement != null ? salesOneComboElement.text() : null;

      if (firstSalesCombo != null && !firstSalesCombo.isEmpty()) {
         sales.add(firstSalesCombo);
      }

      return sales;
   }

   private CreditCards scrapCreditCards(Document doc, Double spotlightPrice) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();

      Installments installments = scrapInstallments(doc);
      if (installments.getInstallments().isEmpty()) {
         installments.add(Installment.InstallmentBuilder.create()
            .setInstallmentNumber(1)
            .setInstallmentPrice(spotlightPrice)
            .build());
      }

      for (String card : cards) {
         creditCards.add(CreditCard.CreditCardBuilder.create()
            .setBrand(card)
            .setInstallments(installments)
            .setIsShopCard(false)
            .build());
      }

      return creditCards;
   }

   public Installments scrapInstallments(Document doc) throws MalformedPricingException {
      Installments installments = new Installments();

      Element installmentsCard = doc.selectFirst(".info_productPrice .product_discount_pay span");

      if (installmentsCard != null) {
         String installmentString = installmentsCard.text().replaceAll("[^0-9]", "").trim();
         Integer installment = !installmentString.isEmpty() ? Integer.parseInt(installmentString) : null;
         Element valueElement = doc.selectFirst(".info_productPrice .product_discount_pay strong");

         if (valueElement != null && installment != null) {
            Double value = MathUtils.parseDoubleWithComma(valueElement.text());

            installments.add(Installment.InstallmentBuilder.create()
               .setInstallmentNumber(installment)
               .setInstallmentPrice(value)
               .build());
         }
      }

      return installments;
   }
}
