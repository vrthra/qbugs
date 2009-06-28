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

enum T_Lex {T_PIPE, T_STRING, T_SYM, T_RBRACE, T_LBRACE}

class Lex {
    T_Lex lex_type;
    int sym;
    String sval;
    int ival;
    Lex(T_Lex t) {lex_type = t;}
    Lex(T_Lex t, int s) {lex_type = t; sym = s;}
    String value() {
        if (lex_type == T_Lex.T_SYM) return ZBug.rsym(sym);
        if (lex_type == T_Lex.T_STRING) return  ZBug.rsym(sym);
        return sym + "";
    }

    String stype() {
        switch (lex_type) {
            case T_PIPE: return "<pipe>";
            case T_STRING: return "<string>";
            case T_SYM: return "<sym>";
            case T_RBRACE: return "<rbrace>";
            case T_LBRACE: return "<lbrace>";
            default: return "<unknown>";
        }
    }
    String xv() {
        return stype() + "(" + value() + ")" ;
    }
}

class Zx extends RuntimeException { public Zx(String s) { super(s); } }

class Ll {
    Lex l;
    Ll next;
    Ll last;
    Ll first;
    Ll() {
    }
    void add(Lex l) {
        Ll n = new Ll();
        n.l = l;
        if (last == null) {
            last = n;
            first = n;
        } else {
            last.next = n;
            last = n;
        }
    }
}

public class ZBug {
    static final String version = "version 0.08";
    static boolean _debug = false;
    static boolean shownum = true;
    static String sep = "\t";
    static boolean fullopt = false;
    static boolean showcolor = true;
    void lex_debug(String s) {}
    void p_debug(String s) {
        //System.out.println("debug:" + s);
    }
    public static String[] _cmdnames = null;
    public static String[] cmdnames() {
        return _cmdnames;
    }
    static String prompt = "| ";
    static Vector<String> newcmds = new Vector<String>();
    static String _lstText = "";
    static Vector<String> _lines = new Vector<String>();
    static int _morenum = 80;
    static boolean _more = false;

    static HashMap<String,Integer> symbols = new HashMap<String, Integer>();
    public static HashMap<Integer,ZHolder> cmd_libs = new HashMap<Integer, ZHolder>();
    static int lastsym = 0;
    static int sym(String sym_) {
        String sym = sym_.trim();
        if (sym.length() == 0) throw new Zx("empty symbol");
        if (symbols.containsKey(sym)) return symbols.get(sym);
        symbols.put(sym, ++lastsym);
        return symbols.get(sym);
    }

    static String rsym(int i) {
        for(String s: symbols.keySet()) if (symbols.get(s) == i) return s;
        return null;
    }

    static void puts(String s) {
        out.println(s);
    }

    int read_string(Ll vl, String s, int i) {
        lex_debug("read_string");
        // read the starting quote
        char qc = ch(s,i);
        return read_generic_block(vl, s, i, qc, '\0');
    }

    char rev(char in) {
        switch (in) {
            case '{': return '}';
            case '}': return '{';
            case '[': return ']';
            case ']': return '[';
            case '(': return ')';
            case ')': return '(';
            case '<': return '>';
            case '>': return '<';
            default: throw new Zx("rev: no char " + in);
        }
    }

    int read_block(Ll vl, String s, int i) {
        lex_debug("read_block");
        // read the starting quote
        char start = ch(s,i);
        char end = rev(start);
        return read_generic_block(vl, s, i, end, start);
    }


    int read_generic_block(Ll vl, String s, int i, char end, char start) {
        StringBuffer sb = new StringBuffer();
        ++i;
        int stack = 1;
        while(true) {
            char c = ch(s,i);
            if (c == '\\') {
                sb.append('\\');
                ++i;
                sb.append(ch(s,i));
                ++i;
                continue;
            } else if (c == start) {
                sb.append(ch(s,i));
                ++i;
                stack++;
                continue;
            } else if (c == end) {
                --stack;
                if (stack > 0) {
                    sb.append(ch(s,i));
                    ++i;
                    continue;
                }
                if (stack < 0) throw new Zx("unstarted " + end);
                // stop, add to vl and return
                vl.add(new Lex(T_Lex.T_STRING, sym(sb.toString())));
                ++i;
                return i;
            } else if (c == 0) {
                throw new Zx("unterminated quote [" + sb.toString() +"]");
            } else {
                sb.append(ch(s,i));
                ++i;
            }
        }
    }

