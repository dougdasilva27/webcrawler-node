package br.com.lett.crawlernode.crawlers.corecontent.extractionutils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.http.cookie.Cookie;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import br.com.lett.crawlernode.core.fetcher.DataFetcher;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
import models.Marketplace;
import models.Seller;
import models.Util;
import models.prices.Prices;

public class VTEXCrawlersUtils {

  private static final String SKU_ID = "sku";
  private static final String PRODUCT_ID = "sku";
  private static final String SKU_NAME = "skuname";
  private static final String PRODUCT_NAME = "name";
  private static final String PRICE_FROM = "listPriceFormated";
  private static final String IMAGES = "Images";
  private static final String IS_PRINCIPAL_IMAGE = "IsMain";
  private static final String IMAGE_PATH = "Path";
  private static final String SELLERS_INFORMATION = "SkuSellersInformation";
  private static final String SELLER_NAME = "Name";
  private static final String SELLER_PRICE = "Price";
  private static final String SELLER_AVAILABLE_QUANTITY = "AvailableQuantity";
  private static final String IS_DEFAULT_SELLER = "IsDefaultSeller";


  private Logger logger;
  private Session session;
  private String sellerNameLower;
  private String homePage;
  private List<Cookie> cookies;

  public VTEXCrawlersUtils(Session session, Logger logger2, String store, String homePage, List<Cookie> cookies) {
    this.session = session;
    this.logger = logger2;
    this.sellerNameLower = store;
    this.homePage = homePage;
    this.cookies = cookies;
  }


  public String crawlInternalId(JSONObject json) {
    String internalId = null;

    if (json.has(SKU_ID)) {
      internalId = Integer.toString(json.getInt(SKU_ID)).trim();
    }

    return internalId;
  }

  public String crawlInternalPid(JSONObject skuJson) {
    String internalPid = null;

    if (skuJson.has(PRODUCT_ID)) {
      internalPid = skuJson.get(PRODUCT_ID).toString();
    }

    return internalPid;
  }

  public String crawlName(JSONObject jsonSku, JSONObject skuJson) {
    String name = null;

    String nameVariation = jsonSku.has(SKU_NAME) ? jsonSku.getString(SKU_NAME) : null;

    if (skuJson.has(PRODUCT_NAME)) {
      name = skuJson.getString(PRODUCT_NAME);

      if (nameVariation != null) {
        if (name.length() > nameVariation.length()) {
          name += " " + nameVariation;
        } else {
          name = nameVariation;
        }
      }
    }

    return name;
  }

  /**
   * Price "de"
   * 
   * @param jsonSku
   * @return
   */
  public Double crawlPriceFrom(JSONObject jsonSku) {
    Double priceFrom = null;

    if (jsonSku.has(PRICE_FROM)) {
      Float price = MathUtils.parseFloat(jsonSku.get(PRICE_FROM).toString());
      priceFrom = MathUtils.normalizeTwoDecimalPlaces(price.doubleValue());
    }

    return priceFrom;
  }

  public Float crawlMainPagePrice(Prices prices) {
    Float price = null;

    if (!prices.isEmpty() && prices.getCardPaymentOptions(Card.VISA.toString()).containsKey(1)) {
      Double priceDouble = prices.getCardPaymentOptions(Card.VISA.toString()).get(1);
      price = priceDouble.floatValue();
    }

    return price;
  }

  public String crawlPrimaryImage(JSONObject json) {
    String primaryImage = null;

    if (json.has(IMAGES)) {
      JSONArray jsonArrayImages = json.getJSONArray(IMAGES);

      for (int i = 0; i < jsonArrayImages.length(); i++) {
        JSONArray arrayImage = jsonArrayImages.getJSONArray(i);
        JSONObject jsonImage = arrayImage.getJSONObject(0);

        if (jsonImage.has(IS_PRINCIPAL_IMAGE) && jsonImage.getBoolean(IS_PRINCIPAL_IMAGE) && jsonImage.has(IMAGE_PATH)) {
          primaryImage = changeImageSizeOnURL(jsonImage.getString(IMAGE_PATH));
          break;
        }
      }
    }

    return primaryImage;
  }

  public String crawlSecondaryImages(JSONObject apiInfo) {
    String secondaryImages = null;
    JSONArray secondaryImagesArray = new JSONArray();

    if (apiInfo.has(IMAGES)) {
      JSONArray jsonArrayImages = apiInfo.getJSONArray(IMAGES);

      for (int i = 0; i < jsonArrayImages.length(); i++) {
        JSONArray arrayImage = jsonArrayImages.getJSONArray(i);
        JSONObject jsonImage = arrayImage.getJSONObject(0);

        // jump primary image
        if (jsonImage.has(IS_PRINCIPAL_IMAGE) && jsonImage.getBoolean(IS_PRINCIPAL_IMAGE)) {
          continue;
        }

        if (jsonImage.has(IMAGE_PATH)) {
          String urlImage = changeImageSizeOnURL(jsonImage.getString(IMAGE_PATH));
          secondaryImagesArray.put(urlImage);
        }

      }
    }

    if (secondaryImagesArray.length() > 0) {
      secondaryImages = secondaryImagesArray.toString();
    }

    return secondaryImages;
  }

