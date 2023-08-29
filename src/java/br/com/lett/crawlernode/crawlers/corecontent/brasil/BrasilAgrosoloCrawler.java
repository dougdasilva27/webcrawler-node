package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.Parser;
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
import models.Offer;
import models.Offers;
import models.pricing.*;
import models.pricing.BankSlip.BankSlipBuilder;
import models.pricing.Pricing.PricingBuilder;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BrasilAgrosoloCrawler extends Crawler {

   private static final String HOME_PAGE = "www.agrosolo.com.br/";
   private final Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),
      Card.AURA.toString(), Card.DINERS.toString(), Card.HIPER.toString(), Card.AMEX.toString());

   public BrasilAgrosoloCrawler(Session session) {
      super(session);
      config.setParser(Parser.HTML);
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      if (isProductPage(doc)) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());
         Elements variations = doc.select(".product__infos--unique--wrapper .product__variants--item");

         for (Element element : variations) {
            String internalId = CrawlerUtils.scrapStringSimpleInfo(doc, ".product__sku span", true);
            String internalPid = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "#product-id", "value");
            String name = CrawlerUtils.scrapStringSimpleInfo(doc, ".product__title", true);
            String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, ".product__gallery--main img", Arrays.asList("src"), "https:", HOME_PAGE);
            List<String> images = CrawlerUtils.scrapSecondaryImages(doc, ".product__gallery--main img", Arrays.asList("src"), "https:", HOME_PAGE, primaryImage);
            String description = CrawlerUtils.scrapSimpleDescription(doc, Arrays.asList(".product__information .container", "std"));

            boolean isAvailable = checkIfIsAvailable(doc);
            Offers offers = isAvailable ? scrapOffers(doc) : new Offers();

            if (variations.size() > 1) {
               String variationName = element.select("label").text();
               Document document = requestFromVariations(internalPid, variationName);
               name = name + " - " + variationName;
               primaryImage = CrawlerUtils.scrapSimplePrimaryImage(document, ".product__gallery--main img", Arrays.asList("src"), "https:", HOME_PAGE);
               internalId = getInternalIdVariation(primaryImage);
               images = CrawlerUtils.scrapSecondaryImages(document, ".product__gallery--main img", Arrays.asList("src"), "https:", HOME_PAGE, primaryImage);
               isAvailable = checkIfIsAvailable(document);
               offers = isAvailable ? scrapOffers(document) : new Offers();
            }

            Product product = ProductBuilder.create()
               .setUrl(session.getOriginalURL())
               .setInternalId(internalId)
               .setInternalPid(internalPid)
               .setName(name)
               .setPrimaryImage(primaryImage)
               .setOffers(offers)
               .setSecondaryImages(images)
               .setDescription(description)
               .build();
            products.add(product);
         }
      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }
      return products;
   }

   private Document requestFromVariations(String internalPid, String variationName) {
      Map<String, String> headers = new HashMap<>();
      headers.put("authority", "www.agrosolo.com.br");
      headers.put("accept", "*/*");
      headers.put("content-type", "application/json");

      Request request = Request.RequestBuilder.create()
         .setUrl("https://www.agrosolo.com.br/snippet")
         .setHeaders(headers)
         .setPayload("{\"fileName\":\"product_view_snippet.html\",\"queryName\":\"product.graphql\",\"variables\":{\"productId\":" + internalPid + ",\"selections\":[{\"attributeId\":258,\"value\":\"" + variationName + "\"}]}}")
         .build();

      Response response = dataFetcher.post(session, request);
      Document document = Jsoup.parse(response.getBody());

      return document;
   }

   private boolean checkIfIsAvailable(Document document) {
      return document.selectFirst("button.add-to-cart-button") != null && document.selectFirst("#product-prices-div div.product__price--after") != null;
   }

   private boolean isProductPage(Document doc) {
      return doc.selectFirst("div.product") != null;
   }

   private String getInternalIdVariation(String imageUrl) {
      Pattern pattern = Pattern.compile("/([^/]+)\\.jpg");
      Matcher matcher = pattern.matcher(imageUrl);

      if (matcher.find()) {
         return matcher.group(1);
      }
      return null;
   }

   private Offers scrapOffers(Document doc) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(doc);
      List<String> sales = new ArrayList<>();
      String calculateSales = CrawlerUtils.calculateSales(pricing);
      sales.add(calculateSales);

      offers.add(Offer.OfferBuilder.create()
         .setMainPagePosition(1)
         .setIsBuybox(false)
         .setPricing(pricing)
         .setSales(sales)
         .setSellerFullName("Agrosolo Brasil")
         .setIsMainRetailer(true)
         .setUseSlugNameAsInternalSellerId(true)
         .build());

      return offers;
   }

   private Pricing scrapPricing(Document doc) throws MalformedPricingException {
      Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, "#product-prices-div div.product__price--after", null, true, ',', session);
      Double priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(doc, "#product-prices-div div.product__price--before", null, true, ',', session);

      CreditCards creditCards = scrapCreditCards(doc, spotlightPrice);
      BankSlip bankSlip = BankSlipBuilder.create()
         .setFinalPrice(spotlightPrice)
         .build();

      return PricingBuilder.create()
         .setSpotlightPrice(spotlightPrice)
         .setPriceFrom(priceFrom)
         .setBankSlip(bankSlip)
         .setCreditCards(creditCards)
         .build();
   }

   private CreditCards scrapCreditCards(Document doc, Double spotlightPrice) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();

      Installments installments = scrapInstallments(doc, spotlightPrice);

      for (String card : cards) {
         creditCards.add(CreditCard.CreditCardBuilder.create()
            .setBrand(card)
            .setInstallments(installments)
            .setIsShopCard(false)
            .build());
      }

      return creditCards;
   }

   private Installments scrapInstallments(Document doc, Double spotlightPrice) throws MalformedPricingException {
      Installments installments = new Installments();

      Elements installmentsEl = doc.select("div.details-content > p");

      if (!installmentsEl.isEmpty()) {
         for (Element el : installmentsEl) {
            Elements results = el.select("b");

            if (results.size() == 2) {
               int installmentNumber = Integer.parseInt(results.get(0).html());
               Double installmentPrice = MathUtils.parseDoubleWithComma(results.get(1).html());

               installments.add(Installment.InstallmentBuilder.create()
                  .setInstallmentNumber(installmentNumber)
                  .setInstallmentPrice(installmentPrice)
                  .build());
            }
         }
      } else {
         installments.add(Installment.InstallmentBuilder.create()
            .setInstallmentNumber(1)
            .setInstallmentPrice(spotlightPrice)
            .build());
      }

      return installments;
   }
}
