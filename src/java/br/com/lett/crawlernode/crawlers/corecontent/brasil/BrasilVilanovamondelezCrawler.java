package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import br.com.lett.crawlernode.core.fetcher.DynamicDataFetcher;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.test.Test;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import br.com.lett.crawlernode.util.Logging;
import models.prices.Prices;

/**
 * Date: 09/07/2019
 * 
 * @author Gabriel Dornelas
 *
 */
public class BrasilVilanovamondelezCrawler extends Crawler {

  public static final String HOME_PAGE = "https://www.vilanova.com.br/";
  private static final String IMAGES_HOST = "i2-vilanova.a8e.net.br";

  private static final String CNPJ = "33.033.028/0040-90";
  private static final String PASSWORD = "Mudar123";

  public BrasilVilanovamondelezCrawler(Session session) {
    super(session);
  }


  @Override
  protected Object fetch() {
    this.webdriver = DynamicDataFetcher.fetchPageWebdriver(session.getOriginalURL(), session);
    this.webdriver.waitLoad(10000);

    String scriptLoginModal = "document.querySelector('#open-login').click()";
    this.webdriver.executeJavascript(scriptLoginModal);
    this.webdriver.waitLoad(2000);

    String scriptLoginModal2 = "document.querySelector('.modal-opcoes a:first-child').click()";
    this.webdriver.executeJavascript(scriptLoginModal2);
    this.webdriver.waitLoad(2000);

    CommonMethods.saveDataToAFile(this.webdriver.getCurrentPageSource(), Test.pathWrite + "VILANOVA.html");

    WebElement cnpj = this.webdriver.driver.findElement(By.cssSelector("#usuarioCnpj"));
    cnpj.sendKeys(CNPJ);
    this.webdriver.waitLoad(2000);

    WebElement pass = this.webdriver.driver.findElement(By.cssSelector("#usuarioSenha"));
    pass.sendKeys(PASSWORD);
    this.webdriver.waitLoad(2000);

    WebElement login = this.webdriver.driver.findElement(By.cssSelector("#realizar-login"));
    this.webdriver.clickOnElementViaJavascript(login);
    this.webdriver.waitLoad(10000);

    return Jsoup.parse(this.webdriver.getCurrentPageSource());
  }

  @Override
  public List<Product> extractInformation(Document doc) throws Exception {
    super.extractInformation(doc);
    List<Product> products = new ArrayList<>();

    CommonMethods.saveDataToAFile(doc, Test.pathWrite + "VILANOVA.html");

    if (isProductPage(doc)) {
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

      JSONArray productJsonArray = CrawlerUtils.selectJsonArrayFromHtml(doc, "script", "var dataLayer = ", ";", false, true);
      JSONObject productJson = extractProductData(productJsonArray);

      String internalPid = crawlInternalPid(productJson);
      List<String> eans = Arrays.asList(CrawlerUtils.scrapStringSimpleInfo(doc, ".product-ean .value", true));
      CategoryCollection categories = CrawlerUtils.crawlCategories(doc, "#Breadcrumbs li a", true);
      String description = CrawlerUtils.scrapElementsDescription(doc, Arrays.asList("#info-abas-mobile"));
      String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, "#imagem-produto #elevateImg", Arrays.asList("data-zoom-image", "href", "src"),
          "https", IMAGES_HOST);
      String secondaryImages = CrawlerUtils.scrapSimpleSecondaryImages(doc, "#imagem-produto #elevateImg", Arrays.asList("data-zoom-image", "href",
          "src"),
          "https", IMAGES_HOST, primaryImage);

      JSONArray productsArray =
          productJson.has("productSKUList") && !productJson.isNull("productSKUList") ? productJson.getJSONArray("productSKUList") : new JSONArray();

      for (Object obj : productsArray) {
        JSONObject skuJson = (JSONObject) obj;

        String internalId = crawlInternalId(skuJson);
        String name = crawlName(skuJson);
        Float price = JSONUtils.getFloatValueFromJSON(skuJson, "price", true);

        Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setInternalPid(internalPid)
            .setName(name)
            .setPrice(price)
            .setPrices(new Prices())
            .setAvailable(false)
            .setCategory1(categories.getCategory(0))
            .setCategory2(categories.getCategory(1))
            .setCategory3(categories.getCategory(2))
            .setPrimaryImage(primaryImage)
            .setSecondaryImages(secondaryImages)
            .setDescription(description)
            .setEans(eans)
            .build();

        products.add(product);
      }

    } else {
      Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
    }

    return products;

  }

  private JSONObject extractProductData(JSONArray productJsonArray) {
    JSONObject firstObjectFromArray = productJsonArray.length() > 0 ? productJsonArray.getJSONObject(0) : new JSONObject();
    return firstObjectFromArray.has("productData") ? firstObjectFromArray.getJSONObject("productData") : firstObjectFromArray;
  }

  private boolean isProductPage(Document doc) {
    return !doc.select(".container #detalhes-container").isEmpty();
  }

  private String crawlInternalId(JSONObject skuJson) {
    String internalId = null;

    if (skuJson.has("sku") && !skuJson.isNull("sku")) {
      internalId = skuJson.get("sku").toString();
    }

    return internalId;
  }

  private String crawlInternalPid(JSONObject productJson) {
    String internalPid = null;

    if (productJson.has("productID") && !productJson.isNull("productID")) {
      internalPid = productJson.get("productID").toString();
    }

    return internalPid;
  }

  private String crawlName(JSONObject skuJson) {
    String name = null;

    if (skuJson.has("name") && skuJson.get("name") instanceof String) {
      name = skuJson.getString("name");
    }

    return name;
  }
}
