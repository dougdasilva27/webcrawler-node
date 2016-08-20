package br.com.lett.crawlernode.util;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import br.com.lett.crawlernode.processor.models.ProcessedModel;

public class ProcessedModelSanitizer {
	
	/**
	 * Processor
	 * 
	 * Testa os atributos definidos no crawlerModel e adiciona aspas simples caso estes não sejam
	 * nulos nem string vazia.
	 * 
	 * @category comparação 
	 * @author julinha
	 */
	public static void prepareToPersist(ProcessedModel processedModel) {
		
		Long 		id = processedModel.getId();
		String 		internalId = processedModel.getInternalId();
		String 		internalPid = processedModel.getInternalPid();
		String 		_class = processedModel.get_class();
		String 		brand = processedModel.getBrand();
		String 		recipient = processedModel.getRecipient();
		Double 		quantity = processedModel.getQuantity();
		Integer 	multiplier = processedModel.getMultiplier();
		String 		unit = processedModel.getUnit();
		String 		extra = processedModel.getExtra();
		String 		pic = processedModel.getPic();
		String 		secondary_pics = processedModel.getSecondary_pics();
		String 		cat1 = processedModel.getCat1();
		String 		cat2 = processedModel.getCat2();
		String 		cat3 = processedModel.getCat3();
		String 		url = processedModel.getUrl();
		Integer 	market = processedModel.getMarket();
		String 		originalName = processedModel.getOriginalName();
		String 		originalDescription = processedModel.getOriginalDescription();
		Float 		price = processedModel.getPrice();
		Integer 	stock = processedModel.getStock();
		JSONObject 	digitalContent = processedModel.getDigitalContent();
		JSONArray 	behaviour = processedModel.getBehaviour();
		JSONArray 	marketplace = processedModel.getMarketplace();
		String 		lmt = processedModel.getLmt();
		String 		ect = processedModel.getEct();
		String 		lat = processedModel.getLat();
		String 		lrt = processedModel.getLrt();
		String 		lms = processedModel.getLms();
		String 		status = processedModel.getStatus();
		Boolean 	available = processedModel.getAvailable();
		Boolean 	void_product = processedModel.getVoid_product();
		JSONObject 	changes = processedModel.getChanges();
		Long 		lettId = processedModel.getLettId();
		JSONArray 	similars = processedModel.getSimilars();
		String 		sanitizedName = processedModel.getSanitizedName();

		// trata as eventuais aspas simples existentes
		if (processedModel.getInternalId() != null) processedModel.setInternalId( processedModel.getInternalId().replace("'", "''") );
		if (internalPid != null) 					processedModel.setInternalPid( internalPid.replace("'", "''") );
		if (originalName != null) 					processedModel.setOriginalName( originalName.replace("'", "''") );
		if (originalDescription != null) 			processedModel.setOriginalDescription( originalDescription.replace("'", "''") );
		if (_class != null) 						processedModel.set_class( _class.replace("'", "''") );
		if (brand != null) 							processedModel.setBrand( brand.replace("'", "''") );
		if (recipient != null) 						processedModel.setRecipient( recipient.replace("'", "''") );
		if (unit != null) 							processedModel.setUnit( unit.replace("'", "''") );
		if (extra != null) 							processedModel.setExtra( extra.replace("'", "''") );
		if (cat1 != null) 							processedModel.setCat1( cat1.replace("'", "''") );
		if (cat2 != null) 							processedModel.setCat2( cat2.replace("'", "''") );
		if (cat3 != null) 							processedModel.setCat3( cat3.replace("'", "''") );

		// adiciona aspas simples ou atribui nulo.
		if (price == null) {
			processedModel.setPrice( (float) 0 );
		}

		if (internalId != null && !internalId.isEmpty()) {
			processedModel.setInternalId( "'" + internalId + "'" );
		} 
		else {
			processedModel.setInternalId( null );
		}

		if (internalPid != null && !internalPid.isEmpty()) {
			processedModel.setInternalPid( "'" + internalPid + "'" );
		} 
		else {
			processedModel.setInternalPid( null );
		}

		if (originalName != null && !originalName.isEmpty()) {
			processedModel.setOriginalName( "'" + originalName + "'" );
		} 
		else {
			processedModel.setOriginalName( null );
		}

		if (originalDescription != null && !originalDescription.isEmpty()) {
			processedModel.setOriginalDescription( "'" + originalDescription + "'" );
		} 
		else {
			processedModel.setOriginalDescription( null );
		}

		if (_class != null && !_class.isEmpty()) {
			processedModel.set_class( "'" + _class + "'" );
		} 
		else {
			processedModel.set_class( null );
		}

		if (brand != null && !brand.isEmpty()) {
			processedModel.setBrand( "'" + brand + "'" );
		} 
		else {
			processedModel.setBrand( null );
		}

		if (recipient != null && !recipient.isEmpty()) {
			processedModel.setRecipient( "'" + recipient + "'" );
		} 
		else {
			processedModel.setRecipient( null );
		}

		if (multiplier == null) {
			processedModel.setMultiplier( 1 );
		}

		if (unit != null && !unit.isEmpty()) {
			processedModel.setUnit( "'" + unit + "'" );
		} 
		else {
			processedModel.setUnit( null );
		}

		if (extra != null && !extra.isEmpty()) {
			processedModel.setExtra( "'" + extra + "'" );
		} 
		else {
			processedModel.setExtra( null );
		}

		if (pic != null && !pic.isEmpty()) {
			processedModel.setPic( "'" + pic + "'" );
		} 
		else {
			processedModel.setPic( null );
		}

		if (secondary_pics != null && !secondary_pics.isEmpty()) {
			processedModel.setSecondary_pics( "'" + secondary_pics + "'" );
		} 
		else {
			processedModel.setSecondary_pics( null );
		}

		if (cat1 != null && !cat1.isEmpty()) {
			processedModel.setCat1( "'" + cat1 + "'" );
		} 
		else {
			processedModel.setCat1( null );
		}

		if (cat2 != null && !cat2.isEmpty()) {
			processedModel.setCat2( "'" + cat2 + "'" );
		} 
		else {
			processedModel.setCat2( null );
		}

		if (cat3 != null && !cat3.isEmpty()) {
			processedModel.setCat3( "'" + cat3 + "'" );
		} 
		else {
			processedModel.setCat3( null );
		}

		if (url != null && !url.isEmpty()) {
			processedModel.setUrl( "'" + url + "'" );
		} 
		else {
			processedModel.setUrl( null );
		}

		if (ect != null && !ect.isEmpty()) {
			processedModel.setEct( "'" + ect + "'" );
		} 
		else {
			processedModel.setEct( null );
		}

		if (lmt != null && !lmt.isEmpty()) {
			processedModel.setLmt( "'" + lmt + "'" );
		} 
		else {
			processedModel.setLmt( null );
		}

		if (lat != null && !lat.isEmpty()) {
			processedModel.setLat( "'" + lat + "'" );
		} 
		else {
			processedModel.setLat( null );
		}

		if (lrt != null && !lrt.isEmpty()) {
			processedModel.setLrt( "'" + lrt + "'" );
		} 
		else {
			processedModel.setLrt( null );
		}

		if (lms != null && !lms.isEmpty()) {
			processedModel.setLms( "'" + lms + "'" );
		} 
		else {
			processedModel.setLms( null );
		}

		if (status != null && !status.isEmpty()) {
			processedModel.setStatus( "'" + status + "'" );
		} 
		else {
			processedModel.setStatus( null );
		}

		if (changes != null && changes.length() == 0) {
			processedModel.setChanges( null );
		}

		if (digitalContent != null && digitalContent.length() == 0) {
			processedModel.setDigitalContent( null );
		}

		if (behaviour != null && behaviour.length() == 0) {
			processedModel.setBehaviour( null );
		}

		if (marketplace != null && marketplace.length() == 0) {
			processedModel.setMarketplace( null );
		}

		if (similars != null && similars.length() == 0) {
			processedModel.setSimilars( null );
		}
	}

