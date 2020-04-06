package br.com.lett.crawlernode.crawlers.corecontent.portoalegre;

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
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.Offer.OfferBuilder;
import models.Offers;
import models.pricing.BankSlip.BankSlipBuilder;
import models.pricing.CreditCard.CreditCardBuilder;
import models.pricing.CreditCards;
import models.pricing.Installment.InstallmentBuilder;
import models.pricing.Installments;
import models.pricing.Pricing;
import models.pricing.Pricing.PricingBuilder;

public class PortoalegreAguiaveterinariaCrawler extends Crawler {

  private static final String INTERNALPID_ID = "varproduto_id='";
  private static final String INTERNALID_ID = "vargrade_id='";
  private static final String MAIN_SELLER_NAME = "Águia Veterinária";
  private Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),
         Card.ELO.toString(), Card.DINERS.toString(), Card.AMEX.toString());

  public PortoalegreAguiaveterinariaCrawler(Session session) {
    super(session);
  }

  @Override
  public List<Product> extractInformation(Document doc) throws Exception {
    super.extractInformation(doc);
    List<Product> products = new ArrayList<>();

    String idsScript = scrapScripWithIds(doc);

    if (idsScript != null) {
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

      String internalPid = CrawlerUtils.extractSpecificStringFromScript(idsScript, INTERNALPID_ID, false, "';", false);
      CategoryCollection categories = new CategoryCollection();
      String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, ".single-product-gallery-item a", Arrays.asList("href"), "https:",
          "www.aguiaveterinaria.com.br");
      String secondaryImages = CrawlerUtils.scrapSimpleSecondaryImages(doc, ".product-sku-image .image-highlight a.main-product img",
          Arrays.asList("data-zoom-image", "src"), "https", "www.aguiaveterinaria.com.br", primaryImage);
      String description = CrawlerUtils.scrapSimpleDescription(doc, Arrays.asList("#description"));

      Elements variationsElements = doc.select("#variacao_ul li");
      if (!variationsElements.isEmpty()) {
        for (Element variationElement : variationsElements) {
          String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(variationElement, "input[value]", "value");
          String name = CrawlerUtils.scrapStringSimpleInfo(variationElement, "> label", true);
          Offers offers = scrapOffers(variationElement, true);

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

          products.add(product);
        }
      } else {
        String internalId = CrawlerUtils.extractSpecificStringFromScript(idsScript, INTERNALID_ID, false, "';", false);
        String name = CrawlerUtils.scrapStringSimpleInfo(doc, "h1[itemprop=\"name\"]", true);
        Offers offers = scrapOffers(doc, false);

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

        products.add(product);
      }

    } else {
      Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
    }

    return products;
  }
  
  private Offers scrapOffers(Element doc, boolean isVariation) throws OfferException, MalformedPricingException {
     Offers offers = new Offers();
     Pricing pricing = scrapPricing(doc, isVariation);

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
  
  private Pricing scrapPricing(Element doc, boolean isVariation) throws MalformedPricingException {
     Double spotlightPrice = isVariation ? 
           scrapPriceVariation(doc) :
           CrawlerUtils.scrapDoublePriceFromHtml(doc, ".final_price", "content", false, ',', session);

     if(spotlightPrice != null) {
        Double priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(doc, "input[data-promo]", "data-promo", true, '.', session);
        if (priceFrom == null || priceFrom <= 0f) {
          priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(doc, "input[data-promo]", "data-preco", true, '.', session);
        }
        
       CreditCards creditCards = scrapCreditCards(doc, spotlightPrice);

       return PricingBuilder.create()
             .setSpotlightPrice(spotlightPrice)
             .setPriceFrom(priceFrom != null ? priceFrom : CrawlerUtils.scrapDoublePriceFromHtml(doc, ".previous-price", null, true, ',', session))
             .setCreditCards(creditCards)
             .setBankSlip(BankSlipBuilder.create().setFinalPrice(spotlightPrice).setOnPageDiscount(0d).build())
             .build();
     }

     return null;
  }
  
  private CreditCards scrapCreditCards(Element doc, Double spotlightPrice) throws MalformedPricingException {
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
  
  private Float scrapPriceVariation(Element variationElement) {
     Float price = CrawlerUtils.scrapFloatPriceFromHtml(variationElement, "input", "data-promo", true, '.', session);

     if (price == null || price <= 0f) {
       price = CrawlerUtils.scrapFloatPriceFromHtml(variationElement, "input", "data-preco", true, '.', session);
     }

     return price;
   }

  private String scrapScripWithIds(Document doc) {
    String script = null;

    Elements scripts = doc.select("script");
    for (Element e : scripts) {
      String html = e.html().replace(" ", "");

      if (html.contains(INTERNALPID_ID) && html.contains(INTERNALID_ID)) {
        script = html;
        break;
      }
    }

    return script;
  }
}
