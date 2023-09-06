package br.com.lett.crawlernode.crawlers.ranking.keywords.mexico;

import br.com.lett.crawlernode.core.fetcher.methods.ApacheDataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.JsoupDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.WalmartSuperCrawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import com.google.common.net.HttpHeaders;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.cookie.BasicClientCookie;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MexicoBodegaaurreraCrawler extends WalmartSuperCrawler {

   public MexicoBodegaaurreraCrawler(Session session) {
      super(session);
      super.HOME_PAGE = "https://www.bodegaaurrera.com.mx";
   }

   @Override
   protected void processBeforeFetch() {
      Map<String, String> headers = new HashMap<>();
      headers.put(HttpHeaders.ACCEPT, "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7");
      headers.put(HttpHeaders.ORIGIN, HOME_PAGE);
      headers.put(HttpHeaders.REFERER, HOME_PAGE);
      headers.put(HttpHeaders.CONTENT_TYPE, "application/json");
      headers.put("accept-language", "en-US,en;q=0.9,pt;q=0.8,pt-PT;q=0.7");

      String payload = "{\"query\":\"mutation MergeAndGetCart( $input:MergeAndGetCartInput! $detailed:Boolean! $includeExtras:Boolean! = false $includePartialFulfillmentSwitching:Boolean! = false $enableAEBadge:Boolean! = false $enableBadges:Boolean! = false $includeQueueing:Boolean! = false $includeExpressSla:Boolean! = false $enableACCScheduling:Boolean! = false $includeWeeklyReservation:Boolean! = false $enableWalmartPlusFreeDiscountedExpress:Boolean! = false $enableWalmartPlusFreeDiscountedExpressProgram:Boolean! = false $enableCartBookslotShortcut:Boolean! = false $enableFutureInventoryCartBookslot:Boolean! = false $enableWeightedItems:Boolean! = false $includeOtherDetailed:Boolean! = true $enableExpressReservationEndTime:Boolean! = false $enableBenefitSavings:Boolean! = false $enableUnifiedBadges:Boolean! = false $enableCartLevelMSI:Boolean! = false $enableIntentControl:Boolean! = false $enableReturnsLabel:Boolean! = false $enableStarRatings:Boolean! = false $enableSpendLimit:Boolean! = false $includeGrandAndSavedSubtotal:Boolean! = false $enablePickupNotAvailable:Boolean! = false $enableMsiMci:Boolean! = false $includeClipRewards:Boolean! = false $enableI18nWave1:Boolean = false $enableWplusPetBenefit:Boolean! = false $enableCartLevelPromotions:Boolean! = false $enablePetRxManualRefill:Boolean! = false $enableLocalizedStringForReservation:Boolean! = false $enableOrderCutOffTime:Boolean! = false $enableHotCartFeature:Boolean! = false $enableSuggestedSlotAvailability:Boolean! = true $enablePFS:Boolean! = false $enableTaxBreakdown:Boolean! = false $enableSubscriptionsInTransaction:Boolean! = false $enableSubscriptionDiscounts:Boolean! = false $enablePromoDiscount:Boolean! = false ){mergeAndGetCart(input:$input){id checkoutable itemFulfillmentTypes fulfillmentMode @include(if:$enableHotCartFeature) showShipFreeForPickupUnavailable @include(if:$enablePickupNotAvailable) installmentDetails @include(if:$enableCartLevelMSI){bankInstallmentOptions{bank installments payments{displayValue value}minAmount{displayValue value}currencyUnit}}basketSwitch @include(if:$enableIntentControl){collapsed switchOptions{fulfillmentOption itemIds switchableQuantity selected}}customer{id isGuest}cartGiftingDetails{isGiftOrder hasGiftEligibleItem isAddOnServiceAdjustmentNeeded isWalmartProtectionPlanPresent isAppleCarePresent}selectedACCInstallationPackage @include(if:$enablePFS){offerId quantity groupType isExplicitPackageSelection name pricePerTire linePrice bundleComponents{offerId quantity}}addressMode lineItems{id quantity quantityString quantityLabel orderedSalesUnit rxDetails{profileId prescriptionId isPrescriptionRequired itemRXType}hasShippingRestriction @include(if:$detailed) isGiftEligible @include(if:$detailed) createdDateTime isWplusEarlyAccess isEventActive isSubstitutionSelected eventType shippingDeliveryDate @include(if:$enablePickupNotAvailable) petItemType @include(if:$enablePetRxManualRefill) refillPrescriptionDetails @include(if:$enablePetRxManualRefill){petName prescribedQShow more";

      Request request = Request.RequestBuilder.create()
         .setUrl(HOME_PAGE + "/orchestra/graphql")
         .setPayload(payload)
         .setHeaders(headers)
         .setProxyservice(PROXIES)
         .build();
      Response response = CrawlerUtils.retryRequestWithListDataFetcher(request, List.of(new ApacheDataFetcher(), new JsoupDataFetcher()), session, "post");

      for (Cookie cookie : response.getCookies()) {
         BasicClientCookie basicClientCookie = new BasicClientCookie(cookie.getName(), cookie.getValue());
         basicClientCookie.setDomain(".bodegaaurrera.com.mx");
         this.cookies.add(basicClientCookie);
      }
   }
}
