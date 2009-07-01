package zbug;
import java.util.*;

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


