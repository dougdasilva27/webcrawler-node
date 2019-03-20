package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.aws.s3.S3Service;
import br.com.lett.crawlernode.core.fetcher.DataFetcher;
import br.com.lett.crawlernode.core.fetcher.DynamicDataFetcher;
import br.com.lett.crawlernode.core.fetcher.Fetcher;
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

public class BrasilBifarmaCrawler extends Crawler {

  private static final String HOME_PAGE = "https://www.bifarma.com.br/";

  public BrasilBifarmaCrawler(Session session) {
    super(session);
    super.config.setFetcher(Fetcher.WEBDRIVER);
  }

  @Override
  public boolean shouldVisit() {
    String href = this.session.getOriginalURL().toLowerCase();
    return !FILTERS.matcher(href).matches() && href.startsWith(HOME_PAGE);
  }


  @Override
  protected Object fetch() {
    Document doc = new Document("");
    this.webdriver = DynamicDataFetcher.fetchPageWebdriver(session.getOriginalURL(), session);

    if (this.webdriver != null) {
      doc = Jsoup.parse(this.webdriver.getCurrentPageSource());

      Element script = doc.select("head script").last();
      Element robots = doc.select("meta[name=robots]").first();

      if (script != null && robots != null) {
        String eval = script.html().trim();

        if (!eval.isEmpty()) {
          Logging.printLogDebug(logger, session, "Execution of incapsula js script...");
          this.webdriver.executeJavascript(eval);
        }
      }

      String requestHash = DataFetcher.generateRequestHash(session);
      this.webdriver.waitLoad(12000);

      doc = Jsoup.parse(this.webdriver.getCurrentPageSource());

      // saving request content result on Amazon
      S3Service.uploadCrawlerSessionContentToAmazon(session, requestHash, doc.toString());
    }

    return doc;
  }

  @Override
  public List<Product> extractInformation(Document doc) throws Exception {
    super.extractInformation(doc);
    List<Product> products = new ArrayList<>();

    JSONObject productInfo = crawlProductInfo(doc);

    if (productInfo.length() < 1) {
      this.webdriver.waitLoad(10000);
      doc = Jsoup.parse(this.webdriver.getCurrentPageSource());
      productInfo = crawlProductInfo(doc);
    }

    if (isProductPage(doc)) {
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

      String internalId = crawlInternalId(productInfo);
      String internalPid = crawlInternalPid(productInfo);
      String name = crawlName(productInfo);
      Float price = crawlPrice(doc);
      Prices prices = crawlPrices(price, productInfo, doc);
      boolean available = crawlAvailability(productInfo);
      CategoryCollection categories = crawlCategories(doc);
      String primaryImage = crawlPrimaryImage(doc);
      String secondaryImages = crawlSecondaryImages(doc, primaryImage);
      String description = crawlDescription(doc);
      Integer stock = null;
      Marketplace marketplace = crawlMarketplace();

      String ean = crawlEan(doc);
      List<String> eans = new ArrayList<>();
      eans.add(ean);

      // Creating the product
      Product product = ProductBuilder.create().setUrl(session.getOriginalURL()).setInternalId(internalId).setInternalPid(internalPid).setName(name)
          .setPrice(price).setPrices(prices).setAvailable(available).setCategory1(categories.getCategory(0)).setCategory2(categories.getCategory(1))
          .setCategory3(categories.getCategory(2)).setPrimaryImage(primaryImage).setSecondaryImages(secondaryImages).setDescription(description)
          .setStock(stock).setMarketplace(marketplace).setEans(eans).build();

      products.add(product);

    } else {
      Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
    }

    return products;

  }

  private boolean isProductPage(Document doc) {
    return !doc.select(".product_body").isEmpty();
  }

  private String crawlInternalId(JSONObject info) {
    String internalId = null;

    if (info.has("skus")) {
      JSONArray skus = info.getJSONArray("skus");

      if (skus.length() > 0) {
        JSONObject sku = skus.getJSONObject(0);

        if (sku.has("sku")) {
          internalId = sku.getString("sku");
        }
      }
    }

    return internalId;
  }

  private String crawlInternalPid(JSONObject info) {
    String internalPid = null;

    if (info.has("id")) {
      internalPid = info.getString("id");
    }

    return internalPid;
  }

  private String crawlName(JSONObject info) {
    String name = null;

    if (info.has("name")) {
      name = info.getString("name");
    }

    return name;
  }

  private Float crawlPrice(Document document) {
    Float price = null;
    Element salePriceElement = document.select(".product_current_price strong").first();

    if (salePriceElement != null) {
      String priceText = salePriceElement.ownText().trim();

      if (!priceText.isEmpty()) {
        price = MathUtils.parseFloatWithComma(priceText);
      }
    }

    return price;
  }

  private boolean crawlAvailability(JSONObject info) {
    boolean available = false;

    if (info.has("status")) {
      String status = info.getString("status");

      if (status.equals("available")) {
        available = true;
      }
    }

    return available;
  }

  private Marketplace crawlMarketplace() {
    return new Marketplace();
  }

