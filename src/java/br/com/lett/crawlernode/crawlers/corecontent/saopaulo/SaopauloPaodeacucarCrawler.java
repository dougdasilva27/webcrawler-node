package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.impl.cookie.BasicClientCookie;
import org.json.JSONArray;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.Logging;
import models.Prices;

public class SaopauloPaodeacucarCrawler extends Crawler {

	private final String HOME_PAGE = "http://www.paodeacucar.com";

	public SaopauloPaodeacucarCrawler(Session session) {
		super(session);
	}

	@Override
	public boolean shouldVisit() {
		String href = this.session.getOriginalURL().toLowerCase();
		return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
	}

	@Override
	public void handleCookiesBeforeFetch() {

		// Criando cookie da loja 3 = São Paulo capital
		BasicClientCookie cookie = new BasicClientCookie("ep.selected_store", "501");
		cookie.setDomain(".paodeacucar.com.br");
		cookie.setPath("/");
		this.cookies.add(cookie);

	}


	@Override
	public List<Product> extractInformation(Document doc) throws Exception {
		super.extractInformation(doc);
		List<Product> products = new ArrayList<>();

		if ( isProductPage(this.session.getOriginalURL()) ) {
			Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

			// Descobrindo se produto existe na loja pão de açúcar selecionada
			Element doNotExist = doc.select("#productArea .message404").first();

			if(doNotExist != null) {
				Logging.printLogDebug(logger, session, "Produto " + this.session.getOriginalURL() + " não existe nessa loja do Pão de Açúcar.");
				return products;
			}

			String internalId = crawlInternalId(doc);
			String internalPid = crawlInternalPid(doc);
			String name = crawlName(doc);
			Float price = crawlPrice(doc);

			// Categorias
			Elements elementCategory = doc.select("ul.breadcrumbs__items > li > a");
			String category1 = "";
			String category2 = "";
			String category3 = "";
			if(elementCategory.size() > 1) category1 = elementCategory.get(1).text().trim();
			if(elementCategory.size() > 2) category2 = elementCategory.get(2).text().trim();

			String primaryImage = crawlMainImage(doc);
			String secondaryImages = crawlSecondaryImages(doc);
			String description = crawlDescription(doc);
			boolean available = crawlAvailability(doc);
			Integer stock = null;
			JSONArray marketplace = null;
			Prices prices = crawlPrices(doc, price);
			
			Product product = new Product();
			product.setUrl(this.session.getOriginalURL());
			product.setInternalId(internalId);
			product.setInternalPid(internalPid);
			product.setName(name);
			product.setPrice(price);
			product.setPrices(prices);
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
			Logging.printLogDebug(logger, session, "Not a product page" + this.session.getOriginalURL());
		}

		return products;
	}

	/*******************************
	 * Product page identification *
	 *******************************/

	private boolean isProductPage(String url) {
		return url.contains("/produto/");
	}
	
	private String crawlInternalId(Document doc) {
		Element elementInternalId = doc.select("input[name=productId]").first();
		if (elementInternalId != null) {
			return elementInternalId.attr("value").trim();
		}
		return null;
	}
	
	private String crawlInternalPid(Document doc) {
		return null;
	}
	
	private String crawlName(Document doc) {
		Elements elementName = doc.select("h1.product-header__heading");
		return elementName.text().replace("'", "").trim();
	}
	
	private Float crawlPrice(Document doc) {
		Float price = null;
		Element elementPrice = doc.select("div.product-control__price.price_per > span.value").last();
		if(elementPrice != null) {
			price = Float.parseFloat( elementPrice.text().trim().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", ".") );
		} else {
			elementPrice = doc.select("div.product-control__price > span.value").first();
			if (elementPrice != null) {
				price = Float.parseFloat( elementPrice.text().trim().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", ".") );
			}
		}
		return price;
	}
	
	private String crawlMainImage(Document doc) {
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
		
		return primaryImage;
	}
	
	private String crawlSecondaryImages(Document doc) {
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
		
		return secondaryImages;
	}
	
	private boolean crawlAvailability(Document doc) {
		boolean available = false;
		Element elementAvailable = doc.select(".btnComprarProd").first();
		if (elementAvailable != null) {
			available = true;
		}
		return available;
	}
	
	private String crawlDescription(Document doc) {
		Element elementDescriptionText = doc.select("#nutritionalChart").first();
		if(elementDescriptionText != null) {
			return elementDescriptionText.html();
		}
		return "";
	}
	
	/**
	 * In this market, installments not appear in product page
	 * 
	 * @param doc
	 * @param price
	 * @return
	 */
	private Prices crawlPrices(Document doc, Float price){
		Prices prices = new Prices();

		if(price != null){
			Map<Integer,Float> installmentPriceMap = new HashMap<>();

			installmentPriceMap.put(1, price);
			prices.setBankTicketPrice(price);

			prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
			prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
			prices.insertCardInstallment(Card.AMEX.toString(), installmentPriceMap);
			prices.insertCardInstallment(Card.DINERS.toString(), installmentPriceMap);
		}

		return prices;
	}
}
