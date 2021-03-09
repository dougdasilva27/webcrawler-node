package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.fetcher.DynamicDataFetcher;
import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import com.google.common.collect.Sets;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.Offer;
import models.Offers;
import models.pricing.*;
import org.json.JSONArray;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class BrasilBifarmaCrawler extends Crawler {

   private static final String HOME_PAGE = "https://www.bifarma.com.br/";

   public BrasilBifarmaCrawler(Session session) {
      super(session);
      super.config.setFetcher(FetchMode.WEBDRIVER);
   }

   @Override
   protected Object fetch() {
      Document doc = new Document("");
      this.webdriver = DynamicDataFetcher.fetchPageWebdriver(session.getOriginalURL(), ProxyCollection.BUY_HAPROXY, session);

      if (this.webdriver != null) {
         doc = Jsoup.parse(this.webdriver.getCurrentPageSource());
         this.webdriver.waitLoad(20000);
      }

      return doc;
   }

   @Override
   public boolean shouldVisit() {
      String href = this.session.getOriginalURL().toLowerCase();
      return !FILTERS.matcher(href).matches() && href.startsWith(HOME_PAGE);
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      List<Product> products = new ArrayList<>();


      if (doc.selectFirst(".main_body .product-primary") != null) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "#produto_id", "value");
         String internalPid = internalId;
         String name = CrawlerUtils.scrapStringSimpleInfo(doc, ".product_content h1", false);
         CategoryCollection categories = crawlCategories(doc);
         String primaryImage = crawlPrimaryImage(doc);
         List<String> secondaryImages = crawlSecondaryImages(doc, primaryImage);
         String description = crawlDescription(doc, internalId);
         boolean available = doc.selectFirst(".btn.click.product_btn") != null;
         Offers offers = available ? scrapOffer(doc) : new Offers();

         // Creating the product
         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setInternalPid(internalPid)
            .setName(name)
            .setCategories(categories)
            .setPrimaryImage(primaryImage)
            .setSecondaryImages(secondaryImages)
            .setDescription(description)
            .setOffers(offers)
            .build();

         products.add(product);

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
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

   private List<String> crawlSecondaryImages(Document document, String primaryImage) {
      List<String> secondaryImages = null;
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
         secondaryImages = Collections.singletonList(secondaryImagesArray.toString());
      }

      return secondaryImages;
   }

   private CategoryCollection crawlCategories(Document doc) {
      CategoryCollection categories = new CategoryCollection();
      Elements catElements = doc.select("#breadcrumbList li[itemprop=\"itemListElement\"] a span");

      for (int i = 1; i < catElements.size(); i++) { // first item is home
         categories.add(catElements.get(i).ownText());
      }

      return categories;
   }

   private String crawlDescription(Document document, String internalId) {
      StringBuilder description = new StringBuilder();
      Element descriptionElement = document.selectFirst(".accordion .accordion-section:not(.dp-banner)");

      if (descriptionElement != null) {
         description.append(descriptionElement.html());
      }

      Element advert = document.select("#tipoMensagemProduto").first();

      if (advert != null) {
         description.append(advert.html());
      }

      Element lett = document.select("#shipper-container").first();

      if (lett != null) {
         description.append(CrawlerUtils.scrapLettHtml(internalId, session, session.getMarket().getNumber()));
      }

      return description.toString();
   }


   private Offers scrapOffer(Document doc) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(doc);
      List<String> sales = scrapSales(doc);

      offers.add(Offer.OfferBuilder.create()
         .setUseSlugNameAsInternalSellerId(true)
         .setSellerFullName("Bifarma Brasil")
         .setMainPagePosition(1)
         .setIsBuybox(false)
         .setIsMainRetailer(true)
         .setPricing(pricing)
         .setSales(sales)
         .build());

      return offers;

   }

   private List<String> scrapSales(Document doc) {
      List<String> sales = new ArrayList<>();

      Element salesOneElement = doc.selectFirst(".product_economize_price");
      String firstSales = salesOneElement != null ? salesOneElement.text() : null;

      if (firstSales != null && !firstSales.isEmpty()) {
         sales.add(firstSales);
      }

      return sales;
   }

   private Pricing scrapPricing(Document doc) throws MalformedPricingException {
      Double priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".product_previous_price", null, false, ',', session);
      Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".product_current_price .preco-produto", null, false, ',', session);
      BankSlip bankSlip = CrawlerUtils.setBankSlipOffers(spotlightPrice, null);
      CreditCards creditCards = scrapCreditCards(doc, spotlightPrice);

      return Pricing.PricingBuilder.create()
         .setPriceFrom(priceFrom)
         .setSpotlightPrice(spotlightPrice)
         .setBankSlip(bankSlip)
         .setCreditCards(creditCards)
         .build();
   }

   private CreditCards scrapCreditCards(Document doc, Double spotlightPrice) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();
      Installments installments = new Installments();
      Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),
         Card.AURA.toString(), Card.DINERS.toString(), Card.HIPER.toString(), Card.AMEX.toString());

      installments.add(Installment.InstallmentBuilder.create()
         .setInstallmentNumber(1)
         .setInstallmentPrice(spotlightPrice)
         .build());

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


