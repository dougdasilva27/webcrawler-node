package br.com.lett.crawlernode.crawlers.corecontent.extractionutils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.http.cookie.Cookie;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import br.com.lett.crawlernode.core.fetcher.methods.DataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
import models.Marketplace;
import models.Offer;
import models.Offer.OfferBuilder;
import models.Offers;
import models.prices.Prices;

public class VTEXCrawlersUtils {

  public static final String SKU_ID = "sku";
  public static final String PRODUCT_ID = "productId";
  public static final String SKU_NAME = "skuname";
  public static final String PRODUCT_NAME = "name";
  public static final String PRODUCT_MODEL = "Reference";
  public static final String PRICE_FROM = "ListPrice";
  public static final String IMAGES = "Images";
  public static final String IS_PRINCIPAL_IMAGE = "IsMain";
  public static final String IMAGE_PATH = "Path";
  public static final String SELLERS_INFORMATION = "SkuSellersInformation";
  public static final String SELLER_NAME = "Name";
  public static final String SELLER_PRICE = "Price";
  public static final String SELLER_AVAILABLE_QUANTITY = "AvailableQuantity";
  public static final String IS_DEFAULT_SELLER = "IsDefaultSeller";
  public static final String BEST_INSTALLMENT_NUMBER = "BestInstallmentNumber";
  public static final String BEST_INSTALLMENT_VALUE = "BestInstallmentValue";

  private Session session;
  private String sellerNameLower;
  private String homePage;
  private Integer cardDiscount;
  private Integer shopCardDiscount;
  private Integer bankTicketDiscount;
  private List<Cookie> cookies;
  private List<Card> cards;
  private boolean hasBankTicket = true;
  private boolean isPriceBasePriceFrom = false;
  private DataFetcher dataFetcher;
  private boolean isBuyBox = false;

  protected static final Logger logger = LoggerFactory.getLogger(VTEXCrawlersUtils.class);

  public VTEXCrawlersUtils(Session session, String store, String homePage, List<Cookie> cookies, DataFetcher dataFetcher) {
    this.session = session;
    this.sellerNameLower = store;
    this.homePage = homePage;
    this.cookies = cookies;
    this.dataFetcher = dataFetcher;
  }

  public VTEXCrawlersUtils(Session session, String store, String homePage, List<Cookie> cookies, Integer cardDiscount, Integer bankDiscount,
                           DataFetcher dataFetcher) {
    this.session = session;
    this.sellerNameLower = store;
    this.homePage = homePage;
    this.cookies = cookies;
    this.cardDiscount = cardDiscount;
    this.bankTicketDiscount = bankDiscount;
    this.dataFetcher = dataFetcher;
  }

  public VTEXCrawlersUtils(Session session, List<Cookie> cookies) {
    this.session = session;
    this.cookies = cookies;
  }

  public void setCardDiscount(Integer cardDiscount) {
    this.cardDiscount = cardDiscount;
  }

  public Integer getShopCardDiscount() {
    return shopCardDiscount;
  }

  public void setShopCardDiscount(Integer shopCardDiscount) {
    this.shopCardDiscount = shopCardDiscount;
  }

  public void setBankTicketDiscount(Integer bankTicketDiscount) {
    this.bankTicketDiscount = bankTicketDiscount;
  }

  /**
   * Set 1x discount scraping from html
   * 
   * @param doc
   * @param cssSelector
   * @param card -> if you desire set card discount
   * @param bank -> if you desire set bank discount
   */
  public void setDiscountWithDocument(Document doc, String cssSelector, boolean card, boolean bank) {

    Element discountElement = doc.selectFirst(cssSelector);
    if (discountElement != null) {
      String text = discountElement.ownText().replaceAll("[^0-9]", "");

      if (!text.isEmpty()) {
        Integer discount = Integer.parseInt(text);

        if (card) {
          setCardDiscount(discount);
        }

        if (bank) {
          setBankTicketDiscount(discount);
        }
      }
    }
  }

  public List<Card> getCards() {
    return cards;
  }

  public void setCards(List<Card> cards) {
    this.cards = cards;
  }

  public void setHasBankTicket(boolean hasBankTicket) {
    this.hasBankTicket = hasBankTicket;
  }

