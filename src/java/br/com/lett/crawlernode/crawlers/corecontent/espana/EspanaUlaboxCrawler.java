package br.com.lett.crawlernode.crawlers.corecontent.espana;

import br.com.lett.crawlernode.core.fetcher.DynamicDataFetcher;
import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import com.google.common.collect.Sets;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.Offer;
import models.Offers;
import models.RatingsReviews;
import models.pricing.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EspanaUlaboxCrawler extends Crawler {

   private static final String SELLER_FULL_NAME = "Ulabox";
   protected Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),
      Card.AURA.toString(), Card.DINERS.toString(), Card.HIPER.toString(), Card.AMEX.toString());

   public EspanaUlaboxCrawler(Session session) {
      super(session);
   }

   protected Object fetch() {
      Request request = Request.RequestBuilder.create()
         .setCookies(cookies)
         .setUrl(session.getOriginalURL())
         .setProxyservice(List.of(
            ProxyCollection.BUY_HAPROXY,
            ProxyCollection.NETNUT_RESIDENTIAL_ES_HAPROXY,
            ProxyCollection.NETNUT_RESIDENTIAL_ES
         ))
         .build();

      return dataFetcher.get(session, request);
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      Element productElement = doc.selectFirst("div[itemtype*=Product]");

      if (productElement != null) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String productUrl = CrawlerUtils.scrapUrl(productElement, "meta[itemprop=\"url\"]", "content", "https", "www.ulabox.com");
         String internalId = scrapInternalId(productUrl);
         String name = CrawlerUtils.scrapStringSimpleInfo(productElement, "[itemprop=\"name\"]", false);
         boolean isAvailable = productElement.selectFirst("[itemprop=\"availability\"][href*=InStock]") != null;
         Offers offers = isAvailable ? scrapOffer(productElement) : null;

         // Creating the product

         Product product = ProductBuilder.create()
            .setUrl(productUrl)
            .setInternalId(internalId)
            .setInternalPid(internalId)
            .setName(name)
            .setOffers(offers)

            .build();

         products.add(product);

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;

   }

   private String scrapInternalId(String url) {
      final String regex = "(producto|product)\\/.*\\/(\\d+)";

      final Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
      final Matcher matcher = pattern.matcher(url);

      return matcher.find() ? matcher.group(2) : null;
   }

   private Offers scrapOffer(Element productElement) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(productElement);
      List<String> sales = new ArrayList<>();

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


   private Pricing scrapPricing(Element productElement) throws MalformedPricingException {
      Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(productElement, "[itemprop=\"price\"]", "content", false, '.', session);
      Double priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(productElement, ".MuiGrid-root.MuiGrid-item.MuiGrid-grid-xs-auto", null, false, ',', session);
      CreditCards creditCards = scrapCreditCards(spotlightPrice);

      return Pricing.PricingBuilder.create()
         .setSpotlightPrice(spotlightPrice)
         .setPriceFrom(priceFrom)
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
