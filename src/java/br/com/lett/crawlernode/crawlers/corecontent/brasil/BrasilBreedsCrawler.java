package br.com.lett.crawlernode.crawlers.corecontent.brasil;

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
import models.pricing.BankSlip;
import models.pricing.BankSlip.BankSlipBuilder;
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
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class BrasilBreedsCrawler extends Crawler {

   private static final String HOME_PAGE = "www.breeds.com.br";
   private static final String SELLER_FULL_NAME = "Breeds";
   protected Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),
      Card.AURA.toString(), Card.DINERS.toString(), Card.HIPER.toString(), Card.AMEX.toString());


   public BrasilBreedsCrawler(Session session) {
      super(session);
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      if (isProductPage(doc)) {
         if (doc.selectFirst(".price-box-content .price-box .regular-price .price") != null) {
            Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

            String internalId = CrawlerUtils.scrapStringSimpleInfo(doc, "tr .data", false);
            String internalPid = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc,".installment-options.modal.fade", "data-id");
            String name = CrawlerUtils.scrapStringSimpleInfo(doc, ".product-name h1", true);
            CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".breadcrumbs ul li a", true);
            String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, ".easyzoom a img", Arrays.asList("src"), "https:", HOME_PAGE);
            String secondaryImages = scrapSecondaryImages(doc, primaryImage);
            String description = CrawlerUtils.scrapSimpleDescription(doc, Arrays.asList(".box-collateral.box-description", ".title", "std"));
            boolean available = doc.selectFirst("#btn-addtocart") != null;
            String ean = CrawlerUtils.scrapStringSimpleInfo(doc, "#product-attribute-specs-table .data", false);
            Offers offers = available ? scrapOffers(doc, true) : new Offers();
            List<String> eans = new ArrayList<>();
            eans.add(ean);

            // main product
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
               .setOffers(offers)
               .setEans(eans)
               .build();

            products.add(product);
         } else {

            for (Element element : doc.select(".row-product-grouped")) {
               String name = element.selectFirst(".name-wrapper").text();
               String primaryImage = element.selectFirst("img").attr("src");
               Offers offers = scrapOffers(element, false);
               String internalTxt = element.selectFirst(".qty-label").attr("for");
               String[] strings = internalTxt.split("_");
               String internalId = strings[strings.length - 1];

               Product product = ProductBuilder.create()
                  .setUrl(session.getOriginalURL())
                  .setInternalId(internalId)
                  .setName(name)
                  .setPrimaryImage(primaryImage)
                  .setOffers(offers)
                  .build();
               products.add(product);
            }
         }
      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }

   private boolean isProductPage(Document doc) {
      return doc.selectFirst(".product") != null;
   }

   private String scrapSecondaryImages(Document doc, String primaryImage) {
      String secondaryImages = null;
      JSONArray secondaryImagesArray = new JSONArray();

      Elements images = doc.select(".images > ul > li:not([style=\"display:none\"])");
      for (int i = 1; i <= images.size(); i++) {
         secondaryImagesArray.put(primaryImage.replace("/Ampliada/", "/Ampliada" + i + "/"));
      }

      if (secondaryImagesArray.length() > 0) {
         secondaryImages = secondaryImagesArray.toString();
      }

      return secondaryImages;
   }


   private Offers scrapOffers(Element doc, boolean main) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(doc, main);
      List<String> sales = new ArrayList<>();

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

   private Pricing scrapPricing(Element doc, boolean main) throws MalformedPricingException {
      Double spotlightPrice;
      if (main) {
         spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".price-box-content .price-box .regular-price .price", null, false, ',', session);
      } else {
         spotlightPrice = MathUtils.parseDoubleWithComma(doc.selectFirst(".regular-price").text());
      }

      CreditCards creditCards = scrapCreditCards(doc, spotlightPrice);
      BankSlip bankSlip = BankSlipBuilder.create()
         .setFinalPrice(spotlightPrice)
         .build();


      return PricingBuilder.create()
         .setPriceFrom(null)
         .setSpotlightPrice(spotlightPrice)
         .setCreditCards(creditCards)
         .setBankSlip(bankSlip)
         .build();

   }

   private CreditCards scrapCreditCards(Element doc, Double spotligthPrice) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();

      Installments installments = scrapInstallments(doc);
      if (installments.getInstallments().isEmpty()) {
         installments.add(InstallmentBuilder.create()
            .setInstallmentNumber(1)
            .setInstallmentPrice(spotligthPrice)
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


   public Installments scrapInstallments(Element doc) throws MalformedPricingException {
      Installments installments = new Installments();

      Integer installment;
      Double value;

      Elements elements = doc.select(".modal-body .installment-option");

      for (Element e : elements) {

         String installmentString = e != null ? e.attr("data-installment").replaceAll("[^0-9]", "").trim() : null;
         installment = installmentString != null ? Integer.parseInt(installmentString) : null;

         String valueString = e != null ? e.selectFirst(".installment-value").text() : null;
         value = valueString != null ? MathUtils.parseDoubleWithComma(valueString) : null;

         installments.add(InstallmentBuilder.create()
            .setInstallmentNumber(installment)
            .setInstallmentPrice(value)
            .build());
      }

      return installments;
   }
}
