package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.DataNode;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.BrowserType;
import org.openqa.selenium.remote.DesiredCapabilities;
import br.com.lett.crawlernode.core.fetcher.CrawlerWebdriver;
import br.com.lett.crawlernode.core.fetcher.DesiredCapabilitiesBuilder;
import br.com.lett.crawlernode.core.fetcher.DynamicDataFetcher;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
import models.Marketplace;
import models.Seller;
import models.Util;
import models.prices.Prices;

public class BrasilCdiscountCrawler extends Crawler {

  public BrasilCdiscountCrawler(Session session) {
    super(session);
  }

  @Override
  public List<Product> extractInformation(Document doc) throws Exception {
    super.extractInformation(doc);
    List<Product> products = new ArrayList<Product>();

    if (isProductPage(doc)) {
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

      // crawl the defaul loaded sku
      Product product = crawlSKU(doc);
      products.add(product);

      if (hasMultipleSKUs(doc)) {
        Logging.printLogDebug(logger, session, "Multiple variations on the same page.");

        // initialize the webdriver
        Logging.printLogDebug(logger, session, "Creating WebDriver instance...");
        initWebdriver();

        // load main page
        Logging.printLogDebug(logger, session, "Loading main page via WebDriver...");
        this.webdriver.loadUrl(session.getOriginalURL());

        // getting all the sku options
        List<WebElement> webElements = this.webdriver.findElementsByCssSelector("select.listaSku.selSku option");

        // iterate through each sku selector
        for (WebElement skuOption : webElements) {

          // eliminate the sku already crawled
          if (isValidOption(skuOption) && !containsId(product.getInternalId(), skuOption)) {
            String skuOptionText = skuOption.getText();

            // clicking on the option
            Logging.printLogDebug(logger, session, "Clicking on option " + skuOptionText);
            skuOption.click();

            // give some time for the webdriver
            Logging.printLogDebug(logger, session, "Waiting WebDriver for 5 seconds...");
            this.webdriver.waitLoad(5000);

            // get html via code using css selector
            String html = this.webdriver.findElementByCssSelector("html").getAttribute("innerHTML");
            Document variationDocument = Jsoup.parse(html);

            // crawl the selected sku
            Product variation = crawlSKU(variationDocument);
            products.add(variation);
          }
        }

      }

    } else {
      Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
    }

    return products;
  }

  /**
   * Looks if the sku selector option contains valid sku data. In some cases the selector doesn't
   * display the sku info, but rather information about the selector itself.
   * 
   * e.g: <option value="">Selecione uma opção</option>
   * 
   * We eliminate the above case and catch only sku selectors:
   * 
   * e.g: <option value="24084">110V | R$ 355,60</option>
   * 
   * @param skuOption
   * @return
   */
  private boolean isValidOption(WebElement skuOption) {
    List<String> parsedIds = MathUtils.parseNumbers(skuOption.getAttribute("value").trim());
    if (parsedIds.size() > 0)
      return true;
    return false;
  }

  /**
   * Looks if the id is on the current option. Used when we are iterating through all sku options on
   * the page, and want to avoid crawl skus that we already crawled.
   * 
   * @param id
   * @param skuOption
   * @return
   */
  private boolean containsId(String id, WebElement skuOption) {
    List<String> parsedIds = MathUtils.parseNumbers(skuOption.getAttribute("value").trim());
    if (parsedIds.size() > 0 && parsedIds.get(0).contains(id))
      return true;
    return false;
  }

  private Product crawlSKU(Document document) {
    JSONObject skuInformationJSON = crawlSiteMetadataJSONObject(document);

    String internalId = crawlInternalId(document);
    String internalPid = crawlInternalPid(skuInformationJSON);
    String name = crawlName(document);
    Float price = crawlPrice(document);
    Prices prices = crawlPrices(document);
    boolean available = crawlAvailability(document);
    String primaryImage = crawlPrimaryImage(document);
    String secondaryImages = crawlSecondaryImages(document);
    String description = crawlDescription(document);

    Map<String, Prices> marketplaceMap = crawlMarketplace(document);

    Marketplace marketplace = new Marketplace();

    if (marketplaceMap.size() > 0) {
      for (String sellerName : marketplaceMap.keySet()) {
        JSONObject sellerJSON = new JSONObject();
        Float sellerPrice = price;

        sellerJSON.put("name", sellerName);
        sellerJSON.put("price", sellerPrice);
        sellerJSON.put("prices", marketplaceMap.get(sellerName).toJSON());

        try {
          Seller seller = new Seller(sellerJSON);
          marketplace.add(seller);
        } catch (Exception e) {
          Logging.printLogWarn(logger, session, Util.getStackTraceString(e));
        }
      }

      // if we have a marketplace than the product is unavailable on main market
      available = false;
      price = null;
      prices = new Prices();
    }

    Integer stock = null;

    ArrayList<String> categories = crawlCategories(document);
    String category1 = getCategory(categories, 0);
    String category2 = getCategory(categories, 1);
    String category3 = getCategory(categories, 2);

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

    return product;
  }