    int read_pipe(Ll vl, String s, int i) {
        lex_debug("read_pipe");
        vl.add(new Lex(T_Lex.T_PIPE));
        ++i;
        return i;
    }
    int read_rbrace(Ll vl, String s, int i) {
        lex_debug("read_rbrace");
        vl.add(new Lex(T_Lex.T_RBRACE));
        ++i;
        return i;
    }
    int read_lbrace(Ll vl, String s, int i) {
        lex_debug("read_lbrace");
        vl.add(new Lex(T_Lex.T_LBRACE));
        ++i;
        return i;
    }
    int skip_space(Ll vl, String s, int i) {
        lex_debug("skip_space");
        char c = ch(s,i);
        while(Character.isWhitespace(c)){
            ++i;
            c = ch(s,i);
            if (c == 0) return -1;
        }
        return i;
    }
    int read_sym(Ll vl, String s, int i) {
        lex_debug("read_sym[" + ch(s,i) + "]");
        char c;
        StringBuffer sb = new StringBuffer();
        while(true) {
            c = ch(s,i);
            if ((Character.isWhitespace(c)) ||
                    (c == '|') ||
                    (c == '{') ||
                    (c == '}') ) {
                vl.add(new Lex(T_Lex.T_SYM, sym(sb.toString())));
                return i;
            }
            if (c == 0) {
                vl.add(new Lex(T_Lex.T_SYM, sym(sb.toString())));
                return -1;
            }
            ++i;
            sb.append(c);
        }
    }

    char ch(String s, int i) {
        if (i >= s.length()) return 0;
        return s.charAt(i);
    }

    Ll parse(String s) {
        Ll vl = new Ll();
        char c = 0;
        int i = 0;
        while(i != -1) {
            c = ch(s,i);
            switch (c) {
                case 0:
                    for (Ll ll=vl.first; ll != null && ll.l != null; ll = ll.next)
                        p_debug(ll.l.xv());
                    p_debug("\n");
                    return vl;
                case '"':
                case '\'':
                    i = read_string(vl, s, i);
                    break;
                case '|':
                    i = read_pipe(vl, s, i);
                    break;
                case '}':
                    i = read_rbrace(vl, s, i);
                    break;
                case '{': 
                    i = read_lbrace(vl, s, i);
                    break;
                case ' ':
                case '\t':
                    i = skip_space(vl, s, i);
                    break;
                case '[': 
                    i = read_block(vl, s, i);
                    break;
                case ']': 
                    throw new Zx("lex:Unstarted ] ");
                default:
                    i = read_sym(vl, s, i);
            }
        }
        return vl;
    }

    ZHolder def_command(Lex l,final ZInst cmd) {
        // invoke and store the xml.
        if (l.lex_type != T_Lex.T_SYM) throw new Zx("Not a symbol[" + l.value() + "]");
        final String myname = l.value();
        final String res = cmd.invoke();
        ZHolder skel = new ZHolder() {
            public String name() {return myname;}
            public String tag() {return cmd.tag();}
            public Action type() {return cmd.type();}
            public String[] get_validopt() { return valid.toArray(new String[0]); }
            public String help() {
                return "[" + cmd.src() + "]";
            }
            public ZInst create() {
                return new ZInst(this) {
                    Vector<String> arglst = new Vector<String>();
                    String _result = null;
                    String result() {
                        if (_result == null) {
                            _result = res;
                            for (int i =0; i <args.size(); i++) {
                                _result = _result.replace("$"+(i+1), args.get(i));
                            }

                            HashMap<String,String> map = get_selectedopts();
                            for (String s : map.keySet()) {
                                _result = _result.replace("$"+s, map.get(s));
                            }
                        }
                        return _result;
                    }
                    ZInst _p = null;
                    ZInst parent() {

                        if (_p == null) _p = p_eval(result(), parent);
                        return _p;
                    }
                    public String src() {return res;}
                    Action type() {
                        return parent().type();
                    }
                    public String invoke() {
                        return parent().invoke();
                    }
                };
            }
        };
        cmd_libs.put(sym(myname), skel);
        newcmds.add(myname);
        ZBug.updatezcomp(); 
        return skel;
    };

