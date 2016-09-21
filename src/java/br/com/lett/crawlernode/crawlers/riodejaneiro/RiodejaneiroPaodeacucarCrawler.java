package br.com.lett.crawlernode.crawlers.riodejaneiro;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.impl.cookie.BasicClientCookie;
import org.json.JSONArray;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.session.CrawlerSession;
import br.com.lett.crawlernode.core.task.Crawler;
import br.com.lett.crawlernode.util.Logging;

public class RiodejaneiroPaodeacucarCrawler extends Crawler {
	
	private final String HOME_PAGE = "http://www.paodeacucar.com.br/";
	
	public RiodejaneiroPaodeacucarCrawler(CrawlerSession session) {
		super(session);
	}

	@Override
	public boolean shouldVisit() {
		String href = this.session.getUrl().toLowerCase();

		// Não pegaremos as páginas que contém "?ftr=" pois elas são categorias filtradas, que não nos interessam.
		return !FILTERS.matcher(href).matches() && href.startsWith(HOME_PAGE) && !href.contains("?ftr=");
	}
	
	@Override
	public void handleCookiesBeforeFetch() {
	    
		// Criando cookie da loja 7 = Rio de Janeiro capital
		BasicClientCookie cookie = new BasicClientCookie("ep.selected_store", "7");
	    cookie.setDomain(".paodeacucar.com.br");
	    cookie.setPath("/");
	    this.cookies.add(cookie);
	    
	}


	@Override
	public List<Product> extractInformation(Document doc) throws Exception {
		super.extractInformation(doc);
		List<Product> products = new ArrayList<Product>();

		if ( isProductPage(this.session.getUrl()) ) {
			Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getUrl());

			// Descobrindo se produto existe na loja pão de açúcar selecionada
			Element doNotExist = doc.select("#productArea .message404").first();

			if(doNotExist != null) {
				Logging.printLogDebug(logger, session, "Produto " + this.session.getUrl() + " não existe nessa loja do Pão de Açúcar.");
				return products;
			}

			// ID interno
			Elements elementInternalID = doc.select("input[name=productId]");
			String internalID = elementInternalID.val().trim();

			// Pid
			String internalPid = internalID;

			// Nome
			Elements elementName = doc.select("h1.product-header__heading");
			String name = elementName.text().replace("'", "").trim();

			// Preço
			Elements elementPrice = doc.select("div.product-control__price > span.value");
			Float price = null;
			if(elementPrice.last() != null) {
				price = Float.parseFloat( elementPrice.last().text().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", ".") );
			}

			// Categorias
			Elements elementCategory = doc.select("ul.breadcrumbs__items > li > a");
			String category1 = "";
			String category2 = "";
			String category3 = "";

			if(elementCategory.size() > 1) category1 = elementCategory.get(1).text().trim();
			if(elementCategory.size() > 2) category2 = elementCategory.get(2).text().trim();

			// Imagem primária
			Elements elementPrimaryImage = doc.select("#product-image > a.zoomImage");
			String primaryImage = elementPrimaryImage.attr("href");

			// Se não tem a foto grande, pega a miniatura mesmo
			if(primaryImage.equals("#")) {
				elementPrimaryImage = doc.select("#product-image > a.zoomImage > img");
				primaryImage = elementPrimaryImage.attr("src");
			}
			primaryImage = "http://www.paodeacucar.com.br" + primaryImage;
			if(primaryImage.contains("nome_da_imagem_do_sem_foto.gif")) {
				primaryImage = ""; //TODO: Verificar o nome da foto genérica
			}

			// Imagens secundárias
			String secondaryImages = "";
			JSONArray secondaryImagesArray = new JSONArray();

			Elements secondaryImageElement = doc.select(".product-image__gallery--holder ul li a");

			if(secondaryImageElement.size() > 1) {
				for(int i = 1; i < secondaryImageElement.size(); i++) { // a partir da segunda, porque a primeira é imagem primária
					String tmp;

					// tentar pegar a imagem grande
					tmp = secondaryImageElement.get(i).attr("data-zoom").toString();

					if(tmp.equals("#")) { // se não tem a grande, pegar a que tiver
						tmp = secondaryImageElement.get(i).attr("href");
					}
					tmp = "http://www.paodeacucar.com.br" + tmp;
					secondaryImagesArray.put(tmp);
				}						
			}
			if(secondaryImagesArray.length() > 0) {
				secondaryImages = secondaryImagesArray.toString();
			}

			// Descrição
			String description = "";
			Element elementDescriptionText = doc.select("#nutritionalChart").first();
			if(elementDescriptionText != null) {
				description = description + elementDescriptionText.html();
			}

			// Disponibilidade
			boolean available = true;
			Element elementUnavailable = doc.select(".flip-tab__content").first();
			if (elementUnavailable == null) {
				available = true;
			}
			else if (elementUnavailable.text().equals("Indisponível")) available = false;

			// Estoque
			Integer stock = null;

			// Marketplace
			JSONArray marketplace = null;

			Product product = new Product();
			product.setUrl(this.session.getUrl());
			
			product.setInternalId(internalID);
			product.setInternalPid(internalPid);
			product.setName(name);
			product.setPrice(price);
			product.setCategory1(category1);
			product.setCategory2(category2);
			product.setCategory3(category3);
			product.setPrimaryImage(primaryImage);
			product.setSecondaryImages(secondaryImages);
			product.setDescription(description);
			product.setStock(stock);
			product.setMarketplace(marketplace);
			product.setAvailable(available);

			if ( !(product.getAvailable() == false && product.getPrice() != null) ) {
				products.add(product);
			}

		} else {
			Logging.printLogDebug(logger, session, "Not a product page" + this.session.getUrl());
		}
		
		return products;
	}
	
	/*******************************
	 * Product page identification *
	 *******************************/

	private boolean isProductPage(String url) {
		return url.contains("/produto/");
	}
}
