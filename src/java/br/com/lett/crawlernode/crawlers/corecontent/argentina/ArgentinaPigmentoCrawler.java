package br.com.lett.crawlernode.crawlers.corecontent.argentina;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
import exceptions.MalformedPricingException;
import exceptions.OfferException;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import models.Offer.OfferBuilder;
import models.Offers;
import models.pricing.BankSlip.BankSlipBuilder;
import models.pricing.CreditCard.CreditCardBuilder;
import models.pricing.CreditCards;
import models.pricing.Installment.InstallmentBuilder;
import models.pricing.Installments;
import models.pricing.Pricing.PricingBuilder;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class ArgentinaPigmentoCrawler extends Crawler {

   public ArgentinaPigmentoCrawler(Session session) {
      super(session);
   }

   @Override
   public List<Product> extractInformation(Document document) throws Exception {
      List<Product> products = new ArrayList<>();

      if (document.selectFirst("div.vtex-store-components-3-x-container") != null) {
         JSONObject json = CrawlerUtils.selectJsonFromHtml(document, "script[type=application/ld+json]", null, null, false, false);
         String internalPid = json.optString("mpn");
         String description = document.selectFirst("div.vtex-tab-layout-0-x-container.vtex-tab-layout-0-x-container--titulos-tab-pdp").wholeText();

         JSONArray variations = json.has("offers") ? json.optJSONObject("offers").optJSONArray("offers") : new JSONArray();

         for (Object o : variations) {
            JSONObject variation = (JSONObject) o;
            Document variationPage = document;

            String internalId = variation.optString("sku");

            if (variations.length() > 1 && !session.getOriginalURL().contains(internalId)) {
               variationPage = scrapVariation(internalId);
            }

            if (variationPage != null) {
               String name = json.optString("name");
               name += " " + scrapName(variationPage);
               List<String> categories = variationPage.select("div.vtex-breadcrumb-1-x-container a[href]").eachText();
               List<String> images = scrapImages(variationPage);
               String primaryImage = images.isEmpty() ? "" : images.remove(0);

               Offers offers = scrapOffers(document);

               products.add(
                  ProductBuilder.create()
                     .setUrl(session.getOriginalURL())
                     .setInternalId(internalId)
                     .setInternalPid(internalPid)
                     .setName(name)
                     .setOffers(offers)
                     .setCategories(categories)
                     .setPrimaryImage(primaryImage)
                     .setSecondaryImages(images)
                     .setDescription(description)
                     .build());
            }
         }
      }

      return products;
   }

   private List<String> scrapImages(Document doc) {
      List<String> images = new ArrayList<>();
      String imagesAttr = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "img.vtex-store-components-3-x-productImageTag.vtex-store-components-3-x-productImageTag--main", "srcset");

      if (imagesAttr != null && !imagesAttr.equals("")) {
         String[] urls = imagesAttr.split(" ");

         if (urls.length != 0) {
            for (String url : urls) {
               if (url.contains("https")) {
                  images.add(url);
               }
            }
         }
      }

      return images;
   }

   private Document scrapVariation(String internalId) {
      try {
         URI uri = new URI(session.getOriginalURL());
         String url = uri.toString();

         if (uri.getQuery() != null) {
            url = url.substring(0, url.indexOf("=")) + "=" + internalId;
         } else {
            url += "?skuId=" + internalId;
         }

         Request request = new Request.RequestBuilder()
            .setUrl(url)
            .build();

         Response response = this.dataFetcher.get(session, request);
         return Jsoup.parse(response.getBody());
      } catch (Exception e) {
         Logging.printLogDebug(logger, e.getMessage());
      }

      return null;
   }

   //vtex-store-components-3-x-productNameContainer vtex-store-components-3-x-productNameContainer--product-name-pdp mv0 t-heading-4

   private String scrapName(Document doc) {
      String name = "";
      Elements elements = doc.select("span.vtex-store-components-3-x-productBrand.vtex-store-components-3-x-productBrand--product-name-pdp");

      if (!elements.isEmpty()) {
         Element el = elements.last();

         name = el.html();
      }

      return name;
   }

   private Offers scrapOffers(Document doc) throws MalformedPricingException, OfferException {
      Offers offers = new Offers();
      Double price = CrawlerUtils.scrapDoublePriceFromHtml(doc, "span.vtex-product-price-1-x-sellingPriceValue span.vtex-product-price-1-x-currencyContainer", null, false, ',', session);
      Double priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(doc, "span.vtex-product-price-1-x-listPriceValue.strike span.vtex-product-price-1-x-currencyContainer", null, false, ',', session);

      CreditCards creditCards =
         new CreditCards(
            Stream.of(Card.MASTERCARD, Card.VISA, Card.AMEX, Card.DINERS, Card.HIPERCARD)
               .map(
                  card -> {
                     try {
                        return CreditCardBuilder.create()
                           .setBrand(card.toString())
                           .setIsShopCard(false)
                           .setInstallments(
                              new Installments(
                                 Collections.singleton(
                                    InstallmentBuilder.create()
                                       .setInstallmentPrice(price)
                                       .setInstallmentNumber(1)
                                       .build())))
                           .build();
                     } catch (MalformedPricingException e) {
                        throw new RuntimeException(e);
                     }
                  })
               .collect(Collectors.toList()));

      offers.add(
         OfferBuilder.create()
            .setSellerFullName("Pigmento")
            .setIsBuybox(false)
            .setPricing(
               PricingBuilder.create()
                  .setSpotlightPrice(price)
                  .setPriceFrom(priceFrom)
                  .setBankSlip(BankSlipBuilder.create().setFinalPrice(price).build())
                  .setCreditCards(creditCards)
                  .build())
            .setIsMainRetailer(true)
            .setUseSlugNameAsInternalSellerId(true)
            .build());

      return offers;
   }
}
