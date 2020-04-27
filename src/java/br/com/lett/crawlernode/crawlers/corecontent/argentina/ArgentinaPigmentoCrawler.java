package br.com.lett.crawlernode.crawlers.corecontent.argentina;

import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.MathUtils;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
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
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

public class ArgentinaPigmentoCrawler extends Crawler {

  public ArgentinaPigmentoCrawler(Session session) {
    super(session);
  }

  @Override
  public List<Product> extractInformation(Document document) throws Exception {
    List<Product> products = new ArrayList<>();
    String internalId =
        document.selectFirst(".price-box.price-final_price").attr("data-product-id");

    String name = document.selectFirst(".base").text();
    String[] measuring = document.selectFirst(".pigmento_tamano").text().split(":");
    if (measuring.length >= 2) {
      name += " " + measuring[1].trim();
    }

    String description = document.selectFirst("#product.info.description").wholeText();

    List<String> categories = document.select("ul.items strong").eachText();

    Offers offers = scrapOffers(document);

    List<JSONObject> jsonImages = scrapJsonImages(document);
    JSONArray secondaryImages = new JSONArray();
    String primaryImage = null;
    for (JSONObject jsonImage : jsonImages) {
      String image = jsonImage.optString("full");
      if (jsonImage.optBoolean("isMain")) {
        primaryImage = image;
      } else {
        secondaryImages.put(image);
      }
    }

    products.add(
        ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setName(name)
            .setOffers(offers)
            .setCategories(categories)
            .setPrimaryImage(primaryImage)
            .setSecondaryImages(secondaryImages.toString())
            .setDescription(description)
            .build());

    return products;
  }

  private List<JSONObject> scrapJsonImages(Document doc) {
    List<JSONObject> listJson = new ArrayList<>();
    Optional<Element> optElem =
        doc.select("script[type='text/x-magento-init']").stream()
            .filter(
                element ->
                    element
                        .text()
                        .contains(
                            "https:\\/\\/perfumeriaspigmento.com.ar\\/media\\/catalog\\/product\\/cache\\/image\\/620x678\\/"))
            .findFirst();
    JSONObject jsonObject =
        optElem.map(element -> new JSONObject(element.text())).orElseGet(JSONObject::new);

    JSONObject jsonObject1 = jsonObject.optJSONObject("[data-gallery-role=gallery-placeholder]");
    if (jsonObject1 != null) {
      JSONObject jsonObject2 = jsonObject1.optJSONObject("mage/gallery/gallery");
      if (jsonObject2 != null) {
        for (Object data : jsonObject2.optJSONArray("data")) {
          listJson.add((JSONObject) data);
        }
      }
    }

    return listJson;
  }

  private Offers scrapOffers(Document doc) throws MalformedPricingException, OfferException {
    Offers offers = new Offers();
    Double price =
        MathUtils.parseDoubleWithDot(
            doc.selectFirst("span[data-price-type=finalPrice] .price").text());
    Double priceFrom =
        MathUtils.parseDoubleWithDot(
            doc.selectFirst("span[data-price-type=oldPrice] .price").text());

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
