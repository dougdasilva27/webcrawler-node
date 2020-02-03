package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
import models.Marketplace;
import models.prices.Prices;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.*;

/*************************************************************************************************************************
 * Crawling notes (01/08/2016):
 * 
 * 1) For this crawler, we have multiple skus on the same page. In cases we have color selector, we
 * have one URL per sku. 2) There is no stock information for skus in this ecommerce by the time
 * this crawler was made. 3) There is no marketplace in this ecommerce by the time this crawler was
 * made. 4) The sku page identification is done simply looking for an specific html element. 5) Even
 * if a product is unavailable, its price is not displayed if product has no variations. 6) There is
 * internalPid for skus in this ecommerce. The internalPid is a number that is the same for all the
 * variations of a given sku. 7) The primary image is the first image in the secondary images
 * selector. 8) To get price of variations is accessed a api to get them. 9) In products have
 * variations, price is displayed if product is unavailable.
 * 
 * Price crawling notes: 1) We have two distinct methods to crawl the payment options. One is used
 * in cases where we don't have sku variations on the same page, and the other is used when we have
 * multiple variations on a page. These variations doesn't include colors variations, because in
 * these cases we have one URL per sku. 2) When crawling prices for multiple variations we use an
 * API response, but for no variations we simply crawl the data from the html. 3) To crawl the
 * installments prices when fetching data from API, we must compute each installment value by
 * dividing the base price for a installment number.
 * 
 * Examples: ex1 (available):
 * https://www.econtinental.com.br/fogao-electrolux-chef-super-52sb-piso-4-bocas-branco-chama-rapida-bivolt
 * ex2 (unavailable):
 * https://www.econtinental.com.br/ar-split-cassete-springer-48000-btus-quente-e-frio-220v ex3
 * (variations):
 * https://www.econtinental.com.br/lavadora-de-roupas-electrolux-turbo-15kg-turbo-branca
 *
 * Optimizations notes: No optimizations.
 *
 ***************************************************************************************************************************/

public class BrasilEcontinentalCrawler extends Crawler {

  private final String HOME_PAGE = "https://www.econtinental.com.br/";

