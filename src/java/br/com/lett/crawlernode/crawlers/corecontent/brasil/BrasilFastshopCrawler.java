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
import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.BrasilFastshopCrawlerUtils;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
import models.Marketplace;
import models.Seller;
import models.Util;
import models.prices.Prices;

/*************************************************************************************************************************
 * Crawling notes (18/11/2016):
 * 
 * 1) For this crawler, we have multiple skus on the same page. 2) There is no stock information for
 * skus in this ecommerce by the time this crawler was made. 3) There is marketplace information in
 * this ecommerce, but when was made, product with variations has no marketplace info. 4) The sku
 * page identification is done simply looking for an specific html element. 5) Even if a product is
 * unavailable, its price is not displayed if product has no variations. 6) There is internalPid for
 * skus in this ecommerce. The internalPid is a number that is the same for all the variations of a
 * given sku. 7) To get price of variations is accessed a api to get them. 8) Avaiability of product
 * is crawl in another api.
 * 
 * Price crawling notes: 1) For get prices, is parsed a json in the same api of principal price.
 * 
 * Examples: ex1 (available):
 * http://www.fastshop.com.br/webapp/wcs/stores/servlet/ProductDisplay?productId=4611686018426492507
 * ex2 (unavailable):
 * http://www.fastshop.com.br/loja/portateis/eletroportateis-cozinha/cafeteira/maquina-de-cafe-espresso-delonghi-manual-ec220cd-5728-fast
 * ex3 (variations):
 * http://www.fastshop.com.br/loja/ofertas-especiais-top-7/portateis-top/cafeteira-nespresso-modo-04-preta-d40brbkne-fast
 * ex4 (marketplace):
 * http://www.fastshop.com.br/loja/portateis/eletroportateis-cozinha/cafeteira/maquina-de-cafe-espresso-magnifica-s-delonghi-superautomatica-preta-ecam-22-110-b-5575-fast
 *
 * Optimizations notes: No optimizations.
 *
 ***************************************************************************************************************************/

public class BrasilFastshopCrawler extends Crawler {

  private static final String HOME_PAGE = "http://www.fastshop.com.br/";

  public BrasilFastshopCrawler(Session session) {
    super(session);
    super.config.setFetcher(FetchMode.APACHE);
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

    // Json com informações dos produtos
    JSONArray jsonArrayInfo = BrasilFastshopCrawlerUtils.crawlSkusInfo(doc);

    if (jsonArrayInfo.length() > 0) {
      products.addAll(crawlProductsOldWay(doc, jsonArrayInfo));
    } else {
      BrasilFastshopNewCrawler fastshop = new BrasilFastshopNewCrawler(session, logger, cookies, dataFetcher);
      products.addAll(fastshop.crawlProductsNewWay());
    }

    return products;
  }

