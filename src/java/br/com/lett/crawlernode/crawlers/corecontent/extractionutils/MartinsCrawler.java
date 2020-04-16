package br.com.lett.crawlernode.crawlers.corecontent.extractionutils;

import br.com.lett.crawlernode.core.fetcher.DynamicDataFetcher;
import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import br.com.lett.crawlernode.util.Logging;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import models.Marketplace;
import models.RatingsReviews;
import models.prices.Prices;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public abstract class MartinsCrawler extends Crawler {

  public MartinsCrawler(Session session) {
    super(session);
    super.config.setFetcher(FetchMode.FETCHER);
  }

  protected static final String HOME_PAGE = "https://www.martinsatacado.com.br";

  protected String password = getPassword();
  protected String login = getLogin();

  protected abstract String getPassword();

  protected abstract String getLogin();

  @Override
  public boolean shouldVisit() {
    String href = session.getOriginalURL().toLowerCase();
    return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
  }

  @Override
  protected Object fetch() {
    try {
      webdriver = DynamicDataFetcher
          .fetchPageWebdriver("https://www.martinsatacado.com.br/login/topo/request", session);
      webdriver.waitLoad(6000);

      waitForElement(webdriver.driver, "#go-login");
      webdriver
          .clickOnElementViaJavascript(webdriver.driver.findElement(By.cssSelector("#go-login")));

      waitForElement(webdriver.driver, "#js_username_login");
      WebElement email = webdriver.driver.findElement(By.cssSelector("#js_username_login"));
      email.sendKeys(login);

      waitForElement(webdriver.driver, "#jsSelectCNPJ");
      WebElement cnpj = webdriver.driver.findElement(By.cssSelector("#jsSelectCNPJ"));
      webdriver.clickOnElementViaJavascript(cnpj);

      waitForElement(webdriver.driver, "#j_password[required]");
      WebElement pass = webdriver.driver.findElement(By.cssSelector("#j_password[required]"));
      pass.sendKeys(password);

      waitForElement(webdriver.driver, ".c-login__button");
      WebElement login = webdriver.driver.findElement(By.cssSelector(".c-login__button"));
      webdriver.clickOnElementViaJavascript(login);

      webdriver.loadUrl(session.getOriginalURL());
      webdriver.waitLoad(6000);

      return Jsoup.parse(webdriver.getCurrentPageSource());
    } catch (Exception e) {
      Logging.printLogDebug(logger, session, CommonMethods.getStackTrace(e));
      return super.fetch();
    }
  }

  public static void waitForElement(WebDriver driver, String cssSelector) {
    WebDriverWait wait = new WebDriverWait(driver, 20);
    wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(cssSelector)));
  }

  @Override
  public List<Product> extractInformation(Document doc) throws Exception {
    super.extractInformation(doc);
    List<Product> products = new ArrayList<>();

    if (isProductPage(doc)) {
      Logging.printLogDebug(logger, session,
          "Product page identified: " + session.getOriginalURL());

      String internalId = CommonMethods.getLast(
          CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "input#id", "value").split("_"));
      String name = CrawlerUtils.scrapStringSimpleInfo(doc, ".qdDetails .title", true);
      Float price = CrawlerUtils
          .scrapFloatPriceFromHtml(doc, ".qdValue .value", null, true, ',', session);
      Prices prices = scrapPrices(price);
      CategoryCollection categories = CrawlerUtils
          .crawlCategories(doc, ".breadcrumb li:not(:first-child) > a", true);
      String primaryImage = CrawlerUtils
          .scrapSimplePrimaryImage(doc, ".imagePrincipal img", Collections.singletonList("src"),
              "https",
              "imgprd.martins.com.br");
      String secondaryImages = CrawlerUtils
          .scrapSimpleSecondaryImages(doc, ".galeryImages img", Collections.singletonList("src"),
              "https",
              "imgprd.martins.com.br", primaryImage);
      String description =
          CrawlerUtils.scrapSimpleDescription(doc,
              Arrays.asList(".qdDetails .cods", ".details", "#especfication", ".body #details"));
      List<String> eans = Collections
          .singletonList(CrawlerUtils.scrapStringSimpleInfo(doc, ".cods .col-2 p", true));
      RatingsReviews ratingsReviews = scrapRating(doc, internalId);

      // Creating the product
      Product product = ProductBuilder.create()
          .setUrl(session.getOriginalURL())
          .setInternalId(internalId)
          .setName(name)
          .setPrice(price)
          .setPrices(prices)
          .setAvailable(price != null)
          .setCategory1(categories.getCategory(0))
          .setCategory2(categories.getCategory(1))
          .setCategory3(categories.getCategory(2))
          .setPrimaryImage(primaryImage)
          .setRatingReviews(ratingsReviews)
          .setSecondaryImages(secondaryImages)
          .setDescription(description)
          .setMarketplace(new Marketplace())
          .setEans(eans)
          .build();

      products.add(product);

    } else {
      Logging.printLogDebug(logger, session, "Not a product page " + session.getOriginalURL());
    }

    return products;
  }

  private RatingsReviews scrapRating(Document doc, String internalId) {
    RatingsReviews ratingsReviews = new RatingsReviews();
    String ratingString = CrawlerUtils
        .scrapStringSimpleInfoByAttribute(doc, ".hidden-sm .rating .rating-stars", "data-rating");
    JSONObject jsonRating = JSONUtils.stringToJson(ratingString);

    int totalReviews = jsonRating.opt("rating") != null ? jsonRating.optInt("rating") : 0;
    ratingsReviews.setTotalRating(totalReviews);
    ratingsReviews.setTotalWrittenReviews(totalReviews);
    ratingsReviews.setDate(session.getDate());
    ratingsReviews.setInternalId(internalId);
    return ratingsReviews;
  }

  private boolean isProductPage(Document doc) {
    return !doc.select("input#id").isEmpty();
  }

  private Prices scrapPrices(Float price) {
    Prices prices = new Prices();

    if (price != null) {
      Map<Integer, Float> installment = new HashMap<>();
      installment.put(1, price);

      prices.setBankTicketPrice(price);
      prices.insertCardInstallment(Card.VISA.toString(), installment);
      prices.insertCardInstallment(Card.MASTERCARD.toString(), installment);
    }

    return prices;
  }
}