  private String crawlSecondaryImages(Document document) {
    String secondaryImages = null;
    JSONArray secondaryImagesArray = new JSONArray();

    Elements imagesElements = document.select("div.boxImg div.carouselBox ul li a");

    for (int i = 1; i < imagesElements.size(); i++) { // starting from index 1, because the first is the primary image
      String secondaryImage = imagesElements.get(i).attr("rev").trim();
      if (!secondaryImage.isEmpty()) {
        secondaryImagesArray.put(secondaryImage);
      } else {
        String relAttribute = imagesElements.get(i).attr("rel").trim();
        if (!relAttribute.isEmpty()) {
          secondaryImagesArray.put(parseImageURLFromRelAttribute(relAttribute));
        }
      }
    }

    if (secondaryImagesArray.length() > 0) {
      secondaryImages = secondaryImagesArray.toString();
    }

    return secondaryImages;
  }

  private Map<String, Prices> crawlMarketplace(Document document) {
    Map<String, Prices> marketplaces = new HashMap<>();

    Element sellerElement = document.select("div.buying a").first();
    if (sellerElement != null) {
      String sellerName = sellerElement.attr("title").trim().toLowerCase();

      if (!sellerName.equals("cdiscount") && !sellerName.isEmpty()) {
        Prices sellerPrices = crawlPrices(document);
        marketplaces.put(sellerName, sellerPrices);
      }
    }

    return marketplaces;
  }

  private String parseImageURLFromRelAttribute(String relAttribute) {
    String imageURL = null;

    imageURL = parseImageURL("largeimage", relAttribute);
    if (imageURL == null) {
      imageURL = parseImageURL("smallimage", relAttribute);
    }

    return imageURL;
  }

  /**
   * rel= { gallery: 'produtoPrincipal', smallimage:
   * 'http://www.cdiscount-imagens.com.br/Eletroportateis/Cafeteiras/CafeteirasEletricas/24084/3235554/Cafeteira-Eletrica-Philips-Walita-RI7546-c-Jarra-em-Inox-24084.jpg',
   * largeimage: '#' }
   * 
   * @param imageSize
   * @param relAttribute
   * @return
   */
  private String parseImageURL(String imageSize, String relAttribute) {
    String imageURL = null;
    int beginIndex = relAttribute.indexOf(imageSize + ": \'");
    String largeImageSubstring = relAttribute.substring(beginIndex, relAttribute.length());

    int srcIndex = largeImageSubstring.indexOf("\'");
    int endIndex = largeImageSubstring.indexOf(".jpg"); // must append the extension on the final url

    if (endIndex > srcIndex) {
      imageURL = largeImageSubstring.substring(srcIndex, endIndex).replaceAll("'", "") + ".jpg";
    }

    return imageURL;
  }

  /**
   * All the card payment methods are the same accross all card brands.
   * 
   * @param document
   * @return
   */
  private Prices crawlPrices(Document document) {
    Prices prices = new Prices();

    // bank slip
    Float bankSlipPrice = crawlPrice(document);
    prices.setBankTicketPrice(bankSlipPrice);

    // card payment options
    Map<Integer, Float> installments = crawlCardInstallments(document);
    prices.insertCardInstallment(Card.VISA.toString(), installments);
    prices.insertCardInstallment(Card.MASTERCARD.toString(), installments);
    prices.insertCardInstallment(Card.AMEX.toString(), installments);
    prices.insertCardInstallment(Card.DINERS.toString(), installments);
    prices.insertCardInstallment(Card.ELO.toString(), installments);
    prices.insertCardInstallment(Card.HIPERCARD.toString(), installments);

    return prices;
  }

  private ArrayList<String> crawlCategories(Document document) {
    ArrayList<String> categories = new ArrayList<String>();
    return categories;
  }

  private String getCategory(ArrayList<String> categories, int n) {
    if (n < categories.size()) {
      return categories.get(n);
    }
    return "";
  }

