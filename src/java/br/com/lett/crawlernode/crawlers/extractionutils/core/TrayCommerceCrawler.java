package br.com.lett.crawlernode.crawlers.extractionutils.core;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import models.Offer.OfferBuilder;
import models.Offers;
import models.RatingsReviews;
import models.pricing.BankSlip.BankSlipBuilder;
import models.pricing.CreditCard;
import models.pricing.CreditCard.CreditCardBuilder;
import models.pricing.CreditCards;
import models.pricing.Installment;
import models.pricing.Installment.InstallmentBuilder;
import models.pricing.Installments;
import models.pricing.Pricing.PricingBuilder;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

abstract public class TrayCommerceCrawler extends Crawler {

  private String sellerName = setSellerName();

  protected abstract String setSellerName();

  public TrayCommerceCrawler(Session session) {
    super(session);
  }

  private static final String IMAGES_API_PARAMETER = "variant_gallery";

  private static final String IMAGES_SELECTOR = "#carousel li a[href]:not(.cloud-zoom-gallery-video), .produto-imagem a";
  private static final String IMAGES_HOST = "#carousel li a[href]:not(.cloud-zoom-gallery-video)";

  @Override
  public List<Product> extractInformation(Document doc) throws Exception {
    super.extractInformation(doc);
    List<Product> products = new ArrayList<>();

    if (isProductPage(doc)) {

      JSONObject productJson = CrawlerUtils
          .selectJsonFromHtml(doc, "script", "dataLayer = [", "]", false, true);

      String storeId = CrawlerUtils
          .scrapStringSimpleInfoByAttribute(doc, "html", "data-store");

      String internalPid =
          productJson.has("idProduct") && !productJson.isNull("idProduct") ? productJson
              .get("idProduct").toString() : null;
      String description = CrawlerUtils.scrapSimpleDescription(doc, Arrays
          .asList("#descricao", "#garantia"));
      String name =
          productJson.has("nameProduct") && productJson.get("nameProduct") instanceof String
              ? productJson.getString("nameProduct") : null;
      RatingsReviews ratingReviews = scrapRatingsReviews(
          productJson.has("rating") && productJson.get("rating") instanceof JSONObject ? productJson
              .getJSONObject("rating") : null);

      JSONArray skus = JSONUtils.getJSONArrayValue(productJson, "listSku");
      List<String> categories = doc.select(".breadcrumb .breadcrumb-item:not(:first-child) a")
          .eachText();
      if (skus.length() > 0) {

        for (Object obj : skus) {
          JSONObject skuJson = obj instanceof JSONObject ? (JSONObject) obj : new JSONObject();

          if (skuJson.has("idSku") && !skuJson.isNull("idSku")) {
            String internalId = skuJson.get("idSku").toString();
            String variationId = CommonMethods.getLast(internalId.split("-"));
            String variationName =
                skuJson.has("nameSku") && !skuJson.isNull("nameSku") ? skuJson.get("nameSku")
                    .toString() : null;

            Document docImages = fetchVariationApi(internalPid, variationId, storeId);

            String primaryImage = getImage(doc);
            String secondaryImages = getSecondaryImg(docImages, primaryImage);

            String ean = JSONUtils.getStringValue(skuJson, "EAN");
            List<String> eans = ean != null ? Collections.singletonList(ean) : null;

            // Creating the product
            Product product = ProductBuilder.create()
                .setUrl(session.getOriginalURL())
                .setInternalId(internalId)
                .setInternalPid(internalPid)
                .setName(variationName != null ? name + " " + variationName : name)
                .setOffers(doc.selectFirst(".botao-comprar") != null ? scrapOffers(skuJson, storeId) : null)
                .setCategories(categories)
                .setPrimaryImage(primaryImage)
                .setSecondaryImages(secondaryImages)
                .setDescription(description)
                .setEans(eans)
                .setRatingReviews(ratingReviews)
                .build();

            products.add(product);
          }
        }
      } else {
        String primaryImage = getImage(doc);
        String secondaryImages = getSecondaryImg(doc, primaryImage);

        String ean = JSONUtils.getStringValue(productJson, "EAN");
        List<String> eans = ean != null ? Collections.singletonList(ean) : null;

        // Creating the product
        Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalPid)
            .setInternalPid(internalPid)
            .setName(name)
            .setOffers(
                doc.selectFirst(".botao-comprar") != null ? scrapOffers(productJson, storeId) : null)
            .setCategories(categories)
            .setPrimaryImage(primaryImage)
            .setSecondaryImages(secondaryImages)
            .setDescription(description)
            .setEans(eans)
            .setRatingReviews(ratingReviews)
            .build();

        products.add(product);
      }

    } else {
      Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
    }

