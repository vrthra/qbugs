package zbug;
import java.io.*;
import java.util.*;
import java.util.regex.*;
import jline.*;
import com.sun.sunit.bugtraq.client.cli.*;
import com.sun.sunit.bugtraq.client.sample.*;
import com.sun.sunit.bugtraq.client.common.*;
import javax.xml.transform.*;
import javax.xml.transform.stream.*;
import org.w3c.dom.*;
import com.sun.sunit.webservice.soap.SOAPServiceMessage;


abstract class ZHolder {
    Vector<String> valid = new Vector<String>();
    abstract ZInst create();
    abstract String name();
    void _reg(HashMap<Integer, ZHolder> lib, String cmd) {
        lib.put(ZBug.sym(cmd), this);
    }
    public void reg(HashMap<Integer, ZHolder> lib) {
        _reg(lib, name());
    }
    public void add_validopt(String o) {
        valid.add(o);
    }
    abstract String tag();
    abstract String help();
    abstract Action type();
    abstract String[] get_validopt();
}

class ArgFilter {
    static String[] statusStr = {
        "1-Dispatched",
        "2-Incomplete",
        "3-Accepted",
        "4-Defer",
        "5-Cause Known",
        "6-Fix Understood",
        "7-Fix in Progress",
        "8-Fix Available",
        "9-Fix Failed",
        "10-Fix Delivered",
        "11-Closed"
    };
    static String[] substatusStr = {
        "Code Freeze",
        "Duplicate",
        "Empty Fields",
        "Filing Error",
        "Future Project",
        "Need More Info",
        "Needs Verification",
        "No Plan to Fix",
        "No Resource Available",
        "Not a Defect",
        "Not Reproducible",
        "Other",
        "Other Values",
        "Too Risky to Fix",
        "Unverified",
        "Verification Not Applicable",
        "Verification Not Needed",
        "Verified",
        "Will Not Fix"
    };

    static String[] priority = {
        "1-Very High",
        "2-High",
        "3-Medium",
        "4-Low",
        "5-Very Low"
    };


    public static String filter(String name, String arg, String opt) {
        try {
        if (opt.equals("-")) opt = "";
        if (name.equals("status") || (name.equals("*set") && arg.equals("-status"))) {
            if (opt.indexOf(',') == -1) return statusStr[Integer.parseInt(opt)-1];
            return opt;
        } else if (name.equals("substatus") || (name.equals("*set") && arg.equals("-substatus"))) {
            if (opt.indexOf(',') == -1) return substatusStr[Integer.parseInt(opt)-1];
            return opt;
        } else if (name.equals("priority") || (name.equals("*set") && arg.equals("-priority"))) {
            if (opt.indexOf(',') == -1) return priority[Integer.parseInt(opt)-1];
            return opt;
        } else if (name.equals("impact") || (name.equals("*set") && arg.equals("-impact"))) {
            char c = opt.charAt(0);
            return ("" + c).toUpperCase() + opt.substring(1);
        } else if (name.equals("functionality") || (name.equals("*set") && arg.equals("-functionality"))) {
            char c = opt.charAt(0);
            return ("" + c).toUpperCase() + opt.substring(1);
        } else if (name.equals("area") || (name.equals("*set") && arg.equals("-area"))) {
            if (!opt.equalsIgnoreCase("Defect") && !opt.equalsIgnoreCase("RFE"))
                throw new Zx("area should be Defect or RFE (" + opt + ")");
            char c = opt.charAt(0);
            return ("" + c).toUpperCase() + opt.substring(1);
        } else if (name.equals("responsibleengineer")||
                name.equals("responsiblemanager") ||
                name.equals("submittedby") ||
                name.equals("category") ||
                name.equals("subcategory") ||
                name.equals("product"))
        {
            return opt.toLowerCase();
        }
        } catch (Exception e) {
            ZBug.err(e);
        }
        return opt;
    }
}
class CField {
    String color;
    String field;
    Pattern regexp;
}

enum Action { T_QUERY, T_SHOW, T_Q_RESULT_XML, T_PRINT, T_UPDATE, T_LINES, T_NONE };

// We go through create so that at a future time, the shell can
// invoke commands that holds private state just like normal shell commands.
abstract class ZInst {
    ZHolder _template = null;
    ZInst parent;
    Vector<String> args;

    ZHolder t() {return _template;}
    abstract String invoke();

    String name() {return t().name();}
    String tag() {return t().tag();}
    Action type() {return t().type();}

    int argsize() {return args.size();}
    String getarg(int i) {
        if (i>= args.size()) throw new Zx("Need argument " + i + " for " + name() + "(" + tag() + ")");
        return args.get(i);
    }

    String getopt(int i) { return ArgFilter.filter(name(), getarg(i), getarg(i+1)); }

    Vector<String> getlov(int i) {
        Vector<String> v = new Vector<String>();
        String name = name();
        String arg = getarg(i);
        String[] opts = getarg(i+1).split(",");
        for(String o: opts) v.add(ArgFilter.filter(name, arg, o));
        return v;
    }

    ZInst(ZHolder t) {
        _template = t;
        args = new Vector<String>();
    }
    void parent(ZInst p) {
        parent = p;
    }
    void add(String arg) {
        args.add(arg);
    }
    String mysrc() {
        StringBuffer arg = new StringBuffer();
        arg.append(name() + " ");
        for(String s: args) arg.append(s + " ");
        return arg.toString();
    }
    String src() {
        String res = "";
        if (parent != null) res = parent.src() + "|";
        return res + mysrc();
    }
    
    public HashMap<String,String> get_selectedopts() {
        HashMap<String,String> opts = new HashMap<String,String>();
        for(int i=0; i< args.size();) {
            String s = getarg(i);
            if (s.charAt(0) == '-') {
                for(String o: t().get_validopt()) {
                    if (o.toLowerCase().startsWith(s))
                        opts.put(o.substring(1), getopt(i));
                }
            } else {
                break;
                //throw new Zx("Unknown option " + s);
            }
            i += 2;
        }
        return opts;
    }

    String t_(String t) {return "<" + t + ">";}
    String _t(String t) {return "</" + t + ">";}

    String withtag(String t, String val) {return t_(t) + val + _t(t);}
    String wrap(String t) {return withtag(tag(), t);}
    String tag_() {return t_(tag());}
    String _tag() {return _t(tag());}

    String eq(String val) { return withtag("EqualTo", val); }
    String noteq(String val) { return withtag("NotEqualTo", val); }
    String gt(String val) { return withtag("GreaterThan", val); }
    String gteq(String val) { return withtag("GreaterThanEqualTo", val); }
    String lt(String val) { return withtag("LesserThan", val); }
    String lteq(String val) { return withtag("LesserThanEqualTo", val); }
    String like(String val) { return withtag("Like", val); }
    String empty(String val) { return withtag("Null", val); }
    String notempty(String val) { return withtag("NotNull", val); }
    String notlike(String val) { return withtag("NotLike", val); }
    String in(String val) { return withtag("In", val); }
    String notin(String val) { return withtag("NotIn", val); }
    String between(String val) { return withtag("BetWeen", val); }

    String value(String val) { return withtag("Value", val); }

    static String[] lc(String[] str) {
        Vector<String> st = new Vector<String>();
        for(String s : str) st.add(s.toLowerCase());
        return st.toArray(new String[0]);
    }
}

class BugsterSkelCmd extends ZHolder {
    String my_tag = null;
    String[] aliases = null;
    public BugsterSkelCmd(String tag) { my_tag = tag; }
    public String name() {return my_tag.toLowerCase();}
    public Action type() {return Action.T_QUERY;}
    public String tag() {return my_tag;}
    public String help() { return "adds the condition " + tag() + " to the bugster query"; }

    public String[] get_validopt() {
        return new String[] {"-eq", "-lt", "-gt", "-like", "-in", "-notin", "-between", "-noteq", "-notlike", "-empty", "-notempty","-gteq", "-lteq"};
    }

    public ZInst create() {
        return new ZInst(this) {

            public String invoke() {
                ZBug.ac();
                StringBuffer lres = new StringBuffer();
                String res = new String();
                int start = 0;
                String p = "";
                if (parent != null) p = parent.invoke();
                while(start < args.size()) {
                    String opt = getarg(start);
                    String v = getopt(start);
                    //http://bt2ws.central.sun.com:8088/services/schemas/cr-query
                    //Between, EqualTo, GreaterThan, GreaterThanEqualTo,In, IsNotNull,IsNull, LessThan,
                    //LessThanEqualTo,Like,NotEqualTo,NotIn,NotLike,Or
                    if (opt.equals("-eq")) res = eq(v);
                    else if (opt.equals("-noteq")) res = noteq(v);
                    else if (opt.equals("-gt")) res = gt(v);
                    else if (opt.equals("-gteq")) res = gteq(v);
                    else if (opt.equals("-lt")) res = lt(v);
                    else if (opt.equals("-lteq")) res = lteq(v);
                    else if (opt.equals("-like")) res = like(v);
                    else if (opt.equals("-empty")) res = empty(v);
                    else if (opt.equals("-notempty")) res = notempty(v);
                    else if (opt.equals("-notlike")) res = notlike(v);
                    else if (opt.equals("-in")) {
                        StringBuffer b = new StringBuffer();
                        for(String s : getlov(start)) b.append(value(s));
                        res = in(b.toString());
                    } else if (opt.equals("-notin")) {
                        StringBuffer b = new StringBuffer();
                        for(String s : getlov(start)) b.append(value(s));
                        res = notin(b.toString());
                    } else if (opt.equals("-between")) {
                        StringBuffer b = new StringBuffer();
                        for(String s : getlov(start)) b.append(value(s));
                        res = between(b.toString());
                    } else throw new Zx("Unknown option " + opt);
                    start += 2;
                    lres.append(res);
                }
                return p + "\n" + wrap(lres.toString());
            }
        };
    }
}