  private String crawlPrimaryImage(Document document) {
    String primaryImage = null;
    Element primaryImageElement = document.select(".slider-product .slide_image img").first();

    if (primaryImageElement != null) {
      primaryImage = primaryImageElement.attr("src").trim();

      if (!primaryImage.contains("bifarma")) {
        primaryImage = HOME_PAGE + primaryImage;
      }

      if (primaryImage.contains("SEM_IMAGEM")) {
        primaryImage = null;
      }
    }

    return primaryImage;
  }

  private String crawlSecondaryImages(Document document, String primaryImage) {
    String secondaryImages = null;
    JSONArray secondaryImagesArray = new JSONArray();

    List<String> images = new ArrayList<>();
    images.add(primaryImage);

    Elements imagesElement = document.select(".slider-thumbs .thumb > img");

    for (int i = 1; i < imagesElement.size(); i++) { // first index is the primary image
      String image = imagesElement.get(i).attr("src").trim().replace("_mini", "");

      if (!images.contains(image)) {
        images.add(image);
        secondaryImagesArray.put(image);
      }
    }

    if (secondaryImagesArray.length() > 0) {
      secondaryImages = secondaryImagesArray.toString();
    }

    return secondaryImages;
  }

  // private boolean isPrimaryImage(String image, String primaryImage) {
  // String x = (image.replace("https://cdn-bifarma3.stoom.com.br/fotos/",
  // "")).split("\\.")[0].replaceAll("[^0-9]", "");
  // String y = (primaryImage.replace("https://cdn-bifarma3.stoom.com.br/fotos/",
  // "")).split("\\.")[0].replaceAll("[^0-9]", "");
  //
  // return x.equals(y);
  // }

  private CategoryCollection crawlCategories(Document doc) {
    CategoryCollection categories = new CategoryCollection();
    Elements catElements = doc.select("#breadcrumbList li[itemprop=\"itemListElement\"] a span");

    for (int i = 1; i < catElements.size(); i++) { // first item is home
      categories.add(catElements.get(i).ownText());
    }

    return categories;
  }

  private String crawlDescription(Document document) {
    StringBuilder description = new StringBuilder();
    Element descriptionElement = document.selectFirst(".accordion .accordion-section:not(.dp-banner)");

    if (descriptionElement != null) {
      description.append(descriptionElement.html());
    }

    Element advert = document.select("#tipoMensagemProduto").first();

    if (advert != null) {
      description.append(advert.html());
    }

    return description.toString();
  }

  private Prices crawlPrices(Float price, JSONObject info, Document doc) {
    Prices prices = new Prices();

    if (price != null) {

      Map<Integer, Float> installmentPriceMap = new HashMap<>();

      prices.setBankTicketPrice(price);
      installmentPriceMap.put(1, price);

      Element priceFrom = doc.select(".product_previous_price").first();
      if (priceFrom != null) {
        prices.setPriceFrom(MathUtils.parseDoubleWithComma(priceFrom.text()));
      }

      if (info.has("installment")) {
        JSONObject installment = info.getJSONObject("installment");

        if (installment.has("price") && installment.has("count")) {
          Double priceInstallment = installment.getDouble("price");
          Integer installmentCount = installment.getInt("count");

          if (installmentCount > 0) {
            installmentPriceMap.put(installmentCount, priceInstallment.floatValue());
          }
        }
      }

      prices.insertCardInstallment(Card.AMEX.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.DINERS.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.ELO.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.HIPERCARD.toString(), installmentPriceMap);
    }

    return prices;
  }

  /**
   * Return a json like this: "{\"product\": {\n" + "\t \"id\": \"7748\",\n" + "\t \"name\": \"Mucilon
   * arroz/aveia 400gr neste\",\n" + "\t \n" + "\t \"url\":
   * \"/produto/mucilon-arroz-aveia-400gr-neste-7748\",\n" + "\t \"images\": {\n" + "\t \"235x235\":
   * \"/fotos/PRODUTO_SEM_IMAGEM_mini.png\"\n" + "\t },\n" + "\t \"status\": \"available\",\n" + "\t\t
   * \n" + "\t \"price\": 12.50,\n" + "\t \"categories\": [{\"name\":\"Mamãe e
   * Bebê\",\"id\":\"8\"},{\"name\":\"Alimentos\",\"id\":\"89\",\"parents\":[\"8\"]},],\n" + "\t
   * \"installment\": {\n" + "\t \"count\": 0,\n" + "\t \"price\": 0.00\n" + "\t },\n" + "\t \n" + "\t
   * \n" + "\t \n" + "\t \t\"skus\": [ {\n" + "\t \t\t\t\"sku\": \"280263\"\n" + "\t \t}],\n" + "\t
   * \n" + "\t \"details\": {},\n" + "\t \t\t\"published\": \"2017-01-24\"\n" + "\t }}";
   *
   * @param doc
   * @return
   */
  private JSONObject crawlProductInfo(Document doc) {
    JSONObject info = new JSONObject();

    JSONObject json = CrawlerUtils.selectJsonFromHtml(doc, "script", "chaordicProduct = ", "};", false, false);
    if (json.has("product")) {
      info = json.getJSONObject("product");
    }

    return info;
  }

  private String crawlEan(Document doc) {
    Element e = doc.selectFirst("[itemprop=itemOffered] [itemprop=gtin13]");
    String ean = null;

    if (e != null) {
      ean = e.attr("content").trim();
    }

    return ean;
  }
}
