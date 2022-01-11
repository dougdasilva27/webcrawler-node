package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.fetcher.DynamicDataFetcher;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.*;
import com.google.common.collect.Sets;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.Offer;
import models.Offers;
import models.RatingsReviews;
import models.pricing.*;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BrasilAnhangueraFerramentas extends Crawler {

   public BrasilAnhangueraFerramentas(Session session) {
      super(session);
   }

   private static final String SELLER_FULL_NAME = "Anhanguera Ferramentas";
   protected Set<String> cards = Sets.newHashSet(Card.ELO.toString(), Card.VISA.toString(), Card.MASTERCARD.toString());

   @Override
   public boolean shouldVisit() {
      String href = session.getOriginalURL().toLowerCase();
      String homePage = "https://www.anhangueraferramentas.com.br";
      return !FILTERS.matcher(href).matches() && (href.startsWith(homePage));
   }

   private String getButtonVariation(String el) {
      String total = null;
      Pattern pattern = Pattern.compile("=(.*) data");
      Matcher matcher = pattern.matcher(el);
      if (matcher.find()) {
         total = matcher.group(1);
      }
      return total;
   }


   private Document fetchDocumentVariation(Element variation) {
      Document doc = null;
      try {
         webdriver = DynamicDataFetcher.fetchPageWebdriver(session.getOriginalURL(), ProxyCollection.BUY_HAPROXY, session);
         webdriver.waitForElement(".page-produto", 30);
         String volts = getButtonVariation(variation.toString());
         String button = "[data-valoratributo=" + volts + "]";

         WebElement variationButton = webdriver.driver.findElement(By.cssSelector(button));
         webdriver.clickOnElementViaJavascript(variationButton);
         webdriver.waitLoad(5000);

         doc = Jsoup.parse(webdriver.getCurrentPageSource());

      } catch (Exception e) {
         Logging.printLogInfo(logger, session, CommonMethods.getStackTrace(e));
      }

      return doc;

   }


   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      if (isProductPage(doc)) {
         Logging.printLogInfo(
            logger, session, "Product page identified: " + session.getOriginalURL());
         RatingsReviews ratingsReviews = scrapRating(doc);
         Elements variations = doc.select(".prodVariante .valorAtributo");

         if (!variations.isEmpty()) {
            for (Element el : variations) {
               products.add(extractProductFromHtml(fetchDocumentVariation(el), ratingsReviews));
            }
         } else {
            products.add(extractProductFromHtml(doc, ratingsReviews));
         }

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + session.getOriginalURL());
      }

      return products;
   }

   private Product extractProductFromHtml(Document doc, RatingsReviews ratingsReviews) throws OfferException, MalformedPricingException, MalformedProductException {
      String internalId = CrawlerUtils.scrapStringSimpleInfo(doc, ".fbits-sku", true);
      String internalPid = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "#hdnProdutoId", "value");
      String name = CrawlerUtils.scrapStringSimpleInfo(doc, ".produto-detalhe-content h1", true);
      CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".order-last .fbits-breadcrumb li", true);
      String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, "#zoomImagemProduto", Collections.singletonList("data-zoom-image"), "https", "anhangueraferramentas.fbitsstatic.net");
      List<String> secondaryImages = CrawlerUtils.scrapSecondaryImages(doc, ".elevatezoom-gallery", Collections.singletonList("data-zoom-image"), "https", "imgprd.martins.com.br", primaryImage);
      String description = CrawlerUtils.scrapSimpleDescription(doc, Arrays.asList(".infoProd"));

      boolean available = !doc.select("#msgEstoqueDisponivel span").isEmpty();
      Offers offers = available ? scrapOffers(doc) : new Offers();

      // Creating the product
      return ProductBuilder.create()
         .setUrl(session.getOriginalURL())
         .setInternalId(internalId)
         .setInternalPid(internalPid)
         .setName(name)
         .setCategories(categories)
         .setPrimaryImage(primaryImage)
         .setRatingReviews(ratingsReviews)
         .setSecondaryImages(secondaryImages)
         .setOffers(offers)
         .setDescription(description)
         .build();

   }


   private RatingsReviews scrapRating(Document doc) {
      RatingsReviews ratingsReviews = new RatingsReviews();
      JSONObject jsonObject = CrawlerUtils.selectJsonFromHtml(doc, ".center script[type='application/ld+json']", null, "", false, false);
      if (jsonObject != null) {
         JSONObject jsonRating = jsonObject.optJSONObject("aggregateRating");
         if (jsonRating != null) {

            Double avgRating = jsonRating.optDouble("ratingValue");
            int totalReviews = jsonRating.optInt("reviewCount");
            ratingsReviews.setTotalRating(totalReviews);
            ratingsReviews.setTotalWrittenReviews(totalReviews);
            ratingsReviews.setAverageOverallRating(avgRating);
         }
      }
      return ratingsReviews;
   }

   private boolean isProductPage(Document doc) {
      return !doc.select(".page-produto").isEmpty();
   }

   private Offers scrapOffers(Document doc) throws OfferException, MalformedPricingException {
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

   private List<String> scrapSales(Document doc) {
      String sale = CrawlerUtils.scrapStringSimpleInfo(doc, ".descontoBoleto-anh", true);
      List<String> sales = new ArrayList<>();

      if (sale != null) {
         sales.add(sale);
      }

      return sales;
   }

   private Pricing scrapPricing(Document doc) throws MalformedPricingException {
      Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".fbits-preco .precoPor", null, true, ',', session);
      Double priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".fbits-preco .precoDe", null, true, ',', session);
      CreditCards creditCards = scrapCreditCards(doc, spotlightPrice);
      BankSlip bankSlip = BankSlip.BankSlipBuilder.create()
         .setFinalPrice(CrawlerUtils.scrapDoublePriceFromHtml(doc, ".fbits-boleto-preco", null, true, ',', session))
         .build();

      return Pricing.PricingBuilder.create()
         .setSpotlightPrice(spotlightPrice)
         .setPriceFrom(priceFrom)
         .setBankSlip(bankSlip)
         .setCreditCards(creditCards)
         .build();
   }

   private CreditCards scrapCreditCards(Document doc, Double spotlightPrice) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();

      Installments installments = new Installments();
      installments.add(Installment.InstallmentBuilder.create()
         .setInstallmentNumber(1)
         .setInstallmentPrice(spotlightPrice)
         .build());

      Pair<Integer, Float> pair = CrawlerUtils.crawlSimpleInstallment("#formasPagamento span", doc, false, "x de ");
      if (!pair.isAnyValueNull()) {
         installments.add(Installment.InstallmentBuilder.create()
            .setInstallmentNumber(pair.getFirst())
            .setInstallmentPrice(MathUtils.normalizeTwoDecimalPlaces(pair.getSecond().doubleValue()))
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
}
