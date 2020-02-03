package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import exceptions.OfferException;
import models.*;
import models.Offer.OfferBuilder;
import models.prices.Prices;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 
 * @author samirleao
 * @author gabriel (refactor) 06/06/17
 *
 */

public class BrasilMagazineluizaCrawler extends Crawler {

  private static final String HOME_PAGE = "https://www.magazineluiza.com.br/";
  private static final String SELLER_NAME = "magazine luiza";

  public BrasilMagazineluizaCrawler(Session session) {
    super(session);
    this.config.setMustSendRatingToKinesis(true);
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

    if (isProductPage(doc)) {
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

      products.add(crawlProduct(doc));
    } else {
      Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
    }

    return products;
  }

  public Product crawlProduct(Document doc) {
    // Sku info in json on html
    JSONObject skuJsonInfo = crawlFullSKUInfo(doc);

    // InternalId
    String internalId = crawlInternalId(skuJsonInfo);

    // InternalPid
    String internalPid = internalId;

    // Product name
    String frontPageName = crawlNameFrontPage(doc, internalId);

    // Categories
    CategoryCollection categories = crawlCategories(doc);

    // Primary Image
    String primaryImage = crawlPrimaryImage(doc);

    // Secondary Images
    String secondaryImages = crawlSecondaryImages(doc, primaryImage);

    // Estoque
    Integer stock = null;

    // Marketplace
    Marketplace marketplace = crawlMarketPlace(doc, skuJsonInfo);

    // Availability
    boolean available = crawlAvailability(doc, marketplace, internalId);

    // Price
    Float price = available ? CrawlerUtils.getFloatValueFromJSON(skuJsonInfo, "priceTemplate", false, true) : null;

    // Prices
    Prices prices = crawlPrices(price, doc, skuJsonInfo);

    // Description
    String description = crawlDescription(doc, internalId);

    // Offers
    Offers offers = available || !marketplace.isEmpty() ? scrapBuyBox(doc) : new Offers();
    
    // RatingsReviews
    RatingsReviews ratingReviews = crawlRatingNew(doc, internalId);

    // Creating the product
    return ProductBuilder.create()
        .setUrl(session.getOriginalURL())
        .setInternalId(internalId)
        .setInternalPid(internalPid)
        .setName(frontPageName)
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
        .setMarketplace(marketplace)
        .setOffers(offers)
        .setRatingReviews(ratingReviews)
        .build();
  }

  private Offers scrapBuyBox(Document doc) {
    Offers offers = new Offers();
    try {
      Element sellerInfo = doc.selectFirst(".seller__indentifier .seller__indentifier-magazine");
      Element priceInfo = doc.selectFirst("meta[itemprop=\"price\"]");
      String sellerFullName = null;
      String slugSellerName = null;
      String internalSellerId = null;
      Double mainPrice = null;

      if (sellerInfo == null) {
        sellerInfo = doc.selectFirst(".seller__indentifier .seller-info-button");
      }

      if (sellerInfo != null) {
        sellerFullName = sellerInfo.text();
        slugSellerName = CrawlerUtils.toSlug(sellerFullName);
        // This market hasn't seller id, then the slug must be this.
        // I don't think that cnpj can be a good seller id.
        internalSellerId = slugSellerName;
      }

      if (priceInfo != null) {
        mainPrice = MathUtils.parseDoubleWithDot(priceInfo.attr("content"));
      }

      Offer offer = new OfferBuilder().setSellerFullName(sellerFullName).setSlugSellerName(slugSellerName).setInternalSellerId(internalSellerId)
          .setMainPagePosition(1).setIsBuybox(false).setMainPrice(mainPrice).build();

      offers.add(offer);
    } catch (OfferException e) {
      Logging.printLogError(logger, session, CommonMethods.getStackTrace(e));
    }


    return offers;
  }

  /*******************************
   * Product page identification *
   *******************************/
  private boolean isProductPage(Document doc) {
    return doc.select("h1[itemprop=name]").first() != null;
  }

  /**
   * Crawl Internal ID
   * 
   * @param doc
   * @return
   */
  private String crawlInternalId(JSONObject skuJson) {
    String internalId = null;

    if (skuJson.has("sku") && !skuJson.isNull("sku")) {
      internalId = skuJson.get("sku").toString();
    }

    return internalId;
  }

  /**
   * Crawl name in front page
   * 
   * @param doc
   * @return
   */
  private String crawlNameFrontPage(Document doc, String id) {
    String name = null;
    Element elementName = doc.select("h1[itemprop=name]").first();

    if (elementName != null) {
      name = elementName.text();
    }

    Elements variations = doc.select(".input__select.information-values__variation-select option");

    for (Element e : variations) {
      String idV = e.val();

      if (idV.equals(id)) {
        String variationName = e.ownText().trim();

        if (variationName.contains("-")) {
          variationName = variationName.split("-")[0];
        }

        name += " - " + variationName;

        break;
      }
    }

    return name;
  }

