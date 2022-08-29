package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

import br.com.lett.crawlernode.core.fetcher.DynamicDataFetcher;
import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.methods.ApacheDataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.FetcherDataFetcher;
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
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.net.URLDecoder;
import java.util.*;

public class SaopauloNdaysCrawler extends Crawler {

   private static final String HOME_PAGE = "www.ndays.com.br";
   private static final String SELLER_FULL_NAME = "Ndays";

   public SaopauloNdaysCrawler(Session session) {
      super(session);
      config.setFetcher(FetchMode.FETCHER);
      config.setParser(Parser.HTML);
   }
   @Override
   protected Response fetchResponse() {
      Request request = Request.RequestBuilder.create()
         .setUrl(session.getOriginalURL())
         .setProxyservice(Arrays.asList(ProxyCollection.BUY, ProxyCollection.NETNUT_RESIDENTIAL_BR))
         .setSendUserAgent(false)
         .build();
      return CrawlerUtils.retryRequestWithListDataFetcher(request, List.of(new ApacheDataFetcher(), new JsoupDataFetcher(), new FetcherDataFetcher()), session, "get");

   }

   protected Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),
      Card.AURA.toString(), Card.DINERS.toString(), Card.HIPER.toString(), Card.AMEX.toString(),
      Card.ELO.toString(), Card.JCB.toString(), Card.DISCOVER.toString());

   public List<Product> extractInformation(Document doc) throws Exception {

      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      if (isProductPage(doc)) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String name = CrawlerUtils.scrapStringSimpleInfo(doc, "#product .modal-content .product-title", false);
         String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "#product .modal-content input[name=\"product_id\"]", "value");

         CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".modal-content ul.breadcrumb li", true);
         String image = CrawlerUtils.scrapSimplePrimaryImage(doc, "#product .modal-content .modal-body .det-prod-img img", Arrays.asList("src"), "https", HOME_PAGE);
         String primaryImage = image != null ? URLDecoder.decode(image, "utf-8") : null;
         boolean available = doc.select("#product .modal-content .modal-body #button-cart").first() != null;
         String description = CrawlerUtils.scrapStringSimpleInfo(doc, ".modal-body .det-prod-selo", false);
         Integer stock = CrawlerUtils.scrapIntegerFromHtml(doc, ".modal-body .det-prod-fields-form p span", false, 0);
         Offers offers = available ? scrapOffers(doc) : new Offers();

         // Creating the product
         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setName(name)
            .setCategories(categories)
            .setPrimaryImage(primaryImage)
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
      return doc.selectFirst("#product") != null;
   }

   private String getSeller(Document doc) {
      return CrawlerUtils.scrapStringSimpleInfo(doc, ".modal-body .linha-vendido .list-lojas-cont h2", false);
   }

   private Offers scrapOffers(Document doc) throws MalformedPricingException, OfferException {

      Offers offers = new Offers();
      Pricing pricing = scrapPricing(doc);
      List<String> sales = scrapSales(doc);

      offers.add(Offer.OfferBuilder.create()
         .setUseSlugNameAsInternalSellerId(true)
         .setSellerFullName(getSeller(doc))
         .setMainPagePosition(1)
         .setIsBuybox(false)
         .setIsMainRetailer(SELLER_FULL_NAME.equalsIgnoreCase(getSeller(doc)))
         .setPricing(pricing)
         .setSales(sales)
         .build());

      return offers;
   }

   private List<String> scrapSales(Document doc) {
      List<String> sales = new ArrayList<>();
      String sale = CrawlerUtils.scrapStringSimpleInfo(doc, ".product-econ strong", true);
      if (sale != null) {
         sales.add(sale);
      }

      return sales;
   }

   private Pricing scrapPricing(Document doc) throws MalformedPricingException {

      Double spotLightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".modal-body .det-prod-price span.det-prod-price-num", null, false, ',', session);
      Double priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".modal-body .det-prod-price span.price2", null, false, ',', session);

      BankSlip bankSlip = CrawlerUtils.setBankSlipOffers(spotLightPrice, null);
      CreditCards creditCards = scrapCreditCards(spotLightPrice);

      return Pricing.PricingBuilder.create()
         .setSpotlightPrice(spotLightPrice)
         .setPriceFrom(priceFrom)
         .setBankSlip(bankSlip)
         .setCreditCards(creditCards)
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
}
