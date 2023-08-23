package br.com.lett.crawlernode.crawlers.corecontent.argentina;

import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
import com.google.common.collect.Sets;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.Offer.OfferBuilder;
import models.Offers;
import models.pricing.CreditCard.CreditCardBuilder;
import models.pricing.CreditCards;
import models.pricing.Installment.InstallmentBuilder;
import models.pricing.Installments;
import models.pricing.Pricing;
import models.pricing.Pricing.PricingBuilder;
import org.json.JSONArray;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Date: 07/12/2016
 * <p>
 * 1) Only one sku per page.
 * <p>
 * Price crawling notes: 1) In the time this crawler was made, we doesn't found any unnavailable
 * product. 2) There is no bank slip (boleto bancario) payment option. 3) There is installments for
 * card payment, but was found only shopCard payment method.
 *
 * @author Gabriel Dornelas
 */
public class ArgentinaCotoCrawler extends Crawler {

   private final String HOME_PAGE = "https://www.cotodigital3.com.ar/";
   private static final String SELLER_FULL_NAME = "Coto";
   protected Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),
      Card.AURA.toString(), Card.DINERS.toString(), Card.HIPER.toString(), Card.AMEX.toString());

   public ArgentinaCotoCrawler(Session session) {
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

         String internalId = crawlInternalId(doc);
         String internalPid = crawlInternalPid(doc);
         String name = crawlName(doc);
         CategoryCollection categories = crawlCategories(doc);
         String primaryImage = crawlPrimaryImage(doc);
         String secondaryImages = crawlSecondaryImages(doc);
         String description = crawlDescription(doc);
         Integer stock = null;
         boolean availableToBuy = crawlAvailability(doc);
         Offers offers = availableToBuy ? scrapOffer(doc, internalId) : new Offers();

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
            .setStock(stock)
            .setOffers(offers)
            .build();

         products.add(product);

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;

   }

   private boolean isProductPage(Document doc) {
      return doc.selectFirst(".product_page") != null;
   }

   private String crawlInternalId(Document document) {
      String internalId = null;

      Element internalIdElement = document.select(".span_codigoplu").first();
      if (internalIdElement != null) {
         internalId = internalIdElement.text().replaceAll("[^0-9]", "");
      }

      return internalId;
   }

   public String crawlInternalPid(Document doc) {
      String href = doc.select("#zoomContent > a").first().attr("href");

      Pattern pattern = Pattern.compile("/(\\d{8})\\.jpg");
      Matcher matcher = pattern.matcher(href);
      if (matcher.find()) {
         return matcher.group(1);
      }
      return null;
   }

   private String crawlName(Document document) {
      String name = null;
      Element nameElement = document.select("h1.product_page").first();

      if (nameElement != null) {
         name = nameElement.ownText().trim();
      }

      return name;
   }


   private String crawlPrimaryImage(Document document) {
      String primaryImage = null;
      Element primaryImageElement = document.select("a.gall-item").first();

      if (primaryImageElement != null) {
         primaryImage = primaryImageElement.attr("href").trim();

         if (primaryImage.contains("?")) {
            primaryImage = primaryImage.split("\\?")[0];
         }
      }

      return primaryImage;
   }

   private String crawlSecondaryImages(Document document) {
      String secondaryImages = null;
      JSONArray secondaryImagesArray = new JSONArray();

      Elements imagesElement = document.select(".zoomThumbLink > img");

      for (int i = 1; i < imagesElement.size(); i++) { // first index is the primary image
         String image = imagesElement.get(i).attr("data-large").trim();
         secondaryImagesArray.put(image);
      }

      if (secondaryImagesArray.length() > 0) {
         secondaryImages = secondaryImagesArray.toString();
      }

      return secondaryImages;
   }

   private CategoryCollection crawlCategories(Document document) {
      CategoryCollection categories = new CategoryCollection();

      Elements elementCategories = document.select("#atg_store_breadcrumbs a:not(.atg_store_navLogo) p");
      for (int i = 0; i < elementCategories.size(); i++) {
         categories.add(elementCategories.get(i).ownText().trim());
      }

      return categories;
   }

   private String crawlDescription(Document document) {
      StringBuilder description = new StringBuilder();

      String haveDescription = CrawlerUtils.scrapStringSimpleInfo(document, ".product_detail_comentario #txtComentario", true);

      if (haveDescription != null && !haveDescription.equals("----")) {
         description.append(haveDescription);
      }

      String haveCarac = CrawlerUtils.scrapStringSimpleInfo(document, ".mute .tbldatatextie span", true);
      if (haveCarac != null && !haveCarac.contains("No se encontraron")) {
         Elements caracElements = document.select(".tblData .mute td");
         if (caracElements != null) {

            for (Element e : caracElements) {
               String caracElement = CrawlerUtils.scrapStringSimpleInfo(e, "span", true);

               if (caracElement != null) {
                  description.append(caracElement).append(" ");
               }
            }
         }
      }
      return description.toString();
   }

   private Offers scrapOffer(Document doc, String internalId) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(internalId, doc);
      List<String> sales = scrapSales(doc);

      offers.add(OfferBuilder.create()
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

   private List<String> scrapSales(Document doc) {
      List<String> sales = new ArrayList<>();

      Element salesOneElement = doc.selectFirst(".first_price_discount_container");
      String firstSales = salesOneElement != null ? salesOneElement.text() : null;

      if (firstSales != null && !firstSales.isEmpty()) {
         sales.add(firstSales);
      }

      return sales;
   }

   private Double scrapSpotlightPrice(Document doc) {
      Double price = null;
      Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".price_regular_precio,.atg_store_newPrice,.atg_store_oldPrice.price_regular", null, false, ',', session);
      Double priceDiscount = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".first_price_discount_container .price_discount", null, false, '.', session);
      if (priceDiscount != null) {
         price = priceDiscount;
      } else {
         price = spotlightPrice;
      }
      return price;
   }

   private Double scrapPriceFrom(Document doc, Double spotlightPrice) {
      Double priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".price_regular_precio", null, false, '.', session);
      if (spotlightPrice.equals(priceFrom)) {
         priceFrom = null;
      }
      return priceFrom;
   }

   private Pricing scrapPricing(String internalId, Document doc) throws MalformedPricingException {
      Double spotlightPrice = scrapSpotlightPrice(doc);
      Double priceFrom = scrapPriceFrom(doc, spotlightPrice);

      CreditCards creditCards = scrapCreditCards(doc, internalId, spotlightPrice);

      return PricingBuilder.create()
         .setPriceFrom(priceFrom)
         .setSpotlightPrice(spotlightPrice)
         .setCreditCards(creditCards)
         .build();
   }


   private CreditCards scrapCreditCards(Document doc, String internalId, Double spotlightPrice) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();

      Installments installments = scrapInstallments(doc);
      if (installments.getInstallments().isEmpty()) {
         installments.add(InstallmentBuilder.create()
            .setInstallmentNumber(1)
            .setInstallmentPrice(spotlightPrice)
            .build());
      }

      for (String card : cards) {
         creditCards.add(CreditCardBuilder.create()
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

            installments.add(InstallmentBuilder.create()
               .setInstallmentNumber(installment)
               .setInstallmentPrice(value)
               .build());
         }
      }

      return installments;
   }

   private boolean crawlAvailability(Document document) {
      return !document.select(".add_products :not(.product_not_available)").isEmpty();
   }

}
