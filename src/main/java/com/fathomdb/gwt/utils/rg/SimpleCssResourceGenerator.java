package com.fathomdb.gwt.utils.rg;

import java.net.URL;

import com.fathomdb.gwt.utils.resources.SimpleCssResource;
import com.google.gwt.core.ext.Generator;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.dev.util.Util;
import com.google.gwt.dom.client.StyleInjector;
import com.google.gwt.resources.ext.AbstractResourceGenerator;
import com.google.gwt.resources.ext.ResourceContext;
import com.google.gwt.resources.ext.ResourceGeneratorUtil;
import com.google.gwt.user.rebind.SourceWriter;
import com.google.gwt.user.rebind.StringSourceWriter;

public class SimpleCssResourceGenerator extends AbstractResourceGenerator {
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
		sw.println("new " + SimpleCssResource.class.getName() + "() {");
		sw.indent();

		// Convenience when examining the generated code.
		sw.println("// " + resource.toExternalForm());

		// Methods defined by JavascriptResource interface
		writeEnsureInjected(sw);
		writeGetText(sw, resource);
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
		sw.indentln(StyleInjector.class.getName() + ".inject(getText());");
		sw.indentln("return true;");
		sw.println("}");
		sw.println("return false;");
		sw.outdent();
		sw.println("}");
	}

	private void writeGetText(SourceWriter sw, URL resource) throws UnableToCompleteException {
		sw.println("public String getText() {");
		sw.indent();

		String toWrite = Util.readURLAsString(resource);

		toWrite = maybeMinify(resource.toExternalForm(), toWrite);

		if (toWrite.length() > MAX_STRING_CHUNK) {
			writeLongString(sw, toWrite);
		} else {
			sw.println("return \"" + Generator.escape(toWrite) + "\";");
		}
		sw.outdent();
		sw.println("}");
	}

	private String maybeMinify(String fileName, String css) {
		StringBuilder out = new StringBuilder();

		char previous = 0;
		for (int i = 0; i < css.length(); i++) {
			char c = css.charAt(i);
			switch (c) {
			case ' ':
			case '\r':
			case '\n':
			case '\t': {
				switch (previous) {
				case ' ':
				case '\r':
				case '\n':
				case '\t':
					// Ignore multiple whitespace
					break;

				case ';':
				case ':':
				case ',':
				case '{':
				case '}':
					// Space not needed after this
					break;

				default:
					previous = ' ';
					out.append(' ');
					break;
				}
				continue;
			}

			case '*': {
				if (previous == '/') {
					char nprev = 0;
					while (true) {
						i++;
						char n = css.charAt(i);
						if (n == '/' && nprev == '*') {
							// Remove the '/' we already output
							out.setLength(out.length() - 1);
							break;
						}
						nprev = n;
					}
					// Clear the state (could be improved)
					previous = 0;
				} else {
					previous = c;
					out.append(c);
				}
				break;
			}

			default: {
				previous = c;
				out.append(c);
				break;
			}
			}
		}

		// We're missing a few things...
		// 1) We'll still have spaces after this:
		// /*aa*/ /*aa*/ /*aa*/

		// 2) We don't trim spaces before a {: " {" => "{"

		// 3) We don't remove the final semi-comma before a {: ";} => "}"

		return out.toString();
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
