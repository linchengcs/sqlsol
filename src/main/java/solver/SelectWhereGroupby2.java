package solver;

import com.microsoft.z3.*;
import table.lang.Table;
import util.Olog;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SelectWhereGroupby2 extends AbstractSolver {

    SeqExpr[] projXs;
    SeqExpr[] fiPrXs;
    Expr[] fiOpXs;
    DatatypeExpr[] fiVaXs;
    Expr[] fiLopXs;
    BoolExpr fiMaX;

    //group by
    SeqExpr grByX;   //group by column, support one column
    SeqExpr haPrX;
    Expr haOpX;      // aggregation function in having
    DatatypeExpr haVaX;
    Expr haCoX;  // comparator operator in having
    Expr haMaX;
    // todo: add support for no group by


    //tmp variables
    ArrayExpr cntAggrCol;
    ArrayExpr sumAggrCol;
    ArrayExpr avgAggrCol;
    ArrayExpr finalAggrCol;

    public void popInOutTables() {
        input = new Table(
                "input",
                Arrays.asList("id1", "name1", "course1", "score1"),
                Arrays.asList(
                        Arrays.asList("1", "john", "eng", "10"),
                        Arrays.asList("2", "bob", "eng", "9"),
                        Arrays.asList("3", "john", "math", "8"),
                        Arrays.asList("4", "bob", "math", "7"),
                        Arrays.asList("5", "bob", "comp", "6"),
                        Arrays.asList("6", "bob", "cal", "11")
                ));
        output = new Table(
                "output",
                Arrays.asList("name2", "sum2"),
                Arrays.asList(
                        Arrays.asList("bob", "27")
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

        grByX = ts.mkStringExpr("grByX");
        haPrX = ts.mkStringExpr("haPrX");
        haOpX = ctx.mkConst("haOpX", ts.aggrSort());
        haVaX = ts.mkValExpr("haVaX");
        haCoX = ctx.mkConst("haCoX", ts.opSort());
        haMaX = (BoolExpr)ctx.mkConst("haMaX", ctx.getBoolSort());
    }

    /**
     * Compute the aggregation columns, use a Int->Val array, which is the same as one input column
     * use parameters: filters, (then) group by col, having clause,
     * that is, grByX, haPrX, haOpX, haVaX
     * returns a list of bool expressions which will be sent to the solver later
     */
    public List<BoolExpr> addAggrCol() {
        cntAggrCol = ctx.mkArrayConst("cntCol", ctx.getIntSort(), ts.valSort());
        sumAggrCol = ctx.mkArrayConst("sumCol", ctx.getIntSort(), ts.valSort());
        avgAggrCol = ctx.mkArrayConst("avgCol", ctx.getIntSort(), ts.valSort());
        finalAggrCol = ctx.mkArrayConst("final", ctx.getIntSort(), ts.valSort());
        List<BoolExpr> ret = new ArrayList<>();

        //cntAggrCol
        for (int i = 0; i < input.nrow(); i++) {
            RealExpr[] t = new RealExpr[input.nrow()];
            for(int j = 0; j < input.nrow(); j++) {
                t[j] = (RealExpr) ctx.mkITE(
                        ctx.mkAnd(
                                ctx.mkEq(ts.select(inputExpr, grByX, j), ts.select(inputExpr, grByX, i)),
                                filterWithMask(j)),
                        ctx.mkReal(1),
                        ctx.mkReal(0));
            }
            BoolExpr b = ctx.mkEq(ctx.mkSelect(cntAggrCol, ctx.mkInt(i)), ts.mkRealVal(ctx.mkAdd(t)));
            ret.add(b);
            // todo: add filter conditions
            // todo: add out of bound
        }

        //sum
        for (int i = 0; i < input.nrow(); i++) {
            RealExpr[] t = new RealExpr[input.nrow()];
            for (int j = 0; j < input.nrow(); j++) {
                t[j] = (RealExpr) ctx.mkITE(
                        ctx.mkAnd(
                                ctx.mkEq(ts.select(inputExpr, grByX, j), ts.select(inputExpr, grByX, i)),
                                filterWithMask(j),
                                ctx.mkTrue()),
                        ts.getRealVal(ts.select(inputExpr, haPrX, j)),
                        ctx.mkReal(0));
            }
            BoolExpr b = ctx.mkEq(
                    ctx.mkSelect(sumAggrCol, ctx.mkInt(i)),
                    ctx.mkITE(
                            ts.isStrVal(ts.select(inputExpr, haPrX, i)),
                            ts.mkNilVal(),
                            ts.mkRealVal(ctx.mkAdd(t))
                    ));
            ret.add(b);
        }

        //average
        for (int i = 0; i < input.nrow(); i++) {
            BoolExpr b = ctx.mkEq(
                    ctx.mkSelect(avgAggrCol, ctx.mkInt(i)),
                    ctx.mkITE(
                            ctx.mkAnd(
                                    ts.isRealVal(ts.select(inputExpr, haPrX, i)),
                                    ctx.mkGt(
                                            ts.getRealVal((DatatypeExpr)ctx.mkSelect(cntAggrCol, ctx.mkInt(i))),
                                            ctx.mkReal(0))),
                            ts.mkRealVal(ctx.mkDiv(
                                    ts.getRealVal((DatatypeExpr) ctx.mkSelect(sumAggrCol, ctx.mkInt(i))),
                                    ts.getRealVal((DatatypeExpr) ctx.mkSelect(cntAggrCol, ctx.mkInt(i))))),
                            ts.mkNilVal()));
            ret.add(b);
        }

        BoolExpr b =  ctx.mkEq(
                finalAggrCol,
                (ArrayExpr)ctx.mkITE(
                        ctx.mkEq(haOpX, ts.aggrSort().getConst(0)),
                        cntAggrCol,
                        ctx.mkITE(
                                ctx.mkEq(haOpX, ts.aggrSort().getConst(1)),
                                sumAggrCol,
                                ctx.mkITE(
                                        ctx.mkEq(haOpX, ts.aggrSort().getConst(2)),
                                        avgAggrCol,
                                        avgAggrCol))));
        ret.add(b);

        // add a new column to inpurExpr
//        ret.add(ctx.mkEq(ctx.mkSelect(inputExpr, ctx.mkString("aggr_col")), finalAggrCol));
        for (int i = 0; i < input.nrow(); i++) {
            ret.add(ctx.mkEq(
                    ts.select(inputExpr, "aggr_col", i),
                    ctx.mkSelect(finalAggrCol, ctx.mkInt(i))
            ));
        }

        return ret;
    }


    /**
     * projection variables should be in `origin headers` or `aggregation headers`
     * filter projection should be in `original headers`
     * group by should be in `original headers`
     * having variables should be in `aggregation headers`
     * @return
     */
    public List<BoolExpr> encodeDomain(){
        // projection variables
        List<BoolExpr> ret  = new ArrayList<>();
        for (int i = 0; i < projXs.length; i++) {
            BoolExpr[] t = new BoolExpr[input.ncol()+1];
            for (int j = 0; j < input.ncol(); j++){
                t[j] = ctx.mkEq(projXs[i], ctx.mkString(input.header(j)));
            }
            t[t.length-1] = ctx.mkEq(projXs[i], ctx.mkString("aggr_col"));
            ret.add(ctx.mkOr(t));
        }

        //filter variables
        for (int i = 0; i < fiPrXs.length; i++) {
            ret.add(isProjection(fiPrXs[i]));

            BoolExpr[] ors = new BoolExpr[input.nrow()];
            for (int j = 0; j < input.nrow(); j++) {
                ors[j] = ctx.mkEq(fiVaXs[i], ts.select(inputExpr, fiPrXs[i], j));
            }
            ret.add(ctx.mkOr(ors));
        }

        // group by variables
        ret.add(isProjection(grByX));

        // having variables
        ret.add(isProjection(haPrX));
//        ret.add(ctx.mkEq(haPrX, ctx.mkString("final")));
        BoolExpr[] ors = new BoolExpr[input.nrow()];
        for (int j = 0; j < ors.length; j++) {
            ors[j] = ctx.mkEq(haVaX, ctx.mkSelect(finalAggrCol, ctx.mkInt(j)));
        }
        ret.add(ctx.mkOr(ors));

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


    protected BoolExpr filter(int row){
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

    protected BoolExpr filterWithMask(int row) {
        return (BoolExpr) ctx.mkITE(
                ctx.mkEq(fiMaX, ctx.mkTrue()),
                ctx.mkTrue(),
                filter(row)
        );
    }

    public BoolExpr having(int row) {
//        return filterClause(ctx.mkString("aggr_col"),  haCoX, haVaX, row);
        return (BoolExpr) ctx.mkITE(
                ctx.mkEq(haMaX, ctx.mkTrue()),
                ctx.mkTrue(),
                filterClause(ctx.mkString("aggr_col"),  haCoX, haVaX, row));
    }

    public List<BoolExpr> encodeRelation() {
        List<BoolExpr> ret = new ArrayList<>();

        // =>
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
                    ctx.mkAnd(having(row), filterWithMask(row)),
//                    having(row),
                    erio,
                    ctx.mkNot(erio));
            ret.add(b);
        }

        // <=
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


    public Status solve() {
        popInOutTables();
        addUnknowns();

        List<BoolExpr> inputCons = encodeTable(inputExpr, input);
        List<BoolExpr> outputCons = encodeTable(outputExpr, output);
        addConstrainsToSolver(inputCons);
        addConstrainsToSolver(outputCons);

        List<BoolExpr> aggrColCons = addAggrCol();
        addConstrainsToSolver(aggrColCons);

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
            Olog.log.debug("solver: \n" + solver);
            Olog.log.debug("model: \n" + model.toString());
        } else {
            Olog.log.debug("solver: \n" + solver);
            Olog.log.info("unsat");
            for (Expr c : solver.getUnsatCore())
            {
                Olog.log.debug(c.toString());
            }
        }


        Olog.log.info("Z3 time usage: " + (etime-stime) + " miniseconds");
        return status;
    }

    public String sql() {
        String ret = "SELECT ";
        for (int i = 0; i < projXs.length; i++) {
            String t = eval(projXs[i]);
            if (t.equals("aggr_col")) {
                ret += eval(haOpX) + "(" + eval(haPrX) + ")" + ", ";
            } else {
                ret += t + ", ";
            }
        }
        ret = ret.substring(0, ret.length()-1);
        ret += "\n";
        ret += "FROM input";
        if (! eval(fiMaX).equals("true")) {
            ret += "\nWHERE ";
            for (int i = 0; i < fiLopXs.length; i++) {
                ret += eval(fiPrXs[i]) + " " + eval(fiOpXs[i]) + " " + eval((fiVaXs[i])) + " " + eval(fiLopXs[i]) + " ";
            }
            ret += eval(fiPrXs[fiLopXs.length]) + " " + eval(fiOpXs[fiLopXs.length]) + " " + eval((fiVaXs[fiLopXs.length]));
        }
        ret += "\nGROUP BY ";
        ret += eval(grByX);
        if (! eval(haMaX).equals("true")) {
            ret += "\nHAVING ";
            ret += eval(haOpX) + "(" + eval(haPrX) + ") " + eval(haCoX) + " " + eval(ts.getRealVal(haVaX));
        }
        return ret;
    }

    public static void main(String[] args) {
        SelectWhereGroupby2 solver = new SelectWhereGroupby2();
        solver.solve();

        for (int i = 0; i < ((SelectWhereGroupby2) solver).projXs.length; i++) {
            Olog.log.debug(solver.solver.getModel().evaluate(((SelectWhereGroupby2) solver).projXs[i], false).toString() + ", projX" + i);
        }
        Olog.log.debug(solver.solver.getModel().evaluate(((SelectWhereGroupby2) solver).fiMaX, false).toString());

        for (int i = 0; i < ((SelectWhereGroupby2) solver).fiLopXs.length; i++) {
            Olog.log.debug(solver.solver.getModel().evaluate(((SelectWhereGroupby2) solver).fiLopXs[i], false).toString());
        }
        for (int i = 0; i < ((SelectWhereGroupby2) solver).fiPrXs.length; i++) {
            Olog.log.debug(solver.solver.getModel().evaluate(((SelectWhereGroupby2) solver).fiPrXs[i], false).toString());
            Olog.log.debug(solver.solver.getModel().evaluate(((SelectWhereGroupby2) solver).fiOpXs[i], false).toString());
            Olog.log.debug(solver.solver.getModel().evaluate(((SelectWhereGroupby2) solver).fiVaXs[i], false).toString());
        }
        Olog.log.debug(solver.solver.getModel().evaluate(((SelectWhereGroupby2) solver).grByX, false).toString() + ", grByX");
        Olog.log.debug(solver.solver.getModel().evaluate(((SelectWhereGroupby2) solver).haPrX, false).toString() + ", haPrX");
        Olog.log.debug(solver.solver.getModel().evaluate(((SelectWhereGroupby2) solver).haOpX, false).toString() + ", haOpX");
        Olog.log.debug(solver.solver.getModel().evaluate(((SelectWhereGroupby2) solver).haVaX, false).toString() + ", haVaX");
        Olog.log.debug(solver.solver.getModel().evaluate(((SelectWhereGroupby2) solver).haCoX, false).toString() + ", haCoX");

//        for (int i = 0; i < solver.input.nrow(); i++) {
//            Olog.log.debug(solver.solver.getModel().evaluate(solver.ts.select(solver.inputExpr, solver.projXs[1], solver.ctx.mkInt(i)), false).toString());
//        }
        Olog.log.debug("cntAggrCol");
        for (int i = 0; i < solver.input.nrow(); i++) {
            Olog.log.debug(solver.solver.getModel().evaluate(solver.ctx.mkSelect(solver.cntAggrCol, solver.ctx.mkInt(i)), false).toString());
        }
        Olog.log.debug("sumAggrCol");
        for (int i = 0; i < solver.input.nrow(); i++) {
            Olog.log.debug(solver.solver.getModel().evaluate(solver.ctx.mkSelect(solver.sumAggrCol, solver.ctx.mkInt(i)), false).toString());
        }
        Olog.log.debug("avgAggrCol");
        for (int i = 0; i < solver.input.nrow(); i++) {
            Olog.log.debug(solver.solver.getModel().evaluate(solver.ctx.mkSelect(solver.avgAggrCol, solver.ctx.mkInt(i)), false).toString());
        }

        Olog.log.debug(solver.input.toString());
        Olog.log.debug(solver.output.toString());

        Olog.log.debug("\n" + solver.sql());
    }

}
