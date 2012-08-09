package com.fathomdb.gwt.utils.rg;

import java.io.IOException;
import java.net.URL;

import org.apache.tools.ant.filters.StringInputStream;

import com.fathomdb.gwt.utils.resources.JavascriptInjector;
import com.fathomdb.gwt.utils.resources.JavascriptResource;
import com.fathomdb.gwt.utils.resources.JavascriptResource.Minification;
import com.google.gwt.core.ext.Generator;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.dev.util.Util;
import com.google.gwt.resources.ext.AbstractResourceGenerator;
import com.google.gwt.resources.ext.ResourceContext;
import com.google.gwt.resources.ext.ResourceGeneratorUtil;
import com.google.gwt.user.rebind.SourceWriter;
import com.google.gwt.user.rebind.StringSourceWriter;
import com.google.javascript.jscomp.CompilationLevel;
import com.google.javascript.jscomp.Compiler;
import com.google.javascript.jscomp.CompilerOptions;
import com.google.javascript.jscomp.JSSourceFile;

public class JavascriptResourceGenerator extends AbstractResourceGenerator {
	/**
	 * Java compiler has a limit of 2^16 bytes for encoding string constants in a class file. Since the max size of a
	 * character is 4 bytes, we'll limit the number of characters to (2^14 - 1) to fit within one record.
	 */
	private static final int MAX_STRING_CHUNK = 16383;

	@Override
	public String createAssignment(TreeLogger logger, ResourceContext context, JMethod method)
			throws UnableToCompleteException {
		URL[] resources = ResourceGeneratorUtil.findResources(logger, context, method);

		if (resources.length != 1) {
			logger.log(TreeLogger.ERROR, "Exactly one resource must be specified", null);
			throw new UnableToCompleteException();
		}

		URL resource = resources[0];

		SourceWriter sw = new StringSourceWriter();
		// Write the expression to create the subtype.
		sw.println("new " + JavascriptResource.class.getName() + "() {");
		sw.indent();

		// Convenience when examining the generated code.
		sw.println("// " + resource.toExternalForm());

		Minification minification = method.getAnnotation(JavascriptResource.Minification.class);

		// Methods defined by JavascriptResource interface
		writeEnsureInjected(sw);
		writeGetText(minification, sw, resource);
		writeGetName(sw, method);

		sw.outdent();
		sw.println("}");

		return sw.toString();
	}

	private void writeEnsureInjected(SourceWriter sw) {
		sw.println("private boolean injected;");
		sw.println("public boolean ensureInjected() {");
		sw.indent();
		sw.println("if (!injected) {");
		sw.indentln("injected = true;");
		sw.indentln(JavascriptInjector.class.getName() + ".inject(getText());");
		sw.indentln("return true;");
		sw.println("}");
		sw.println("return false;");
		sw.outdent();
		sw.println("}");
	}

	private void writeGetText(Minification minification, SourceWriter sw, URL resource)
			throws UnableToCompleteException {
		sw.println("public String getText() {");
		sw.indent();

		String toWrite = Util.readURLAsString(resource);

		toWrite = maybeMinify(minification, resource.toExternalForm(), toWrite);

		if (toWrite.length() > MAX_STRING_CHUNK) {
			writeLongString(sw, toWrite);
		} else {
			sw.println("return \"" + Generator.escape(toWrite) + "\";");
		}
		sw.outdent();
		sw.println("}");
	}

	private String maybeMinify(Minification minification, String fileName, String js) {
		// TODO: Error handling
		// TODO: Don't bother if it's small anyway??
		// TODO: Use heuristics to check if already minified (look for comments or whitespace?)
		// TODO: Skip in hosted mode?
		// TODO: Make external?
		// TODO: When external, bundle? (Like images?)

		boolean minify = true;
		if (minification != null) {
			minify = minification.minify();
		}

		if (minify) {
			Compiler compiler = new Compiler();
			JSSourceFile input;
			try {
				input = JSSourceFile.fromInputStream(fileName, new StringInputStream(js));
			} catch (IOException e) {
				throw new IllegalStateException();
			}
			JSSourceFile[] externs = new JSSourceFile[0];
			JSSourceFile[] inputs = { input };
			CompilerOptions options = new CompilerOptions();
			CompilationLevel.SIMPLE_OPTIMIZATIONS.setOptionsForCompilationLevel(options);
			/* Result result = */compiler.compile(externs, inputs, options);

			return compiler.toSource();
		} else {
			return js;
		}
	}

	private void writeGetName(SourceWriter sw, JMethod method) throws UnableToCompleteException {
		sw.println("public String getName() {");
		sw.indent();
		sw.println("return \"" + method.getName() + "\";");
		sw.outdent();
		sw.println("}");
	}

	/**
	 * A single constant that is too long will crash the compiler with an out of memory error. Break up the constant and
	 * generate code that appends using a buffer.
	 */
	private void writeLongString(SourceWriter sw, String toWrite) {
		sw.println("StringBuilder builder = new StringBuilder();");
		int offset = 0;
		int length = toWrite.length();
		while (offset < length - 1) {
			int subLength = Math.min(MAX_STRING_CHUNK, length - offset);
			sw.print("builder.append(\"");
			sw.print(Generator.escape(toWrite.substring(offset, offset + subLength)));
			sw.println("\");");
			offset += subLength;
		}
		sw.println("return builder.toString();");
	}
}
