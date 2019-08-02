package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import org.json.JSONArray;
import org.json.JSONObject;
import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;

/**
 * Date: 31/07/2019
 * 
 * @author Joao Pedro
 *
 */
public class BrasilJcdistribuicaoCrawler extends CrawlerRankingKeywords {

  private static final String API_URL = "https://superon.lifeapps.com.br/api/v1/app";
  private static final String COMPANY_ID = "6f0ae38d-50cd-4873-89a5-6861467b5f52";
  private static final String APP_ID = "dccca000-b2ea-11e9-a27c-b76c91df9dd6cc64548c0cbad6cf58d4d3bbd433142b";
  private static final String FORMA_PAGAMENTO = "2001546a-9851-4393-bb68-7c04e932fa4c";
  private JSONArray search = new JSONArray();

  public BrasilJcdistribuicaoCrawler(Session session) {
    super(session);
    super.fetchMode = FetchMode.FETCHER;
  }

  @Override
  public void extractProductsFromCurrentPage() {
    this.pageSize = 20;
    this.log("Página " + this.currentPage);

    search = fetchProductsFromAPI();

    if (search.length() > 0) {

      for (int i = 0; i < search.length(); i++) {
        JSONObject product = search.getJSONObject(i);

        String internalPid = crawlInternalPid(product);
        String internalId = crawlInternalId(product);
        String productUrl = crawlProductUrl(product);

        saveDataProduct(internalId, internalPid, productUrl);

        this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + internalPid + " - Url: " + productUrl);

        if (this.arrayProducts.size() == productsLimit) {
          break;
        }
      }
    } else {
      this.result = false;
      this.log("Keyword sem resultado!");
    }

    this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");

  }

  @Override
  protected boolean hasNextPage() {
    return search.length() >= this.pageSize;
  }

  private String crawlInternalId(JSONObject json) {
    String internalId = null;

    if (json.has("id_produto_erp") && !json.isNull("id_produto_erp")) {
      String idProdutoErp = json.get("id_produto_erp").toString();

      if (idProdutoErp.contains("|")) {
        String[] idProdutoErpArray = idProdutoErp.split("\\|");
        internalId = idProdutoErpArray[1].concat("-").concat(idProdutoErpArray[0]);
      }
    }

    return internalId;
  }

  private String crawlInternalPid(JSONObject product) {
    String internalPid = null;

    if (product.has("idcadastroextraproduto") && !product.isNull("idcadastroextraproduto")) {
      internalPid = product.get("idcadastroextraproduto").toString();
    }

    return internalPid;
  }

  // url must be in this format:
  // https://jcdistribuicao.superon.app/commerce/6f0ae38d-50cd-4873-89a5-6861467b5f52/produto/WHISKY-JOHNNIE-WALKER-18-ANOS-750ML-1NawxZ3t/
  private String crawlProductUrl(JSONObject product) {
    String productUrl = null;
    String host = "https://jcdistribuicao.superon.app";

    if (product.has("slug") && !product.isNull("slug")) {
      String slug = product.get("slug").toString();
      productUrl = host;

      productUrl = productUrl
          .concat("/")
          .concat("commerce")
          .concat("/")
          .concat(COMPANY_ID)
          .concat("/")
          .concat("produto")
          .concat("/")
          .concat(slug);
    }


    return productUrl;
  }

  private JSONArray fetchProductsFromAPI() {
    JSONArray products = new JSONArray();
    String url = API_URL
        .concat("/")
        .concat(APP_ID)
        .concat("/")
        .concat("listaprodutosf")
        .concat("/")
        .concat(COMPANY_ID)
        .concat("?sk=")
        .concat(this.keywordEncoded)
        .concat("&page=")
        .concat(Integer.toString(this.currentPage - 1))
        .concat("&formapagamento=")
        .concat(FORMA_PAGAMENTO)
        .concat("&canalVenda=WEB");

    Request request = RequestBuilder.create()
        .setUrl(url)
        .setCookies(cookies)
        .mustSendContentEncoding(false)
        .build();

    JSONObject apiResponse = CrawlerUtils.stringToJson(this.dataFetcher.get(session, request).getBody());

    if (apiResponse.has("dados") && apiResponse.get("dados") instanceof JSONArray) {
      products = apiResponse.getJSONArray("dados");
    }

    return products;
  }
}
