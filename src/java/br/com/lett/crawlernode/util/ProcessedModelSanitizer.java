package br.com.lett.crawlernode.util;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;

public class ProcessedModelSanitizer {

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