  private Map<Integer, Float> crawlCardInstallments(Document document) {
    Map<Integer, Float> installments = new TreeMap<Integer, Float>();
    Elements trElements = document.select("div.parcelamento ul.tabsCont .tabCont.selected .parcelCartao table tbody tr");
    for (Element trElement : trElements) {
      Element installmentNumberElement = trElement.select("th").first();
      Element installmentPriceElement = trElement.select("td").first();

      if (installmentNumberElement != null && installmentPriceElement != null) {
        List<String> parsedNumbers = MathUtils.parseNumbers(installmentNumberElement.text());

        if (parsedNumbers.size() > 0) {
          Integer installmentNumber = Integer.parseInt(parsedNumbers.get(0));
          Float installmentPrice = MathUtils.parseFloatWithComma(installmentPriceElement.text());

          installments.put(installmentNumber, installmentPrice);
        }
      }
    }

    return installments;
  }

  private boolean crawlAvailability(Document document) {
    boolean available = true;

    Element unavailableElement = document.select(".alertaIndisponivel.box3").first();
    if (unavailableElement != null) {
      available = false;
    }

    return available;
  }

  private Float crawlPrice(Document document) {
    Float price = null;

    Element priceElement = document.select("#ctl00_Conteudo_ctl22_precoPorValue i").first();
    if (priceElement != null) {
      price = MathUtils.parseFloatWithComma(priceElement.text());
    }

    return price;
  }

  private String crawlInternalId(Document document) {
    String internalId = null;

    Element internalIdElement = document.select("span[itemprop=productID]").first();
    if (internalIdElement != null) {
      List<String> parsedNumbers = MathUtils.parseNumbers(internalIdElement.text());
      if (parsedNumbers.size() > 0) {
        internalId = parsedNumbers.get(0);
      }
    }

    return internalId;
  }

  private String crawlInternalPid(JSONObject skuInformationJSON) {
    String internalPid = null;

    if (skuInformationJSON.has("product")) {
      JSONObject product = skuInformationJSON.getJSONObject("product");

      if (product.has("idProduct")) {
        internalPid = product.getString("idProduct");
      }
    }

    return internalPid;
  }

  private String crawlName(Document document) {
    String name = null;

    // get the first part of the name
    Element nameElement = document.select("b[itemprop=name]").first();
    if (nameElement != null) {
      name = nameElement.text().trim();
    }

    // get the variation part
    String internalId = crawlInternalId(document);
    if (internalId != null) {
      Elements optionsElements = document.select("select.listaSku.selSku option");
      for (Element option : optionsElements) {
        if (option.attr("value").trim().equals(internalId)) {
          String variation = option.text(); // 110V | R$ 355,60

          String[] tokens = variation.split("\\|");
          if (tokens.length > 0) {
            variation = tokens[0].trim();
            name = name + " " + variation;
          }
        }
      }
    }

    return name;
  }

  private void initWebdriver() {
    DesiredCapabilities capabilities =
        DesiredCapabilitiesBuilder.create().setBrowserType(BrowserType.PHANTOMJS).setUserAgent(DynamicDataFetcher.randUserAgent()).build();

    this.webdriver = new CrawlerWebdriver(capabilities, session);
  }

  private String crawlDescription(Document document) {
    String description = "";

    Element skuDetailsElement = document.select("div#detalhes").first();
    if (skuDetailsElement != null) {
      description = skuDetailsElement.html();
    }

    return description;
  }

  private String crawlPrimaryImage(Document document) {
    String primaryImage = null;

    Element primaryImageElement = document.select("div#divFullImage a").first();
    if (primaryImageElement != null) {
      primaryImage = primaryImageElement.attr("href").trim();
    }

    return primaryImage;
  }

  private boolean isProductPage(Document document) {
    return (document.select("#ctl00_Conteudo_22").first() != null);
  }

  private JSONObject crawlSiteMetadataJSONObject(Document document) {
    Elements scriptTags = document.getElementsByTag("script");
    JSONObject skuJson = null;
    JSONObject pageMetadataJSON = null;

    for (Element tag : scriptTags) {
      for (DataNode node : tag.dataNodes()) {
        if (tag.html().trim().startsWith("var siteMetadata = ")) {
          skuJson = new JSONObject(node.getWholeData().split(Pattern.quote("var siteMetadata = "))[1]
              + node.getWholeData().split(Pattern.quote("var siteMetadata = "))[1].split(Pattern.quote("}};"))[0]);
        }
      }
    }

    if (skuJson != null && skuJson.has("page")) {
      pageMetadataJSON = skuJson.getJSONObject("page");
    } else {
      pageMetadataJSON = new JSONObject();
    }

    return pageMetadataJSON;
  }

  private boolean hasMultipleSKUs(Document document) {
    return (document.select("div#ctl00_Conteudo_divSelSku").first() != null);
  }
}
