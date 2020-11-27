package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import static models.Offer.OfferBuilder;
import static models.pricing.BankSlip.BankSlipBuilder;
import static models.pricing.Installment.InstallmentBuilder;
import static models.pricing.Pricing.PricingBuilder;

import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.crawlers.extractionutils.core.TrustvoxRatingCrawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
import br.com.lett.crawlernode.util.Pair;
import com.google.common.collect.Sets;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import models.Offers;
import models.RatingsReviews;
import models.pricing.CreditCard;
import models.pricing.CreditCards;
import models.pricing.Installments;
import models.pricing.Pricing;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class BrasilMadeiramadeiraCrawler extends Crawler {

   private static final String HOME_PAGE = "https://www.madeiramadeira.com.br/";

   public BrasilMadeiramadeiraCrawler(Session session) {
      super(session);
      super.config.setMustSendRatingToKinesis(true);
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();
      if (isProductPage(doc)) {

         String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "data[data-product-id]", "value");
         String name = CrawlerUtils.scrapStringSimpleInfo(doc, ".product-info__title", false);
         CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".breadcrumb li", true);
         String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, "ul.media-gallery__list li img",
             Collections.singletonList(
                 "src"),
               "https",
               "images.madeiramadeira.com.br");
         String secondaryImages = CrawlerUtils.scrapSimpleSecondaryImages(doc, "ul.media-gallery__list li img",
             Collections.singletonList("src"), "https", "images.madeiramadeira.com.br",
               primaryImage);

         RatingsReviews ratingsReviews = scrapRating(internalId, doc);
         String description = CrawlerUtils.scrapSimpleDescription(doc,
             Collections.singletonList(".tab__content"));

         String availableEl = doc.selectFirst("#product-buy-button") != null ? doc.selectFirst("#product-buy-button").toString() : "";
         Offers offers = availableEl.contains("Comprar")? scrapOffers(doc) : new Offers();

         //identificamos uma mudança de internalId no mm e pedimos que reunifiquem essa loja
         // Creating the product
         Product product = ProductBuilder.create()
               .setUrl(session.getOriginalURL())
               .setInternalId(internalId)
               .setInternalPid(internalId)
               .setName(name)
               .setOffers(offers)
               .setCategory1(categories.getCategory(0))
               .setCategory2(categories.getCategory(1))
               .setCategory3(categories.getCategory(2))
               .setPrimaryImage(primaryImage)
               .setSecondaryImages(secondaryImages)
               .setDescription(description)
               .setRatingReviews(ratingsReviews)
               .build();

         products.add(product);

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;

   }

   private Offers scrapOffers(Document doc) throws MalformedPricingException, OfferException {
      Offers offers = new Offers();

      final String regex = "(?i)madeiramadeira\\s?|madeira?[-]?madeira";
      Double price = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".product-prices__box .product-prices__big-price", null, false, ',', session);
      Double priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".product-prices__box .helper--has-text-large", null, false, ',', session);
      Pair<Integer, Float> pairInst = CrawlerUtils.crawlSimpleInstallment("span.helper--has-text-medium:nth-of-type(2)", doc, true, "x");
      Double creditCardPrice;

      if(doc.selectFirst(".product__buybox .helper--has-text-medium .helper--has-text-bold") != null){
         creditCardPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".product__buybox  .helper--has-text-medium .helper--has-text-bold", null, false, ',', session);
      } else {
         creditCardPrice = price;
      }

      Pricing pricing = PricingBuilder.create()
          .setSpotlightPrice(price)
          .setPriceFrom(priceFrom)
          .setBankSlip(BankSlipBuilder.create()
              .setFinalPrice(price)
              .build())
          .setCreditCards(new CreditCards(scrapCards(pairInst, creditCardPrice)))
          .build();


      List<String> sales = new ArrayList<>();
      Element saleElem = doc.selectFirst(".product-flag,.blackfriday-flags");
      if (saleElem != null) sales.add(saleElem.wholeText().trim());

      String sellerName = doc.selectFirst(".product-info__seller strong").text();

      String url = session.getOriginalURL().replace(HOME_PAGE, HOME_PAGE + "/parceiros/");
      Response response = dataFetcher.get(session, RequestBuilder.create().setCookies(cookies).setUrl(url).build());
      Document sellers = Jsoup.parse(response.getBody());
      Elements markets = sellers.select(".buybox-list div[data-product-container]");

      offers.add(OfferBuilder.create()
              .setPricing(pricing)
              .setIsBuybox(markets.size() > 1)
              .setSellerFullName(sellerName)
              .setMainPagePosition(1)
              .setUseSlugNameAsInternalSellerId(true)
              .setIsMainRetailer(Pattern.matches(regex, sellerName))
              .setSales(sales)
              .build());

      for (int i = 0; i < markets.size(); i++) {
         Element element = markets.get(i);
         Element nameElement = element.selectFirst(".name");
         Element priceElement = element.selectFirst(".price");
         Double priceNext = MathUtils.parseDoubleWithComma(priceElement.ownText());
         if (nameElement.text().equalsIgnoreCase(sellerName)) {
            continue;
         }
         offers.add(OfferBuilder.create()
                 .setPricing(PricingBuilder.create()
                     .setSpotlightPrice(priceNext)
                     .setBankSlip(BankSlipBuilder.create()
                         .setFinalPrice(priceNext)
                         .build())
                     .setCreditCards(new CreditCards(scrapCards(pairInst, priceNext)))
                     .build())
                 .setIsBuybox(markets.size() > 1)
                 .setSellerFullName(nameElement.text())
                 .setSellersPagePosition(i + 1)
                 .setUseSlugNameAsInternalSellerId(true)
                 .setIsMainRetailer(Pattern.matches(regex, sellerName))
                 .setSales(sales)
                 .build());
      }

      return offers;
   }

   private List<CreditCard> scrapCards(Pair<Integer, Float> pairInst, Double price){
      if (pairInst.isAnyValueNull()) {
         return new ArrayList<>();
      }
      return Stream.of(Card.VISA, Card.MASTERCARD, Card.ELO)
              .map(card -> {
                 try {
                    return CreditCard.CreditCardBuilder.create()
                            .setIsShopCard(false)
                            .setBrand(card.toString())
                            .setInstallments(new Installments(
                                    Sets.newHashSet(InstallmentBuilder.create()
                                            .setInstallmentNumber(1)
                                            .setInstallmentPrice(price)
                                            .setFinalPrice(price)
                                            .build(), InstallmentBuilder.create()
                                            .setInstallmentNumber(pairInst.getFirst())
                                            .setInstallmentPrice(MathUtils.normalizeTwoDecimalPlaces(pairInst.getSecond().doubleValue()))
                                            .setFinalPrice(pairInst.getFirst() * pairInst.getSecond().doubleValue())
                                            .build())
                            ))
                            .build();
                 } catch (MalformedPricingException e) {
                    throw new RuntimeException(e);
                 }
              })
              .collect(Collectors.toList());
   }

   private boolean isProductPage(Document doc) {
      return doc.select(".section product__header").isEmpty();
   }

   private RatingsReviews scrapRating(String internalId, Document doc) {
      TrustvoxRatingCrawler trustVox = new TrustvoxRatingCrawler(session, "85050", logger);
      return trustVox.extractRatingAndReviews(internalId, doc, dataFetcher);
   }
}
