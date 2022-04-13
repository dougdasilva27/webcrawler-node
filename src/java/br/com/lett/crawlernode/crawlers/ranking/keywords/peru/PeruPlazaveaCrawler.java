package br.com.lett.crawlernode.crawlers.ranking.keywords.peru;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.JSONUtils;
import br.com.lett.crawlernode.util.MathUtils;
import org.apache.http.HttpHeaders;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;

import java.util.*;

public class PeruPlazaveaCrawler extends CrawlerRankingKeywords {

   public PeruPlazaveaCrawler(Session session) {
      super(session);
      super.fetchMode = FetchMode.FETCHER;
   }

   private String vtexSegment = getVtexSegment();
   private String homePage = "www.plazavea.com.pe";

   @Override
   protected void processBeforeFetch() {
      BasicClientCookie cookie = new BasicClientCookie("vtex_segment", vtexSegment);
      cookie.setDomain(homePage);
      cookie.setPath("/");
      this.cookies.add(cookie);
   }


   private String getVtexSegment() {
      return "eyJjYW1wYWlnbnMiOm51bGwsImNoYW5uZWwiOiIxIiwicHJpY2VUYWJsZXMiOm51bGwsInJlZ2lvbklkIjpudWxsLCJ1dG1fY2FtcGFpZ24iOm51bGwsInV0bV9zb3VyY2UiOm51bGwsInV0bWlfY2FtcGFpZ24iOm51bGwsImN1cnJlbmN5Q29kZSI6IlBFTiIsImN1cnJlbmN5U3ltYm9sIjoiUy8iLCJjb3VudHJ5Q29kZSI6IlBFUiIsImN1bHR1cmVJbmZvIjoiZXMtUEUiLCJjaGFubmVsUHJpdmFjeSI6InB1YmxpYyJ9";
   }

