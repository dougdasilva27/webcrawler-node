package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import com.google.common.collect.Sets;
import br.com.lett.crawlernode.core.fetcher.DynamicDataFetcher;
import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.Offer.OfferBuilder;
import models.Offers;
import models.pricing.CreditCard.CreditCardBuilder;
import models.pricing.CreditCards;
import models.pricing.Installment.InstallmentBuilder;
import models.pricing.Installments;
import models.pricing.Pricing;
import models.pricing.Pricing.PricingBuilder;

public class BrasilDimedCrawler extends Crawler {

   private static final String HOST = "www.dimed.com.br";

   private static final String USER = "tstpfo";
   private static final String PASSWORD = "dimed987@Poa";

   public BrasilDimedCrawler(Session session) {
      super(session);
      super.config.setFetcher(FetchMode.JSOUP);
   }

   private Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),
         Card.ELO.toString(), Card.DINERS.toString(), Card.AMEX.toString());

   @Override
   protected Object fetch() {
      try {
         webdriver = DynamicDataFetcher.fetchPageWebdriver("https://www.dimed.com.br/", ProxyCollection.LUMINATI_SERVER_BR_HAPROXY, session);

         webdriver.waitLoad(5000);

         Logging.printLogDebug(logger, session, "Trying to input login ...");
         WebElement check = webdriver.driver.findElement(By.cssSelector("input[name=naoExibirNovamente]"));
         webdriver.clickOnElementViaJavascript(check);

         webdriver.waitLoad(2000);
         webdriver.waitForElement("#username", 20);
         WebElement email = webdriver.driver.findElement(By.cssSelector("#username"));
         email.sendKeys(USER);

         webdriver.waitLoad(2000);
         webdriver.waitForElement("input[name=j_password]", 20);
         WebElement pass = webdriver.driver.findElement(By.cssSelector("input[name=j_password]"));
         pass.sendKeys(PASSWORD);

         Logging.printLogDebug(logger, session, "awaiting login button");
         webdriver.waitLoad(2000);

         webdriver.waitForElement("input[value=Entrar]", 20);
         WebElement login = webdriver.driver.findElement(By.cssSelector("input[value=Entrar]"));
         webdriver.clickOnElementViaJavascript(login);

         Logging.printLogDebug(logger, session, "awaiting product page");
         webdriver.waitLoad(6000);

         webdriver.loadUrl(session.getOriginalURL());

         Document doc = Jsoup.parse(webdriver.getCurrentPageSource());

         if (doc.select("input[name=codigoProduto]").isEmpty()) {
            doc = (Document) super.fetch();
         }

         return doc;
      } catch (Exception e) {
         Logging.printLogDebug(logger, session, CommonMethods.getStackTrace(e));
         return super.fetch();
      }
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "input[name=codigoProduto]", "value");

      if (internalId != null && !internalId.isEmpty()) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String name = CrawlerUtils.scrapStringSimpleInfo(doc, ".detalhes .descricaoproduto", true);
         String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, ".imagem-principal imgl", Arrays.asList("src"), "https", HOST);

         if (primaryImage != null) {
            primaryImage = primaryImage.replace("235x", "500x");
         }

         List<String> secondaryImages = CrawlerUtils.scrapSecondaryImages(doc, ".image-gallery__image .lazyOwl", Arrays.asList("data-zoom-image", "src"), "https", HOST, primaryImage);
         String description = CrawlerUtils.scrapElementsDescription(doc, Arrays.asList(".fieldcontain:not(.descricaoproduto):not(.precoproduto)"));
         String internalPid = CrawlerUtils.scrapStringSimpleInfo(doc, ".fieldcontain.ean span", true);
         boolean available = doc.selectFirst(".btaviseme") == null;
         Offers offers = available ? scrapOffers(doc) : new Offers();
         List<String> eans = Arrays.asList(internalPid);

         Product product = ProductBuilder.create()
               .setUrl(session.getOriginalURL())
               .setInternalId(internalId)
               .setInternalPid(internalPid)
               .setName(name)
               .setPrimaryImage(primaryImage)
               .setSecondaryImages(secondaryImages)
               .setOffers(offers)
               .setDescription(description)
               .setEans(eans)
               .build();

         products.add(product);

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }

   private Offers scrapOffers(Document doc) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(doc);

      if (pricing != null) {
         offers.add(OfferBuilder.create()
               .setUseSlugNameAsInternalSellerId(true)
               .setSellerFullName("Dimed")
               .setSellersPagePosition(1)
               .setIsBuybox(false)
               .setIsMainRetailer(true)
               .setPricing(pricing)
               .build());
      }

      return offers;
   }

   private Pricing scrapPricing(Document doc) throws MalformedPricingException {
      Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".fieldcontain.precoproduto", null, false, ',', session);

      if (spotlightPrice != null) {
         Double priceFrom = null;
         CreditCards creditCards = scrapCreditCards(spotlightPrice);

         return PricingBuilder.create()
               .setSpotlightPrice(spotlightPrice)
               .setPriceFrom(priceFrom)
               .setCreditCards(creditCards)
               .build();
      }

      return null;
   }

   private CreditCards scrapCreditCards(Double spotlightPrice) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();

      Installments installments = new Installments();
      installments.add(InstallmentBuilder.create()
            .setInstallmentNumber(1)
            .setInstallmentPrice(spotlightPrice)
            .build());

      for (String brand : cards) {
         creditCards.add(CreditCardBuilder.create()
               .setBrand(brand)
               .setIsShopCard(false)
               .setInstallments(installments)
               .build());
      }

      return creditCards;
   }
}
