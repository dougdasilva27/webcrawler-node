package br.com.lett.crawlernode.crawlers.corecontent.chile;

import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
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
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;


public class ChileUnimarcCrawler extends Crawler {

   public ChileUnimarcCrawler(Session session) {
      super(session);
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {

      List<Product> products = new ArrayList<>();

      Logging.printLogDebug(logger, session, "Product page identified: " + session.getOriginalURL());
      JSONObject dataJson = CrawlerUtils.selectJsonFromHtml(doc, "#__NEXT_DATA__", null, null, false, false);
      JSONArray productsArray = JSONUtils.getValueRecursive(dataJson, "props.pageProps.product.data.products", JSONArray.class);
      for (Object o : productsArray) {
         JSONObject data = (JSONObject) o;
         if (data != null && !data.isEmpty()) {
            String internalId = JSONUtils.getValueRecursive(data, "item.itemId", String.class);
            String internalPid = JSONUtils.getValueRecursive(data, "item.productId", String.class);
            String name = JSONUtils.getValueRecursive(data, "item.nameComplete", String.class);
            JSONArray images = JSONUtils.getValueRecursive(data, "item.images", JSONArray.class);
            String primaryImage = !images.isEmpty() ? (String) images.get(0) : "";
            String description = JSONUtils.getValueRecursive(data, "item.description", String.class);
            CategoryCollection categories = scrapCategories(data);
            Integer stock = JSONUtils.getValueRecursive(data, "price.availableQuantity", Integer.class, 0);
            List<String> secondaryImages = scrapSecondaryImages(images, primaryImage);
            Offers offers = stock > 0 ? scrapOffers(data) : new Offers();
            Product product = ProductBuilder.create()
               .setUrl(session.getOriginalURL())
               .setInternalId(internalId)
               .setInternalPid(internalPid)
               .setName(name)
               .setOffers(offers)
               .setPrimaryImage(primaryImage)
               .setSecondaryImages(secondaryImages)
               .setDescription(description)
               .setCategories(categories)
               .setStock(stock)
               .build();

            products.add(product);
         } else {
            Logging.printLogDebug(logger, session, "Not a product page " + session.getOriginalURL());
         }
      }
      return products;
   }


   private List<String> scrapSecondaryImages(JSONArray images, String primaryImage) {
      List<String> list = new ArrayList<>();
      for (Integer i = 0; i < images.length(); i++) {
         String image = (String) images.get(i);
         if (!image.equals(primaryImage)) {
            list.add(image);
         }
      }
      return list;
   }

   private CategoryCollection scrapCategories(JSONObject dataJson) {
      JSONArray arr = JSONUtils.getValueRecursive(dataJson, "item.categories", JSONArray.class);
      CategoryCollection categories = new CategoryCollection();
      int cont = 0;
      for (Integer i = 0; i < arr.length(); i++) {
         categories.add((String) arr.get(i));
         cont++;
         if (cont == 3) {
            return categories;
         }
      }
      return categories;
   }

   private Offers scrapOffers(JSONObject data) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      List<String> sales = new ArrayList<>();

      Pricing pricing = scrapPricing(data);
      sales.add(CrawlerUtils.calculateSales(pricing));

      offers.add(Offer.OfferBuilder.create()
         .setMainPagePosition(1)
         .setIsBuybox(false)
         .setPricing(pricing)
         .setSales(sales)
         .setSellerFullName("unimarc")
         .setIsMainRetailer(true)
         .setUseSlugNameAsInternalSellerId(true)
         .build());

      return offers;
   }

   private Pricing scrapPricing(JSONObject data) throws MalformedPricingException {
      Integer spotlightPriceInt = JSONUtils.getValueRecursive(data, "price.price", Integer.class);
      if (spotlightPriceInt == null) {
         Double priceDouble = JSONUtils.getValueRecursive(data, "price.price", Double.class);
         spotlightPriceInt = priceDouble.intValue();
      }
      spotlightPriceInt = spotlightPriceInt * 100;
      Integer priceFromInt = JSONUtils.getValueRecursive(data, "price.priceWithoutDiscount", Integer.class);
      if (priceFromInt == null) {
         Double priceDouble = JSONUtils.getValueRecursive(data, "price.priceWithoutDiscount", Double.class);
         priceFromInt = priceDouble.intValue();
      }
      priceFromInt = priceFromInt * 100;
      Double spotlightPrice = spotlightPriceInt / 100.0;
      Double priceFrom = priceFromInt / 100.0;


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

      Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(), Card.AURA.toString(), Card.DINERS.toString(), Card.AMEX.toString());

      for (String card : cards) {
         creditCards.add(CreditCard.CreditCardBuilder.create()
            .setBrand(card)
            .setInstallments(installments)
            .setIsShopCard(false)
            .build());
      }
      return creditCards;
   }

   @Override
   protected Response fetchResponse() {
      try {
         HttpClient client = HttpClient.newBuilder().build();
         HttpRequest request = HttpRequest.newBuilder()
            .GET()
            .headers("accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9", "accept-language", "pt-BR,pt;q=0.9,en-US;q=0.8,en;q=0.7", "accept-encoding", "gzip, deflate, br", "authority", "www.unimarc.cl")
            .build();
         HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
         return new Response.ResponseBuilder()
            .setBody(response.body())
            .setLastStatusCode(response.statusCode())
            .build();
      } catch (Exception e) {
         throw new RuntimeException("Failed scrape in API: " + session.getOriginalURL(), e);
      }
   }
}
