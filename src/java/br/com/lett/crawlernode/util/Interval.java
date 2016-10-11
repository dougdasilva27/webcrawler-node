package br.com.lett.crawlernode.util;

/**
 * 
 * Auxiliar data structure to represent a mathematical interval.
 * 
 * @author Samir Leao
 *
 * @param <T>
 */
public class Interval<T> {
	
	/** A representative name for the interval */
	String name;
	
	private T start;
	private T end;
	
	public Interval(T start, T end) {
		this.start = start;
		this.end = end;
	}
	
	public Interval(String name, T start, T end) {
		this.name = name;
		this.start = start;
		this.end = end;
	}
	
	public T getStart() {
		return this.start;
	}
	
	public T getEnd() {
		return this.end;
	}
	
	public String getName() {
		return this.name;
	}
	
	@Override
	public String toString() {
		StringBuilder stringBuilder = new StringBuilder();
		
		stringBuilder.append(this.name);
		stringBuilder.append("[");
		stringBuilder.append(this.start);
		stringBuilder.append(", ");
		stringBuilder.append(this.end);
		stringBuilder.append("]");
		
		return stringBuilder.toString();
	}
	
}