public class ZLib {
    static ZHolder _do =  new ZHolder() {
        public String name() {return ";";}
        public String tag() {return ";";}
        public Action type() {return Action.T_NONE;}
        public String help() {return "force the execution of commands before.";}
        public String[] get_validopt() { return new String[0]; }

        public ZInst create() {
            return new ZInst(this) {
                public String invoke() {
                    ZInst cmd = parent;
                    if (cmd != null) {
                        if (cmd.type() == Action.T_QUERY) {
                            // a little bit of helping
                            ZInst query = _query.create();
                            ZInst exec = _exec.create();
                            ZInst show = _show.create();
                            query.parent(cmd);
                            exec.parent(query);
                            show.parent(exec);
                            cmd = show;
                        } else if (cmd.type() == Action.T_Q_RESULT_XML) {
                            ZInst show = _show.create();
                            show.parent(cmd);
                            cmd = show;
                        } else if (cmd.type() == Action.T_UPDATE) {
                            ZInst showup = _showupdatexml.create();
                            showup.parent(cmd);
                            cmd = showup;
                        } else if (cmd.type() == Action.T_SHOW) {
                            ZInst showcr = _fmtcr.create();
                            showcr.parent(cmd);
                            cmd = showcr;
                        }

                        ZBug.set_last(cmd.invoke());
                        if (ZBug._more) {
                            ZBug.show_more("");
                            return "";
                        }
                        return ZBug._lstText;
                    } else return ZBug._lstText;
                }
            };
        }
    };

    static ZHolder _exec =  new ZHolder() {
        public String name() {return "*exec";}
        public String tag() {return "";}
        public Action type() {return Action.T_NONE;}
        public String help() {return "execute the bugster command in the pipeline";}
        public String[] get_validopt() { return new String[0]; }
        public void reg() { /* dont register.*/ }

        public ZInst create() {
            return new ZInst(this) {
                public String query(String q) {
                    try {
                        // redirect stdout; (gods above! forgive me)
                        ByteArrayOutputStream tmp = new ByteArrayOutputStream();
                        PrintStream ps = new PrintStream(tmp);

                        QueryChangeRequest cr = new QueryChangeRequest();
                        cr.setToken(token());
                        cr.query(q);
                        System.setOut(ps);
                        cr.process(ZBug.ia);
                        InputStream is =cr.resultStream();
                        _lastXml = ZBug.readstream(is);
                        return _lastXml;
                    } catch (Exception e) {
                        ZBug.err(e);
                        throw new Zx("query:" + e.getMessage());
                    } finally {
                        System.setOut(ZBug.out);
                    }
                }
                public String invoke() {
                    return query(parent.invoke());
                }
            };
        }
    };

    static ZHolder _query =  new ZHolder() {
        public String name() {return "*query";}
        public String tag() {return "*query";}
        public Action type() {return Action.T_NONE;}
        public String help() {return "create a bugster query with conditions in the pipeline";}
        public String[] get_validopt() { return new String[0]; }
        public void reg() { /* dont register.*/ }
        public ZInst create() {
            return new ZInst(this) {
                public String invoke() {
                    if (parent == null) throw new Zx("query should not be used as the first component.");
                    String[] strs = ZBug.split_lines(parent.invoke());
                    Arrays.sort(strs);
                    StringBuffer b = new StringBuffer();
                    for(String s: strs)
                        b.append(s + "\n");
                    return withtag("QueryChangeRequest", withtag("ChangeRequest", b.toString()));
                }
            };
        }
    };
    
    static int[][] statusmap = {
        {0,0,0, 0,0,0},
        {3,4,6,12,13,0},
        {0,0,0, 0,0,0},
        {14,1,9,5,8,0},
        {0,0,0, 0,0,0},
        {0,0,0, 0,0,0},
        {0,0,0, 0,0,0},
        {16,17,7,0, 0, 0},
        {0,0,0, 0,0,0},
        {18,15,17,16,7, 0},
        {18,2,15,10,19,11}
        };

    static ZHolder _lststatus =  new ZHolder() {
        public String name() {return "?status";}
        public String tag() {return "?status";}
        public Action type() {return Action.T_NONE;}
        public String help() {return "list the status and substatus";}
        public String[] get_validopt() { return new String[] {}; }
        public ZInst create() {
            return new ZInst(this) {
                public String invoke() {
                    HashMap<String,String> map = get_selectedopts();
                    String s = getarg(0);
                    if (s != null) {
                        try {
                            int index = Integer.parseInt(s);
                            String str = ArgFilter.statusStr[index - 1];
                            int[] stm = statusmap[index - 1];
                            StringBuffer sb = new StringBuffer();
                            sb.append("[" + str + "]\n");
                            for(int i : stm) {
                                if (i == 0) break;
                                sb.append("" + (i)+"-"+ ArgFilter.substatusStr[i-1] + "\n");
                            }
                            return sb.toString();
                        } catch (Exception e) {
                            return "status error:" + e.getMessage();
                        }
                    } else {
                        StringBuffer sb = new StringBuffer();
                        for (String str : ArgFilter.statusStr) {
                            sb.append(str + "\n");
                        }
                        return sb.toString();
                    }
                }
            };
        }
    };

    static ZHolder _lstrel =  new ZHolder() {
        public String name() {return "?release";}
        public String tag() {return "?release";}
        public Action type() {return Action.T_NONE;}
        public String help() {return "list releases and corresponding builds";}
        public String[] get_validopt() { return new String[] {"-release", "-build"}; }
        public Map<String,String> optmap = new HashMap<String,String>();
        String get(String key, Map<String, Vector> hs ) {
            Vector v = hs.get(key);
            if (v == null || v.size() == 0) return "";
            return (String) v.get(0);
        }
        public String fmt_release(Vector v, Map<String,String> opts) {
            StringBuffer sb = new StringBuffer();
            String rel = opts.get("-release");
            String bld = opts.get("-build");
            Pattern prel = Pattern.compile(rel == null? ".*" : rel , Pattern.CASE_INSENSITIVE);
            Pattern pbld = Pattern.compile(bld == null? ".*" : bld, Pattern.CASE_INSENSITIVE);
            try {
                for (Object o : v) {
                    Map<String, Vector> h =(Map<String, Vector>) o;
                    String name = get("Name", h);
                    Matcher m = prel.matcher(name);
                    if (!m.find()) continue;
                    if ((rel != null) && (bld != null) ) {
                        Vector vect = h.get("Build");
                        if (vect == null) continue;
                        StringBuffer subbuf = new StringBuffer();
                        for (Object ob : vect) {
                            Map<String, Vector> hs =(Map<String, Vector>) ob;
                            String n = get("Name", hs);
                            Matcher mb = pbld.matcher(n);
                            if (!mb.find()) continue;
                            subbuf.append("  " +n);
                        }
                        if (subbuf.length() > 0) {
                            sb.append(name + "\n");
                            sb.append(subbuf.toString() + "\n");
                        }
                    } else {
                        sb.append(name + ", ");
                    }
                }
            } catch (Exception e) {
                throw new Zx("lstcat: " + e.getMessage());
            }
            return sb.toString();
        }
        public ZInst create() {
            return new ZInst(this) {
                public String invoke() {
                    String[] ss = parent.invoke().split("\n");
                    /*for(String s: get_validopt())
                        optmap.put(s, ".*");*/
                    for(int i = 0; i < argsize() ; i+= 2) {
                        optmap.put(getarg(i), getopt(i));
                    }
                    StringBuffer sb = new StringBuffer();
                    for (String s : ss) {
                        QueryProduct qp = new QueryProduct(s);
                        qp.setToken(token());
                        qp.process(ZBug.ia);
                        String res = qp.result();
                        HXml thexml = new HXml(res);
                        Map<String, Vector> ht = thexml.getRoot();
                        ht = (Map<String, Vector>) ht.get("SOAP-ENV:Body").get(0);
                        ht = (Map<String, Vector>) ht.get("ProductQueryService:QueryProduct").get(0);
                        ht = (Map<String, Vector>) ht.get("Product").get(0);
                        Vector v = ht.get("Release");
                        String r =  fmt_release(v, optmap);
                        sb.append(r + "\n");
                    }
                    return sb.toString();
                }
            };
        }
    };