  public boolean isPriceBasePriceFrom() {
    return isPriceBasePriceFrom;
  }

  public void setPriceBasePriceFrom(boolean isPriceBasePriceFrom) {
    this.isPriceBasePriceFrom = isPriceBasePriceFrom;
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

  /**
   * Use crawlName(JSONObject jsonSku, JSONObject skuJson, String separator)
   * 
   * @param jsonSku
   * @param skuJson
   * @return
   */
  @Deprecated
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

  public String crawlName(JSONObject jsonSku, JSONObject skuJson, String separator) {
    StringBuilder name = new StringBuilder();

    String nameVariation = jsonSku.has(SKU_NAME) ? jsonSku.getString(SKU_NAME) : null;

    if (skuJson.has(PRODUCT_NAME)) {
      name.append(skuJson.getString(PRODUCT_NAME));

      if (nameVariation != null) {
        if (name.length() > nameVariation.length()) {
          name.append(separator).append(nameVariation);
        } else {
          name = new StringBuilder().append(nameVariation);
        }
      }
    }

    return name.toString();
  }

  /**
   * Capture name with product model
   * 
   * @param jsonSku
   * @param skuJson
   * @param apiJson
   * @return
   */
  public String crawlName(JSONObject jsonSku, JSONObject skuJson, JSONObject apiJson) {
    StringBuilder name = new StringBuilder();

    String nameVariation = jsonSku.has(SKU_NAME) ? jsonSku.getString(SKU_NAME) : null;

    if (skuJson.has(PRODUCT_NAME)) {
      name.append(skuJson.getString(PRODUCT_NAME));

      if (nameVariation != null) {
        if (name.length() > nameVariation.length()) {
          name.append(" ").append(nameVariation);
        } else {
          name = new StringBuilder(nameVariation);
        }
      }
    }

    if (apiJson.has(PRODUCT_MODEL)) {
      name.append(" ").append(apiJson.get(PRODUCT_MODEL));
    }

    return name.toString();
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
      priceFrom = Double.parseDouble(jsonSku.get(PRICE_FROM).toString());
    }

    return priceFrom;
  }

  public Float crawlMainPagePrice(Prices prices) {
    return crawlMainPagePrice(prices, Card.VISA);
  }

  public Float crawlMainPagePrice(Prices prices, Card card) {
    Float price = null;

    if (!prices.isEmpty() && prices.getCardPaymentOptions(card.toString()).containsKey(1)) {
      Double priceDouble = prices.getCardPaymentOptions(card.toString()).get(1);
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
  public static String changeImageSizeOnURL(String url) {
    String[] tokens = url.trim().split("/");
    String dimensionImage = tokens[tokens.length - 2]; // to get dimension image and the image id

    String[] tokens2 = dimensionImage.split("-"); // to get the image-id
    String dimensionImageFinal = tokens2[0] + "-1000-1000";

    return url.replace(dimensionImage, dimensionImageFinal); // The image size is changed
  }

  /**
   * @deprecated
   * @param json
   * @param internalId
   * @return
   */
  public Map<String, Prices> crawlMarketplace(JSONObject json, String internalId) {
    return crawlMarketplace(json, internalId, true);
  }

  /**
   * 
   * @param json
   * @param internalId
   * @param usePriceApi -> if you need acces price api for crawl installments ex: homePage +
   *        "productotherpaymentsystems/" + internalId
   * @return
   */
  public Map<String, Prices> crawlMarketplace(JSONObject json, String internalId, boolean usePriceApi) {
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
          Float price = CrawlerUtils.getFloatValueFromJSON(json, SELLER_PRICE, true, false);
          boolean isDefaultSeller =
              seller.has(IS_DEFAULT_SELLER) && seller.get(IS_DEFAULT_SELLER) instanceof Boolean && seller.getBoolean(IS_DEFAULT_SELLER);
          marketplace.put(nameSeller, crawlPrices(internalId, price, json, isDefaultSeller, usePriceApi));
        }
      }
    }

