package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;
import br.com.lett.crawlernode.core.fetcher.DynamicDataFetcher;
import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import models.RatingsReviews;
import models.prices.Prices;

public class BrasilLojacolgateCrawler extends Crawler {

  private static final String HOST = "lojacolgate.com.br";

  private static final String LOGIN_URL = "https://lojacolgate.com.br/cpb2bglobalstorefront/pt/login";
  private static final String CNPJ = "45.543.915/0001-81";
  private static final String PASSWORD = "12345678";

  public BrasilLojacolgateCrawler(Session session) {
    super(session);
    super.config.setFetcher(FetchMode.FETCHER);
    super.config.setMustSendRatingToKinesis(true);
  }

  @Override
  protected Object fetch() {
    try {
      this.webdriver = DynamicDataFetcher.fetchPageWebdriver(LOGIN_URL, session);
      this.webdriver.waitLoad(10000);

      if (this.webdriver.driver instanceof JavascriptExecutor) {
        JavascriptExecutor jsExecutor = (JavascriptExecutor) this.webdriver.driver;

        // Javascript code to delete loader that goes over the page and
        // Show the main contents of the page by removing the hidden class from it
        jsExecutor.executeScript("return document.getElementsByClassName('loader-contain')[0].remove();");
        jsExecutor.executeScript("document.querySelector('.main__inner-wrapper').classList.remove('hidden')");
        this.webdriver.waitLoad(2000);
      }

      WebElement email = this.webdriver.driver.findElement(By.cssSelector("#j_username"));
      email.sendKeys(CNPJ);
      this.webdriver.waitLoad(2000);

      WebElement pass = this.webdriver.driver.findElement(By.cssSelector("#j_password"));
      pass.sendKeys(PASSWORD);
      this.webdriver.waitLoad(2000);

      WebElement login = this.webdriver.driver.findElement(By.cssSelector("#loginForm .btn-primary"));
      this.webdriver.clickOnElementViaJavascript(login);
      this.webdriver.waitLoad(2000);

      this.webdriver.loadUrl(session.getOriginalURL());
      this.webdriver.waitLoad(10000);

      return Jsoup.parse(this.webdriver.getCurrentPageSource());
    } catch (Exception e) {
      Logging.printLogDebug(logger, session, CommonMethods.getStackTrace(e));
      return super.fetch();
    }
  }

  @Override
  public List<Product> extractInformation(Document doc) throws Exception {
    super.extractInformation(doc);
    List<Product> products = new ArrayList<>();

    if (isProductPage(doc)) {
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

      Elements variations = doc.select(".js-variant-select > option");

      String name = CrawlerUtils.scrapStringSimpleInfo(doc, ".product-details .name", true).replace("|", "");
      CategoryCollection categories = scrapCategories(doc);
      String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, ".image-gallery__image .lazyOwl", Arrays.asList("data-zoom-image", "src"), "https", HOST);
      String secondaryImages = CrawlerUtils.scrapSimpleSecondaryImages(doc, ".image-gallery__image .lazyOwl", Arrays.asList("data-zoom-image", "src"), "https", HOST, primaryImage);
      String description = CrawlerUtils.scrapElementsDescription(doc, Arrays.asList(".tabhead", ".tabbody .tab-details"));
      RatingsReviews ratingsReviews = new RatingsReviews();

      if (variations.isEmpty()) {

        String internalId = CrawlerUtils.scrapStringSimpleInfo(doc, ".js-variant-sku", true);
        String internalPid = null;
        Float price = CrawlerUtils.scrapFloatPriceFromHtml(doc, ".js-variant-price", null, false, ',', session);
        Prices prices = scrapPrices(doc, price);
        Integer stock = CrawlerUtils.scrapIntegerFromHtmlAttr(doc, "[id*=productMaxQty-]", "data-max", 0);
        boolean available = doc.selectFirst("#outofstock.hidden") != null;
        List<String> eans = Arrays.asList(internalId);

        // Creating the product
        Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setInternalPid(internalPid)
            .setName(name)
            .setPrice(price)
            .setPrices(prices)
            .setAvailable(available)
            .setCategory1(categories.getCategory(0))
            .setCategory2(categories.getCategory(1))
            .setCategory3(categories.getCategory(2))
            .setPrimaryImage(primaryImage)
            .setSecondaryImages(secondaryImages)
            .setDescription(description)
            .setStock(stock)
            .setEans(eans)
            .setRatingReviews(ratingsReviews)
            .build();

        products.add(product);

      } else {
        for (Element sku : variations) {

          String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(sku, null, "value");
          String internalPid = null;
          String variationName = sku.hasText() ? name + " - " + sku.text().split("-")[0].trim() : name;
          Float price = CrawlerUtils.scrapFloatPriceFromHtml(sku, null, "data-formatted", false, ',', session);
          Prices prices = scrapVariationPrices(sku, price);
          Integer stock = CrawlerUtils.scrapIntegerFromHtmlAttr(sku, null, "data-maxqty", 0);
          boolean available = sku.hasAttr("data-stock") && sku.attr("data-stock").equalsIgnoreCase("inStock");
          List<String> eans = Arrays.asList(internalId);

          // Creating the product
          Product product = ProductBuilder.create()
              .setUrl(session.getOriginalURL())
              .setInternalId(internalId)
              .setInternalPid(internalPid)
              .setName(variationName)
              .setPrice(price)
              .setPrices(prices)
              .setAvailable(available)
              .setCategory1(categories.getCategory(0))
              .setCategory2(categories.getCategory(1))
              .setCategory3(categories.getCategory(2))
              .setPrimaryImage(primaryImage)
              .setSecondaryImages(secondaryImages)
              .setDescription(description)
              .setStock(stock)
              .setEans(eans)
              .setRatingReviews(ratingsReviews)
              .build();

          products.add(product);
        }
      }
    } else {
      Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
    }

    return products;
  }

  private boolean isProductPage(Document doc) {
    return doc.selectFirst(".page-productDetails") != null;
  }

  private CategoryCollection scrapCategories(Document doc) {
    CategoryCollection categoryCollection = new CategoryCollection();
    Elements elementsCategories = doc.select(".breadcrumb > li:not(:first-child) > a");

    for (Element categoryElement : elementsCategories) {

      if (categoryElement.hasAttr("href") && !categoryElement.attr("href").contains("Categories") && !categoryElement.attr("href").contains("AllProducts")) {
        categoryCollection.add(categoryElement.text().trim());
      }
    }

    return categoryCollection;
  }

  private Prices scrapPrices(Document doc, Float price) {
    Prices prices = new Prices();

    if (price != null) {
      prices.setBankTicketPrice(price);
      prices.setPriceFrom(CrawlerUtils.scrapDoublePriceFromHtml(doc, ".js-variant-discount", null, false, ',', session));
    }

    return prices;
  }

  private Prices scrapVariationPrices(Element e, Float price) {
    Prices prices = new Prices();

    if (price != null) {
      prices.setBankTicketPrice(price);
      prices.setPriceFrom(CrawlerUtils.scrapDoublePriceFromHtml(e, null, "data-discount", false, ',', session));
    }

    return prices;
  }
}
