package br.com.lett.crawlernode.crawlers.ranking.keywords.unitedstates;

import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.methods.JsoupDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UnitedStatesTheHomeDepotCrawler extends CrawlerRankingKeywords {
   private static final String HOME_PAGE = "https://www.homedepot.com/";
   private String ZIPCODE = session.getOptions().optString("zipcode");
   private String STORE_ID = session.getOptions().optString("storeId");
   private Integer pageCount = 0;
   private String category = null;
   private String API_URL = "https://www.homedepot.com/federation-gateway/graphql?opname=searchModel";
   public UnitedStatesTheHomeDepotCrawler(Session session) {
      super(session);
   }

   protected JSONObject fetch(String url) {
      String payload = null;
      String keyword = null;
      String navParam = null;

      if (category != null) {
         navParam = category;
      } else {
         keyword = this.keywordEncoded;
      }

      payload = "{\"operationName\":\"searchModel\"," +
         "\"variables\":{\"skipInstallServices\":false,\"skipKPF\":false,\"skipSpecificationGroup\":false,\"skipSubscribeAndSave\":false,\"storefilter\":\"ALL\",\"channel\":\"DESKTOP\",\"additionalSearchParams\":{\"sponsored\":true,\"deliveryZip\":\"" + this.ZIPCODE + "\"}," +
         "\"filter\":{},\"keyword\":\""+ keyword + "\", \"navParam\":\"" + navParam + "\",\"orderBy\":{\"field\":\"TOP_SELLERS\",\"order\":\"ASC\"},\"pageSize\":24,\"startIndex\":" + this.pageCount + ",\"storeId\":\"" + this.STORE_ID + "\"}," +
         "\"query\":\"query searchModel($storeId: String, $zipCode: String, $skipInstallServices: Boolean = true, $startIndex: Int, $pageSize: Int, $orderBy: ProductSort, $filter: ProductFilter, $skipKPF: Boolean = false, $skipSpecificationGroup: Boolean = false, $skipSubscribeAndSave: Boolean = false, $keyword: String, $navParam: String, $storefilter: StoreFilter = ALL, $itemIds: [String], $channel: Channel = DESKTOP, $additionalSearchParams: AdditionalParams, $loyaltyMembershipInput: LoyaltyMembershipInput) {\\n  searchModel(keyword: $keyword, navParam: $navParam, storefilter: $storefilter, storeId: $storeId, itemIds: $itemIds, channel: $channel, additionalSearchParams: $additionalSearchParams, loyaltyMembershipInput: $loyaltyMembershipInput) {\\n    metadata {\\n      hasPLPBanner\\n      categoryID\\n      analytics {\\n        semanticTokens\\n        dynamicLCA\\n        __typename\\n      }\\n      canonicalUrl\\n      searchRedirect\\n      clearAllRefinementsURL\\n      contentType\\n      isStoreDisplay\\n      productCount {\\n        inStore\\n        __typename\\n      }\\n      stores {\\n        storeId\\n        storeName\\n        address {\\n          postalCode\\n          __typename\\n        }\\n        nearByStores {\\n          storeId\\n          storeName\\n          distance\\n          address {\\n            postalCode\\n            __typename\\n          }\\n          __typename\\n        }\\n        __typename\\n      }\\n      __typename\\n    }\\n    products(startIndex: $startIndex, pageSize: $pageSize, orderBy: $orderBy, filter: $filter) {\\n      identifiers {\\n        storeSkuNumber\\n        canonicalUrl\\n        brandName\\n        itemId\\n        productLabel\\n        modelNumber\\n        productType\\n        parentId\\n        isSuperSku\\n        __typename\\n      }\\n      installServices(storeId: $storeId, zipCode: $zipCode) @skip(if: $skipInstallServices) {\\n        scheduleAMeasure\\n        gccCarpetDesignAndOrderEligible\\n        __typename\\n      }\\n      itemId\\n      dataSources\\n      media {\\n        images {\\n          url\\n          type\\n          subType\\n          sizes\\n          __typename\\n        }\\n        __typename\\n      }\\n      pricing(storeId: $storeId) {\\n        value\\n        alternatePriceDisplay\\n        alternate {\\n          bulk {\\n            pricePerUnit\\n            thresholdQuantity\\n            value\\n            __typename\\n          }\\n          unit {\\n            caseUnitOfMeasure\\n            unitsOriginalPrice\\n            unitsPerCase\\n            value\\n            __typename\\n          }\\n          __typename\\n        }\\n        original\\n        mapAboveOriginalPrice\\n        message\\n        preferredPriceFlag\\n        promotion {\\n          type\\n          description {\\n            shortDesc\\n            longDesc\\n            __typename\\n          }\\n          dollarOff\\n          percentageOff\\n          savingsCenter\\n          savingsCenterPromos\\n          specialBuySavings\\n          specialBuyDollarOff\\n          specialBuyPercentageOff\\n          dates {\\n            start\\n            end\\n            __typename\\n          }\\n          __typename\\n        }\\n        specialBuy\\n        unitOfMeasure\\n        __typename\\n      }\\n      reviews {\\n        ratingsReviews {\\n          averageRating\\n          totalReviews\\n          __typename\\n        }\\n        __typename\\n      }\\n      availabilityType {\\n        discontinued\\n        type\\n        __typename\\n      }\\n      badges(storeId: $storeId) {\\n        name\\n        __typename\\n      }\\n      details {\\n        collection {\\n          collectionId\\n          name\\n          url\\n          __typename\\n        }\\n        __typename\\n      }\\n      favoriteDetail {\\n        count\\n        __typename\\n      }\\n      fulfillment(storeId: $storeId, zipCode: $zipCode) {\\n        backordered\\n        backorderedShipDate\\n        bossExcludedShipStates\\n        excludedShipStates\\n        seasonStatusEligible\\n        fulfillmentOptions {\\n          type\\n          fulfillable\\n          services {\\n            type\\n            hasFreeShipping\\n            freeDeliveryThreshold\\n            locations {\\n              curbsidePickupFlag\\n              isBuyInStoreCheckNearBy\\n              distance\\n              inventory {\\n                isOutOfStock\\n                isInStock\\n                isLimitedQuantity\\n                isUnavailable\\n                quantity\\n                maxAllowedBopisQty\\n                minAllowedBopisQty\\n                __typename\\n              }\\n              isAnchor\\n              locationId\\n              storeName\\n              state\\n              type\\n              __typename\\n            }\\n            __typename\\n          }\\n          __typename\\n        }\\n        __typename\\n      }\\n      info {\\n        hasSubscription\\n        isBuryProduct\\n        isSponsored\\n        isGenericProduct\\n        isLiveGoodsProduct\\n        sponsoredBeacon {\\n          onClickBeacon\\n          onViewBeacon\\n          __typename\\n        }\\n        sponsoredMetadata {\\n          campaignId\\n          placementId\\n          slotId\\n          __typename\\n        }\\n        globalCustomConfigurator {\\n          customExperience\\n          __typename\\n        }\\n        returnable\\n        hidePrice\\n        productSubType {\\n          name\\n          link\\n          __typename\\n        }\\n        categoryHierarchy\\n        samplesAvailable\\n        customerSignal {\\n          previouslyPurchased\\n          __typename\\n        }\\n        productDepartmentId\\n        productDepartment\\n        augmentedReality\\n        ecoRebate\\n        quantityLimit\\n        sskMin\\n        sskMax\\n        unitOfMeasureCoverage\\n        wasMaxPriceRange\\n        wasMinPriceRange\\n        swatches {\\n          isSelected\\n          itemId\\n          label\\n          swatchImgUrl\\n          url\\n          value\\n          __typename\\n        }\\n        totalNumberOfOptions\\n        paintBrand\\n        dotComColorEligible\\n        __typename\\n      }\\n      keyProductFeatures @skip(if: $skipKPF) {\\n        keyProductFeaturesItems {\\n          features {\\n            name\\n            refinementId\\n            refinementUrl\\n            value\\n            __typename\\n          }\\n          __typename\\n        }\\n        __typename\\n      }\\n      specificationGroup @skip(if: $skipSpecificationGroup) {\\n        specifications {\\n          specName\\n          specValue\\n          __typename\\n        }\\n        specTitle\\n        __typename\\n      }\\n      subscription @skip(if: $skipSubscribeAndSave) {\\n        defaultfrequency\\n        discountPercentage\\n        subscriptionEnabled\\n        __typename\\n      }\\n      sizeAndFitDetail {\\n        attributeGroups {\\n          attributes {\\n            attributeName\\n            dimensions\\n            __typename\\n          }\\n          dimensionLabel\\n          productType\\n          __typename\\n        }\\n        __typename\\n      }\\n      __typename\\n    }\\n    id\\n    searchReport {\\n      totalProducts\\n      didYouMean\\n      correctedKeyword\\n      keyword\\n      pageSize\\n      searchUrl\\n      sortBy\\n      sortOrder\\n      startIndex\\n      __typename\\n    }\\n    relatedResults {\\n      universalSearch {\\n        title\\n        __typename\\n      }\\n      relatedServices {\\n        label\\n        __typename\\n      }\\n      visualNavs {\\n        label\\n        imageId\\n        webUrl\\n        categoryId\\n        imageURL\\n        __typename\\n      }\\n      visualNavContainsEvents\\n      relatedKeywords {\\n        keyword\\n        __typename\\n      }\\n      __typename\\n    }\\n    taxonomy {\\n      brandLinkUrl\\n      breadCrumbs {\\n        browseUrl\\n        creativeIconUrl\\n        deselectUrl\\n        dimensionId\\n        dimensionName\\n        label\\n        refinementKey\\n        url\\n        __typename\\n      }\\n      __typename\\n    }\\n    templates\\n    partialTemplates\\n    dimensions {\\n      label\\n      refinements {\\n        refinementKey\\n        label\\n        recordCount\\n        selected\\n        imgUrl\\n        url\\n        nestedRefinements {\\n          label\\n          url\\n          recordCount\\n          refinementKey\\n          __typename\\n        }\\n        __typename\\n      }\\n      collapse\\n      dimensionId\\n      isVisualNav\\n      isVisualDimension\\n      nestedRefinementsLimit\\n      visualNavSequence\\n      __typename\\n    }\\n    orangeGraph {\\n      universalSearchArray {\\n        pods {\\n          title\\n          description\\n          imageUrl\\n          link\\n          recordType\\n          __typename\\n        }\\n        info {\\n          title\\n          __typename\\n        }\\n        __typename\\n      }\\n      productTypes\\n      intents\\n      orderNumber\\n      __typename\\n    }\\n    appliedDimensions {\\n      label\\n      refinements {\\n        label\\n        refinementKey\\n        url\\n        __typename\\n      }\\n      __typename\\n    }\\n    __typename\\n  }\\n}\\n\"}";

      Map<String, String> headers = new HashMap<>();
      headers.put("cookie", CommonMethods.cookiesToString(this.cookies));
      headers.put("authority", "www.homedepot.com");
      headers.put("referer", session.getOriginalURL());
      headers.put("origin", HOME_PAGE);
      headers.put("accept", "*/*");
      headers.put("content-type", "application/json");
      headers.put("x-debug", "false");
      headers.put("x-experience-name", "general-merchandise");
      headers.put("x-hd-dc", "origin");

      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .setHeaders(headers)
         .setCookies(cookies)
         .setPayload(payload)
         .setProxyservice(List.of(ProxyCollection.NETNUT_RESIDENTIAL_US_HAPROXY, ProxyCollection.NETNUT_RESIDENTIAL_MX_HAPROXY))
         .build();

      Response response = this.dataFetcher.post(session, request);

      if (!response.isSuccess()) {
         response = new JsoupDataFetcher().get(session, request);

         if (!response.isSuccess()) {
            int tries = 0;
            while (!response.isSuccess() && tries < 3) {
               tries++;
               if (tries % 2 == 0) {
                  response = new JsoupDataFetcher().post(session, request);
               } else {
                  response = this.dataFetcher.post(session, request);
               }
            }
         }
      }

      return CrawlerUtils.stringToJson(response.getBody());
   }

   @Override
   protected void extractProductsFromCurrentPage() throws MalformedProductException {
      JSONObject json = fetch(API_URL);

      json = isCategoryPage(json);

      this.pageCount += 24;

      JSONArray productsJson = JSONUtils.getValueRecursive(json, "data.searchModel.products", JSONArray.class);

      if (productsJson != null && !productsJson.isEmpty()) {
         if (this.totalProducts == 0) {
            setTotalProducts();
         }

         for (Object product : productsJson) {
            String internalPid = JSONUtils.getValueRecursive(product, "identifiers.itemId", String.class);
            String name = JSONUtils.getValueRecursive(product, "identifiers.productLabel", String.class);
            String productUrl =  "https://www.homedepot.com" + JSONUtils.getValueRecursive(product, "identifiers.canonicalUrl", String.class);
            String imageUrl = scrapImageUrl(product);
            int price = scrapPricing(product);
            boolean isAvailable = price != 0;

            RankingProduct productRanking = RankingProductBuilder.create()
               .setUrl(productUrl)
               .setInternalId(internalPid)
               .setName(name)
               .setImageUrl(imageUrl)
               .setPriceInCents(price)
               .setAvailability(isAvailable)
               .build();

            saveDataProduct(productRanking);
            if (this.arrayProducts.size() == productsLimit)
               break;
         }
      } else {
         this.result = false;
         this.log("Keyword sem resultado!");
      }

      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");
   }

   protected String scrapImageUrl(Object productJson) {
      JSONArray images = JSONUtils.getValueRecursive(productJson, "media.images", JSONArray.class);

      if (images != null && images.length() > 1) {
         Object imageOptions = images.get(0);
         String primaryImageUrl = JSONUtils.getStringValue((JSONObject) imageOptions, "url");

         if (primaryImageUrl != null) {
            primaryImageUrl = primaryImageUrl.replaceAll("<SIZE>", "1000");
            return primaryImageUrl;
         }
      }

      return null;
   }

   protected Integer scrapPricing(Object productJson) {
      Double priceDouble = JSONUtils.getValueRecursive(productJson, "pricing.value", Double.class);

      if (priceDouble != null) {
         priceDouble = priceDouble * 100;
         return priceDouble.intValue();
      }

      return null;
   }
   protected void setTotalProducts(JSONArray productsJson) {
      this.totalProducts = productsJson.length();
      this.log("Total da busca: " + this.totalProducts);
   }

   @Override
   protected boolean hasNextPage(){
      return true;
   }

   private JSONObject isCategoryPage(JSONObject json) {
      if (json != null) {
         String redirectUrl = JSONUtils.getValueRecursive(json, "data.searchModel.metadata.searchRedirect", String.class);

         if (redirectUrl == null) return json;

         Pattern pattern = Pattern.compile("(?<=N-)(.*?)(?=\\?)");
         Matcher matcher = pattern.matcher(redirectUrl);

         if (matcher.find()) {
            redirectUrl = matcher.group(1);
         }

         this.category = redirectUrl;

         JSONObject categoryRequest = fetch(API_URL);

         return categoryRequest;
      }

      return null;
   }
}
