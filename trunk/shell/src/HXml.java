package zbug;
import org.xml.sax.SAXException;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;
import javax.xml.parsers.*;
import java.util.*;
import java.io.*;

public class HXml extends DefaultHandler {
    private StringBuffer textContent;
    private Stack <Map<String, Vector>> pstack;
    private Map<String, Vector> last;

    public HXml(String fn) {
        try {
               SAXParserFactory factory = SAXParserFactory.newInstance();
               factory.setFeature("http://xml.org/sax/features/validation", false); 
               factory.setFeature("http://apache.org/xml/features/validation/schema", false);
               factory.setFeature("http://apache.org/xml/features/validation/schema-full-checking", false); 
               factory.setValidating(false);
               SAXParser parser = factory.newSAXParser();

               this.pstack = new Stack();
               pstack.push(new LinkedHashMap<String, Vector>()); // attributes and children
               parser.parse(new ByteArrayInputStream(fn.getBytes()), this);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
            throw new Zx("xmlparse:" + e.getMessage());
        }
    }

    public void startElement(String uri, String local, String qname,
            Attributes atts) throws SAXException {
        pstack.push(new LinkedHashMap<String, Vector>()); // attributes and children
        for (int i=0; i<atts.getLength(); i++) 
            add("@"+atts.getQName(i), atts.getValue(i));
        this.textContent = new StringBuffer();
    }

    public void endElement(String uri, String local, String qname)
        throws SAXException {
        last = pstack.peek();
        pstack.pop(); // attributes and children
        if (last.keySet().size() != 0 ) {
            add(qname, last);
        } else {
            if (this.textContent != null) 
                add(qname, this.textContent.toString());
        }
        this.textContent = null;
    }

    public void characters(char[] ch, int start, int length) throws SAXException {
        if (this.textContent != null) {
            this.textContent.append(ch, start, length);
        }
    }

    Map<String, Vector> getRoot() {
        if (pstack.size() > 1) throw new Zx("hxml:Something wrong with xml");
        if (last == null) throw new Zx("hxml: elements not found");
        return last;
    }

    Map<String, Vector> current() {
        return this.pstack.peek();
    }

    void add(String key, Object value) {
        Vector v = this.current().get(key);
        if (v == null) {
            v = new Vector();
            this.current().put(key,v);
        }
        v.add(value);
    }

/*    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.err.println("Usage: java zbug.HXml file");
            System.exit(1);
        }
        HXml theConfig = new HXml(args[0]);
        LinkedHashMap ht = theConfig.getLinkedHashMap();
        for (Enumeration e = ht.keys(); e.hasMoreElements(); ){
            String key = (String)e.nextElement();
            System.out.println(key+"="+ht.get(key));
        }
    }*/
}

