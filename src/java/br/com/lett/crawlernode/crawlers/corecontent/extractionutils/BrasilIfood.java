package br.com.lett.crawlernode.crawlers.corecontent.extractionutils;

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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public abstract class BrasilIfood extends Crawler {

  protected String region = getRegion();
  protected String store_name = getStore_name();
  protected String seller_full_name = getSellerFullName();

  protected abstract String getRegion();

  protected abstract String getStore_name();

  protected abstract String getSellerFullName();

  public BrasilIfood(Session session) {
    super(session);
  }

  private final String HOME_PAGE = "https://www.ifood.com.br/delivery/" + region + "/" + store_name;

  protected Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),
        Card.AURA.toString(), Card.DINERS.toString(), Card.HIPER.toString(), Card.AMEX.toString());
  
  @Override
  public boolean shouldVisit() {
    String href = this.session.getOriginalURL().toLowerCase();
    return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
  }

  @Override
  public List<Product> extractInformation(Document doc) throws Exception {
    System.err.println(HOME_PAGE);
    super.extractInformation(doc);
    List<Product> products = new ArrayList<>();

    if (doc.selectFirst("#__NEXT_DATA__") != null) {

      JSONObject json = CrawlerUtils.selectJsonFromHtml(doc, "#__NEXT_DATA__", null, null, false, false);
      JSONObject props = JSONUtils.getJSONValue(json, "props");
      JSONObject initialState = JSONUtils.getJSONValue(props, "initialState");
      JSONObject restaurant = JSONUtils.getJSONValue(initialState, "restaurant");
      JSONArray menu = JSONUtils.getJSONArrayValue(restaurant, "menu");

      String urlCode = "";
      if (session.getOriginalURL().contains("=")) {
        urlCode = CommonMethods.getLast(session.getOriginalURL().split("="));
      }

      if (menu != null && !menu.isEmpty()) {

        for (Object menuArr : menu) {

          JSONObject menuObject = (JSONObject) menuArr;
          JSONArray itens = JSONUtils.getJSONArrayValue(menuObject, "itens");

          for (Object itensArr : itens) {

            JSONObject itensObject = (JSONObject) itensArr;

            String getCode = itensObject.optString("code");

            if (getCode.equals(urlCode)) {

              String name = itensObject.optString("description");
              String available = itensObject.optString("availability");
              boolean availableToBuy = available.equalsIgnoreCase("AVAILABLE");
              String url = "https://static-images.ifood.com.br/image/upload/t_medium/pratos/";
              String primaryImage = url + itensObject.optString("logoUrl");
              String description = itensObject.optString("details");
              Offers offers = availableToBuy ? scrapOffer(itensObject) : new Offers();

              // Creating the product
              Product product = ProductBuilder.create()
                    .setUrl(session.getOriginalURL())
                    .setInternalId(getCode)
                    .setInternalPid(getCode)
                    .setName(name)
                    .setPrimaryImage(primaryImage)
                    .setDescription(description)
                    .setOffers(offers)
                    .build();

              products.add(product);

              Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());
            }
          }
        }
      }

    } else {
      Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
    }

    return products;
  }



  private Offers scrapOffer(JSONObject jsonOffers) throws OfferException, MalformedPricingException {
    Offers offers = new Offers();
    Pricing pricing = scrapPricing(jsonOffers);
    List<String> sales = new ArrayList<>();

    offers.add(Offer.OfferBuilder.create()
          .setUseSlugNameAsInternalSellerId(true)
          .setSellerFullName(seller_full_name)
          .setMainPagePosition(1)
          .setIsBuybox(false)
          .setIsMainRetailer(true)
          .setPricing(pricing)
          .setSales(sales)
          .build());

    return offers;

  }

  private Pricing scrapPricing(JSONObject jsonOffers) throws MalformedPricingException {
    Double spotlightPrice = jsonOffers.optDouble("unitPrice");
    CreditCards creditCards = scrapCreditCards(spotlightPrice);

    return Pricing.PricingBuilder.create()
          .setSpotlightPrice(spotlightPrice)
          .setCreditCards(creditCards)
          .build();


  }

  private CreditCards scrapCreditCards(Double spotlightPrice) throws MalformedPricingException {
    CreditCards creditCards = new CreditCards();

    Installments installments = new Installments();
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

}
