package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import com.google.common.collect.Sets;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.Offer;
import models.Offers;
import models.pricing.*;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.*;

public class SaopauloImigrantesbebidasCrawler extends Crawler {

   private static final String BASE_URL = "www.imigrantesbebidas.com.br";
   private static final String MAIN_SELLER_NAME = "Imigrantes Bebidas";
   private final Set<String> cards = Sets.newHashSet(Card.VISA.toString(),
      Card.MASTERCARD.toString(), Card.AMEX.toString(), Card.ELO.toString(), Card.DINERS.toString());

   public SaopauloImigrantesbebidasCrawler(Session session) {
      super(session);
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      if (isProductPage(doc)) {
         JSONObject productJson = CrawlerUtils.selectJsonFromHtml(doc, "head > script:nth-of-type(6)", null, "}", false, true);

         String internalId = productJson.optString("sku");
         String internalPId = internalId;
         String name = CrawlerUtils.scrapStringSimpleInfo(doc, ".productPage__name", true);
         List<String> images = scrapImages(doc);
         String primaryImage = !images.isEmpty() ? images.remove(0) : null;
         String description = CrawlerUtils.scrapElementsDescription(doc, Collections.singletonList(".tabs"));
         boolean availability = doc.selectFirst(".out-of-stock") == null;
         Offers offers = availability ? scrapOffers(doc) : new Offers();
         CategoryCollection categories = scrapCategories(productJson);
         List<String> eans = scrapEans(productJson);

         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setInternalPid(internalPId)
            .setName(name)
            .setPrimaryImage(primaryImage)
            .setSecondaryImages(images)
            .setCategory1(categories.getCategory(0))
            .setCategory2(categories.getCategory(1))
            .setCategory3(categories.getCategory(2))
            .setDescription(description)
            .setOffers(offers)
            .setEans(eans)
            .build();

         products.add(product);

      }

      return products;
   }

   private boolean isProductPage(Document doc) {
      return doc.selectFirst(".productPage") != null;
   }

   private List<String> scrapImages (Document doc){
      List<String> imgList = new ArrayList<>();

      Elements el = doc.select("div.slider.productGallery__slider > picture");

      if(el != null){
         el.forEach(img -> imgList.add("https://www.imigrantesbebidas.com.br" + img.attr("rel")));
      }

      return imgList;
   }

   private Offers scrapOffers(Document doc) throws MalformedPricingException, OfferException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(doc);

      if (pricing != null) {
         offers.add(Offer.OfferBuilder.create()
            .setUseSlugNameAsInternalSellerId(true)
            .setSellerFullName(MAIN_SELLER_NAME)
            .setSellersPagePosition(1)
            .setIsBuybox(false)
            .setIsMainRetailer(true)
            .setPricing(pricing)
            .build());
      }
      return offers;
   }

   private Pricing scrapPricing(Document doc) throws MalformedPricingException {
      Double priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".productPage__wholeprice", null, true, ',', this.session);
      Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".productPage__price", null, true, ',', this.session);

      return Pricing.PricingBuilder.create()
         .setSpotlightPrice(spotlightPrice)
         .setPriceFrom(priceFrom)
         .setCreditCards(scrapCreditCards(spotlightPrice))
         .setBankSlip(scrapBankSlip(spotlightPrice))
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

   private BankSlip scrapBankSlip(Double spotlightPrice) throws MalformedPricingException {
      return BankSlip.BankSlipBuilder.create()
         .setFinalPrice(spotlightPrice)
         .build();
   }

   private CategoryCollection scrapCategories(JSONObject productJson) {
      CategoryCollection finalCategory = new CategoryCollection();
      String categories = productJson.optString("category");
      String[] split = categories.split("\\/");
      finalCategory.addAll(Arrays.asList(split));
      return finalCategory;
   }

   private List<String> scrapEans(JSONObject productJson) {
      String key = getEansKey(productJson);
      return key != null ? Collections.singletonList(productJson.optString(key)) : null;
   }

   private String getEansKey(JSONObject productJson) {
      Iterator<String> keys = productJson.keys();
      String currentKey;
      while (keys.hasNext()) {
         currentKey = keys.next();
         if (currentKey.contains("gtin")) {
            return currentKey;
         }
      }
      return null;
   }
}
