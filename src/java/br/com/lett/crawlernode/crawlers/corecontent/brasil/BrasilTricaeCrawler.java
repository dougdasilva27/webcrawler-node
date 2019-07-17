package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
import exceptions.IllegalSellerValueException;
import exceptions.OfferException;
import models.Marketplace;
import models.Offer.OfferBuilder;
import models.Offers;
import models.Seller.SellerBuilder;
import models.prices.Prices;

/**
 * Date: 09/07/2019
 * 
 * @author Gabriel Dornelas
 *
 */
public class BrasilTricaeCrawler extends Crawler {

  private static final String IMAGES_HOST = "dafitistatic-a.akamaihd.net";
  private static final String PRODUCT_API_URL = "https://www.tricae.com.br/catalog/detailJson?sku=";
  private static final String SELLER_NAME_LOWER = "tricae";

  public BrasilTricaeCrawler(Session session) {
    super(session);
    super.config.setFetcher(FetchMode.FETCHER);
  }

  @Override
  public List<Product> extractInformation(Document doc) throws Exception {
    super.extractInformation(doc);
    List<Product> products = new ArrayList<>();

    if (isProductPage(doc)) {
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

      String internalPid = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "input[name=p]", "value");
      String description = CrawlerUtils.scrapSimpleDescription(doc, Arrays.asList(".box-description", ".box-informations"));
      String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, "#gallery li.gallery-items > a", Arrays.asList("data-img-zoom", "href"),
          "https", IMAGES_HOST);
      String secondaryImages = CrawlerUtils.scrapSimpleSecondaryImages(doc, "#gallery li.gallery-items > a", Arrays.asList("data-img-zoom", "href"),
          "https", IMAGES_HOST, primaryImage);
      String productMainName = CrawlerUtils.scrapStringSimpleInfo(doc, "h1.product-name", true);
      String sellerName = scrapSellerName(doc);
      boolean sellByOwnStore = SELLER_NAME_LOWER.equalsIgnoreCase(sellerName);

      JSONObject productApi = crawlProductAPI(internalPid);
      Prices prices = scrapPrices(productApi);
      Float price = CrawlerUtils.extractPriceFromPrices(prices, Card.VISA);
      Marketplace marketplace = scrapMarketplace(sellerName, prices, price);
      Offers offers = scrapOffers(doc, price, sellerName);

      JSONArray productsArray = productApi.has("sizes") && !productApi.isNull("sizes") ? productApi.getJSONArray("sizes") : new JSONArray();

