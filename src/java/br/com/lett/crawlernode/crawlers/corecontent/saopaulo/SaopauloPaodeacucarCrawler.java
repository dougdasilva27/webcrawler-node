package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.GPACrawler;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.Offer;
import models.Offers;
import models.pricing.BankSlip;
import models.pricing.CreditCards;
import models.pricing.Pricing;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class SaopauloPaodeacucarCrawler extends GPACrawler {

   private static final String CEP1 = "01007-040";

   private static final String SELLER_NAME = "Pão de Açúcar";

   public SaopauloPaodeacucarCrawler(Session session) {
      super(session);
      this.cep = CEP1;
   }

   @Override
   protected boolean hasMarketPlace(Document doc) {
      Elements sellerContainer = doc.select(".buy-box-contentstyles__Container-sc-18rwav0-2.grwTtk");
      String sellerName = CrawlerUtils.scrapStringSimpleInfo(doc,".buy-box-contentstyles__Container-sc-18rwav0-2.grwTtk p:first-child span:not(:first-child)", false);

      boolean equalsSeller = false;

      if(sellerName != null){
         equalsSeller = sellerName.equalsIgnoreCase(SELLER_NAME);
      }
      return sellerContainer.size() > 1 || equalsSeller;
   }


   @Override
   protected Offers offersFromMarketPlace(Document doc) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      int pos = 1;

      Elements ofertas = doc.select(".buy-box-contentstyles__Container-sc-18rwav0-2.grwTtk");

      if (ofertas != null) {
         for (Element oferta : ofertas) {
            String sellerName = CrawlerUtils.scrapStringSimpleInfo(oferta, "p:first-child span:not(:first-child)", false);
            Pricing pricing = scrapSellersPricing(oferta);
            boolean isMainRetailer = sellerName.equalsIgnoreCase(SELLER_NAME);

            offers.add(Offer.OfferBuilder.create()
               .setInternalSellerId(CommonMethods.toSlug(SELLER_NAME))
               .setSellerFullName(sellerName)
               .setSellersPagePosition(pos)
               .setIsBuybox(false)
               .setIsMainRetailer(isMainRetailer)
               .setPricing(pricing)
               .build());

            pos++;
         }
      }
      return offers;
   }


   @Override
   protected Pricing scrapSellersPricing(Element e) throws MalformedPricingException {
      Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(e, ".current-pricesectionstyles__CurrentPrice-sc-17j9p6i-0 p", null, false, ',', session);
      BankSlip bankSlip = CrawlerUtils.setBankSlipOffers(spotlightPrice, null);
      CreditCards creditCards = scrapCreditCards(spotlightPrice);
      return Pricing.PricingBuilder.create()
         .setSpotlightPrice(spotlightPrice)
         .setCreditCards(creditCards)
         .setBankSlip(bankSlip)
         .build();
   }

}
