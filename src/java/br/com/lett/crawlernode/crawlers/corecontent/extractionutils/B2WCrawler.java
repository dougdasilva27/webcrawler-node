package br.com.lett.crawlernode.crawlers.corecontent.extractionutils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.http.cookie.Cookie;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import br.com.lett.crawlernode.core.fetcher.DataFetcher;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
import models.Marketplace;
import models.Seller;
import models.Util;
import models.prices.Prices;

public class B2WCrawler {

  private static final String MAIN_B2W_NAME_LOWER = "b2w";
  private Logger logger;
  private Session session;
  private String sellerNameLower;
  private List<String> subSellers;
  private String homePage;
  private List<Cookie> cookies;

  public B2WCrawler(Session session, Logger logger2, String store, String homePage, List<Cookie> cookies, List<String> subSellers) {
    this.session = session;
    this.logger = logger2;
    this.sellerNameLower = store;
    this.subSellers = subSellers;
    this.homePage = homePage;
    this.cookies = cookies;
  }

  public List<Product> extractProducts(Document doc) {
    List<Product> products = new ArrayList<>();

    // Json da pagina principal
    JSONObject frontPageJson = SaopauloB2WCrawlersUtils.getDataLayer(doc);

    // Pega só o que interessa do json da api
    JSONObject infoProductJson = SaopauloB2WCrawlersUtils.assembleJsonProductWithNewWay(frontPageJson);

    if (infoProductJson.has("skus")) {
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

      String internalPid = this.crawlInternalPid(infoProductJson);
      CategoryCollection categories = crawlCategories(infoProductJson);
      boolean hasImages = doc.select(".main-area .row > div > span > img:not([src])").first() == null && doc.select(".gallery-product") != null;
      String primaryImage = hasImages ? this.crawlPrimaryImage(infoProductJson) : null;
      String secondaryImages = hasImages ? this.crawlSecondaryImages(infoProductJson) : null;
      String description = this.crawlDescription(internalPid, doc);
      Map<String, String> skuOptions = this.crawlSkuOptions(infoProductJson, doc);

      for (Entry<String, String> entry : skuOptions.entrySet()) {
        String internalId = entry.getKey();
        String name = entry.getValue().trim();
        Map<String, Prices> marketplaceMap = this.crawlMarketplace(infoProductJson, internalId);
        Marketplace variationMarketplace = this.assembleMarketplaceFromMap(marketplaceMap);
        boolean available = this.crawlAvailability(marketplaceMap);
        Float variationPrice = this.crawlPrice(marketplaceMap);
        Prices prices = crawlPrices(marketplaceMap);
        Integer stock = null; // stock só tem na api

        // Creating the product
        Product product = ProductBuilder.create().setUrl(session.getOriginalURL()).setInternalId(internalId).setInternalPid(internalPid).setName(name)
            .setPrice(variationPrice).setPrices(prices).setAvailable(available).setCategory1(categories.getCategory(0))
            .setCategory2(categories.getCategory(1)).setCategory3(categories.getCategory(2)).setPrimaryImage(primaryImage)
            .setSecondaryImages(secondaryImages).setDescription(description).setStock(stock).setMarketplace(variationMarketplace).build();

        products.add(product);
      }


    } else {
      Logging.printLogDebug(logger, session, "Not a product page" + this.session.getOriginalURL());
    }

    return products;
  }

  /*******************************
   * Product page identification *
   *******************************/

  private String crawlInternalPid(JSONObject assembleJsonProduct) {
    String internalPid = null;

    if (assembleJsonProduct.has("internalPid")) {
      internalPid = assembleJsonProduct.getString("internalPid").trim();
    }

    return internalPid;
  }

