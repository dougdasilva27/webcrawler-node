package br.com.lett.crawlernode.crawlers.corecontent.riodejaneiro;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
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
import org.apache.http.impl.cookie.BasicClientCookie;
import org.jsoup.nodes.Document;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class RiodejaneiroLafruteriaCrawler  extends Crawler {

   private static final String ASPX_ID = "lzgyggrjkwxl42howk045y1c";
   private static final String HOME_PAGE = "lojaonline.lafruteria.com.br";
   private static final String SELLER_FULL_NAME = "La Fruteria";
   protected Set<Card> cards = Sets.newHashSet(Card.VISA,Card.VISA.MASTERCARD,Card.ELO,Card.AMEX,Card.HIPERCARD,Card.JCB);

   public RiodejaneiroLafruteriaCrawler(Session session) {
      super(session);
      config.setFetcher(FetchMode.FETCHER);
   }

   @Override
   public void handleCookiesBeforeFetch() {
      this.cookies = Arrays.asList(new BasicClientCookie("ASP.NET_SessionId",ASPX_ID));
   }

   @Override
   public List<Product> extractInformation(Document document) throws Exception {
      List<Product> products = new ArrayList<>();
      String internalId = CrawlerUtils.scrapStringSimpleInfo(document,"#ctl00_ContentPlaceHolder1_lblRef",true);

      if(internalId!=null && !internalId.isEmpty()){
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String name = CrawlerUtils.scrapStringSimpleInfo(document,".nome",true);
         CategoryCollection category = ScrapCategory(document);
         String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(document,"#ctl00_ContentPlaceHolder1_imgProdutoExposicao",Arrays.asList("src"),"https:",HOME_PAGE);
         String description = CrawlerUtils.scrapStringSimpleInfo(document,"#ctl00_ContentPlaceHolder1_lblDescricaoProduto",true);
         Offers offers = ScrapOffers(document);

         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setName(name)
            .setCategories(category)
            .setPrimaryImage(primaryImage)
            .setDescription(description)
            .setOffers(offers)
            .build();
         products.add(product);
      }
      else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }




      return products;
   }

   private Offers ScrapOffers(Document document) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();

      //no unavailable product was found.Solution that possibly works
      boolean available = document.selectFirst(".btn-comprar") != null;

      if(available){
         Pricing pricing = ScrapPricing(document);
         
         offers.add(Offer.OfferBuilder.create()
            .setUseSlugNameAsInternalSellerId(true)
            .setSellerFullName(SELLER_FULL_NAME)
            .setMainPagePosition(1)
            .setIsBuybox(false)
            .setIsMainRetailer(true)
            .setPricing(pricing)
            .build());
      }
      return offers;
   }

   private Pricing ScrapPricing(Document document) throws MalformedPricingException {

      Double price = CrawlerUtils.scrapDoublePriceFromHtml(document,"#ctl00_ContentPlaceHolder1_lblValorProduto",null,true,',',session);
      CreditCards creditCards = ScrapCreditCards(document,price);
      return Pricing.PricingBuilder.create()
         .setSpotlightPrice(price)
         .setCreditCards(creditCards)
         .build();
   }

   private CreditCards ScrapCreditCards(Document document, Double price) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();
      Installments installments = new Installments();
      installments.add(Installment.InstallmentBuilder.create()
         .setInstallmentPrice(price)
         .setInstallmentNumber(1)
         .build());
      for (Card card : cards) {
         creditCards.add(CreditCard.CreditCardBuilder.create()
            .setBrand(card.toString())
            .setInstallments(installments)
            .setIsShopCard(false)
            .build());

      }

      return creditCards;
   }

   private CategoryCollection ScrapCategory(Document document) {
      CategoryCollection category = new CategoryCollection();
      category.add(CrawlerUtils.scrapStringSimpleInfo(document,"#ctl00_ContentPlaceHolder1_lblSecao",true));
      category.add(CrawlerUtils.scrapStringSimpleInfo(document,"#ctl00_ContentPlaceHolder1_lblSubsecao",true));
      return category;
   }
}