  private List<Product> crawlProductsOldWay(Document doc, JSONArray jsonArrayInfo) {
    List<Product> products = new ArrayList<>();

    if (isProductPage(session.getOriginalURL(), doc)) {
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

      String internalPid = BrasilFastshopCrawlerUtils.crawlPartnerId(doc);
      String newUrl = internalPid != null ? CrawlerUtils.crawlFinalUrl(session.getOriginalURL(), session) : session.getOriginalURL();
      String name = crawlName(doc);
      ArrayList<String> categories = crawlCategories(doc);
      String category1 = getCategory(categories, 0);
      String category2 = getCategory(categories, 1);
      String category3 = getCategory(categories, 2);
      String primaryImage = crawlPrimaryImage(doc);
      String secondaryImages = crawlSecondaryImages(doc, primaryImage);
      String description = crawlDescription(doc);
      boolean hasVariations = hasVariations(jsonArrayInfo);
      Integer stock = null;

      for (int i = 0; i < jsonArrayInfo.length(); i++) {
        JSONObject productInfo = jsonArrayInfo.getJSONObject(i);

        // InternalId
        String internalId = crawlInternalId(productInfo);

        // Avaiability
        boolean available = crawlAvailability(internalId, hasVariations, doc);

        // Name
        String variationName = crawlVariationName(productInfo, name);

        // Json prices
        JSONObject jsonPrices = BrasilFastshopCrawlerUtils.fetchPrices(internalId, available, session, logger, dataFetcher);

        // Marketplace
        Marketplace marketplace = crawlMarketPlace(doc, jsonPrices, available);

        // boolean
        boolean availableForFastshop = (available && (marketplace.size() < 1));

        // Price
        Float price = crawlPrice(jsonPrices, availableForFastshop);

        // Prices
        Prices prices = crawlPrices(jsonPrices, price, doc);

        Product product = new Product();
        product.setUrl(newUrl);
        product.setInternalId(internalId);
        product.setInternalPid(internalPid);
        product.setName(variationName);
        product.setPrice(price);
        product.setPrices(prices);
        product.setAvailable(availableForFastshop);
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

  private boolean isProductPage(String url, Document doc) {
    Element elementProductInfoViewer = doc.select("#widget_product_info_viewer").first();
    return elementProductInfoViewer != null;
  }

  private boolean hasVariations(JSONArray jsonInfo) {
    return jsonInfo.length() > 1;
  }

  private String crawlInternalId(JSONObject jsonInfo) {
    String internalId = null;

    if (jsonInfo.has("catentry_id")) {
      internalId = jsonInfo.getString("catentry_id").trim();
    }

    return internalId;
  }

  private boolean crawlAvailability(String internalId, boolean hasVariations, Document doc) {
    boolean available = false;

    if (hasVariations) {
      String url =
          "http://www.fastshop.com.br/loja/GetInventoryStatusByIDView?storeId=10151&catalogId=11052&langId=-6&hotsite=fastshop&itemId=" + internalId;

      Request request = RequestBuilder.create().setUrl(url).setCookies(cookies).build();
      String json = this.dataFetcher.get(session, request).getBody();

      JSONObject jsonStock = new JSONObject();
      try {
        int x = json.indexOf("/*");
        int y = json.indexOf("*/", x + 2);

        json = json.substring(x + 2, y);

        jsonStock = new JSONObject(json);
      } catch (Exception e) {
        Logging.printLogWarn(logger, session, CommonMethods.getStackTrace(e));
      }

      if (jsonStock.has("onlineInventory")) {
        JSONObject jsonInventory = jsonStock.getJSONObject("onlineInventory");

        if (jsonInventory.has("status")) {
          available = jsonInventory.getString("status").trim().toLowerCase().equals("em estoque");
        }
      }
    } else {
      Element buyButton = doc.select("#buy_holder_buy_button").first();

      if (buyButton != null) {
        available = true;
      }
    }

    return available;
  }

  private String crawlName(Document document) {
    String name = null;
    Element nameElement = document.select(".newTitleBar").first();
    if (nameElement != null) {
      name = nameElement.text().trim();
    }

    return name;
  }

  private String crawlVariationName(JSONObject productInfo, String mainName) {
    String name = mainName;

    if (productInfo.has("Attributes")) {
      JSONObject jsonAttributes = productInfo.getJSONObject("Attributes");

      if (jsonAttributes.has("Voltagem_110V")) {
        if (jsonAttributes.get("Voltagem_110V").equals("1")) {
          name += " 110V";
        }
      } else if (jsonAttributes.has("Voltagem_220V")) {
        if (jsonAttributes.get("Voltagem_220V").equals("1")) {
          name += " 220V";
        }
      } else if (jsonAttributes.has("Voltagem") && !jsonAttributes.getString("Voltagem").trim().equalsIgnoreCase("bivolt")) {
        name += " " + jsonAttributes.getString("Voltagem");
      }
    }

    return name;
  }

  private String crawlPrimaryImage(Document document) {
    String primaryImage = null;
    Element elementPrimaryImage = document.select("#WC_CachedProductOnlyDisplay_images_1_1").first();
    if (elementPrimaryImage != null) {
      String tmpImg = elementPrimaryImage.attr("src");
      if (!tmpImg.startsWith("https:") && !tmpImg.startsWith("http:")) {
        primaryImage = CommonMethods.sanitizeUrl("https:" + tmpImg);
      } else {
        primaryImage = CommonMethods.sanitizeUrl(tmpImg);
      }
    }

    return primaryImage;
  }

  private String crawlSecondaryImages(Document document, String primaryImage) {
    String secondaryImages = null;
    JSONArray secondaryImagesArray = new JSONArray();
    Elements elementsSecondaryImages = document.select(".swiper-container.gallery-thumbs div");

    for (Element e : elementsSecondaryImages) {
      String secondaryImage = e.attr("style");

      if (secondaryImage.contains("url(")) {
        int x = secondaryImage.indexOf("url(") + 4;
        int y = secondaryImage.indexOf(")", x);

        String urlImage = secondaryImage.substring(x, y).trim();

        if (!urlImage.equals(primaryImage)) {
          secondaryImagesArray.put(CommonMethods.sanitizeUrl(urlImage));
        }
      }
    }

    if (secondaryImagesArray.length() > 0) {
      secondaryImages = secondaryImagesArray.toString();
    }

    return secondaryImages;
  }

  private ArrayList<String> crawlCategories(Document document) {
    ArrayList<String> categories = new ArrayList<>();
    Elements elementCategories = document.select("#widget_breadcrumb > ul li a");

    for (int i = 1; i < elementCategories.size(); i++) { // start with index 1 because the first
                                                         // item is home page
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


  private Prices crawlPrices(JSONObject jsonPrices, Float price, Document doc) {
    Prices prices = new Prices();

    if (price != null) {
      Map<Integer, Float> installmentPriceMap = new HashMap<>();

      Elements pricesImgs = doc.select(".stamps img");

      boolean promotion = false;
      for (Element e : pricesImgs) {
        String img = e.attr("src");

        if (img.contains("boleto")) {
          String[] tokens = img.split("/");
          String priceBoleto = tokens[tokens.length - 1].split("_")[0].replaceAll("[^0-9]", "");

          if (!priceBoleto.isEmpty()) {
            promotion = true;
            prices.setBankTicketPrice(Float.parseFloat(priceBoleto));
          }
        }
      }

      if (jsonPrices.has("priceData")) {
        JSONObject priceData = jsonPrices.getJSONObject("priceData");

        if (priceData.has("offerPrice")) {
          Float offerPrice = MathUtils.parseFloatWithComma(priceData.getString("offerPrice"));

          // Preço de boleto e 1 vez no cartão são iguais.
          installmentPriceMap.put(1, offerPrice);

          if (!promotion) {
            prices.setBankTicketPrice(offerPrice);
          }
        }

        if (priceData.has("installmentPrice")) {
          String text = priceData.getString("installmentPrice").toLowerCase();

          if (text.contains("x")) {
            int x = text.indexOf("x");

            Integer installment = Integer.parseInt(text.substring(0, x));
            Float value = MathUtils.parseFloatWithComma(text.substring(x));

            installmentPriceMap.put(installment, value);
          }
        }

        prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
        prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
        prices.insertCardInstallment(Card.AMEX.toString(), installmentPriceMap);
        prices.insertCardInstallment(Card.DINERS.toString(), installmentPriceMap);
        prices.insertCardInstallment(Card.HIPERCARD.toString(), installmentPriceMap);
        prices.insertCardInstallment(Card.ELO.toString(), installmentPriceMap);
      }
    }

    return prices;
  }

  private Marketplace crawlMarketPlace(Document doc, JSONObject jsonPrices, boolean available) {
    Marketplace marketplace = new Marketplace();

    Element mktElement = doc.select("span.mktPartnerGreen").first();

    if (mktElement == null) {
      mktElement = doc.select(".mktPartnerBlue .infoValue").first();
    }

    if (mktElement != null) {
      JSONObject sellerJSON = new JSONObject();
      Float price = crawlPrice(jsonPrices, available);
      Prices prices = crawlPrices(jsonPrices, price, doc);

      sellerJSON.put("name", mktElement.text().toLowerCase().trim());
      sellerJSON.put("price", price);
      sellerJSON.put("prices", prices.toJSON());

      if (available) {
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

  private static Float crawlPrice(JSONObject jsonPrices, boolean available) {
    Float price = null;

    if (available) {
      if (jsonPrices.has("priceData")) {
        JSONObject jsonCatalog = jsonPrices.getJSONObject("priceData");

        if (jsonCatalog.has("totalPrice")) {
          String text = jsonCatalog.getString("totalPrice");
          if (!text.isEmpty()) {
            price = MathUtils.parseFloatWithComma(text);
          } else if (jsonCatalog.has("offerPrice")) {
            price =
                Float.parseFloat(jsonCatalog.getString("offerPrice").trim().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", "."));
          }
        } else if (jsonCatalog.has("offerPrice")) {
          price = Float.parseFloat(jsonCatalog.getString("offerPrice").trim().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", "."));
        }
      }
    }


    return price;
  }

  private String crawlDescription(Document document) {
    String description = "";
    Element productTabContainer = document.select("#productTabContainer").first();
    if (productTabContainer != null) {
      productTabContainer.select("#_geral").remove();
      description = productTabContainer.text().trim();
    }
    return description;
  }
}
