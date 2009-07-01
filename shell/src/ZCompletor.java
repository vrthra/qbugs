package zbug;

import java.util.*;
import java.io.*;
import java.net.*;
import jline.*;

public class ZCompletor implements Completor {
    SortedSet candidates;

    /**
     *  Create a new ZCompletor with a list of possible completion
     *  values.
     */
    public ZCompletor(final String[] strings) {
        setCandidateStrings(strings);
    }

    public void setCandidateStrings(final String[] strings) {
        candidates = new TreeSet(Arrays.asList(strings));
    }


    public static void  debug(String s) {
        ZBug.out.println("[complete: " + s + "]");
    }

    public int complete(final String buffer, final int cursor, final List clist) {
        String start = (buffer == null) ? "" : buffer;

        SortedSet matches = candidates.tailSet(start);

        for (Iterator i = matches.iterator(); i.hasNext();) {
            String can = (String) i.next();

            if (!(can.startsWith(start))) break;
            clist.add(can);
        }

        switch (clist.size()) {
            case 1:
                clist.set(0, ((String) clist.get(0)) + " ");
                break;
            case 0:
                if (buffer != null) clist.add(buffer);
        }
        // the index of the completion is always from the beginning of
        // the buffer.
        return (clist.size() == 0) ? (-1) : 0;
    }
}


class OptionCompletor implements jline.Completor {
    String _command = null;
    ArrayList<String> _options = null;
    public static final String HYPHEN = "-";
    public static final String OPTION_START = "-";
    public static final String PIPE = "|";

    static BufferedWriter _dbg = null;
    static void init_dbg() {
        try {
            _dbg = new BufferedWriter(new FileWriter("/tmp/zbug.dbg", true));
            dbg("----------------------------------init\n");
        } catch (Exception e) {
            Zx.err(e);
        }
    }

    static void dbg(String content) {
        /*if (_dbg == null) init_dbg();
        try {
            _dbg.write(content + "\n");
            _dbg.flush();
        } catch (Exception e) {
            throw new Zx("dbg: " + e.getMessage());
        }*/
    }

