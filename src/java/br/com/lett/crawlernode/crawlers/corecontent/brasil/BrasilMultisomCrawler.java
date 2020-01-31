package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
import models.Marketplace;
import models.prices.Prices;
import org.json.JSONArray;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.*;


/************************************************************************************************************************************************************************************
 * Crawling notes (01/08/2016):
 * 
 * 1) For this crawler, we have one url per each sku.
 * 
 * 2) There is no stock information for skus in this ecommerce by the time this crawler was made.
 * 
 * 3) There is no marketplace in this ecommerce by the time this crawler was made.
 * 
 * 4) The sku page identification is done simply looking for an specific html element.
 * 
 * 5) Even if a product is unavailable, its price is not displayed.
 * 
 * 6) There is no internalPid for skus in this ecommerce. The internalPid must be a number that is
 * the same for all the variations of a given sku.
 * 
 * 7) The primary image is the first image in the secondary images selector.
 * 
 * 8) To crawled the variations, there are urls in the HTML element on the page, except for the
 * product that is already loaded it is not necessary to reload
 * 
 * Examples: ex1 (available):
 * http://www.multisom.com.br/produto/ar-condicionado-split-lg-inverter-libero-18-000-btus-quente-frio-220v-tecnologia-inverter-economia-de-energia-modelo-art-cool-asuw182crg2-2535
 * ex2 (unavailable): http://www.multisom.com.br/produto/escaleta-stagg-melosta-32-teclas-preto-5668
 * ex3 (variations):
 * https://www.multisom.com.br/produto/tv-65-lg-led-uf8500-ultra-hd-4k-webos-3d-smart-tv-sistema-webos-wi-fi-painel-ips-entradas-3-hdmi-e-3-usb-controle-smart-magic-4-oculos-3d-6516
 *
 * Optimizations notes: No optimizations.
 *
 ************************************************************************************************************************************************************************************/

public class BrasilMultisomCrawler extends Crawler {

  private final String HOME_PAGE = "https://www.multisom.com.br/";

  public BrasilMultisomCrawler(Session session) {
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

      /*
       * *********************************** crawling data of only one product *
       *************************************/

      // InternalId
      String internalIDFirstProduct = crawlInternalId(doc);

      // Pid
      String internalPid = crawlInternalPid(doc);

      // Name
      String name = crawlName(doc);

      // Price
      Float price = crawlMainPagePrice(doc);

      // Prices
      Prices prices = crawlPrices(doc);

      // Availability
      boolean available = crawlAvailability(doc);

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
      product.setInternalId(internalIDFirstProduct);
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

      // ArrayList<String> variationsUrls = this.crawlUrlForMutipleVariations(doc,
      // internalIDFirstProduct);
      //
      // for(String urlVariation : variationsUrls){
      // Document docVariation = this.fetchPageVariation(urlVariation);
      //
      // // InternalId
      // String internalIDVariation = crawlInternalId(docVariation);
      //
      // // Name
      // String nameVariation = crawlName(docVariation);
      //
      // // Price
      // Float priceVariation = crawlMainPagePrice(docVariation);
      //
      // // Availability
      // boolean availableVariation = crawlAvailability(docVariation);
      //
      // // Categories
      // ArrayList<String> categoriesVariation = crawlCategories(docVariation);
      // String category1Variation = getCategory(categoriesVariation , 0);
      // String category2Variation = getCategory(categoriesVariation , 1);
      // String category3Variation = getCategory(categoriesVariation , 2);
      //
      // // Primary image
      // String primaryImageVariation = crawlPrimaryImage(docVariation);
      //
      // // Secondary images
      // String secondaryImagesVariation = crawlSecondaryImages(docVariation);
      //
      // // Description
      // String descriptionVariation = crawlDescription(doc);
      //
      // // Creating the product
      // Product productVariation = new Product();
      // productVariation.setUrl(urlVariation);
      // productVariation.setInternalId(internalIDVariation);
      // productVariation.setInternalPid(internalPid);
      // productVariation.setName(nameVariation);
      // productVariation.setPrice(priceVariation);
      // productVariation.setAvailable(availableVariation);
      // productVariation.setCategory1(category1Variation);
      // productVariation.setCategory2(category2Variation);
      // productVariation.setCategory3(category3Variation);
      // productVariation.setPrimaryImage(primaryImageVariation);
      // productVariation.setSecondaryImages(secondaryImagesVariation);
      // productVariation.setDescription(descriptionVariation);
      // productVariation.setStock(stock);
      // productVariation.setMarketplace(marketplace);
      //
      // products.add(productVariation);
      // }

    } else {
      Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
    }