    static ZHolder _lstcat =  new ZHolder() {
        public String name() {return "?category";}
        public String tag() {return "?category";}
        public Action type() {return Action.T_NONE;}
        public String help() {return "list category and subcategory <*for webstack solaris |?category -initialevaluator $me>";}
        public String[] get_validopt() { return new String[] {"-category", "-subcategory", "-initialevaluator", "-responsiblemanager"}; }
        public Map<String,String> optmap = new HashMap<String,String>();
        String get(String key, Map<String, Vector> hs ) {
            Vector v = hs.get(key);
            if (v == null || v.size() == 0) return "";
            return (String) v.get(0);
        }
        public String fmt_product(Vector v, Map<String,String> opts) {
            StringBuffer sb = new StringBuffer();
            String cat = opts.get("-category");
            String subcat = opts.get("-subcategory");
            String eng = opts.get("-initialevaluator");
            String mangr = opts.get("-responsiblemanager");
            Pattern pcat = Pattern.compile(cat, Pattern.CASE_INSENSITIVE);
            Pattern psubcat = Pattern.compile(subcat, Pattern.CASE_INSENSITIVE);
            Pattern peng = Pattern.compile(eng, Pattern.CASE_INSENSITIVE);
            Pattern pmgr = Pattern.compile(mangr, Pattern.CASE_INSENSITIVE);
            try {
                for (Object o : v) {
                    Map<String, Vector> h =(Map<String, Vector>) o;
                    String name = get("Name", h);
                    Matcher m = pcat.matcher(name);
                    if (!m.find()) continue;
                    String desc = get("Description", h);
                    Vector vect = h.get("SubCategory");
                    if (vect == null) continue;
                    StringBuffer subbuf = new StringBuffer();
                    for (Object ob : vect) {
                        Map<String, Vector> hs =(Map<String, Vector>) ob;
                        String n = get("Name", hs);
                        Matcher ms = psubcat.matcher(n);
                        if (!ms.find()) continue;
                        String i = get("InitialEvaluator", hs);
                        ms = peng.matcher(i);
                        if (!ms.find()) continue;
                        String mgr = get("ResponsibleManager", hs);
                        ms = pmgr.matcher(mgr);
                        if (!ms.find()) continue;
                        subbuf.append("  " +n+" : "+i + " ("+mgr+")\n");
                    }
                    if (subbuf.length() > 0) {
                        sb.append(name + ": " + desc + "\n");
                        sb.append(subbuf.toString());
                    }
                }
            } catch (Exception e) {
                throw new Zx("lstcat: " + e.getMessage());
            }
            return sb.toString();
        }
        public ZInst create() {
            return new ZInst(this) {
                public String invoke() {
                    if (parent == null) {
                        throw new Zx("?category requires product names to be to be piped (see help). ");
                    }
                    String[] ss = parent.invoke().split("\n");
                    for(String s: get_validopt())
                        optmap.put(s, ".*");
                    for(int i = 0; i < argsize() ; i+= 2) {
                        optmap.put(getarg(i), getopt(i));
                    }
                    StringBuffer sb = new StringBuffer();
                    for (String s : ss) {
                        QueryProduct qp = new QueryProduct(s);
                        qp.setToken(token());
                        qp.process(ZBug.ia);
                        String res = qp.result();
                        HXml thexml = new HXml(res);
                        Map<String, Vector> ht = thexml.getRoot();
                        ht = (Map<String, Vector>) ht.get("SOAP-ENV:Body").get(0);
                        ht = (Map<String, Vector>) ht.get("ProductQueryService:QueryProduct").get(0);
                        ht = (Map<String, Vector>) ht.get("Product").get(0);
                        Vector v = ht.get("Category");
                        String r =  fmt_product(v, optmap);
                        sb.append(r + "\n");
                    }
                    return sb.toString();
                }
            };
        }
    };

    static ZHolder _print =  new ZHolder() {
        public String name() {return "*print";}
        public String tag() {return "*print";}
        public Action type() {return Action.T_SHOW;}
        public String help() {return "*for <crnum> |*print || *print <crnum>";}
        public String[] get_validopt() { return new String[] {}; }
        public ZInst create() {
            return new ZInst(this) {
                public String invoke() {
                    String[] ss = null;
                    if (parent == null) {
                        if (argsize() == 0)
                            throw new Zx("print requires cr id to be passed. ");
                        ss = new String[] {getarg(0)};
                    } else {
                        ss = parent.invoke().split("\n");
                    }
                    StringBuffer sb = new StringBuffer();
                    sb.append("<Bug>\n");
                    // unfortunately cant query more than one at a time.
                    for(String s: ss) {
                        PrintChangeRequest cr = new PrintChangeRequest(s);
                        cr.setToken(token());
                        cr.process(ZBug.ia);
                        String res = cr.result();
                        sb.append("\n");
                        sb.append(res);
                    }
                    sb.append("</Bug>\n");
                    return sb.toString();
                }
            };
        }
    };

    static ZHolder _brief =  new ZHolder() {
        public String name() {return "*brief";}
        public String tag() {return "*brief";}
        public Action type() {return Action.T_NONE;}
        public String help() {return "*for <crnum> |*brief || *brief <crnum>";}
        public String[] get_validopt() { return new String[] {}; }
        public ZInst create() {
            return new ZInst(this) {
                public String process(String xml,String[] ss) {
                    Set crset = new HashSet();
                    for(String s: ss) {
                        crset.add(s.trim());
                    }

                    if (xml.trim().length() <1) throw new Zx("Need to exec query before brief can be used.");
                    StringBuffer sb = new StringBuffer();
                    Xq x = Xq.init(xml);
                    NodeList lst = x.qnodes("/QueryChangeRequest/ChangeRequest");
                    for(int i = 0; i < lst.getLength(); ++i) {
                        if (lst.item(i).getNodeType() == Node.TEXT_NODE) continue;
                        String cr = Xq.get(lst.item(i), "CrNumber");
                        if (!crset.isEmpty())
                            if (!crset.contains(cr)) continue; // not selected.

                        for (String s: showopt) {
                            String txt = Xq.get(lst.item(i), s);
                            sb.append(s + "\t : " + txt+"\n");
                        }
                        sb.append("\n---------------------------------\n");
                    }
                    return sb.toString();
                }

                public String invoke() {
                    String[] ss = null;
                    if (parent == null) {
                        /*if (argsize() != 0)
                            ss = new String[] {getarg(0)};*/
                        ss = new String[0];
                    } else {
                        ss = parent.invoke().split("\n");
                    }
                    ZInst last = _last.create();
                    return process(last.invoke(), ss);
                }
            };
        }
    };


    static ZHolder _fmtcr =  new ZHolder() {
        public String name() {return "*fmtcr";}
        public String tag() {return "*fmtcr";}
        public Action type() {return Action.T_NONE;}
        public String help() {return "*for <crnum> |*print";}
        public String[] get_validopt() { return new String[] {}; }
        public void reg() { /* dont register.*/ }
        public ZInst create() {
            return new ZInst(this) {
                Vector<Node> get(Node n, String name) {
                    NodeList nl = n.getChildNodes();
                    Vector<Node> vn = new Vector<Node>();
                    for (int i =0; i < nl.getLength(); ++i) {
                        Node node = nl.item(i);
                        if (node.getNodeName().equals(name))
                            vn.add(node);
                    }
                    return vn;
                }

                void addtextelem(Node n, String name, StringBuffer sb, String color) {
                    Vector<Node> vn = get(n, name);
                    boolean append = false;
                    for (Node node: vn) {
                        if (!append) append = true;
                        else sb.append(" ");
                        sb.append(Colors.colorize(Xq.getText(node.getChildNodes().item(0)), color));
                    }
                }

                void addtextelem(Node n, String name, StringBuffer sb) {
                    addtextelem(n, name, sb, null);
                }

                void fmtbug(Node n, StringBuffer sb) {
                    String[] order = {
                            "Type",
                            "Area",
                            "Category",
                            "SubCategory",
                            "Product",
                            "RelatedChangeRequest",
                            "Release",
                            //"SubmittedDate",
                            //"FixAffectsDocumentaton", "FixAffectsLocalization",
                            "InitialEvaluator",
                            //"MakePublic", "MakePublicValue", "MrNumber",
                            "ResponsibleEngineer",
                            "ResponsibleManager",
                            //"MrIndicator",
                            //"QueryIndicator",
                            //"ModifiedDate",
                            //"ModifiedBy",
                            //"ModifiedNumber",
                            "InterestList",
                            "ServiceRequest",
                           // "AuditTrail"
                    };
                    sb.append("\n\n"); 
                    sb.append("    CR:\t");
                    addtextelem(n, "CrNumber", sb , "yellow");
                    sb.append("\n");
                    sb.append("    Synopsis:\t");
                    addtextelem(n, "Synopsis", sb);
                    sb.append("\n");
                    sb.append("    Priority:\t");
                    addtextelem(n, "Priority", sb, "yellow");
                    sb.append("\n");
                    sb.append("    Status:\t");
                    addtextelem(n, "Status", sb);
                    sb.append("\t");
                    addtextelem(n, "SubStatus", sb);
                    sb.append("\n\n");
                    for (String s : order) {
                        sb.append("    " +s + ":\t");
                        addtextelem(n, s, sb);
                        sb.append("\n");
                    }
                    sb.append("\n    Keys: ");
                    Vector<Node> vkn = get(n, "Keyword");
                    for (Node node: vkn) {
                        addtextelem(node,"Value", sb);
                        sb.append(", ");
                    }
                    sb.append("\n\n");

                    Vector<Node> vn = get(n, "EngNote");
                    String prev = "";
                    for (Node node: vn) {
                        String now = Xq.getText(get(node, "Type").get(0)).trim();
                        sb.append("\n\n");
                        if (!now.equals(prev)) {
                            sb.append("================== *");
                            addtextelem(node,"Type", sb, "blue");
                            sb.append("* ==================");
                            prev = now;
                        }

                        sb.append("\n");
                        addtextelem(node, "Note", sb);
                        sb.append("\n\n--");
                        addtextelem(node,"ModifiedBy", sb, "red");
                        sb.append(" : ");
                        addtextelem(node,"ModifiedDate", sb);
                    }
                }
                public String invoke() {
                    if (parent == null) throw new Zx("fmtcr requires *print in the pipeline.");
                    String res = parent.invoke();
                    Xq x = Xq.init(res);
                    NodeList nlst = x.qnodes("/Bug/ChangeRequest");
                    StringBuffer sb = new StringBuffer();
                    for (int i = 0; i <nlst.getLength(); ++i) {
                        Node n = nlst.item(i);
                        fmtbug(n, sb);
                        if (nlst.getLength() > 1) sb.append(Colors.colorize("\n====================================\n", "yellow"));
                    }
                    return sb.toString();
                }
            };
        }
    };

    static ZHolder _src =  new ZHolder() {
        public String name() {return "*src";}
        public String tag() {return "*src";}
        public Action type() {return Action.T_NONE;}
        public String help() {return "src <command> : returns the source of the <command>";}
        public String[] get_validopt() {
            return ZBug.cmdnames();
        }
        public ZInst create() {
            return new ZInst(this) {
                public String invoke() {
                    ZHolder s = ZBug.cmd_libs.get(ZBug.sym(getarg(0)));
                    return s.create().src();
                }
            };
        }
    };

