package br.com.lett.crawlernode.crawlers.corecontent.brasil;

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
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
import models.Marketplace;
import models.prices.Prices;

/************************************************************************************************************************************************************************************
 * Crawling notes (09/11/2016):
 * 
 * 1) For this crawler, we have one url for mutiples skus.
 * 
 * 2) There is stock information for skus in this ecommerce by the time this crawler was made.
 * 
 * 3) There is no marketplace information in this ecommerce
 * 
 * 4) To get the secondaryImages in for products with colors, is acessed a page from this color for
 * crawled
 * 
 * 5) The sku page identification is done simply looking the URL format and the html element.
 * 
 * 6) Even if a product is unavailable, its price is displayed.
 * 
 * 7) There is internalPid for skus in this ecommerce. The internalPid is a number that is the same
 * for all the variations of a given sku.
 * 
 * 8) The first image in secondary images is the primary image, sometimes the second image too, then
 * it made ​​a check.
 * 
 * 9) The price of variation is crawler from the script code var_variants in html
 * 
 * Examples: ex1 (available):
 * http://www.estrela10.com.br/placa-de-video-vga-geforce-gtx-750ti-oc-2gb-gddr5-128bits-pny-84830-p11270167
 * ex2 (unavailable):
 * http://www.estrela10.com.br/placa-de-video-geforce-gt-640-2gb-ddr3-128-bits-evga-25650-p4356170
 * ex3 (With colors):
 * http://www.estrela10.com.br/jaqueta-fleece-com-bolso-frontal-flexxxa-mormaii-64858-p10269522 ex4
 * (With Volts):
 * http://www.estrela10.com.br/furadeira-profissional-sem-impacto-fsv400-vonder-14743-p4399019
 *
 * Optimizations notes: No optimizations.
 *
 ************************************************************************************************************************************************************************************/

public class BrasilEstrela10Crawler extends Crawler {

  private final String HOME_PAGE = "http://www.estrela10.com.br/";