   @Override
   protected void extractProductsFromCurrentPage() throws MalformedProductException {
      this.pageSize = 24;
      this.log("Página " + this.currentPage);

      String urlApi = "https://www.plazavea.com.pe/api/io/_v/public/graphql/v1?workspace=master";

      this.log("Link onde são feitos os crawlers: " + urlApi);

      JSONObject searchJson = crawlSearchApi(urlApi);

      if (this.totalProducts == 0) {
         this.totalProducts = JSONUtils.getValueRecursive(searchJson, "recordsFiltered", ".", Integer.class, 0);
         this.log("Total da busca: " + this.totalProducts);
      }

      JSONArray products = JSONUtils.getValueRecursive(searchJson, "products", ".", JSONArray.class, new JSONArray());

      if (!products.isEmpty()) {
         for (int i = 0; i < products.length(); i++) {

            JSONObject product = products.optJSONObject(i);

            String internalId = product.optString("productId");
            String internalPid = internalId;
            String linkText = product.optString("linkText");
            String productUrl = linkText != null ? "https://www.plazavea.com.pe/" + linkText + "/p" : null;
            String name = product.optString("productName");
            String imgUrl = JSONUtils.getValueRecursive(product, "items.0.images.0.imageUrl", ".", String.class, null);
            JSONObject commertialOffer = JSONUtils.getValueRecursive(product, "items.0.sellers.0.commertialOffer", ".", JSONObject.class, null);
            Double price = null;
            if (commertialOffer != null) {
               if (commertialOffer.opt("Price") instanceof Integer) {
                  price = ((Integer) commertialOffer.opt("Price")).doubleValue();
               } else {
                  price = commertialOffer.optDouble("Price");
               }
            }

            Integer priceInCents = price != null ? MathUtils.parseInt(price * 100) : 0;
            Integer availableQuantity = JSONUtils.getValueRecursive(product, "items.0.sellers.0.commertialOffer.AvailableQuantity", ".", Integer.class, null);
            boolean isAvailable = availableQuantity != null && availableQuantity > 0;

            RankingProduct productRanking = RankingProductBuilder.create()
               .setUrl(productUrl)
               .setInternalId(internalId)
               .setInternalPid(internalPid)
               .setImageUrl(imgUrl)
               .setName(name)
               .setPriceInCents(priceInCents)
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


   private JSONObject crawlSearchApi(String urlApi) {
      Map<String, String> headers = new HashMap<>();
      headers.put("Content-Type", "application/json");
      headers.put("Accept", "application/json");
      headers.put("Origin", "https://www.plazavea.com.pe/");
      headers.put("Referer", "https://www.plazavea.com.pe/");
      String payload = "{\"query\":\"query productSearch($fullText: String, $selectedFacets: [SelectedFacetInput], $from: Int, $to: Int, $orderBy: String) {\\n    productSearch(fullText: $fullText, selectedFacets: $selectedFacets, from: $from, to: $to, orderBy: $orderBy, hideUnavailableItems: true, productOriginVtex:true) @context(provider: \\\"vtex.search-graphql\\\") {\\n      products {\\n        cacheId\\n        productId\\n        categoryId\\n        description\\n        productName\\n        properties {\\n          name\\n          values\\n        }\\n        categoryTree{\\n          id\\n          name\\n          href\\n          hasChildren\\n          children {\\n            id\\n            name\\n            href\\n          }\\n        }\\n        linkText\\n        brand\\n        link\\n        clusterHighlights {\\n          id\\n          name\\n        }\\n        skuSpecifications {\\n          field {\\n            name\\n          }\\n          values {\\n            name\\n          }\\n        }\\n        items {\\n          itemId\\n          name\\n          nameComplete\\n          complementName\\n          ean\\n          referenceId {\\n            Key\\n            Value\\n          }\\n          measurementUnit\\n          unitMultiplier\\n          images {\\n            cacheId\\n            imageId\\n            imageLabel\\n            imageTag\\n            imageUrl\\n            imageText\\n          }\\n          sellers {\\n            sellerId\\n            sellerName\\n            addToCartLink\\n            commertialOffer {\\n              discountHighlights {\\n                name\\n              }\\n              Price\\n              ListPrice\\n              Tax\\n              taxPercentage\\n              spotPrice\\n              PriceWithoutDiscount\\n              RewardValue\\n              PriceValidUntil\\n              AvailableQuantity\\n              giftSkuIds\\n              teasers {\\n                name\\n                conditions {\\n                  minimumQuantity\\n                  parameters {\\n                    name\\n                    value\\n                  }\\n                }\\n                effects {\\n                  parameters {\\n                    name\\n                    value\\n                  }\\n                }\\n              }\\n            }\\n          }\\n          variations{\\n            name\\n            values\\n          }\\n        }\\n        productClusters{\\n          id\\n          name\\n        }\\n        itemMetadata {\\n          items {\\n            id\\n            assemblyOptions {\\n              name\\n              required\\n            }\\n          }\\n        }\\n      }\\n      redirect\\n      recordsFiltered\\n      operator\\n      fuzzy\\n      correction {\\n        misspelled\\n      }\\n    }\\n  }\",\"variables\":{\"fullText\":\"" + this.keywordEncoded + "\",\"selectedFacets\":[],\"from\":" + (this.currentPage - 1) * this.pageSize + ",\"to\":" + (this.currentPage * this.pageSize - 1)  + ",\"orderBy\":\"\"}}";

      Request request = Request.RequestBuilder.create()
         .setUrl(urlApi)
         .mustSendContentEncoding(false)
         .setHeaders(headers)
         .setPayload(payload)
         .setCookies(cookies)
         .build();

      Response r = this.dataFetcher.post(session, request);
      String content = r.getBody();

      int tries = 0;
      while(("".equals(content) || "{}".equals(content)) && tries < 3) {
         r = this.dataFetcher.post(session, request);
         content = r.getBody();
         tries++;
      }

      JSONObject contentJson = CrawlerUtils.stringToJson(content);

      return JSONUtils.getValueRecursive(contentJson, "data.productSearch", ".", JSONObject.class, new JSONObject());
   }

}
