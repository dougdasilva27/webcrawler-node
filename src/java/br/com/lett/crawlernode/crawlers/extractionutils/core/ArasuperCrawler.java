package br.com.lett.crawlernode.crawlers.extractionutils.core;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.models.Card;
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
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.util.*;

public class ArasuperCrawler extends Crawler {

   private static final String SELLER_FULL_NAME = "Arasuper";
   protected Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),
      Card.AURA.toString(), Card.DINERS.toString(), Card.HIPER.toString(), Card.AMEX.toString());

   public ArasuperCrawler(Session session) {
      super(session);
   }

   @Override
   public void handleCookiesBeforeFetch() {

      Map<String, String> headers = new HashMap<>();
      headers.put("referer", "https://www.arasuper.com.br/index/");

      String payload = "state=" + session.getOptions().optString("state");

      Request request = Request.RequestBuilder.create()
         .setUrl("https://www.arasuper.com.br/home/state")
         .setHeaders(headers)
         .setPayload(payload)
         .build();

      this.cookies = dataFetcher.post(session, request).getCookies();
   }

   @Override
   public List<Product> extractInformation(Document document) throws Exception {
      List<Product> products = new ArrayList<>();


      if (document.select("script[type=\"application/ld+json\"]").size() > 0) {
         JSONObject jsonObject = CrawlerUtils.selectJsonFromHtml(document, "script[type=\"application/ld+json\"]", null, "", false, false);

         List<String> images = scrapeImages(jsonObject);
         Offers offers = scraperOffers(jsonObject, document);
         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(jsonObject.optString("sku"))
            .setInternalPid(jsonObject.optString("sku"))
            .setName(jsonObject.optString("name"))
            .setCategory1(jsonObject.optString("category"))
            .setPrimaryImage(images.remove(0))
            .setSecondaryImages(images)
            .setDescription(jsonObject.optString("description"))
            .setOffers(offers)
            .build();

         products.add(product);
      } else {
         Logging.printLogDebug(logger, "Nenhum produto encontrado");
      }

      return products;
   }

   private Offers scraperOffers(JSONObject jsonObject, Document doc) throws MalformedPricingException, OfferException {
      Offers offers = new Offers();
      if (jsonObject.has("offers")) {
      Pricing pricing = scrapPricing(jsonObject, doc);

      offers.add(Offer.OfferBuilder.create()
         .setUseSlugNameAsInternalSellerId(true)
         .setSellerFullName(SELLER_FULL_NAME)
         .setMainPagePosition(1)
         .setIsBuybox(false)
         .setIsMainRetailer(true)
         .setPricing(pricing)
         .build());
      }

      return offers;

   }

   private Pricing scrapPricing(JSONObject jsonObject, Document doc) throws MalformedPricingException {
      Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".item-produto__price-por",null,true,',',session);
      Double priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".item-produto__price-de",null,true,',',session);

      if (spotlightPrice == null) {
         spotlightPrice = priceFrom;
      }

      CreditCards creditCards = scrapCreditCards(doc, spotlightPrice);

      return Pricing.PricingBuilder.create()
         .setPriceFrom(priceFrom)
         .setSpotlightPrice(spotlightPrice)
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

      for (String card : cards) {
         creditCards.add(CreditCard.CreditCardBuilder.create()
            .setBrand(card)
            .setInstallments(installments)
            .setIsShopCard(false)
            .build());
      }

      return creditCards;
   }


   private List<String> scrapeImages(JSONObject jsonObject) {
      List<String> images = new ArrayList<>();

      if (jsonObject.optJSONArray("image") != null) {
         for (int i = 0; i < jsonObject.optJSONArray("image").length(); i++) {
            images.add(jsonObject.optJSONArray("image").optString(i));
         }
      }

      return images;
   }
}
