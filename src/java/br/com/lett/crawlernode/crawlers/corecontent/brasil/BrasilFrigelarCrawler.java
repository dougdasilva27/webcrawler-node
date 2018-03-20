package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.DataNode;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.fetcher.DataFetcher;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.Logging;
import models.Marketplace;
import models.prices.Prices;

/************************************************************************************************************************************************************************************
 * Crawling notes (22/09/2016):
 * 
 * 1) For this crawler, we have one URL for multiple skus.
 * 
 * 2) There is no stock information for skus in this ecommerce by the time this crawler was made.
 * 
 * 3) There is no marketplace in this ecommerce by the time this crawler was made.
 * 
 * 4) The sku page identification is done simply looking for an specific html element.
 * 
 * 5) If the sku is unavailable, it's price is not displayed.
 * 
 * 6) The price of sku, found in json script, is wrong when the same is unavailable, then it is not
 * crawled.
 * 
 * 7) There is internalPid for skus in this ecommerce. The internalPid is a number that is the same
 * for all the variations of a given sku.
 * 
 * 7) The primary image is the first image on the secondary images.
 * 
 * 8) To get the internal_id is necessary to get a json , where internal_id is an attribute " sku ".
 * 
 * Examples: ex1 (available):
 * http://www.frigelar.com.br/ar-condicionado-split-gree-novo-garden-so-frio-high-wall-9000btus-gwc09ma-d1nna8c-kit510/p
 * ex2 (unavailable):
 * http://www.frigelar.com.br/ar-condicionado-split-gree-novo-garden-so-frio-high-wall-28000btus-gwc28me-d1nna8b-kit514/p
 *
 * Optimizations notes: No optimizations.
 *
 ************************************************************************************************************************************************************************************/

public class BrasilFrigelarCrawler extends Crawler {

  private final String HOME_PAGE = "http://www.frigelar.com.br/";

  public BrasilFrigelarCrawler(Session session) {
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

    if (isProductPage(doc)) {
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

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

      // Marketplace map
      Map<String, Float> marketplaceMap = crawlMarketplace(doc);

      // Marketplace
      Marketplace marketplace = assembleMarketplaceFromMap(marketplaceMap);

      // sku data in json
      JSONArray arraySkus = crawlSkuJsonArray(doc);

      for (int i = 0; i < arraySkus.length(); i++) {
        JSONObject jsonSku = arraySkus.getJSONObject(i);

        // Availability
        boolean available = crawlAvailability(jsonSku);

        // InternalId
        String internalId = crawlInternalId(jsonSku);

        // Price
        Float price = crawlMainPagePrice(jsonSku, available);

        // Prices
        Prices prices = crawlPrices(internalId, price, jsonSku, doc);

        // Primary image
        String primaryImage = crawlPrimaryImage(doc);

        // Name
        String name = crawlName(doc, jsonSku);

        // Secondary images
        String secondaryImages = crawlSecondaryImages(doc);

        // Creating the product
        Product product = new Product();

        product.setUrl(session.getOriginalURL());
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
      Logging.printLogDebug(logger, session, "Not a product page" + this.session.getOriginalURL());
    }

    return products;
  }

  /*******************************
   * Product page identification *
   *******************************/

  private boolean isProductPage(Document document) {
    if (document.select(".product-main").first() != null)
      return true;
    return false;
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
      internalPid = internalPidElement.attr("value").toString().trim();
    }

    return internalPid;
  }

  private String crawlName(Document document, JSONObject jsonSku) {
    String name = null;
    Element nameElement = document.select(".productName").first();

    String nameVariation = jsonSku.getString("skuname");

    if (nameElement != null) {
      name = nameElement.text().toString().trim();

      if (!name.toLowerCase().contains(nameVariation.toLowerCase())) {
        if (name.length() > nameVariation.length()) {
          name += " " + nameVariation;
        } else {
          name = nameVariation;
        }
      }
    }

    return name;
  }

