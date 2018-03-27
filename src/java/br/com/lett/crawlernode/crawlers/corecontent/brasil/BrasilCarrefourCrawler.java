package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.fetcher.DataFetcher;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
import models.Marketplace;
import models.Seller;
import models.Util;
import models.prices.Prices;

/**
 * 02/02/2018
 * 
 * @author gabriel
 *
 */
public class BrasilCarrefourCrawler extends Crawler {

  private static final String HOME_PAGE = "https://www.carrefour.com.br/";
  private static final String SELLER_NAME_LOWER = "carrefour";

  public BrasilCarrefourCrawler(Session session) {
    super(session);
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

    if (isProductPage(this.session.getOriginalURL())) {
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());


      String internalPid = crawlInternalPid(session.getOriginalURL());
      String name = crawlName(doc);
      CategoryCollection categories = crawlCategories(doc);
      String primaryImage = crawlPrimaryImage(doc);
      String secondaryImages = crawlSecondaryImages(doc);
      String description = crawlDescription(doc);
      Integer stock = null;
      String internalId = crawlInternalId(doc);
      Elements marketplacesElements = doc.select(".list-group-item");
      Map<String, Float> marketplaceMap;

      if (marketplacesElements.isEmpty()) {
        marketplaceMap = crawlMarketplaceForSingleSeller(doc, internalId);
      } else {
        marketplaceMap = crawlMarketplaceForMutipleSellers(marketplacesElements);
      }

      Marketplace marketplace = assembleMarketplaceFromMap(marketplaceMap, internalId);

      boolean available = marketplaceMap.containsKey(SELLER_NAME_LOWER);
      Float price = available ? marketplaceMap.get(SELLER_NAME_LOWER) : null;
      Prices prices = crawlPrices(price, internalId);

      Product product = ProductBuilder.create().setUrl(session.getOriginalURL()).setInternalId(internalId).setInternalPid(internalPid).setName(name)
          .setPrice(price).setPrices(prices).setAvailable(available).setCategory1(categories.getCategory(0)).setCategory2(categories.getCategory(1))
          .setCategory3(categories.getCategory(2)).setPrimaryImage(primaryImage).setSecondaryImages(secondaryImages).setDescription(description)
          .setStock(stock).setMarketplace(marketplace).build();

      products.add(product);

    } else

    {
      Logging.printLogDebug(logger, session, "Not a product page" + this.session.getOriginalURL());
    }

