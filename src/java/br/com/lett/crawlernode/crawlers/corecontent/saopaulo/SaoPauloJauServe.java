package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.methods.ApacheDataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.JsoupDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.Parser;
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
import org.apache.http.HttpHeaders;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.util.*;

public class SaoPauloJauServe extends Crawler {
   public SaoPauloJauServe(Session session) {
      super(session);
      super.config.setParser(Parser.HTML);
   }

   private static final String SELLER_FULL_NAME = "jauserve";

   @Override
   protected Response fetchResponse() {
      Map<String, String> headers = new HashMap<>();
      headers.put(HttpHeaders.ACCEPT, "*/*");
      BasicClientCookie cookie = new BasicClientCookie("dw_shippostalcode","17206-220");
      cookie.setDomain("www.jauserve.com.br");
      cookie.setPath("/");
      this.cookies.add(cookie);

      Request request = Request.RequestBuilder.create()
         .setUrl(session.getOriginalURL())
         .setProxyservice(Arrays.asList(
            ProxyCollection.SMART_PROXY_BR,
            ProxyCollection.SMART_PROXY_BR_HAPROXY
         ))
         .setCookies(this.cookies)
         .setHeaders(headers)
         .build();
      return CrawlerUtils.retryRequestWithListDataFetcher(request, List.of(new ApacheDataFetcher(), new JsoupDataFetcher()), session, "get");
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();
      // Document d2 = Jsoup.parse(this.fetchResponse().getBody());
      String internalPid = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, ".container.product-detail", "data-pid");
      String name = CrawlerUtils.scrapStringSimpleInfo(doc, ".product-name", true);
      String primaryImage = getImage(doc);
      List<String> categories = CrawlerUtils.crawlCategories(doc,".breadcrumb-item a");
      String description  = CrawlerUtils.scrapSimpleDescription(doc,List.of(".text-muted"));
      boolean isAvailable = getAvailabity(doc);
      Offers offers = isAvailable ? scrapOffer(doc) : new Offers();

      Product product = ProductBuilder.create()
         .setUrl(session.getOriginalURL())
         .setInternalId(internalPid)
         .setInternalPid(internalPid)
         .setName(name)
         .setPrimaryImage(primaryImage)
         .setCategories(categories)
         .setDescription(description)
         .setOffers(offers)
         .build();

      products.add(product);

      return products;

   }
   private boolean getAvailabity(Document doc) {
      return doc.selectFirst(".btn-out-of-stock.add-to-cart-disabled") == null;
   }
   private String getImage(Document doc) {
      String imagePath = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, ".slide-img", "src");
      if (imagePath != null && !imagePath.isEmpty()) {
         return imagePath.substring(0, imagePath.indexOf('?'));
      }
      return null;
   }

   private Offers scrapOffer(Document doc) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(doc);

      offers.add(Offer.OfferBuilder.create()
         .setUseSlugNameAsInternalSellerId(true)
         .setSellerFullName(SELLER_FULL_NAME)
         .setMainPagePosition(1)
         .setIsBuybox(false)
         .setIsMainRetailer(true)
         .setPricing(pricing)
         .build());

      return offers;

   }

   private Pricing scrapPricing(Document doc) throws MalformedPricingException {
      Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".sales .value", "content", false, '.', session);
      Double priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".strike-through .value", "content", false, '.', session);
      if(spotlightPrice == null) {
         spotlightPrice = priceFrom;
         priceFrom = null;
      }

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
      Set<String> cards = Sets.newHashSet(Card.MASTERCARD.toString(), Card.VISA.toString(),
         Card.HIPERCARD.toString(), Card.ELO.toString(), Card.AMEX.toString());
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
