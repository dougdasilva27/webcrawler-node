package br.com.lett.crawlernode.crawlers.corecontent.argentina;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.crawlers.extractionutils.core.VTEXCrawlersUtils;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
import com.google.common.collect.Sets;
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
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class ArgentinaJumboCrawler extends Crawler {

   public ArgentinaJumboCrawler(Session session) {
      super(session);
   }

   private static final String HOME_PAGE = "https://www.jumbo.com.ar/";
   private static final String MAIN_SELLER_NAME_LOWER = "jumbo argentina";
   private static final String MAIN_SELLER_NAME_LOWER_2 = "jumboargentina";
   private static final String SELLER_FULL_NAME = "Jumbo";
   protected Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),
         Card.AURA.toString(), Card.DINERS.toString(), Card.HIPER.toString(), Card.AMEX.toString());

   @Override
   public boolean shouldVisit() {
      String href = session.getOriginalURL().toLowerCase();
      return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
   }


   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      if (isProductPage(doc)) {
         VTEXCrawlersUtils vtexUtil = new VTEXCrawlersUtils(session, MAIN_SELLER_NAME_LOWER, HOME_PAGE, cookies, dataFetcher);
         vtexUtil.setHasBankTicket(false);
         JSONObject skuJson = CrawlerUtils.crawlSkuJsonVTEX(doc, session);

         String internalPid = vtexUtil.crawlInternalPid(skuJson);
         CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".bread-crumb li > a", true);
         String description = CrawlerUtils.scrapSimpleDescription(doc, Arrays.asList(".productDescription", "#caracteristicas table"));

         // sku data in json
         JSONArray arraySkus = skuJson != null && skuJson.has("skus") ? skuJson.getJSONArray("skus") : new JSONArray();

         for (int i = 0; i < arraySkus.length(); i++) {
            JSONObject jsonSku = arraySkus.getJSONObject(i);

            String internalId = vtexUtil.crawlInternalId(jsonSku);
            JSONObject apiJSON = vtexUtil.crawlApi(internalId);
            String name = vtexUtil.crawlName(jsonSku, skuJson);
            String primaryImage = vtexUtil.crawlPrimaryImage(apiJSON);
            String secondaryImages = vtexUtil.crawlSecondaryImages(apiJSON);
            Integer stock = vtexUtil.crawlStock(apiJSON);
            boolean availableToBuy = jsonSku.optBoolean("available", false);
            Offers offers = availableToBuy ? scrapOffer(doc, internalId) : new Offers();

            // Creating the product
            Product product = ProductBuilder.create().setUrl(session.getOriginalURL()).setInternalId(internalId).setInternalPid(internalPid).setName(name)
                  .setCategory1(categories.getCategory(0)).setCategory2(categories.getCategory(1))
                  .setCategory3(categories.getCategory(2)).setPrimaryImage(primaryImage).setSecondaryImages(secondaryImages).setDescription(description)
                  .setStock(stock).setOffers(offers).build();

            products.add(product);
         }

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }

   private Offers scrapOffer(Document doc, String internalId) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      List<String> sales = new ArrayList<>();
      Pricing pricing = scrapPricing(internalId, doc);

      offers.add(OfferBuilder.create()
            .setUseSlugNameAsInternalSellerId(true)
            .setSellerFullName(SELLER_FULL_NAME)
            .setMainPagePosition(1)
            .setIsBuybox(false)
            .setIsMainRetailer(true)
            .setPricing(pricing)
            .setSales(sales)
            .build());

      return offers;
   }

   private Pricing scrapPricing(String internalId, Document doc) throws MalformedPricingException {
      Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".plugin-preco .skuBestPrice", null, false, ',', session);
      CreditCards creditCards = scrapCreditCards(internalId, doc, spotlightPrice);

      return PricingBuilder.create()
            .setSpotlightPrice(spotlightPrice)
            .setCreditCards(creditCards)
            .build();
   }


   private CreditCards scrapCreditCards(String internalId, Document document, Double spotlightPrice) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();

      String finalIndexUrl = CrawlerUtils.scrapStringSimpleInfoByAttribute(document, "#preFooter > div > div:nth-child(4) > a", "href");

      String pricesApi = "https://www.jumbo.com.ar/" + finalIndexUrl;
      Request request = RequestBuilder.create().setUrl(pricesApi).setCookies(cookies).build();
      Document doc = Jsoup.parse(this.dataFetcher.get(session, request).getBody());


      Elements cardsElements = doc.select(".payment-methods__card.cards-item img");

      if (!cardsElements.isEmpty()) {
         for (Element e : cardsElements) {
            String text = e.attr("alt").toLowerCase();
            String idCard = e.attr("alt");
            String card = null;
            Installments installments = scrapInstallments(document, idCard, spotlightPrice);

            if (text.contains("visa")) {
               card = Card.VISA.toString();

                  creditCards.add(CreditCardBuilder.create()
                        .setBrand(card)
                        .setInstallments(installments)
                        .setIsShopCard(false)
                        .build());

            } else if (text.contains("mastercard")) {
               card = Card.MASTERCARD.toString();

                  creditCards.add(CreditCardBuilder.create()
                        .setBrand(card)
                        .setInstallments(installments)
                        .setIsShopCard(false)
                        .build());

            } else if (text.contains("cabal")) {
               card = Card.CABAL.toString();

                  creditCards.add(CreditCardBuilder.create()
                        .setBrand(card)
                        .setInstallments(installments)
                        .setIsShopCard(false)
                        .build());

            } else if (text.contains("nativa")) {
               card = Card.NATIVA.toString();

                  creditCards.add(CreditCardBuilder.create()
                        .setBrand(card)
                        .setInstallments(installments)
                        .setIsShopCard(false)
                        .build());

            } else if (text.contains("naranja")) {
               card = Card.NARANJA.toString();

                  creditCards.add(CreditCardBuilder.create()
                        .setBrand(card)
                        .setInstallments(installments)
                        .setIsShopCard(false)
                        .build());

            } else if (text.contains("american express")) {
               card = Card.AMEX.toString();

                  creditCards.add(CreditCardBuilder.create()
                        .setBrand(card)
                        .setInstallments(installments)
                        .setIsShopCard(false)
                        .build());
               }
         }
      } else {
         Installments installments = new Installments();
         installments.add(InstallmentBuilder.create()
               .setInstallmentNumber(1)
               .setInstallmentPrice(spotlightPrice)
               .build());

         for (String card : cards) {
            creditCards.add(CreditCardBuilder.create()
                  .setBrand(card)
                  .setInstallments(installments)
                  .setIsShopCard(false)
                  .build());
         }
      }

      return creditCards;
   }

   public Installments scrapInstallments(Document doc, String idCard, Double spotlightPrice) throws MalformedPricingException {
      Installments installments = new Installments();

      if (installments.getInstallment(1) == null) {
         installments.add(InstallmentBuilder.create()
               .setInstallmentNumber(1)
               .setInstallmentPrice(spotlightPrice)
               .build());
      }

      if (doc.select(".skuBestInstallmentNumber") != null && doc.select(".skuBestInstallmentValue") != null) {
         String installmentNotFormatted = doc.select(".skuBestInstallmentNumber").toString();
         installmentNotFormatted = installmentNotFormatted.replaceAll("[^0-9]", "");
         Integer installment = Integer.parseInt(installmentNotFormatted);
         String doubleValue = doc.select(".skuBestInstallmentValue").text();
         doubleValue = doubleValue.replace("$ ", "");
         Double value = MathUtils.parseDoubleWithComma(doubleValue);

         installments.add(InstallmentBuilder.create()
               .setInstallmentNumber(installment)
               .setInstallmentPrice(value)
               .build());
      }
      return installments;
   }

   private boolean isProductPage(Document document) {
      return document.selectFirst(".productName") != null;
   }
}
