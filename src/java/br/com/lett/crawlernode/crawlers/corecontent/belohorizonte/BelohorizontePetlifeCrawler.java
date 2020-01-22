package br.com.lett.crawlernode.crawlers.corecontent.belohorizonte;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
import br.com.lett.crawlernode.util.Pair;
import models.prices.Prices;

public class BelohorizontePetlifeCrawler extends Crawler {

  private static final String CDN_URL = "www.petlifebh.com.br";

  public BelohorizontePetlifeCrawler(Session session) {
    super(session);
  }

  @Override
  public List<Product> extractInformation(Document doc) throws Exception {
    super.extractInformation(doc);
    List<Product> products = new ArrayList<>();

    if (isProductPage(doc)) {
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

      String internalPid = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "[name=\"add-to-cart\"][value]", "value");
      String internalId = internalPid;
      String name = CrawlerUtils.scrapStringSimpleInfo(doc, ".product_title", false);
      Elements priceElem = doc.select(".entry-summary > p.price  span.woocommerce-Price-amount.amount");
      Float price = MathUtils.parseFloatWithDots(priceElem.last() != null ? priceElem.last().ownText() : null);
      Prices prices = scrapPrices(doc, price);
      CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".breadcrumb > li:not(.home):not([itemtype]) > span a", true);
      String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, ".product-images .img-thumbnail img",
          Arrays.asList("href", "content", "src"), "https", CDN_URL);
      String secondaryImages = CrawlerUtils.scrapSimpleSecondaryImages(doc, ".product-images .img-thumbnail img",
          Arrays.asList("href", "content", "src"), "https", CDN_URL, primaryImage);
      String description = CrawlerUtils.scrapElementsDescription(doc, Arrays.asList("#product-tab[aria-controls=\"tab-description\"]",
          "#product-tab[aria-controls=\"tab-additional_information\"]", "#tab-description", "#tab-additional_information"));
      boolean available = doc.selectFirst(".single_add_to_cart_button") != null;

      // Creating the product
      Product product = ProductBuilder.create()
          .setUrl(session.getOriginalURL())
          .setInternalId(internalId)
          .setInternalPid(internalPid)
          .setName(name)
          .setPrice(price)
          .setPrices(prices)
          .setAvailable(available)
          .setCategory1(categories.getCategory(0))
          .setCategory2(categories.getCategory(1))
          .setCategory3(categories.getCategory(2))
          .setPrimaryImage(primaryImage)
          .setSecondaryImages(secondaryImages)
          .setDescription(description)
          .build();

      String productsVarationsString = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, ".variations_form[data-product_variations]", "data-product_variations");
      if (productsVarationsString != null && !productsVarationsString.isEmpty()) {
        JSONArray variations = JSONUtils.stringToJsonArray(productsVarationsString);
        for (Object o : variations) {
          JSONObject skuJson = o instanceof JSONObject ? (JSONObject) o : new JSONObject();

          if (skuJson.optString("variation_id") instanceof String) {
            Product clone = product.clone();

            String variationId = skuJson.optString("variation_id");
            String variationName = scrapVariationName(clone.getName(), skuJson);
            Float priceVariation = JSONUtils.getFloatValueFromJSON(skuJson, "display_price", true);
            Object availabilityObj = JSONUtils.getValue(skuJson, "is_in_stock");
            boolean avaiabilityVariation = availabilityObj instanceof Boolean && ((Boolean) availabilityObj);
            Prices pricesVariation = scrapVariationPrices(priceVariation, skuJson);

            clone.setInternalId(variationId);
            clone.setName(variationName);
            clone.setPrice(priceVariation);
            clone.setAvailable(avaiabilityVariation);
            clone.setPrices(pricesVariation);

            products.add(clone);
          }
        }
      } else {
        products.add(product);
      }
    } else {
      Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
    }

    return products;
  }

  private boolean isProductPage(Document doc) {
    return doc.selectFirst(".product-summary-wrap") != null;
  }

  private String scrapVariationName(String originalName, JSONObject skuJson) {
    StringBuilder name = new StringBuilder();
    name.append(originalName);

    if (skuJson.has("attributes") && skuJson.get("attributes") instanceof JSONObject) {
      JSONObject attributes = skuJson.getJSONObject("attributes");

      for (String key : attributes.keySet()) {
        Object variationName = JSONUtils.getValue(attributes, key);
        if (variationName != null) {
          name.append(" ").append(variationName.toString());
        }
      }
    }

    return name.toString();
  }

  private Prices scrapPrices(Document doc, Float price) {
    Prices prices = new Prices();

    if (price != null) {
      prices.setBankTicketPrice(price);
      Element bankTicketElem = doc.select(".single > p > span.woocommerce-Price-amount.amount").last();

      Float bankPrice = MathUtils.parseFloatWithDots(bankTicketElem != null ? bankTicketElem.ownText() : null);
      if (bankPrice != null) {
        prices.setBankTicketPrice(bankPrice);
      }
      Double priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".entry-summary > p.price  span.woocommerce-Price-amount.amount", null, true, '.', session);
      if (Float.compare(price, priceFrom.floatValue()) != 0) {
        prices.setPriceFrom(priceFrom);
      }

      Map<Integer, Float> installmentPriceMap = new HashMap<>();
      installmentPriceMap.put(1, price);

      Pair<Integer, Float> installment = CrawlerUtils.crawlSimpleInstallment(".product-summary-wrap .fswp_installments_price .price", doc, false, "x", "com", true, '.');
      if (!installment.isAnyValueNull()) {
        installmentPriceMap.put(installment.getFirst(), installment.getSecond());
      }

      prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.HIPERCARD.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.AMEX.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.ELO.toString(), installmentPriceMap);
    }

    return prices;
  }

  private Prices scrapVariationPrices(Float price, JSONObject skuJson) {
    Prices prices = new Prices();

    if (price != null) {
      prices.setBankTicketPrice(price);
      prices.setPriceFrom(JSONUtils.getDoubleValueFromJSON(skuJson, "display_regular_price", true));

      Map<Integer, Float> installmentPriceMap = new TreeMap<>();
      installmentPriceMap.put(1, price);

      prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.HIPERCARD.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.AMEX.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.ELO.toString(), installmentPriceMap);
    }

    return prices;
  }
}