    return products;
  }



  /*******************************
   * Product page identification *
   *******************************/

  private boolean isProductPage(Document document) {
      return document.select(".detailProduct").first() != null;
  }

  /********************
   * Multiple Product *
   ********************/

  // private ArrayList<String> crawlUrlForMutipleVariations(Document doc, String
  // internalIDFirstProduct){
  // ArrayList<String> productsUrls = new ArrayList<String>();
  // Elements variations = doc.select(".variation li input");
  //
  // for(Element e : variations){
  // String idVariation = e.attr("value");
  // String url = e.attr("data-urlproduct");
  //
  // if(!idVariation.equals(internalIDFirstProduct)){
  // productsUrls.add(url);
  // }
  // }
  //
  //
  // return productsUrls;
  // }

  // private Document fetchPageVariation(String url){
  // Document doc = new Document("");
  //
  // if(url != null){
  // doc = DataFetcher.fetchDocument(DataFetcher.GET_REQUEST, session, url, null, null);
  // }
  //
  // return doc;
  // }

  /*******************
   * General methods *
   *******************/

  private String crawlInternalId(Document document) {
    String internalId = null;
    Element internalIdElement = document.select("input[name=data[Produto][rating][id_produto]]").first();

    if (internalIdElement != null) {
        internalId = internalIdElement.attr("value").trim();
    }

    return internalId;
  }

  private String crawlInternalPid(Document document) {
    String internalPid = null;

    return internalPid;
  }

  private String crawlName(Document document) {
    String name = null;
    Element nameElement = document.select(".detailProduct h1 span").first();

    if (nameElement != null) {
        name = nameElement.text().trim();
    }

    return name;
  }

  private Float crawlMainPagePrice(Document document) {
    Float price = null;
    Element specialPrice = document.select("p.prices ins").first();
    if (specialPrice != null) {
        price = Float.parseFloat(specialPrice.text().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", "."));
    }

    return price;
  }

  /**
   * The card payment options are the same across all card brands.
   * 
   * @param document
   * @return
   */
  private Prices crawlPrices(Document document) {
    Prices prices = new Prices();

    // bank slip
    Float bankSlipPrice = crawlBankSlipPrice(document);

    if (bankSlipPrice != null) {
      prices.setBankTicketPrice(bankSlipPrice);
    }

    // installments
    Map<Integer, Float> installments = new TreeMap<Integer, Float>();
    Elements installmentElements = document.select(".productRight .plots tbody tr");

    for (Element installmentElement : installmentElements) {
      Elements tdElements = installmentElement.select("td");
      if (tdElements.size() >= 2) {
        String installmentNumberText = tdElements.get(0).text();
        String installmentPriceText = tdElements.get(1).text();
        if (!installmentNumberText.isEmpty() && !installmentPriceText.isEmpty()) {
          List<String> parsedNumbers = MathUtils.parseNumbers(installmentNumberText);
          if (parsedNumbers.size() > 0) {
            installments.put(Integer.parseInt(parsedNumbers.get(0)), MathUtils.parseFloatWithComma(installmentPriceText));
          }
        }
      }
    }

    if (installments.isEmpty() && bankSlipPrice != null) {
      installments.put(1, bankSlipPrice);
    }

    if (installments.size() > 0) {
      prices.insertCardInstallment(Card.VISA.toString(), installments);
      prices.insertCardInstallment(Card.MASTERCARD.toString(), installments);
      prices.insertCardInstallment(Card.AMEX.toString(), installments);
      prices.insertCardInstallment(Card.ELO.toString(), installments);
      prices.insertCardInstallment(Card.VISA.toString(), installments);
      prices.insertCardInstallment(Card.HIPERCARD.toString(), installments);
      prices.insertCardInstallment(Card.DINERS.toString(), installments);
    }

    return prices;
  }

  private Float crawlBankSlipPrice(Document document) {
    Float bankSlipPrice = null;
    Element bankSlipPriceElement = document.select(".productRight small strong").first();
    if (bankSlipPriceElement != null) {
      bankSlipPrice = MathUtils.parseFloatWithComma(bankSlipPriceElement.text());
    }
    return bankSlipPrice;
  }

  private boolean crawlAvailability(Document document) {
    Element notifyMeElement = document.select(".productUnavailable").first();

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
    Element primaryImageElement = document.select("figure.imageWrapper a").first();

    if (primaryImageElement != null) {
      primaryImage = CommonMethods.sanitizeUrl(HOME_PAGE + primaryImageElement.attr("href").trim());
    }

    return primaryImage;
  }

  private String crawlSecondaryImages(Document document) {
    String secondaryImages = null;
    JSONArray secondaryImagesArray = new JSONArray();

    Elements imagesElement = document.select("#carousel li a img");

    for (int i = 1; i < imagesElement.size(); i++) { // start with index 1 because the first image is the primary image
      secondaryImagesArray.put(CommonMethods.sanitizeUrl(HOME_PAGE + imagesElement.get(i).attr("src").trim().replaceAll("false", "true"))); // montando
                                                                                                                                            // url
                                                                                                                                            // para
                                                                                                                                            // pegar a
                                                                                                                                            // maior
                                                                                                                                            // imagem
    }

    if (secondaryImagesArray.length() > 0) {
      secondaryImages = secondaryImagesArray.toString();
    }

    return secondaryImages;
  }

  private ArrayList<String> crawlCategories(Document document) {
    ArrayList<String> categories = new ArrayList<String>();
    Elements elementCategories = document.select(".breadcrumbs.breadcrumbsPages ul li a");

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
    Element descriptionElement = document.select("#descricao .boxDescricao").first();
    Element specElement = document.select("#especificacao .boxDescricao").first();
    Element grantiaElement = document.select("#garantia .boxDescricao").first();

    if (descriptionElement != null)
      description = description + descriptionElement.html();
    if (specElement != null)
      description = description + specElement.html();
    if (grantiaElement != null)
      description = description + grantiaElement.html();

    return description;
  }

}
