package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import br.com.lett.crawlernode.util.Logging;
import com.google.common.collect.Sets;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.Offer;
import models.Offers;
import models.pricing.*;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class BrasilDDMaquinasCrawler extends Crawler {
   static final String SELLER_FULL_NAME = "Brasil DDMaquinas";
   protected Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),
      Card.AURA.toString(), Card.DINERS.toString(), Card.HIPER.toString(), Card.AMEX.toString());

   public BrasilDDMaquinasCrawler(Session session) {
      super(session);
   }


   @Override
   public List<Product> extractInformation(Document document) throws Exception {
      List<Product> products = new ArrayList<>();

      if (document.selectFirst("div#product") != null) {

         String productName = CrawlerUtils.scrapStringSimpleInfo(document, "#details-product .title", true);
         String productDescription = CrawlerUtils.scrapStringSimpleInfo(document, "#box-description", false);
         Elements productImages = document.select(".content-image-product ul li#zoom-image");
         String primaryImage = productImages.size() > 0 ? CrawlerUtils.scrapStringSimpleInfoByAttribute(productImages.get(0), null, "data-zoom") : "";
         List<String> secondaryImages = CrawlerUtils.scrapSecondaryImagesFromElements(productImages, null, List.of("data-zoom"), "https", "img.irroba.com.br", primaryImage);
         String internalPid = CrawlerUtils.scrapStringSimpleInfo(document, "#details-product .text_model_ref", true);


         Elements variations = document.select("div.options_list .product_options_list li");

         if (variations.size() > 0) {
            for (Element variation : variations) {

               String variationName = productName + " " + variation.text();
               String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(variation, null, "data-product-option-value-id");


               Offers offers = scrapOffers(document, internalId);

               Product product = ProductBuilder.create()
                  .setUrl(session.getOriginalURL())
                  .setInternalId(internalId)
                  .setInternalPid(internalPid)
                  .setName(variationName)
                  .setPrimaryImage(primaryImage)
                  .setSecondaryImages(secondaryImages)
                  .setDescription(productDescription)
                  .setOffers(offers)
                  .build();

               products.add(product);
            }
         } else {


            String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(document, "#product div[id*=\"input-option\"] input ", "value");
            Offers offers = scrapOffers(document, internalId);

            Product product = ProductBuilder.create()
               .setUrl(session.getOriginalURL())
               .setInternalId(internalId)
               .setInternalPid(internalPid)
               .setName(productName)
               .setPrimaryImage(primaryImage)
               .setSecondaryImages(secondaryImages)
               .setDescription(productDescription)
               .setOffers(offers)
               .build();

            products.add(product);

         }
      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;

   }

   private Offers scrapOffers(Document document, String internalId) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      Request request = Request.RequestBuilder.create()
         .setUrl("https://ddmaquinas.com.br/index.php?route=product/product/quantityByOptionValue&product_option_value_id=" + internalId)
         .build();
      Response response = dataFetcher.get(session, request);
      JSONObject jsonObject = JSONUtils.stringToJson(response.getBody());
      int stock = jsonObject.optInt("quantity");
      if (stock > 0) {

         Pricing pricing = scrapPricing(document);

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

   private Pricing scrapPricing(Document doc) throws MalformedPricingException {
      Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".discount_simulator b", null, true, ',', session);
      Double priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(doc, "#product .price", null, true, ',', session);

      CreditCards creditCards = scrapCreditCards(spotlightPrice);

      return Pricing.PricingBuilder.create()
         .setPriceFrom(priceFrom)
         .setSpotlightPrice(spotlightPrice)
         .setBankSlip(BankSlip.BankSlipBuilder.create()
            .setFinalPrice(spotlightPrice)
            .build())
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