    ZHolder get_command(Lex l) {
        if (l.lex_type != T_Lex.T_SYM) throw new Zx("Not a symbol[" + l.value() + "]");
        return cmd_libs.get(l.sym);
    }

    static void debug(Object s) {
        out.println("debug:\t" + s.toString());
    }

    String process_macros(String s) {
        Collections.sort(newcmds, Collections.reverseOrder());
        if (s.indexOf("$") != -1) {
            for(String str : newcmds) {
                if (s.indexOf("$") == -1) break;
                ZHolder z = cmd_libs.get(sym(str));
                String repl = z.create().src();
                s = s.replaceAll("\\$" + str, repl);
            }
        }
        return s;
    }

    static void show_more(String str) {
        _more = true;
        if (str.startsWith("q")) {
            _more = false;
            return;
        }
        if (_lines == null) {
            _lines = new Vector<String>(Arrays.asList(split_lines(_lstText)));
        }
        // for now.
        if (_lines.size() < _morenum) {
            for (String s: _lines)
                out.println(s);
            _lines = null;
            _more = false;
        } else {
            for (int i = 0; i < _morenum; ++i)
                out.println(_lines.get(i));
            _lines = new Vector<String>(_lines.subList(_morenum, _lines.size() - 1));
            out.println(">" + _lines.size() + " more. press q<Enter> to quit.");
        }
    }

    static void reset_more(int i) {
        _more = true;
        _morenum = i;
    }

    static void set_last(String s) {
        ZBug._lstText = s;
        ZBug._lines = new Vector<String>(Arrays.asList(split_lines(_lstText)));
    }

    private static String exec(final String[] cmd) throws IOException, InterruptedException {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();

        Process p = Runtime.getRuntime().exec(cmd);
        InputStream in = p.getInputStream();
        int c;
        while ((c = in.read()) != -1) bout.write(c);

        in = p.getErrorStream();
        while ((c = in.read()) != -1) bout.write(c);
        p.waitFor();
        return new String(bout.toByteArray());
    }
    private static String exec(final String cmd) throws IOException, InterruptedException {
        return exec(new String[] { "sh", "-c", cmd });
    }

    private static String stty(final String args) throws IOException, InterruptedException {
        return exec("stty " + args + " < /dev/tty").trim();
    }

    String eval(String str, ZInst prev) {
        try {
            ZInst cmd = p_eval(str, prev);
            if (cmd != null) {
                if (!cmd.name().equals(";")) {
                    ZInst doex = ZLib._do.create();
                    doex.parent(cmd);
                    cmd = doex;
                }
                return cmd.invoke();
            }
        } catch (Zx x) {
            return "error: " + x.getMessage();
        }
        return null;
    }

    ZInst p_eval(String str, ZInst prev) {
        //Ll vLex = parse(process_macros(str)).first;
        Ll vLex = parse(str).first;

        for (Ll ll=vLex; ll != null && ll.l != null; ll = ll.next)
            p_debug(ll.l.xv());

        int idx = 0;
        ZInst cmd = null;
        boolean defcmd = false;
        int stream_cmd = 0;
        ZHolder newcmd = null;
        while(vLex != null && vLex.l != null) {
            // assemble the command, invoke it, get the xml back.
            //Lex l = vLex.get(idx);
            Lex l = vLex.l;
            if (defcmd) {
                newcmd = def_command(l, prev);
                prev = null;
                defcmd = false;
                vLex = vLex.next;
                idx++;
                continue;
            }

            if (cmd == null && newcmd == null) {
                ZHolder z = get_command(l);
                if (z == null) throw new Zx("Not a command[" + l.value() + "]");
                cmd = z.create();
                vLex = vLex.next;
                idx++;
                //debug("has a parent " + prev);
                cmd.parent(prev);
                continue;
            }
            if (l.lex_type == T_Lex.T_PIPE) {
                // invoke the previous, get the xml,
                prev = cmd;
                cmd = null;
                vLex = vLex.next;
                idx++;
                continue;
            }
            if (l.lex_type == T_Lex.T_RBRACE) {
                // create new command
                prev = cmd;
                cmd = null;
                defcmd = true;
                vLex = vLex.next;
                idx++;
                continue;
            }
            if (l.lex_type == T_Lex.T_LBRACE) {
                ++stream_cmd;
                vLex = vLex.next;
                idx ++;
                continue;
            }

            String value = l.value();
            // replace macros first.
            value = process_macros(value);

            switch(stream_cmd) {
                case 1: // file input
                    if (value.startsWith("~")) {
                        value = home + value.substring(1);
                    }
                    value = read_file(value);
                    break;
                case 2:
                    try {
                        StringBuffer sb = new StringBuffer();
                        History h = reader.getHistory();
                        reader.setHistory(new History());
                        //stty("echo");
                        //stty("erase");
                        while(true) {
                            //String us = reader.readLine('\0');
                            String us = reader.readLine();
                            if (us.equals(value)) break;
                            sb.append(us);
                            sb.append("\n");
                            //out.println(us);
                        }
                        // set the variable
                        set_var(value, sb.toString()); 
                        //stty("-erase");
                        //stty("-echo");
                        reader.setHistory(h);
                        value = sb.toString();
                    } catch (Exception e) {
                        err(e);//ignore
                    }
                    break; // heredoc
            }
            stream_cmd = 0;
            if (newcmd != null) {
                newcmd.add_validopt(value);
            } else {
                cmd.add(value);
            }
            vLex = vLex.next;
            ++idx;
        }
        return cmd;
    }