  private Map<String, String> crawlSkuOptions(JSONObject infoProductJson, Document doc) {
    Map<String, String> skuMap = new HashMap<>();

    boolean unnavailablePage = !doc.select(".unavailable-informer").isEmpty();

    if (infoProductJson.has("skus")) {
      JSONArray skus = infoProductJson.getJSONArray("skus");

      for (int i = 0; i < skus.length(); i++) {
        JSONObject sku = skus.getJSONObject(i);

        if (sku.has("internalId")) {
          String internalId = sku.getString("internalId");
          StringBuilder name = new StringBuilder();

          String variationName = "";
          if (sku.has("variationName")) {
            variationName = sku.getString("variationName");
          }

          String varationNameWithoutVolts = variationName.replace("volts", "").trim();

          if (unnavailablePage || (variationName.isEmpty() && skus.length() < 2) && infoProductJson.has("name")) {
            name.append(infoProductJson.getString("name"));
            if (!name.toString().toLowerCase().contains(varationNameWithoutVolts.toLowerCase())) {
              name.append(" " + variationName);
            }
          } else if (sku.has("name")) {
            name.append(sku.getString("name"));

            if (!name.toString().toLowerCase().contains(varationNameWithoutVolts.toLowerCase())) {
              name.append(" " + variationName);
            }
          }

          skuMap.put(internalId, name.toString().trim());
        }
      }
    }

    return skuMap;

  }

  private Map<String, Prices> crawlMarketplace(JSONObject skus, String internalId) {
    Map<String, Prices> marketplaces = new HashMap<>();

    if (skus.has("prices")) {
      JSONObject pricesJson = skus.getJSONObject("prices");

      if (pricesJson.has(internalId)) {
        JSONArray marketArrays = pricesJson.getJSONArray(internalId);

        for (int i = 0; i < marketArrays.length(); i++) {
          JSONObject seller = marketArrays.getJSONObject(i);

          if (seller.has("sellerName")) {
            String sellerName = seller.getString("sellerName");
            Prices prices = crawlMarketplacePrices(seller);

            if (marketArrays.length() == 1 && seller.has("priceFrom")) {
              String text = seller.get("priceFrom").toString();
              prices.setPriceFrom(MathUtils.parseDoubleWithComma(text));
            }

            marketplaces.put(sellerName, prices);
          }
        }
      }
    }

    return marketplaces;
  }

  private Prices crawlMarketplacePrices(JSONObject seller) {
    Prices prices = new Prices();

    if (seller.has("bankTicket")) {
      prices.setBankTicketPrice(seller.getDouble("bankTicket"));
    }

    if (seller.has("installments")) {
      Map<Integer, Float> installmentMapPrice = new HashMap<>();

      JSONArray installments = seller.getJSONArray("installments");

      for (int i = 0; i < installments.length(); i++) {
        JSONObject installment = installments.getJSONObject(i);

        if (installment.has("quantity") && installment.has("value")) {
          Integer quantity = installment.getInt("quantity");
          Double value = installment.getDouble("value");

          installmentMapPrice.put(quantity, MathUtils.normalizeTwoDecimalPlaces(value.floatValue()));
        }
      }

      // Isso acontece quando o seller principal não é a b2w, com isso não aparecem as parcelas
      // Na maioria dos casos a primeira parcela tem desconto e as demais não
      // O preço default seria o preço sem desconto.
      // Para pegar esse preço, dividimos ele por 2 e adicionamos nas parcelas como 2x esse preço
      if (!installmentMapPrice.isEmpty() && installmentMapPrice.containsKey(1) && seller.has("defaultPrice")) {
        Float defaultPrice = CrawlerUtils.getFloatValueFromJSON(seller, "defaultPrice");

        if (!defaultPrice.equals(installmentMapPrice.get(1))) {
          installmentMapPrice.put(2, MathUtils.normalizeTwoDecimalPlaces(defaultPrice / 2f));
        }
      }

      prices.insertCardInstallment(Card.VISA.toString(), installmentMapPrice);
      prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentMapPrice);
      prices.insertCardInstallment(Card.AURA.toString(), installmentMapPrice);
      prices.insertCardInstallment(Card.DINERS.toString(), installmentMapPrice);
      prices.insertCardInstallment(Card.HIPER.toString(), installmentMapPrice);
      prices.insertCardInstallment(Card.AMEX.toString(), installmentMapPrice);
    }

