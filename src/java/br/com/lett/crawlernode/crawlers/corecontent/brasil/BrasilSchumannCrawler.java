package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringEscapeUtils;
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
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
import models.Marketplace;
import models.prices.Prices;

/************************************************************************************************************************************************************************************
 * Crawling notes (23/08/2016):
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
 * 8) Variations of skus are not crawled if the variation is unavailable because it is not displayed
 * for the user, except if there is a html element voltage, because variations of voltage are
 * displayed for the user even though unavailable.
 * 
 * 9) The url of the primary images are changed to bigger dimensions manually.
 * 
 * 10) The secondary images are get in api "http://www.schumann.com.br/produto/sku/" + internalId
 * 
 * Examples: ex1 (available):
 * http://www.schumann.com.br/cafeteira-expresso-tres-coracoes-s04-modo-vermelha-multibebidas/p ex2
 * (unavailable):
 * http://www.schumann.com.br/ar-condicionado-springer-split-hi-wall-12000-btus-quente-e-frio/p
 *
 *
 ************************************************************************************************************************************************************************************/

public class BrasilSchumannCrawler extends Crawler {

  private static final String HOME_PAGE = "http://www.schumann.com.br/";

  public BrasilSchumannCrawler(Session session) {
    super(session);
  }

  @Override
  public boolean shouldVisit() {
    String href = session.getOriginalURL().toLowerCase();
    return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
  }


  @Override
  public List<Product> extractInformation(Document doc) throws Exception {
    super.extractInformation(doc);
    List<Product> products = new ArrayList<>();

    if (isProductPage(doc)) {
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

      String originalUrl = session.getOriginalURL();
      String mainPageName = crawlName(doc);
      String internalPid = this.crawlInternalPid(doc);
      CategoryCollection categories = crawlCategories(doc);
      String description = this.crawlDescription(doc);
      Map<String, String> colorsMap = this.identifyNumberOfColors(doc);
      JSONArray imageColorsArray = this.fetchImageColors(colorsMap, originalUrl);
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
        JSONObject jsonSku = jsonProducts.getJSONObject(i);

        String name = this.crawlName(jsonSku, mainPageName);
        String internalId = this.crawlInternalId(jsonSku);
        String redirectUrl = internalId != null ? CrawlerUtils.crawlFinalUrl(originalUrl, session) : originalUrl;
        Integer stock = crawlStock(jsonSku);
        boolean available = this.crawlAvailability(stock);
        Float price = this.crawlPrice(jsonSku);
        String primaryImage = this.crawlPrimaryImage(doc, name, imageColorsArray);
        String secondaryImages = this.crawlSecondaryImages(doc, primaryImage, name, imageColorsArray);
        Prices prices = crawlPrices(internalId, internalPid, price, jsonSku);

        // Creating the product
        Product product = ProductBuilder.create().setUrl(redirectUrl).setInternalId(internalId).setInternalPid(internalPid).setName(name)
            .setPrice(price).setPrices(prices).setAvailable(available).setCategory1(categories.getCategory(0)).setCategory2(categories.getCategory(1))
            .setCategory3(categories.getCategory(2)).setPrimaryImage(primaryImage).setSecondaryImages(secondaryImages).setDescription(description)
            .setStock(stock).setMarketplace(new Marketplace()).build();

        products.add(product);
      }

    } else {
      Logging.printLogDebug(logger, session, "Not a product page" + this.session.getOriginalURL());
    }

