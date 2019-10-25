package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
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
import models.prices.Prices;

public class BrasilViannapetCrawler extends Crawler {

  public BrasilViannapetCrawler(Session session) {
    super(session);
  }

  @Override
  public List<Product> extractInformation(Document doc) throws Exception {
    super.extractInformation(doc);
    List<Product> products = new ArrayList<>();

    if (isProductPage(doc)) {
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

      String internalPid = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "#itemId", "value");
      String name = CrawlerUtils.scrapStringSimpleInfo(doc, "h1.product_name", true);
      Float price = CrawlerUtils.scrapFloatPriceFromHtml(doc, "#product_price .money", null, true, ',', session);
      Prices prices = scrapPrices(price);
      CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".breadcrumb li:not(.firstItem):not(.lastItem) a");
      String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, "#product_image li a", Arrays.asList("href"), "https",
          "static.mercadoshops.com");
      String secondaryImages = CrawlerUtils.scrapSimpleSecondaryImages(doc, "#product_image li a", Arrays.asList("href"), "https",
          "static.mercadoshops.com", primaryImage);
      String description = CrawlerUtils.scrapSimpleDescription(doc, Arrays.asList("#product_description"));
      Integer stock = CrawlerUtils.scrapIntegerFromHtmlAttr(doc, "#itemStock", "value", 0);
      boolean available = stock > 0;

      // Creating the product
      Product product = ProductBuilder.create()
          .setUrl(session.getOriginalURL())
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
          .setStock(stock)
          .build();

      Elements variations = doc.select("#product-select option");
      if (!variations.isEmpty()) {
        for (Element e : variations) {
          Product clone = product.clone();
          clone.setInternalId(e.val());
          clone.setName(clone.getName() + " " + e.ownText());

          products.add(clone);
        }
      } else {
        Product clone = product.clone();
        clone.setInternalId(internalPid);

        products.add(clone);
      }
    } else {
      Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
    }

    return products;
  }

  private boolean isProductPage(Document doc) {
    return doc.selectFirst("#itemId") != null;
  }

  private Prices scrapPrices(Float price) {
    Prices prices = new Prices();

    if (price != null) {
      prices.setBankTicketPrice(price);

      Request request = RequestBuilder.create()
          .setUrl("https://www.viannapet.com/paymentmethods?price=" + price)
          .setCookies(cookies)
          .build();
      JSONArray priceApiArray = JSONUtils.stringToJsonArray(this.dataFetcher.get(session, request).getBody());
      for (Object o : priceApiArray) {
        JSONObject paymentMethodJson = o instanceof JSONObject ? (JSONObject) o : new JSONObject();

        Card card = scrapCard(paymentMethodJson);
        if (card != null) {
          Map<Integer, Float> installmentPriceMap = new TreeMap<>();
          installmentPriceMap.put(1, price);

          JSONArray installmentsArray = JSONUtils.getJSONArrayValue(paymentMethodJson, "payer_costs");
          for (Object obj : installmentsArray) {
            JSONObject installmentJson = obj instanceof JSONObject ? (JSONObject) obj : new JSONObject();

            Integer installmentNumber = JSONUtils.getIntegerValueFromJSON(installmentJson, "installments", null);
            if (installmentNumber != null) {
              Float interestFee = JSONUtils.getFloatValueFromJSON(installmentJson, "installment_rate", true);
              Float installmentFinalPrice = price;

              if (interestFee != null && interestFee > 0) {
                installmentFinalPrice = MathUtils.normalizeTwoDecimalPlaces(price + (price * (interestFee / 100f)));
              }

              installmentPriceMap.put(installmentNumber, MathUtils.normalizeTwoDecimalPlaces(installmentFinalPrice) / installmentNumber);
            }
          }

          prices.insertCardInstallment(card.toString(), installmentPriceMap);
        }
      }
    }

    return prices;
  }

  private Card scrapCard(JSONObject paymentMethodJson) {
    Card card = null;

    String paymentName = JSONUtils.getStringValue(paymentMethodJson, "id");
    if (paymentName != null) {
      if (paymentName.equalsIgnoreCase("visa")) {
        card = Card.VISA;
      } else if (paymentName.equalsIgnoreCase("master")) {
        card = Card.MASTERCARD;
      } else if (paymentName.equalsIgnoreCase("amex")) {
        card = Card.AMEX;
      } else if (paymentName.equalsIgnoreCase("elo")) {
        card = Card.ELO;
      } else if (paymentName.equalsIgnoreCase("hipercard")) {
        card = Card.HIPERCARD;
      }
    }

    return card;
  }
}
