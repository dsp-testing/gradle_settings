package org.commcare.formplayer.util;

import java.io.StringReader;
import java.io.StringWriter;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

/**
 * Utility methods for dealing with XML
 */
public class XmlUtils {
    /**
     * Given a String representation of a valid XML document, this returns an indented version of
     * it.
     *
     * @param xml - A string XML document
     * @return A string XML document that is indented
     */
    public static String indent(String xml) {
        Transformer transformer;
        try {
            transformer = TransformerFactory.newInstance().newTransformer();
        } catch (TransformerConfigurationException e) {
            throw new RuntimeException(e);
        }
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");


        Source xmlInput = new StreamSource(new StringReader(xml));
        StreamResult result = new StreamResult(new StringWriter());

        try {
            transformer.transform(xmlInput, result);
        } catch (TransformerException e) {
            throw new RuntimeException(e);
        }
        return result.getWriter().toString();
    }
}
