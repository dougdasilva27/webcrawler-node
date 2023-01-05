package br.com.lett.crawlernode.crawlers.corecontent.mexico;

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
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MexicoLumenCrawler extends Crawler {

   private static final String SELLER_FULL_NAME = "Lumen";
   private final String HOME_PAGE = "https://lumen.com.mx";
   protected Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(), Card.AMEX.toString());

   public MexicoLumenCrawler(Session session) {
      super(session);
   }

   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      if (isProductPage(doc)) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, ".informar meta[itemprop=\"sku\"]", "content");
         String internalPid = null;
         String name = CrawlerUtils.scrapStringSimpleInfo(doc, ".informar h1[itemprop=\"name\"]", true);
         CategoryCollection categories = CrawlerUtils.crawlCategories(doc, "div.breadcrumb span[itemprop=\"name\"]", true);
         List<String> imagesList = crawlImagesList(doc);
         String primaryImage = imagesList.get(0);
         List<String> secondaryImages = imagesList.subList(1, imagesList.size());
         String description = crawlDescription(doc);
         boolean availableToBuy = doc.select(".no-disp").isEmpty();
         Offers offers = availableToBuy ? scrapOffer(doc, internalId) : new Offers();

         Elements variants = doc.select(".informar .selector-colores a");
         if (variants.size() > 0) {
            for (Element variant : variants) {
               String variantId = getVariantInternalId(variant);
               String variantUrl = null;
               if (variantId != null) {
                  variantUrl = session.getOriginalURL().replace(internalId, variantId);
               }
               String variantName = name + " - " + CrawlerUtils.scrapStringSimpleInfo(variant, "span", true);
               List<String> variantListImages = gatVariantImages(imagesList, variantId, internalId);
               String variantPrimaryImage = variantListImages.get(0);
               List<String> variantSecondaryImages = variantListImages.subList(1, variantListImages.size());

               Product product = ProductBuilder.create()
                  .setUrl(variantUrl)
                  .setInternalId(variantId)
                  .setInternalPid(internalPid)
                  .setName(variantName)
                  .setCategories(categories)
                  .setPrimaryImage(variantPrimaryImage)
                  .setSecondaryImages(variantSecondaryImages)
                  .setDescription(description)
                  .setOffers(offers)

                  .build();

               products.add(product);
            }
         } else {
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
         }

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }

   private List<String> gatVariantImages(List<String> secondaryImage, String variantId, String internalId) {
      List<String> variantListImages = new ArrayList<>();
      for (String image : secondaryImage) {
         if (image.contains(internalId)) {
            image = image.replace(internalId, variantId);
            variantListImages.add(image);
         }
      }

      return variantListImages;
   }

   private String getVariantInternalId(Element variant) {
      String imageUrl = CrawlerUtils.scrapStringSimpleInfoByAttribute(variant, "img", "src");
      String regex = "\\/([0-9]*).jpg";

      Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
      Matcher matcher = pattern.matcher(imageUrl);

      if (matcher.find()) {
         return matcher.group(1);
      }

      return null;
   }

   private List<String> crawlImagesList(Document doc) {
      Elements imagesGaleria = doc.select(".galeria img.img-responsive");
      List<String> imagesArray = new ArrayList<>();
      if(imagesGaleria.size() > 0) {
         for (Element image : imagesGaleria) {
            if (image.attr("src") != null && !image.attr("src").isEmpty()) {
               String imgUrl = HOME_PAGE + image.attr("src").replace("productPics_180x180", "productPics");
               imagesArray.add(imgUrl);
            }
         }
      }
      else{
         String imgUrl = HOME_PAGE + doc.select(".galeria #main-img").attr("src");
         imagesArray.add(imgUrl);
      }

      return imagesArray;
   }

   private String crawlDescription(Document doc) {
      String listDescription = CrawlerUtils.scrapElementsDescription(doc, Arrays.asList("ul.sobresalientes li"));
      String description = CrawlerUtils.scrapElementsDescription(doc, Arrays.asList("div.sobre-producto"));

      if (listDescription != null && !listDescription.isEmpty() && description != null && !description.isEmpty()) {
         return listDescription + description;
      }

      return null;
   }

   private boolean isProductPage(Document doc) {
      return doc.selectFirst("#product-details-form") != null;
   }

   private Offers scrapOffer(Document doc, String internalId) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(internalId, doc);
      List<String> sales = scrapSales(doc);

      offers.add(Offer.OfferBuilder.create()
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
      Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".precio.descuento", null, false, ',', session);
      Double priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".precio.viejo", null, false, ',', session);

      if (spotlightPrice == null) {
         spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".comprar .precio", null, false, ',', session);
      }

      CreditCards creditCards = scrapCreditCards(spotlightPrice);

      return Pricing.PricingBuilder.create()
         .setPriceFrom(priceFrom)
         .setSpotlightPrice(spotlightPrice)
         .setCreditCards(creditCards)
         .build();
   }

   private List<String> scrapSales(Document doc) {
      List<String> sales = new ArrayList<>();

      Element salesOneElement = doc.selectFirst(".span-btn-msi");
      String firstSales = salesOneElement != null ? salesOneElement.text() : null;

      if (firstSales != null && !firstSales.isEmpty()) {
         sales.add(firstSales);
      }

      return sales;
   }

   private CreditCards scrapCreditCards(Double spotlightPrice) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();
      Installments installments = new Installments();

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