    static ZHolder _rollback =  new ZHolder() {
        public String name() {return "*rollback";}
        public String tag() {return "*rollback";}
        public Action type() {return Action.T_NONE;}
        public String[] get_validopt() { return new String[0]; }
        public String help() {return "rollback : clear the uncommitted changes";}
        public ZInst create() {
            return new ZInst(this) {
                public String invoke() {
                    if (_lastUpdateXml == null) return "";
                    if (_lastUpdateXml.substring(1,10).startsWith("Create")) {
                        _lastUpdateXml = null;
                        return "rolled back create.";
                    } else {
                        int i = ZBug.split_lines(_lastUpdateXml.toString()).length;
                        _lastUpdateXml = null;
                        return "rolled back changes.";
                    }
                }
            };
        }
    };

    static ZHolder _changes =  new ZHolder() {
        public String name() {return "*changes";}
        public String tag() {return "*changes";}
        public Action type() {return Action.T_UPDATE;}
        public String help() {return "changes : list the uncommitted changes";}
        public String[] get_validopt() { return new String[0]; }
        public ZInst create() {
            return new ZInst(this) {
                public String invoke() {
                    if (parent != null) {
                        _lastUpdateXml = new StringBuffer(parent.invoke());
                    } else {
                        if (_lastUpdateXml == null) throw new Zx("No changes found.");
                    }
                    return _lastUpdateXml.toString();
                }
            };
        }
    };

    static ZHolder _commit =  new ZHolder() {
        public String name() {return "*commit";}
        public String tag() {return "*commit";}
        public Action type() {return Action.T_NONE;}
        public String help() {return "commit : commit changes to bugster";}
        public String[] get_validopt() { return new String[0]; }
        public ZInst create() {
            return new ZInst(this) {
                public String invoke() {
                    try {
                        if (_lastUpdateXml == null) return " no uncommited changes";
                        if (_lastUpdateXml.substring(1,10).startsWith("Create")) {
                            CreateChangeRequest cr = new CreateChangeRequest(_lastUpdateXml.toString());
                            cr.setToken(token());
                            cr.process(ZBug.ia);
                            return ".";
                        } else {
                            StringBuffer sb = new StringBuffer();
                            sb.append("<UpdateChangeRequest>");
                            sb.append(_lastUpdateXml.toString());
                            sb.append("</UpdateChangeRequest>");

                            UpdateChangeRequest cr = new UpdateChangeRequest(sb.toString());
                            cr.setToken(token());
                            cr.process(ZBug.ia);

                            int i = ZBug.split_lines(_lastUpdateXml.toString()).length;
                            return ".";
                        }
                    } catch (Exception e) {
                        throw new Zx(e.getMessage());
                    } finally {
                        _lastUpdateXml = null;
                    }
                }
            };
        }
    };

    static ZHolder _echo =  new ZHolder() {
        public String name() {return "*echo";}
        public String tag() {return "*echo";}
        public Action type() {return Action.T_NONE;}
        public String help() {return "echo <var> : used for defining macros";}
        public String[] get_validopt() { return new String[0]; }
        public ZInst create() {
            return new ZInst(this) {
                public String invoke() {
                    StringBuffer arg = new StringBuffer();
                    for(String s: args) arg.append(s + " ");
                    return arg.toString();
                }
            };
        }
    };

    static ZHolder _for =  new ZHolder() {
        public String name() {return "*for";}
        public String tag() {return "*for";}
        public Action type() {return Action.T_LINES;}
        public String help() {return "for <vars> : used for line sequences";}
        public String[] get_validopt() { return new String[0]; }
        public ZInst create() {
            return new ZInst(this) {
                public String invoke() {
                    StringBuffer arg = new StringBuffer();
                    for(String s: args) arg.append(s + "\n");
                    return arg.toString();
                }
            };
        }
    };

    static ZHolder _more =  new ZHolder() {
        static final int MAX_MORE = 100;
        public String name() {return "*more";}
        public String tag() {return "*more";}
        public Action type() {return Action.T_NONE;}
        public String help() {return "more";}
        public String[] get_validopt() { return new String[0]; }
        public void reg() { /* dont register.*/ }
        public ZInst create() {
            return new ZInst(this) {
                public Action type() {
                    if (parent != null)
                        return parent.type();
                    else
                        return Action.T_NONE;
                }
                public String invoke() {
                    int opt = ZBug._morenum;
                    if (argsize() > 0) {
                        opt = Integer.parseInt(getarg(0));
                    }
                    ZBug.reset_more(opt);
                    if (parent != null)
                        return parent.invoke();
                    else
                        return ZBug._lstText;
                }
            };
        }
    };

    static HashMap<String, HashMap<String,CField>> colorMap = new HashMap<String,HashMap<String,CField>>();

    static ZHolder _color =  new ZHolder() {
        public String name() {return "+color";}
        public String tag() {return "+color";}
        public Action type() {return Action.T_NONE;}
        public String help() {return "+color -<cr|engineer..> /match/ -color red|blue|green|yellow " +
            ": used for defining colorization for crlist";}
        public String[] get_validopt() {
            String[] s = new String[showopt.length +2];
            int i = 0;
            for(i = 0; i < showopt.length; ++i)
                s[i] = "-" + showopt[i].toLowerCase();
            s[i++] = "-color";
            s[i] = "-field";
            return s;
        }

        public ZInst create() {
            return new ZInst(this) {
                public String invoke() {
                    if (parent != null) throw new Zx("+color does not accept parents.");
                    HashMap<String,String> map = get_selectedopts();
                    String color = map.get("color");
                    String field = map.get("field");
                    if (args.size() == 0) {
                        StringBuffer sb = new StringBuffer();
                        for(String s: colorMap.keySet()) {
                            sb.append("[" + s + "]\n");
                            HashMap<String,CField> o = colorMap.get(s);
                            for (String r :  o.keySet()) {
                                CField x = o.get(r);
                                sb.append("/" + r + "/ "+ x.field+ " #" + x.color + "\n");
                            }
                        }
                        return sb.toString();
                    } else {
                        int start = 0;
                        while(start < args.size()) {
                            String opt = getarg(start);
                            String v = getopt(start);
                            start += 2;
                            if (opt.equals("-color")) continue;
                            if (opt.equals("-field")) continue;
                            HashMap<String,CField> o = colorMap.get(opt.substring(1));
                            if(o == null) o = new HashMap<String,CField>();
                            {
                                CField fld = new CField();
                                if (field == null) {
                                    fld.field = "%" +  opt.substring(1) + "%";
                                } else fld.field = field;
                                fld.color = color;
                                fld.regexp = Pattern.compile(v);
                                o.put(v,fld);
                            }
                            colorMap.put(opt.substring(1), o);
                        }
                        return "";
                    }
                }
            };
        }
    };


    static ZHolder _help =  new ZHolder() {
        public String name() {return "help";}
        public String tag() {return "help";}
        public Action type() {return Action.T_NONE;}
        public String[] get_validopt() { return ZBug.cmdnames(); }
        public String help() {return "help || help <cmd>: prints a short summary of commands or the specifid <cmd>";}
        public void reg(HashMap<Integer, ZHolder> lib) {
            super.reg(lib);
            _reg(lib, "*help");
        }

        public ZInst create() {
            return new ZInst(this) {
                public String invoke() {
                    if (parent != null) {
                        return parent.t().help();
                    }
                    if (argsize() > 0) {
                        return ZBug.cmd_libs.get(ZBug.sym(getarg(0))).help();
                    }
                    return "You can get a list of commands by tabbing.\n" +
                        "Most of the bugster commands take the following options: \n" +
                        "\t-eq -gt -lt -like -in -notin -between -noteq -notlike -empty -gteq -lteq\n" +
                        "Non bugster commands available are:" +
                        "\t*login print *show *last *query *apply *process ? <*tab>\n" +
                        "Use help <command> for more help.";
                }
            };
        }
    };

    static HashMap<String,String> _likeMap = new HashMap<String,String>();
    static ZHolder _like =  new ZHolder() {
        public String name() {return "*like";}
        public String tag() {return "*like";}
        public Action type() {return Action.T_NONE;}
        public String[] get_validopt() { return new String[0]; }
        public String help() {return "*like <bugid> - sets bugid values as the template for the new bugs created.";}

        public ZInst create() {
            return new ZInst(this) {
                public String invoke() {
                    StringBuffer sb = new StringBuffer();
                    if (parent != null) {
                        String[] res = parent.invoke().split("\n");
                        for (String r : res) {
                            String[] fld = r.split(":");
                            _likeMap.put(fld[0], fld[1]);
                        }
                        for (String r : _likeMap.keySet()) {
                            sb.append(r + ":" + _likeMap.get(r) + "\n" );
                        }
                    } else {
                        try {
                            String[] opts = new String[] {"Area" ,"Category","Priority", "Product" ,"SubCategory"};
                            String[] sopts = new String[] {"Functionality", "Severity", "Release", "ContactRole", "ContactType",
                                "Hardware", "OperatingSystem", "Impact", "SunContact"};
                            if (argsize() > 0) {
                                PrintChangeRequest cr = new PrintChangeRequest(getarg(0));
                                cr.setToken(token());
                                cr.process(ZBug.ia);
                                String res = cr.result();
                                Xq x = Xq.init(res);
                                NodeList lst = null;
                                NodeList nl = x.qnodes("//ChangeRequest");
                                Node n = nl.item(0);
                                for(String s: opts) _likeMap.put(s,Xq.get(n,s));
                                x = Xq.init(res);
                                nl = x.qnodes("//ChangeRequest/ServiceRequest");
                                Node ns = nl.item(0);
                                for (String s: sopts) {
                                    String k = s;
                                    if (s.equals("SunContact")) k = "Email";
                                    _likeMap.put(k,Xq.get(ns,s));
                                }
                            }
                            for(String s: opts) {
                                sb.append( s +":" + _likeMap.get(s));
                                sb.append("\n");
                            }
                            for(String s: sopts) {
                                String k = s;
                                if (s.equals("SunContact")) k = "Email";
                                sb.append( k +":" + _likeMap.get(k));
                                sb.append("\n");
                            }
                        } catch (Exception e) {
                            ZBug.err(e);
                            throw new Zx("like: " + e.getMessage());
                        }
                    }
                    return sb.toString();
                }
            };
        }
    };