  private Float crawlMainPagePrice(JSONObject json, boolean available) {
    Float price = null;

    if (json.has("bestPriceFormated") && available) {
      price = Float.parseFloat(json.getString("bestPriceFormated").replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", "."));
    }

    return price;
  }

  /**
   * To crawl this prices is accessed a api Is removed all accents for crawl price 1x like this: Visa
   * à vista R$ 1.790,00
   * 
   * @param internalId
   * @param price
   * @return
   */
  private Prices crawlPrices(String internalId, Float price, JSONObject json, Document docPrincipal) {
    Prices prices = new Prices();

    if (price != null) {
      String url = "https://www.frigelar.com.br/productotherpaymentsystems/" + internalId;

      Document doc = DataFetcher.fetchDocument(DataFetcher.GET_REQUEST, session, url, null, cookies);

      Float bestPriceFormated = crawlMainPagePrice(json, true);

      // apply discount for bank slip
      Float bankSlipPrice = price;
      Element discount = docPrincipal.select("#desconto-boleto").first();
      if (discount != null) {
        try {
          Integer discountRate = Integer.parseInt(discount.text().trim());
          bankSlipPrice = bestPriceFormated - ((discountRate / 100.0f) * bestPriceFormated);
        } catch (NumberFormatException numberFormatException) {
          Logging.printLogWarn(logger, "Could not get discount rate.");
        }
      }

      prices.setBankTicketPrice(bankSlipPrice);

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
        String textInstallment = installmentElement.text().toLowerCase();
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

  private boolean crawlAvailability(JSONObject json) {

    if (json.has("available"))
      return json.getBoolean("available");

    return false;
  }

  private Map<String, Float> crawlMarketplace(Document document) {
    return new HashMap<String, Float>();
  }

  private Marketplace assembleMarketplaceFromMap(Map<String, Float> marketplaceMap) {
    return new Marketplace();
  }

  private String crawlPrimaryImage(Document doc) {
    String primaryImage = null;

    Element image = doc.select(".image-zoom").first();

    if (image != null) {
      primaryImage = image.attr("href");
    }

    if (primaryImage == null) {
      Element imageMainElement = doc.select("#image-main").last();
      if (imageMainElement != null) {
        primaryImage = imageMainElement.attr("src").trim();
      }
    }

    return primaryImage;
  }

  private String crawlSecondaryImages(Document doc) {
    String secondaryImages = null;
    JSONArray secondaryImagesArray = new JSONArray();

    Element imageThumbs = doc.select("ul.thumbs").first();
    Elements images = null;
    if (imageThumbs != null) {
      images = imageThumbs.select("> li:not(.selected) a");
    }

    for (int i = 1; i < images.size(); i++) {// starts with index 1, because the first image is the
                                             // primary image
      Element e = images.get(i);
      String image = null;

      if (e.hasAttr("zoom")) {
        if (e.attr("zoom").startsWith("http")) {
          image = e.attr("zoom");
        }
      }

      if (image == null) {
        image = e.attr("rel");
      }

      if (image != null && !image.isEmpty()) {
        secondaryImagesArray.put(image);
      }
    }

    if (secondaryImagesArray.length() > 0) {
      secondaryImages = secondaryImagesArray.toString();
    }

    return secondaryImages;
  }

  private ArrayList<String> crawlCategories(Document document) {
    ArrayList<String> categories = new ArrayList<String>();
    Elements elementCategories = document.select(".bread-crumb > ul li a");

    for (int i = 1; i < elementCategories.size(); i++) { // starting from index 1, because the first
                                                         // is the market name
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
    Element descElement = document.select("#product-description").first();

    if (descElement != null) {
      description = description + descElement.html();
    }

    Element specElement = document.select("#caracteristicas").first();

    if (specElement != null) {
      description = description + specElement.html();
    }

    return description;
  }

  /**
   * Get the script having a json with the availability information
   * 
   * @return
   */
  private JSONArray crawlSkuJsonArray(Document document) {
    Elements scriptTags = document.getElementsByTag("script");
    JSONObject skuJson = null;
    JSONArray skuJsonArray = null;

    for (Element tag : scriptTags) {
      for (DataNode node : tag.dataNodes()) {
        if (tag.html().trim().startsWith("var skuJson_0 = ")) {

          skuJson = new JSONObject(node.getWholeData().split(Pattern.quote("var skuJson_0 = "))[1]
              + node.getWholeData().split(Pattern.quote("var skuJson_0 = "))[1].split(Pattern.quote("}]};"))[0]);

        }
      }
    }

    try {
      skuJsonArray = skuJson.getJSONArray("skus");
    } catch (Exception e) {
      e.printStackTrace();
    }

    return skuJsonArray;
  }
}
