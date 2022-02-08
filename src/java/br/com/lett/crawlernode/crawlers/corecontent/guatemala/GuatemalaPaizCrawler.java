package br.com.lett.crawlernode.crawlers.corecontent.guatemala;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.crawlers.extractionutils.core.VTEXNewScraper;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.MathUtils;
import com.google.common.collect.Sets;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.Offer;
import models.Offers;
import models.RatingsReviews;
import models.pricing.*;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class GuatemalaPaizCrawler extends VTEXNewScraper {

   public GuatemalaPaizCrawler(Session session) {
      super(session);
      config.setFetcher(FetchMode.FETCHER);
   }

   protected Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),
      Card.AURA.toString(), Card.DINERS.toString(), Card.HIPER.toString(), Card.AMEX.toString());

   @Override
   public void handleCookiesBeforeFetch() {
      this.cookies.add(new BasicClientCookie("vtex_segment", "eyJjYW1wYWlnbnMiOm51bGwsImNoYW5uZWwiOiIyIiwicHJpY2VUYWJsZXMiOm51bGwsInJlZ2lvbklkIjpudWxsLCJ1dG1fY2FtcGFpZ24iOm51bGwsInV0bV9zb3VyY2UiOm51bGwsInV0bWlfY2FtcGFpZ24iOm51bGwsImN1cnJlbmN5Q29kZSI6IkdUUSIsImN1cnJlbmN5U3ltYm9sIjoiUSIsImNvdW50cnlDb2RlIjoiR1RNIiwiY3VsdHVyZUluZm8iOiJlcy1HVCIsImFkbWluX2N1bHR1cmVJbmZvIjoiZXMtR1QiLCJjaGFubmVsUHJpdmFjeSI6InB1YmxpYyJ9"));
   }

   @Override
   protected String getHomePage() {
      return "https://www.paiz.com.gt/";
   }

   @Override
   protected List<String> getMainSellersNames() {
      return Arrays.asList("paizgt");
   }

   @Override
   protected RatingsReviews scrapRating(String internalId, String internalPid, Document doc, JSONObject jsonSku) {
      return null;
   }

   @Override
   protected Product extractProductHtml(Document doc, String internalPid) throws MalformedProductException, OfferException, MalformedPricingException {
      return ProductBuilder.create()
         .setUrl(session.getOriginalURL())
         .setInternalId(CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "meta[property=\"product:sku\"]", "content"))
         .setInternalPid(internalPid)
         .setName(CrawlerUtils.scrapStringSimpleInfo(doc, ".vtex-store-components-3-x-productBrand", true))
         .setCategories(CrawlerUtils.crawlCategories(doc, ".vtex-breadcrumb-1-x-container span", false))
         .setPrimaryImage(CrawlerUtils.scrapSimplePrimaryImage(doc, ".vtex-store-components-3-x-productImageTag", Arrays.asList("srcset"), "https", "walmartgt.vtexassets.com"))
         .setDescription(CrawlerUtils.scrapSimpleDescription(doc, Arrays.asList(".vtex-store-components-3-x-productDescriptionText")))
         .setOffers(scrapOffer(doc))
         .build();
   }

   private Offers scrapOffer(Document doc) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      try {
         Pricing pricing = scrapPricing(doc);

         offers.add(Offer.OfferBuilder.create()
            .setUseSlugNameAsInternalSellerId(true)
            .setSellerFullName("paizgt")
            .setMainPagePosition(1)
            .setIsBuybox(false)
            .setIsMainRetailer(true)
            .setPricing(pricing)
            .build());

         return offers;
      } catch(MalformedPricingException e) {
         e.printStackTrace();
         return offers;
      }
   }

   private Pricing scrapPricing(Document doc) throws MalformedPricingException {
      Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".vtex-store-components-3-x-sellingPrice", null, false, '.', session);

      CreditCards creditCards = scrapCreditCards(spotlightPrice);

      return Pricing.PricingBuilder.create()
         .setSpotlightPrice(spotlightPrice)
         .setCreditCards(creditCards)
         .build();
   }


   private CreditCards scrapCreditCards( Double spotlightPrice) throws MalformedPricingException {
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
