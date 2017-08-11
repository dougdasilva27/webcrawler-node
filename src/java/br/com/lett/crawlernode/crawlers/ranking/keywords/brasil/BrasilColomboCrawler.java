package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;

public class BrasilColomboCrawler extends CrawlerRankingKeywords{

	public BrasilColomboCrawler(Session session) {
		super(session);
	}

	@Override
	protected void extractProductsFromCurrentPage() {
		//número de produtos por página do market
		this.pageSize = 20;
	
		this.log("Página "+ this.currentPage);
		
		//monta a url com a keyword e a página
		String url = "http://busca.colombo.com.br/search?televendas=&p=Q&srid=S12-USESD02&lbc=colombo&ts=ajax&w="+this.keywordEncoded
				+"&uid=687778702&method=and&isort=score&view=grid&srt="+this.arrayProducts.size();
		this.log("Link onde são feitos os crawlers: "+url);	
		
		//chama função de pegar a url
		this.currentDoc = fetchDocument(url);

		Elements id =  this.currentDoc.select("div.produto-info-content ");		
		
		//se obter 1 ou mais links de produtos e essa página tiver resultado faça:
		if(id.size() >= 1) {			
			for(Element e : id) {				
				// InternalPid
				String internalPid 	= crawlInternalPid(e);
				
				// InternalId
				String internalId 	= crawlInternalId(e);
				
				// Url do produto
				String urlProduct = crawlProductUrl(e);
				
				saveDataProduct(internalId, internalPid, urlProduct);
				
				this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + internalPid + " - Url: " + urlProduct);
				if(this.arrayProducts.size() == productsLimit) break;
				
			}
		} else {
			this.result = false;
			this.log("Keyword sem resultado!");
		}
	
		this.log("Finalizando Crawler de produtos da página "+this.currentPage+" - até agora "+this.arrayProducts.size()+" produtos crawleados");
	}

	@Override
	protected boolean hasNextPage() {
		Elements page = this.currentDoc.select("div.produto-info-content span.produtoDescPreco > a");
		
		//se  elemeno page obtiver algum resultado
		if(page.size() >= 20)
		{
			//tem próxima página
			return true;
		}
		else
		{
			//não tem próxima página
			return false;
		}
	}
	

	private String crawlInternalId(Element e){
		String internalId = null;
		
		return internalId;
	}
	
	private String crawlInternalPid(Element e){
		String internalPid = null;
		Element pid = e.select("span.produtoDescPreco > a > span.valor").first();
		
		if(pid != null){
			internalPid 	= pid.attr("id").replaceAll("[^0-9]", "").trim();
		}
		
		return internalPid;
	}
	
	private String crawlProductUrl(Element e){
		String urlProduct = null;
		Element urlElement = e.select(".produto-descricao a").first();
		
		if(urlElement != null){
			urlProduct = urlElement.attr("title");
			
			if(!urlProduct.contains("colombo")){
				urlProduct = "http://www.colombo.com.br/" + urlElement.attr("title");
			}
		}
		
		return urlProduct;
	}
}
