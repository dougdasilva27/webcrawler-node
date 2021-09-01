package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.methods.DataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.FetcherDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.FetcherOptions;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.B2WCrawler;
import br.com.lett.crawlernode.crawlers.extractionutils.core.SaopauloB2WCrawlersUtils;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.Offer;
import models.Offers;
import models.pricing.*;
import org.apache.http.cookie.Cookie;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.*;

public class SaopauloShoptimeCrawler extends B2WCrawler {

   private static final String HOME_PAGE = "https://www.shoptime.com.br/";
   private static final String MAIN_SELLER_NAME_LOWER = "shoptime";
   private static final String MAIN_SELLER_NAME_LOWER_FROM_HTML = "Shoptime";


   public SaopauloShoptimeCrawler(Session session) {
      super(session);
      super.subSellers = new ArrayList<>();
      super.sellerNameLower = MAIN_SELLER_NAME_LOWER;
      super.sellerNameLowerFromHTML = MAIN_SELLER_NAME_LOWER_FROM_HTML;
      super.config.setFetcher(FetchMode.JSOUP);
      super.homePage = HOME_PAGE;
   }

   @Override
   protected Offers scrapOffers(Document doc, String internalId, String internalPid, int arrayPosition) throws MalformedPricingException, OfferException {

      Offers offers = new Offers();

      JSONObject jsonSeller = CrawlerUtils.selectJsonFromHtml(doc, "script", "window.__PRELOADED_STATE__ =", ";", false, true);

      //offerId é responsavel por definir qual sellers vai aparecer na primeira posição da pagina de sellers.
      String offerId = JSONUtils.getValueRecursive(jsonSeller,"entities.offers."+ internalPid+".0.id",String.class);
      String offersPageUrl = "https://www.shoptime.com.br/parceiros/" + internalPid + "?offerId=" + offerId + "&productSku=" + internalId;

      Document offersDoc = acessOffersPage(offersPageUrl);

      if(offersDoc.select(".src__Background-rlbmr6-1 .src__Card-rlbmr6-3 > div").isEmpty()){
         JSONObject offersJson = SaopauloB2WCrawlersUtils.newWayToExtractJsonOffers(jsonSeller,internalPid,arrayPosition);
         setOffersForMainPageSeller(offers, offersJson, internalId);
      } else {
         Elements offersFromHTML = offersDoc.select(".src__Background-rlbmr6-1 .src__Card-rlbmr6-3 > div");
         setOffersForSellersPage(offers, offersFromHTML);
      }
      return offers;
   }

   protected Document acessOffersPage(String offersPageURL) {
      return Jsoup.parse(fetchPage(offersPageURL,this.dataFetcher,cookies,headers,session));
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
            String sellerName = CrawlerUtils.scrapStringSimpleInfo(sellerInfo,".seller-card__SellerInfo-sc-1752s9o-2  p:nth-child(2)",false);
            String rawSellerId = CrawlerUtils.scrapStringSimpleInfoByAttribute(sellerInfo,".seller-card__ButtonContainer-sc-1752s9o-5 a","href");
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

      Double priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(sellerInfo, ".src__ListPrice-sc-17hp6jc-2",null,false,',', session);
      Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(sellerInfo,".src__BestPrice-sc-17hp6jc-5",null,false,',', session);
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

   @Override
   protected Document fetch() {
      setHeaders();
      return Jsoup.parse(SaopauloAmericanasCrawler.fetchPage(session.getOriginalURL(), this.dataFetcher, cookies, headers, session));
   }

   @Override
   public void setHeaders() {
      super.headers.put("host", "www.shoptime.com.br");
      super.headers.put("sec-ch-ua", " \" Not A;Brand\";v=\"99\", \"Chromium\";v=\"90\", \"Google Chrome\";v=\"90\"");
      super.headers.put("sec-ch-ua-mobile", "?0");
      super.headers.put("upgrade-insecure-requests", "1");
      super.headers.put("accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,/;q=0.8,application/signed-exchange;v=b3;q=0.9");
      super.headers.put("sec-fetch-site", "none");
      super.headers.put("sec-fetch-mode", "navigate");
      super.headers.put("sec-fetch-user", "?1");
      super.headers.put("sec-fetch-dest", "document");
      super.headers.put("accept-language", "pt-BR,pt;q=0.9,en-US;q=0.8,en;q=0.7,es;q=0.6");
   }
}
