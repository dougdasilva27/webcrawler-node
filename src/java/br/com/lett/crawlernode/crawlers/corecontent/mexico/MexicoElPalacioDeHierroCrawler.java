package br.com.lett.crawlernode.crawlers.corecontent.mexico;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import br.com.lett.crawlernode.util.Logging;
import com.google.common.collect.Sets;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.Offer;
import models.Offers;
import models.pricing.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MexicoElPalacioDeHierroCrawler extends Crawler {
   public MexicoElPalacioDeHierroCrawler(Session session) {
      super(session);
   }

   private static final String SELLER_FULL_NAME = "El Palacio de Hierro";

   protected JSONObject fetchJSONObject(String url) {
      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .build();
      Response response = this.dataFetcher.get(session, request);

      return JSONUtils.stringToJson(response.getBody());
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      if (isProductPage(doc)) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());
         String internalPid = crawPid(doc);
         Product product = addProduct(doc, internalPid);
         products.add(product);

         String url = "https://www.elpalaciodehierro.com/on/demandware.store/Sites-palacio-MX-Site/es_MX/Product-Variation?pid="+ internalPid +"&quantity=1&ajax=true";
         JSONObject variantsDoc = fetchJSONObject(url);
         JSONArray variants = (JSONArray) variantsDoc.optQuery("/product/variationAttributes/0/swatchable/0/values");

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }
      return products;
   }

   private Product addProduct(Document variantDoc, String internalPid) throws OfferException, MalformedPricingException, MalformedProductException {
      String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(variantDoc, "button.b-add_to_cart-btn", "data-pid");
      String name = CrawlerUtils.scrapStringSimpleInfo(variantDoc, "h1.b-product_main_info-name", true);
      CategoryCollection categories = CrawlerUtils.crawlCategories(variantDoc, "li a.b-breadcrumbs-link");
      String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(variantDoc, "div.l-pdp-product_images picture.b-product_image img", Arrays.asList("src"), "https", "www.elpalaciodehierro.com");
      List<String> secondaryImages = CrawlerUtils.scrapSecondaryImages(variantDoc, "div.l-pdp-product_images picture.b-product_image img", Arrays.asList("src"), "https", "www.elpalaciodehierro.com", primaryImage);
      String description = CrawlerUtils.scrapSimpleDescription(variantDoc, Arrays.asList("div.b-product_description .b-product_description-short"));
      boolean availableToBuy = variantDoc.selectFirst("p.b-product_availability.m-out_of_stock") == null;
      Offers offers = availableToBuy ? scrapOffers(variantDoc) : new Offers();

      Product product = ProductBuilder.create()
         .setUrl(session.getOriginalURL())
         .setInternalId(internalId)
         .setInternalPid(internalPid)
         .setName(name)
         .setDescription(description)
         .setCategories(categories)
         .setPrimaryImage(primaryImage)
         .setSecondaryImages(secondaryImages)
         .setOffers(offers)
         .build();
      return product;
   }

   private boolean isProductPage(Document doc) {
      return doc.selectFirst("span.b-product_description-key") != null;
   }

   private String crawPid(Document doc) {
      String regex = ": ([0-9]*)";
      String pid = CrawlerUtils.scrapStringSimpleInfo(doc, "span.b-product_description-key", true);;

      final Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
      final Matcher matcher = pattern.matcher(pid);

      if (matcher.find()) {
         return matcher.group(1);
      }
      return null;
   }

   private Offers scrapOffers(Document doc) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      List<String> sales = new ArrayList<>();

      Pricing pricing = scrapPricing(doc);
      sales.add(CrawlerUtils.calculateSales(pricing));

      offers.add(Offer.OfferBuilder.create()
         .setMainPagePosition(1)
         .setIsBuybox(false)
         .setPricing(pricing)
         .setSales(sales)
         .setSellerFullName(SELLER_FULL_NAME)
         .setIsMainRetailer(true)
         .setUseSlugNameAsInternalSellerId(true)
         .build());

      return offers;
   }

   private Pricing scrapPricing(Document doc) throws MalformedPricingException {
      Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, "div[class*=main_info] div.b-product_price-sales span", null, true, '.', session);
      Double priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(doc, "div[class*=main_info] div.b-product_price-old span.b-product_price-value", null, true, '.', session);

      CreditCards creditCards = scrapCreditCards(spotlightPrice);
      BankSlip bankSlip = BankSlip.BankSlipBuilder.create()
         .setFinalPrice(spotlightPrice)
         .build();

      return Pricing.PricingBuilder.create()
         .setSpotlightPrice(spotlightPrice)
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

      Set<String> cards = Sets.newHashSet(Card.AMEX.toString(), Card.AURA.toString(), Card.DINERS.toString(), Card.ELO.toString(),
         Card.HIPERCARD.toString(), Card.HIPERCARD.toString(), Card.VISA.toString());

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