  /**
   * Crawl Description
   * 
   * @param doc
   * @return
   */
  private String crawlDescription(Document doc, String internalId) {
    StringBuilder description = new StringBuilder();

    Element elementDescription = doc.select(".factsheet-main-container").first();
    Element anchorDescription = doc.select("#anchor-description").first();

    if (elementDescription != null) {
      description.append(elementDescription.html());
    }

    if (anchorDescription != null) {
      description.append(anchorDescription.html());
    }

    // String descriptionURL = "http://www.magazineluiza.com.br/produto/ficha-tecnica/" + internalId +
    // "/";
    // description.append(DataFetcher.fetchString("GET", session, descriptionURL, null, cookies));

    return CommonMethods.stripNonValidXMLOrHTMLCharacters(description.toString());
  }

  /**
   * 
   * @param doc
   * @return
   */
  private String crawlPrimaryImage(Document doc) {
    String primaryImage = null;

    Element image = doc.select(".showcase-product__big-img").first();

    if (image != null) {
      primaryImage = image.attr("src").trim();
    }

    if (primaryImage == null) {
      Element primaryImageElement = doc.select(".product-thumbs-carousel__column img").first();

      if (primaryImageElement != null) {
        primaryImage = primaryImageElement.attr("src").replace("88x66", "618x463");
      }
    }

    if (primaryImage == null) {
      Element primaryImageElement = doc.select(".unavailable__product-img").first();

      if (primaryImageElement != null) {
        primaryImage = primaryImageElement.attr("src").replace("88x66", "618x463");
      }
    }

    return primaryImage;
  }

  /**
   * 
   * @param doc
   * @return
   */
  private String crawlSecondaryImages(Document doc, String primaryImage) {
    String secondaryImages = null;
    JSONArray secondaryImagesArray = new JSONArray();

    Elements imageThumbs = doc.select(".showcase-product__container-thumbs .showcase-product__thumbs img");
    Elements imageThumbsSpecial = doc.select("img.product-thumbs-carousel__thumb");

    if (imageThumbs.size() > imageThumbsSpecial.size()) {
      for (int i = 1; i < imageThumbs.size(); i++) { // starts with index 1, because the first image
        // is the primary image
        Element e = imageThumbs.get(i);

        String image = e.attr("src").replace("88x66", "618x463");

        if (!image.equalsIgnoreCase(primaryImage)) {
          secondaryImagesArray.put(image);
        }

      }
    } else {

      for (int i = 1; i < imageThumbsSpecial.size(); i++) { // starts with index 1, because the
        // first image is the primary image
        Element e = imageThumbsSpecial.get(i);

        String image = e.attr("src").replace("88x66", "618x463");

        if (!image.equalsIgnoreCase(primaryImage)) {
          secondaryImagesArray.put(image);
        }

      }
    }

    if (secondaryImagesArray.length() > 0) {
      secondaryImages = secondaryImagesArray.toString();
    }

    return secondaryImages;
  }

  /**
   * Crawl categories
   * 
   * @param document
   * @return
   */
  private CategoryCollection crawlCategories(Document document) {
    CategoryCollection categories = new CategoryCollection();
    Elements elementCategories = document.select(".breadcrumb__title > a.breadcrumb__item");

    for (int i = 0; i < elementCategories.size(); i++) {
      categories.add(elementCategories.get(i).text().trim());
    }

    return categories;
  }

  /**
   * Crawl marketplace
   * 
   * When pass skuInfo = null, the method crawl price variation
   * 
   * @param doc
   * @param skuInfo
   * @param idForPrice
   * @return
   */
  private Marketplace crawlMarketPlace(Document doc, JSONObject skuInfo) {
    Marketplace marketplace = new Marketplace();
    Element marketplaceName = doc.select(".seller__indentifier meta[itemprop=name]").first();

    if (marketplaceName != null) {
      String sellerName = marketplaceName.attr("content").trim();
      if (!sellerName.equalsIgnoreCase(SELLER_NAME)
          && (skuInfo == null || (skuInfo.has("priceTemplate")))) {
        Float sellerPrice = CrawlerUtils.getFloatValueFromJSON(skuInfo, "priceTemplate", false, true);

        JSONObject sellerJSON = new JSONObject();
        sellerJSON.put("name", sellerName);
        sellerJSON.put("price", sellerPrice);
        sellerJSON.put("prices", crawlPrices(sellerPrice, doc, skuInfo).toJSON());

        try {
          Seller seller = new Seller(sellerJSON);
          marketplace.add(seller);
        } catch (Exception e) {
          Logging.printLogError(logger, session, Util.getStackTraceString(e));
        }
      }
    }

    return marketplace;
  }

  /**
   * if has element in html and has no marketplace
   * 
   * @param doc
   * @param marketplace
   * @return
   */
  private boolean crawlAvailability(Document doc, Marketplace marketplace, String internalId) {
    Element elementAvailable = doc.selectFirst(".button__buy-product-detail");

    if (elementAvailable != null && marketplace.isEmpty()) {
      String data = elementAvailable.attr("data-product");

        return data.contains(internalId);
    }

    return false;
  }

