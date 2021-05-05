package br.com.lett.crawlernode.crawlers.corecontent.brasil;


import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
import com.google.common.collect.Sets;
import exceptions.MalformedPricingException;
import exceptions.OfferException;

import java.util.*;

import models.Offer;
import models.Offers;
import models.pricing.BankSlip;
import models.pricing.CreditCard;
import models.pricing.CreditCards;
import models.pricing.Installment;
import models.pricing.Installments;
import models.pricing.Pricing;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 * date: 05/09/2018
 *
 * @author gabriel
 * @author gabriel
 */

//this website does not have more ratings.

public class BrasilSephoraCrawler extends Crawler {

   private static final String HOME_PAGE = "https://www.sephora.com.br/";
   private static final String MAIN_SELLER_NAME_LOWER = "sephora";

   public BrasilSephoraCrawler(Session session) {
      super(session);
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
         String internalPid = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, ".product-number.show-for-medium>span", "data-masterid");
         String description = CrawlerUtils.scrapStringSimpleInfo(doc, ".tabs-panel.is-active", false);
         CategoryCollection categories = CrawlerUtils.crawlCategories(doc, "div.breadcrumb-element", true);

         Elements variants = doc.select(".product-detail .product-variations .no-bullet .variation-content--list");
         for (Element variant : variants) {
            String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(variant, "meta[itemprop=sku]", "content");

            Document variantProductPage = fetchVariantProductPage(internalId);
            String name = scrapName(variantProductPage);
            List<String> secondaryImages = crawlImages(variantProductPage);
            String primaryImage = secondaryImages.isEmpty() ? null : secondaryImages.remove(0);

            boolean isAvailable = variantProductPage.select(".not-available-msg").isEmpty();
            Offers offers = isAvailable ? scrapOffers(variantProductPage) : new Offers();

            Product product = ProductBuilder.create()
               .setUrl(session.getOriginalURL())
               .setInternalId(internalId)
               .setInternalPid(internalPid)
               .setName(name)
               .setCategories(categories)
               .setPrimaryImage(primaryImage)
               .setSecondaryImages(secondaryImages)
               .setDescription(description)
               .setOffers(offers)
               .build();

            products.add(product);
         }
      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }

   private boolean isProductPage(Document document) {
      return document.selectFirst(".product-cart") != null;
   }

   private String scrapName(Document doc) {
      return CrawlerUtils.scrapStringSimpleInfo(doc, ".product-name-small-wrapper", false)
         + " - "
         + CrawlerUtils.scrapStringSimpleInfo(doc, "span.selected-value-name", false);
   }

   private Offers scrapOffers(Document doc) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();

      Pricing pricing = scrapPricing(doc);
      List<String> sales = scrapSales(pricing);

      offers.add(Offer.OfferBuilder.create()
         .setUseSlugNameAsInternalSellerId(true)
         .setSellerFullName(MAIN_SELLER_NAME_LOWER)
         .setMainPagePosition(1)
         .setIsBuybox(false)
         .setIsMainRetailer(true)
         .setPricing(pricing)
         .setSales(sales)
         .build());
      return offers;
   }

   private Pricing scrapPricing(Document doc) throws MalformedPricingException {
      Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".price-sales.price-sales-standard span:first-child", null, false, ',', session);
      Double priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(doc, "span.price-standard", null, false, ',', session);

      CreditCards creditCards = scrapCreditCards(doc, spotlightPrice);

      BankSlip bankSlip = BankSlip.BankSlipBuilder.create()
         .setFinalPrice(spotlightPrice)
         .build();

      return Pricing.PricingBuilder.create()
         .setPriceFrom(priceFrom)
         .setSpotlightPrice(spotlightPrice)
         .setCreditCards(creditCards)
         .setBankSlip(bankSlip)
         .build();
   }

   private List<String> scrapSales(Pricing pricing) {
      List<String> sales = new ArrayList<>();

      String saleDiscount = CrawlerUtils.calculateSales(pricing);
      if (saleDiscount != null) {
         sales.add(saleDiscount);
      }

      return sales;
   }

   private CreditCards scrapCreditCards(Document doc, Double spotlightPrice) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();

      Set<Card> cards = Sets.newHashSet(
         Card.VISA,
         Card.MASTERCARD,
         Card.AURA,
         Card.DINERS,
         Card.HIPER,
         Card.AMEX
      );

      Installments installments = scrapInstallments(doc);

      if (installments.getInstallments().isEmpty()) {

         installments.add(Installment.InstallmentBuilder.create()
            .setInstallmentNumber(1)
            .setInstallmentPrice(spotlightPrice)
            .build());

      }
         for (Card card : cards) {
            creditCards.add(CreditCard.CreditCardBuilder.create()
               .setBrand(card.toString())
               .setInstallments(installments)
               .setIsShopCard(false)
               .build());
         }

      return creditCards;
   }

   private Installments scrapInstallments(Document doc) throws MalformedPricingException {
      Installments installments = new Installments();

      String[] pairInstallment = null;
      Element installmentElem = doc.selectFirst(".installments.installments-pdp");
      if (installmentElem != null) {
         String text = installmentElem.text();
         if (text != null) {
            pairInstallment = text.split("\\sde\\s");
         }
      }

      if (Objects.nonNull(pairInstallment) && pairInstallment.length >= 2) {
         installments.add(
            Installment.InstallmentBuilder.create()
               .setInstallmentNumber(MathUtils.parseInt(pairInstallment[0]))
               .setInstallmentPrice(MathUtils.parseDoubleWithDot(pairInstallment[1]))
               .build()
         );
      }

      return installments;
   }

   private List<String> crawlImages(Document productPage) {
      List<String> secondaryImagesArray = new ArrayList<>();

      Elements imagesList = productPage.select("div.show-for-small-only.text-center ul li.thumb>a");

      if (!imagesList.isEmpty()) {
         for (Element imageElement : imagesList) {
            secondaryImagesArray.add(CrawlerUtils.scrapStringSimpleInfoByAttribute(imageElement, "a", "href"));
         }
      }

      return secondaryImagesArray;
   }

   private Document fetchVariantProductPage(String internalPid) {
      String url = "https://www.sephora.com.br/on/demandware.store/Sites-Sephora_BR-Site/pt_BR/Product-Variation?pid=" + internalPid + "&format=ajax";

      Request request = Request.RequestBuilder.create().setUrl(url).build();
      String response = this.dataFetcher.get(session, request).getBody();

      return Jsoup.parse(response);
   }
}
