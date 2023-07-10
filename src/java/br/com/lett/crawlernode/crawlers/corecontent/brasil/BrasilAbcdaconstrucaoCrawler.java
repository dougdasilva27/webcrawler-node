package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.*;
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
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;

/**
 * Date: 23/10/2019
 *
 * @author Gabriel Dornelas
 */
public class BrasilAbcdaconstrucaoCrawler extends Crawler {

   private static final String HOME_PAGE = "https://www.abcdaconstrucao.com.br/";

   public BrasilAbcdaconstrucaoCrawler(Session session) {
      super(session);
      super.config.setParser(Parser.HTML);
   }

   @Override
   public boolean shouldVisit() {
      String href = session.getOriginalURL().toLowerCase();
      return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
   }

   @Override
   protected Response fetchResponse() {
      try {
         HttpClient client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();
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
         throw new RuntimeException("Failed in load document: " + session.getOriginalURL(), e);
      }
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      if (isProductPage(doc)) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "#product-variant-id", "value");
         String internalPid = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "#product-id", "value");
         String name = CrawlerUtils.scrapStringSimpleInfo(doc, ".product__title", false);
         CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".breadcrumb__link");
         String primaryImage = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, ".gallery-slider.swiper > div >.swiper-slide > img", "src");
         List<String> secondaryImages = CrawlerUtils.scrapSecondaryImages(doc, ".gallery-slider.swiper > div >.swiper-slide > img", Arrays.asList("src"), "https", "abcdaconstrucao.fbitsstatic.net", primaryImage);
         String description = CrawlerUtils.scrapStringSimpleInfo(doc, "#tblDescricao", false);
         boolean available = doc.selectFirst(".product__buttons > .buy-button") != null;
         Offers offers = available ? scrapOffer(doc) : new Offers();

         // Creating the product
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

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }

   private boolean isProductPage(Document doc) {
      return doc.selectFirst(".product__view") != null;
   }

   private Offers scrapOffer(Document doc) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(doc);
      List<String> sales = Collections.singletonList(CrawlerUtils.calculateSales(pricing));

      offers.add(Offer.OfferBuilder.create()
         .setUseSlugNameAsInternalSellerId(true)
         .setSellerFullName("abc da construcao")
         .setMainPagePosition(1)
         .setIsBuybox(false)
         .setIsMainRetailer(true)
         .setPricing(pricing)
         .setSales(sales)
         .build());

      return offers;

   }

   private Pricing scrapPricing(Document doc) throws MalformedPricingException {
      Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, "#precoCalculado", null, false, ',', session);
      Double priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".product__prices > div > p > s", null, false, ',', session);

      if (spotlightPrice == null) {
         spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, "#product-prices-div > div.spot-price > h3 > span", null, false, ',', session);
      }

      if (doc.selectFirst(".box-price") != null) {
         priceFrom = null;
      }

      BankSlip bankSlip = CrawlerUtils.setBankSlipOffers(spotlightPrice, null);
      CreditCards creditCards = scrapCreditCards(doc, spotlightPrice);

      return Pricing.PricingBuilder.create()
         .setSpotlightPrice(spotlightPrice)
         .setPriceFrom(priceFrom)
         .setBankSlip(bankSlip)
         .setCreditCards(creditCards)
         .build();

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

      Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),
         Card.AURA.toString(), Card.DINERS.toString(), Card.HIPER.toString(), Card.AMEX.toString());

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

      Elements installmentsCard = doc.select("#product-prices-div > .best-installment");

      for (Element e : installmentsCard) {

         if (e != null) {

            String installmentString = e.text();

            Integer installment = installmentString.contains("x") ? MathUtils.parseInt(installmentString.split("x")[0]) : null;

            //4 x sem juros de R$ 29,49 no Cartão
            String valueString = installmentString.contains("R$") ? installmentString.split("R")[1].replace("$ ", "") : null;
            String valueString2 = valueString != null && valueString.contains(" ") ? valueString.split(" ")[0] : null;

            double installmentValue = valueString2 != null ? MathUtils.parseDoubleWithComma(valueString2) : null;

            installments.add(Installment.InstallmentBuilder.create()
               .setInstallmentNumber(installment)
               .setInstallmentPrice(installmentValue)
               .build());
         }
      }

      return installments;
   }

}
