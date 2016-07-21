package br.com.lett.crawlernode.main;

import java.util.ArrayList;

import br.com.lett.crawlernode.base.ExecutionParameters;
import br.com.lett.crawlernode.fetcher.Proxies;

/**
 * 
 * @author Samir Leão
 *
 */

public class Main {

	public static ExecutionParameters 	executionParameters;
	public static Proxies proxies;
	
	public static void main(String args[]) {

		// setting execution parameters
		executionParameters = new ExecutionParameters(args);
		executionParameters.setUpExecutionParameters();
		
		// fetching proxies
		proxies = new Proxies();
		proxies.fetchPremiumProxies();
		proxies.fetchRegularProxies();
		
		// creating list of markets
		
		
	}

}
