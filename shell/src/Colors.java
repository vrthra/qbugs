package zbug;
import java.util.*;
import java.util.regex.*;

class CField {
    String color;
    String field;
    Pattern regexp;
}
public class Colors {
    public static String RED_="\033[1;41m";
    public static String _RED="\033[31m";
    public static String GREEN_="\033[1;42m";
    public static String _GREEN="\033[1;32m";
    public static String YELLOW_="\033[1;43m";
    public static String _YELLOW="\033[1;33m";
    public static String BLUE_="\033[1;44m";
    public static String _BLUE="\033[1;34m";
    public static String MAGENTA_="\033[1;45m";
    public static String _MAGENTA="\033[1;35m";
    public static String CYAN_="\033[1;46m";
    public static String _CYAN="\033[1;36m";
    public static String WHITE_="\033[1;47m";
    public static String _WHITE="\033[1;37m";
    public static String RESET="\033[0m";

    public static String red(String s) { return _RED + s + RESET; }
    public static String blue(String s) { return _BLUE + s + RESET; }
    public static String green(String s) { return _GREEN + s + RESET; }
    public static String yellow(String s) { return _YELLOW + s + RESET; }
    public static String magenta(String s) { return _MAGENTA + s + RESET; }
    public static String cyan(String s) { return _CYAN + s + RESET; }
    public static String white(String s) { return _WHITE + s + RESET; }

    private static String with_color(String str, String color) {
        if (color.equals("red")) return red(str);
        if (color.equals("green")) return green(str);
        if (color.equals("yellow")) return yellow(str);
        if (color.equals("blue")) return blue(str);
        if (color.equals("magenta")) return magenta(str);
        if (color.equals("cyan")) return cyan(str);
        if (color.equals("white")) return white(str);
        return str;
    }

    public static String colorize(String s, String color) {
        if (color == null) return s;
        if (!ZBug.showcolor) return s;
        return with_color(s, color);
    }

    public static String allcolors() {
        String[] colors = {"red", "green", "yellow", "blue", "magenta", "cyan", "white" };
        StringBuffer sb = new StringBuffer();
        for (String s: colors) {
            sb.append(with_color(s,s));
            sb.append(" ");
        }
        return sb.toString();
    }
}
