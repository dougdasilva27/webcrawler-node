package br.com.lett.crawlernode.crawlers.florianopolis;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.task.Crawler;
import br.com.lett.crawlernode.core.task.CrawlerSession;
import br.com.lett.crawlernode.util.Logging;

public class FlorianopolisAngelonieletroCrawler extends Crawler {
	
	private final String HTTP_PROTOCOL = "http://";
	
	public FlorianopolisAngelonieletroCrawler(CrawlerSession session) {
		super(session);
	}

	@Override
	public boolean shouldVisit() {
		String href = this.session.getUrl().toLowerCase();
		boolean shouldVisit = false;
		shouldVisit = !FILTERS.matcher(href).matches() && (href.startsWith("http://www.angeloni.com.br/eletro/"));

		return shouldVisit;
	}


	@Override
	public List<Product> extractInformation(Document doc) throws Exception {
		super.extractInformation(doc);
		List<Product> products = new ArrayList<Product>();

		if ( isProductPage(this.session.getUrl()) ) {
			Logging.printLogDebug(logger, "Product page identified: " + this.session.getUrl());

			// ID interno
			Element elementInternalID = doc.select(".codigo span[itemprop=sku]").first();
			String internalId = null;
			if(elementInternalID != null) {
				internalId = elementInternalID.text();
			}

			// Pid
			String internalPid = internalId;

			// Nome
			Element elementName = doc.select("#titulo h1[itemprop=name]").first();
			String name = null;
			if(elementName != null) {
				name = elementName.text();
			}

			// Preço
			Element elementPrice = doc.select("div#descricao .esquerda .valores .preco-por .microFormatoProduto").first();
			Float price = null;
			if(elementPrice != null) {
				price = Float.parseFloat(elementPrice.text().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", "."));
			}

			// Disponibilidade
			Element elementAvailable = doc.select("div#descricao .esquerda .produto-esgotado").first();
			boolean available = true;
			if(elementAvailable != null) {
				available = false;
			}

			// Categorias
			Elements elementCategories = doc.select("ol.breadcrumb li a[title]:not([title=home]) span[itemprop=title]");
			String category1 = null;
			String category2 = null;
			String category3 = null;

			if(elementCategories.size() == 1) {
				category1 = elementCategories.get(0).text();
			}
			else if(elementCategories.size() == 2) {
				category1 = elementCategories.get(0).text();
				category2 = elementCategories.get(1).text();
			}
			else if(elementCategories.size() == 3) {
				category1 = elementCategories.get(0).text();
				category2 = elementCategories.get(1).text();
				category3 = elementCategories.get(2).text();
			}

			// Imagem primária
			Element elementPrimaryImage = doc.select("div#imagem-grande a").first();
			String primaryImage = null;
			if(elementPrimaryImage != null) {
				primaryImage = HTTP_PROTOCOL + elementPrimaryImage.attr("href").replace("//", "");
			}

			// Imagens secundárias
			Elements elementsSecondaryImages = doc.select("section#galeria .jcarousel-wrapper .jcarousel ul li a");
			int elementsSize = elementsSecondaryImages.size();
			String secondaryImages = null;
			JSONArray secondaryImagesArray = new JSONArray();

			if(elementsSize >0) {
				for(int i = 0; i < elementsSize; i++) {
					String secondaryImage = HTTP_PROTOCOL + elementsSecondaryImages.get(i).attr("href").replace("//", "");
					if(secondaryImage != null) {
						if( !secondaryImage.equals(primaryImage) ) {
							secondaryImagesArray.put( secondaryImage );
						}
					}
				}
			}

			if(secondaryImagesArray.length() > 0) {
				secondaryImages = secondaryImagesArray.toString();
			}

			// Estoque
			Integer stock = null;

			// Descrição
			Elements elementsDescription = doc.select("section#abas .tab-content div[role=tabpanel]:not([id=tab-avaliacoes-clientes])");
			String description = elementsDescription.html();

			// Marketplace
			JSONArray marketplace = null;

			Product product = new Product();
			
			product.setUrl(session.getUrl());
			product.setInternalId(internalId);
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

			products.add(product);

		} else {
			Logging.printLogDebug(logger, session, "Not a product page " + this.session.getUrl());
		}
		
		return products;
	}
	
	/*******************************
	 * Product page identification *
	 *******************************/

	private boolean isProductPage(String url) {
		return url.contains("http://www.angeloni.com.br/eletro/p/");
	}

}
