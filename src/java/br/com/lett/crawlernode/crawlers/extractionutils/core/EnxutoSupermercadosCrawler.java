package br.com.lett.crawlernode.crawlers.extractionutils.core;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import com.google.common.collect.Sets;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.Offer;
import models.Offers;
import models.pricing.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.*;

public abstract class EnxutoSupermercadosCrawler extends Crawler {

   protected String storeId = getStoreId();

   protected abstract String getStoreId();

   public EnxutoSupermercadosCrawler(Session session) {
      super(session);
   }


   @Override
   protected Object fetch() {

      String url = "https://drive.enxuto.com.br/shop/home.xhtml";
      String keyword = getKeywordFromUrl(session.getOriginalURL());

      Map<String, String> headers = new HashMap<>();
      headers.put("Cookie", "JSESSIONID=fa09b6760425579e0fe62ac2feaf");
      headers.put("Connection", "keep-alive");

      StringBuilder payload = new StringBuilder();
      payload.append("formPesquisa:pesquisa: " + keyword);
      payload.append("javax.faces.ViewState: " + storeId);
      payload.append("ofertas_offset: 160");

      Request request = Request.RequestBuilder.create().setUrl(url).setHeaders(headers).setPayload(payload.toString()).build();
      return Jsoup.parse(this.dataFetcher.post(session, request).getBody());
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      if (session.getOriginalURL().contains("product_pid")) {
         String internalPidURL = CommonMethods.getLast(session.getOriginalURL().split("product_pid="));

         Elements productsElements = doc.select(".ui-datascroller-list li .ui-corner-all.oferta");

         for (Element e : productsElements) {

            String internalPid = CommonMethods.getLast(CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".ui-panelgrid.oferta-actions .ui-g .ui-panelgrid-cell:first-child a span", "id").split("_"));

            if (internalPidURL.equals(internalPid)) {

               String internalId = internalPid;
               String name = CrawlerUtils.scrapStringSimpleInfo(e, ".oferta-descricao", false);
               String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(e, ".oferta-img", Arrays.asList("src"), "https:", "drive.enxuto.com.br");
               CategoryCollection categories = CrawlerUtils.crawlCategories(e.ownerDocument(), ".oferta-sessoes a");
               boolean available = e.selectFirst(".indisponivel") == null;
               Offers offers = available? scrapOffers(e): new Offers();

               // Creating the product
               Product product = ProductBuilder.create()
                  .setUrl(session.getOriginalURL())
                  .setInternalId(internalId)
                  .setInternalPid(internalId)
                  .setName(name)
                  .setPrimaryImage(primaryImage)
                  .setCategories(categories)
                  .setOffers(offers)
                  .build();

               products.add(product);

               Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

            }
         }
      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }
      return products;
   }


   private String getKeywordFromUrl(String url){
      String keyword = "";

      if(url.contains("search=")){
         String splitOne = CommonMethods.getLast(url.split("search="));
         keyword = splitOne != null && splitOne.contains("/")? splitOne.split("/")[0]: null;
      }
      return keyword;
   }


   private Offers scrapOffers(Element e) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(e);
      List<String> sales = new ArrayList<>();

      offers.add(Offer.OfferBuilder.create()
         .setUseSlugNameAsInternalSellerId(true)
         .setSellerFullName("Enxuto")
         .setMainPagePosition(1)
         .setIsBuybox(false)
         .setIsMainRetailer(true)
         .setPricing(pricing)
         .setSales(sales)
         .build());

      return offers;

   }

   private Pricing scrapPricing(Element e) throws MalformedPricingException {

      Double priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(e,".oferta-preco-cortado", null,true,',',session);
      Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(e,".oferta-preco-destaque", null,true,',',session);
      BankSlip bankSlip = CrawlerUtils.setBankSlipOffers(spotlightPrice, null);
      CreditCards creditCards = scrapCreditCards(spotlightPrice);

      return Pricing.PricingBuilder.create()
         .setPriceFrom(priceFrom)
         .setSpotlightPrice(spotlightPrice)
         .setCreditCards(creditCards)
         .setBankSlip(bankSlip)
         .build();
   }

   private CreditCards scrapCreditCards(Double spotlightPrice) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();
      Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),
         Card.AURA.toString(), Card.DINERS.toString(), Card.HIPER.toString(), Card.AMEX.toString());

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

}
