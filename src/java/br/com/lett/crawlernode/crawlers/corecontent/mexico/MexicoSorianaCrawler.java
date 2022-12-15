package br.com.lett.crawlernode.crawlers.corecontent.mexico;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.methods.ApacheDataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.JsoupDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.*;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import com.google.common.collect.Sets;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.Offer;
import models.Offers;
import models.pricing.*;
import org.apache.http.HttpHeaders;
import org.jsoup.nodes.Document;

import java.util.*;

public class MexicoSorianaCrawler extends Crawler {

   private static final String SELLER_FULL_NAME = "soriana";
   protected Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),
      Card.AURA.toString(), Card.DINERS.toString(), Card.HIPER.toString(), Card.AMEX.toString());

   public MexicoSorianaCrawler(Session session) {
      super(session);
      super.config.setFetcher(FetchMode.APACHE);
      super.config.setParser(Parser.HTML);
   }

   @Override
   protected Response fetchResponse() {
      Map<String, String> headers = new HashMap<>();
      headers.put(HttpHeaders.ACCEPT, "*/*");

      Request request = Request.RequestBuilder.create()
         .setUrl(session.getOriginalURL())
         .setProxyservice(Arrays.asList(
            ProxyCollection.NETNUT_RESIDENTIAL_BR,
            ProxyCollection.SMART_PROXY_MX,
            ProxyCollection.SMART_PROXY_MX_HAPROXY,
            ProxyCollection.NETNUT_RESIDENTIAL_MX_HAPROXY
         ))
         .setHeaders(headers)
         .build();
      return CrawlerUtils.retryRequestWithListDataFetcher(request, List.of(new ApacheDataFetcher(), new JsoupDataFetcher()), session, "get");

   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      if (isProductPage(doc)) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, ".container.product-detail.product-wrapper", "data-pid");
         String internalPid = internalId;
         String name = CrawlerUtils.scrapStringSimpleInfo(doc, ".product-name", true);
         String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, ".carousel-item.active img", Collections.singletonList("src"), "https", "centralar.com.br");
         List<String> secondaryImages = CrawlerUtils.scrapSecondaryImages(doc, ".carousel-indicators li:not(:first-child) img", Collections.singletonList("src"), "https", "www.soriana.com", primaryImage);
         String description = CrawlerUtils.scrapSimpleDescription(doc, Arrays.asList(".description-and-detail"));
         CategoryCollection categoryCollection = CrawlerUtils.crawlCategories(doc, ".breadcrumb li:not(:last-child) a", true);
         boolean availableToBuy = doc.select(".cart-icon.d-none").isEmpty();
         Offers offers = availableToBuy ? scrapOffer(doc) : new Offers();

         // Creating the product
         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setInternalPid(internalPid)
            .setName(name)
            .setPrimaryImage(primaryImage)
            .setSecondaryImages(secondaryImages)
            .setCategories(categoryCollection)
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
      return !doc.select(".container.product-detail.product-wrapper").isEmpty();
   }

   private Offers scrapOffer(Document doc) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(doc);
      List<String> sales = scrapSales(pricing);

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

   private List<String> scrapSales(Pricing pricing) {
      List<String> sales = new ArrayList<>();

      String sale = CrawlerUtils.calculateSales(pricing);

      if (sale != null) {
         sales.add(sale);
      }

      return sales;
   }

   private Pricing scrapPricing(Document doc) throws MalformedPricingException {
      Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".sales .value", "content", true, '.', session);
      Double priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".strike-through.list span", "content", true, '.', session);
      CreditCards creditCards = scrapCreditCards(spotlightPrice);

      return Pricing.PricingBuilder.create()
         .setPriceFrom(priceFrom)
         .setSpotlightPrice(spotlightPrice)
         .setCreditCards(creditCards)
         .build();
   }


   private CreditCards scrapCreditCards(Double spotlightPrice) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();

      Installments installments = new Installments();
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
}
