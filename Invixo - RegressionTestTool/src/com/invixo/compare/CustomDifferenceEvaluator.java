package com.invixo.compare;

import java.util.List;

import org.xmlunit.diff.Comparison;
import org.xmlunit.diff.ComparisonResult;
import org.xmlunit.diff.DifferenceEvaluator;

import com.invixo.common.util.Logger;

public class CustomDifferenceEvaluator implements DifferenceEvaluator {
private List<String> exceptionList;
private static Logger logger			= Logger.getInstance();
private static final String LOCATION 	= CustomDifferenceEvaluator.class.getName();
	
	/**
	 * Class constructor
	 * @param configuredExceptionList	List of Xpath strings used to ignore DIFFERENCES found in XML evaluation
	 */
	public CustomDifferenceEvaluator(List<String> configuredExceptionList) {
		exceptionList = configuredExceptionList;
	}
	
	
	@Override
	public ComparisonResult evaluate(Comparison comp, ComparisonResult compResult) {
		String SIGNATURE = "evaluate";
		
		// React only on diffences found during evaluation
		if (compResult.name().equals("DIFFERENT")) {
			
			// Should the difference be ignored?
			for (String exceptionXpathString : exceptionList) {
				
				/* Strip square bractes from comp.getControlDetails().getXPath() to create a generic xpath.
				 * /Astro_Envelope[1]/PurchaseOrderSync[1]/DataArea[1]/PurchaseOrderLine[1]/DeliveryDate[1]/text()[1]
				 * /Astro_Envelope/PurchaseOrderSync/DataArea/PurchaseOrderLine/DeliveryDate/text()
				 * This so we can ignore ALL occurrences in one exception entry
				 */
				String strippedXpath = comp.getControlDetails().getXPath().replaceAll("\\[(.+?)\\]", "");
				
				if (strippedXpath.equals(exceptionXpathString)) {
					logger.writeDebug(LOCATION, SIGNATURE, "--> Diff found but is configured to be ignored using exception: " + exceptionXpathString);
					
					// Change result from DIFFERENT to EQUAL as it is found in our exception list
					compResult = ComparisonResult.EQUAL;
				}
			}			
		}
		
		// Return result
		return compResult;
	}

}