  /**
   * Get the image url and change it size
   * 
   * @param url
   * @return
   */
  public String changeImageSizeOnURL(String url) {
    String[] tokens = url.trim().split("/");
    String dimensionImage = tokens[tokens.length - 2]; // to get dimension image and the image id

    String[] tokens2 = dimensionImage.split("-"); // to get the image-id
    String dimensionImageFinal = tokens2[0] + "-1000-1000";

    return url.replace(dimensionImage, dimensionImageFinal); // The image size is changed
  }

  public Map<String, Prices> crawlMarketplace(JSONObject json, String internalId) {
    Map<String, Prices> marketplace = new HashMap<>();

    if (json.has(SELLERS_INFORMATION)) {
      JSONArray sellers = json.getJSONArray(SELLERS_INFORMATION);

      for (Object s : sellers) {
        JSONObject seller = (JSONObject) s;

        if (seller.has(SELLER_NAME) && seller.has(SELLER_PRICE) && seller.has(SELLER_AVAILABLE_QUANTITY)
            && seller.get(SELLER_AVAILABLE_QUANTITY) instanceof Integer) {

          if (seller.getInt(SELLER_AVAILABLE_QUANTITY) < 1) {
            continue;
          }

          String nameSeller = seller.getString(SELLER_NAME).toLowerCase().trim();
          Object priceObject = seller.get(SELLER_PRICE);
          boolean isDefaultSeller =
              seller.has(IS_DEFAULT_SELLER) && seller.get(IS_DEFAULT_SELLER) instanceof Boolean && seller.getBoolean(IS_DEFAULT_SELLER);

          if (priceObject instanceof Double) {
            marketplace.put(nameSeller,
                crawlPrices(internalId, MathUtils.normalizeTwoDecimalPlaces(((Double) priceObject).floatValue()), json, isDefaultSeller));
          } else if (priceObject instanceof Integer) {
            marketplace.put(nameSeller,
                crawlPrices(internalId, MathUtils.normalizeTwoDecimalPlaces(((Integer) priceObject).floatValue()), json, isDefaultSeller));
          }
        }
      }
    }

    return marketplace;
  }

  public Marketplace assembleMarketplaceFromMap(Map<String, Prices> marketplaceMap) {
    Marketplace marketplace = new Marketplace();

    for (Entry<String, Prices> seller : marketplaceMap.entrySet()) {
      String sellerName = seller.getKey();
      if (!sellerName.equalsIgnoreCase(sellerNameLower)) {
        Prices prices = seller.getValue();

        JSONObject sellerJSON = new JSONObject();
        sellerJSON.put("name", sellerName);
        sellerJSON.put("prices", prices.toJSON());

        if (prices.getCardPaymentOptions(Card.VISA.toString()).containsKey(1)) {
          Double price = prices.getCardPaymentOptions(Card.VISA.toString()).get(1);
          Float priceFloat = MathUtils.normalizeTwoDecimalPlaces(price.floatValue());

          sellerJSON.put("price", priceFloat);
        }

        try {
          Seller s = new Seller(sellerJSON);
          marketplace.add(s);
        } catch (Exception e) {
          Logging.printLogError(logger, session, Util.getStackTraceString(e));
        }
      }
    }

    return marketplace;
  }

