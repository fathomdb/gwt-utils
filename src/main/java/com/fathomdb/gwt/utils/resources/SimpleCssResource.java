package com.fathomdb.gwt.utils.resources;

import com.fathomdb.gwt.utils.rg.SimpleCssResourceGenerator;
import com.google.gwt.resources.client.ResourcePrototype;
import com.google.gwt.resources.ext.ResourceGeneratorType;

@ResourceGeneratorType(SimpleCssResourceGenerator.class)
public interface SimpleCssResource extends ResourcePrototype {
	/**
	 * Calls {@link com.google.gwt.dom.client.StyleInjector#injectStylesheet(String)} to inject the contents of the
	 * CssResource into the DOM. Repeated calls to this method on an instance of a CssResources will have no effect.
	 * 
	 * @return <code>true</code> if this method mutated the DOM.
	 */
	boolean ensureInjected();

	/**
	 * Provides the contents of the SimpleCssResource.
	 */
	String getText();

}
