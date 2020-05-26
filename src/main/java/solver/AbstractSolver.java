package solver;

import com.microsoft.z3.*;
import table.lang.Table;
import table.lang.datatype.ValType;
import table.lang.datatype.Value;
import theory.TableSorts;
import util.Olog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

abstract class AbstractSolver {
    Context ctx;
    Solver solver;
    TableSorts ts;
    public Table input, output;
    ArrayExpr inputExpr, outputExpr;
    public static int Z3TIMEOUT = 120000;
    protected int nfilter ;

    public int status = -1;  //0 for unsat, 1 for sat
    public int time_usage = 0;


    public AbstractSolver() {
        HashMap<String, String> cfg = new HashMap<String, String>();
        cfg.put("model", "true");
        this.ctx = new Context(cfg);
        this.solver = ctx.mkSolver();
        this.ts = new TableSorts(ctx);
        this.inputExpr = this.ts.mkTblExpr("input");
        this.outputExpr = this.ts.mkTblExpr("output");

        if (Z3TIMEOUT > 0) {
            Params p = ctx.mkParams();
            p.add("timeout", Z3TIMEOUT);
            solver.setParameters(p);
        }
    }

    public void setNfilter(int nfilter) {
        this.nfilter = nfilter;
    }

    abstract public void popInOutTables();

    abstract public void addUnknowns();

    protected List<BoolExpr> encodeDomain() {
        return new ArrayList<>();
    }

    protected List<BoolExpr> encodeRelation(){
        return new ArrayList<>();
    }

    public Status solve() {
        popInOutTables();
        addUnknowns();

        List<BoolExpr> inputCons = encodeTable(inputExpr, input);
        List<BoolExpr> outputCons = encodeTable(outputExpr, output);
        addConstrainsToSolver(inputCons);
        addConstrainsToSolver(outputCons);

        List<BoolExpr> domainCons = encodeDomain();
        addConstrainsToSolver(domainCons);

        List<BoolExpr> relationCons = encodeRelation();
        addConstrainsToSolver(relationCons);

        long stime = System.currentTimeMillis();
        Status status = solver.check();
        long etime = System.currentTimeMillis();

        if (status == Status.SATISFIABLE) {
            Model model = solver.getModel();
            Olog.log.debug(input.toString());
            Olog.log.debug(output.toString());
            System.out.println("solver: \n" + solver);
            System.out.println("model: \n" + model.toString());
        } else {
            System.out.println("solver: \n" + solver);
            System.out.println("unsat");
        }


        Olog.log.debug("Z3 time usage: " + (etime-stime) + " miniseconds");
        return  status;
    }

    public List<BoolExpr> encodeTable(ArrayExpr e, Table t) {
        List<BoolExpr> ret = new ArrayList<>();
        for (int col = 0; col < t.ncol(); col++){
            for (int row = 0; row < t.nrow(); row++) {
                Value value = t.get(col, row);
                String header = t.header(col);
                DatatypeExpr ve = null;
                if (value.getValType() == ValType.NumberVal){
                    ve = ts.mkRealVal(value.toString());
                } else  {
                    ve = ts.mkStrVal(value.toString());
                }

                ret.add(
                        ctx.mkEq(ts.select(e, header, row), ve)
                );
            }
        }

        // out of bound
        /*
        for (int col = 0; col < t.ncol(); col++) {
            IntExpr[] vars = {ctx.mkIntConst("r")};
            IntExpr var = vars[0];
            ret.add(
                    ctx.mkForall(
                            vars,
                            ctx.mkImplies(
                                    ctx.mkOr(
                                            ctx.mkLt(var, ctx.mkInt(0)),
                                            ctx.mkGt(var, ctx.mkInt(t.nrow() - 1))
                                    ),
                                    ctx.mkEq(ts.select(e, ctx.mkString(t.header(col)), var), ts.mkNilVal())
                            ),
                            1, null, null, null, null
                    )
            );
        }
        */
        return ret;
    }

    protected void addConstrainsToSolver(List<BoolExpr> bs){
        for (BoolExpr b : bs)
            solver.add(b);
    }

    protected  BoolExpr isProjection(Expr expr) {
        BoolExpr[] ors = new BoolExpr[(input.ncol())];
        for (int col = 0; col < input.ncol(); col++) {
            ors[col] = ctx.mkEq(expr, ctx.mkString(input.header(col)));
        }
        return ctx.mkOr(ors);
    }

    public String eval(Expr expr) {
        String s = solver.getModel().eval(expr, false).toString();
        if (s.startsWith("\""))
                return  s.substring(1, s.length()-1);
        else
            return s;
    }

}