  /**
   * To crawl this prices is accessed a api Is removed all accents for crawl price 1x like this: Visa
   * à vista R$ 1.790,00
   * 
   * @param internalId
   * @param price
   * @return
   */
  public Prices crawlPrices(String internalId, Float price, JSONObject jsonSku, boolean marketplace) {
    Prices prices = new Prices();

    if (price != null) {

      if (marketplace) {
        String url = homePage + "productotherpaymentsystems/" + internalId;
        Document doc = DataFetcher.fetchDocument(DataFetcher.GET_REQUEST, session, url, null, cookies);

        prices.setPriceFrom(crawlPriceFrom(jsonSku));

        Element bank = doc.select("#ltlPrecoWrapper em").first();
        if (bank != null) {
          prices.setBankTicketPrice(MathUtils.parseFloat(bank.text()));
        } else {
          prices.setBankTicketPrice(price);
        }

        Elements cardsElements = doc.select("#ddlCartao option");

        if (!cardsElements.isEmpty()) {
          for (Element e : cardsElements) {
            String text = e.text().toLowerCase();

            if (text.contains("visa")) {
              Map<Integer, Float> installmentPriceMap = getInstallmentsForCard(doc, e.attr("value"), price);
              prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);

            } else if (text.contains("mastercard")) {
              Map<Integer, Float> installmentPriceMap = getInstallmentsForCard(doc, e.attr("value"), price);
              prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);

            } else if (text.contains("diners")) {
              Map<Integer, Float> installmentPriceMap = getInstallmentsForCard(doc, e.attr("value"), price);
              prices.insertCardInstallment(Card.DINERS.toString(), installmentPriceMap);

            } else if (text.contains("american") || text.contains("amex")) {
              Map<Integer, Float> installmentPriceMap = getInstallmentsForCard(doc, e.attr("value"), price);
              prices.insertCardInstallment(Card.AMEX.toString(), installmentPriceMap);

            } else if (text.contains("hipercard") || text.contains("amex")) {
              Map<Integer, Float> installmentPriceMap = getInstallmentsForCard(doc, e.attr("value"), price);
              prices.insertCardInstallment(Card.HIPERCARD.toString(), installmentPriceMap);

            } else if (text.contains("credicard")) {
              Map<Integer, Float> installmentPriceMap = getInstallmentsForCard(doc, e.attr("value"), price);
              prices.insertCardInstallment(Card.CREDICARD.toString(), installmentPriceMap);

            } else if (text.contains("elo")) {
              Map<Integer, Float> installmentPriceMap = getInstallmentsForCard(doc, e.attr("value"), price);
              prices.insertCardInstallment(Card.ELO.toString(), installmentPriceMap);

            }
          }
        } else {
          Map<Integer, Float> installmentPriceMap = new HashMap<>();
          installmentPriceMap.put(1, price);

          prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
          prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
          prices.insertCardInstallment(Card.DINERS.toString(), installmentPriceMap);
          prices.insertCardInstallment(Card.HIPERCARD.toString(), installmentPriceMap);
          prices.insertCardInstallment(Card.AMEX.toString(), installmentPriceMap);
          prices.insertCardInstallment(Card.ELO.toString(), installmentPriceMap);
        }
      } else {
        Map<Integer, Float> installmentPriceMap = new HashMap<>();
        installmentPriceMap.put(1, price);

        prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
        prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
        prices.insertCardInstallment(Card.DINERS.toString(), installmentPriceMap);
        prices.insertCardInstallment(Card.HIPERCARD.toString(), installmentPriceMap);
        prices.insertCardInstallment(Card.AMEX.toString(), installmentPriceMap);
        prices.insertCardInstallment(Card.ELO.toString(), installmentPriceMap);
      }
    }

    return prices;
  }

  public Map<Integer, Float> getInstallmentsForCard(Document doc, String idCard, Float bankPrice) {
    Map<Integer, Float> mapInstallments = new HashMap<>();

    Elements installmentsCard = doc.select(".tbl-payment-system#tbl" + idCard + " tr");
    for (Element i : installmentsCard) {
      Element installmentElement = i.select("td.parcelas").first();

      if (installmentElement != null) {
        String textInstallment = installmentElement.text().toLowerCase();
        Integer installment;

        if (textInstallment.contains("vista")) {
          installment = 1;
        } else {
          installment = Integer.parseInt(textInstallment.replaceAll("[^0-9]", "").trim());
        }

        Element valueElement = i.select("td:not(.parcelas)").first();

        if (valueElement != null) {
          Float value = Float.parseFloat(valueElement.text().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", ".").trim());

          mapInstallments.put(installment, value);
        }
      }
    }

    if (!mapInstallments.containsKey(1)) {
      mapInstallments.put(1, bankPrice);
    }

    return mapInstallments;
  }

  public JSONObject crawlApi(String internalId) {
    String url = homePage + "produto/sku/" + internalId;

    JSONArray jsonArray = DataFetcher.fetchJSONArray(DataFetcher.GET_REQUEST, session, url, null, cookies);

    if (jsonArray.length() > 0) {
      return jsonArray.getJSONObject(0);
    }

    return new JSONObject();
  }


  public Integer crawlStock(JSONObject json) {
    Integer stock = null;

    if (json.has(SELLERS_INFORMATION)) {
      JSONArray sellers = json.getJSONArray(SELLERS_INFORMATION);

      for (Object s : sellers) {
        JSONObject seller = (JSONObject) s;

        if (seller.has(SELLER_NAME) && sellerNameLower.equals(seller.get(SELLER_NAME).toString()) && seller.has(SELLER_AVAILABLE_QUANTITY)
            && seller.get(SELLER_AVAILABLE_QUANTITY) instanceof Integer) {
          stock = seller.getInt(SELLER_AVAILABLE_QUANTITY);
          break;
        }
      }
    }

    return stock;
  }
}
