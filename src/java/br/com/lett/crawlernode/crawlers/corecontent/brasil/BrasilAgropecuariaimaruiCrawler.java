package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
import br.com.lett.crawlernode.util.Pair;
import com.google.common.collect.Sets;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.Offer.OfferBuilder;
import models.Offers;
import models.prices.Prices;
import models.pricing.BankSlip.BankSlipBuilder;
import models.pricing.CreditCards;
import models.pricing.Installment.InstallmentBuilder;
import models.pricing.Installments;
import models.pricing.Pricing;
import models.pricing.Pricing.PricingBuilder;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.util.*;

public class BrasilAgropecuariaimaruiCrawler extends Crawler {

  private static final String HOME_PAGE = "https://agropecuariaimarui.com.br/";
  private final String MAIN_SELLER_NAME = "Agropecuária Imaruí";
  private Set<String> cards = Sets.newHashSet(Card.MASTERCARD.toString(),
      Card.VISA.toString(), Card.ELO.toString(), Card.AMEX.toString(), Card.DINERS.toString(),
      Card.DISCOVER.toString(), Card.AURA.toString(), Card.UNKNOWN_CARD.toString(), Card.HIPERCARD.toString());

  public BrasilAgropecuariaimaruiCrawler(Session session) {
    super(session);
  }

