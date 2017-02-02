package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import java.util.HashMap;
import java.util.Map;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;

public class BrasilBifarmaCrawler extends CrawlerRankingKeywords{
	
	public BrasilBifarmaCrawler(Session session) {
		super(session);
	}

	private Map<String,String> headers = new HashMap<>();
	
	@Override
	protected void processBeforeFetch() {
		headers.put("Content-Type", "application/json");
	}
	
	@Override
	protected void extractProductsFromCurrentPage() {
		//número de produtos por página do market
		this.pageSize = 12;
	
		String keyword = this.location.replaceAll(" ", "-");
		
		this.log("Página "+ this.currentPage);
		
		String payload = "{\"termo_busca\": \""+ keyword +"\", \"id_departamento\": \"0\", "
				+ "\"ordenar\": \"Nenhum\", \"itens\": \"32\", "
				+ "\"pagina\": \""+ this.currentPage +"\", \"marcas\": \"\", \"filtros\": \"\"}";
		
		//monta a url com a keyword e a página
		String url = "http://www.bifarma.com.br/postbusca/Produtos";
		this.log("Link onde são feitos os crawlers: "+url);	
		
		//chama função de pegar a url
		this.currentDoc = Jsoup.parse(fetchStringPOST(url, payload, headers, null));

		Elements products =  this.currentDoc.select(".modo_tabela > div:not(.busca_404)");
		
		//se obter 1 ou mais links de produtos e essa página tiver resultado faça:
		if(products.size() >= 1) {	
			
			for(Element e : products) {
				// InternalPid
				String internalPid = crawlInternalPid(e);
				
				// InternalId
				String internalId = crawlInternalId(e);
				
				// Url do produto
				String urlProduct = crawlProductUrl(e);
				
				saveDataProduct(internalId, internalPid, urlProduct);
				
				this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + internalPid + " - Url: " + urlProduct);
				if(this.arrayProducts.size() == productsLimit) break;
			}
		} else {
			setTotalBusca();
			this.result = false;
			this.log("Keyword sem resultado!");
		}
	
		this.log("Finalizando Crawler de produtos da página "+this.currentPage+" - até agora "+this.arrayProducts.size()+" produtos crawleados");
		if(!hasNextPage()) setTotalBusca();
	}

	@Override
	protected boolean hasNextPage() {
		Element nextPage = this.currentDoc.select("a .bold.red").last();
		
		if(nextPage != null){
			String text = nextPage.text();
			
			if(text.contains("ltima")){
				return true;
			}
		}
			
		return false;
		
	}
	
	private String crawlInternalId(Element e){
		String internalId = null;
		Element inid = e.select("> div[id]").first();
		
		if(inid != null){
			String[] tokens = inid.attr("id").split("_");
			
			internalId = tokens[tokens.length-1];
		}
		
		return internalId;
	}
	
	private String crawlInternalPid(Element e){
		String internalPid = null;
		
		return internalPid;
	}
	
	private String crawlProductUrl(Element e){
		String urlProduct = null;
		Element urlElement = e.select(" > div > a").first();
		
		if(urlElement != null){
			String link = urlElement.attr("href");
			
			if(link.startsWith("http")){
				urlProduct = link;
			} else if(link.startsWith("/")){
				urlProduct = "http://www.bifarma.com.br" + link;
			} else {
				urlProduct = "http://www.bifarma.com.br/" + link;
			}
		}
		
		return urlProduct;
	}
}
