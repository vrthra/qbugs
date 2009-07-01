package zbug;
import javax.xml.xpath.*;
import javax.xml.parsers.*;
import org.w3c.dom.*;
import org.xml.sax.*;
import java.io.*;
import java.util.*;

public class Xq {
    static XPathFactory  factory = XPathFactory.newInstance();
    public static Xq init(String xml) {
        return new Xq(xml);
    }
    InputSource inputSource;
    XPath xpath;
    public Xq(String xml) {
        try {
            inputSource = new InputSource(new StringReader(xml));
            xpath=factory.newXPath();
        } catch (Exception e) {
            throw new Zx("Xq:" +e.getMessage());
        }
    }
    public NodeList qnodes(String path) {
        try {
            return (NodeList) xpath.evaluate(path, inputSource, XPathConstants.NODESET);
        } catch (Exception e) {
            Zx.err(e);
            throw new Zx("Xq:qnodes:" +e.getMessage());
        }
    }
    /*public String query(String path) {
        try {
            return xpath.evaluate(path, inputSource);
        } catch (Exception e) {
            throw new Zx("Xq:query " +e.getMessage());
        }
    }*/
    public static String get(Node nl, String s) {
        NodeList l = nl.getChildNodes();
        for(int i = 0; i < l.getLength();++i) {
            Node n = l.item(i);
            if (n.getNodeName().equalsIgnoreCase(s)) return decode(n.getFirstChild().getNodeValue()); //encode?
        }
        return "";
    }

    public static String nodeToString(Node n) {
        try {
            StringBuffer sb = new StringBuffer();
            _xml(n, sb);
            return sb.toString();
        } catch (Exception e) {
            throw new Zx("Xq:nodetostr:" + e.getMessage());
        }
    }

    public static String encode(String text) {
        if (text == null) return null;
        int[] chars = {38, 60, 62, 34, 61, 39};
        for(int i=0;i<chars.length-1;i++){
            text = text.replaceAll(String.valueOf((char)chars[i]),
                    "&#"+chars[i]+";");
        }
        return text;
    }

    public static String decode(String text) {
        if (text == null) return null;
        int[] chars = {38, 60, 62, 34, 61, 39};
        for(int i=0;i<chars.length-1;i++) {
            text = text.replaceAll( "&#"+chars[i]+";", String.valueOf((char)chars[i]));
        }
        return text;
    }

    static void _xml(Node node, StringBuffer sb) {
        if (node.getNodeType() == Node.TEXT_NODE) {
            String s = node.getNodeValue();
            if (s != null && s.length() > 0) 
                sb.append(encode(s));
        } else {
            String name = node.getNodeName();
            sb.append("<" + name + ">");
            NodeList lst = node.getChildNodes();
            for (int i = 0; i < lst.getLength(); i++) {
                Node n = lst.item(i);
                _xml(n, sb);
            }
            sb.append("</" + name + ">");
        }
    }

    public static String nodeToRep(Node n) {
        try {
            StringBuffer sb = new StringBuffer();
            _rep(n, sb);
            return sb.toString();
        } catch (Exception e) {
            throw new Zx("Xq:nodetorep:" + e.getMessage());
        }
    }
    
    public static String getText(Node n) {
        try {
            StringBuffer sb = new StringBuffer();
            _text(n, sb);
            return decode(sb.toString());
        } catch (Exception e) {
            throw new Zx("Xq:gettext:" + e.getMessage());
        }

    }

    public static void _text(Node node, StringBuffer sb) {
        if (node.getNodeType() == Node.TEXT_NODE) {
            String s = node.getNodeValue();
            if (s != null && s.length() > 0) 
                sb.append(encode(s));
        } else {
            NodeList lst = node.getChildNodes();
            for (int i = 0; i < lst.getLength(); i++) {
                Node n = lst.item(i);
                _text(n, sb);
            }
            sb.append(" ");
        }
    }

    static void _rep(Node node, StringBuffer sb) {
        if (node.getNodeType() == Node.TEXT_NODE) {
            String s = node.getNodeValue();
            if (s != null && s.length() > 0) 
                sb.append(decode(s));
        } else {
            String name = node.getNodeName();
            sb.append("[" + name + ": ");
            NodeList lst = node.getChildNodes();
            for (int i = 0; i < lst.getLength(); i++) {
                Node n = lst.item(i);
                _rep(n, sb);
            }
            sb.append(" ] ");
        }
    }


    public static void sort(Node node, boolean descending, Vector<String> keys) {
        String key = keys.get(0).substring(1); // for now.

        List nodes = new ArrayList();
        NodeList lst = node.getChildNodes();
        if (lst.getLength() > 0) {
            for (int i = 0; i < lst.getLength(); i++) {
                Node n = lst.item(i);
                // Remove empty text nodes
                if ((!(n instanceof Text)) || (n instanceof Text && ((Text) n).getTextContent().trim().length() > 1))
                    nodes.add(n);
            }
            Comparator comp = new BugsterComparator(key);
            //if descending is true, get the reverse ordered comparator
            if (descending) Collections.sort(nodes, Collections.reverseOrder(comp));
            else Collections.sort(nodes, comp);

            for (Iterator iter = nodes.iterator(); iter.hasNext();) node.appendChild((Node)iter.next());
        }

    }
}

class BugsterComparator implements Comparator {
    String key = null;
    public BugsterComparator(String key) {
        this.key = key;
    }
    public int compare(Object a, Object b) {
        String s1 = Xq.get((Node) a, key);
        String s2 = Xq.get((Node) b, key);
        return (s1.compareToIgnoreCase(s2));
    }
}

