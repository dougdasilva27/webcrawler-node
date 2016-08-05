package br.com.lett.crawlernode.crawlers.brasil;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.kernel.Crawler;
import br.com.lett.crawlernode.kernel.CrawlerSession;
import br.com.lett.crawlernode.models.Product;
import br.com.lett.crawlernode.util.Logging;

public class BrasilFnacCrawler extends Crawler {

	private final String HOME_PAGE = "http://www.fnac.com.br/";

	public BrasilFnacCrawler(CrawlerSession session) {
		super(session);
	}

	@Override
	public boolean shouldVisit() {
		String href = this.session.getUrl().toLowerCase();
		return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
	}

	@Override
	public List<Product> extractInformation(Document doc) {
		super.extractInformation(doc);
		List<Product> products = new ArrayList<Product>();

		if ( isProductPage(this.session.getUrl()) ) {
			Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getUrl());

			// ID interno
			String internalId = this.session.getUrl().split("/")[5];

			// Pid
			String internalPid = null;

			// Nome
			String name = null;
			Element elementName = doc.select("#nomeProduto").first();
			if(elementName != null) {
				name = elementName.text();
			}

			// Disponibilidade
			boolean available = true;
			Element elementNotifyMe = doc.select(".indisponivel").first();
			if (elementNotifyMe != null) {
				available = false;
			}

			// Preço
			Float price = null;
			Element elementPrice = doc.select("#spanValorAtual").first();
			if (elementPrice != null && available) {
				price = Float.parseFloat(elementPrice.text().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", "."));
			}

			// Categoria
			String category1 = "";
			String category2 = "";
			String category3 = "";
			Elements elementCategories = doc.select(".breadcrumb ul li a strong");
			ArrayList<String> categories = new ArrayList<String>();
			for(Element e : elementCategories) {
				String tmp = e.text();
				if( !tmp.equals("Home") ) {
					categories.add(tmp);
				}
			}
			for (String c : categories) {
				if (category1.isEmpty()) {
					category1 = c.trim();
				} else if (category2.isEmpty()) {
					category2 = c.trim();
				} else if (category3.isEmpty()) {
					category3 = c.trim();
				}
			}

			// Imagens
			Elements elementImages = doc.select(".foto img.fotoGrande");
			String primaryImage = null;
			String secondaryImages = null;
			JSONArray secondaryImagesArray = new JSONArray();

			for(Element image : elementImages) {
				if(primaryImage == null) {
					primaryImage = image.attr("src");
				} else {
					secondaryImagesArray.put(image.attr("src"));
				}
			}
			if (secondaryImagesArray.length() > 0) {
				secondaryImages = secondaryImagesArray.toString();
			}

			// Descrição
			String description = "";
			Element elementDescription = doc.select(".conteudoAbas #conteudoDescricao").first();
			Element elementEspecification = doc.select(".conteudoAbas #conteudoEspecificacao").first();
			if(elementDescription != null) {
				description = description + elementDescription.html();
			}
			if(elementEspecification != null) {
				description = description + elementEspecification.html();
			}

			// Estoque
			Integer stock = null;

			// Marketplace
			JSONArray marketplace = null;

			Product product = new Product();
			product.setSeedId(this.session.getSeedId());
			product.setUrl(this.session.getUrl());
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
			Logging.printLogDebug(logger, session, "Not a product page" + this.session.getUrl());
		}
		
		return products;
	}
	
	
	/*******************************
	 * Product page identification *
	 *******************************/

	private boolean isProductPage(String url) {
		return url.contains("/p/");
	}

}