    return products;
  }

  private String getSecondaryImg(Document docImages, String primaryImage) {
    return CrawlerUtils
        .scrapSimpleSecondaryImages(docImages, IMAGES_SELECTOR,
            Collections.singletonList("href"), "https", IMAGES_HOST,
            primaryImage);
  }

  private String getImage(Document doc) {
    return CrawlerUtils
        .scrapSimplePrimaryImage(doc, IMAGES_SELECTOR, Collections.singletonList("href"),
            "https", IMAGES_HOST);
  }

  private boolean isProductPage(Document doc) {
    return doc.selectFirst(".page-content #product-container") != null;
  }

  private Document fetchVariationApi(String internalPid, String variationId, String storeId) {
    Request request = RequestBuilder.create()
        .setUrl("https://www.petshopweb.com.br/mvc/store/product/"
            + TrayCommerceCrawler.IMAGES_API_PARAMETER + "/?loja=" + storeId + "&variant_id="
            + variationId + "&product_id=" + internalPid)
        .setCookies(cookies)
        .build();

    return Jsoup.parse(this.dataFetcher.get(session, request).getBody());
  }

  private Offers scrapOffers(JSONObject json, String storeId)
      throws MalformedPricingException, OfferException {

    Double price = json.optDouble("priceSell");
    Double priceFrom = json.optDouble("price");
    if (price.equals(priceFrom)) {
      priceFrom = null;
    }

    Offers offers = new Offers();

    JSONArray installmentsJson = JSONUtils.getJSONArrayValue(json, "priceSellDetails");
    Set<Installment> instsSet = new HashSet<>();
    for (Object obj : installmentsJson) {
      JSONObject installmentJson = (JSONObject) obj;

      int installmentNumber = installmentJson.optInt("installment.months", 0);

      double installmentPrice = installmentJson.optDouble("installment.amount", 0D);

      instsSet.add(InstallmentBuilder.create()
          .setInstallmentPrice(installmentPrice)
          .setInstallmentNumber(installmentNumber)
          .setFinalPrice(installmentPrice * installmentNumber).build());
    }
    Installments installments = new Installments(instsSet);
    List<CreditCard> creditCards = Sets
        .newHashSet(Card.VISA, Card.MASTERCARD, Card.ELO, Card.DINERS, Card.AMEX).stream()
        .map(card -> {
          try {
            return CreditCardBuilder.create().setBrand(card.toString())
                .setInstallments(installments).setIsShopCard(false).build();
          } catch (MalformedPricingException e) {
            throw new RuntimeException(e);
          }
        }).collect(Collectors.toList());

    offers.add(OfferBuilder.create()
        .setSellerFullName(sellerName)
        .setMainPagePosition(1)
        .setInternalSellerId(storeId)
        .setIsBuybox(false)
        .setIsMainRetailer(true)
        .setPricing(PricingBuilder.create().setCreditCards(new CreditCards(creditCards))
            .setBankSlip(BankSlipBuilder.create().setFinalPrice(price)
                .build())
            .setSpotlightPrice(price)
            .setPriceFrom(priceFrom)
            .build())
        .build());

    return offers;
  }

  private RatingsReviews scrapRatingsReviews(JSONObject json) {
    RatingsReviews ratingsReviews = new RatingsReviews();
    ratingsReviews.setDate(session.getDate());

    double avgRating = 0D;
    int numOfEval = 0;

    if (json != null) {
      avgRating = json.optDouble("average", 0D);
      numOfEval = json.optInt("count", 0);
    }

    ratingsReviews.setAverageOverallRating(avgRating);
    ratingsReviews.setTotalRating(numOfEval);

    return ratingsReviews;
  }
}