    return products;
  }


  private boolean isProductPage(Document doc) {
    return doc.select(".identification-product").first() != null;
  }

  private String crawlInternalPid(Document document) {
    String internalPid = null;

    Element elementInternalId = document.select("input[name=ProductID]").first();
    if (elementInternalId != null) {
      internalPid = elementInternalId.val().trim();
    }

    return internalPid;
  }

  private String crawlName(Document doc) {
    String name = null;
    Element nameElement = doc.select("h1[itemprop=name]").first();

    if (nameElement != null) {
      name = nameElement.text();
    }

    return name;
  }

  private Float crawlPrice(JSONObject jsonSku) {
    Float price = null;

    if (jsonSku.has("price")) {
      price = Float.parseFloat(jsonSku.getString("price").replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", ".").trim());
    }

    return price;
  }

  private String crawlInternalId(JSONObject jsonSku) {
    String internalID = null;

    if (jsonSku.has("sku")) {
      internalID = jsonSku.getString("sku");
    }

    return internalID;
  }

  private boolean crawlAvailability(Integer stock) {
    return stock != null && stock > 0;
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
    Map<Integer, String> secondaryImagesMap = new HashMap<>();
    JSONArray secondaryImagesArray = new JSONArray();

    if (colorsImages.length() < 1) {
      Elements elementFotoSecundaria = document.select("a.large-gallery");

      if (elementFotoSecundaria.size() > 1) {
        for (int i = 1; i < elementFotoSecundaria.size(); i++) { // starts with index 1 because de primary image is the first image
          Element e = elementFotoSecundaria.get(i);
          String secondaryImagesTemp = null;

          if (e != null) {
            secondaryImagesTemp = e.attr("href");

            if (!secondaryImagesTemp.equals(primaryImage)) { // identify if the image is the primary image
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
    StringBuilder name = new StringBuilder();
    name.append(mainPageName);

    if (jsonSku.has("options")) {
      JSONArray jsonOptions = jsonSku.getJSONArray("options");

      for (int i = 0; i < jsonOptions.length(); i++) {
        JSONObject option = jsonOptions.getJSONObject(i);

        if (option.has("title")) {
          String nameVariation = StringEscapeUtils.unescapeHtml4(option.getString("title").trim());

          if (!nameVariation.isEmpty() && !name.toString().toLowerCase().contains(nameVariation.toLowerCase())) {
            name.append(" " + nameVariation);
          }
        }
      }
    }


    return name.toString();
  }

  private CategoryCollection crawlCategories(Document document) {
    CategoryCollection categories = new CategoryCollection();
    Elements elementCategories = document.select(".wd-browsing-breadcrumbs li:not(.first) a span");

    for (Element e : elementCategories) {
      categories.add(e.text().trim());
    }

    return categories;
  }

  private String crawlDescription(Document document) {
    StringBuilder description = new StringBuilder();
    Element elementProductDetails = document.select(".table-informations").first();

    if (elementProductDetails != null) {
      description.append(elementProductDetails.html());
    }

    Elements descriptions = document.select(".descriptions");
    for (Element e : descriptions) {
      description.append(e.html());
    }

    return description.toString();
  }

  private Map<String, String> identifyNumberOfColors(Document doc) {
    Map<String, String> colors = new HashMap<>();
    Elements colorsElements = doc.select(".variation-group");
    Element colorElement = null;

    for (Element e : colorsElements) {
      Element type = e.select(".title").first();
      if (type != null && type.text().trim().equalsIgnoreCase("Cor")) {
        colorElement = e;
        break;
      }
    }

    if (colorElement != null) {
      Elements colorsElementsTemp = colorElement.select("label[title]");

      for (Element e : colorsElementsTemp) {
        Element id = e.select("input").first();
        if (id != null) {
          colors.put(id.val().trim(), e.attr("title"));
        }
      }
    }

    return colors;
  }


  private JSONArray fetchImageColors(Map<String, String> colors, String url) {
    JSONArray colorsArray = new JSONArray();

    for (Entry<String, String> entry : colors.entrySet()) {
      String urlColor = url + "?pp=/" + entry.getKey() + "/";
      JSONObject jsonColor = new JSONObject();
      jsonColor.put("color", entry.getValue());

      Document doc = DataFetcher.fetchDocument(DataFetcher.GET_REQUEST, session, urlColor, null, null);
      Elements colorsElements = doc.select("li.image");

      if (!colorsElements.isEmpty()) {

        String primaryImage = colorsElements.get(0).select("img").attr("data-image-large");
        JSONArray secondaryImages = new JSONArray();

        for (Element e : colorsElements) {
          if (!e.hasAttr("style")) {
            String image = e.select("img").attr("data-image-large");

            if (e.hasClass("selected")) {
              primaryImage = image;
            } else {
              secondaryImages.put(image);
            }
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

    if (price != null && jsonSku.has("priceDescription")) {
      String html =
          jsonSku.getString("priceDescription").replaceAll("&lt;", "<").replaceAll("&gt;", ">").replaceAll("&#39;", "\"").replaceAll("&quot;", "");

      Map<Integer, Float> installmentPriceMap = new HashMap<>();
      installmentPriceMap.put(1, price);

      Document docJson = Jsoup.parse(html);
      Element boleto = docJson.select(".instant-price").first();

      if (boleto != null) {
        Float inCashPrice = MathUtils.parseFloatWithComma(boleto.text());
        installmentPriceMap.put(1, inCashPrice);
        prices.setBankTicketPrice(inCashPrice);
      }

      Element parcels = docJson.select(".condition .parcels").first();
      Element parcelValue = docJson.select(".condition .parcel-value").first();

      if (parcels != null && parcelValue != null) {
        String installment = parcels.ownText().replaceAll("[^0-9]", "").trim();
        Float value = MathUtils.parseFloatWithComma(parcelValue.ownText());

        if (!installment.isEmpty() && value != null) {
          installmentPriceMap.put(Integer.parseInt(installment), value);
        }
      }

      prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.AMEX.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.DINERS.toString(), installmentPriceMap);

      Element priceFrom = docJson.select(".list-price > span").first();
      if (priceFrom != null) {
        prices.setPriceFrom(MathUtils.parseDoubleWithComma(priceFrom.ownText()));
      }
    }

    return prices;
  }

  private boolean hasVariationsSku(Document doc) {
    return doc.select(".information .sku-option").size() > 2;
  }

  /**
   * Get the script having a json with the availability information
   * 
   * @return
   */
  private JSONArray crawlSkuJsonArray(Document document) {
    Elements scriptTags = document.getElementsByTag("script");
    JSONArray skuJson = new JSONArray();

    String token = "var variants = ";

    for (Element tag : scriptTags) {
      for (DataNode node : tag.dataNodes()) {
        if (tag.html().trim().startsWith(token)) {

          skuJson = new JSONArray(
              node.getWholeData().split(Pattern.quote(token))[1] + node.getWholeData().split(Pattern.quote(token))[1].split(Pattern.quote("];"))[0]);

        }
      }
    }

    return skuJson;
  }
}