  public BrasilEstrela10Crawler(Session session) {
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
    List<Product> products = new ArrayList<Product>();

    if (isProductPage(this.session.getOriginalURL(), doc)) {
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

      // Name main page
      String mainPageName = crawlName(doc);

      // Pid
      String internalPid = this.crawlInternalPid(doc);

      // Marketplace map
      Map<String, Float> marketplaceMap = this.crawlMarketplaces(doc);

      // Assemble marketplace from marketplace map
      Marketplace marketplaces = this.assembleMarketplaceFromMap(marketplaceMap);

      // Categories
      ArrayList<String> categories = this.crawlCategories(doc);
      String category1 = getCategory(categories, 0);
      String category2 = getCategory(categories, 1);
      String category3 = getCategory(categories, 2);

      // Description
      String description = this.crawlDescription(doc);

      // Number of colors in the sku
      Map<String, String> colorsMap = this.identifyNumberOfColors(doc);

      // Get images from Colors
      JSONArray imageColorsArray = this.fetchImageColors(colorsMap, this.session.getOriginalURL());

      // Array de produtos
      JSONArray jsonProducts = crawlSkuJsonArray(doc);

      // HasVariations
      boolean hasVariations = hasVariationsSku(doc);

      // if product has variations, the first product is a product default, so is not crawled
      // then if is not, the last product is not crawled, because is a invalid product
      int indexStart = 0;
      int indexFinished = jsonProducts.length();

      if (hasVariations) {
        indexStart++;
      } else {
        indexFinished--;
      }


      for (int i = indexStart; i < indexFinished; i++) {
        // Json sku
        JSONObject jsonSku = jsonProducts.getJSONObject(i);

        // Name
        String name = this.crawlName(jsonSku, mainPageName);

        // InternalId
        String internalID = this.crawlInternalId(jsonSku);

        // Estoque
        Integer stock = crawlStock(jsonSku);

        // Available
        boolean available = this.crawlAvailability(stock);

        // Price
        Float price = this.crawlPrice(jsonSku, available);

        // PrimaryImage
        String primaryImageVariation = this.crawlPrimaryImage(doc, name, imageColorsArray);

        // Secondary Images
        String secondaryImagesVariation = this.crawlSecondaryImages(doc, primaryImageVariation, name, imageColorsArray);

        // Prices
        Prices prices = crawlPrices(internalID, internalPid, price, jsonSku);


        Product product = new Product();
        product.setUrl(this.session.getOriginalURL());
        product.setInternalId(internalID);
        product.setInternalPid(internalPid);
        product.setName(name);
        product.setPrice(price);
        product.setPrices(prices);
        product.setCategory1(category1);
        product.setCategory2(category2);
        product.setCategory3(category3);
        product.setPrimaryImage(primaryImageVariation);
        product.setSecondaryImages(secondaryImagesVariation);
        product.setDescription(description);
        product.setStock(stock);
        product.setMarketplace(marketplaces);
        product.setAvailable(available);

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
    Element producElement = doc.select(".title-product").first();

    if (producElement != null && !url.contains("?"))
      return true;
    return false;
  }



  /************************************
   * Multiple products identification *
   ************************************/

  private String crawlInternalPid(Document document) {
    String internalPid = null;
    Element elementInternalId = document.select("small.code").first();
    if (elementInternalId != null) {
      internalPid = elementInternalId.text().trim();
    }

    return internalPid;
  }


  /*******************
   * General methods *
   *******************/

  private String crawlName(Document doc) {
    String name = null;
    Element nameElement = doc.select(".title-product").first();

    if (nameElement != null) {
      name = nameElement.text();
    }

    return name;
  }

  private Float crawlPrice(JSONObject jsonSku, boolean available) {
    Float price = null;

    if (jsonSku.has("price")) {
      price = Float.parseFloat(jsonSku.getString("price").replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", ".").trim());
    }

    return price;
  }

  private String crawlInternalId(JSONObject jsonSku) {
    String internalID = null;

    if (jsonSku.has("productID")) {
      internalID = jsonSku.getString("productID");
    }

    return internalID;
  }

  private boolean crawlAvailability(Integer stock) {
    boolean available = false;

    if (stock != null) {
      if (stock > 0)
        return true;
    }

    return available;
  }

  private String crawlPrimaryImage(Document document, String name, JSONArray colorsImages) {
    String primaryImage = null;

    if (colorsImages.length() < 1) {
      Elements primaryImageElements = document.select("a.large-gallery");

      if (primaryImageElements != null)
        primaryImage = primaryImageElements.get(0).attr("href");

    } else {

      for (int i = 0; i < colorsImages.length(); i++) {
        JSONObject colorsJson = colorsImages.getJSONObject(i);
        String color = colorsJson.getString("color").toLowerCase();

        if (name.toLowerCase().contains(color) || name.toLowerCase().contains(color.substring(0, color.length() - 1))) {
          primaryImage = colorsJson.getString("primaryImage");
          break;
        }
      }

    }

    return primaryImage;

  }

  private String crawlSecondaryImages(Document document, String primaryImage, String name, JSONArray colorsImages) {
    String secondaryImages = null;
    Map<Integer, String> secondaryImagesMap = new HashMap<Integer, String>();
    JSONArray secondaryImagesArray = new JSONArray();

    if (colorsImages.length() < 1) {
      Elements elementFotoSecundaria = document.select("a.large-gallery");

      if (elementFotoSecundaria.size() > 1) {
        for (int i = 1; i < elementFotoSecundaria.size(); i++) { // starts with index 1 because de primary image is the first image
          Element e = elementFotoSecundaria.get(i);
          String secondaryImagesTemp = null;

          if (e != null) {
            secondaryImagesTemp = e.attr("href");

            if (!primaryImage.equals(secondaryImagesTemp)) { // identify if the image is the primary image
              secondaryImagesMap.put(i, secondaryImagesTemp);
            }
          }

        }
      }

      for (String image : secondaryImagesMap.values()) {
        secondaryImagesArray.put(image);
      }

    } else {

      for (int i = 0; i < colorsImages.length(); i++) {
        JSONObject colorsJson = colorsImages.getJSONObject(i);

        String color = colorsJson.getString("color").toLowerCase();

        if (name.toLowerCase().contains(color) || name.toLowerCase().contains(color.substring(0, color.length() - 1))) {
          secondaryImagesArray = colorsJson.getJSONArray("secondaryImages");
          break;
        }
      }
    }

    if (secondaryImagesArray.length() > 0) {
      secondaryImages = secondaryImagesArray.toString();
    }

    return secondaryImages;
  }

  private String crawlName(JSONObject jsonSku, String mainPageName) {
    String name = mainPageName;

    if (jsonSku.has("options")) {
      JSONArray jsonOptions = jsonSku.getJSONArray("options");

      for (int i = 0; i < jsonOptions.length(); i++) {
        JSONObject option = jsonOptions.getJSONObject(i);

        if (option.has("title")) {
          String nameVariation = option.getString("title").trim();

          if (!nameVariation.isEmpty() && !name.toLowerCase().contains(nameVariation.toLowerCase())) {
            name += " " + nameVariation;
          }
        }
      }
    }


    return name;
  }

  private ArrayList<String> crawlCategories(Document document) {
    Elements elementCategories = document.select(".wd-browsing-breadcrumbs li a span");
    ArrayList<String> categories = new ArrayList<String>();

    for (Element e : elementCategories) {
      String category = e.text().trim();

      if (!category.toLowerCase().contains("inicial")) {
        categories.add(category);
      }
    }

    return categories;
  }

  private String getCategory(ArrayList<String> categories, int n) {
    if (n < categories.size()) {
      return categories.get(n);
    }

    return "";
  }


  private Marketplace assembleMarketplaceFromMap(Map<String, Float> marketplaceMap) {
    return new Marketplace();
  }

  private Map<String, Float> crawlMarketplaces(Document doc) {
    Map<String, Float> marketplace = new HashMap<String, Float>();

    return marketplace;
  }

  private String crawlDescription(Document document) {
    String description = "";
    Element elementProductDetails = document.select(".products-info-container#basicinfotoggle").first();
    Element elementProductTec = document.select(".products-info-container#informacoes-tecnicas-0").first();

    if (elementProductDetails != null)
      description = description + elementProductDetails.html();
    if (elementProductTec != null)
      description = description + elementProductTec.html();

    return description;
  }

  private Map<String, String> identifyNumberOfColors(Document doc) {
    Map<String, String> colors = new HashMap<String, String>();
    Elements colorsElements = doc.select(".variation-group");
    Element colorElement = null;


    for (Element e : colorsElements) {
      if (e.select("b").text().equals("Cor")) {
        colorElement = e;
        break;
      }
    }

    if (colorElement != null) {
      Elements colorsElementsTemp = colorElement.select("option[id]");

      for (int i = 0; i < colorsElementsTemp.size(); i++) {
        Element e = colorsElementsTemp.get(i);
        colors.put(e.attr("value"), e.text().trim());
      }

    }

    return colors;
  }


  private JSONArray fetchImageColors(Map<String, String> colors, String url) {
    JSONArray colorsArray = new JSONArray();

    if (colors.size() > 0) {
      for (String idColor : colors.keySet()) {
        String urlColor = url + "?pp=/" + idColor + "/";
        JSONObject jsonColor = new JSONObject();
        jsonColor.put("color", colors.get(idColor));

        Request request = RequestBuilder.create().setUrl(urlColor).setCookies(cookies).build();
        Document doc = Jsoup.parse(this.dataFetcher.get(session, request).getBody());
        Elements colorsElements = doc.select("li.image");

        String primaryImage = colorsElements.get(0).select("img").attr("data-image-large");
        JSONArray secondaryImages = new JSONArray();

        for (Element e : colorsElements) {
          if (!e.hasAttr("style")) {
            String image = e.select("img").attr("data-image-large");

            if (e.hasClass("selected"))
              primaryImage = image;
            else
              secondaryImages.put(image);
          }
        }

        jsonColor.put("primaryImage", primaryImage);
        jsonColor.put("secondaryImages", secondaryImages);

        colorsArray.put(jsonColor);
      }
    }

    return colorsArray;
  }

  private Integer crawlStock(JSONObject jsonSku) {
    Integer stock = null;

    if (jsonSku.has("StockBalance")) {
      String stockString = jsonSku.getString("StockBalance");

      if (stockString.contains(",")) {
        stock = Integer.parseInt(stockString.split(",")[0].trim());
      } else {
        stock = Integer.parseInt(stockString);
      }
    }

    return stock;
  }

  private Prices crawlPrices(String internalId, String internalPid, Float price, JSONObject jsonSku) {
    Prices prices = new Prices();

    if (price != null) {
      if (jsonSku.has("priceDescription")) {
        String html =
            jsonSku.getString("priceDescription").replaceAll("&lt;", "<").replaceAll("&gt;", ">").replaceAll("&#39;", "\"").replaceAll("&quot;", "");

        Document docJson = Jsoup.parse(html);
        Element boleto = docJson.select(".instant-price").first();

        if (boleto != null) {
          Float inCashPrice = MathUtils.parseFloatWithComma(boleto.text());
          prices.setBankTicketPrice(inCashPrice);
        }

        String url = "http://www.estrela10.com.br/widget/product_payment_options?SkuID=" + internalId + "&ProductID=" + internalPid
            + "&Template=/wd.product.payment.options.result.custom.template";

        Request request = RequestBuilder.create().setUrl(url).setCookies(cookies).build();
        Document docPrices = Jsoup.parse(this.dataFetcher.get(session, request).getBody());

        Element indexCards = docPrices.select(".grid table th").first();

        if (indexCards != null) {
          Elements cards = indexCards.select("img");

          for (int i = 0; i < cards.size(); i++) {
            Element card = cards.get(i);

            // Nome cartao
            String cardName = card.attr("title").toLowerCase();

            if (cardName.contains("visa")) {
              Map<Integer, Float> installmentPriceMap = getInstallmentsForCard(docPrices, i);
              prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);

            } else if (cardName.contains("mastercard")) {
              Map<Integer, Float> installmentPriceMap = getInstallmentsForCard(docPrices, i);
              prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);

            } else if (cardName.contains("diners")) {
              Map<Integer, Float> installmentPriceMap = getInstallmentsForCard(docPrices, i);
              prices.insertCardInstallment(Card.DINERS.toString(), installmentPriceMap);

            } else if (cardName.contains("american") || cardName.contains("amex")) {
              Map<Integer, Float> installmentPriceMap = getInstallmentsForCard(docPrices, i);
              prices.insertCardInstallment(Card.AMEX.toString(), installmentPriceMap);

            } else if (cardName.contains("hipercard")) {
              Map<Integer, Float> installmentPriceMap = getInstallmentsForCard(docPrices, i);
              prices.insertCardInstallment(Card.HIPERCARD.toString(), installmentPriceMap);

            } else if (cardName.contains("credicard")) {
              Map<Integer, Float> installmentPriceMap = getInstallmentsForCard(docPrices, i);
              prices.insertCardInstallment(Card.CREDICARD.toString(), installmentPriceMap);

            } else if (cardName.contains("elo")) {
              Map<Integer, Float> installmentPriceMap = getInstallmentsForCard(docPrices, i);
              prices.insertCardInstallment(Card.ELO.toString(), installmentPriceMap);

            } else if (cardName.contains("aura")) {
              Map<Integer, Float> installmentPriceMap = getInstallmentsForCard(docPrices, i);
              prices.insertCardInstallment(Card.AURA.toString(), installmentPriceMap);

            } else if (cardName.contains("discover")) {
              Map<Integer, Float> installmentPriceMap = getInstallmentsForCard(docPrices, i);
              prices.insertCardInstallment(Card.DISCOVER.toString(), installmentPriceMap);

            }

          }
        }

      }
    }

    return prices;
  }

  private Map<Integer, Float> getInstallmentsForCard(Document doc, int idCard) {
    Map<Integer, Float> mapInstallments = new HashMap<>();

    Elements installmentsCards = doc.select(".grid table");

    if ((installmentsCards.size() - 1) >= idCard) {
      Element cardInstallment = installmentsCards.get(idCard);
      Elements installments = cardInstallment.select("tbody tr td");

      for (Element e : installments) {
        String text = e.text().toLowerCase();

        if (text.contains("vista")) {
          Float value = MathUtils.parseFloatWithComma(text);
          mapInstallments.put(1, value);
        } else {
          int x = text.indexOf("x") + 1;
          int y = text.indexOf("juros", x);

          Integer installment = Integer.parseInt(text.substring(0, x).replaceAll("[^0-9]", "").trim());
          Float value = MathUtils.parseFloatWithComma(text.substring(x, y));

          mapInstallments.put(installment, value);
        }
      }
    }

    return mapInstallments;
  }

  private boolean hasVariationsSku(Document doc) {
    Elements skus = doc.select(".sku-option");

    if (skus.size() > 2) {
      return true;
    }

    return false;
  }

  /**
   * Get the script having a json with the availability information
   * 
   * @return
   */
  private JSONArray crawlSkuJsonArray(Document document) {
    Elements scriptTags = document.getElementsByTag("script");
    JSONArray skuJson = null;

    for (Element tag : scriptTags) {
      for (DataNode node : tag.dataNodes()) {
        if (tag.html().trim().startsWith("var variants = ")) {

          skuJson = new JSONArray(node.getWholeData().split(Pattern.quote("var variants = "))[1]
              + node.getWholeData().split(Pattern.quote("var variants = "))[1].split(Pattern.quote("];"))[0]);

        }
      }
    }

    return skuJson;
  }
}
