package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CommonMethods;
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
import ucar.ma2.ArrayObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class BrasilZoolojapetCrawler extends Crawler {
   public BrasilZoolojapetCrawler(Session session) {
      super(session);
   }
   private String SELLER_NAME="zoolojapet";
   private String HOME_PAGE = "https://zoolojapet.com.br/";
   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      List<Product> products = new ArrayList<Product>();
      JSONObject dataJson = getScriptJson(doc);
      if(dataJson!=null) {
         String internalId = dataJson.optString("id",null);
         String internalPid = dataJson.optString("sku",null);
         String url = getUrl(dataJson);
         String name = dataJson.optString("name");
         String description = dataJson.optString("description");
         String primaryImage = getPrimaryImage(dataJson);
         List<String> secondaryImages = getSecondaryImages(dataJson);
         Offers offers = scrapOffers(dataJson);
         Product product = ProductBuilder.create()
            .setInternalId(internalId)
            .setInternalPid(internalPid)
            .setUrl(url)
            .setName(name)
            .setOffers(offers)
            .setPrimaryImage(primaryImage)
            .setSecondaryImages(secondaryImages)
            .setDescription(description)
            .build();
         products.add(product);
      }else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }
      return products;
   }
   private String getUrl(JSONObject json) {
      if(json!=null) {
         String slug = json.optString("slug");
         if(!"".equals(slug) && !slug.isEmpty()) {
            return HOME_PAGE+slug;
         }
      }
      return null;
   }
   private String getPrimaryImage(JSONObject json) {
      if (json!=null) {
         String imageUrl = JSONUtils.getValueRecursive(json,"image.url", String.class);
         if (!imageUrl.isEmpty() && !"".equals(imageUrl)) {
            return imageUrl;
         }
      }
      return null;
   }
   private List<String> getSecondaryImages(JSONObject json) {
      if (json!=null) {
         JSONArray arrayImages = JSONUtils.getValueRecursive(json,"images",JSONArray.class);
         if(!arrayImages.isEmpty() && arrayImages!=null) {
            List<String> secondaryImages = new ArrayList<String>();
            for(Integer i=0;i<arrayImages.length();i++) {
               JSONObject img = arrayImages.optJSONObject(i);
               String urlImage = img.optString("url");
               if(!"".equals(urlImage) && !urlImage.isEmpty()) {
                  secondaryImages.add(urlImage);
               }
            }
            if (secondaryImages.size() > 0) {
               return secondaryImages;
            }
         }
      }
      return null;
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
         .setSellerFullName(SELLER_NAME)
         .setIsMainRetailer(true)
         .setUseSlugNameAsInternalSellerId(true)
         .build());
      return offers;
   }
   private Pricing scrapPricing(JSONObject json) throws MalformedPricingException {
     Double price = json.optDouble("price");

      CreditCards creditCards = scrapCreditCards(price);
      BankSlip bankSlip = BankSlip.BankSlipBuilder.create()
         .setFinalPrice(price)
         .build();

      return Pricing.PricingBuilder.create()
         .setSpotlightPrice(price)
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

      Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(), Card.HIPERCARD.toString(),
         Card.DINERS.toString(),Card.HIPER.toString(), Card.ELO.toString(), Card.SOROCRED.toString(),Card.AMEX.toString());

      for (String card : cards) {
         creditCards.add(CreditCard.CreditCardBuilder.create()
            .setBrand(card)
            .setInstallments(installments)
            .setIsShopCard(false)
            .build());
      }

      return creditCards;
   }


   private JSONObject getScriptJson(Document doc) {
      String dataScript = CrawlerUtils.scrapScriptFromHtml(doc,"#__NEXT_DATA__");
      if(dataScript != null && !dataScript.isEmpty()) {
         JSONArray arrayJson = CrawlerUtils.stringToJsonArray(dataScript);
         if (arrayJson!=null && !arrayJson.isEmpty()) {
            return JSONUtils.getValueRecursive(arrayJson,"0.props.pageProps.resource.data",JSONObject.class);
         }

      }
      return new JSONObject();
   }

}
