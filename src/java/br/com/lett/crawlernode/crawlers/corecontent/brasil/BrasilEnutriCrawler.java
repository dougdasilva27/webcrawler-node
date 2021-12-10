package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.*;

import br.com.lett.crawlernode.util.*;
import com.google.common.collect.Sets;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.*;
import models.pricing.BankSlip;
import models.pricing.CreditCards;
import models.pricing.Pricing;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;

/**
 * Date: 20/08/2018
 *
 * @author Gabriel Dornelas
 */
public class BrasilEnutriCrawler extends Crawler {

   private static final String SELLER_FULL_NAME = "Enutri (Brasil)";
   private static final String HOME_PAGE = "https://www.enutri.com.br/";
   protected Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),
      Card.AMEX.toString(), Card.DINERS.toString(), Card.HIPERCARD.toString(), Card.AURA.toString(), Card.ELO.toString(),Card.DISCOVER.toString(), Card.JCB.toString());

   public BrasilEnutriCrawler(Session session) {
      super(session);
   }

   @Override
   public boolean shouldVisit() {
      String href = session.getOriginalURL().toLowerCase();
      return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
   }

   @Override
   public void handleCookiesBeforeFetch() {
      Logging.printLogDebug(logger, session, "Adding cookie...");

      BasicClientCookie cookie = new BasicClientCookie("loja", "base");
      cookie.setDomain(".www.enutri.com.br");
      cookie.setPath("/");
      this.cookies.add(cookie);
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      if (isProductPage(doc)) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         JSONObject productJSON = crawlProductJSON(doc);

         String internalPid = CrawlerUtils.scrapStringSimpleInfo(doc, "#product-reference", true);
         String internalId = productJSON.optString("idProduct");
         String name = productJSON.optString("nameProduct");
         String primaryImage = productJSON.optString("urlImage");
         List<String> secondaryImages = CrawlerUtils.scrapSecondaryImages(doc, ".box-img .zoom > img", Collections.singletonList("data-src"), "https", "images.tcdn.com.br", primaryImage);
         CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".breadcrumb-item:not(:first-child):not(:nth-child(2)):not(:last-child)", false);
         String description = CrawlerUtils.scrapSimpleDescription(doc, Collections.singletonList(".description"));
         List<String> eans = Collections.singletonList(productJSON.optString("EAN"));
         boolean availableToBuy = checkIfIsAvailable(doc);

         Offers offers = availableToBuy ? scrapOffers(doc, productJSON) : new Offers();

         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setInternalPid(internalPid)
            .setName(name)
            .setPrimaryImage(primaryImage)
            .setSecondaryImages(secondaryImages)
            .setCategories(categories)
            .setDescription(description)
            .setOffers(offers)
            .setEans(eans)
            .build();
         products.add(product);
      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }

   private CategoryCollection crawlCategories(Document doc) {
      CategoryCollection categoryCollection = CrawlerUtils.crawlCategories(doc, ".breadcrumb-item a", true);
      if(!categoryCollection.isEmpty()) {
         categoryCollection.remove(0);
      } else {
         return new CategoryCollection();
      }
      return categoryCollection;
   }

   private Offers scrapOffers(Document doc, JSONObject productJSON) throws MalformedPricingException, OfferException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(doc, productJSON);

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

   private Pricing scrapPricing(Document doc, JSONObject productJSON) throws MalformedPricingException {
      Double spotlightPrice = Double.parseDouble(productJSON.optString("priceSell"));
      Double priceFrom = Double.parseDouble(productJSON.optString("price"));
      CreditCards creditCards = scrapCreditCards(spotlightPrice);

      Double bankSlipPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".precoAvista", null, false, ',', session);
      BankSlip bankSlip = BankSlip.BankSlipBuilder.create()
         .setFinalPrice(bankSlipPrice)
         .setOnPageDiscount(0.05)
         .build();

      return Pricing.PricingBuilder.create()
         .setSpotlightPrice(spotlightPrice)
         .setPriceFrom(priceFrom)
         .setCreditCards(creditCards)
         .setBankSlip(bankSlip)
         .build();
   }

   private CreditCards scrapCreditCards(Double spotlightPrice) throws MalformedPricingException {
      return CrawlerUtils.scrapCreditCards(spotlightPrice, cards);
   }

   private boolean checkIfIsAvailable(Document doc) {
      return doc.select(".botao-nao_indisponivel").isEmpty() && doc.select(".botao-sob-consulta").isEmpty();
   }

   private JSONObject crawlProductJSON(Document doc) {
      String productJSONString = doc.selectFirst("script:containsData(dataLayer =)").data().replace("dataLayer = ", "");
      return CrawlerUtils.stringToJsonArray(productJSONString).getJSONObject(0);
   }

   private boolean isProductPage(Document doc) {
      return !doc.select(".box-col-product").isEmpty();
   }
}
