package br.com.lett.crawlernode.crawlers.corecontent.brasil;


import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.Logging;
import models.Marketplace;
import models.Seller;
import models.Util;
import models.prices.Prices;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.DataNode;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class BrasilEletrocityCrawler extends Crawler {

  private final String HOME_PAGE = "http://www.eletrocity.com.br/";
  private final String ELETROCITY_SELLER_NAME_LOWER_CASE = "eletrocity";

  public BrasilEletrocityCrawler(Session session) {
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

    if (isProductPage(session.getOriginalURL())) {
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());


      /*
       * ************************************************************** crawling data of multiple
       * variations and the single products *
       ****************************************************************/

      // Pid
      String internalPid = crawlInternalPid(doc);

      // Categories
      ArrayList<String> categories = crawlCategories(doc);
      String category1 = getCategory(categories, 0);
      String category2 = getCategory(categories, 1);
      String category3 = getCategory(categories, 2);

      // Description
      String description = crawlDescription(doc);

      // Stock
      Integer stock = null;

      // sku data in json
      JSONArray arraySkus = crawlSkuJsonArray(doc);

      // Primary image
      String primaryImage = crawlPrimaryImage(doc);

      // Secondary images
      String secondaryImages = crawlSecondaryImages(doc);

      for (int i = 0; i < arraySkus.length(); i++) {
        JSONObject jsonSku = arraySkus.getJSONObject(i);

        // InternalId
        String internalId = crawlInternalId(jsonSku);

        // Name
        String name = crawlName(doc, jsonSku);

        // Marketplace map
        Map<String, Float> marketplaceMap = crawlMarketplace(jsonSku);

        // Marketplace
        Marketplace marketplace = assembleMarketplaceFromMap(marketplaceMap, internalId);

        // Availability
        boolean available = crawlAvailability(marketplaceMap);

        // Price
        Float price = crawlMainPagePrice(marketplaceMap);

        // Prices
        Prices prices = crawlPrices(internalId, price);

        // Creating the product
        Product product = new Product();
        product.setUrl(this.session.getOriginalURL());
        product.setInternalId(internalId);
        product.setInternalPid(internalPid);
        product.setName(name);
        product.setPrice(price);
        product.setPrices(prices);
        product.setAvailable(available);
        product.setCategory1(category1);
        product.setCategory2(category2);
        product.setCategory3(category3);
        product.setPrimaryImage(primaryImage);
        product.setSecondaryImages(secondaryImages);
        product.setDescription(description);
        product.setStock(stock);
        product.setMarketplace(marketplace);

        products.add(product);
      }

    } else {
      Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
    }

    return products;

  }

  /*******************************
   * Product page identification *
   *******************************/

  private boolean isProductPage(String url) {
    return url.endsWith("/p");
  }


  /*******************
   * General methods *
   *******************/

  private String crawlInternalId(JSONObject json) {
    String internalId = null;

    if (json.has("sku")) {
      internalId = Integer.toString((json.getInt("sku"))).trim();
    }

    return internalId;
  }


  private String crawlInternalPid(Document document) {
    String internalPid = null;
    Element internalPidElement = document.select("#___rc-p-id").first();

    if (internalPidElement != null) {
      internalPid = internalPidElement.attr("value").trim();
    }

    return internalPid;
  }

  private String crawlName(Document document, JSONObject jsonSku) {
    String name = null;
    Element nameElement = document.select(".gtp-compra h1 .fn").first();

    String nameVariation = jsonSku.getString("skuname");

    if (nameElement != null) {
      name = nameElement.text().trim();

      if (!name.contains(nameVariation)) {
        name = name + " - " + nameVariation;
      }
    }

    return name;
  }

  private Float crawlMainPagePrice(Map<String, Float> marketplace) {
    Float price = null;

    if (marketplace.containsKey(ELETROCITY_SELLER_NAME_LOWER_CASE)) {
      price = marketplace.get(ELETROCITY_SELLER_NAME_LOWER_CASE);
    }

    return price;
  }

  private boolean crawlAvailability(Map<String, Float> marketplace) {

    return marketplace.containsKey(ELETROCITY_SELLER_NAME_LOWER_CASE);
  }

  private boolean crawlAvailabilityMarketPlace(JSONObject json) {

    if (json.has("available"))
      return json.getBoolean("available");

    return false;
  }

  private Map<String, Float> crawlMarketplace(JSONObject json) {
    Map<String, Float> marketplace = new HashMap<String, Float>();

    if (json.has("seller")) {
      String nameSeller = json.getString("seller").toLowerCase().trim();

      if (json.has("bestPriceFormated") && crawlAvailabilityMarketPlace(json)) {
        Float price = Float.parseFloat(json.getString("bestPriceFormated").replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", "."));
        marketplace.put(nameSeller, price);
      }
    }

    return marketplace;
  }

  private Marketplace assembleMarketplaceFromMap(Map<String, Float> marketplaceMap, String internalId) {
    Marketplace marketplace = new Marketplace();

    for (String seller : marketplaceMap.keySet()) {
      if (!seller.equals(ELETROCITY_SELLER_NAME_LOWER_CASE)) {
        Float price = marketplaceMap.get(seller);

        JSONObject sellerJSON = new JSONObject();
        sellerJSON.put("name", seller);
        sellerJSON.put("price", price);
        sellerJSON.put("prices", crawlPrices(internalId, price).toJSON());

        try {
          Seller s = new Seller(sellerJSON);
          marketplace.add(s);
        } catch (Exception e) {
          Logging.printLogWarn(logger, session, Util.getStackTraceString(e));
        }
      }
    }

    return marketplace;
  }

  private String crawlPrimaryImage(Document document) {
    String primaryImage = null;
    Elements imagesElement = document.select(".thumbs li #botaoZoom");

    if (imagesElement.size() > 0) {
      primaryImage = imagesElement.get(0).attr("zoom").trim();
    }

    return primaryImage;
  }

  private String crawlSecondaryImages(Document document) {
    String secondaryImages = null;
    JSONArray secondaryImagesArray = new JSONArray();

    Elements imagesElement = document.select(".thumbs li #botaoZoom");

    for (int i = 1; i < imagesElement.size(); i++) { // starting from index 1, because the first is the primary image
      secondaryImagesArray.put(imagesElement.get(i).attr("zoom").trim());
    }

    if (secondaryImagesArray.length() > 0) {
      secondaryImages = secondaryImagesArray.toString();
    }

    return secondaryImages;
  }

  private ArrayList<String> crawlCategories(Document document) {
    ArrayList<String> categories = new ArrayList<String>();
    Elements elementCategories = document.select(".wrapped .bread-crumb ul li a");

    for (int i = 1; i < elementCategories.size(); i++) { // starting from index 1, because the first is the market name
      categories.add(elementCategories.get(i).text().trim());
    }

    return categories;
  }

  private String getCategory(ArrayList<String> categories, int n) {
    if (n < categories.size()) {
      return categories.get(n);
    }

    return "";
  }

  private String crawlDescription(Document document) {
    String description = "";
    Element descriptionElement = document.select("section .gtp-descricao.spec").first();

    if (descriptionElement != null)
      description = description + descriptionElement.html();
    return description;
  }

  private Prices crawlPrices(String internalId, Float price) {
    Prices prices = new Prices();

    if (price != null) {
      String url = "http://www.eletrocity.com.br/productotherpaymentsystems/" + internalId;

      Request request = RequestBuilder.create().setUrl(url).setCookies(cookies).build();
      Document doc = Jsoup.parse(this.dataFetcher.get(session, request).getBody());

      Element bank = doc.select("#ltlPrecoWrapper em").first();
      if (bank != null) {
        prices.setBankTicketPrice(Float.parseFloat(bank.text().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", ".").trim()));
      }

      Elements cardsElements = doc.select("#ddlCartao option");

      for (Element e : cardsElements) {
        String text = e.text().toLowerCase();

        if (text.contains("visa")) {
          Map<Integer, Float> installmentPriceMap = getInstallmentsForCard(doc, e.attr("value"));
          prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);

        } else if (text.contains("mastercard")) {
          Map<Integer, Float> installmentPriceMap = getInstallmentsForCard(doc, e.attr("value"));
          prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);

        } else if (text.contains("diners")) {
          Map<Integer, Float> installmentPriceMap = getInstallmentsForCard(doc, e.attr("value"));
          prices.insertCardInstallment(Card.DINERS.toString(), installmentPriceMap);

        } else if (text.contains("american") || text.contains("amex")) {
          Map<Integer, Float> installmentPriceMap = getInstallmentsForCard(doc, e.attr("value"));
          prices.insertCardInstallment(Card.AMEX.toString(), installmentPriceMap);

        } else if (text.contains("hipercard") || text.contains("amex")) {
          Map<Integer, Float> installmentPriceMap = getInstallmentsForCard(doc, e.attr("value"));
          prices.insertCardInstallment(Card.HIPERCARD.toString(), installmentPriceMap);

        } else if (text.contains("credicard")) {
          Map<Integer, Float> installmentPriceMap = getInstallmentsForCard(doc, e.attr("value"));
          prices.insertCardInstallment(Card.CREDICARD.toString(), installmentPriceMap);

        } else if (text.contains("elo")) {
          Map<Integer, Float> installmentPriceMap = getInstallmentsForCard(doc, e.attr("value"));
          prices.insertCardInstallment(Card.ELO.toString(), installmentPriceMap);

        }
      }


    }

    return prices;
  }

  private Map<Integer, Float> getInstallmentsForCard(Document doc, String idCard) {
    Map<Integer, Float> mapInstallments = new HashMap<>();

    Elements installmentsCard = doc.select(".tbl-payment-system#tbl" + idCard + " tr");
    for (Element i : installmentsCard) {
      Element installmentElement = i.select("td.parcelas").first();

      if (installmentElement != null) {
        String textInstallment = removeAccents(installmentElement.text().toLowerCase());
        Integer installment = null;

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

    return mapInstallments;
  }

  private String removeAccents(String str) {
    str = Normalizer.normalize(str, Normalizer.Form.NFD);
    str = str.replaceAll("[^\\p{ASCII}]", "");
    return str;
  }


  /**
   * Get the script having a json with the availability information
   * 
   * @return
   */
  private JSONArray crawlSkuJsonArray(Document document) {
    Elements scriptTags = document.getElementsByTag("script");
    JSONObject skuJson = null;
    JSONArray skuJsonArray = new JSONArray();

    for (Element tag : scriptTags) {
      for (DataNode node : tag.dataNodes()) {
        if (tag.html().trim().startsWith("var skuJson_0 = ")) {
          skuJson = new JSONObject(node.getWholeData().split(Pattern.quote("var skuJson_0 = "))[1]
              + node.getWholeData().split(Pattern.quote("var skuJson_0 = "))[1].split(Pattern.quote("}]};"))[0]);

        }
      }
    }

    if (skuJson != null && skuJson.has("skus")) {
      skuJsonArray = skuJson.getJSONArray("skus");
    }

    return skuJsonArray;
  }

  // @Override
  // public List<Product> extractInformation(Document doc) throws Exception {
  // super.extractInformation(doc);
  // List<Product> products = new ArrayList<Product>();
  //
  // if ( isProductPage(this.session.getOriginalURL()) ) {
  // Logging.printLogDebug(logger, session, "Product page identified: " +
  // this.session.getOriginalURL());
  //
  // /* *********************************************************
  // * crawling data common to both the cases of product page *
  // ***********************************************************/
  //
  // // Pid
  // String internalPid = crawlInternalPid(doc);
  //
  // // Categories
  // ArrayList<String> categories = crawlCategories(doc);
  // String category1 = "";
  // String category2 = "";
  // String category3 = "";
  // for (String c : categories) {
  // if (category1.isEmpty()) {
  // category1 = c;
  // } else if (category2.isEmpty()) {
  // category2 = c;
  // } else if (category3.isEmpty()) {
  // category3 = c;
  // }
  // }
  //
  // // Description
  // String description = crawlDescription(doc);
  //
  // // Primary image
  // String primaryImage = crawlPrimaryImage(doc);
  //
  // // Secondary images
  // String secondaryImages = crawlSecondaryImages(doc);
  //
  // // Stock
  // Integer stock = null;
  //
  //
  // /* **************************************
  // * crawling data of multiple variations *
  // ****************************************/
  // if ( hasProductVariations(doc) ) {
  // Logging.printLogDebug(logger, session, "Crawling multiple variations of a product...");
  //
  // // geting list of skus in page
  // Elements skusElements = doc.select(".skuList");
  //
  // for (int i = 0; i < skusElements.size(); i++) {
  // Element sku = skusElements.get(i);
  //
  // // InternalId
  // String internalId = crawlInternalForElement(doc, i);
  //
  // // Name
  // String name = crawlNameFromElement(sku);
  //
  // // Price
  // Float price = crawlPriceFromElement(sku);
  //
  // // Marketplace map
  // Map<String, Float> marketplaceMap = crawlMarketplaceFromElement(sku);
  //
  // // Marketplace
  // JSONArray marketplace = assembleMarketplaceFromMap(marketplaceMap, internalId);
  //
  // // Availability and Price from marketplace
  // boolean available = false;
  // boolean hasNotifyMe = variationHasNotifyMe(sku);
  // boolean hasMainSellerOnMarketplace = hasMainSellerOnMarketplace(marketplaceMap);
  //
  // if (hasNotifyMe) {
  // available = false;
  // } else {
  // if (hasMainSellerOnMarketplace) {
  // available = true;
  // } else {
  // available = false;
  // }
  // }
  // if (!available) price = null;
  //
  // // Prices
  // Prices prices = crawlPrices(internalId, price);
  //
  // // Creating the product
  // Product product = new Product();
  // product.setInternalId(internalId);
  // product.setInternalPid(internalPid);
  // product.setName(name);
  // product.setAvailable(available);
  // product.setPrice(price);
  // product.setPrices(prices);
  // product.setCategory1(category1);
  // product.setCategory2(category2);
  // product.setCategory3(category3);
  // product.setPrimaryImage(primaryImage);
  // product.setSecondaryImages(secondaryImages);
  // product.setDescription(description);
  // product.setStock(stock);
  // product.setMarketplace(marketplace);
  //
  // products.add(product);
  // }
  // }
  //
  // /* *******************************************
  // * crawling data of only one product in page *
  // *********************************************/
  // else {
  // Logging.printLogDebug(logger, session, "Crawling only one product...");
  //
  // // InternalId
  // String internalId = crawlInternalId(doc);
  //
  // // Name
  // String name = crawlName(doc);
  //
  // // Price
  // Float price = crawlMainPagePrice(doc);
  //
  // // Marketplace map
  // Map<String, Float> marketplaceMap = crawlMarketplace(doc);
  //
  // // Marketplace
  // JSONArray marketplace = assembleMarketplaceFromMap(marketplaceMap, internalId);
  //
  // // Availability and Price from marketplace
  // boolean available = false;
  // boolean hasNotifyMe = hasNotifyMe(doc);
  // boolean hasMainSellerOnMarketplace = hasMainSellerOnMarketplace(marketplaceMap);
  //
  // if (hasNotifyMe) {
  // available = false;
  // } else {
  // if (hasMainSellerOnMarketplace) {
  // available = true;
  // } else {
  // available = false;
  // }
  // }
  // if (!available) price = null;
  //
  // // Prices
  // Prices prices = crawlPrices(internalId, price);
  //
  // // Creating the product
  // Product product = new Product();
  // product.setUrl(this.session.getOriginalURL());
  // product.setInternalId(internalId);
  // product.setInternalPid(internalPid);
  // product.setName(name);
  // product.setPrice(price);
  // product.setPrices(prices);
  // product.setCategory1(category1);
  // product.setCategory2(category2);
  // product.setCategory3(category3);
  // product.setPrimaryImage(primaryImage);
  // product.setSecondaryImages(secondaryImages);
  // product.setDescription(description);
  // product.setStock(stock);
  // product.setMarketplace(marketplace);
  // product.setAvailable(available);
  //
  // products.add(product);
  // }
  //
  // } else {
  // Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
  // }
  //
  // return products;
  // }
  //
  //
  //
  // /*******************************
  // * Product page identification *
  // *******************************/
  //
  //
  //
  // /************************************
  // * Multiple products identification *
  // ************************************/
  //
  // private boolean hasProductVariations(Document document) {
  // Elements skuList = document.select(".gtp-compra .skuList");
  //
  // if (skuList.size() > 1) return true;
  // return false;
  // }
  //
  //
  // /*******************************
  // * Single product page methods *
  // *******************************/
  //
  // private String crawlName(Document document) {
  // String name = null;
  // Element nameElement = document.select(".gtp-compra h1 .fn").first();
  //
  // if (nameElement != null) {
  // name = nameElement.text().toString().trim();
  // }
  //
  // return name;
  // }
  //
  // private String crawlInternalId(Document document) {
  // String internalId = null;
  // Element internalIdElement = document.select("#___rc-p-sku-ids").first();
  //
  // if (internalIdElement != null) {
  // internalId = internalIdElement.attr("value").toString().trim();
  // }
  //
  // return internalId;
  // }
  //
  // private boolean hasNotifyMe(Document document) {
  // Element notifymeElement = document.select(".portal-notify-me-ref").first();
  //
  // if (notifymeElement != null) return true;
  // return false;
  // }
  //
  //
  // private Float crawlMainPagePrice(Document document) {
  // Float price = null;
  // Element mainPagePriceElement = document.select(".descricao-preco .skuBestPrice").first();
  //
  // if (mainPagePriceElement != null) {
  // price = Float.parseFloat( mainPagePriceElement.text().toString().replaceAll("[^0-9,]+",
  // "").replaceAll("\\.", "").replaceAll(",", ".") );
  // }
  //
  // return price;
  // }
  //
  // private Map<String, Float> crawlMarketplace(Document document) {
  // Map<String, Float> marketplace = new HashMap<String, Float>();
  // Element sellerElement = document.select(".seller-description .seller-name a").first();
  //
  // if (sellerElement != null) {
  // String sellerName = sellerElement.text().toString().trim().toLowerCase();
  // Float sellerPrice = this.crawlMainPagePrice(document);
  //
  // marketplace.put(sellerName, sellerPrice);
  // }
  //
  //
  // return marketplace;
  // }
  //
  //
  // /*********************************
  // * Multiple product page methods *
  // *********************************/
  //
  // private String crawlNameFromElement(Element sku) {
  // String name = null;
  // Element nameElement = sku.select(".nomeSku").first();
  //
  // if (nameElement != null) {
  // name = nameElement.text().toString().trim();
  // }
  //
  // return name;
  // }
  //
  //
  // private String crawlInternalForElement(Document document, int i) {
  // String internalId = null;
  // Element internalIdElement = document.select("#___rc-p-sku-ids").first();
  //
  // if (internalIdElement != null) {
  // String[] ids = internalIdElement.attr("value").toString().split(",");
  // if (i <= ids.length - 1) {
  // internalId = ids[i];
  // }
  // }
  //
  // return internalId;
  // }
  //
  // private boolean variationHasNotifyMe(Element sku) {
  // Element notifymeElement = sku.select(".portal-notify-me-ref").first();
  //
  // if (notifymeElement != null) return true;
  // return false;
  // }
  //
  // private Map<String, Float> crawlMarketplaceFromElement(Element sku) {
  // Map<String, Float> marketplace = new HashMap<String, Float>();
  //
  // Element sellerElement = sku.select("a[href^=/seller-info?]").first();
  // if (sellerElement != null) {
  // String sellerName = sellerElement.text().toString().trim().toLowerCase();
  // Float sellerPrice = crawlPriceFromElement(sku);
  //
  // marketplace.put(sellerName, sellerPrice);
  // }
  //
  //
  // return marketplace;
  // }
  //
  //
  // private Float crawlPriceFromElement(Element sku) {
  // Float price = null;
  // Element priceElement = sku.select(".preco .valor-por").first();
  //
  // if (priceElement != null) {
  // price = Float.parseFloat(priceElement.text().trim().replaceAll("[^0-9,]+", "").replaceAll("\\.",
  // "").replaceAll(",", "."));
  // }
  //
  // return price;
  // }
  //
  //
  // /*******************
  // * General methods *
  // *******************/
  //
  // private String crawlInternalPid(Document document) {
  // String internalPid = null;
  // Element internalPidElement = document.select("#___rc-p-id").first();
  //
  // if (internalPidElement != null) {
  // internalPid = internalPidElement.attr("value").toString().trim();
  // }
  //
  // return internalPid;
  // }
  //
  // private boolean hasMainSellerOnMarketplace(Map<String, Float> marketplaceMap) {
  // for (String seller : marketplaceMap.keySet()) {
  // if (seller.equals(ELETROCITY_SELLER_NAME_LOWER_CASE)) {
  // return true;
  // }
  // }
  //
  // return false;
  // }
  //
  //
  // private String crawlDescription(Document document) {
  // String description = "";
  // Element descriptionSpecElement = document.select("section .gtp-descricao.spec").first();
  //
  // if (descriptionSpecElement != null) {
  // description = descriptionSpecElement.html();
  // }
  //
  // return description;
  // }
  //
  // private JSONArray assembleMarketplaceFromMap(Map<String, Float> marketplaceMap, String
  // internalId) {
  // JSONArray marketplace = new JSONArray();
  //
  // for(String sellerName : marketplaceMap.keySet()) {
  // if ( !sellerName.equals(ELETROCITY_SELLER_NAME_LOWER_CASE) ) {
  // JSONObject seller = new JSONObject();
  // seller.put("name", sellerName);
  // seller.put("price", marketplaceMap.get(sellerName));
  // seller.put("prices", crawlPrices(internalId, marketplaceMap.get(sellerName)).getPricesJson());
  // marketplace.put(seller);
  // }
  // }
  //
  // return marketplace;
  // }
  //
  //
  // private ArrayList<String> crawlCategories(Document document) {
  // ArrayList<String> categories = new ArrayList<String>();
  // Elements elementCategories = document.select(".wrapped .bread-crumb ul li a");
  //
  // for (int i = 1; i < elementCategories.size(); i++) { // starting from index 1, because the first
  // is the market name
  // categories.add( elementCategories.get(i).text().trim() );
  // }
  //
  // return categories;
  // }
  //
  // /**
  // * No momento em que peguei os preços não foi achado prçeo no boleto com desconto
  // * @param internalId
  // * @param price
  // * @return
  // */
  // private Prices crawlPrices(String internalId, Float price){
  // Prices prices = new Prices();
  //
  // if(price != null){
  // String url = "http://www.eletrocity.com.br/productotherpaymentsystems/" + internalId;
  //
  // Document docPrices = DataFetcher.fetchDocument(DataFetcher.GET_REQUEST, session, url, null,
  // cookies);
  //
  // Element bankTicketElement = docPrices.select("#divBoleto em").first();
  // if(bankTicketElement != null){
  // Float bankTicketPrice = MathCommonsMethods.parseFloat(bankTicketElement.text());
  // prices.insertBankTicket(bankTicketPrice);
  // }
  //
  // Elements cardsElements = docPrices.select("#ddlCartao option");
  //
  // for(Element e : cardsElements){
  // String text = e.text().toLowerCase();
  //
  // if (text.contains("visa")) {
  // Map<Integer,Float> installmentPriceMap = getInstallmentsForCard(docPrices, e.attr("value"));
  // prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
  //
  // } else if (text.contains("mastercard")) {
  // Map<Integer,Float> installmentPriceMap = getInstallmentsForCard(docPrices, e.attr("value"));
  // prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
  //
  // } else if (text.contains("diners")) {
  // Map<Integer,Float> installmentPriceMap = getInstallmentsForCard(docPrices, e.attr("value"));
  // prices.insertCardInstallment(Card.DINERS.toString(), installmentPriceMap);
  //
  // } else if (text.contains("american") || text.contains("amex")) {
  // Map<Integer,Float> installmentPriceMap = getInstallmentsForCard(docPrices, e.attr("value"));
  // prices.insertCardInstallment(Card.AMEX.toString(), installmentPriceMap);
  //
  // } else if (text.contains("hipercard")) {
  // Map<Integer,Float> installmentPriceMap = getInstallmentsForCard(docPrices, e.attr("value"));
  // prices.insertCardInstallment(Card.HIPERCARD.toString(), installmentPriceMap);
  //
  // } else if (text.contains("credicard") ) {
  // Map<Integer,Float> installmentPriceMap = getInstallmentsForCard(docPrices, e.attr("value"));
  // prices.insertCardInstallment(Card.CREDICARD.toString(), installmentPriceMap);
  //
  // } else if (text.contains("elo") ) {
  // Map<Integer,Float> installmentPriceMap = getInstallmentsForCard(docPrices, e.attr("value"));
  // prices.insertCardInstallment(Card.ELO.toString(), installmentPriceMap);
  //
  // } else if (text.contains("aura") ) {
  // Map<Integer,Float> installmentPriceMap = getInstallmentsForCard(docPrices, e.attr("value"));
  // prices.insertCardInstallment(Card.AURA.toString(), installmentPriceMap);
  //
  // } else if (text.contains("discover") ) {
  // Map<Integer,Float> installmentPriceMap = getInstallmentsForCard(docPrices, e.attr("value"));
  // prices.insertCardInstallment(Card.DISCOVER.toString(), installmentPriceMap);
  //
  // }
  // }
  //
  //
  // }
  //
  // return prices;
  // }
  //
  // private Map<Integer,Float> getInstallmentsForCard(Document doc, String idCard){
  // Map<Integer,Float> mapInstallments = new HashMap<>();
  //
  // Elements installmentsCard = doc.select(".tbl-payment-system#tbl" + idCard + " tr");
  // for(Element i : installmentsCard){
  // Element installmentElement = i.select("td.parcelas").first();
  //
  // if(installmentElement != null){
  // String textInstallment = removeAccents(installmentElement.text().toLowerCase());
  // Integer installment = null;
  //
  // if(textInstallment.contains("vista")){
  // installment = 1;
  // } else {
  // installment = Integer.parseInt(textInstallment.replaceAll("[^0-9]", "").trim());
  // }
  //
  // Element valueElement = i.select("td:not(.parcelas)").first();
  //
  // if(valueElement != null){
  // Float value = Float.parseFloat(valueElement.text().replaceAll("[^0-9,]+", "").replaceAll("\\.",
  // "").replaceAll(",", ".").trim());
  //
  // mapInstallments.put(installment, value);
  // }
  // }
  // }
  //
  // return mapInstallments;
  // }
  //
  // private String removeAccents(String str) {
  // str = Normalizer.normalize(str, Normalizer.Form.NFD);
  // str = str.replaceAll("[^\\p{ASCII}]", "");
  // return str;
  // }

}
