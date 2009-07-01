package zbug;
import java.io.*;
import java.util.*;
public class ZUtil {
    public static int write_file(String name, String content) {
        try {
            BufferedWriter out = new BufferedWriter(new FileWriter(name));
            out.write(content);
            out.close();
        } catch (Exception e) {
            Zx.err(e);
            throw new Zx("writefile:" + name + " " + e.getMessage());
        }
        return content.length();
    }

    public static String read_file(String f) {
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
            Zx.err(e);
            throw new Zx("readfile:" + e.getMessage());
        }
        return sb.toString();
    }

}

