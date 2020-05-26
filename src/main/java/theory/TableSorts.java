package theory;

import com.microsoft.z3.*;

public class TableSorts {
    private Context ctx;
    private DatatypeSort singleton_val = null;
    private ArraySort singleton_tbl = null;
    private ArraySort singleton_col = null;
    private EnumSort singleton_op = null;
    private EnumSort singleton_aggr = null;
    private EnumSort singleton_lop = null;

    public TableSorts(Context ctx) {
        this.ctx = ctx;
    }

    public ArraySort colSort() {
        if (this.singleton_col == null) {
            this.singleton_col = ctx.mkArraySort(ctx.getIntSort(), this.valSort());
        }
        return this.singleton_col;
    }

    public ArraySort tblSort(){
        if (this.singleton_tbl == null) {
            this.singleton_tbl = ctx.mkArraySort(ctx.getStringSort(), this.colSort());
        }
        return this.singleton_tbl;
    }

    public DatatypeSort valSort() {
        if (this.singleton_val == null) {
            Constructor nil = ctx.mkConstructor("nil", "is_nil", null, null, null);
            Constructor realv = ctx.mkConstructor("realv", "is_realv",
                    new String[]{"realv"},
                    new Sort[]{ctx.getRealSort()},
                    new int[]{0});
            Constructor strv = ctx.mkConstructor("strv", "is_strv",
                    new String[]{"strv"},
                    new Sort[]{ctx.getStringSort()},
                    new int[]{0});
            this.singleton_val = ctx.mkDatatypeSort("Val", new Constructor[]{nil, realv, strv});
        }
        return this.singleton_val;
    }

    public EnumSort opSort() {
        if (this.singleton_op == null) {
            this.singleton_op = this.ctx.mkEnumSort(
                    this.ctx.mkSymbol("Op"),
                    this.ctx.mkSymbol("="),
                    this.ctx.mkSymbol("!="),
                    this.ctx.mkSymbol(">"),
                    this.ctx.mkSymbol("<"),
                    this.ctx.mkSymbol(">="),
                    this.ctx.mkSymbol("<=")
            );
        }
        return this.singleton_op;
    }

    public EnumSort aggrSort(){
        if (this.singleton_aggr == null) {
            this.singleton_aggr = this.ctx.mkEnumSort(
                    this.ctx.mkSymbol("Aggr"),
                    this.ctx.mkSymbol("count"),
                    this.ctx.mkSymbol("sum"),
                    this.ctx.mkSymbol("average"),
                    this.ctx.mkSymbol("max"),
                    this.ctx.mkSymbol("min")
            );
        }
        return this.singleton_aggr;
    }

    public EnumSort lopSort(){
        if (this.singleton_lop == null) {
            this.singleton_lop = this.ctx.mkEnumSort(
                    this.ctx.mkSymbol("LogicOp"),
                    this.ctx.mkSymbol("and"),
                    this.ctx.mkSymbol("or")
            );
        }
        return this.singleton_lop;
    }

    public BoolExpr isNilVal(DatatypeExpr v) {
        return (BoolExpr) ctx.mkApp(valSort().getRecognizers()[0], v);
    }

    public BoolExpr isRealVal(DatatypeExpr v) {
        return (BoolExpr) ctx.mkApp(valSort().getRecognizers()[1], v);
    }

    public BoolExpr isStrVal(DatatypeExpr v) {
        return (BoolExpr) ctx.mkApp(valSort().getRecognizers()[2],v);
    }

    public RealExpr getRealVal(DatatypeExpr v) {
        return  (RealExpr)ctx.mkApp(valSort().getAccessors()[1][0], v);
    }

    public SeqExpr getStrVal(DatatypeExpr v) {
        return (SeqExpr) ctx.mkApp(valSort().getAccessors()[2][0], v);
    }

    public DatatypeExpr mkRealVal(String s){
        FuncDecl f = this.valSort().getConstructors()[1];
        return (DatatypeExpr)ctx.mkApp(f, ctx.mkReal(s));
    }

    public DatatypeExpr mkRealVal(Expr s){
        FuncDecl f = this.valSort().getConstructors()[1];
        return (DatatypeExpr)ctx.mkApp(f, s);
    }

    public DatatypeExpr mkStrVal(String s){
        FuncDecl f = this.valSort().getConstructors()[2];
        return (DatatypeExpr)ctx.mkApp(f, ctx.mkString(s));
    }

    public DatatypeExpr mkNilVal(){
        FuncDecl f = this.valSort().getConstructors()[0];
        return (DatatypeExpr)ctx.mkApp(f);
    }


    public SeqExpr mkStringExpr(String s) {
        return (SeqExpr)ctx.mkConst(s, ctx.getStringSort());
    }

    public ArrayExpr mkTblExpr(String name) {
        return (ArrayExpr)ctx.mkConst(name, tblSort());
    }

    public DatatypeExpr mkValExpr(String s){
        return (DatatypeExpr)ctx.mkConst(s, valSort());
    }


    public DatatypeExpr select(ArrayExpr tbl, SeqExpr h, IntExpr r) {
        return (DatatypeExpr) ctx.mkSelect((ArrayExpr) ctx.mkSelect(tbl, h), r);
    }

    public DatatypeExpr select(ArrayExpr tbl, String h, int r) {
        return (DatatypeExpr) ctx.mkSelect((ArrayExpr) ctx.mkSelect(tbl, ctx.mkString(h)), ctx.mkInt(r));
    }

    public DatatypeExpr select(ArrayExpr tbl, SeqExpr h, int r) {
        return (DatatypeExpr) ctx.mkSelect((ArrayExpr) ctx.mkSelect(tbl, h), ctx.mkInt(r));
    }

    public DatatypeExpr select(ArrayExpr tbl, String h, IntExpr r) {
        return (DatatypeExpr) ctx.mkSelect((ArrayExpr) ctx.mkSelect(tbl, ctx.mkString(h)), r);
    }

}