    static ZHolder _status =  new ZHolder() {
        public String name() {return "*status";}
        public String tag() {return "*status";}
        public String help() {return "prints status";}
        public Action type() {return Action.T_NONE;}
        public String[] get_validopt() { return new String[0]; }
        public ZInst create() {
            return new ZInst(this) {
                public String invoke() {
                    if (ZBug.email != null) {
                        ZBug.out.println("> connected as [" + ZBug.email + "]");
                        ZBug.out.println("> " + ZBug.token);
                        return "";
                    } else return "not connected.";
                }
            };
        }
    };

    static ZHolder _version =  new ZHolder() {
        public String name() {return "*version";}
        public String tag() {return "*version";}
        public String help() {return "prints version";}
        public Action type() {return Action.T_NONE;}
        public String[] get_validopt() { return new String[0]; }
        public ZInst create() {
            return new ZInst(this) {
                public String invoke() {
                    return ZBug.version;
                }
            };
        }
    };

    static ZHolder _cat =  new ZHolder() {
        public String name() {return "*cat";}
        public String tag() {return "*cat";}
        public Action type() {return Action.T_NONE;}
        public String[] get_validopt() { return new String[0]; }
        public String help() {return "write <filename>: writes contents in the pipeline to <filename>";}
        public ZInst create() {
            return new ZInst(this) {
                public String invoke() {
                    return ZBug.read_file(getarg(0));
                }
            };
        }
    };

    static ZHolder _edit =  new ZHolder() {
        public String name() {return "*edit";}
        public String tag() {return "*edit";}
        public Action type() {return Action.T_NONE;}
        public String[] get_validopt() { return new String[0]; }
        public String help() {return "*edit : modify pipelines";}
        public ZInst create() {
            return new ZInst(this) {
                public Action type() {
                    if (parent == null) return t().type();
                    return parent.type();
                }
                public String invoke() {
                    try {
                        String xml = parent.invoke();
                        String mod = ZBug.edit(xml);
                        if (mod ==null) return xml;
                        return mod;
                    } catch (Exception e) {
                        ZBug.err(e);
                        throw new Zx("edit:" + e.getMessage());
                    }
                }
            };
        }
    };

    static ZHolder _limit =  new ZHolder() {

        public String name() {return "*limit";}
        public String tag() {return "*limit";}
        public Action type() {return Action.T_NONE;}
        public String[] get_validopt() { return new String[]{"-nomatch"}; }
        public String help() {
            return "limit <re>: limits the lines to those matching regular expression\n" +
            "to be used with *show (use -nomatch to inverse)";
        }
        public ZInst create() {
            return new ZInst(this) {
                public String invoke() {

                    ZInst z = _do.create();
                    z.parent(parent);
                    parent = z;

                    boolean not = false;
                    String re = getarg(0);
                    if (re.equals("-nomatch")) {
                        not = true;
                        re = getarg(1);
                    }
                    Pattern p = Pattern.compile(re, Pattern.CASE_INSENSITIVE);
                    StringBuffer sb = new StringBuffer();
                    for (String s: ZBug.split_lines(parent.invoke())) {
                        Matcher m = p.matcher(s);
                        if (m.find()) {
                            if (!not) sb.append(s + "\n");
                        } else {
                            if (not) sb.append(s + "\n");
                        }
                    }
                    return sb.toString();
                }
            };
        }
    };

    static ZHolder _wc =  new ZHolder() {

        public String name() {return "*wc";}
        public String tag() {return "*wc";}
        public Action type() {return Action.T_NONE;}
        public String[] get_validopt() { return new String[]{}; }
        public String help() {
            return "wc - count the number of lines";
        }
        public ZInst create() {
            return new ZInst(this) {
                public String invoke() {
                    String[] lines = new String[0];
                    if (parent != null) {
                        ZInst p = _do.create();
                        p.parent(parent);
                        parent = p;
                        lines = ZBug.split_lines(parent.invoke());
                    } else 
                        lines =  ZBug.split_lines(ZBug._lstText);
                    return "" + lines.length;
                }
            };
        }
    };

    static ZHolder _ls =  new ZHolder() {
        public String name() {return "ls";}
        public String tag() {return "ls";}
        public Action type() {return Action.T_NONE;}
        public String[] get_validopt() { return new String[0]; }
        public String help() {return "write <filename>: writes contents in the pipeline to <filename>";}
        public void reg(HashMap<Integer, ZHolder> lib) {
            super.reg(lib);
            _reg(lib, "*ls");
        }
        public ZInst create() {
            return new ZInst(this) {
                public String invoke() {
                    try {
                        StringBuffer sb = new StringBuffer();
                        String[] strs = ZBug.cmdnames();
                        Arrays.sort(strs);
                        for(String s: strs) sb.append(s + "  ");
                        return sb.toString();
                    } catch (Exception e) {
                        ZBug.err(e);
                        throw new Zx("ls:" + e.getMessage());
                    }
                }
            };
        }
    };

    static ZHolder _write =  new ZHolder() {
        public String name() {return "*write";}
        public String tag() {return "*write";}
        public Action type() {return Action.T_NONE;}
        public String[] get_validopt() { return new String[0]; }
        public String help() {return "write <filename>: writes contents in the pipeline to <filename>";}
        public ZInst create() {
            return new ZInst(this) {
                public String invoke() {
                    try {
                        if (parent == null) this.parent(_last.create());
                        if (!parent.name().equals(";")) {
                            ZInst s = _do.create();
                            s.parent(parent);
                            parent = s;
                        }
                        return "" + ZBug.write_file(getarg(0), parent.invoke());
                    } catch (Exception e) {
                        ZBug.err(e);
                        throw new Zx("write:" + e.getMessage());
                    }
                }
            };
        }
    };

    static String token() {
        return ZBug.ac().getToken();
    }

    static ZHolder _login =  new ZHolder() {
        public String name() {return "*login";}
        public String tag() {return "*login";}
        public Action type() {return Action.T_NONE;}
        public String[] get_validopt() { return new String[0];  }
        public String help() {return "login : prompts for authentication details (not necessary if in unix and ~/.bugster is present, *savepwd creates .bugster)";}
        public ZInst create() {
            return new ZInst(this) {
                public String invoke() {
                    try {
                        String user = null;
                        String pass = null;
                        String enc = null;
                        if (argsize() > 0) {
                            user = getarg(0).trim();
                        } else if (ZBug.user != null) {
                            user = ZBug.user;
                        } else {
                            user = ZBug.gets("user: ");
                        }
                        if (user.equals(ZBug.user) && ZBug.enc != null) {
                            ZBug.token = ZBug.login(ZBug.user,ZBug.enc, true);
                        } else {
                            ZBug.out.print("password: ");
                            pass = ZBug.reader().readLine(new Character('*'));
                            if ((pass == null) || (user == null)) throw new Zx("invalid username or password. (" + user + "," + pass + ")");
                            ZBug.token = ZBug.login(user, pass, false);
                            ZBug.user = user;
                        }
                        ZBug.set_var("email", ZBug.email);
                        ZBug.out("welcome " + ZBug.email);
                    } catch (Exception e) {
                        ZBug.err(e);
                        throw new Zx("invalid username or password.");
                    }
                    return "";
                }
            };
        }
    };

    static ZHolder _savepwd =  new ZHolder() {
        public String name() {return "*savepwd";}
        public String tag() {return "*savepwd";}
        public Action type() {return Action.T_NONE;}
        public String[] get_validopt() { return new String[] {"-save"}; }
        public String help() {return "savepwd : creates .bugster";}
        public ZInst create() {
            return new ZInst(this) {
                public String enc(String usr, String pass) {
                    try {
                        EncryptionClient ec = new EncryptionClient();
                        ec.setUserID(usr);
                        ec.setText(pass);
                        ByteArrayOutputStream xmlout = new ByteArrayOutputStream();
                        SOAPServiceMessage msg = ec.doProcess(true);
                        msg.getMessage().writeTo(xmlout);
                        String str = xmlout.toString();
                        Pattern r =  Pattern.compile("<TextEncryptionService:Text>(.*)</TextEncryptionService:Text>");
                        Matcher m = r.matcher(str);
                        if (m.find()) return m.group(1).trim();
                    } catch (Exception e) {
                        ZBug.err(e);
                        throw new Zx("savepwd:" + e.getMessage());
                    }
                    throw new Zx("unable to login.");
                }

                public String invoke() {
                    try {
                        String user = null;
                        String pass = null;
                        String enc = null;
                        if (argsize() > 0) {
                            user = getarg(0).trim();
                        } else if (ZBug.user != null) {
                            user = ZBug.user;
                        } else {
                            user = ZBug.gets("user: ");
                        }
                        ZBug.out.print("password: ");
                        pass = ZBug.reader().readLine(new Character('*'));
                        if ((pass == null) || (user == null)) throw new Zx("invalid username or password. (" + user + ")");
                        try {
                            (new File(ZBug.home + "/.bugster")).delete();
                        } catch (Exception e) {/*ignore*/}
                        enc = enc(user, pass);
                        if (enc == null) throw new Zx("savepwd: unsuccessful");
                        ZBug.write_file(ZBug.home + "/.bugster", enc);
                        Runtime.getRuntime().exec("chmod 400 " + ZBug.home + "/.bugster");
                        ZBug.enc = enc;
                    } catch (Exception e) {
                        ZBug.err(e);
                        throw new Zx("savepwd:invalid username or password." + e.getMessage());
                    }
                    return "";
                }
            };
        }
    };