  /**
   * @param internalId
   * @param price
   * @return
   */
  private Prices crawlPrices(Float price, Document doc, JSONObject skuInfo) {
    Prices prices = new Prices();

    if (price != null) {
      Map<Integer, Float> installmentsPriceMap = new HashMap<>();
      Map<Integer, Float> installmentsPriceMapShop = new HashMap<>();

      prices.setBankTicketPrice(CrawlerUtils.getDoubleValueFromJSON(skuInfo, "bestPriceTemplate", false, true));

      if (prices.getBankTicketPrice() == null) {
        prices.setBankTicketPrice(price);
      }

      Elements luizaCard = doc.select(".method-payment__card-luiza-box ul[class^=method-payment__values--] li > p");

      for (Element e : luizaCard) {
        String text = e.ownText();

        if (text.contains("x")) {
          int x = text.indexOf('x') + 1;

          Integer installment = Integer.parseInt(text.substring(0, x).replaceAll("[^0-9]", ""));
          Float installmentValue = MathUtils.parseFloatWithComma(text.substring(x));

          installmentsPriceMapShop.put(installment, installmentValue);
        } else if (!installmentsPriceMap.containsKey(1)) {
          Float installmentValue = MathUtils.parseFloatWithComma(text);
          installmentsPriceMapShop.put(1, installmentValue);
        }
      }

      Elements normalCards = doc.select(".method-payment__card-box .method-payment__values--general-cards li > p");

      for (Element e : normalCards) {
        String text = e.ownText();

        if (text.contains("x")) {
          int x = text.indexOf('x') + 1;

          Integer installment = Integer.parseInt(text.substring(0, x).replaceAll("[^0-9]", ""));
          Float installmentValue = MathUtils.parseFloatWithComma(text.substring(x));

          installmentsPriceMap.put(installment, installmentValue);
        } else if (!installmentsPriceMap.containsKey(1)) {
          Float installmentValue = MathUtils.parseFloatWithComma(text);
          installmentsPriceMap.put(1, installmentValue);
        }
      }

      if (installmentsPriceMap.isEmpty()) {
        installmentsPriceMap.put(1, price);
      }

      if (installmentsPriceMapShop.isEmpty()) {
        installmentsPriceMapShop = installmentsPriceMap;
      }

      prices.insertCardInstallment(Card.SHOP_CARD.toString(), installmentsPriceMapShop);
      prices.insertCardInstallment(Card.VISA.toString(), installmentsPriceMap);
      prices.insertCardInstallment(Card.AMEX.toString(), installmentsPriceMap);
      prices.insertCardInstallment(Card.DINERS.toString(), installmentsPriceMap);
      prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentsPriceMap);
      prices.insertCardInstallment(Card.HIPERCARD.toString(), installmentsPriceMap);
      prices.insertCardInstallment(Card.ELO.toString(), installmentsPriceMap);
      prices.insertCardInstallment(Card.AURA.toString(), installmentsPriceMap);
    }

    return prices;
  }

  /**
   * @param document
   * @return a json object containing all sku informations in this page.
   */
  private JSONObject crawlFullSKUInfo(Document document) {
    JSONObject skuJson = new JSONObject();

    String dataProduct = CrawlerUtils.scrapStringSimpleInfoByAttribute(document, ".js-header-product[data-product]", "data-product");
    if (dataProduct != null) {
      JsonObject jsonObject = new JsonObject();

      // We use Gson in this case because this json has duplicate keys
      // Gson unify those values
      try {
        jsonObject = (new JsonParser()).parse(dataProduct).getAsJsonObject();
      } catch (JsonSyntaxException e) {
        Logging.printLogError(logger, session, CommonMethods.getStackTrace(e));
      }

      skuJson = CrawlerUtils.stringToJson(jsonObject.toString());
    }

    return skuJson;
  }
  
  public RatingsReviews crawlRatingNew(Document doc, String internalId) {
      
    RatingsReviews ratingReviews = crawlRatingReviews(doc);
    ratingReviews.setInternalId(internalId);
      
    return ratingReviews;
  }
  
  private RatingsReviews crawlRatingReviews(Document doc) {
    RatingsReviews ratingReviews = new RatingsReviews();

    ratingReviews.setDate(session.getDate());

    ratingReviews.setTotalRating(getTotalReviewCount(doc));
    ratingReviews.setAverageOverallRating(getAverageOverallRating(doc));

    return ratingReviews;
  }

  private Integer getTotalReviewCount(Document doc) {
    Integer totalReviewCount = null;
    Element total = doc.select(".interaction-client__rating-info > span").last();
      
    if (total != null) {
      totalReviewCount = Integer.parseInt(total.ownText().replaceAll("[^0-9]", ""));
    }
    
    return totalReviewCount;
  }

  private Double getAverageOverallRating(Document doc) {
    Double avgOverallRating = null;
    Element avg = doc.select(".interaction-client__rating-info > span").first();
      
    if (avg != null) {
      avgOverallRating = Double.parseDouble(avg.ownText().replaceAll("[^0-9,]", "").replace(",", "."));
    }
    
    return avgOverallRating;
  }
}
