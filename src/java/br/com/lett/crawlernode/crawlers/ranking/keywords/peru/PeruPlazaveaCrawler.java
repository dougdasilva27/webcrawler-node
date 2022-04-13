package br.com.lett.crawlernode.crawlers.ranking.keywords.peru;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.util.JSONUtils;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PeruPlazaveaCrawler extends CrawlerRankingKeywords {

  public PeruPlazaveaCrawler(Session session) {
    super(session);
  }

   private String vtexSegment = getVtexSegment();
   private String homePage = "www.plazavea.com.pe";

   @Override
   protected void processBeforeFetch() {
      super.processBeforeFetch();

      if (!vtexSegment.equals("")) {
         BasicClientCookie cookie = new BasicClientCookie("vtex_segment", vtexSegment);
         cookie.setDomain(homePage.replace("https://", "").replace("/", ""));
         cookie.setPath("/");
         this.cookies.add(cookie);

         cookie = new BasicClientCookie("janus_sid", "a32797c0-132c-4848-960a-3296e41ea904");
         cookie.setDomain(homePage.replace("https://", "").replace("/", ""));
         cookie.setPath("/");
         this.cookies.add(cookie);
      }
   }

   private String getVtexSegment() {
      return "eyJjYW1wYWlnbnMiOm51bGwsImNoYW5uZWwiOiIxIiwicHJpY2VUYWJsZXMiOm51bGwsInJlZ2lvbklkIjpudWxsLCJ1dG1fY2FtcGFpZ24iOm51bGwsInV0bV9zb3VyY2UiOm51bGwsInV0bWlfY2FtcGFpZ24iOm51bGwsImN1cnJlbmN5Q29kZSI6IlBFTiIsImN1cnJlbmN5U3ltYm9sIjoiUy8iLCJjb3VudHJ5Q29kZSI6IlBFUiIsImN1bHR1cmVJbmZvIjoiZXMtUEUiLCJjaGFubmVsUHJpdmFjeSI6InB1YmxpYyJ9";
   }

   @Override
  protected void extractProductsFromCurrentPage() {
    this.pageSize = 24;
    this.log("Página " + this.currentPage);

    String url = "https://www.plazavea.com.pe/Busca/?PS=20&cc=24&sm=0&PageNumber=" +this.currentPage +"&O=OrderByScoreDESC&ft=" + this.keywordEncoded;
    String urlApi = "https://www.plazavea.com.pe/api/io/_v/public/graphql/v1?workspace=master";

    this.log("Link onde são feitos os crawlers: " + urlApi);

    JSONArray searchJson = crawlSearchApi(urlApi);

    this.currentDoc = fetchDocument(url);
    Elements products = this.currentDoc.select("li[layout] .g-producto[data-prod]");

    if (!products.isEmpty()) {
      if (this.totalProducts == 0) {
        setTotalProducts();
      }
      for (Element e : products) {

        String productPid = e.attr("data-prod");
        String productUrl = CrawlerUtils.scrapUrl(e, "a.Showcase__name", "href", "https", "www.plazavea.com.pe");

        saveDataProduct(null, productPid, productUrl);

        if (this.arrayProducts.size() == productsLimit)
          break;

      }
    } else {
      this.result = false;
      this.log("Keyword sem resultado!");
    }

    this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");
  }


  private JSONArray crawlSearchApi(String urlApi) {

     Map<String, String> headers = new HashMap<>();
     headers.put("Content-Type", "application/json");
//     String payload = "{\"query\":\"query productSearch($fullText: String, $selectedFacets: [SelectedFacetInput], $from: Int, $to: Int, $orderBy: String) {\\n    productSearch(fullText: $fullText, selectedFacets: $selectedFacets, from: $from, to: $to, orderBy: $orderBy, hideUnavailableItems: true, productOriginVtex:true) @context(provider: \\\"vtex.search-graphql\\\") {\\n      products {\\n        cacheId\\n        productId\\n        categoryId\\n        description\\n        productName\\n        properties {\\n          name\\n          values\\n        }\\n        categoryTree{\\n          id\\n          name\\n          href\\n          hasChildren\\n          children {\\n            id\\n            name\\n            href\\n          }\\n        }\\n        linkText\\n        brand\\n        link\\n        clusterHighlights {\\n          id\\n          name\\n        }\\n        skuSpecifications {\\n          field {\\n            name\\n          }\\n          values {\\n            name\\n          }\\n        }\\n        items {\\n          itemId\\n          name\\n          nameComplete\\n          complementName\\n          ean\\n          referenceId {\\n            Key\\n            Value\\n          }\\n          measurementUnit\\n          unitMultiplier\\n          images {\\n            cacheId\\n            imageId\\n            imageLabel\\n            imageTag\\n            imageUrl\\n            imageText\\n          }\\n          sellers {\\n            sellerId\\n            sellerName\\n            addToCartLink\\n            commertialOffer {\\n              discountHighlights {\\n                name\\n              }\\n              Price\\n              ListPrice\\n              Tax\\n              taxPercentage\\n              spotPrice\\n              PriceWithoutDiscount\\n              RewardValue\\n              PriceValidUntil\\n              AvailableQuantity\\n              giftSkuIds\\n              teasers {\\n                name\\n                conditions {\\n                  minimumQuantity\\n                  parameters {\\n                    name\\n                    value\\n                  }\\n                }\\n                effects {\\n                  parameters {\\n                    name\\n                    value\\n                  }\\n                }\\n              }\\n            }\\n          }\\n          variations{\\n            name\\n            values\\n          }\\n        }\\n        productClusters{\\n          id\\n          name\\n        }\\n        itemMetadata {\\n          items {\\n            id\\n            assemblyOptions {\\n              name\\n              required\\n            }\\n          }\\n        }\\n      }\\n      redirect\\n      recordsFiltered\\n      operator\\n      fuzzy\\n      correction {\\n        misspelled\\n      }\\n    }\\n  }\","
//        + "\"variables\":{\"fullText\":\"" + this.keywordEncoded + "\",\"selectedFacets\":[],\"from\":" + (this.currentPage - 1) * this.pageSize + ",\"to\":" + ((this.currentPage * this.pageSize) - 1) +  ",\"orderBy\":\"\"}}";
     String payload = "{\"query\":\"query productSearch($fullText: String, $selectedFacets: [SelectedFacetInput], $from: Int, $to: Int, $orderBy: String) {    productSearch(fullText: $fullText, selectedFacets: $selectedFacets, from: $from, to: $to, orderBy: $orderBy, hideUnavailableItems: true, productOriginVtex:true) @context(provider: \\\"vtex.search-graphql\\\") {      products {        cacheId        productId        categoryId        description        productName        properties {          name          values        }        categoryTree{          id          name          href          hasChildren          children {            id            name            href          }        }        linkText        brand        link        clusterHighlights {          id          name        }        skuSpecifications {          field {            name          }          values {            name          }        }        items {          itemId          name          nameComplete          complementName          ean          referenceId {            Key            Value          }          measurementUnit          unitMultiplier          images {            cacheId            imageId            imageLabel            imageTag            imageUrl            imageText          }          sellers {            sellerId            sellerName            addToCartLink            commertialOffer {              discountHighlights {                name              }              Price              ListPrice              Tax              taxPercentage              spotPrice              PriceWithoutDiscount              RewardValue              PriceValidUntil              AvailableQuantity              giftSkuIds              teasers {                name                conditions {                  minimumQuantity                  parameters {                    name                    value                  }                }                effects {                  parameters {                    name                    value                  }                }              }            }          }          variations{            name            values          }        }        productClusters{          id          name        }        itemMetadata {          items {            id            assemblyOptions {              name              required            }          }        }      }      redirect      recordsFiltered      operator      fuzzy      correction {        misspelled      }    }  }\",\"variables\":{\"fullText\":\"vino\",\"selectedFacets\":[],\"from\":0,\"to\":23,\"orderBy\":\"\"}}";

     Request request = Request.RequestBuilder.create()
        .setUrl(urlApi)
        .mustSendContentEncoding(false)
        .setHeaders(headers)
        .setPayload(payload)
        .build();

     Response r = this.dataFetcher.post(session, request);
     String content = r.getBody();
     JSONObject contentJson =  CrawlerUtils.stringToJson(content);

     return JSONUtils.getValueRecursive(contentJson, "data.productSearch.products", ".", JSONArray.class, new JSONArray());
  }

  @Override
  protected void setTotalProducts() {
    Element totalElement = this.currentDoc.select(".resultado-busca-numero .value").first();

    if (totalElement != null) {
      String text = totalElement.ownText().replaceAll("[^0-9]", "").trim();

      if (!text.isEmpty()) {
        this.totalProducts = Integer.parseInt(text);
      }

      this.log("Total da busca: " + this.totalProducts);
    }
  }
}