    return marketplace;
  }

  public Marketplace assembleMarketplaceFromMap(Map<String, Prices> marketplaceMap) {
    return CrawlerUtils.assembleMarketplaceFromMap(marketplaceMap, Arrays.asList(sellerNameLower), Card.VISA, session);
  }

  /**
   * Crawl description api on this url examples:
   * https://www.thebeautybox.com.br/api/catalog_system/pub/products/search?fq=skuId:19245
   * https://www.drogariasaopaulo.com.br/api/catalog_system/pub/products/search?fq=productId:19245
   * 
   * HOME_PAGE + api/catalog_system/pub/products/search?fq= + idType(sku or product) + : + id
   * 
   * @param id
   * @param idType
   * @return
   */
  public JSONObject crawlDescriptionAPI(String id, String idType) {
    JSONObject json = new JSONObject();

    String url = homePage + "api/catalog_system/pub/products/search?fq=" + idType + ":" + id;

    Request request = RequestBuilder.create().setUrl(url).setCookies(cookies).build();
    JSONArray array = CrawlerUtils.stringToJsonArray(this.dataFetcher.get(session, request).getBody());

    if (array.length() > 0) {
      json = array.getJSONObject(0);
    }

    return json;
  }

  /**
   * To crawl this prices is accessed a api Is removed all accents for crawl price 1x like this: Visa
   * à vista R$ 1.790,00
   * 
   * @param internalId
   * @param price
   * @return
   */
  public Prices crawlPrices(String internalId, Float price, JSONObject jsonSku, boolean marketplace, boolean usePriceApi) {
    Prices prices = new Prices();

    if (price != null) {
      Map<Integer, Float> installmentPriceMap = new HashMap<>();
      installmentPriceMap.put(1, price);

      prices.setPriceFrom(crawlPriceFrom(jsonSku));
      Float priceBase = isPriceBasePriceFrom ? MathUtils.normalizeNoDecimalPlaces(prices.getPriceFrom().floatValue()) : price;

      if (marketplace && usePriceApi) {
        crawlPricesFromApi(internalId, jsonSku, prices, price, priceBase);
      } else if (marketplace && jsonSku.has(BEST_INSTALLMENT_NUMBER) && jsonSku.has(BEST_INSTALLMENT_VALUE)) {
        Float value = CrawlerUtils.getFloatValueFromJSON(jsonSku, BEST_INSTALLMENT_VALUE);

        if (value != null) {
          installmentPriceMap.put(jsonSku.getInt(BEST_INSTALLMENT_NUMBER), value);
        }
      }

      if (this.cardDiscount != null) {
        installmentPriceMap.put(1, MathUtils.normalizeTwoDecimalPlaces(priceBase - (priceBase * (this.cardDiscount / 100f))));
      }

      if (prices.isEmpty()) {

        if (hasBankTicket) {
          prices.setBankTicketPrice(price);

          if (this.bankTicketDiscount != null && this.bankTicketDiscount > 0) {
            prices.setBankTicketPrice(MathUtils.normalizeTwoDecimalPlaces(priceBase - (priceBase * (this.bankTicketDiscount / 100f))));
          }
        }

        if (this.shopCardDiscount != null && this.shopCardDiscount > 0) {
          Map<Integer, Float> installmentPriceMapShopCard = new HashMap<>(installmentPriceMap);
          installmentPriceMapShopCard.put(1, MathUtils.normalizeTwoDecimalPlaces(priceBase - (priceBase * (this.shopCardDiscount / 100f))));

          prices.insertCardInstallment(Card.SHOP_CARD.toString(), installmentPriceMapShopCard);
        } else {
          prices.insertCardInstallment(Card.SHOP_CARD.toString(), installmentPriceMap);
        }

        if (cards == null) {
          prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
          prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
          prices.insertCardInstallment(Card.DINERS.toString(), installmentPriceMap);
          prices.insertCardInstallment(Card.HIPERCARD.toString(), installmentPriceMap);
          prices.insertCardInstallment(Card.AMEX.toString(), installmentPriceMap);
          prices.insertCardInstallment(Card.ELO.toString(), installmentPriceMap);
        } else {
          for (Card card : cards) {
            prices.insertCardInstallment(card.toString(), installmentPriceMap);
          }
        }
      }
    }

    return prices;
  }

  public void crawlPricesFromApi(String internalId, JSONObject jsonSku, Prices prices, Float price, Float priceBase) {
    String url = homePage + "productotherpaymentsystems/" + internalId;

    Request request = RequestBuilder.create().setUrl(url).setCookies(cookies).build();
    Document doc = Jsoup.parse(this.dataFetcher.get(session, request).getBody());

    if (hasBankTicket) {
      Element bank = doc.select("#ltlPrecoWrapper em").first();
      if (bank != null) {
        prices.setBankTicketPrice(MathUtils.parseFloatWithComma(bank.text()));
      } else {
        prices.setBankTicketPrice(price);
      }

      if (this.bankTicketDiscount != null) {
        prices.setBankTicketPrice(MathUtils.normalizeTwoDecimalPlaces(priceBase - (priceBase * (this.bankTicketDiscount / 100f))));
      }
    }

    Elements cardsElements = doc.select("#ddlCartao option");

    if (!cardsElements.isEmpty()) {
      for (Element e : cardsElements) {
        String text = e.text().toLowerCase();

        if (text.contains("visa")) {
          Map<Integer, Float> installmentPriceMap = getInstallmentsForCard(doc, e.attr("value"), price, priceBase);
          prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);

        } else if (text.contains("mastercard")) {
          Map<Integer, Float> installmentPriceMap = getInstallmentsForCard(doc, e.attr("value"), price, priceBase);
          prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);

        } else if (text.contains("diners")) {
          Map<Integer, Float> installmentPriceMap = getInstallmentsForCard(doc, e.attr("value"), price, priceBase);
          prices.insertCardInstallment(Card.DINERS.toString(), installmentPriceMap);

        } else if (text.contains("american") || text.contains("amex")) {
          Map<Integer, Float> installmentPriceMap = getInstallmentsForCard(doc, e.attr("value"), price, priceBase);
          prices.insertCardInstallment(Card.AMEX.toString(), installmentPriceMap);

        } else if (text.contains("hipercard") || text.contains("amex")) {
          Map<Integer, Float> installmentPriceMap = getInstallmentsForCard(doc, e.attr("value"), price, priceBase);
          prices.insertCardInstallment(Card.HIPERCARD.toString(), installmentPriceMap);

        } else if (text.contains("credicard")) {
          Map<Integer, Float> installmentPriceMap = getInstallmentsForCard(doc, e.attr("value"), price, priceBase);
          prices.insertCardInstallment(Card.CREDICARD.toString(), installmentPriceMap);

        } else if (text.contains("elo")) {
          Map<Integer, Float> installmentPriceMap = getInstallmentsForCard(doc, e.attr("value"), price, priceBase);
          prices.insertCardInstallment(Card.ELO.toString(), installmentPriceMap);

        } else if (text.contains("naranja")) {
          Map<Integer, Float> installmentPriceMap = getInstallmentsForCard(doc, e.attr("value"), price, priceBase);
          prices.insertCardInstallment(Card.NARANJA.toString(), installmentPriceMap);

        } else if (text.contains("cabal")) {
          Map<Integer, Float> installmentPriceMap = getInstallmentsForCard(doc, e.attr("value"), price, priceBase);
          prices.insertCardInstallment(Card.CABAL.toString(), installmentPriceMap);

        }
      }
    } else {
      Map<Integer, Float> installmentPriceMap = new HashMap<>();
      installmentPriceMap.put(1, price);

      if (cards == null) {
        prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
        prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
        prices.insertCardInstallment(Card.DINERS.toString(), installmentPriceMap);
        prices.insertCardInstallment(Card.HIPERCARD.toString(), installmentPriceMap);
        prices.insertCardInstallment(Card.AMEX.toString(), installmentPriceMap);
        prices.insertCardInstallment(Card.ELO.toString(), installmentPriceMap);
      } else {
        for (Card card : cards) {
          prices.insertCardInstallment(card.toString(), installmentPriceMap);
        }
      }
    }
  }

  public Map<Integer, Float> getInstallmentsForCard(Document doc, String idCard, Float bankPrice, Float priceBase) {
    Map<Integer, Float> mapInstallments = new HashMap<>();

    Elements installmentsCard = doc.select(".tbl-payment-system#tbl" + idCard + " tr");
    for (Element i : installmentsCard) {
      Element installmentElement = i.select("td.parcelas").first();

      if (installmentElement != null) {
        String textInstallment = installmentElement.text().toLowerCase();
        Integer installment = null;

        if (textInstallment.contains("vista")) {
          installment = 1;
        } else {
          String text = textInstallment.replaceAll("[^0-9]", "").trim();

          if (!text.isEmpty()) {
            installment = Integer.parseInt(text);
          }
        }

        Element valueElement = i.select("td:not(.parcelas)").first();

        if (valueElement != null && installment != null) {
          Float value = Float.parseFloat(valueElement.text().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", ".").trim());

          mapInstallments.put(installment, value);

          if (this.cardDiscount != null && installment == 1) {
            mapInstallments.put(1, MathUtils.normalizeTwoDecimalPlaces(priceBase - (priceBase * (this.cardDiscount / 100f))));
          }
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

    Request request = RequestBuilder.create().setUrl(url).setCookies(cookies).build();
    JSONArray jsonArray = CrawlerUtils.stringToJsonArray(this.dataFetcher.get(session, request).getBody());

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

        if (seller.has(SELLER_NAME) && sellerNameLower.equalsIgnoreCase(seller.get(SELLER_NAME).toString()) && seller.has(SELLER_AVAILABLE_QUANTITY)
            && seller.get(SELLER_AVAILABLE_QUANTITY) instanceof Integer) {
          stock = seller.getInt(SELLER_AVAILABLE_QUANTITY);
          break;
        }
      }
    }

    return stock;
  }

  public static Document sanitizeDescription(Object obj) {
    return Jsoup.parse(obj.toString().replace("[\"", "").replace("\"]", "").replace("\\r\\n\\r\\n\\r\\n", "").replace("\\", ""));
  }

  /******************************** RATING ****************************************/

  public static List<String> crawlIdList(JSONObject skuJson) {
    List<String> idList = new ArrayList<>();

    if (skuJson.has("skus")) {
      JSONArray skus = skuJson.getJSONArray("skus");

      for (int i = 0; i < skus.length(); i++) {
        JSONObject sku = skus.getJSONObject(i);

        if (sku.has("sku")) {
          idList.add(Integer.toString(sku.getInt("sku")));
        }
      }
    }

    return idList;
  }

  public void setBuyBox(boolean isBuyBox) {
    this.isBuyBox = isBuyBox;
  }

  public Offers scrapBuyBox(JSONObject jsonSku) {
    Offers offers = new Offers();

    if (jsonSku.has("SkuSellersInformation")) {
      JSONArray sellers = jsonSku.getJSONArray("SkuSellersInformation");

      int position = 1;
      for (Object o : sellers) {
        JSONObject seller = (JSONObject) o;

        if (CrawlerUtils.getIntegerValueFromJSON(seller, "AvailableQuantity", 0) > 0) {

          String sellerFullName = null;
          String slugSellerName = null;
          String internalSellerId = null;
          Double mainPrice = null;

          if (seller.has("Name")) {
            sellerFullName = seller.get("Name").toString();
            slugSellerName = CrawlerUtils.toSlug(sellerFullName);
          }

          if (seller.has("SellerId")) {
            internalSellerId = seller.get("SellerId").toString();
          }


          if (seller.has("Price")) {
            mainPrice = CrawlerUtils.getDoubleValueFromJSON(seller, "Price", true, true);
          }

          try {
            Offer offer = new OfferBuilder().setSellerFullName(sellerFullName).setSlugSellerName(slugSellerName).setInternalSellerId(internalSellerId)
                .setMainPagePosition(position).setIsBuybox(this.isBuyBox).setMainPrice(mainPrice).build();

            offers.add(offer);
          } catch (Exception e) {
            Logging.printLogError(logger, session, CommonMethods.getStackTrace(e));
          }
          position++;
        }
      }
    }

    return offers;
  }

  public static List<String> scrapEanFromProductAPI(JSONObject productAPI) {
    List<String> eans = null;

    if (productAPI.has("Ean") && !productAPI.isNull("Ean")) {
      eans = Arrays.asList(productAPI.get("Ean").toString());
    }

    return eans;
  }

}
