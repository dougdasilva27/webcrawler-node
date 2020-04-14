package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.MathUtils;
import com.google.common.collect.Sets;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import models.Offer.OfferBuilder;
import models.Offers;
import models.RatingsReviews;
import models.pricing.BankSlip.BankSlipBuilder;
import models.pricing.CreditCard;
import models.pricing.CreditCard.CreditCardBuilder;
import models.pricing.CreditCards;
import models.pricing.Installment.InstallmentBuilder;
import models.pricing.Installments;
import models.pricing.Pricing;
import models.pricing.Pricing.PricingBuilder;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;

public class BrasilSalonlineCrawler extends Crawler {

  public BrasilSalonlineCrawler(Session session) {
    super(session);
  }

  @Override
  public List<Product> extractInformation(Document document) throws Exception {
    List<Product> products = new ArrayList<>();
    if (document.selectFirst(".detalhe-produto") != null) {
      JSONObject jsonSku = CrawlerUtils
          .selectJsonFromHtml(document, "#content-wrapper script", "item_product = ", "};", false,
              true);
      String internalId = jsonSku.optString("SKU");
      String internalPid = jsonSku.optString("ProductID");
      String name = jsonSku.optString("Name");

      String description = document.selectFirst(".info").wholeText();
      List<String> categories = document.select("li[itemprop=itemListElement]:not(:first-child)")
          .eachText();
      categories.set(categories.size() - 1,
          categories.get(categories.size() - 1).replace("Página Inicial", "").trim());
      String secondaryImages = new JSONArray(
          document.select(".image:not(:first-child) img").eachAttr("data-image-big")).toString();
      Float stockFloat = MathUtils.parseFloatWithComma(jsonSku.optString("StockBalance"));

      RatingsReviews reviews = scrapRating(document);
      reviews.setInternalId(internalId);
      int stock = stockFloat != null ? stockFloat.intValue() : 0;

      Offers offers = scrapOffers(jsonSku.optJSONObject("Price"));

      products.add(ProductBuilder.create()
          .setUrl(session.getOriginalURL())
          .setInternalId(internalId)
          .setInternalPid(internalPid)
          .setName(name)
          .setCategories(categories)
          .setPrimaryImage(document.selectFirst(".image.Image").attr("src"))
          .setSecondaryImages(secondaryImages)
          .setDescription(description)
          .setOffers(offers)
          .setRatingReviews(reviews)
          .setStock(stock)
          .build());
    }

    return products;
  }

  private RatingsReviews scrapRating(Document doc) {
    RatingsReviews reviews = new RatingsReviews();
    reviews.setAverageOverallRating(
        MathUtils.parseDoubleWithComma(doc.selectFirst(".rating-average").text()));
    reviews.setTotalRating(
        MathUtils.parseInt(doc.selectFirst("label[itemprop='reviewCount']").text()));
    reviews.setDate(session.getDate());
    return reviews;
  }

  private Offers scrapOffers(JSONObject priceJson)
      throws MalformedPricingException, OfferException {
    Offers offers = new Offers();

    double price = priceJson.optDouble("SalesPrice");
    double priceFrom = priceJson.optDouble("ListPrice");
    JSONObject installmentsJson = priceJson.optJSONObject("MaxInstallmentsNoInterest");
    List<CreditCard> cards = Sets
        .newHashSet(Card.VISA, Card.MASTERCARD, Card.AMEX, Card.ELO, Card.HIPERCARD, Card.DINERS)
        .stream().map(card -> {
          try {
            return CreditCardBuilder.create()
                .setIsShopCard(false)
                .setInstallments(new Installments(Sets.newHashSet(
                    InstallmentBuilder.create().setInstallmentPrice(price).setFinalPrice(price)
                        .setInstallmentNumber(1).build(),
                    InstallmentBuilder.create()
                        .setInstallmentPrice(installmentsJson.optDouble("InstallmentPrice"))
                        .setFinalPrice(installmentsJson.optDouble("RetailPrice"))
                        .setInstallmentNumber(installmentsJson.optInt("Installments")).build())))
                .setBrand(card.toString())
                .build();
          } catch (MalformedPricingException e) {
            logger.error(CommonMethods.getStackTrace(e));
            return null;
          }
        })
        .collect(Collectors.toList());

    Pricing pricing = PricingBuilder.create()
        .setPriceFrom(priceFrom != price ? priceFrom : null)
        .setSpotlightPrice(price)
        .setBankSlip(BankSlipBuilder.create()
            .setFinalPrice(price)
            .build())
        .setCreditCards(new CreditCards(cards))
        .build();

    String fullName = "Salonline";
    offers.add(OfferBuilder.create()
        .setUseSlugNameAsInternalSellerId(true)
        .setSellerFullName(fullName)
        .setMainPagePosition(1)
        .setIsBuybox(false)
        .setIsMainRetailer(Pattern.matches("(?i)^salonline\\s?$", fullName))
        .setPricing(pricing)
        .build());

    return offers;
  }
}