      for (Object obj : productsArray) {
        JSONObject skuJson = (JSONObject) obj;

        String internalId = crawlInternalId(skuJson);
        String name = crawlName(skuJson, productMainName);
        Integer stock = CrawlerUtils.getIntegerValueFromJSON(skuJson, "stock", null);

        // Price on this market is the same for all variations
        // Avaiability is for variation
        boolean buyable = stock != null && stock > 0;
        boolean availabilityVariation = buyable && sellByOwnStore;
        Marketplace marketplaceVariation = buyable ? marketplace : new Marketplace();
        Offers offersVarition = buyable ? offers : new Offers();
        Prices pricesVariation = availabilityVariation ? prices : new Prices();
        Float priceVariation = availabilityVariation ? price : null;

        Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setInternalPid(internalPid)
            .setName(name)
            .setPrimaryImage(primaryImage)
            .setSecondaryImages(secondaryImages)
            .setDescription(description)
            .setMarketplace(marketplaceVariation)
            .setAvailable(availabilityVariation)
            .setPrice(priceVariation)
            .setPrices(pricesVariation)
            .setOffers(offersVarition)
            .setStock(stock)
            .build();

        products.add(product);
      }

    } else {
      Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
    }

    return products;

  }

  private boolean isProductPage(Document doc) {
    return !doc.select(".product-page").isEmpty();
  }

  private String crawlInternalId(JSONObject skuJson) {
    String internalId = null;

    if (skuJson.has("sku") && !skuJson.isNull("sku")) {
      internalId = skuJson.get("sku").toString();
    }

    return internalId;
  }

  private JSONObject crawlProductAPI(String internalPid) {
    String url = PRODUCT_API_URL + internalPid;
    Request request = RequestBuilder.create().setUrl(url).setCookies(cookies).build();

    return CrawlerUtils.stringToJson(this.dataFetcher.get(session, request).getBody());
  }

  private String crawlName(JSONObject skuJson, String productMainName) {
    String name = productMainName;

    if (skuJson.has("name") && !skuJson.isNull("name")) {
      name += " Tamanho " + skuJson.get("name").toString();
    }

    return name;
  }

  private Marketplace scrapMarketplace(String sellerName, Prices prices, Float price) {
    Marketplace marketplace = new Marketplace();

    if (!SELLER_NAME_LOWER.equalsIgnoreCase(sellerName)) {
      try {
        marketplace.add(new SellerBuilder()
            .setName(sellerName.toLowerCase())
            .setPrices(prices)
            .setPrice(MathUtils.normalizeTwoDecimalPlaces(price.doubleValue()))
            .build());
      } catch (IllegalSellerValueException e) {
        Logging.printLogError(logger, session, CommonMethods.getStackTrace(e));
      }
    }

    return marketplace;
  }

  private Offers scrapOffers(Document doc, Float price, String sellerName) {
    Offers offers = new Offers();

    String sellerId = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "input[name=seller]", "value");
    if (sellerId != null) {
      try {
        offers.add(new OfferBuilder()
            .setInternalSellerId(sellerId)
            .setMainPagePosition(1)
            .setSellerFullName(sellerName)
            .setMainPrice(MathUtils.normalizeTwoDecimalPlaces(price.doubleValue()))
            .setSlugSellerName(CrawlerUtils.toSlug(sellerName))
            .setIsBuybox(false)
            .build());
      } catch (OfferException e) {
        Logging.printLogError(logger, session, CommonMethods.getStackTrace(e));
      }
    }

    return offers;
  }

  /**
   * Scrap seller name from this text: "Vendido e entregue por Kyly"
   * 
   * Where kyly is the seller name
   * 
   * @param doc
   * @return
   */
  private String scrapSellerName(Document doc) {
    String sellerName = null;

    String sellerText = CrawlerUtils.scrapStringSimpleInfo(doc, ".product-seller-name", false);
    if (sellerText != null && sellerText.contains("por")) {
      sellerName = CommonMethods.getLast(sellerText.split("por")).trim();
    }

    return sellerName;
  }

  private Prices scrapPrices(JSONObject productJson) {
    Prices prices = new Prices();

    Float price = CrawlerUtils.getFloatValueFromJSON(productJson, "price", false, true);
    if (price != null) {
      Map<Integer, Float> installmentsMap = new HashMap<>();
      installmentsMap.put(1, price);
      prices.setBankTicketPrice(price);
      prices.setPriceFrom(CrawlerUtils.getDoubleValueFromJSON(productJson, "specialPrice", false, true));

      if (productJson.has("installments")) {
        JSONObject installments = productJson.getJSONObject("installments");

        Integer installment = CrawlerUtils.getIntegerValueFromJSON(installments, "count", null);
        Float value = CrawlerUtils.getFloatValueFromJSON(installments, "value", false, true);

        if (installment != null && value != null) {
          installmentsMap.put(installment, value);
        }
      }

      prices.insertCardInstallment(Card.VISA.toString(), installmentsMap);
      prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentsMap);
      prices.insertCardInstallment(Card.DINERS.toString(), installmentsMap);
      prices.insertCardInstallment(Card.ELO.toString(), installmentsMap);
      prices.insertCardInstallment(Card.AMEX.toString(), installmentsMap);
      prices.insertCardInstallment(Card.HIPERCARD.toString(), installmentsMap);
    }

    return prices;
  }
}
