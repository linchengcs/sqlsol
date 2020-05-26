package solver;

import com.microsoft.z3.*;
import table.lang.Table;
import util.Helper;
import util.Olog;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SelectWhere extends  AbstractSolver{
    public SeqExpr[] projXs;
    public SeqExpr[] fiPrXs;
    public Expr[] fiOpXs;
    public DatatypeExpr[] fiVaXs;
    public Expr[] fiLopXs;
    public BoolExpr fiMaX;
    public  int nfilter = 2;


    @Override
    public void popInOutTables() {
        input = new Table(
                "input",
                Arrays.asList("id1", "name1"),
                Arrays.asList(
                        Arrays.asList("1", "john"),
                        Arrays.asList("2", "bob")
                ));
        output = new Table(
                "output",
                Arrays.asList("id2", "name2"),
                Arrays.asList(
                        Arrays.asList("1", "john")
                ));
    }

    @Override
    public void addUnknowns() {
        // projection unknowns
        projXs = new SeqExpr[output.ncol()];
        for (int col = 0; col < output.ncol(); col++)
            projXs[col] = ts.mkStringExpr("projX" + col);
        // filter unknowns
        fiPrXs = new SeqExpr[nfilter];
        fiOpXs = new Expr[nfilter];
        fiVaXs = new DatatypeExpr[nfilter];
        for (int i = 0; i < nfilter; i++) {
            fiPrXs[i] = ts.mkStringExpr("fiPrX" + i);
            fiOpXs[i] = ctx.mkConst("fiOpX" + i, ts.opSort());
            fiVaXs[i] = ts.mkValExpr("fiVaX" + i);
        }
        fiLopXs = new Expr[nfilter-1];
        for (int i = 0; i < fiLopXs.length; i++) {
            fiLopXs[i] = ctx.mkConst("fiLopX" + i, ts.lopSort());
        }
        // no filter clause mask unknown
        fiMaX = (BoolExpr)ctx.mkConst("fiMaX", ctx.getBoolSort());
    }

    public void addUnknowns(int nfilter) {
        nfilter = nfilter;
        addUnknowns();
    }

    public List<BoolExpr> encodeDomain(){
        List<BoolExpr> ret  = new ArrayList<>();
        for (int i = 0; i < projXs.length; i++) {
            ret.add(isProjection(projXs[i]));
        }
        for (int i = 0; i < fiPrXs.length; i++) {
            ret.add(isProjection(fiPrXs[i]));

            BoolExpr[] ors = new BoolExpr[input.nrow()];
            for (int j = 0; j < input.nrow(); j++) {
                ors[j] = ctx.mkEq(fiVaXs[i], ts.select(inputExpr, fiPrXs[i], j));
            }
            ret.add(ctx.mkOr(ors));
        }
        return ret;
    }

    private BoolExpr filterClause(SeqExpr col, Expr op, DatatypeExpr val, int row) {
        return (BoolExpr)ctx.mkITE(
                ctx.mkEq(op, ts.opSort().getConst(0)),  //eq
                ctx.mkEq(ts.select(inputExpr, col, row), val),
                ctx.mkITE(
                        ctx.mkEq(op, ts.opSort().getConst(1)),  //neq
                        ctx.mkNot(ctx.mkEq(ts.select(inputExpr, col, row), val)),
                        ctx.mkITE(
                                ctx.mkAnd(
                                        ctx.mkEq(op, ts.opSort().getConst(2)),  //gt
                                        ts.isRealVal(val)),
                                ctx.mkGt(ts.getRealVal(ts.select(inputExpr, col, row)), ts.getRealVal(val)),
                                ctx.mkITE(
                                        ctx.mkAnd(
                                            ctx.mkEq(op, ts.opSort().getConst(3)),  //lt
                                            ts.isRealVal(val)),
                                        ctx.mkLt(ts.getRealVal(ts.select(inputExpr, col, row)), ts.getRealVal(val)),
                                        ctx.mkFalse()
                                )
                        )
                )
        );
    }


    private BoolExpr filter(int row){
        List<BoolExpr> clauses = new ArrayList<>();
        for (int i = 0; i < nfilter; i++) {
            BoolExpr b = filterClause(fiPrXs[i], fiOpXs[i], fiVaXs[i], row);
            clauses.add(b);
        }
        BoolExpr ret = clauses.get(0);
        for (int i = 0; i < fiLopXs.length; i++) {
            ret = (BoolExpr)ctx.mkITE(
                    ctx.mkEq(fiLopXs[i], ts.lopSort().getConst(0)),  //and
                    ctx.mkAnd(ret, clauses.get(i+1)),
                    ctx.mkOr(ret, clauses.get(i+1))
            );
        }
        return ret;
        
    }
    
    public BoolExpr filterWithMask(int row) {
        return (BoolExpr) ctx.mkITE(
                ctx.mkEq(fiMaX, ctx.mkTrue()),
                ctx.mkTrue(),
                filter(row)
        );
    }
    
    public List<BoolExpr> encodeRelation() {
        List<BoolExpr> ret = new ArrayList<>();
//        ret.add(filterWithMask(0));
//        ret.add(ctx.mkNot(filterWithMask(1)));

        IntExpr[] outrs = {ctx.mkIntConst("r")};
        IntExpr outr = outrs[0];
        for (int row = 0; row < input.nrow(); row++) {
            BoolExpr[] ors = new BoolExpr[projXs.length + 2];
            for (int i = 0; i < projXs.length; i++) {
                ors[i] = ctx.mkEq(ts.select(inputExpr, projXs[i], row), ts.select(outputExpr, output.header(i), outr));
            }
            ors[projXs.length] = ctx.mkGe(outr, ctx.mkInt(0));
            ors[projXs.length+1] = ctx.mkLt(outr, ctx.mkInt(output.nrow()));
            BoolExpr rino = ctx.mkAnd(ors);
            BoolExpr erio = ctx.mkExists(outrs, rino, 1, null, null, null, null);
            BoolExpr b = (BoolExpr) ctx.mkITE(
                    filterWithMask(row),
                    erio,
                    ctx.mkNot(erio));
            ret.add(b);
        }

        IntExpr[] inrs = {ctx.mkIntConst("r")};
        IntExpr inr = inrs[0];
        for (int row = 0; row < output.nrow(); row++) {
            BoolExpr[] ors = new BoolExpr[output.ncol() + 2];
            for (int i = 0; i < output.ncol(); i++) {
                ors[i] = ctx.mkEq(ts.select(inputExpr, projXs[i], inr), ts.select(outputExpr, output.header(i), row));
            }
            ors[output.ncol()] = ctx.mkGe(inr, ctx.mkInt(0));
            ors[output.ncol() + 1] = ctx.mkLt(inr, ctx.mkInt(input.nrow()));
            BoolExpr rini = ctx.mkAnd(ors);
            BoolExpr erio = ctx.mkExists(inrs, rini, 1, null, null, null, null);
            ret.add(erio);
        }
        return ret;
        
    }

    public static void main(String[] args) {
        AbstractSolver solver = new SelectWhere();
        solver.solve();

        Olog.log.debug(Helper.printAllFields(solver));

        for (int i = 0; i < ((SelectWhere) solver).projXs.length; i++) {
            Olog.log.debug(solver.solver.getModel().evaluate(((SelectWhere) solver).projXs[i], false).toString());
        }
        Olog.log.debug(solver.solver.getModel().evaluate(((SelectWhere) solver).fiMaX, false).toString());
        for (int i = 0; i < ((SelectWhere) solver).fiPrXs.length; i++) {
            Olog.log.debug(solver.solver.getModel().evaluate(((SelectWhere) solver).fiPrXs[i], false).toString());
            Olog.log.debug(solver.solver.getModel().evaluate(((SelectWhere) solver).fiOpXs[i], false).toString());
            Olog.log.debug(solver.solver.getModel().evaluate(((SelectWhere) solver).fiVaXs[i], false).toString());
        }
    }

}
