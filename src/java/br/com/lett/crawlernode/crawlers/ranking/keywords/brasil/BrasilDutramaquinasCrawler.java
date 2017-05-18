package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;

public class BrasilDutramaquinasCrawler extends CrawlerRankingKeywords{

	public BrasilDutramaquinasCrawler(Session session) {
		super(session);
	}

	private String termoUrl;
	private String termoAlgum;
	private String termoAmbos;
	
	@Override
	protected void extractProductsFromCurrentPage() {
		//número de produtos por página do market
		this.pageSize = 27;
			
		this.log("Página "+ this.currentPage);
				
		//monta a url com a keyword e a página
		String url;
		
		if(this.currentPage == 1){
			url = "http://www.dutramaquinas.com.br/busca/" + this.keywordEncoded;
		} else {
			url = "http://www.dutramaquinas.com.br/model/md_busca.php?vc_termo="+ this.keywordEncoded +"&termo_tipo="+ this.termoUrl
				+ "&total_prod_ambos="+ this.termoAmbos +"&total_prod_algum="+ this.termoAlgum +"&ordering=relevancia&tot_rows="+this.totalProducts
				+ "&pg_num="+ this.currentPage +"&max=27";
		}
		
		this.log("Link onde são feitos os crawlers: "+url);	
			
		//chama função de pegar a url
		this.currentDoc = fetchDocument(url);
		
		//monta a keyword especial de acordo com o site
		if(this.currentPage == 1) makeKeywordsSpecial();
		
		Elements products =  this.currentDoc.select("div.produto");
		
		//se obter 1 ou mais links de produtos e essa página tiver resultado faça:
		if(products.size() >= 1) {
			//se o total de busca não foi setado ainda, chama a função para setar
			if(this.currentPage == 1) setTotalProducts();
			
			for(Element e: products) {
				//seta o id com o seletor
				String internalId 	= e.attr("key");
				String internalPid 	= null;
				
				//monta a url
				Element eUrl = e.select("h4 > a").first();
				String productUrl  = eUrl.attr("href");
				
				saveDataProduct(internalId, internalPid, productUrl);
				
				this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + internalPid + " - Url: " + productUrl);
				if(this.arrayProducts.size() == productsLimit) break;
			}
		}
		else
		{
			this.result = false;
			this.log("Keyword sem resultado!");
		}

		this.log("Finalizando Crawler de produtos da página "+this.currentPage+" - até agora "+this.arrayProducts.size()+" produtos crawleados");
	}

	@Override
	protected boolean hasNextPage() {
		if(this.arrayProducts.size() < this.totalProducts){
			//tem próxima página
			return true;
		} else {
			//não tem próxima página
			return false;
		}
	}
	
	@Override
	protected void setTotalProducts()
	{
		Element totalElement = this.currentDoc.select("input#tot_rows").first();
		
		if(totalElement != null)
		{ 	
			try
			{				
				this.totalProducts = Integer.parseInt(totalElement.attr("value"));
			}
			catch(Exception e)
			{
				this.logError(e.getMessage());
			}
			
			this.log("Total da busca: "+this.totalProducts);
		}
	}
	
	private void makeKeywordsSpecial(){
		
		this.keywordEncoded = "";
		
		Elements termSearch = this.currentDoc.select("span.termo-pesquisado");
		this.termoUrl		= this.currentDoc.select("#termo_tipo").first().attr("value");
		this.termoAlgum		= this.currentDoc.select("#total_prod_algum").first().attr("value");
		this.termoAmbos		= this.currentDoc.select("#total_prod_ambos").first().attr("value");
		
		int count = 0;
		
		for(Element e : termSearch){
			count++;
			
			if(count == 1) 	this.keywordEncoded = e.text().trim().replaceAll(" ", "+").replaceAll("\"", "");
			else			this.keywordEncoded = this.keywordEncoded +"+"+ e.text().trim().replaceAll(" ", "+").replaceAll("\"", "");
		}
		
	}
}
