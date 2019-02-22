package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.DataNode;
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
 * Date: 03/05/2017
 * 
 * @author Gabriel Dornelas
 *
 */
public class SaopauloWalmartCrawler extends Crawler {

  private final String HOME_PAGE = "http://www.walmart.com.br/";
  private final String HOME_PAGE_HTTPS = "https://www.walmart.com.br/";

  public SaopauloWalmartCrawler(Session session) {
    super(session);
  }

  @Override
  public boolean shouldVisit() {
    String href = this.session.getOriginalURL().toLowerCase();
    return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE) || href.startsWith(HOME_PAGE_HTTPS));
  }


  @Override
  public List<Product> extractInformation(Document doc) throws Exception {
    super.extractInformation(doc);
    List<Product> products = new ArrayList<>();

    if (isProductPage(doc)) {
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

      // Pid
      String internalPid = crawlInternalPid(session.getOriginalURL());

      // Categories
      CategoryCollection categories = crawlCategories(doc);

      // Description
      String description = crawlDescription(doc);

      // sku data in json
      JSONArray arraySkus = crawlDataLayer(doc);

      for (int i = 0; i < arraySkus.length(); i++) {
        JSONObject jsonSku = arraySkus.getJSONObject(i);

        // InternalId
        String internalId = crawlInternalId(jsonSku);

        // Document of marketplaces
        Document infoDoc = fetchMarketplaceInfoDocMainPage(internalId);

        // Marketplace
        Map<String, Prices> marketplaceMap = crawlMarketplace(internalId, internalPid, infoDoc);

        // ARRAY Marketplace
        Marketplace marketplace = assembleMarketplaceFromMap(marketplaceMap);

        // Availability
        boolean available = crawlAvailability(marketplaceMap);

        // Price
        Float price = crawlPrice(marketplaceMap, available);

        // Primary image
        String primaryImage = crawlPrimaryImage(doc);

        // Name
        String name = crawlName(doc, arraySkus.length() > 1 ? jsonSku : new JSONObject());

        // Secondary images
        String secondaryImages = crawlSecondaryImages(doc);

        // Prices
        Prices prices = crawlPrices(internalPid, price, marketplaceMap);

        // Stock
        Integer stock = crawlStock(infoDoc);

        // Creating the product
        Product product = ProductBuilder.create().setUrl(session.getOriginalURL()).setInternalId(internalId).setInternalPid(internalPid).setName(name)
            .setPrice(price).setPrices(prices).setAvailable(available).setCategory1(categories.getCategory(0)).setCategory2(categories.getCategory(1))
            .setCategory3(categories.getCategory(2)).setPrimaryImage(primaryImage).setSecondaryImages(secondaryImages).setDescription(description)
            .setStock(stock).setMarketplace(marketplace).build();

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

  private boolean isProductPage(Document doc) {
    return doc.select(".product-name").first() != null;
  }

  /*******************
   * General methods *
   *******************/

  private String crawlInternalId(JSONObject json) {
    String internalId = null;

    if (json.has("skuId")) {
      internalId = json.get("skuId").toString();
    } else if (json.has("options")) {
      internalId = json.get("productSku").toString();
    }

    return internalId;
  }


  private String crawlInternalPid(String url) {
    String[] tokens = url.split("/");

    return tokens[tokens.length - 2].trim();
  }

  private String crawlName(Document doc, JSONObject infoProduct) {
    String name = null;
    Element elementName = doc.select("h1.product-name").first();

    if (elementName != null) {
      name = elementName.text().trim();
    }

    if (infoProduct.has("name")) {
      String nameV = infoProduct.getString("name");

      if (name != null && !name.toLowerCase().contains(nameV.toLowerCase())) {
        name += " " + nameV;
      }
    }

    return name;
  }

  private Float crawlPrice(Map<String, Prices> marketplaceMap, boolean available) {
    Float price = null;

    if (available) {
      Map<Integer, Double> visaCardPaymentOptions = marketplaceMap.get("walmart").getCardPaymentOptions("visa");

      if (visaCardPaymentOptions.containsKey(1)) {
        Double priceDouble = visaCardPaymentOptions.get(1);
        price = MathUtils.normalizeTwoDecimalPlaces(priceDouble.floatValue());
      }
    }

    return price;
  }

  private boolean crawlAvailability(Map<String, Prices> marketplaceMap) {
    if (marketplaceMap.containsKey("walmart")) {
      return true;
    }
    return false;
  }

  private Marketplace assembleMarketplaceFromMap(Map<String, Prices> marketplaceMap) {
    Marketplace marketplace = new Marketplace();
    for (String partnerName : marketplaceMap.keySet()) {
      if (!partnerName.equals("walmart")) {
        JSONObject sellerJSON = new JSONObject();
        sellerJSON.put("name", partnerName);
        sellerJSON.put("price", marketplaceMap.get(partnerName).getBankTicketPrice());

        sellerJSON.put("prices", marketplaceMap.get(partnerName).toJSON());

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

  private String crawlPrimaryImage(Document doc) {
    String primaryImage = null;

    Elements imagesElements = doc.select("#wm-pictures-carousel a");

    if (!imagesElements.isEmpty()) {
      Element e = imagesElements.first();
      String imgUrl = null;

      if (e.attr("data-zoom") != null && !e.attr("data-zoom").isEmpty()) {
        imgUrl = e.attr("data-zoom");
      } else if (e.attr("data-normal") != null && !e.attr("data-normal").isEmpty()) {
        imgUrl = e.attr("data-normal");
      } else if (e.attr("src") != null && !e.attr("src").isEmpty()) {
        imgUrl = e.attr("src");
      }

      if (imgUrl != null && !imgUrl.startsWith("http")) {
        imgUrl = "http:" + imgUrl;
      }

      primaryImage = imgUrl;
    } else { // this case occurs when the product is discontinued, but it's info are still displayed
      Element mainImageElement = doc.select(".buybox-column.aside.discontinued .main-picture").first();
      if (mainImageElement != null) {
        primaryImage = "https:" + mainImageElement.attr("src");
      }
    }

    return primaryImage != null ? CommonMethods.sanitizeUrl(primaryImage) : null;
  }

  private String crawlSecondaryImages(Document doc) {
    String secondaryImages = null;
    JSONArray secondaryImagesArray = new JSONArray();

    Elements imagesElements = doc.select("#wm-pictures-carousel a");

    if (!imagesElements.isEmpty()) {
      for (int i = 1; i < imagesElements.size(); i++) {
        Element e = imagesElements.get(i);

        String imgUrl = null;

        if (e.attr("data-zoom") != null && !e.attr("data-zoom").isEmpty()) {
          imgUrl = e.attr("data-zoom");
        } else if (e.attr("data-normal") != null && !e.attr("data-normal").isEmpty()) {
          imgUrl = e.attr("data-normal");
        } else if (e.attr("src") != null && !e.attr("src").isEmpty()) {
          imgUrl = e.attr("src");
        }

        if (imgUrl != null && !imgUrl.startsWith("http")) {
          imgUrl = "http:" + imgUrl;
        }

        secondaryImagesArray.put(imgUrl != null ? CommonMethods.sanitizeUrl(imgUrl) : null);
      }
    }

    if (secondaryImagesArray.length() > 0) {
      secondaryImages = secondaryImagesArray.toString();
    }

    return secondaryImages;
  }

  private CategoryCollection crawlCategories(Document document) {
    CategoryCollection categories = new CategoryCollection();
    Elements elementCategories = document.select(".breadcrumb li");

    for (int i = 1; i < elementCategories.size(); i++) { // first index is the home page
      categories.add(elementCategories.get(i).text().replace(">", "").trim());
    }

    return categories;
  }


  private String crawlDescription(Document doc) {
    StringBuilder description = new StringBuilder();

    Element elementDescription = doc.select(".product-description").first();
    if (elementDescription != null) {
      description.append(elementDescription.html());
    }

    Element supplierDescription = doc.selectFirst(".product-manufacturer");
    if (supplierDescription != null) {

      Element iframe = supplierDescription.selectFirst("iframe");
      if (iframe != null) {
        String url = iframe.attr("src");

        if (!url.endsWith(".jpg") && !url.endsWith(".jpeg") && !url.endsWith(".png") && !url.endsWith(".gif")) {
          supplierDescription.selectFirst("iframe").remove();
          description.append(supplierDescription.html());
          description.append(DataFetcher.fetchDocument(DataFetcher.GET_REQUEST, session, url, null, cookies));
        }
      } else {
        description.append(supplierDescription.html());
      }
    }

    Element elementCharacteristics = doc.select("#product-characteristics-container").first();
    if (elementCharacteristics != null) {
      description.append(elementCharacteristics.html());
    }

    Element elementDimensions = doc.select(".product-dimensions").first();
    if (elementDimensions != null) {
      description.append(elementDimensions.html());
    }

    return description.toString();
  }

  private Map<String, Prices> crawlMarketplace(String productId, String internalPid, Document infoDoc) {
    Map<String, Prices> marketplace = new HashMap<>();

    Elements sellers = infoDoc.select(".product-sellers-list-item");

    for (Element e : sellers) {
      Element nameElement = e.select(".seller-name .name").first();

      if (nameElement != null) {
        String name = nameElement.ownText().trim().toLowerCase();

        Prices prices = new Prices();
        Map<Integer, Float> installmentPriceMap = new HashMap<>();

        Element priceElement = e.select(".product-price .product-price-value").first();

        if (priceElement != null) {
          Float price = MathUtils.parseFloatWithComma(priceElement.text().trim());
          installmentPriceMap.put(1, price);
          prices.setBankTicketPrice(price);
        }

        Element priceFrom = e.select(".product-price-old").first();
        if (priceFrom != null) {
          prices.setPriceFrom(MathUtils.parseDoubleWithComma(priceFrom.text()));
        }

        Element installmentElement = e.select(".product-price-installment").first();
        String priceInstallmentAmount = installmentElement.attr("data-price-installment-amount");

        if (installmentElement != null && !priceInstallmentAmount.isEmpty()) {
          Integer installment = Integer.parseInt(priceInstallmentAmount);

          Element valueElement = installmentElement.select(".product-price-price").first();

          if (valueElement != null) {
            Float value = MathUtils.parseFloatWithComma(valueElement.text());

            installmentPriceMap.put(installment, value);
          }
        }
        prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);

        marketplace.put(name, prices);
      }
    }

    Element moreSellers = infoDoc.select(".more-sellers-link").first();

    if (moreSellers != null) {
      Document docMarketPlaceMoreSellers = fetchMarketplaceInfoDoc(productId, internalPid);

      Elements marketplaces = docMarketPlaceMoreSellers.select(".sellers-list tr:not([class])");

      for (Element e : marketplaces) {

        // Name
        Element nameElement = e.select("td span[data-seller]").first();

        if (nameElement != null) {
          String name = nameElement.text().trim().toLowerCase();

          Prices prices = new Prices();
          Map<Integer, Float> installmentPriceMap = new HashMap<>();

          Element priceElement = e.select(".payment-price").first();

          if (priceElement != null) {
            Float price = Float.parseFloat(priceElement.text().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", ".").trim());
            installmentPriceMap.put(1, price);
            prices.setBankTicketPrice(price);
          }

          Element installmentElement = e.select(".payment-installment-amount").first();

          if (installmentElement != null) {
            Integer installment = Integer.parseInt(installmentElement.ownText().trim().replaceAll("[^0-9]", ""));

            Element installmentValueElement = e.select(".payment-installment-price").first();

            if (installmentValueElement != null) {
              Float value =
                  Float.parseFloat(installmentValueElement.ownText().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", ".").trim());

              installmentPriceMap.put(installment, value);
              prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
            }
          }

          marketplace.put(name, prices);
        }

      }
    }

    return marketplace;
  }

  private Document fetchMarketplaceInfoDoc(String productId, String pid) {
    String infoUrl = "https://www.walmart.com.br/xhr/sellers/sku/" + productId + "?productId=" + pid;
    String fetchResult = DataFetcher.fetchString(DataFetcher.GET_REQUEST, session, infoUrl, null, null);

    return Jsoup.parse(fetchResult);
  }

  private Document fetchMarketplaceInfoDocMainPage(String productId) {
    String infoUrl = "https://www.walmart.com.br/xhr/sku/buybox/" + productId + "/?isProductPage=true";
    String fetchResult = DataFetcher.fetchString(DataFetcher.GET_REQUEST, session, infoUrl, null, null);

    return Jsoup.parse(fetchResult);
  }


  private Prices crawlPrices(String internalPid, Float price, Map<String, Prices> marketplaces) {
    Prices p = new Prices();
    Map<Integer, Float> installmentPriceMap = new HashMap<>();

    if (marketplaces.containsKey("walmart")) {
      Prices prices = marketplaces.get("walmart");
      p.setBankTicketPrice(prices.getBankTicketPrice());

      if (price != null) {
        // preço principal é o mesmo preço de 1x no cartão
        installmentPriceMap.put(1, price);

        String urlInstallmentPrice = "https://www.walmart.com.br/produto/installment/1," + internalPid + "," + price + ",VISA/";
        Document doc = DataFetcher.fetchDocument(DataFetcher.GET_REQUEST, session, urlInstallmentPrice, null, cookies);
        Elements installments = doc.select(".installment-table tr:not([id])");

        for (Element e : installments) {
          Element parc = e.select("td.parcelas").first();

          if (parc != null) {
            Integer installment = Integer.parseInt(parc.text().replaceAll("[^0-9]", "").trim());

            Element parcValue = e.select(".valor-parcela").first();

            if (parcValue != null) {
              Float installmentValue =
                  Float.parseFloat(parcValue.text().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", ".").trim());

              installmentPriceMap.put(installment, installmentValue);
            }
          }
        }

        p.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
        p.insertCardInstallment(Card.AMEX.toString(), installmentPriceMap);
        p.insertCardInstallment(Card.ELO.toString(), installmentPriceMap);
        p.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
        p.insertCardInstallment(Card.DINERS.toString(), installmentPriceMap);
        p.insertCardInstallment(Card.HIPERCARD.toString(), installmentPriceMap);
      }
    }

    return p;
  }

  private Integer crawlStock(Document infoDoc) {
    Integer stock = null;
    Element stockWalmart = infoDoc.select("#buybox-Walmart").first();

    if (stockWalmart != null) {
      if (stockWalmart.hasAttr("data-quantity")) {
        stock = Integer.parseInt(stockWalmart.attr("data-quantity"));
      }
    }

    return stock;
  }

  /**
   * Get the script having a json with the availability information
   * 
   * @return
   */
  private JSONArray crawlDataLayer(Document doc) {
    JSONArray productsListInfo = new JSONArray();

    Elements scriptTags = doc.getElementsByTag("script");
    for (Element tag : scriptTags) {
      for (DataNode node : tag.dataNodes()) {
        if (tag.html().trim().startsWith("var dataLayer = ") && tag.html().trim().contains("dataLayer.push(")) {

          JSONObject dataLayer = new JSONObject(node.getWholeData().split(Pattern.quote("dataLayer.push("))[1]
              + node.getWholeData().split(Pattern.quote("dataLayer.push("))[1].split(Pattern.quote(");"))[0]);

          productsListInfo = dataLayer.getJSONArray("trees").getJSONObject(0).getJSONObject("skuTree").getJSONArray("options");

          assembleSkuJsonForSingleProduct(productsListInfo, dataLayer);
        }
      }
    }

    return productsListInfo;
  }

  private void assembleSkuJsonForSingleProduct(JSONArray productsListInfo, JSONObject dataLayer) {
    if (productsListInfo.length() > 0) {
      for (int i = 0; i < productsListInfo.length(); i++) {
        JSONObject product = productsListInfo.getJSONObject(i);

        if (!product.has("skuId") && dataLayer.has("trees")) {
          JSONArray trees = dataLayer.getJSONArray("trees");

          if (trees.length() >= i) {
            JSONObject sku = trees.getJSONObject(i);

            if (sku.has("standardSku")) {
              productsListInfo.getJSONObject(i).put("skuId", sku.get("standardSku"));
            }
          }
        }
      }

    } else {
      productsListInfo.put(new JSONObject("{\"name\":\"\",\"skuId\":" + dataLayer.getJSONArray("trees").getJSONObject(0).get("standardSku") + "}"));
    }
  }
}
