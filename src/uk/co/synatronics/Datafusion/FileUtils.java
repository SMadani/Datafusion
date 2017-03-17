package uk.co.synatronics.Datafusion;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.validation.SchemaFactory;
import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class FileUtils {

    public static boolean validateDocument(File xsd, Source xml) {
        try {
            SchemaFactory.newInstance("http://www.w3.org/2001/XMLSchema").newSchema(xsd).newValidator().validate(xml);
            return true;
        }
        catch (SAXException | IOException ex) {
            System.err.println(ex.getMessage());
            return false;
        }
    }

    public static Document docFromString(String xml) {
        try {
            return DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
        }
        catch (Exception error) {
            error.printStackTrace();
            return null;
        }
    }

    public static Document docFromFile(String filename) {
        try {
            return DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new FileInputStream(new File(filename)));
        }
        catch (Exception error) {
            error.printStackTrace();
            return null;
        }
    }

    public static Document docFromURL (URL url) {
        return url != null ? docFromURL(url.toExternalForm()) : null;
    }

    public static Document docFromURL (String url) {
        try {
            return DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new URL(url).openStream());
        }
        catch (Exception error) {
            error.printStackTrace();
            return null;
        }
    }

    public static String toXML(Node node) {
        try {
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
            ByteArrayOutputStream xml = new ByteArrayOutputStream();
            transformer.transform(new DOMSource(node), new StreamResult(xml));
            return xml.toString();
        }
        catch (TransformerException te) {
            return null;
        }
    }

    protected static String getExtension (String uri) {
        int dotIndex = uri.lastIndexOf('.');
        return (dotIndex == -1) ? "" : uri.substring(dotIndex+1);
    }

    protected static String inferFileName(URL url) {
        String fileName = null;
        if (url != null) try {
            fileName = url.getFile();
            if (fileName.length() > 1) {
                int lastSlash = fileName.lastIndexOf('/');
                if (lastSlash >= 0)
                    fileName = fileName.substring(lastSlash+1, fileName.length());
                String extension = getExtension(url.toString());
                if (extension != null && !extension.isEmpty()) {
                    int extIndex = fileName.indexOf(extension);
                    if (extIndex > 1)
                        fileName = fileName.substring(0, extIndex-1);
                }
            }
        } catch (Exception e) {
            System.err.println("Could not infer file name for URL \""+url+"\": "+e.getMessage());
        }
        return fileName;
    }

    protected static String readFile (File source) {
        if (source != null && source.canRead()) {
            StringBuilder sb = new StringBuilder();
            try {
                BufferedReader br = new BufferedReader(new FileReader(source));
                String line = br.readLine();
                while (line != null) {
                    sb.append(line).append(System.getProperty("line.separator", "\n"));
                    line = br.readLine();
                }
                br.close();
                return sb.toString();
            }
            catch (IOException ioe) {
                System.err.println("Could not read file \""+source.getAbsolutePath()+"\": "+ioe.getMessage());
            }
        }
        return null;
    }

    protected static String readURL(URL site) {
        return site != null ? readURL(site.toExternalForm()) : "";
    }

    protected static String readURL(String site) {
        StringBuilder buff = new StringBuilder();
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(site).openConnection();
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)");
            conn.connect();
            BufferedReader bfr = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String inputLine;
            while ((inputLine = bfr.readLine()) != null)
                buff.append(inputLine).append('\n');
            bfr.close();
        }
        catch (IOException ioe) {
            System.err.println("Failed to read from \""+site+"\": "+ioe.getMessage());
        }
        return buff.toString();
    }

    protected static String find (String input, String expr, int occurN, int after) throws StringIndexOutOfBoundsException {
        String srch = "";
        int index = 0;
        for (int i = 0; i < occurN; i++) {
            index = input.indexOf(expr, index);
            if (index < 0)
                break;
            index += expr.length();
            srch = input.substring (index, index+after);
        }
        return srch;
    }

    protected static Set<String> find (String input, Pattern regex)  {
        Set<String> result = new HashSet<>();
        Matcher match = regex.matcher(input);
        while (match.find())
            result.add (input.substring(match.start(), match.end()));
        return result;
        //regex.matcher(raw).findAll().toSet()
    }

    static void forceAgentHeader(final String header) throws Exception {
        final Class<?> clazz = Class.forName("sun.net.www.protocol.http.HttpURLConnection");
        final Field field = clazz.getField("userAgent");
        field.setAccessible(true);
        Field modifiersField = Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);
        field.set(null, header);
    }
}