    if (seller.has("installmentsShopCard")) {
      Map<Integer, Float> installmentMapPrice = new HashMap<>();

      JSONArray installments = seller.getJSONArray("installmentsShopCard");

      for (int i = 0; i < installments.length(); i++) {
        JSONObject installment = installments.getJSONObject(i);

        if (installment.has("quantity") && installment.has("value")) {
          Integer quantity = installment.getInt("quantity");
          Double value = installment.getDouble("value");

          installmentMapPrice.put(quantity, MathUtils.normalizeTwoDecimalPlaces(value.floatValue()));
        }
      }

      prices.insertCardInstallment(Card.SHOP_CARD.toString(), installmentMapPrice);
    }

    return prices;
  }

  /*******************
   * General methods *
   *******************/

  private Float crawlPrice(Map<String, Prices> marketplaces) {
    Float price = null;
    Prices prices = null;

    String sellerName = getPrincipalSellerName(marketplaces);
    if (sellerName != null) {
      prices = marketplaces.get(sellerName);
    }

    if (prices != null && prices.getCardPaymentOptions(Card.VISA.toString()).containsKey(1)) {
      Double priceDouble = prices.getCardPaymentOptions(Card.VISA.toString()).get(1);
      price = priceDouble.floatValue();
    }

    return price;
  }

  private boolean crawlAvailability(Map<String, Prices> marketplaces) {
    boolean available = false;

    String sellerName = getPrincipalSellerName(marketplaces);
    if (sellerName != null) {
      available = true;
    }

    return available;
  }

  private String crawlPrimaryImage(JSONObject infoProductJson) {
    String primaryImage = null;

    if (infoProductJson.has("images")) {
      JSONObject images = infoProductJson.getJSONObject("images");

      if (images.has("primaryImage")) {
        primaryImage = images.getString("primaryImage");
      }
    }

    return primaryImage;
  }

  private String crawlSecondaryImages(JSONObject infoProductJson) {
    String secondaryImages = null;

    JSONArray secondaryImagesArray = new JSONArray();

    if (infoProductJson.has("images")) {
      JSONObject images = infoProductJson.getJSONObject("images");

      if (images.has("secondaryImages")) {
        secondaryImagesArray = images.getJSONArray("secondaryImages");
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
  private CategoryCollection crawlCategories(JSONObject document) {
    CategoryCollection categories = new CategoryCollection();

    JSONArray categoryList = document.getJSONArray("categories");

    for (int i = categoryList.length() - 1; i >= 0; i--) { // Invert the Loop since the categorys in the JSONArray come reversed
      String cat = (categoryList.getJSONObject(i).get("name")).toString();

      if (!cat.isEmpty()) {
        categories.add(cat);
      }
    }

    return categories;
  }


  private Marketplace assembleMarketplaceFromMap(Map<String, Prices> marketplaceMap) {
    Marketplace marketplace = new Marketplace();

    boolean hasPrincipalSeller = marketplaceMap.containsKey(MAIN_B2W_NAME_LOWER) || marketplaceMap.containsKey(sellerNameLower);

    for (String sellerName : marketplaceMap.keySet()) {
      if (!sellerName.equalsIgnoreCase(sellerNameLower) && !sellerName.equalsIgnoreCase(MAIN_B2W_NAME_LOWER)) {
        if (!hasPrincipalSeller && this.subSellers.contains(sellerName)) {
          continue;
        }

        JSONObject sellerJSON = new JSONObject();
        sellerJSON.put("name", sellerName);

        Prices prices = marketplaceMap.get(sellerName);

        if (prices.getCardPaymentOptions(Card.VISA.toString()).containsKey(1)) {
          // Pegando o preço de uma vez no cartão
          Double price = prices.getCardPaymentOptions(Card.VISA.toString()).get(1);
          Float priceFloat = MathUtils.normalizeTwoDecimalPlaces(price.floatValue());

          sellerJSON.put("price", priceFloat); // preço de boleto é o mesmo de preço uma vez.
        }
        sellerJSON.put("prices", marketplaceMap.get(sellerName).toJSON());

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

  private String crawlDescription(String internalPid, Document doc) {
    StringBuilder description = new StringBuilder();

    boolean alreadyCapturedHtmlSlide = false;

    Element datasheet = doc.selectFirst("#info-section");
    if (datasheet != null) {
      Element iframe = datasheet.selectFirst("iframe");

      if (iframe != null) {
        Document docDescriptionFrame = DataFetcher.fetchDocument(DataFetcher.GET_REQUEST, session, iframe.attr("src"), null, cookies);
        if (docDescriptionFrame != null) {
          alreadyCapturedHtmlSlide = true;
          description.append(docDescriptionFrame.html());
        }
      }

      datasheet.select("iframe, h1.sc-hgHYgh").remove();
      description.append(datasheet.html().replace("hidden", ""));
    }

    if (internalPid != null) {
      Element desc2 = doc.select(".info-description-frame-inside").first();

      if (desc2 != null && !alreadyCapturedHtmlSlide) {
        String urlDesc2 = homePage + "product-description/acom/" + internalPid;
        Document docDescriptionFrame = DataFetcher.fetchDocument(DataFetcher.GET_REQUEST, session, urlDesc2, null, cookies);
        if (docDescriptionFrame != null) {
          description.append(docDescriptionFrame.html());
        }
      }

      Element elementProductDetails = doc.select(".info-section").last();
      if (elementProductDetails != null) {
        elementProductDetails.select(".info-section-header.hidden-md.hidden-lg").remove();
        description.append(elementProductDetails.html());
      }
    }

    return description.toString();
  }

  private Prices crawlPrices(Map<String, Prices> marketplaceMap) {
    Prices prices = new Prices();

    String sellerName = getPrincipalSellerName(marketplaceMap);
    if (sellerName != null) {
      prices = marketplaceMap.get(sellerName);
    }

    return prices;
  }

  /**
   * se retornar null o produto nao e vendido pela loja
   * 
   * @param marketplaceMap
   * @return
   */
  private String getPrincipalSellerName(Map<String, Prices> marketplaceMap) {
    String sellerName = null;

    if (marketplaceMap.containsKey(sellerNameLower)) {
      sellerName = sellerNameLower;
    } else if (marketplaceMap.containsKey(MAIN_B2W_NAME_LOWER)) {
      sellerName = MAIN_B2W_NAME_LOWER;
    } else {
      for (String seller : subSellers) {
        if (marketplaceMap.containsKey(seller)) {
          sellerName = seller;
          break;
        }
      }
    }

    return sellerName;
  }

  // private Integer crawlStock(String internalId, JSONObject jsonProduct){
  // Integer stock = null;
  //
  // if(jsonProduct.has("prices")){
  // if(jsonProduct.getJSONObject("prices").has(internalId)){
  // JSONArray offers = jsonProduct.getJSONObject("prices").getJSONArray(internalId);
  //
  // for(int i = 0; i < offers.length(); i++) {
  // JSONObject seller = offers.getJSONObject(i);
  //
  // if(seller.has("sellerName") && seller.has("stock")) {
  // String sellerName = seller.getString("sellerName");
  //
  // if(sellerName.equalsIgnoreCase(MAIN_SELLER_NAME_LOWER) ||
  // sellerName.equalsIgnoreCase(MAIN_B2W_NAME_LOWER)) {
  // stock = seller.getInt("stock");
  // break;
  // }
  // }
  // }
  // }
  // }
  //
  // return stock;
  // }
}