    static void at(int l) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < l; ++i) {
            sb.append(' ');
        }
        sb.append("^" +l +"\n");
        dbg(sb.toString());
    }


    //Processes the current buffer to get the current command
    //and options filled already.
    private void processLine(String line, int pos) {
        String [] words = line.trim().split("[ \t]+");
        _command = words[0];
        int excludeLast = 1;
        //Take care of situations where the --option is complete but tabbing
        //still happens. we do not want to remove the option
        if (line.charAt(pos - 1) == ' ' ) excludeLast = 0;
        _options = new ArrayList<String>();
        _options.add(PIPE + _command);
        for (int i=1; i<(words.length - excludeLast); ++i ) {
            String word = words[i];
            if (word.startsWith(PIPE) || word.startsWith(OPTION_START)) { //we need only commands
                _options.add(word);
            }
        }
    }
    static void debug(String arg) {
        ZBug.out.println("<" + arg + ">");
    }

    //This is the function that is called for completing or listing the available completions
    //for the string in the buffer. buffer holds the current word that is being completed,
    //cursor holds the position of the cursor with in the word and clist is the list of 
    //valid completions that will be returned.
    public int logit(final String buffer, final int r_cursor, final List clist) {
        try {
            int cursor = r_cursor - 1;
            //r_cursor contains the char exactly where the cursor is. so what we want is often
            //the just prev char.
            //
            // buffer and cursor contain the last completed words. while _buffer and _cursor
            // contains the entire line.

            /*String _buffer =  ZBug.reader().getCursorBuffer().buffer.toString();
            int _cursor =  ZBug.reader().getCursorBuffer().cursor - 1;*/
            if (cursor < 0) {
                dbg("*");
                return -1;
            }
            dbg(buffer + "$==");
            if (buffer != null)
                if (buffer.length() > cursor)
                    dbg("[" + buffer.charAt(cursor) + "]");
                else
                    dbg("[<" + cursor + ">]");
            at(cursor);
        } catch (Exception e){
            Zx.err(e);
        }
        return clist.size () == 0 ? -1 : 0;
    }

    public int complete (final String buf_word, final int cur_word, final List clist) {
        try {
            //logit(buf_word, cur_word, clist);
            String buf_line_ =  ZBug.reader().getCursorBuffer().buffer.toString();
            int cur_line =  ZBug.reader().getCursorBuffer().cursor;
            String buf_line = buf_line_.substring(0, cur_line);
            dbg(">" + buf_line + "<");


            boolean haspipe = (buf_line.charAt(buf_line.length() -1) == '|');

            int start = buf_line.lastIndexOf('|');
            start++; // if start == -1, we start at 0 else skip pipe

            String mycmd = "";
            if (start >= buf_line.length()) {
                mycmd = "";
            } else {
                mycmd = buf_line.substring(start);
            }
            dbg("+" + mycmd + "+");

            // are we completing option or a command?
            boolean isopt = false; 

            if (mycmd.trim().length() == 0) {
                isopt = false;
            } else if (mycmd.charAt(mycmd.length() -1) == ' ') {
                isopt = true;
            } else if (mycmd.trim().lastIndexOf(' ') != -1) {
                isopt = true;
            }

            LinkedList<String> lst = new LinkedList<String>();
            if (!isopt) {
                for (String c : ZBug.cmdnames()) {
                    if (c.startsWith(mycmd)) {
                        lst.add(PIPE + c);
                    }
                }
                processLine(buf_line, cur_line); // remove commands
            } else {
                int fspace = mycmd.indexOf(' ');
                String command = mycmd.substring(0,fspace).trim();
                int lspace = mycmd.lastIndexOf(' ');
                String opt = mycmd.substring(lspace + 1);
                if (opt.startsWith("{")) {
                    return filecomplete(buf_word, cur_word, clist);
                }
                ZHolder h = ZBug.cmd_libs.get(ZBug.sym(command));


                String[] opts = new String[0];
                if (h != null) opts = h.get_validopt();
                if (opts == null) opts = new String[0];
                for (String c : opts) {
                    if (c.startsWith(opt.trim())) lst.add(c);
                }
                processLine(mycmd, mycmd.length()  -1); // remove commands
            }

            //remove all that we have completed already
            //if ((buf_line.indexOf("*show") == -1) && (buf_line.indexOf("*sort") == -1)) {
                // bug when either comes. - TODO
                lst.removeAll(_options);
            //}

            switch (lst.size()) {
                case 1:
                    // if there is only one choice, complete appending a space to option
                    String op = lst.get(0);
                    dbg("1[" + op + "]");
                    clist.add(op + ' ');
                    break;
                case 0:
                    dbg("0[" + buf_word + "]");
                    if(buf_word != null)
                        clist.add(buf_word);//do not add space if it is user completed*/
                    break;
                default:
                    dbg("def " + lst.size() + "[" + "]");
                    for (String s : lst) {
                        dbg("\t >" + s + "|");
                        clist.add(s);
                    }
            }
        } catch (Exception e){
            Zx.err(e);
        }
        return clist.size () == 0 ? -1 : 0;
    }

    //Files
    //
    public int filecomplete(final String buf, final int cursor, final List candidates) {
        String buffer = (buf == null) ? "" : buf;

        String translated = buffer.substring(1); // remove "{"
        String append = "{";

        // special character: ~ maps to the user's home directory
        if (buffer.startsWith( "{~/")) {
            translated = ZBug.home + translated.substring(1);
        }

        File f = new File(translated);

        final File dir;

        if (translated.endsWith("/")) { /*TODO check also that the trans + / is a directory*/
            dir = f;
        } else {
            dir = f.getParentFile();
        }

        final File[] entries = (dir == null) ? new File[0] : dir.listFiles();
        if ( entries == null || entries.length == 0) return -1;

        // filter based on the starting string given.
        Vector<String> v = new Vector<String>();
        for (int i = 0; i < entries.length; i++) {
            String s = entries[i].getAbsolutePath();
            if (s.startsWith(translated)) {
                if (entries[i].isDirectory())
                    v.add(get_name(entries[i].getAbsolutePath() + "/", buffer));
                else
                    v.add(get_name(entries[i].getAbsolutePath(), buffer));
            }
        }

        try {
            for (String s : v) candidates.add(append + s + " ");
            return candidates.size() == 0 ? -1 : 0;
        } finally {
            // we want to output a sorted list of files
            Collections.sort(candidates);
        }
    }

    private String get_name(String fpath, String cstr) {
        if (cstr.startsWith("{~/")) {
            return "~/" + fpath.substring(ZBug.home.length() +1);
        }
        return fpath;
    }

    private void out(String arg) {
        ZBug.out.println("[" + arg + "]");
    }

    private void out(Collection args) {
        ZBug.out.println("{");
        for (Object o : args) ZBug.out.println("[" + o+ "]");
        ZBug.out.println("}");
    }
}

