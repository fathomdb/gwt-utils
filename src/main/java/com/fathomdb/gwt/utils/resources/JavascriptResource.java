package com.fathomdb.gwt.utils.resources;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.fathomdb.gwt.utils.rg.JavascriptResourceGenerator;
import com.google.gwt.resources.client.ResourcePrototype;
import com.google.gwt.resources.ext.ResourceGeneratorType;

@ResourceGeneratorType(JavascriptResourceGenerator.class)
public interface JavascriptResource extends ResourcePrototype {
	/**
	 * Calls {@link com.google.gwt.dom.client.StyleInjector#injectStylesheet(String)} to inject the contents of the
	 * CssResource into the DOM. Repeated calls to this method on an instance of a CssResources will have no effect.
	 * 
	 * @return <code>true</code> if this method mutated the DOM.
	 */
	boolean ensureInjected();

	/**
	 * Provides the contents of the JavascriptResource.
	 */
	String getText();

	@Documented
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.METHOD)
	public @interface Minification {
		boolean minify() default true;
	}

}