    return products;
  }


  /*******************************
   * Product page identification *
   *******************************/

  private boolean isProductPage(String url) {
    if ((url.contains("/p/")))
      return true;
    return false;
  }



  /*******************
   * General methods *
   *******************/

  private String crawlInternalId(Document document) {
    String internalId = null;
    Element internalIdElement = document.select("#productCod").first();

    if (internalIdElement != null) {
      internalId = internalIdElement.val().trim();
    }

    return internalId;
  }

  private String crawlInternalPid(String url) {
    String internalPid = null;

    if (url.contains("?")) {
      url = url.split("\\?")[0];
    }

    if (url.contains("/p/")) {
      String[] tokens = url.split("p/");

      if (tokens.length > 1 && tokens[1].contains("/")) {
        internalPid = tokens[1].split("/")[0];
      } else if (tokens.length > 1) {
        internalPid = tokens[1];
      }
    }
    return internalPid;
  }

  private String crawlName(Document document) {
    String name = null;
    Element nameElement = document.select("h1[itemprop=name]").first();

    if (nameElement != null) {
      name = nameElement.text().trim();
    }

    return name;
  }

  private Float crawlMainPagePrice(Document document) {
    Float price = null;
    Element specialPrice = document.select(".prince-product-default").first();

    if (specialPrice != null) {
      price = Float.parseFloat(specialPrice.text().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", "."));
    }

    return price;
  }

  private Map<String, Float> crawlMarketplaceForSingleSeller(Document document, String internalId) {
    Map<String, Float> marketplaces = new HashMap<>();

    Element oneMarketplaceInfo = document.select(".block-add-cart #moreInformation" + internalId).first();
    Element oneMarketplace = document.select(".block-add-cart > span").first();
    Element notifyMeElement = document.select(".text-not-product-avisme").first();

    if (notifyMeElement == null) {
      Float price = crawlMainPagePrice(document);

      if (oneMarketplaceInfo != null && oneMarketplace != null) {
        String text = oneMarketplace.ownText().trim().toLowerCase();

        if (text.contains("por") && text.contains(".")) {
          int x = text.indexOf("por") + 3;
          int y = text.indexOf(". entrega", x);

          String sellerName = text.substring(x, y).trim();

          marketplaces.put(sellerName, price);
        }
      } else {
        marketplaces.put(SELLER_NAME_LOWER, price);
      }
    }

    return marketplaces;
  }

  private Map<String, Float> crawlMarketplaceForMutipleSellers(Elements marketplacesElements) {
    Map<String, Float> marketplaces = new HashMap<>();

    for (Element e : marketplacesElements) {
      Element name = e.select(".font-mirakl-vendor-name strong").first();
      Element price = e.select(".prince-product-default span").first();

      if (name != null && price != null) {
        String sellerName = name.ownText().trim().toLowerCase();
        Float sellerPrice = MathUtils.parseFloat(price.ownText());

        if (sellerPrice != null && !sellerName.isEmpty()) {
          marketplaces.put(sellerName, sellerPrice);
        }
      }
    }

    return marketplaces;
  }

  private Marketplace assembleMarketplaceFromMap(Map<String, Float> marketplaceMap, String internalId) {
    Marketplace marketplace = new Marketplace();

    for (String sellerName : marketplaceMap.keySet()) {
      if (!sellerName.equalsIgnoreCase(SELLER_NAME_LOWER) && !sellerName.equalsIgnoreCase(SELLER_NAME_LOWER)) {
        JSONObject sellerJSON = new JSONObject();
        sellerJSON.put("name", sellerName);

        Float price = marketplaceMap.get(sellerName);
        Prices prices = crawlPrices(price, internalId);

        sellerJSON.put("price", price);
        sellerJSON.put("prices", prices.toJSON());

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

  private String crawlPrimaryImage(Document document) {
    String primaryImage = null;
    Element primaryImageElement = document.select(".sust-gallery div.item .thumb img").first();

    if (primaryImageElement != null) {
      String image = primaryImageElement.attr("data-zoom-image");
      if (image != null) {
        primaryImage = image.trim();
      }

    }

    return primaryImage;
  }

  private String crawlSecondaryImages(Document document) {
    String secondaryImages = null;
    JSONArray secondaryImagesArray = new JSONArray();

    Elements imagesElement = document.select(".sust-gallery div.item .thumb img");

    for (int i = 1; i < imagesElement.size(); i++) { // start with index 1 because the first image
                                                     // is the primary image
      Element e = imagesElement.get(i);

      if (e.attr("data-zoom-image") != null && !e.attr("data-zoom-image").isEmpty()) {
        secondaryImagesArray.put(e.attr("data-zoom-image").trim());
      }
    }

    if (secondaryImagesArray.length() > 0) {
      secondaryImages = secondaryImagesArray.toString();
    }

    return secondaryImages;
  }

  /**
   * @param document
   * @return
   */
  private CategoryCollection crawlCategories(Document document) {
    CategoryCollection categories = new CategoryCollection();
    Elements elementCategories = document.select(".breadcrumb > li > a");

    for (int i = 1; i < elementCategories.size() - 1; i++) { // starting from index 1, because the
      categories.add(elementCategories.get(i).text().trim());
    }

    return categories;
  }

  private String crawlDescription(Document document) {
    String description = "";
    Elements descriptionElements = document.select("#accordionFichaTecnica");

    if (descriptionElements != null) {
      description = description + descriptionElements.html();
    }

    Element desc = document.select(".productDetailsPageDescription").first();

    if (desc != null) {
      description += desc.outerHtml();
    }

    return description;
  }

  private Prices crawlPrices(Float price, String internalId) {
    Prices prices = new Prices();

    if (price != null) {
      prices.setBankTicketPrice(price);

      String url = "https://www.carrefour.com.br/installment/creditCard?productPrice=" + price + "&productCode=" + internalId;
      String json = DataFetcher.fetchString(DataFetcher.GET_REQUEST, session, url, null, cookies);

      JSONObject jsonPrices = new JSONObject();

      try {
        jsonPrices = new JSONObject(json);
      } catch (Exception e) {
        Logging.printLogError(logger, session, CommonMethods.getStackTrace(e));
      }

      if (jsonPrices.has("maestroInstallments")) {
        Map<Integer, Float> installmentPriceMap = crawlInstallment(jsonPrices, "maestroInstallments");
        prices.insertCardInstallment(Card.MAESTRO.toString(), installmentPriceMap);
      }

      if (jsonPrices.has("carrefourInstallments")) {
        Map<Integer, Float> installmentPriceMap = crawlInstallment(jsonPrices, "carrefourInstallments");

        prices.insertCardInstallment(Card.SHOP_CARD.toString(), installmentPriceMap);
      }

      if (jsonPrices.has("mastercardInstallments")) {
        Map<Integer, Float> installmentPriceMap = crawlInstallment(jsonPrices, "mastercardInstallments");
        prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
      }

      if (jsonPrices.has("dinersInstallments")) {
        Map<Integer, Float> installmentPriceMap = crawlInstallment(jsonPrices, "dinersInstallments");
        prices.insertCardInstallment(Card.DINERS.toString(), installmentPriceMap);
      }

      if (jsonPrices.has("visaInstallments")) {
        Map<Integer, Float> installmentPriceMap = crawlInstallment(jsonPrices, "visaInstallments");
        prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
      }

      if (jsonPrices.has("hipercardInstallments")) {
        Map<Integer, Float> installmentPriceMap = crawlInstallment(jsonPrices, "hipercardInstallments");
        prices.insertCardInstallment(Card.HIPERCARD.toString(), installmentPriceMap);
      }

      if (jsonPrices.has("amexInstallments")) {
        Map<Integer, Float> installmentPriceMap = crawlInstallment(jsonPrices, "amexInstallments");
        prices.insertCardInstallment(Card.AMEX.toString(), installmentPriceMap);
      }

      if (jsonPrices.has("eloInstallments")) {
        Map<Integer, Float> installmentPriceMap = crawlInstallment(jsonPrices, "eloInstallments");
        prices.insertCardInstallment(Card.ELO.toString(), installmentPriceMap);
      }
    }

    return prices;
  }

  private Map<Integer, Float> crawlInstallment(JSONObject jsonPrices, String keyCard) {
    Map<Integer, Float> installmentPriceMap = new HashMap<>();
    JSONArray installments = jsonPrices.getJSONArray(keyCard);

    for (int i = 0; i < installments.length(); i++) {
      JSONObject jsonInstallment = installments.getJSONObject(i);

      if (jsonInstallment.has("index")) {
        Integer installment = jsonInstallment.getInt("index");

        if (jsonInstallment.has("value")) {
          Float value =
              Float.parseFloat(jsonInstallment.getString("value").replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", ".").trim());

          installmentPriceMap.put(installment, value);
        }
      }
    }

    return installmentPriceMap;
  }
}