    static void out(Object o) {if (o != null) out.println(o.toString());}
    static void out(String o) {if (o != null) out.println(o);}

    static String readstream(InputStream is) {
        StringBuffer sb = new StringBuffer();
        try {
            BufferedReader ib = new BufferedReader(new InputStreamReader(is));
            String temp = ib.readLine();
            while (temp !=null){
                sb.append (temp);
                sb.append ("\n");
                temp = ib.readLine();
            }
        } catch (Exception e) {
            err(e);
            throw new Zx("readstream:" + e.getMessage());
        }
        return sb.toString();
    }

    static BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
    static String _gets(String p) {
        try {
            out.print(p);
            return br.readLine();
        } catch (Exception e) {
            return null;
        }
    }

    static ConsoleReader reader = null;
    static String gets(String p) {
        try {
            return reader.readLine(p);
        } catch (Exception e) {
            return null;
        }
    }

    static ConsoleReader reader() {
        return reader;
    }

    static ZCompletor zcomp = null;
    static OptionCompletor optcomp = null;
    static void updatezcomp() {
        Vector<String> cmds = new Vector<String>();
        for(int i: cmd_libs.keySet()) cmds.add(rsym(i));
        _cmdnames = cmds.toArray(new String[0]);
        zcomp.setCandidateStrings(_cmdnames);
    }

    public static void init_jline() {
        try {
            System.setProperty("jline.keybindings", home+ File.separator + ".zbug.keys");

            reader = new ConsoleReader();
            zcomp = new ZCompletor(new String[0]);
            updatezcomp();
            List<Completor> completors = new LinkedList<Completor>();
            completors.add(zcomp);
            optcomp = new OptionCompletor();
            completors.add(optcomp);
            reader.addCompletor(new ArgumentCompletor(completors.toArray(new Completor[0]),
                        new ArgumentCompletor.AbstractArgumentDelimiter() {
                            public boolean isDelimiterChar(String buffer, int pos) {
                                return Character.isWhitespace(buffer.charAt(pos)); 
                            }
                        }));
            reader.addCompletor(new MultiCompletor(completors.toArray(new Completor[0])));
            
            String histName = home + File.separator + ".zbug_history";
            File histFile = new File(histName); 
            History hist = new History(histFile);
            reader.setHistory(hist);
        } catch (Exception e) {
            err(e);
            throw new Zx("readline:" + e.getMessage());
        }
    }

    static String edit(String content) {
        String name = write_temp_file(content);
        try {
            if(editor(name) == 0) {
                return read_file(name);
            } else return null;
        } finally {
            (new File(name)).delete();
        }
    }

    static int editor(String file) {
        try {
            String editor = System.getenv("VISUAL");
            if (editor == null || !(new File(editor.split("[ \t]+")[0])).exists()) {
                ZBug.out("editor does not exist. (set env VISUAL to absolute path)");
                ZBug.out("file: " + file);
                String s = "n";
                while (!s.equals("y")) {
                    s = gets("enter y to continue: ");
                }
                return 0;
            }
            return Runtime.getRuntime().exec(editor + " " + file).waitFor();
        } catch (Exception e) {
            err(e);
            throw new Zx("exec:" + e.getMessage());
        }
    }