    static String[] showopt = {"CrNumber" ,"Area" ,"Category","CommitToFixInBuild"
        ,"FixAffectsDocumentation" ,"FixAffectsLocalization", "FixedInBuild"
        ,"Id" ,"InitialEvaluator", "IntegratedInBuild", "IntroducedInBuild", "IntroducedInRelease"
        ,"MakePublic" ,"ModifiedBy" ,"ModifiedDate" ,"ModifiedNumber" ,"MrNumber"
        ,"Priority" ,"Product" ,"Release", "ResponsibleEngineer","ResponsibleManager"
        ,"Status" ,"SubArea" ,"SubCategory" ,"SubmittedBy" ,"SubmittedDate" ,"Synopsis" };

    static ZHolder _sort =  new ZHolder() {
        public String name() {return "*sort";}
        public String tag() {return "sort";}
        public Action type() {return Action.T_Q_RESULT_XML;}
        public String[] get_validopt() {
            String[] s = new String[showopt.length];
            for(int i = 0; i < showopt.length; ++i)
                s[i] = "-" + showopt[i].toLowerCase();
            return s;
        }
        public String help() {
            return "sorts the output of a query based on arguments\n" +
                "should be used before show";
        }

        public ZInst create() {
            return new ZInst(this) {
                public String process(String xml, String key) {
                    Xq x = Xq.init(xml);
                    Node n = x.qnodes("/QueryChangeRequest").item(0);
                    Xq.sort(n, true, args);
                    return Xq.nodeToString(n);
                }

                public String invoke() {
                    if (parent == null) this.parent(_last.create());
                    else if (parent.name().equalsIgnoreCase("*show")) {
                        throw new Zx("*sort: *sort should come before *show");
                    } else {
                        if (parent.type() == Action.T_QUERY) {
                            // it requires a littlebit of helping
                            ZInst query = _query.create();
                            ZInst exec = _exec.create();
                            query.parent(parent);
                            exec.parent(query);
                            this.parent(exec);
                        }
                    }
                    return process(parent.invoke(), "");
                }
            };
        }
    };

    static ZHolder _create =  new ZHolder() {
        public String name() {return "*create";}
        public String tag() {return "create";}
        public Action type() {return Action.T_NONE;}

        String[] createopt = {//"!Activity"
            "Area" ,"Category", "Description", "Functionality"
            ,"Email", "Impact", "OperatingSystem", "Hardware"
            //,"!CommitToFixInBuild"
            //,"!FixAffectsDocumentation" ,"!FixAffectsLocalization"
            //,"!Hook1", "!Hook2", "!Hook3", "!Hook4", "!Hook5", "!Hook6"
            //,"!IntroducedInBuild", "!IntroducedInRelease"
            //,"!MakePublic"
            ,"Priority","PriorityJustification", "Product"
            //,"!ProductManagement","!RelatedChangeRequest"
            ,"Release"
            //,"!ReportedBy","!ResponsibleEngineer","!ResponsibleManager", "!RootCause",
            //,"SubArea"
            ,"SubCategory","Synopsis"};

        String[] srvreq = {"Functionality", "Impact", "OperatingSystem", "Hardware",
            "Product", "Release", "SunContact"};

        public String[] get_validopt() {
            String[] s = new String[createopt.length];
            int i =0;
            for(i = 0; i < createopt.length; ++i)
                s[i] = "-" + createopt[i].toLowerCase();
            return s;
        }

        public String help() {
            return "create cr in bugster\n";
        }

        public ZInst create() {
            return new ZInst(this) {

                String getoptdef(String s, HashMap<String,String> map) {
                    String v = map.get(s.toLowerCase());
                    if (v == null) {
                        v = _likeMap.get(s.toLowerCase());
                    }
                    if (v == null) {
                        if (s.equalsIgnoreCase("Area")) return "Defect";
                        if (s.equalsIgnoreCase("Functionality")) return "Secondary";
                        if (s.equalsIgnoreCase("Hardware")) return "all";
                        if (s.equalsIgnoreCase("OperatingSystem")) return "OperatingSystem";
                        if (s.equalsIgnoreCase("Priority")) return "4-Low";
                        if (s.equalsIgnoreCase("Impact")) return "Limited";
                        throw new Zx("create: " + s.toLowerCase() + " is mandatory." );
                    } else {
                        return ArgFilter.filter(s.toLowerCase(), "", v);
                    }
                }

                void getfilledopt(String s, StringBuffer sb, HashMap<String,String> map) {
                    if (s.equalsIgnoreCase("ServiceRequest")) {
                        sb.append("<ServiceRequest>\n");
                        sb.append("\t<Account><AccountName>sunmicrosystems</AccountName></Account>\n");
                        sb.append("\t<ContactRole>D-Development</ContactRole>\n");
                        sb.append("\t<ContactType>I-Internal (SMI) Customer</ContactType>\n");
                        sb.append("\t<Functionality>" +getoptdef("Functionality", map)+ "</Functionality>\n");
                        sb.append("\t<Hardware>" +getoptdef("Hardware", map)+ "</Hardware>\n");
                        sb.append("\t<Impact>" +getoptdef("Impact", map)+ "</Impact>\n");
                        sb.append("\t<OperatingSystem>" +getoptdef("OperatingSystem", map)+ "</OperatingSystem>\n");
                        sb.append("\t<Product>" +getoptdef("Product", map)+ "</Product>\n");
                        sb.append("\t<Release>" +getoptdef("Release", map)+ "</Release>\n");
                        sb.append("\t<SunContact>" +getoptdef("Email", map)+ "</SunContact>\n");
                        sb.append("</ServiceRequest>\n");
                    } else {
                        if (s.equalsIgnoreCase("Description")) {
                            sb.append("<EngNote><Type>Description</Type>");
                            sb.append("<Note>");
                            sb.append(getoptdef(s, map));
                            sb.append("</Note></EngNote>\n");
                        } else {
                            sb.append("<" + s + ">");
                            sb.append(getoptdef(s, map));
                            sb.append("</" + s + ">\n");
                        }
                    }
                }

                public String invoke() {
                    StringBuffer sb = new StringBuffer();
                    try {
                        HashMap<String,String> map = get_selectedopts();
                        sb.append("<CreateChangeRequest>\n");
                        sb.append("<ChangeRequest>\n");
                        getfilledopt("Area", sb, map);
                        getfilledopt("Category", sb, map);
                        getfilledopt("Description", sb, map);
                        getfilledopt("Priority", sb, map);
                        getfilledopt("Product", sb, map);
                        //getfilledopt("Release", sb, map);
                        getfilledopt("ServiceRequest", sb, map);
                        getfilledopt("SubCategory", sb, map);
                        getfilledopt("Synopsis", sb, map);
                        getfilledopt("PriorityJustification", sb, map);
                        sb.append("\n</ChangeRequest>\n");
                        sb.append("</CreateChangeRequest>\n");
                        _lastUpdateXml = sb;
                        return sb.toString();
                    } catch (Exception e) {
                        ZBug.err(e);
                        throw new Zx("create: " + e.getMessage());
                    }
                }
            };
        }
    };


    static ZHolder _set =  new ZHolder() {
        public String name() {return "*set";}
        public String tag() {return "set";}
        public Action type() {return Action.T_UPDATE;}

        String[] setopt = {"!Activity", "Area" ,"Category","CommitToFixInBuild", "DuplicateOf"
            ,"!Escalation", "FixAffectsDocumentation" ,"FixAffectsLocalization", "FixedInBuild"
            ,"Hook1", "Hook2", "Hook3", "Hook4", "Hook5", "Hook6"
            ,"IntegratedInBuild", "IntroducedInBuild", "IntroducedInRelease"
            ,"MakePublic","Priority", "PriorityJustification" ,"Product","ProductManagement","!RelatedChangeRequest","Release"
            ,"ReportedBy","ResponsibleEngineer","ResponsibleManager", "RootCause","!ServiceRequest","!Solution"
            ,"Status" ,"SubArea" ,"SubCategory" ,"!SubChangeRequest" ,"SubStatus" ,"Synopsis", "VerifiedInBuild" };

        public String[] get_validopt() {
            String[] s = new String[setopt.length];
            for(int i = 0; i < setopt.length; ++i)
                s[i] = "-" + setopt[i].toLowerCase();
            return s;
        }
        public String help() {
            return "updates stuff in bugster\n";
        }

        public ZInst create() {
            return new ZInst(this) {

                Vector<String> get_crs_xml(String xml) {
                    Vector<String> crs = new Vector<String>();
                    Xq x = Xq.init(xml);
                    NodeList lst = x.qnodes("/QueryChangeRequest/ChangeRequest");
                    for(int i = 0; i < lst.getLength(); ++i) {
                        crs.add(Xq.get(lst.item(i),"CrNumber"));
                    }
                    return crs;
                }

                Vector<String> get_crs_plain(String lines) {
                    Vector<String> crs = new Vector<String>();
                    for(String s: ZBug.split_lines(lines)) {
                        crs.add(s.split("\t")[0]);
                    }
                    return crs;
                }

                public String invoke() {
                    Vector<String> crs = new Vector<String>();
                    if (parent == null) {
                        this.parent(_last.create());
                    } 
                    if (parent.type() == Action.T_Q_RESULT_XML) {
                        crs = get_crs_xml(parent.invoke());
                    } else if (parent.type() == Action.T_LINES) {
                        crs = get_crs_plain(parent.invoke());
                    } else {
                        /*ZInst query = _query.create();
                          ZInst exec = _exec.create();
                          query.parent(parent);
                          exec.parent(query);
                          this.parent(exec);*/
                        ZBug.out.println("Dummy : TODO");
                    }
                    StringBuffer sb = new StringBuffer();
                    HashMap<String,String> map = get_selectedopts();
                    for(String s: crs) {
                        sb.append("<ChangeRequest>");
                        sb.append("<CrNumber>");
                        sb.append(s);
                        sb.append("</CrNumber>");
                        Set<String> opts = map.keySet();
                        for(String sopt: setopt) {
                            if (!opts.contains(sopt.toLowerCase())) continue;
                            sb.append("<"+ sopt + ">");
                            sb.append(map.get(sopt.toLowerCase()));
                            sb.append("</"+ sopt +">");
                        }
                        sb.append("</ChangeRequest>\n");
                    }
                    if (_lastUpdateXml  == null) _lastUpdateXml = sb;
                    else _lastUpdateXml.append(sb.toString());
                    return sb.toString();
                }
            };
        }
    };


