package zbug;
public class Zx extends RuntimeException {
    public Zx(String s) { super(s); }

    public static void err(Exception e) {
        if (ZBug._debug) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }
}