  public BrasilEcontinentalCrawler(Session session) {
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

      Elements variations = doc.select("#ctrEscolheTamanho li a");
      if (variations.size() == 0)
        variations = doc.select("#ctrEscolheTamanho option[valormoeda]");

      if (variations.size() > 0) {

        /*
         * *********************************** crawling data of mutiple products *
         *************************************/

        for (Element e : variations) {

          // InternalID
          String internalId = e.attr("idgrade").trim();
          if (internalId.isEmpty()) {
            internalId = e.attr("value");
          }

          // Name Variation
          String nameVariation = e.text();

          // Pid
          String internalPid = crawlInternalPid(doc);

          // Name
          String name = crawlName(doc) + " - " + nameVariation;

          // JSON price
          JSONObject jsonPrice = this.fetchPriceAPIResponse(internalId);

          // Availability
          boolean available = crawlAvailabilityVariation(e);

          // Price
          Float price = crawlPriceVariation(jsonPrice);

          // Prices
          Prices prices = crawlPricesVariationsCase(jsonPrice);

          // Categories
          ArrayList<String> categories = crawlCategories(doc);
          String category1 = getCategory(categories, 0);
          String category2 = getCategory(categories, 1);
          String category3 = getCategory(categories, 2);

          // Primary image
          String primaryImage = crawlPrimaryImage(doc);

          // Secondary images
          String secondaryImages = crawlSecondaryImages(doc);

          // Description
          String description = crawlDescription(doc);

          // Stock
          Integer stock = crawlStock(e);

          // Marketplace map
          Map<String, Float> marketplaceMap = crawlMarketplace(doc);

          // Marketplace
          Marketplace marketplace = assembleMarketplaceFromMap(marketplaceMap);

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
      }

      else {

        /*
         * *********************************** crawling data of only one product *
         *************************************/

        // InternalId
        String internalId = crawlInternalId(doc);

        // Pid
        String internalPid = crawlInternalPid(doc);

        // Name
        String name = crawlName(doc);

        // Availability
        boolean available = crawlAvailability(doc);

        // Price
        Float price = crawlMainPagePrice(doc, available);

        // Prices
        Prices prices = crawlPricesNoVariationsCase(doc);

        // Categories
        ArrayList<String> categories = crawlCategories(doc);
        String category1 = getCategory(categories, 0);
        String category2 = getCategory(categories, 1);
        String category3 = getCategory(categories, 2);

        // Primary image
        String primaryImage = crawlPrimaryImage(doc);

        // Secondary images
        String secondaryImages = crawlSecondaryImages(doc);

        // Description
        String description = crawlDescription(doc);

        // Stock
        Integer stock = null;

        // Marketplace map
        Map<String, Float> marketplaceMap = crawlMarketplace(doc);

        // Marketplace
        Marketplace marketplace = assembleMarketplaceFromMap(marketplaceMap);

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

  private boolean isProductPage(Document document) {
    return document.select(".content-product").first() != null || document.select(".promotion-product-container").first() != null;
  }

  /*********************
   * Variation product *
   *********************/

  /**
   * Fetch an API response to crawl price information. This response is used to get the normal price
   * and all the payment options as well.
   * 
   * @param internalID
   * @return
   */
  private JSONObject fetchPriceAPIResponse(String internalID) {
    String urlVariation = "https://www.econtinental.com.br/produto/do_escolhe_variacao?idgrade=" + internalID;

    Map<String, String> headers = new HashMap<>();
    headers.put("Content-Type", "application/json");
    headers.put("X-Requested-With", "XMLHttpRequest");

    Request request = RequestBuilder.create().setUrl(urlVariation).setCookies(cookies).setHeaders(headers).setPayload("").build();
    return CrawlerUtils.stringToJson(this.dataFetcher.post(session, request).getBody());
  }

  private Float crawlPriceVariation(JSONObject jsonPrice) {
    Float price = null;
    if (jsonPrice.has("valor")) {
      String priceString = jsonPrice.getString("valor");
      if (!priceString.isEmpty()) {
        price = MathUtils.parseFloatWithComma(priceString);
      }
    }
    return price;
  }

  /**
   * The payment options are the same across card brands.
   * 
   * @param jsonPrice
   * @return
   */
  private Prices crawlPricesVariationsCase(JSONObject jsonPrice) {
    Prices prices = new Prices();

    // bank slip
    if (jsonPrice.has("valorVistaNum")) {
      Double bankSlipPriceDouble = jsonPrice.getDouble("valorVistaNum");
      Float bankSlipPriceFloat = new Float(bankSlipPriceDouble);
      if (!bankSlipPriceFloat.equals(0.0f)) {
        prices.setBankTicketPrice(MathUtils.normalizeTwoDecimalPlaces(bankSlipPriceFloat));
      }
    }

    // installments
    Map<Integer, Float> installments = new TreeMap<Integer, Float>();
    if (jsonPrice.has("valor") && jsonPrice.has("parcelaSemJuros")) {
      String priceString = jsonPrice.getString("valor");
      if (!priceString.isEmpty()) {
        Float basePrice = MathUtils.parseFloatWithComma(priceString);
        Integer maxInstallmentNumber = jsonPrice.getInt("parcelaSemJuros");

        for (int i = 1; i < maxInstallmentNumber; i++) { // calculate each installment price
          Float installmentPrice = MathUtils.normalizeTwoDecimalPlaces(basePrice / i);
          installments.put(i, installmentPrice);
        }
      }
    }

    if (installments.size() > 0) {
      prices.insertCardInstallment(Card.MASTERCARD.toString(), installments);
      prices.insertCardInstallment(Card.VISA.toString(), installments);
      prices.insertCardInstallment(Card.AMEX.toString(), installments);
      prices.insertCardInstallment(Card.DINERS.toString(), installments);
      prices.insertCardInstallment(Card.ELO.toString(), installments);
      prices.insertCardInstallment(Card.HIPERCARD.toString(), installments);
    }


    return prices;
  }

  private boolean crawlAvailabilityVariation(Element e) {
    if (e.hasAttr("maxqtdcompratam") && !e.attr("maxqtdcompratam").isEmpty()) {
      int stock = Integer.parseInt(e.attr("maxqtdcompratam").replaceAll("[^0-9]", "").trim());
      return stock > 0;
    } else if (e.hasAttr("maxqtdcompra") && !e.attr("maxqtdcompra").isEmpty()) {
      int stock = Integer.parseInt(e.attr("maxqtdcompra").replaceAll("[^0-9]", "").trim());
      return stock > 0;
    }

    return false;
  }

  /**
   * In cases with attribute maxqtdcompra, stock is not trusted
   * 
   * @param e
   * @return
   */
  private Integer crawlStock(Element e) {
    Integer stock = null;

    if (e.hasAttr("maxqtdcompratam") && !e.attr("maxqtdcompratam").isEmpty()) {
      stock = Integer.parseInt(e.attr("maxqtdcompratam").replaceAll("[^0-9]", "").trim());
    }

    // else if (e.hasAttr("maxqtdcompra") && !e.attr("maxqtdcompra").isEmpty()) {
    // stock = Integer.parseInt(e.attr("maxqtdcompra").replaceAll("[^0-9]", "").trim());
    // }

    return stock;
  }

  /*******************
   * General methods *
   *******************/

  private String crawlInternalId(Document document) {
    String internalId = null;
    Element internalIdElement = document.select("#ctrIdGrade").first();

    if (internalIdElement != null) {
      internalId = internalIdElement.attr("value");
    }

    return internalId;
  }

  private String crawlInternalPid(Document document) {
    String internalPid = null;
    Element internalPidElement = document.select(".submensagem").first();

    if (internalPidElement != null) {
      internalPid = internalPidElement.text().trim();

      if (internalPid.contains(":")) {
        int x = internalPid.indexOf(":");

        internalPid = internalPid.substring(x + 1).trim();
      }
    }

    return internalPid;
  }

  private String crawlName(Document document) {
    String name = null;
    Element nameElement = document.select("h1[itemprop=name]").first();

    if (nameElement != null) {
      name = nameElement.ownText().trim();
    }

    return name;
  }

  private Float crawlMainPagePrice(Document document, boolean available) {
    Float price = null;
    Element specialPrice = document.select(".new-value-by .ctrValorMoeda").first();

    if (specialPrice != null && available) {
      price = Float.parseFloat(specialPrice.text().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", "."));
    }

    return price;
  }

  private Prices crawlPricesNoVariationsCase(Document document) {
    Prices prices = new Prices();

    // bank slip
    Float bankSlipPrice = crawlMainPageBankSlipPrice(document);
    if (bankSlipPrice != null) {
      prices.setBankTicketPrice(bankSlipPrice);
    }

    // installments
    Map<Integer, Float> installments = new TreeMap<Integer, Float>();
    Elements installmentElements = document.select("div.parcels-payment .parcels li");
    for (Element installmentElement : installmentElements) {
      Element installmentNumberElement = installmentElement.select("strong").first();
      Element installmentPriceElement = installmentElement.select("strong").last();

      if (installmentNumberElement != null && installmentPriceElement != null) {
        Integer installmentNumber = Integer.parseInt(installmentNumberElement.text());
        Float installmentPrice = MathUtils.parseFloatWithComma(installmentPriceElement.text());

        installments.put(installmentNumber, installmentPrice);
      }
    }

    if (installments.size() > 0) {
      prices.insertCardInstallment(Card.MASTERCARD.toString(), installments);
      prices.insertCardInstallment(Card.VISA.toString(), installments);
      prices.insertCardInstallment(Card.AMEX.toString(), installments);
      prices.insertCardInstallment(Card.DINERS.toString(), installments);
      prices.insertCardInstallment(Card.ELO.toString(), installments);
      prices.insertCardInstallment(Card.HIPERCARD.toString(), installments);
    }

    return prices;
  }

  private Float crawlMainPageBankSlipPrice(Document document) {
    Float bankSlipPrice = null;
    Element bankSlipPriceElement = document.select(".product-info .new-value.ctrValorArea span.billet-value").first();
    if (bankSlipPriceElement != null) {
      bankSlipPrice = MathUtils.parseFloatWithComma(bankSlipPriceElement.text());
    }
    return bankSlipPrice;
  }

  private boolean crawlAvailability(Document document) {
    Element notifyMeElement = document.select(".produto-indisponivel").first();

    return notifyMeElement == null;
  }

  private Map<String, Float> crawlMarketplace(Document document) {
    return new HashMap<String, Float>();
  }

  private Marketplace assembleMarketplaceFromMap(Map<String, Float> marketplaceMap) {
    return new Marketplace();
  }

  private String crawlPrimaryImage(Document document) {
    String primaryImage = null;
    Element primaryImageElement = document.select(".ctrFotoPrincipalZoom").first();

    if (primaryImageElement != null) {
      primaryImage = primaryImageElement.attr("href").trim().replaceAll("det", "original"); // montando imagem com zoom
    }

    return primaryImage;
  }

  private String crawlSecondaryImages(Document document) {
    String secondaryImages = null;
    JSONArray secondaryImagesArray = new JSONArray();

    Elements imagesElement = document.select(".maisfotos-foto a");

    for (int i = 1; i < imagesElement.size(); i++) { // start with index 1 because the first image is the primary image
      secondaryImagesArray.put(imagesElement.get(i).attr("urlfoto").trim().replaceAll("det", "original"));
    }

    if (secondaryImagesArray.length() > 0) {
      secondaryImages = secondaryImagesArray.toString();
    }

    return secondaryImages;
  }

  private ArrayList<String> crawlCategories(Document document) {
    ArrayList<String> categories = new ArrayList<String>();
    Elements elementCategories = document.select("li.f-l a span");

    for (int i = 0; i < elementCategories.size(); i++) {
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
    Element descriptionElement = document.select(".product-description").first();

    if (descriptionElement != null)
      description = description + descriptionElement.html();

    return description;
  }
}
