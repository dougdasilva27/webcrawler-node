package br.com.lett.crawlernode.crawlers.corecontent.brasil;

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
import models.pricing.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class BrasilCamicadoCrawler extends Crawler {

   private final String HOME_PAGE = "http://www.camicado.com.br/";
   protected Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),
      Card.AURA.toString(), Card.DINERS.toString(), Card.HIPER.toString(), Card.AMEX.toString());

   public BrasilCamicadoCrawler(Session session) {
      super(session);
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      List<Product> products = new ArrayList<>();

      if (isProductPage(doc)) {

         String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, ".js-sku-ref", "data-sku-id");
         String internalPid = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "#js-product-form > input[name=\"product\"]", "value");
         String name = CrawlerUtils.scrapStringSimpleInfo(doc, ".product-info__title", true);
         String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, ".product-showcase__thumbnails-item > img", Collections.singletonList("data-img"), "https", "img.camicado.com.br");
         String secondaryImage = CrawlerUtils.scrapSimpleSecondaryImages(doc, ".product-showcase__thumbnails-item > img", Collections.singletonList("data-img"), "https", "img.camicado.com.br", primaryImage);
         CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".breadcrumb > li", true);
         String description = CrawlerUtils.scrapElementsDescription(doc, Collections.singletonList(".detailsTab > .h-flexbox"));
         boolean availability = doc.selectFirst(".unavaible-form") == null;
         Offers offers = availability ? scrapOffers(doc) : new Offers();

         Elements variations = doc.select(".product-info__item-container .js-sku-selector");
         if (variations.size() > 1) {
            for (Element variation : variations) {
               String sku = CrawlerUtils.scrapStringSimpleInfoByAttribute(variation, "input", "value");
               JSONObject variationJson = fetchVariation(sku, internalPid);

               String attributeVariation = scrapVariationName(variationJson);
               List<String> images = scrapVariationImages(variationJson);
               boolean variationAvailability = scrapVariationAvailability(variationJson);
               Offers variationOffers = variationAvailability ? offers : new Offers();

               Product product = ProductBuilder.create()
                  .setUrl(session.getOriginalURL().replace(internalId, sku))
                  .setInternalId(sku)
                  .setInternalPid(internalPid)
                  .setName(name + " - " + attributeVariation)
                  .setCategory1(categories.getCategory(0))
                  .setCategory2(categories.getCategory(1))
                  .setCategory3(categories.getCategory(2))
                  .setPrimaryImage(images.size() > 0 ? images.remove(0) : null)
                  .setSecondaryImages(images.size() > 0 ? images : null)
                  .setDescription(variationJson.optString("description"))
                  .setOffers(variationOffers)
                  .build();

               products.add(product);
            }

         } else {
            Product product = ProductBuilder.create()
               .setUrl(session.getOriginalURL())
               .setInternalId(internalId)
               .setInternalPid(internalPid)
               .setName(name)
               .setCategory1(categories.getCategory(0))
               .setCategory2(categories.getCategory(1))
               .setCategory3(categories.getCategory(2))
               .setPrimaryImage(primaryImage)
               .setSecondaryImages(secondaryImage)
               .setDescription(description)
               .setOffers(offers)
               .build();

            products.add(product);
         }

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }

   private boolean isProductPage(Document doc) {
      return doc.selectFirst(".page-product-single") != null;
   }

   private Offers scrapOffers(Document doc) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(doc);

      if (pricing != null) {
         offers.add(Offer.OfferBuilder.create()
            .setUseSlugNameAsInternalSellerId(true)
            .setSellerFullName(CrawlerUtils.scrapStringSimpleInfo(doc, ".product-info__mkt strong", true).toLowerCase())
            .setSellersPagePosition(1)
            .setIsBuybox(false)
            .setIsMainRetailer(true)
            .setPricing(pricing)
            .build());
      }

      return offers;
   }

   private Pricing scrapPricing(Document doc) throws MalformedPricingException {

      Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".product-info__new-value", null, false, ',', this.session);
      Double priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".product-info__old-value", null, false, ',', this.session);
      CreditCards creditCards = scrapCreditCards(doc, spotlightPrice);
      BankSlip bankSlip = scrapBankSlip(spotlightPrice);

      return Pricing.PricingBuilder.create()
         .setSpotlightPrice(spotlightPrice)
         .setPriceFrom(priceFrom)
         .setCreditCards(creditCards)
         .setBankSlip(bankSlip)
         .build();
   }

   private BankSlip scrapBankSlip(Double spotlightPrice) throws MalformedPricingException {
      return BankSlip.BankSlipBuilder.create()
         .setFinalPrice(spotlightPrice)
         .build();
   }

   private CreditCards scrapCreditCards(Document doc, Double spotlightPrice) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();
      Installments installments = scrapInstallments(doc);

      if (installments.getInstallments().isEmpty()) {
         installments.add(Installment.InstallmentBuilder.create()
            .setInstallmentNumber(1)
            .setInstallmentPrice(spotlightPrice)
            .build());
      }

      for (String brand : cards) {
         creditCards.add(CreditCard.CreditCardBuilder.create()
            .setBrand(brand)
            .setIsShopCard(false)
            .setInstallments(installments)
            .build());
      }

      return creditCards;
   }

   private Installments scrapInstallments(Document doc) {
      Installments installments = new Installments();
      //2x de R$ 46,74 s/ juros no cartão de Crédito



      return installments;
   }

   private JSONObject fetchVariation(String sku, String internalPId) {

      String url = "https://www.camicado.com.br/rest/model/lrsa/api/CatalogActor/refreshProductPage?" +
         "pushSite=camicadoBrasilDesktop" +
         "&skuId=" + sku +
         "&productId=" + internalPId;

      Request request = Request.RequestBuilder.create().setUrl(url)
         .setCookies(cookies)
         .mustSendContentEncoding(false)
         .build();

      Response response = this.dataFetcher.get(session, request);
      return CrawlerUtils.stringToJson(response.getBody());
   }

   private String scrapVariationName(JSONObject jsonVariation) {
      JSONArray attributes = jsonVariation.optJSONArray("skuAttributes");
      JSONObject attribute = (JSONObject) attributes.opt(0);
      return attribute.optString("name");
   }

   private List<String> scrapVariationImages(JSONObject jsonVariation) {
      List<String> finalImages = new ArrayList<>();
      JSONArray images = jsonVariation.optJSONArray("mediaSets");
      for (Object obj : images) {
         JSONObject image = (JSONObject) obj;
         String link = image.optString("largeImageUrl");
         if (link != null) {
            finalImages.add(link);
         }
      }
      return finalImages;
   }

   private boolean scrapVariationAvailability(JSONObject jsonVariation) {
      return !jsonVariation.optBoolean("outOfStock", true);
   }
}