    static ZHolder _add =  new ZHolder() {
        String[] setopt = {"Description", "Comments" ,"Evaluation", "Suggested-Fix","Work-Around", "keys", "interestlist", "seealso"};
        Set engnote_opts = new HashSet(Arrays.asList(new String[]
        {"Description","Comments","Evaluation", "Suggested-Fix","Work-Around"}));


        public String name() {return "*add";}
        public String tag() {return "add";}
        public Action type() {return Action.T_UPDATE;}


        public String[] get_validopt() {
            String[] s = new String[setopt.length];
            for(int i = 0; i < setopt.length; ++i)
                s[i] = "-" + setopt[i].toLowerCase();
            return s;
        }
        public String help() {
            return "adds comments, evaluation, description, keywords, interestlist and seealso to bugs in bugster\n";
        }
        
        HashMap<String,String> process_map( HashMap<String,String> m) {
            HashMap<String,String> map  = new HashMap<String,String>();
            for(String s: setopt) {
                if (m.get(s.toLowerCase()) != null) {
                    map.put(s, m.get(s.toLowerCase()));
                }
            }
            return map;
        }

        public ZInst create() {
            return new ZInst(this) {

                Vector<String> get_crs_xml(String xml) {
                    Vector<String> crs = new Vector<String>();
                    Xq x = Xq.init(xml);
                    NodeList lst = x.qnodes("/QueryChangeRequest/ChangeRequest");
                    for(int i = 0; i < lst.getLength(); ++i) {
                        crs.add(Xq.get(lst.item(i),"CrNumber"));
                    }
                    return crs;
                }

                Vector<String> get_crs_plain(String lines) {
                    Vector<String> crs = new Vector<String>();
                    for(String s: ZBug.split_lines(lines)) {
                        crs.add(s.split("\t")[0]);
                    }
                    return crs;
                }

                public String invoke() {
                    Vector<String> crs = new Vector<String>();
                    if (parent == null) {
                        this.parent(_last.create());
                    } 
                    if (parent.type() == Action.T_Q_RESULT_XML) {
                        crs = get_crs_xml(parent.invoke());
                    } else if (parent.type() == Action.T_LINES) {
                        crs = get_crs_plain(parent.invoke());
                    } else {
                        /*ZInst query = _query.create();
                          ZInst exec = _exec.create();
                          query.parent(parent);
                          exec.parent(query);
                          this.parent(exec);*/
                        ZBug.out.println("Dummy : TODO");
                    }
                    StringBuffer sb = new StringBuffer();
                    HashMap<String,String> nmap = get_selectedopts();
                    HashMap<String,String> map = process_map(nmap);

                    for(String s: crs) {
                        sb.append("<ChangeRequest>");
                        sb.append("<CrNumber>");
                        sb.append(s);
                        sb.append("</CrNumber>");
                        for(String sopt: map.keySet()) {
                            if (engnote_opts.contains(sopt)) {
                                sb.append("<EngNote>");
                                sb.append("<Type>" + sopt.replaceAll("-"," ") + "</Type>");
                                sb.append("<Note>");
                                String t = Xq.encode(map.get(sopt));
                                sb.append(t);
                                sb.append("</Note>");
                                sb.append("</EngNote>");
                            } else if (sopt.equalsIgnoreCase("keys")) {
                                sb.append("<Keyword Action='Update'>");
                                String[] sarr = map.get(sopt).split(",");
                                for(String str: sarr)
                                    sb.append("<Value>" + Xq.encode(str) + "</Value>");
                                sb.append("</Keyword>");
                            } else if (sopt.equalsIgnoreCase("interestlist")) {
                                sb.append("<InterestList Action='Update'>");
                                String[] sarr = map.get(sopt).split(",");
                                for(String str: sarr)
                                    sb.append("<Value>" + Xq.encode(str) + "</Value>");
                                sb.append("</InterestList>");
                            } else if (sopt.equalsIgnoreCase("seealso")) {
                                sb.append("<RelatedChangeRequest Action='Update'>");
                                String str = map.get(sopt);
                                sb.append("<CrNumber>" + Xq.encode(str) + "</CrNumber>");
                                sb.append("</RelatedChangeRequest>");
                            } else {
                                throw new Zx("*add: unknown option (" + sopt + ")");
                            }
                        }
                        sb.append("</ChangeRequest>\n");
                    }
                    if (_lastUpdateXml  == null) _lastUpdateXml = sb;
                    else _lastUpdateXml.append(sb.toString());
                    return sb.toString();
                }
            };
        }
    };

    static ZHolder _rm =  new ZHolder() {
        String[] setopt = {"keys", "interestlist", "seealso"};

        public String name() {return "*rm";}
        public String tag() {return "rm";}
        public Action type() {return Action.T_UPDATE;}


        public String[] get_validopt() {
            String[] s = new String[setopt.length];
            for(int i = 0; i < setopt.length; ++i)
                s[i] = "-" + setopt[i].toLowerCase();
            return s;
        }
        public String help() {
            return "removes keywords, interestlist and seealso from bugs in bugster\n";
        }

        public ZInst create() {
            return new ZInst(this) {

                Vector<String> get_crs_xml(String xml) {
                    Vector<String> crs = new Vector<String>();
                    Xq x = Xq.init(xml);
                    NodeList lst = x.qnodes("/QueryChangeRequest/ChangeRequest");
                    for(int i = 0; i < lst.getLength(); ++i) {
                        crs.add(Xq.get(lst.item(i),"CrNumber"));
                    }
                    return crs;
                }

                Vector<String> get_crs_plain(String lines) {
                    Vector<String> crs = new Vector<String>();
                    for(String s: ZBug.split_lines(lines)) {
                        crs.add(s.split("\t")[0]);
                    }
                    return crs;
                }

                public String invoke() {
                    Vector<String> crs = new Vector<String>();
                    if (parent == null) {
                        this.parent(_last.create());
                    } 
                    if (parent.type() == Action.T_Q_RESULT_XML) {
                        crs = get_crs_xml(parent.invoke());
                    } else if (parent.type() == Action.T_LINES) {
                        crs = get_crs_plain(parent.invoke());
                    } else {
                        /*ZInst query = _query.create();
                          ZInst exec = _exec.create();
                          query.parent(parent);
                          exec.parent(query);
                          this.parent(exec);*/
                        ZBug.out.println("Dummy : TODO");
                    }
                    StringBuffer sb = new StringBuffer();
                    HashMap<String,String> map = get_selectedopts();
                    for(String s: crs) {
                        sb.append("<ChangeRequest>");
                        sb.append("<CrNumber>");
                        sb.append(s);
                        sb.append("</CrNumber>");
                        for(String sopt: map.keySet()) {
                            if (sopt.equalsIgnoreCase("keys")) {
                                sb.append("<Keyword Action='Remove'>");
                                String[] sarr = map.get(sopt).split(",");
                                for(String str: sarr)
                                    sb.append("<Value>" + str + "</Value>");
                                sb.append("</Keyword>");
                            } else if (sopt.equalsIgnoreCase("interestlist")) {
                                sb.append("<InterestList Action='Remove'>");
                                String[] sarr = map.get(sopt).split(",");
                                for(String str: sarr)
                                    sb.append("<Value>" + str + "</Value>");
                                sb.append("</InterestList>");
                            } else if (sopt.equalsIgnoreCase("seealso")) {
                                sb.append("<RelatedChangeRequest Action='Remove'>");
                                String str = map.get(sopt);
                                sb.append("<CrNumber>" + str + "</CrNumber>");
                                sb.append("</RelatedChangeRequest>");
                            } else {
                                throw new Zx("*add: unknown option.");
                            }
                        }
                        sb.append("</ChangeRequest>\n");
                    }
                    if (_lastUpdateXml  == null) _lastUpdateXml = sb;
                    else _lastUpdateXml.append(sb.toString());
                    return sb.toString();
                }
            };
        }
    };




