package br.com.lett.crawlernode.crawlers.extractionutils.core;

import br.com.lett.crawlernode.core.fetcher.DynamicDataFetcher;
import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.methods.ApacheDataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.JsoupDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.LettProxy;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.fetcher.models.Response;
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
import com.google.common.collect.Sets;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.Offer;
import models.Offers;
import models.prices.Prices;
import models.pricing.*;
import org.apache.http.HttpHeaders;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.util.*;

import static java.util.Collections.singletonList;

public abstract class BrasilVilanova extends Crawler {

   public static final String HOME_PAGE = "https://www.vilanova.com.br/";
   private static final String IMAGES_HOST = "i2-vilanova.a8e.net.br";

   private static final String LOGIN_URL = "https://www.vilanova.com.br/Cliente/Logar";
   private final String CNPJ = getCnpj();
   private final String PASSWORD = getPassword();

   protected Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),
      Card.AURA.toString(), Card.DINERS.toString(), Card.HIPER.toString(), Card.AMEX.toString());

   public BrasilVilanova(Session session) {
      super(session);
      super.config.setFetcher(FetchMode.JSOUP);
   }

   public abstract String getCnpj();

   public abstract String getPassword();

   public abstract String getSellerFullname();

   private String cookiePHPSESSID = null;

   @Override
   public void handleCookiesBeforeFetch() {
//      Map<String, String> headers = new HashMap<>();
//      headers.put(HttpHeaders.USER_AGENT, "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/88.0.4324.182 Safari/537.36");
//      headers.put(HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded; charset=UTF-8");
//      headers.put("X-Requested-With", "XMLHttpRequest");
//
//      StringBuilder payload = new StringBuilder();
//      payload.append("usuario_cnpj=").append(CNPJ);
//      payload.append("&usuario_senha=").append(PASSWORD);
//
//      Request requestHome = RequestBuilder.create()
//         .setUrl(LOGIN_URL)
//         .setHeaders(headers)
//         .setPayload(payload.toString())
//         .build();
//      Response response = this.dataFetcher.post(session, requestHome);
//
//      String cookieStr = response.getHeaders().getOrDefault("set-cookie", "");
//      if (!cookieStr.equals("")) {
//         cookiePHPSESSID = cookieStr.substring(10, cookieStr.indexOf(';'));
//      }


   }



   @Override
   protected Object fetch() {

      try {
         Logging.printLogDebug(logger, session, "Fetching page with webdriver...");

         webdriver = DynamicDataFetcher.fetchPageWebdriver(session.getOriginalURL(), ProxyCollection.BUY_HAPROXY, session);
         Document doc = Jsoup.parse(webdriver.getCurrentPageSource());

         if(doc.selectFirst("button.btn-politicas-cookies") != null){
            webdriver.findAndClick("button.btn-politicas-cookies", 2000);
            webdriver.findAndClick("a.cc-ALLOW", 2000);
         }

         webdriver.findAndClick("button.open-login", 2000);
         webdriver.waitLoad(2000);
         webdriver.findAndClick("button[id=fazer-login]", 2000);

         Logging.printLogDebug(logger, session, "Sending credentials...");

         webdriver.waitLoad(2000);
         waitForElement(webdriver.driver, "#usuarioCnpj");
         WebElement username = webdriver.driver.findElement(By.cssSelector("#usuarioCnpj"));
         username.sendKeys(getCnpj());

         webdriver.waitLoad(2000);
         waitForElement(webdriver.driver, "#usuarioSenha");
         WebElement pass = webdriver.driver.findElement(By.cssSelector("#usuarioSenha"));
         pass.sendKeys(getPassword());

         Logging.printLogDebug(logger, session, "awaiting login button");
         webdriver.waitLoad(4000);

         waitForElement(webdriver.driver, "#realizar-login");
         WebElement login = webdriver.driver.findElement(By.cssSelector("#realizar-login"));
         webdriver.clickOnElementViaJavascript(login);

         Logging.printLogDebug(logger, session, "awaiting product page");
         webdriver.waitLoad(25000);

         doc = Jsoup.parse(webdriver.getCurrentPageSource());

         if (!isProductPage(doc)) {
            doc = (Document) super.fetch();
         }

         return doc;
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
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         JSONObject json = CrawlerUtils.selectJsonFromHtml(doc, "script", "window.dataLayer = window.dataLayer || []; window.dataLayer.push(", ");", false, true);

         if (json != null && !json.isEmpty()) {
            JSONObject jsonProduct = json.optJSONObject("productData");

            String internalPid = jsonProduct.optString("productSKU");
            List<String> eans = singletonList(CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, ".variacao-container", "data-produtoean"));
            CategoryCollection categories = scrapCategories(jsonProduct);
            String description = CrawlerUtils.scrapElementsDescription(doc, singletonList("#info-abas-mobile"));
            String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, "#imagem-produto #elevateImg", Arrays.asList("data-zoom-image", "href", "src"), "https", IMAGES_HOST);
            //cannot find any secondary image

            JSONArray skuArray = jsonProduct.optJSONArray("productSKUList");

            for (Object sku : skuArray) {
               if (sku instanceof JSONObject) {
                  JSONObject skuJson = (JSONObject) sku;
                  String name = skuJson.optString("name");
                  String internalId = skuJson.optString("id");

                  int stock = skuJson.optInt("stock");

                  boolean isAvailable = stock != 0 || !skuJson.optString("available").equals("no");
                  Offers offers = isAvailable ? scrapOffers(skuJson) : new Offers();

                  Product product = ProductBuilder.create()
                     .setUrl(session.getOriginalURL())
                     .setInternalId(internalId)
                     .setInternalPid(internalPid)
                     .setName(name)
                     .setOffers(offers)
                     .setCategories(categories)
                     .setPrimaryImage(primaryImage)
                     .setDescription(description)
                     .setEans(eans)
                     .setStock(stock)
                     .build();

                  products.add(product);
               }
            }
         }
      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }

   private boolean isProductPage(Document doc) {
      return !doc.select("div.product-row").isEmpty();
   }

   private CategoryCollection scrapCategories(JSONObject json) {
      CategoryCollection categories = new CategoryCollection();
      String categoriesStr = json.optString("categoryTree");

      if (categoriesStr != null) {
         String[] categorieList = categoriesStr.split("/");
         if (categorieList.length != 0) {
            categories.addAll(Arrays.asList(categorieList));
         }
      }

      return categories;
   }

   private JSONObject getSkusList(Document doc) {
      String ean =
         CrawlerUtils.scrapStringSimpleInfoByAttribute(
            doc, ".variacao-container", "data-produtoean");
      if (ean == null) {
         JSONObject json = CrawlerUtils.selectJsonFromHtml(doc, "script", "var dataLayer = [", "];",
            false, true).optJSONObject("productData");
         JSONObject jsonObject = new JSONObject();

         jsonObject.put("productData", new JSONObject()
            .put("Nome", json.optString("productName"))
            .put("Id", json.optString("productID"))
            .put("PrecoPor", json.opt("productDiscountPrice"))
            .put("PrecoPorSemPromocao", json.opt("productOldPrice")));

         return jsonObject;
      }
      Map<String, String> headers = new HashMap<>();
      headers.put("Cookie", "PHPSESSID=" + cookiePHPSESSID);

      Response response =
         dataFetcher.get(session,
            RequestBuilder.create()
               .setUrl("https://www.vilanova.com.br/Produto/Variacoes/" + ean)
               .setHeaders(headers)
               .build());

      return JSONUtils.stringToJson(response.getBody());
   }

   private Offers scrapOffers(JSONObject product) throws MalformedPricingException, OfferException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(product);
      List<String> sales = new ArrayList<>(); // When this new offer model was implemented, no sales was found

      offers.add(Offer.OfferBuilder.create()
         .setUseSlugNameAsInternalSellerId(true)
         .setSellerFullName(getSellerFullname())
         .setMainPagePosition(1)
         .setIsBuybox(false)
         .setIsMainRetailer(true)
         .setPricing(pricing)
         .setSales(sales)
         .build());


      return offers;
   }

   private Pricing scrapPricing(JSONObject product) throws MalformedPricingException {
      Double priceFrom = JSONUtils.getDoubleValueFromJSON(product, "old_price", false);
      Double spotlightPrice = JSONUtils.getDoubleValueFromJSON(product, "price", false);

      if (priceFrom != null && priceFrom.equals(spotlightPrice)) {
         priceFrom = null;
      }

      BankSlip bankSlip = CrawlerUtils.setBankSlipOffers(spotlightPrice, 0d);
      CreditCards creditCards = scrapCreditcards(product, spotlightPrice);

      return Pricing.PricingBuilder.create()
         .setPriceFrom(priceFrom)
         .setSpotlightPrice(spotlightPrice)
         .setBankSlip(bankSlip)
         .setCreditCards(creditCards)
         .build();
   }

   private CreditCards scrapCreditcards(JSONObject product, Double spotlightPrice) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();
      Installments installments = new Installments();

      JSONObject installmentsJson = product.optJSONObject("installment");

      if (installmentsJson != null && !installmentsJson.isEmpty()) {
         installments.add(Installment.InstallmentBuilder.create()
            .setInstallmentNumber(installmentsJson.optInt("count"))
            .setInstallmentPrice(installmentsJson.optDouble("price"))
            .build());
      } else {
         installments.add(Installment.InstallmentBuilder.create()
            .setInstallmentNumber(1)
            .setInstallmentPrice(spotlightPrice)
            .build());
      }

      for (String card : cards) {
         creditCards.add(CreditCard.CreditCardBuilder.create()
            .setBrand(card)
            .setInstallments(installments)
            .setIsShopCard(false)
            .build());
      }

      return creditCards;
   }
}