  @Override
  public List<Product> extractInformation(Document doc) throws Exception {
    super.extractInformation(doc);
    List<Product> products = new ArrayList<>();

    if (isProductPage(doc)) {
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

      JSONObject skuJson = CrawlerUtils.selectJsonFromHtml(doc, "script[type=\"application/ld+json\"]", null, null, false, true);
      JSONArray graphJsonArray = skuJson != null &&
          skuJson.has("@graph") &&
          !skuJson.isNull("@graph")
          ? skuJson.getJSONArray("@graph")
          : new JSONArray();

      if (graphJsonArray.length() > 1 && graphJsonArray.get(1) instanceof JSONObject) {
        skuJson = graphJsonArray.getJSONObject(1);
      } else {
        skuJson = null;
      }

      String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "input#comment_post_ID", "value");
      String internalPid = skuJson != null && skuJson.has("sku") ? skuJson.get("sku").toString() : null;
      String name = skuJson != null && skuJson.has("name") ? skuJson.get("name").toString() : null;
      CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".product-info .woocommerce-breadcrumb a[href]", true);
      String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, ".woocommerce-product-gallery figure img", Arrays.asList("src"), "https:", HOME_PAGE);
      String secondaryImages = null;
      String description = CrawlerUtils.scrapElementsDescription(doc, Arrays.asList(".product-short-description p", ".product-footer .tab-panels #tab-description"));
      Offers offers = scrapOffers(doc);

      // Creating the product
      Product product = ProductBuilder.create()
          .setUrl(session.getOriginalURL())
          .setInternalId(internalId)
          .setInternalPid(internalPid)
          .setName(name)
          .setCategory1(categories.getCategory(0))
          .setCategory2(categories.getCategory(1))
          .setCategory3(categories.getCategory(2))
          .setPrimaryImage(primaryImage)
          .setSecondaryImages(secondaryImages)
          .setDescription(description)
          .setOffers(offers)
          .build();

      Element variationElement = doc.selectFirst(".product-container .product-info form.variations_form");
      JSONArray variationJsonArray = variationElement != null && variationElement.hasAttr("data-product_variations")
          ? new JSONArray(variationElement.attr("data-product_variations"))
          : null;

      if (variationJsonArray != null && variationJsonArray.length() > 0) {
        for (Object obj : variationJsonArray) {
          JSONObject json = (JSONObject) obj;

          Product clone = product.clone();
          clone.setInternalId(internalId + "-" + json.get("variation_id").toString());
          clone.setName(scrapVariationName(json, name));
          clone.setPrice(scrapVariationPrice(json));
          clone.setPrices(scrapVariationPrices(json, clone.getPrice()));
          clone.setStock(null);
          clone.setAvailable(scrapVariationAvailability(json));

          products.add(clone);

        }
      } else {
        products.add(product);
      }
    } else {
      Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
    }

    return products;
  }

  private Offers scrapOffers(Document doc) throws OfferException, MalformedPricingException {
    Offers offers = new Offers();
    Pricing pricing = scrapPricing(doc);

    if (pricing != null) {
      offers.add(OfferBuilder.create()
          .setUseSlugNameAsInternalSellerId(true)
          .setSellerFullName(MAIN_SELLER_NAME)
          .setSellersPagePosition(1)
          .setIsBuybox(false)
          .setIsMainRetailer(true)
          .setPricing(pricing)
          .build());
    }

    return offers;
  }

  private Pricing scrapPricing(Document doc) throws MalformedPricingException {
    Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".price-wrapper .product-page-price :not(:first-child) .woocommerce-Price-amount", null, true, ',', session);
    if (spotlightPrice == null) {
      spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".price-wrapper .product-page-price .woocommerce-Price-amount", null, true, ',', session);
    }

    if (spotlightPrice != null) {
      Double priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".price-wrapper .price del .amount", null, true, ',', session);
      CreditCards creditCards = scrapCreditCards(doc, spotlightPrice);
      Double finalBankSlip = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".wc-simulador-parcelas-offer .woocommerce-Price-amount.amount", null, true, ',', session);

      return PricingBuilder.create()
          .setSpotlightPrice(spotlightPrice)
          .setPriceFrom(priceFrom)
          .setCreditCards(creditCards)
          .setBankSlip(BankSlipBuilder.create().setFinalPrice(finalBankSlip != null ? finalBankSlip : spotlightPrice).build())
          .build();
    }

    return null;
  }

  private CreditCards scrapCreditCards(Document doc, Double spotlightPrice) throws MalformedPricingException {
    CreditCards creditCards = new CreditCards();

    Installments installments = new Installments();
    installments.add(InstallmentBuilder.create()
        .setInstallmentNumber(1)
        .setInstallmentPrice(spotlightPrice)
        .build());

    for (Element e : doc.select(".product-info .wc-simulador-parcelas-payment-options li")) {
      Pair<Integer, Float> installment = CrawlerUtils.crawlSimpleInstallment(null, e, false, "x de", "juros", true, ',');

      if (!installment.isAnyValueNull()) {
        installments.add(InstallmentBuilder.create()
            .setInstallmentNumber(installment.getFirst())
            .setInstallmentPrice(MathUtils.normalizeTwoDecimalPlaces(installment.getSecond().doubleValue()))
            .build());
      }
    }

    return creditCards;
  }

  private boolean isProductPage(Document doc) {
    return doc.selectFirst(".product-container .product-main") != null;
  }

  private String scrapVariationName(JSONObject json, String name) {

    if (json.has("attributes") && !json.isNull("attributes") && json.get("attributes") instanceof JSONObject) {
      json = json.getJSONObject("attributes");

      for (String key : json.keySet()) {
        if (!json.isNull(key) && json.get(key) instanceof String) {
          name += " - " + json.getString(key);
        }
      }
    }

    return name;
  }

  private boolean scrapVariationAvailability(JSONObject json) {
    if (json.has("is_in_stock") && json.get("is_in_stock") instanceof Boolean) {
      return json.getBoolean("is_in_stock");
    }

    return false;
  }

  private Float scrapVariationPrice(JSONObject json) {
    Float price = null;

    if (json.has("display_price") && !json.isNull("display_price") &&
        (json.get("display_price") instanceof Float || json.get("display_price") instanceof Double)) {

      price = json.getFloat("display_price");
    }

    return price;
  }

  private Prices scrapVariationPrices(JSONObject json, Float price) {
    Prices prices = new Prices();

    if (price != null && json.has("price_html") && json.get("price_html") instanceof String) {
      Document doc = Jsoup.parse(json.getString("price_html"));

      prices.setBankTicketPrice(CrawlerUtils.scrapFloatPriceFromHtml(doc, ".wc-simulador-parcelas-offer > .amount", null, false, ',', session));

      Map<Integer, Float> installmentPriceMap = new TreeMap<>();
      installmentPriceMap.put(1, price);

      Pair<Integer, Float> installment = CrawlerUtils.crawlSimpleInstallment(
          ".wc-simulador-parcelas-parcelamento-info-container .wc-simulador-parcelas-parcelamento-info",
          doc, false, "x de", "juros", true);

      if (!installment.isAnyValueNull()) {
        installmentPriceMap.put(installment.getFirst(), installment.getSecond());
      }

      prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.ELO.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.AMEX.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.DINERS.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.DISCOVER.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.AURA.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.UNKNOWN_CARD.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.HIPERCARD.toString(), installmentPriceMap);
    }

    return prices;
  }
}
