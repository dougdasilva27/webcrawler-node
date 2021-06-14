package br.com.lett.crawlernode.crawlers.extractionutils.core;

import br.com.lett.crawlernode.core.fetcher.models.Request;
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
import org.apache.http.HttpHeaders;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.jsoup.nodes.Document;

import java.util.*;

public abstract class ComprebemCrawler extends Crawler {

   protected String HOME_PAGE = getHomePage();
   protected String MAIN_SELLER_NAME = getMainSellerName();
   protected String CEP = getCep();
   protected Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),
      Card.AURA.toString(), Card.DINERS.toString(), Card.HIPER.toString(), Card.AMEX.toString());

   protected abstract String getHomePage();

   protected abstract String getMainSellerName();

   protected abstract String getCep();

   public ComprebemCrawler(Session session) {
      super(session);
   }

   @Override
   public void handleCookiesBeforeFetch() {
      Map<String, String> headers = new HashMap<>();
      headers.put(HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded; charset=UTF-8");
      headers.put(HttpHeaders.ACCEPT, "*/*;q=0.5, text/javascript, application/javascript, application/ecmascript, application/x-ecmascript");
      headers.put(HttpHeaders.ACCEPT_ENCODING, "gzip, deflate, br");
      headers.put("origin", "https://delivery.comprebem.com.br");
      headers.put("x-requested-with", "XMLHttpRequest");
      headers.put("connection", "keep-alive");

      StringBuilder payload = new StringBuilder();
      payload.append("utf8=%E2%9C%93");
      payload.append("&_method=put");
      payload.append("&order%5Bshipping_mode%5D=delivery");
      payload.append("&order%5Bship_address_attributes%5D%5Btemporary%5D=true");
      payload.append("&order%5Bship_address_attributes%5D%5Bzipcode%5D=" + CEP);
      payload.append("&button=");

      Request request = Request.RequestBuilder.create()
         .setUrl("https://delivery.comprebem.com.br/current_stock")
         .setPayload(payload.toString())
         .setHeaders(headers)
         .setCookies(cookies)
         .mustSendContentEncoding(false)
         .build();
      List<Cookie> loadPageCookies = this.dataFetcher.post(session, request).getCookies();

      for (Cookie cookieResponse : loadPageCookies) {
         BasicClientCookie cookie = new BasicClientCookie(cookieResponse.getName(), cookieResponse.getValue());
         cookie.setDomain(HOME_PAGE);
         cookie.setPath("/");
         cookies.add(cookie);
      }
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      List<Product> products = new ArrayList<>();

      if (isProductPage(doc)) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "#variant_id", "value");
         String internalPid = internalId;
         String name = CrawlerUtils.scrapStringSimpleInfo(doc, ".product-name", true);
         String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, "#image_zoom", Collections.singletonList("src"), "https://", "s3-sa-east-1.amazonaws.com");
         CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".breadcrumb > li", true);
         String description = CrawlerUtils.scrapStringSimpleInfo(doc, ".description", true);
         boolean availability = doc.selectFirst(".btn-comprar") != null;
         Offers offers = availability ? scrapOffers(doc) : new Offers();
         RatingsReviews ratingsReviews = scrapRatings();

         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setInternalPid(internalPid)
            .setName(name)
            .setCategory1(categories.getCategory(0))
            .setCategory2(categories.getCategory(1))
            .setCategory3(categories.getCategory(2))
            .setPrimaryImage(primaryImage)
            .setDescription(description)
            .setOffers(offers)
            .setRatingReviews(ratingsReviews)
            .build();

         products.add(product);


      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }

   private boolean isProductPage(Document doc) {
      return doc.selectFirst(".product-details") != null;
   }

   private Offers scrapOffers(Document doc) throws OfferException, MalformedPricingException {
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
      Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".principal span:nth-child(4)", null, false, ',', this.session);
      Double priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".principal span:nth-child(2)", null, false, ',', this.session);

      if(spotlightPrice == null){
         spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".principal span:nth-child(2)", null, false, ',', this.session);
         priceFrom = null;
      }

      CreditCards creditCards = scrapCreditCards(spotlightPrice);
      return Pricing.PricingBuilder.create()
         .setSpotlightPrice(spotlightPrice)
         .setPriceFrom(priceFrom)
         .setCreditCards(creditCards)
         .setBankSlip(BankSlip.BankSlipBuilder.create().setFinalPrice(spotlightPrice).build())
         .build();
   }

   private CreditCards scrapCreditCards(Double spotlightPrice) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();
      Installments installments = new Installments();

      installments.add(Installment.InstallmentBuilder.create()
         .setInstallmentNumber(1)
         .setInstallmentPrice(spotlightPrice)
         .build());

      for (String brand : cards) {
         creditCards.add(CreditCard.CreditCardBuilder.create()
            .setBrand(brand)
            .setIsShopCard(false)
            .setInstallments(installments)
            .build());
      }

      return creditCards;
   }

   //When this crawler was made, no product with rating was found
   private RatingsReviews scrapRatings() {
      return new RatingsReviews();
   }
}
