package br.com.lett.crawlernode.crawlers.corecontent.curitiba;

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
import models.AdvancedRatingReview;
import models.Offer;
import models.Offers;
import models.RatingsReviews;
import models.pricing.*;
import org.json.JSONArray;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.DataOutput;
import java.util.*;

public class CuritibaCasafiestaCrawler extends Crawler {

   private static final String HOME_PAGE = "https://www.casafiesta.com.br/";
   private static final String SELLER_FULL_NAME = "Casa Fiesta";
   protected Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),
      Card.DINERS.toString(), Card.HIPER.toString());

   public CuritibaCasafiestaCrawler(Session session){
      super(session);
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      if(isProductPage(doc)){

         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String name = CrawlerUtils.scrapStringSimpleInfo(doc, ".produtoInfo-title > h1", false);
         String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, ".produtoInfo #hdnProdutoId", "value");
         String internalPid = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, ".produtoInfo #hdnProdutoVarianteId-0", "value");
         String description = CrawlerUtils.scrapElementsDescription(doc, Arrays.asList(".fbits-produto-informacoes-extras .nutricional table tbody tr"));
         CategoryCollection categories = scrapCategories(doc);
         String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, "#imagem-pagina-produto .imagem-etiquetas img", Collections.singletonList("src"), "https", HOME_PAGE);
         String secondaryImages = CrawlerUtils.scrapSimpleSecondaryImages(doc, "#imagem-pagina-produto .jcarousel #galeria a",Collections.singletonList("data-image"), "https", HOME_PAGE, primaryImage);

         boolean available = isAvailable(doc);
         Offers offers = available ? scrapOffers(doc) : new Offers();
         RatingsReviews rating = scrapRating(doc, internalId);

         //Create product
         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setName(name)
            .setInternalId(internalId)
            .setInternalPid(internalPid)
            .setDescription(description)
            .setCategories(categories)
            .setPrimaryImage(primaryImage)
            .setSecondaryImages(secondaryImages)
            .setOffers(offers)
            .setRatingReviews(rating)
            .build();

         products.add(product);

      } else{
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }

   private boolean isProductPage(Document doc){
      return CrawlerUtils.scrapStringSimpleInfo(doc, ".detalhe-produto", false) != null;
   }

   private CategoryCollection scrapCategories(Document doc){
      CategoryCollection categories = new CategoryCollection();

      Elements categoriesTags = doc.select("#fbits-breadcrumb li a");
      for(Element e:categoriesTags){
         categories.add(e.text());
      }
      return categories;
   }

   private boolean isAvailable(Document doc){

      String availability = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "meta[property='product:availability']", "content");
      if(availability != null){
         return availability.equals("em estoque");
      }
      return false;
   }

   private Offers scrapOffers(Document doc) throws MalformedPricingException, OfferException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(doc);

      offers.add(Offer.OfferBuilder.create()
         .setUseSlugNameAsInternalSellerId(true)
         .setSellerFullName(SELLER_FULL_NAME)
         .setMainPagePosition(1)
         .setIsBuybox(false)
         .setIsMainRetailer(true)
         .setPricing(pricing)
         .build());

      return offers;
   }

   private Pricing scrapPricing(Document doc) throws MalformedPricingException {

      Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".produtoInfo .precoPor", null, false, ',', session);
      Double priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".produtoInfo .precoDe", null, false, ',', session);
      BankSlip bankSlip = CrawlerUtils.setBankSlipOffers(spotlightPrice, null);

      CreditCards creditCards = scrapCreditCards(spotlightPrice);

      return Pricing.PricingBuilder.create()
         .setPriceFrom(priceFrom)
         .setSpotlightPrice(spotlightPrice)
         .setBankSlip(bankSlip)
         .setCreditCards(creditCards)
         .build();

   }

   private CreditCards scrapCreditCards(Double spotlightPrice) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();

      Installments installments = new Installments();
      if (installments.getInstallments().isEmpty()) {
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

   private RatingsReviews scrapRating(Document doc, String internalId){

      RatingsReviews ratingReviews = new RatingsReviews();
      Integer totalNumOfEvaluations = CrawlerUtils.scrapIntegerFromHtmlAttr(doc, "#avaliacao-Produto > div:nth-child(3) meta[itemprop=ratingCount]", "content",0);
      Integer totalWrittenReviews = CrawlerUtils.scrapIntegerFromHtmlAttr(doc, "#avaliacao-Produto > div:nth-child(3) meta[itemprop=reviewCount]", "content",0);
      Double avgRating = CrawlerUtils.scrapDoublePriceFromHtml(doc, "#avaliacao-Produto > div:nth-child(3) meta[itemprop=ratingValue]", "content", false, ',', session);


      ratingReviews.setDate(session.getDate());
      ratingReviews.setInternalId(internalId);
      ratingReviews.setTotalRating(totalNumOfEvaluations);
      ratingReviews.setTotalWrittenReviews(totalWrittenReviews);
      ratingReviews.setAverageOverallRating(avgRating);

      return ratingReviews;
   }
}