	// Fonte http://www.ranks.nl/stopwords/brazilian
	public static String sanitizeTextAndRemoveStopWords(String text) {

		String result = text;

		result = result.toLowerCase();
		result = Normalizer.normalize(result, Normalizer.Form.NFD).replaceAll("[^\\p{ASCII}]", "");
		result = " " + result + " ";

		for(String stopWord: stopWords()) {
			result = result.replace(" " + stopWord + " ", " ");
		}

		return result;

	}


	// Fonte http://www.ranks.nl/stopwords/brazilian
	public static List<String> stopWords() {

		List<String> stopWords = new ArrayList<String>();

		stopWords.add("a");
		stopWords.add("ainda");
		stopWords.add("alem");
		stopWords.add("ambas");
		stopWords.add("ambos");
		stopWords.add("antes");
		stopWords.add("ao");
		stopWords.add("aonde");
		stopWords.add("aos");
		stopWords.add("apos");
		stopWords.add("aquele");
		stopWords.add("aqueles");
		stopWords.add("as");
		stopWords.add("assim");
		stopWords.add("com");
		stopWords.add("como");
		stopWords.add("contra");
		stopWords.add("contudo");
		stopWords.add("cuja");
		stopWords.add("cujas");
		stopWords.add("cujo");
		stopWords.add("cujos");
		stopWords.add("da");
		stopWords.add("das");
		stopWords.add("de");
		stopWords.add("dela");
		stopWords.add("dele");
		stopWords.add("deles");
		stopWords.add("demais");
		stopWords.add("depois");
		stopWords.add("desde");
		stopWords.add("desta");
		stopWords.add("deste");
		stopWords.add("dispoe");
		stopWords.add("dispoem");
		stopWords.add("diversa");
		stopWords.add("diversas");
		stopWords.add("diversos");
		stopWords.add("do");
		stopWords.add("dos");
		stopWords.add("durante");
		stopWords.add("e");
		stopWords.add("ela");
		stopWords.add("elas");
		stopWords.add("ele");
		stopWords.add("eles");
		stopWords.add("em");
		stopWords.add("entao");
		stopWords.add("entre");
		stopWords.add("essa");
		stopWords.add("essas");
		stopWords.add("esse");
		stopWords.add("esses");
		stopWords.add("esta");
		stopWords.add("estas");
		stopWords.add("este");
		stopWords.add("estes");
		stopWords.add("ha");
		stopWords.add("isso");
		stopWords.add("isto");
		stopWords.add("logo");
		stopWords.add("mais");
		stopWords.add("mas");
		stopWords.add("mediante");
		stopWords.add("menos");
		stopWords.add("mesma");
		stopWords.add("mesmas");
		stopWords.add("mesmo");
		stopWords.add("mesmos");
		stopWords.add("na");
		stopWords.add("nas");
		stopWords.add("nao");
		stopWords.add("nas");
		stopWords.add("nem");
		stopWords.add("nesse");
		stopWords.add("neste");
		stopWords.add("nos");
		stopWords.add("o");
		stopWords.add("os");
		stopWords.add("ou");
		stopWords.add("outra");
		stopWords.add("outras");
		stopWords.add("outro");
		stopWords.add("outros");
		stopWords.add("pelas");
		stopWords.add("pelas");
		stopWords.add("pelo");
		stopWords.add("pelos");
		stopWords.add("perante");
		stopWords.add("pois");
		stopWords.add("por");
		stopWords.add("porque");
		stopWords.add("portanto");
		stopWords.add("proprio");
		stopWords.add("propios");
		stopWords.add("quais");
		stopWords.add("qual");
		stopWords.add("qualquer");
		stopWords.add("quando");
		stopWords.add("quanto");
		stopWords.add("que");
		stopWords.add("quem");
		stopWords.add("quer");
		stopWords.add("se");
		stopWords.add("seja");
		stopWords.add("sem");
		stopWords.add("sendo");
		stopWords.add("seu");
		stopWords.add("seus");
		stopWords.add("sob");
		stopWords.add("sobre");
		stopWords.add("sua");
		stopWords.add("suas");
		stopWords.add("tal");
		stopWords.add("tambem");
		stopWords.add("teu");
		stopWords.add("teus");
		stopWords.add("toda");
		stopWords.add("todas");
		stopWords.add("todo");
		stopWords.add("todos");
		stopWords.add("tua");
		stopWords.add("tuas");
		stopWords.add("tudo");
		stopWords.add("um");
		stopWords.add("uma");
		stopWords.add("umas");
		stopWords.add("uns");

		return stopWords;
	}

}