    static ZHolder _show =  new ZHolder() {
        public String name() {return "*show";}
        public String tag() {return "show";}
        public Action type() {return Action.T_LINES;}
        public String[] get_validopt() {
            String[] s = new String[showopt.length];
            for(int i = 0; i < showopt.length; ++i)
                s[i] = "-" + showopt[i].toLowerCase();
            return s;
        }
        public String help() {
            return "formats the current contents in the pipeline according to the arguments\n" +
                "Uses the output of *last if used as the first command in a pipeline ";
        }

        public ZInst create() {
            return new ZInst(this) {
                private Vector<String> get_selectedopts_lst() {
                    Vector<String> opts = new Vector<String>();
                    boolean hascr = false; 

                    // check if -all is specified
                    if ((argsize() > 0) && getarg(0).equals("-all")) {
                        for(String o: showopt) opts.add(o);
                        return opts;
                    }

                    for(String s: args) {
                        String a = s.substring(1);
                        if (a.equalsIgnoreCase("CrNumber")) hascr = true;
                        for(String o: showopt) {
                            if (o.toLowerCase().startsWith(a))
                                opts.add(o);
                        }
                    }
                    /*if(opts.size() == 0) {
                        opts.add("CrNumber");
                        opts.add("Status");
                        opts.add("Priority");
                        opts.add("Synopsis");
                    } else {
                        if (!hascr) opts.add(0, "CrNumber");
                    }*/
                    return opts;
                }

                public String transform(String s, String str) {
                    if (!ZBug.fullopt) {
                        if (s.equalsIgnoreCase("Status") || s.equalsIgnoreCase("Priority"))
                            return str.substring(0, str.indexOf('-')+2);
                    }
                    return str;
                }

                public String process(String xml) {
                    StringBuffer sb = new StringBuffer();
                    Xq x = Xq.init(xml);
                    NodeList lst = x.qnodes("/QueryChangeRequest/ChangeRequest");
                    for(int i = 0; i < lst.getLength(); ++i) {
                        if (lst.item(i).getNodeType() == Node.TEXT_NODE) continue;
                        HashMap<String,String> map = new HashMap<String, String>();
                        HashMap<String,CField> cmap = new HashMap<String,CField>();
                        Vector<CField> clist = new Vector<CField>();
                        for (String s: showopt) {
                            String t = s.toLowerCase();
                            String txt = transform(s, Xq.get(lst.item(i), s));
                            map.put(t, txt);

                            // does any of the color rules match?
                            if (ZBug.showcolor && colorMap.containsKey(t)) {
                                // atleast a rule with s is defined. 
                                // Look if the regexp match. 
                                //  If matches, put it to the cmap
                                cmap = colorMap.get(t);
                                if (cmap != null) {
                                    for (String xt : cmap.keySet()) {
                                        CField f = cmap.get(xt);
                                        Matcher m = f.regexp.matcher(txt);
                                        if (m.find()) clist.add(f);
                                    }
                                }
                            }
                        }
                        // show numbers if set to.
                        if (ZBug.shownum) sb.append(String.format("%3d| ", i+1));

                        // set $1 $2 ...
                        ZBug.set_var("" + (i + 1), Xq.get(lst.item(i), "CrNumber"));
                        // does there exist a format?

                        ZHolder z = ZBug.cmd_libs.get(ZBug.sym("%show_format%"));
                        String tmpl = "%crnumber% %status% %priority% (%engineer%) [%$%]  %synopsis%";
                        if (z != null ) tmpl =  z.create().src().toLowerCase();

                        if (ZBug.showcolor) {
                            for(CField t : clist)
                                tmpl = tmpl.replaceAll(t.field,Colors.colorize(t.field, t.color));
                        }
                        for (String s: map.keySet()) {
                            String txt = map.get(s);
                            tmpl = tmpl.replace("%" + s.toLowerCase() + "%", txt);
                            if (s.equalsIgnoreCase("responsibleengineer")) {
                                tmpl = tmpl.replaceAll("%engineer%", txt.split("\\.")[0]);
                            }
                        }
                        if (tmpl.indexOf("%$%") != 1) {
                            StringBuffer mb = new StringBuffer();
                            for (String s: get_selectedopts_lst()) {
                                String txt = transform(s, Xq.get(lst.item(i), s));
                                mb.append(txt + " ");
                            }
                            tmpl = tmpl.replaceAll("%\\$%", mb.toString().trim());
                        }
                        sb.append(tmpl);
                        sb.append("\n");
                    }
                    return sb.toString();
                }

                public String invoke() {
                    if (parent == null) this.parent(_last.create());
                    else {
                        if (parent.type() == Action.T_QUERY) {
                            // it requires a littlebit of helping
                            ZInst query = _query.create();
                            ZInst exec = _exec.create();
                            query.parent(parent);
                            exec.parent(query);
                            this.parent(exec);
                        }
                    }
                    return process(parent.invoke());
                }
            };
        }
    };

    static ZHolder _last =  new ZHolder() {
        public String name() {return "*last";}
        public String tag() {return "*last";}
        public Action type() {return Action.T_Q_RESULT_XML;}
        public String help() {return "return the last query results. (not a remote operation)";}
        public String[] get_validopt() { return new String[0];}
        public ZInst create() {
            return new ZInst(this) {
                public String invoke() {

                    if (parent != null) {
                        _lastXml = parent.invoke();
                    } else {
                        if (_lastXml == null) throw new Zx("no last bugster command to get output from.");
                    }
                    return _lastXml;
                }
            };
        }
    };

    static ZHolder _showupdatexml =  new ZHolder() {
        public String name() {return "showupdatexml";}
        public String tag() {return "showupdatexml";}
        public Action type() {return Action.T_NONE;}
        public String help() {return "";}
        public void reg() { /* dont register.*/ }
        public String[] get_validopt() { return new String[0];}
        public ZInst create() {
            return new ZInst(this) {
                String update_xml_lines(String xml) {
                    StringBuffer sb = new StringBuffer();
                    Xq x = Xq.init("<UpdateChangeRequest>"+ xml + "</UpdateChangeRequest>");
                    NodeList lst = x.qnodes("/UpdateChangeRequest/ChangeRequest");
                    for(int i = 0; i < lst.getLength(); ++i) {
                        Node ln = lst.item(i);
                        if (ln.getNodeType() == Node.TEXT_NODE) continue;
                        String txt = Xq.get(ln, "CrNumber");
                        sb.append(txt+":\t");
                        for(int j = 1; j < ln.getChildNodes().getLength(); ++j) {
                            sb.append(Xq.nodeToRep(ln.getChildNodes().item(j)));
                            sb.append(" ");
                        }
                        sb.append("\n");
                    }
                    return sb.toString();
                }

                public String invoke() {
                    if (parent != null) {
                        String xml = parent.invoke();
                        if (xml.substring(1,10).startsWith("Create")) {
                            return xml;
                        } else return update_xml_lines(xml);
                    }
                    throw new Zx("requires last update xml");
                }
            };
        }
    };

    static String[] xmlcmds = {"Activity.ActivityId", "Area", "Category", "CommitToFixInBuild", "CrNumber" ,
        "DuplicateOf", "EngNote.CreatedBy", "EngNote.CreatedDate", "EngNote.ModifiedBy" ,
        "EngNote.ModifiedDate", "EngNote.Type", "Escalation.CustomerAdvocate", "Escalation.EscalationDate",
        "Escalation.EscalationEngineer", "Escalation.EscalationNumber", "Escalation.EscalationStatus",
        "Escalation.ManagementAlert", "FixAffectsDocumentation", "FixAffectsLocalization", "FixedInBuild",
        "Hook1", "Hook2", "Hook3", "Hook4", "Hook5", "Hook6", "InitialEvaluator", "IntegratedInBuild",
        "InterestList", "IntroducedInBuild", "IntroducedInRelease", "Keyword", "MakePublic", "ModifiedDate",
        "MrNumber", "Priority", "Product", "ProgramManagement", "RelatedChangeRequest.CrNumber", "Release",
        "ReportedBy", "ResponsibleEngineer", "ResponsibleManager", "RootCause", "ServiceRequest.SrNumber",
        "Solution.SolutionId", "Status", "SubArea", "SubCategory", "SubmittedBy", "SubmittedDate",
        "SubStatus", "Synopsis", "VerifiedInBuild" };

    void init(HashMap<Integer,ZHolder> cmd_libs) {
        _help.reg(cmd_libs);
        _login.reg(cmd_libs);
        _savepwd.reg(cmd_libs);
        _last.reg(cmd_libs);
        _src.reg(cmd_libs);
        _echo.reg(cmd_libs);
        _for.reg(cmd_libs);
        _more.reg(cmd_libs);
        _color.reg(cmd_libs);
        _rollback.reg(cmd_libs);
        _changes.reg(cmd_libs);
        _commit.reg(cmd_libs);

        _print.reg(cmd_libs);
        _lstcat.reg(cmd_libs);
        _lstrel.reg(cmd_libs);
        _brief.reg(cmd_libs);
        _fmtcr.reg(cmd_libs);
        _write.reg(cmd_libs);
        _ls.reg(cmd_libs);
        _cat.reg(cmd_libs);
        _limit.reg(cmd_libs);
        _wc.reg(cmd_libs);
        _query.reg(cmd_libs);
        _exec.reg(cmd_libs);
        _do.reg(cmd_libs);
        _show.reg(cmd_libs);
        _sort.reg(cmd_libs);
        _set.reg(cmd_libs);
        _create.reg(cmd_libs);
        _add.reg(cmd_libs);
        _rm.reg(cmd_libs);
        _version.reg(cmd_libs);
        _status.reg(cmd_libs);
        _like.reg(cmd_libs);
        _edit.reg(cmd_libs);
        _lststatus.reg(cmd_libs);
        cmd_libs.put(ZBug.sym("?"), new ZHolder() {
            public String name() {return "?";}
            public String tag() {return "";}
            public Action type() {return Action.T_NONE;}
            public String[] get_validopt() { return new String[0];}
            public String help() {return "show the current contents in the pipeline";}
            public ZInst create() {
                return new ZInst(this) {
                    public String invoke() {
                        if (parent == null) this.parent(_last.create());
                        return(parent.invoke());
                    }
                };
            }
        });
        cmd_libs.put(ZBug.sym("?def"), new ZHolder() {
            public String name() {return "?def";}
            public String tag() {return "";}
            public Action type() {return Action.T_NONE;}
            public String[] get_validopt() { return new String[0];}
            public String help() {return "show the current contents in the pipeline";}
            public ZInst create() {
                return new ZInst(this) {
                    public int print_parent(ZInst p) {
                        if (p.parent == null) {
                            ZBug.out.println("*>"+ p.name() +  " [" + p.src() + "]");
                            return 1;
                        } else {
                            int i = print_parent(p.parent);
                            StringBuffer b = new StringBuffer();
                            for (int j=0; j <i; j++) b.append(" ");
                            b.append(" |>"+ p.name() +  " [" + p.src() + "]");
                            ZBug.out.println(b.toString());
                            return i+1;
                        }
                    }
                    public String invoke() {
                        if (parent == null) this.parent(_last.create());
                        print_parent(this.parent);
                        return "";
                    }
                };
            }
        });

        for(String c : xmlcmds) {
            (new BugsterSkelCmd(c)).reg(cmd_libs);
        }
    }

    static String _lastXml = null;
    static StringBuffer _lastUpdateXml = null;

}

