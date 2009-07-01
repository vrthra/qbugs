package zbug;
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