    static String write_temp_file(String content) {
        File tmp = null;
        try {
            tmp = File.createTempFile(".review", ".tmp" );
            tmp.deleteOnExit();
            BufferedWriter out = new BufferedWriter(new FileWriter(tmp));
            out.write(content);
            out.close();
            return tmp.getCanonicalPath();
        } catch (Exception e) {
            err(e);
            throw new Zx("writetempfile:" + " " + e.getMessage());
        }
    }

    static int write_file(String name, String content) {
        try {
            BufferedWriter out = new BufferedWriter(new FileWriter(name));
            out.write(content);
            out.close();
        } catch (Exception e) {
            err(e);
            throw new Zx("writefile:" + name + " " + e.getMessage());
        }
        return content.length();
    }

    static String read_file(String f) {
        StringBuffer sb = new StringBuffer();
        try {
            File fi = new File(f.trim());
            if (fi.isDirectory()) {
                String[] entries = fi.list();
                for (String e : entries) {
                    sb.append(e + "\n");
                }
            } else {
                BufferedReader ib = new BufferedReader(new InputStreamReader(new FileInputStream(fi)));
                while(true) {
                    String st = ib.readLine();
                    if (st ==null) break;
                    if (st.startsWith("#")) continue;
                    sb.append(st);
                    sb.append("\n");
                }
            }
        } catch (Exception e) {
            err(e);
            throw new Zx("readfile:" + e.getMessage());
        }
        return sb.toString();
    }

    static void set_var(final String key,final  String val) {
        ZHolder var =  new ZHolder() {
            public String name() {return key;}
            public String tag() {return key;}
            public String help() {return "defined as variable";}
            public Action type() {return Action.T_NONE;}
            public String[] get_validopt() { return new String[0];}
            public ZInst create() {
                return new ZInst(this) {
                    public String src() {return val;}
                    public String invoke() {
                        return val;
                    }
                };
            }
        };
        cmd_libs.put(sym(key), var);
        newcmds.add(key);
    }


    static AuthenticationClient ac = null;
    static AuthenticationClient ac() {
        try {
            auth_thread.join();
            return ac;
        } catch (Exception e) {
            err(e);
            throw new Zx("auth:" + e.getMessage());
        }
    }

    static String login(String user, String pass, boolean enc) {
        try {
            ac.setUserID(user);
            ac.setPassword(pass);
            ac.setEncrypted(enc);
            ByteArrayOutputStream xmlout = new ByteArrayOutputStream();

            ByteArrayOutputStream tmp = new ByteArrayOutputStream();
            PrintStream ps = new PrintStream(tmp);
            System.setOut(ps);
            SOAPServiceMessage msg = ac.doProcess(false);
            msg.getMessage().writeTo(xmlout);
            String str = xmlout.toString();
            Pattern r =  Pattern.compile("<Email>(.*)</Email>");
            Matcher m = r.matcher(str);
            if (m.find()) {
                email = m.group(1);
            } else {
                throw new Zx("unable to login.");
            }
            token = ac.getToken();
            ZBug.set_var("email", ZBug.email);
            return token;
        } catch (Exception e) {
            err(e);
            throw new Zx("login:" + e.getMessage());
        } finally {
            System.setOut(out);
        }
    }

    static Thread auth_thread = null;
    static String pfile = null;
    static String user = null;
    static String enc = null;
    static String home = null;
    static String email = null;
    static String token = null;
    static void init_auth() {
        try {
            auth_thread = new Thread() {
                public void run() {
                    String s = ZBug.prompt;
                    ZBug.prompt = "* ";
                    try {
                        Thread.yield();
                        ac = new AuthenticationClient();
                        token = login(user, read_file(pfile).trim(), true);
                        ZBug.prompt = s;
                    } catch (Exception e) {
                        System.out.println("auth:" + e.getMessage());
                        System.out.println("use *login or *savepwd");
                    }
                }
            };
            auth_thread.start();
        } catch (Exception e) {
            System.out.println("auth:" + e.getMessage());
        }
    }

    static InputArgs ia = null ;
    static String[] split_lines(String s) {
        return s.split("\n");
    }
    public static PrintStream out = System.out;
    public static Queue<String> statement_queue = new LinkedList<String>();
    static String queue_saved_prompt = null;

