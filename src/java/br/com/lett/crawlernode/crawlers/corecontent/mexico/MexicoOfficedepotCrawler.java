package br.com.lett.crawlernode.crawlers.corecontent.mexico;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.methods.ApacheDataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.FetcherDataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.JsoupDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.Card;
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
import models.pricing.*;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.*;

public class MexicoOfficedepotCrawler extends Crawler {

   private final String HOME_PAGE = "https://www.officedepot.com.mx";
   private static String SELLER_NAME = "Office Depot";

   public Set<String> cards = Sets.newHashSet(Card.AMEX.toString());

   public MexicoOfficedepotCrawler(Session session) {
      super(session);
      super.config.setFetcher(FetchMode.FETCHER);
   }

   @Override
   protected Response fetchResponse() {
      Request request = Request.RequestBuilder.create()
         .setProxyservice(Arrays.asList(
            ProxyCollection.BUY,
            ProxyCollection.NETNUT_RESIDENTIAL_MX,
            ProxyCollection.NETNUT_RESIDENTIAL_MX_HAPROXY))
         .build();
      Response response = CrawlerUtils.retryRequestWithListDataFetcher(request, List.of(this.dataFetcher, new ApacheDataFetcher(), new FetcherDataFetcher(), new JsoupDataFetcher()), session, "get");

      return response;
   }

   @Override
   public List<Product> extractInformation(Document document) throws Exception {
      List<Product> products = new ArrayList<>();

      if (isProductPage(document)) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String productName = CrawlerUtils.scrapStringSimpleInfo(document, ".col-md-12.name > h1 ", true);
         String internalId = CrawlerUtils.scrapStringSimpleInfo(document, ".sku-detail > .productCode", false);
         String primaryImage = CrawlerUtils.scrapStringSimpleInfoByAttribute(document, "[property=\"og:image\"]", "content");
         List<String> secondaryImages = getSecondaryImages(document);
         String description = CrawlerUtils.scrapStringSimpleInfo(document, ".col-md-12.name > h1 ", true);
         List<String> categories = scrapCategories(document);
         boolean available = document.select(".add_to_cart_form.bb") != null;
         Offers offers = available ? scrapOffers(document) : new Offers();

         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setName(productName)
            .setInternalId(internalId)
            .setInternalPid(internalId)
            .setPrimaryImage(primaryImage)
            .setSecondaryImages(secondaryImages)
            .setDescription(description)
            .setCategories(categories)
            .setOffers(offers)
            .build();
         products.add(product);

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }

   private List<String> scrapCategories(Document doc) {
      List<String> categories = new LinkedList<>();
      for (Element elem : doc.select(".breadcrumb-detail > a > span")) {
         categories.add(elem.ownText());
      }
      if (categories.size() > 0) {
         categories.remove(0);
      }
      return categories;
   }

   private List<String> getSecondaryImages(Document doc) {
      List<String> secondaryImages = new ArrayList<>();

      Elements imageList = doc.select(".img-detail-prin");
      for (Element image : imageList) {
         secondaryImages.add(HOME_PAGE + image.attr("src"));
      }
      if (secondaryImages.size() > 0) {
         secondaryImages.remove(0);
      }
      return secondaryImages;
   }

   private boolean isProductPage(Document document) {
      return document.selectFirst(".h-product.productDetailsPanel") != null;
   }

   private Offers scrapOffers(Document doc) throws MalformedPricingException, OfferException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(doc);

      offers.add(new Offer.OfferBuilder()
         .setUseSlugNameAsInternalSellerId(true)
         .setSellerFullName(this.SELLER_NAME)
         .setMainPagePosition(1)
         .setIsBuybox(false)
         .setIsMainRetailer(true)
         .setPricing(pricing)
         .build());

      return offers;
   }

   private Pricing scrapPricing(Document doc) throws MalformedPricingException {
      Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".priceDataa", null, false, '.', session);
      Double priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".p-price", "value", false, '.', session);

      BankSlip bankSlip = CrawlerUtils.setBankSlipOffers(spotlightPrice, null);
      CreditCards creditCards = scrapCreditCards(spotlightPrice);

      return Pricing.PricingBuilder.create()
         .setSpotlightPrice(spotlightPrice)
         .setPriceFrom(priceFrom)
         .setCreditCards(creditCards)
         .setBankSlip(bankSlip)
         .build();

   }

   private CreditCards scrapCreditCards(Double price) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();
      Installments installments = new Installments();

      installments.add(Installment.InstallmentBuilder.create()
         .setInstallmentNumber(1)
         .setInstallmentPrice(price)
         .setFinalPrice(price)
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
