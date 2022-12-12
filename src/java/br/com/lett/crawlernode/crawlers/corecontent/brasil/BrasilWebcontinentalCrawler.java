package br.com.lett.crawlernode.crawlers.corecontent.brasil;

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
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class BrasilWebcontinentalCrawler extends Crawler {

   private static final String HOME_PAGE = "https://www.webcontinental.com.br";
   private static final String SELLER_NAME_LOWER = "webcontinental";
   protected Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),
      Card.AURA.toString(), Card.DINERS.toString(), Card.HIPER.toString(), Card.AMEX.toString());

   public BrasilWebcontinentalCrawler(Session session) {
      super(session);
   }

   @Override
   public boolean shouldVisit() {
      String href = this.session.getOriginalURL().toLowerCase();
      return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      if (doc.selectFirst("script[data-name=\"occ-structured-data\"]") != null) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String json = doc.selectFirst("script[data-name=\"occ-structured-data\"]").html();
         JSONArray jsonArray = JSONUtils.stringToJsonArray(json);

         for (Object object : jsonArray) {
            JSONObject jsonObject = (JSONObject) object;
            String internalId = jsonObject.optString("sku");
            String internalPid = jsonObject.optString("productId");
            String name = jsonObject.optString("name");
            String description = getDescription(doc);
            String image = jsonObject.optString("image");
            List<String> secondaryImages = getSecondaryImages(doc);
            JSONObject offerJson = jsonObject.optJSONObject("offers");
            boolean available = doc.selectFirst(".ProductNoStock__Title") != null;
            Offers offers = !available ? scrapeOffers(offerJson) : new Offers();
            products.add(new ProductBuilder()
               .setUrl(this.session.getOriginalURL())
               .setInternalId(internalId)
               .setInternalPid(internalPid)
               .setName(name)
               .setDescription(description)
               .setPrimaryImage(image)
               .setSecondaryImages(secondaryImages)
               .setOffers(offers)
               .build());

         }
      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }

   private Offers scrapeOffers(JSONObject offerJson) throws MalformedPricingException, OfferException {
      Offers offers = new Offers();
      Double price = offerJson.optDouble("price");

      Installments installments = new Installments();
      installments.add(Installment.InstallmentBuilder.create()
         .setInstallmentNumber(1)
         .setInstallmentPrice(price)
         .build());

      CreditCards creditCards = new CreditCards();
      for (String card : cards) {
         creditCards.add(CreditCard.CreditCardBuilder.create()
            .setBrand(card)
            .setInstallments(installments)
            .setIsShopCard(false)
            .build());
      }


      Pricing pricing = Pricing.PricingBuilder.create()
         .setSpotlightPrice(price)
         .setCreditCards(creditCards)
         .setBankSlip(BankSlip.BankSlipBuilder.create()
            .setFinalPrice(price)
            .build())
         .build();

      offers.add(Offer.OfferBuilder.create()
         .setUseSlugNameAsInternalSellerId(true)
         .setSellerFullName(SELLER_NAME_LOWER)
         .setMainPagePosition(1)
         .setIsBuybox(false)
         .setIsMainRetailer(true)
         .setPricing(pricing)
         .build());

      return offers;
   }

   private String getDescription(Document doc) {
      String simpleDescription = CrawlerUtils.scrapStringSimpleInfo(doc, "article > div > .txt-cabecalho", false);
      if (simpleDescription == null) {
         simpleDescription = CrawlerUtils.scrapSimpleDescription(doc, Arrays.asList(".ProductLongDescription"));
      }
      return simpleDescription;
   }

   private List<String> getSecondaryImages(Document doc) {
      List<String> secondaryImages = new ArrayList<>();
      Elements images = doc.select(".ProductDetails__Image.ProductDetails_ActiveImage");
      for (Element imageList : images) {
         secondaryImages.add(HOME_PAGE + imageList.attr("src"));
      }
      if (secondaryImages.size() > 0) {
         secondaryImages.remove(0);
      }
      return secondaryImages;
   }

}
