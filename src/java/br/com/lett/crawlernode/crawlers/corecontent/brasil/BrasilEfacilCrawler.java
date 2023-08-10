package br.com.lett.crawlernode.crawlers.corecontent.brasil;


import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
import com.google.common.collect.Sets;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.Offer;
import models.Offers;
import models.pricing.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class BrasilEfacilCrawler extends Crawler {

   private static final String SELLER_FULL_NAME = "efacil";

   protected Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),
      Card.AURA.toString(), Card.DINERS.toString(), Card.HIPER.toString(), Card.AMEX.toString());

   public BrasilEfacilCrawler(Session session) {
      super(session);
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();
      JSONObject dataJson = CrawlerUtils.selectJsonFromHtml(doc, "#__NEXT_DATA__", null, null, false, false);
      JSONObject productInfo = JSONUtils.getValueRecursive(dataJson, "props.pageProps.staticProductData", JSONObject.class);

      if (productInfo != null && !productInfo.isEmpty()) {
         String internalId = JSONUtils.getValueRecursive(productInfo, "idProduto", String.class);
         String name = JSONUtils.getValueRecursive(productInfo, "nome", String.class);
         JSONArray images = JSONUtils.getValueRecursive(productInfo, "skus.0.imagens", JSONArray.class, new JSONArray());
         String primaryImage = JSONUtils.getValueRecursive(images, "0.url1000", String.class);
         List<String> secondaryImages = getSecondaryImages(images);
         String description = getDescription(productInfo);
         List<String> categories = getCategories(productInfo);
         boolean available = JSONUtils.getValueRecursive(productInfo, "skus.0.disponivel", Boolean.class);
         Offers offers = available ? scrapOffers(productInfo) : new Offers();
         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setInternalPid(internalId)
            .setName(name)
            .setOffers(offers)
            .setPrimaryImage(primaryImage)
            .setSecondaryImages(secondaryImages)
            .setDescription(description)
            .setCategories(categories)
            .build();

         products.add(product);
      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + session.getOriginalURL());
      }
      return products;
   }

   private String getDescription(JSONObject productInfo) {
      StringBuilder description = new StringBuilder();

      String longDescription = JSONUtils.getValueRecursive(productInfo, "descricaoLonga", String.class, "");
      if (longDescription != null && !longDescription.isEmpty()) {
         description.append(longDescription);
      }

      String shortDescription = JSONUtils.getValueRecursive(productInfo, "descricao", String.class, "");
      if (shortDescription != null && !shortDescription.isEmpty()) {
         description.append(description);
      }

      return description.toString();
   }

   private List<String> getCategories(JSONObject productInfo) {
      List<String> categories = new ArrayList<>();

      String category = JSONUtils.getValueRecursive(productInfo, "categoria", String.class, "");
      if (category != null && !category.isEmpty()) {
         categories.add(category);
      }

      String subCategory = JSONUtils.getValueRecursive(productInfo, "subcategoria", String.class, "");
      if (subCategory != null && !subCategory.isEmpty()) {
         categories.add(subCategory);
      }

      return categories;
   }


   private List<String> getSecondaryImages(JSONArray images) {
      List<String> secondaryImages = new ArrayList<>();
      if (images != null) {
         for (Object o : images) {
            JSONObject data = (JSONObject) o;
            String image = JSONUtils.getValueRecursive(data, "url1000", String.class);

            if (image != null) {
               secondaryImages.add(image);
            }
         }
         if (secondaryImages.size() > 0) {
            secondaryImages.remove(0);
         }
         return secondaryImages;
      }
      return null;
   }


   private Offers scrapOffers(JSONObject productInfo) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(productInfo);
      List<String> sales = new ArrayList<>();
      sales.add(CrawlerUtils.calculateSales(pricing));

      offers.add(new Offer.OfferBuilder()
         .setUseSlugNameAsInternalSellerId(true)
         .setSellerFullName(this.SELLER_FULL_NAME)
         .setMainPagePosition(1)
         .setIsBuybox(false)
         .setIsMainRetailer(true)
         .setPricing(pricing)
         .setSales(sales)
         .build());

      return offers;

   }

   private Pricing scrapPricing(JSONObject productInfo) throws MalformedPricingException {
      String spotlightPriceString = JSONUtils.getValueRecursive(productInfo, "skus.0.preco.precoPorText", String.class, "");
      String priceFromString = JSONUtils.getValueRecursive(productInfo, "skus.0.preco.precoDeText", String.class, "");
      Double spotlightPrice = null;
      Double priceFrom = null;

      if (spotlightPriceString != null && !spotlightPriceString.isEmpty()) {
         spotlightPrice = MathUtils.parseDoubleWithComma(spotlightPriceString);
      }

      if (priceFromString != null && !priceFromString.isEmpty()) {
         priceFrom = MathUtils.parseDoubleWithComma(priceFromString);
      }

      BankSlip bankSlip = CrawlerUtils.setBankSlipOffers(spotlightPrice, null);
      CreditCards creditCards = scrapCreditCards(productInfo, spotlightPrice);

      return Pricing.PricingBuilder.create()
         .setPriceFrom(priceFrom)
         .setSpotlightPrice(spotlightPrice)
         .setBankSlip(bankSlip)
         .setCreditCards(creditCards)
         .build();

   }

   private CreditCards scrapCreditCards(JSONObject productInfo, Double spotlightPrice) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();

      Installments installments = scrapInstallments(productInfo);
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

   public Installments scrapInstallments(JSONObject productInfo) throws MalformedPricingException {
      Installments installments = new Installments();

      if (productInfo != null && !productInfo.isEmpty()) {
         Integer installment = JSONUtils.getValueRecursive(productInfo, "preco.parcelas.numeroParcelas", Integer.class, 0);
         Double value = MathUtils.parseDoubleWithComma(JSONUtils.getValueRecursive(productInfo, "preco.parcelas.valorParcela", String.class, ""));

         if (installment == null || installment == 0) {
            installment++;
         }

         if (value == null) {
            String valueString = JSONUtils.getValueRecursive(productInfo, "skus.0.preco.precoPorText", String.class, "");
            if (valueString != null && !valueString.isEmpty()) {
               value = MathUtils.parseDoubleWithComma(valueString);
            }
         }

         installments.add(Installment.InstallmentBuilder.create()
            .setInstallmentNumber(installment)
            .setInstallmentPrice(value)
            .build());
      }
      return installments;

   }

}
