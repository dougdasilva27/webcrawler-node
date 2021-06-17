package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.methods.FetcherDataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.JsoupDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.FetcherOptions.FetcherOptionsBuilder;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.B2WCrawler;
import br.com.lett.crawlernode.crawlers.extractionutils.core.SaopauloB2WCrawlersUtils;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.Offer;
import models.Offers;
import models.pricing.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class SaopauloSubmarinoCrawler extends B2WCrawler {

   private static final String HOME_PAGE = "https://www.submarino.com.br/";
   private static final String MAIN_SELLER_NAME_LOWER = "submarino";
   private static final String MAIN_SELLER_NAME_LOWER_FROM_HTML = "Submarino";


   public SaopauloSubmarinoCrawler(Session session) {
      super(session);
      super.subSellers = new ArrayList<>();
      super.sellerNameLower = MAIN_SELLER_NAME_LOWER;
      super.sellerNameLowerFromHTML = MAIN_SELLER_NAME_LOWER_FROM_HTML;
      super.homePage = HOME_PAGE;
      super.config.setFetcher(FetchMode.FETCHER);

   }

   @Override
   public void handleCookiesBeforeFetch() {
      Request request;

      if (dataFetcher instanceof FetcherDataFetcher) {
         request = RequestBuilder.create().setUrl(HOME_PAGE)
               .setCookies(cookies)
               .setProxyservice(
                     Arrays.asList(
                           ProxyCollection.INFATICA_RESIDENTIAL_BR,
                           ProxyCollection.NETNUT_RESIDENTIAL_BR,
                           ProxyCollection.BUY
                     )
               ).mustSendContentEncoding(false)
               .setFetcheroptions(FetcherOptionsBuilder.create()
                     .setForbiddenCssSelector("#px-captcha")
                     .mustUseMovingAverage(false)
                     .mustRetrieveStatistics(true).build())
               .build();
      } else {
         request = RequestBuilder.create().setUrl(HOME_PAGE).setCookies(cookies).build();
      }

      this.cookies = CrawlerUtils.fetchCookiesFromAPage(request, "www.submarino.com.br", "/", null, session, dataFetcher);
   }

   @Override
   protected Offers scrapOffers(Document doc, String internalId, String internalPid, int arrayPosition) throws MalformedPricingException, OfferException {

      Offers offers = new Offers();

      String scrapUrl = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, ".offers-box__Wrapper-fiox0-0 a[aria-current]", "href");

      if (scrapUrl == null) {

         JSONObject jsonSeller = CrawlerUtils.selectJsonFromHtml(doc, "script", "window.__PRELOADED_STATE__ =", null, false, true);
         JSONObject offersJson = SaopauloB2WCrawlersUtils.newWayToExtractJsonOffers(jsonSeller,internalPid, arrayPosition);

         setOffersForMainPageSeller(offers, offersJson, internalId);

      } else {

         String offersPageUrl = " https://www.submarino.com.br/parceiros/"+ internalPid +"?productSku=" + internalId;

         Document offersDoc = acessOffersPage(offersPageUrl);
         Elements offersFromHTML = offersDoc.select(".src__Background-qslyla-1 .src__Card-qslyla-3 > div");
         setOffersForSellersPage(offers, offersFromHTML);

      }
      return offers;
   }

   private Document acessOffersPage(String offersPageURL) {
      Request request = Request.RequestBuilder.create().setUrl(offersPageURL).setProxyservice(
         Arrays.asList(
            ProxyCollection.INFATICA_RESIDENTIAL_BR_HAPROXY,
            ProxyCollection.NETNUT_RESIDENTIAL_ES_HAPROXY,
            ProxyCollection.NETNUT_RESIDENTIAL_BR_HAPROXY
         )
      ).build();
      Response response = this.dataFetcher.get(session, request);
      String content = response.getBody();

      int statusCode = response.getLastStatusCode();

      if ((Integer.toString(statusCode).charAt(0) != '2' &&
         Integer.toString(statusCode).charAt(0) != '3'
         && statusCode != 404)) {
         request.setProxyServices(Arrays.asList(
            ProxyCollection.BUY_HAPROXY,
            ProxyCollection.NETNUT_RESIDENTIAL_BR_HAPROXY,
            ProxyCollection.INFATICA_RESIDENTIAL_BR_HAPROXY));

         content = new JsoupDataFetcher().get(session, request).getBody();
      }

      return Jsoup.parse(content);
   }


   private void setOffersForMainPageSeller (Offers offers ,JSONObject offersJson, String internalId) throws OfferException, MalformedPricingException {
      Map<String, Double> mapOfSellerIdAndPrice = new HashMap<>();
      boolean twoPositions = false;

      if (offersJson.has(internalId)) {
         JSONArray sellerInfo = offersJson.getJSONArray(internalId);
         // The Business logic is: if we have more than 1 seller is buy box
         boolean isBuyBox = sellerInfo.length() > 1;
         for (int i = 0; i < sellerInfo.length(); i++) {
            JSONObject info = (JSONObject) sellerInfo.get(i);

            if (info.has("sellerName") && !info.isNull("sellerName") && info.has("id") && !info.isNull("id")) {
               String name = info.get("sellerName").toString();
               String internalSellerId = info.get("id").toString();
               Integer mainPagePosition = i == 0 ? 1 : null;
               Integer sellersPagePosition = i == 0 ? 1 : null;

               if (i > 0 && name.equalsIgnoreCase("b2w")) {
                  sellersPagePosition = 1;
                  twoPositions = true;
               }
               Pricing pricing = scrapPricing(info, i, internalSellerId, mapOfSellerIdAndPrice, false);

               Offer offer = Offer.OfferBuilder.create()
                  .setInternalSellerId(internalSellerId)
                  .setSellerFullName(name)
                  .setMainPagePosition(mainPagePosition)
                  .setSellersPagePosition(sellersPagePosition)
                  .setPricing(pricing)
                  .setIsBuybox(isBuyBox)
                  .setIsMainRetailer(false)
                  .build();

               offers.add(offer);
            }
         }
      }
   }


   private void setOffersForSellersPage(Offers offers, Elements sellers) throws MalformedPricingException, OfferException {

      if(sellers.size() > 0){

         for(int i = 0; i < sellers.size(); i++){
            Element sellerInfo = sellers.get(i);
            boolean isBuyBox = sellers.size() > 1;
            String sellerName = CrawlerUtils.scrapStringSimpleInfo(sellerInfo,".seller-card__SellerInfo-zjlv7o-2 p:nth-child(2)",false);
            String rawSellerId = CrawlerUtils.scrapStringSimpleInfoByAttribute(sellerInfo,".seller-card__ButtonBox-zjlv7o-4 a","href");
            String sellerId = scrapSellerIdFromURL(rawSellerId);
            Integer mainPagePosition = i == 0 ? 1: null;
            Integer sellersPagePosition = i + 1;
            Pricing pricing = scrapPricingForOffersPage(sellerInfo);

            Offer offer = Offer.OfferBuilder.create()
               .setInternalSellerId(sellerId)
               .setSellerFullName(sellerName)
               .setMainPagePosition(mainPagePosition)
               .setSellersPagePosition(sellersPagePosition)
               .setPricing(pricing)
               .setIsBuybox(isBuyBox)
               .setIsMainRetailer(false)
               .build();

            offers.add(offer);
         }
      }
   }

   private String scrapSellerIdFromURL(String rawSellerId){
      String sellerId = "";
      if(rawSellerId != null){
         sellerId = CommonMethods.getLast(rawSellerId.split("sellerId")).replaceAll("[^0-9]","").trim();
      }
      return sellerId;
   }

   private Pricing scrapPricingForOffersPage(Element sellerInfo)
      throws MalformedPricingException {

      Double priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(sellerInfo, ".best-discount__ListPrice-sc-1xaobkm-1",null,false,',', session);
      Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(sellerInfo,".src__BestPrice-sc-1jnodg3-4",null,false,',', session);
      BankSlip bt = CrawlerUtils.setBankSlipOffers(spotlightPrice, null);
      CreditCards creditCards = scrapCreditCardsForSellersPage(sellerInfo,spotlightPrice);

      return Pricing.PricingBuilder.create()
         .setPriceFrom(priceFrom)
         .setSpotlightPrice(spotlightPrice)
         .setCreditCards(creditCards)
         .setBankSlip(bt)
         .build();
   }

   private CreditCards scrapCreditCardsForSellersPage(Element sellerInfo, Double spotlightPrice) throws MalformedPricingException {
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

}
