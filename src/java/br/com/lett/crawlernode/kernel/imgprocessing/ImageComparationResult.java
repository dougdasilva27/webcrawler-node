package br.com.lett.crawlernode.kernel.imgprocessing;

public class ImageComparationResult implements Comparable<ImageComparationResult> {
	
	private int totalNumberOfMatches;
	private int numberOfMatches;
	private double rate;
	private boolean passed;
	
	public ImageComparationResult() {
		super();
		passed = false;
	}
	
	@Override
	public int compareTo(ImageComparationResult o) {
		
		// TODO: Dividir pelo total
		
		if(this.numberOfMatches < o.numberOfMatches) {
			return -1;
		}
		if(this.numberOfMatches > o.numberOfMatches) {
			return 1;
		}
		return 0;
	}
	
	public void setTotalNumberOfMatches(int totalNumberOfMatches) {
		this.totalNumberOfMatches = totalNumberOfMatches;
	}
	
	public int getTotalNumberOfMatches() {
		return this.totalNumberOfMatches;
	}
	
	public void setRate(double rate) {
		this.rate = rate;
	}
	
	public double getRate() {
		return rate;
	}
	
	public void setNumberOfMatches(int numberOfMatches) {
		this.numberOfMatches = numberOfMatches;
	}
	
	public void setPassed() {
		this.passed = true;
	}
	
	public int getNumberOfMatches() {
		return this.numberOfMatches;
	}
	
	public boolean passed() {
		return this.passed;
	}

}