    static void doit() {
        try {
            EncryptionClient ec = new EncryptionClient();
            ec.setUserID("rn151090");
            ec.setText("a0^icare4u");
            ByteArrayOutputStream xmlout = new ByteArrayOutputStream();
            SOAPServiceMessage msg = ec.doProcess(true);
            msg.getMessage().writeTo(xmlout);
            String str = xmlout.toString();
            Pattern r =  Pattern.compile("<TextEncryptionService:Text>(.*)</TextEncryptionService:Text>");
            Matcher m = r.matcher(str);
            if (m.find()) {
               String enc = m.group(1);
                System.out.println(enc);
            } else {
                throw new Zx("unable to login.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        System.out.println(version);
        System.out.flush();
        String[] Args = null;
        user = System.getenv("LOGNAME");
        home = System.getenv("HOME");
        String debug = System.getenv("ZDEBUG");
        if (debug != null && debug.length() > 0) {
            _debug = true;
            Args = new String[] {"-U","me","-pFile","just_pass_this_to_make_the_thing_happy", "-rawxml", "-debug"};
        } else {
            Args = new String[] {"-U","me","-pFile","just_pass_this_to_make_the_thing_happy", "-rawxml"};
        }
        if (args.length == 0) {
            pfile = home +"/.bugster";
            Args[1] = user;
            Args[3] = pfile;
            ia = new InputArgs(Args);
        } else {
            ia = new InputArgs(args);
        }
        init_auth();
        ZBug zb = new ZBug();
        ZLib zl = new ZLib();
        zl.init(cmd_libs);
        init_jline();
        String rc = home +"/.zbugrc";
        File frc = new File(rc);
        if (frc.exists()) for (String s : split_lines(read_file(rc))) {
            String st = zb.eval(s, null);
            if (st!= null && st.length() >0) out(st);
        }
        if (args.length > 4) {
            String n = args[4];
            File f = new File(n);
            if (f.exists())
                for (String s : split_lines(read_file(n))) {
                    String st = zb.eval(s, null);
                    if (st != null && st.length() >0) out(st);
                }
            else
                System.out.println("evalfile: File does not exist(" + n+ ")");
            return;
        }
        while(true) {
            try {
                String statement = gets(prompt);
                if (statement == null) System.exit(0);
                if (_more) {
                    show_more(statement);
                } else if (statement.startsWith("#") ) {
                    if (statement.equals("#?")) {
                        ZBug.out("#debug #nodebug #crnum #fullopt #shortopt #nocrnum #color #nocolor #sep #prompt #wait");
                    } if (statement.equals("#debug")) {
                        _debug = true;
                    } else if (statement.equals("#nodebug")) {
                        _debug = false;
                    } else if (statement.equals("#crnum")) {
                        shownum= true;
                    } else if (statement.equals("#fullopt")) {
                        fullopt= true;
                    } else if (statement.equals("#shortopt")) {
                        fullopt= false;
                    } else if (statement.equals("#nocrnum")) {
                        shownum= false;
                    } else if (statement.equals("#color")) {
                        showcolor= true;
                        ZBug.out(Colors.allcolors());
                    } else if (statement.equals("#nocolor")) {
                        showcolor= false;
                    } else if (statement.startsWith("#sep")) {
                        sep= statement.substring(0, statement.indexOf(' ')+1);
                    } else if (statement.startsWith("#promp")) {
                        prompt= statement.substring(statement.indexOf(' ')+1);
                    } else if (statement.startsWith("#wait")) {
                        ac();
                    }
                } else if (statement.endsWith("|")) {
                    queue_saved_prompt = "|";
                    prompt = "+\t";
                    statement_queue.offer(statement);
                } else if (statement.endsWith("#")){
                    // do nothing
                }
                else {
                    StringBuffer sb = new StringBuffer();
                    while (statement_queue.peek() != null) {
                        sb.append(statement_queue.remove());
                    }
                    sb.append(statement);
                    if (queue_saved_prompt != null) {
                        prompt = queue_saved_prompt;
                        queue_saved_prompt = null;
                    }
                    out(zb.eval(sb.toString(), null));
                }
            } catch (Exception e) {
                err(e);
                System.out.println("zbug:" + e.getMessage());
            }
        }
    }

    public static void err(Exception e) {
        if (_debug) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }

    public static void terr(String msg) {
        throw new Zx(msg);
    }
}


