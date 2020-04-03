package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import com.google.common.collect.Sets;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
import br.com.lett.crawlernode.util.Pair;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.Marketplace;
import models.Offer;
import models.Offer.OfferBuilder;
import models.Offers;
import models.Seller;
import models.prices.Prices;
import models.pricing.BankSlip.BankSlipBuilder;
import models.pricing.CreditCard.CreditCardBuilder;
import models.pricing.CreditCards;
import models.pricing.Installment.InstallmentBuilder;
import models.pricing.Installments;
import models.pricing.Pricing;
import models.pricing.Pricing.PricingBuilder;

public class BrasilPethereCrawler extends Crawler {
  
  private static final String CDN_URL = "cdn.awsli.com.br";
  private static final String MAIN_SELLER_NAME = "Pet Here";
  private Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),
        Card.HIPERCARD.toString(), Card.AMEX.toString(), Card.ELO.toString());
  
  public BrasilPethereCrawler(Session session) {
    super(session);
  }

  @Override
  public List<Product> extractInformation(Document doc) throws Exception {
    super.extractInformation(doc);
    List<Product> products = new ArrayList<>();
    
    if (isProductPage(doc)) {
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

      String internalId = CrawlerUtils.scrapStringSimpleInfo(doc, ".codigo-produto [itemprop=\"sku\"]", true);
      String internalPid = CrawlerUtils.scrapStringSimpleInfo(doc, ".codigo-produto span[itemprop=\"sku\"]", true);
      String name = CrawlerUtils.scrapStringSimpleInfo(doc, ".nome-produto", false) + " - " + 
          CrawlerUtils.scrapStringSimpleInfo(doc, "[itemprop=\"brand\"] > a", true);
      CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".breadcrumbs > ul > li", true);
      String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, "#carouselImagem > ul > li img", 
          Arrays.asList("data-largeimg", "src", "data-mediumimg"), "https", CDN_URL);
      String secondaryImages = CrawlerUtils.scrapSimpleSecondaryImages(doc, "#carouselImagem > ul > li img", 
          Arrays.asList("data-largeimg", "src", "data-mediumimg"), "https", CDN_URL, primaryImage);
      String description = CrawlerUtils.scrapElementsDescription(doc, Arrays.asList("#descricao"));
      Offers offers = scrapOffers(doc);
          
      // Creating the product
      Product product = ProductBuilder.create()
          .setUrl(session.getOriginalURL())
          .setInternalId(internalId)
          .setInternalPid(internalPid)
          .setName(name)
          .setCategory1(categories.getCategory(0))
          .setCategory2(categories.getCategory(1))
          .setCategory3(categories.getCategory(2))
          .setPrimaryImage(primaryImage)
          .setSecondaryImages(secondaryImages)
          .setDescription(description)
          .setOffers(offers)
          .build();
      
      Elements variations = doc.select(".atributo-comum > ul > li > a");
      if(variations != null && !variations.isEmpty()) {
        for(Element variation : variations) {
          String variationId = variation.hasAttr("data-variacao-id") ? variation.attr("data-variacao-id") : null;
          if(variationId == null) {
             continue;
          }
          
          Product clone = product.clone();
          
          String variationName = CrawlerUtils.scrapStringSimpleInfo(variation, null, false);
          if(variationName != null) {
            clone.setName(product.getName() + " - " + variationName);
          }
          
          Element subElement = doc.selectFirst(".acoes-produto[data-variacao-id=\"" + variationId + "\"]");
          if(subElement != null) {
            if(subElement.hasAttr("data-produto-id")) {
              clone.setInternalId(subElement.attr("data-produto-id"));
            }
            
            Offers variationOffers = scrapVariationOffers(doc, subElement, clone.getInternalId());
            clone.setOffers(variationOffers);
            
            // TODO: remove this when bug is fixed - this is an almost copy paste from ProductBuilder.build()
            // bug happens because this code only occur at ProductBuilder.build()
            // when you clone a product and set a new offers, the other fields that are supposed
            // to change stays the same.
            if (variationOffers != null) {
               for (Offer offer : variationOffers.getOffersList()) {
                  if (offer.getPricing() != null) {
                     if (offer.getIsMainRetailer()) {
                        Pricing pricing = offer.getPricing();
                        clone.setAvailable(true);
                        clone.setPrices(new Prices(pricing));
                        clone.setPrice(pricing.getSpotlightPrice().floatValue());
                     } else {
                        Marketplace variationMkp = clone.getMarketplace();

                        if (variationMkp == null) {
                           variationMkp = new Marketplace();
                        }

                        variationMkp.add(new Seller(offer));
                        clone.setMarketplace(variationMkp);
                     }
                  }
               }
            }
            // ----------- REMOVE ABOVE -----------
          }
          
          products.add(clone);
        }
      } else {
        products.add(product);
      } 
    } else {
      Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
    }
    
    return products;
  }
  
  private Offers scrapVariationOffers(Element doc, Element subElement, String productId) throws OfferException, MalformedPricingException {
     Offers offers = new Offers();
     Pricing pricing = scrapVariationPricing(doc, subElement, productId);

     if(pricing != null) {
       offers.add(OfferBuilder.create()
             .setUseSlugNameAsInternalSellerId(true)
             .setSellerFullName(MAIN_SELLER_NAME)
             .setSellersPagePosition(1)
             .setIsBuybox(false)
             .setIsMainRetailer(true)
             .setPricing(pricing)
             .build());
     }
     
     return offers;
  }
  
  private Pricing scrapVariationPricing(Element doc, Element subElement, String productId) throws MalformedPricingException {
     Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(subElement, "[itemprop=\"price\"]", "content", false, '.', session);
           
     if(spotlightPrice != null) {
       Double priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(subElement, ".preco-venda", null, true, ',', session);
       CreditCards creditCards = scrapVariationCreditCards(doc, spotlightPrice, productId);

       return PricingBuilder.create()
             .setSpotlightPrice(spotlightPrice)
             .setPriceFrom(priceFrom)
             .setCreditCards(creditCards)
             .setBankSlip(BankSlipBuilder.create().setFinalPrice(spotlightPrice).setOnPageDiscount(0d).build())
             .build();
     }

     return null;
  }
  
  private CreditCards scrapVariationCreditCards(Element doc, Double spotlightPrice, String productId) throws MalformedPricingException {
     CreditCards creditCards = new CreditCards();
     Element installmentElement = doc.selectFirst(".parcelas-produto[data-produto-id=\"" + productId + "\"]");
     
     Installments installments = new Installments();
     installments.add(InstallmentBuilder.create()
           .setInstallmentNumber(1)
           .setInstallmentPrice(spotlightPrice)
           .build());
     
     Elements elements = installmentElement.select("[id*=mercadopago] ul > li");
     for(Element e : elements) {
        Pair<Integer, Float> pair = CrawlerUtils.crawlSimpleInstallment(null, e, false, "x", "juros", true, ',');
        if (!pair.isAnyValueNull()) {
           installments.add(InstallmentBuilder.create()
                 .setInstallmentNumber(pair.getFirst())
                 .setInstallmentPrice(MathUtils.normalizeTwoDecimalPlaces(pair.getSecond().doubleValue()))
                 .build());
        }
     }
     
     for (String brand : cards) {
        creditCards.add(CreditCardBuilder.create()
              .setBrand(brand)
              .setIsShopCard(false)
              .setInstallments(installments)
              .build());
     }
     
     return creditCards;
  }
  
  private Offers scrapOffers(Document doc) throws OfferException, MalformedPricingException {
     Offers offers = new Offers();
     Pricing pricing = scrapPricing(doc);

     if(pricing != null) {
       offers.add(OfferBuilder.create()
             .setUseSlugNameAsInternalSellerId(true)
             .setSellerFullName(MAIN_SELLER_NAME)
             .setSellersPagePosition(1)
             .setIsBuybox(false)
             .setIsMainRetailer(true)
             .setPricing(pricing)
             .build());
     }
     
     return offers;
  }
  
  private Pricing scrapPricing(Document doc) throws MalformedPricingException {
     Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".principal .preco-promocional", null, true, ',', session);

     if(spotlightPrice != null) {
       Double priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".principal .preco-venda", null, true, ',', session);
       CreditCards creditCards = scrapCreditCards(doc, spotlightPrice);

       return PricingBuilder.create()
             .setSpotlightPrice(spotlightPrice)
             .setPriceFrom(priceFrom)
             .setCreditCards(creditCards)
             .setBankSlip(BankSlipBuilder.create().setFinalPrice(spotlightPrice).setOnPageDiscount(0d).build())
             .build();
     }

     return null;
  }
  
  private CreditCards scrapCreditCards(Document doc, Double spotlightPrice) throws MalformedPricingException {
     CreditCards creditCards = new CreditCards();

     Installments installments = new Installments();
     installments.add(InstallmentBuilder.create()
           .setInstallmentNumber(1)
           .setInstallmentPrice(spotlightPrice)
           .build());
     
     Elements elements = doc.select("[id*=mercadopago] ul > li");
     for(Element e : elements) {
        Pair<Integer, Float> pair = CrawlerUtils.crawlSimpleInstallment(null, e, false, "x", "juros", true, ',');
        if (!pair.isAnyValueNull()) {
           installments.add(InstallmentBuilder.create()
                 .setInstallmentNumber(pair.getFirst())
                 .setInstallmentPrice(MathUtils.normalizeTwoDecimalPlaces(pair.getSecond().doubleValue()))
                 .build());
        }
     }
     
     for (String brand : cards) {
        creditCards.add(CreditCardBuilder.create()
              .setBrand(brand)
              .setIsShopCard(false)
              .setInstallments(installments)
              .build());
     }
     
     return creditCards;
  }
  
  private boolean isProductPage(Document doc) {
    return doc.selectFirst(".pagina-produto") != null;
  }
}
