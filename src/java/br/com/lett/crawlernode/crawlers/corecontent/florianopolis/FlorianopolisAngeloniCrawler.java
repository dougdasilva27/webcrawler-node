package br.com.lett.crawlernode.crawlers.corecontent.florianopolis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.crawler.Crawler;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.Prices;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathCommonsMethods;

public class FlorianopolisAngeloniCrawler extends Crawler {
	
	public FlorianopolisAngeloniCrawler(Session session) {
		super(session);
	}

	@Override
	public boolean shouldVisit() {
		String href = this.session.getOriginalURL().toLowerCase();

		boolean shouldVisit = false;

		shouldVisit = !FILTERS.matcher(href).matches() && (href.startsWith("http://www.angeloni.com.br/super/"));

		return shouldVisit;
	}


	@Override
	public List<Product> extractInformation(Document doc) throws Exception {
		super.extractInformation(doc);
		List<Product> products = new ArrayList<Product>();

		if( isProductPage(this.session.getOriginalURL()) ) {
			Logging.printLogDebug(logger, "Product page identified: " + this.session.getOriginalURL());

			// ID interno
			String internalId = null;
			Element elementInternalID = doc.select("input#idProduto[name=idProduto]").first();
			if(elementInternalID != null) {
				internalId = elementInternalID.attr("value").trim();
			} else {
				return products;
			}

			// Pid
			String internalPid = internalId;

			// Nome
			Elements elementName = doc.select("#imgProdBig img");
			String name = elementName.attr("alt").replace("'", "").trim();

			// Preço
			Elements elementPrice = doc.select("span.valorPrice");
			Float price = Float.parseFloat(elementPrice.text().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", "."));

			// Disponibilidade
			boolean available = true;

			// Categorias
			Elements elementCategory1 = doc.select(".boxInIn ul li");
			String category1 = (elementCategory1.size() >= 5) ? elementCategory1.get(4).text().trim() : "";

			Elements elementCategory2 = doc.select(".boxInIn ul li");
			String category2 = (elementCategory2.size() >= 7) ? elementCategory2.get(6).text().trim() : "";

			Elements elementCategory3 = doc.select(".boxInIn ul li");
			String category3 = (elementCategory3.size() >= 9) ? elementCategory3.get(8).text().trim() : "";

			// Imagem primária
			Elements elementPrimaryImage;
			String primaryImage = "";

			// Tentando pegar a foto grande
			elementPrimaryImage = doc.select("#imgProdBig a");

			if(elementPrimaryImage != null && !elementPrimaryImage.isEmpty()) {
				primaryImage = elementPrimaryImage.attr("href");
			}
			else {

				// Não tem foto grande, então pega a peguena
				elementPrimaryImage = doc.select("#imgProdBig img");
				primaryImage = elementPrimaryImage.attr("src");
			}
			if(primaryImage.contains("semfoto_detalhe_super.gif")) primaryImage = "";

			// Imagens secundárias
			String secondaryImages = null;

			JSONArray secondaryImagesArray = new JSONArray();
			Elements elementSecondaryImage = doc.select("#lstProdImgs ul a");

			if(elementSecondaryImage.size() > 1){
				for(int i = 0; i < elementSecondaryImage.size();i++){
					Element e = elementSecondaryImage.get(i);
					if( !e.attr("rel").equals(primaryImage) ) {
						secondaryImagesArray.put(e.attr("rel"));
					}
				}
			}

			if(secondaryImagesArray.length() > 0) {
				secondaryImages = secondaryImagesArray.toString();
			}

			// Estoque
			Integer stock = null;

			// Marketplace
			JSONArray marketplace = null;

			// Descrição
			String description = "";
			Element elementTabDescription = doc.select("#abaDescription").first();
			Element elementTabComposition = doc.select("#abaComposicao").first();
			Element elementTabIncludedItens = doc.select("#abaIncludedItens").first();

			if(elementTabDescription != null) 		description = description + elementTabDescription.html();
			if(elementTabComposition != null) 		description = description + elementTabComposition.html();
			if(elementTabIncludedItens != null) 	description = description + elementTabIncludedItens.html();

			// Prices
			Prices prices = crawlPrices(doc, price);
			
			Product product = new Product();
			
			product.setUrl(session.getOriginalURL());
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

			products.add(product);

		} else {
			Logging.printLogDebug(logger, session, "Not a product page.");
		}
		
		return products;
	}
	
	/*******************************
	 * Product page identification *
	 *******************************/

	private boolean isProductPage(String url) {
		return url.contains("idProduto=");
	}
	
	/**
	 * Each card has your owns installments
	 * Showcase price is price sight
	 * @param doc
	 * @param price
	 * @return
	 */
	private Prices crawlPrices(Document doc, Float price){
		Prices prices = new Prices();
		
		if(price != null) {
			prices.insertBankTicket(price);
			
			prices.insertCardInstallment(Card.SHOP_CARD.toString(), crawlInstallments(price, doc, "01"));
			prices.insertCardInstallment(Card.VISA.toString(), crawlInstallments(price, doc, "02"));
			prices.insertCardInstallment(Card.MASTERCARD.toString(), crawlInstallments(price, doc, "03"));
			prices.insertCardInstallment(Card.AMEX.toString(), crawlInstallments(price, doc, "04"));
			prices.insertCardInstallment(Card.DINERS.toString(), crawlInstallments(price, doc, "05"));
		}
		
		return prices;
	}
	
	private Map<Integer,Float> crawlInstallments(Float price, Document doc, String idCard){
		Map<Integer,Float> installmentsPriceMap = new HashMap<>();
		
		installmentsPriceMap.put(1, price);
		
		Elements installments = doc.select("#valCard"+ idCard +" table tr");
		
		for(Element e : installments) {
			Elements installmentElements = e.select("td");
			
			if(installmentElements.size() > 1){
				String textVezes = installmentElements.first().text().replaceAll("[^0-9]", "").trim();
				if(!textVezes.isEmpty()){
					Integer installment = Integer.parseInt(textVezes);
					Float value = MathCommonsMethods.parseFloat(installmentElements.get(1).text());
					
					installmentsPriceMap.put(installment, value);
				}
			}
		}
		
		return installmentsPriceMap;
	}
}
