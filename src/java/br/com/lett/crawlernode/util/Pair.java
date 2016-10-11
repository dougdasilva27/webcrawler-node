package br.com.lett.crawlernode.util;

/**
 * Auxiliar data structure representing a pair of values
 * associated with each other. Like c++ pair ;)
 * 
 * @author Samir Leao
 *
 * @param <F>
 * @param <S>
 */
public class Pair<F, S> {
	
	private F first;
	private S second;
	
	public Pair(F first, S second) {
		this.first = first;
		this.second = second;
	}
	
	public F getFirst() {
		return this.first;
	}
	
	public S getSecond() {
		return this.second;
	}

}
