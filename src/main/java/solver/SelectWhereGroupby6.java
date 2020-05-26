package solver;

import ch.qos.logback.classic.Level;
import com.microsoft.z3.*;
import parser.ExampleDS;
import table.lang.Table;
import table.lang.datatype.ValType;
import table.lang.datatype.Value;
import util.Olog;

import java.util.ArrayList;
import java.util.List;


/**
 * combine 2 3 OptionalGroupby into one file
 */
public class SelectWhereGroupby6 extends AbstractSolver {

    SeqExpr[] projXs;
    SeqExpr[] fiPrXs;
    Expr[] fiOpXs;
    DatatypeExpr[] fiVaXs;
    Expr[] fiLopXs;
    BoolExpr[] fiMaXs;
    private int nfilter ;

    //group by
    SeqExpr grByX;   //group by column, support one column
    SeqExpr haPrX;
    Expr haOpX;      // aggregation function in having
    DatatypeExpr haVaX;
    Expr haCoX;  // comparator operator in having
    BoolExpr haMaX;
    // todo: add support for no group by

    SeqExpr aggrCol;  // header for assistant aggregation column
    SeqExpr autoIDCol;  // header for assistant auto id column

    String benchmarkPath;

//    Table input, output;

    public SelectWhereGroupby6(String benchmarkPath) {
        super();
        this.benchmarkPath = benchmarkPath;
    }

    public SelectWhereGroupby6(String benchmarkPath, int nfilter) {
        super();
        this.benchmarkPath = benchmarkPath;
        this.nfilter = nfilter;
    }

    public void popInOutTables() {
        ExampleDS ds = ExampleDS.readFromFile(benchmarkPath);
        input = ds.inputs.get(0);
        output = ds.output;
    }

    @Override
    public void addUnknowns() {
        // projection unknowns
        projXs = new SeqExpr[output.ncol()];
        for (int col = 0; col < output.ncol(); col++)
            projXs[col] = ts.mkStringExpr("projX" + col);
        // filter unknowns
        fiMaXs = new BoolExpr[nfilter];
        fiPrXs = new SeqExpr[nfilter];
        fiOpXs = new Expr[nfilter];
        fiVaXs = new DatatypeExpr[nfilter];
        for (int i = 0; i < nfilter; i++) {
            fiMaXs[i] = ctx.mkBoolConst("fiMaX" + i);
            fiPrXs[i] = ts.mkStringExpr("fiPrX" + i);
            fiOpXs[i] = ctx.mkConst("fiOpX" + i, ts.opSort());
            fiVaXs[i] = ts.mkValExpr("fiVaX" + i);
        }
        fiLopXs = new Expr[nfilter-1];
        for (int i = 0; i < fiLopXs.length; i++) {
            fiLopXs[i] = ctx.mkConst("fiLopX" + i, ts.lopSort());
        }

        grByX = ts.mkStringExpr("grByX");
        haPrX = ts.mkStringExpr("haPrX");
        haOpX = ctx.mkConst("haOpX", ts.aggrSort());
        haVaX = ts.mkValExpr("haVaX");
        haCoX = ctx.mkConst("haCoX", ts.opSort());
        haMaX = ctx.mkBoolConst("haMaX");

        aggrCol = ctx.mkString("aggr_col");
        autoIDCol = ctx.mkString("_auto_id_");
    }

    /**
     * Compute the aggregation columns, use a Int->Val array, which is the same as one input column, added to table inputExpr
     * use parameters: filters, (then) group by col, having clause,
     * that is, grByX, haPrX, haOpX, haVaX
     * returns a list of bool expressions which will be sent to the solver later
     */
    public List<BoolExpr> addAggrCol() {
        ArrayExpr cntAggrCol = ctx.mkArrayConst("cntCol", ctx.getIntSort(), ts.valSort());
        ArrayExpr sumAggrCol = ctx.mkArrayConst("sumCol", ctx.getIntSort(), ts.valSort());
        ArrayExpr avgAggrCol = ctx.mkArrayConst("avgCol", ctx.getIntSort(), ts.valSort());
        ArrayExpr maxAggrCol = ctx.mkArrayConst("maxCol", ctx.getIntSort(), ts.valSort());
        ArrayExpr minAggrCol = ctx.mkArrayConst("minCol", ctx.getIntSort(), ts.valSort());
        ArrayExpr finalAggrCol = ctx.mkArrayConst("final", ctx.getIntSort(), ts.valSort());
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

        //max
        for (int i = 0; i < input.nrow(); i++) {
            RealExpr t = ts.getRealVal(ts.select(inputExpr, haPrX, i));
            for (int j = 0; j < input.nrow(); j++) {
                t = (RealExpr) ctx.mkITE(
                        ctx.mkAnd(ctx.mkEq(
                                ts.select(inputExpr, grByX, j), ts.select(inputExpr, grByX, i)),
                                filterWithMask(j),
                                ctx.mkGt(ts.getRealVal(ts.select(inputExpr, haPrX, j)), ts.getRealVal(ts.select(inputExpr, haPrX, i)))
                        ),
                        ts.getRealVal(ts.select(inputExpr, haPrX, j)),
                        t);
            }
            BoolExpr b = ctx.mkEq(
                    ctx.mkSelect(maxAggrCol, ctx.mkInt(i)),
                    ctx.mkITE(
                            ts.isStrVal(ts.select(inputExpr, haPrX, i)),
                            ts.mkNilVal(),
                            ts.mkRealVal(t)
                    ));
            ret.add(b);
        }

        //min
        for (int i = 0; i < input.nrow(); i++) {
            RealExpr t = ts.getRealVal(ts.select(inputExpr, haPrX, i));
            for (int j = 0; j < input.nrow(); j++) {
                t = (RealExpr) ctx.mkITE(
                        ctx.mkAnd(ctx.mkEq(
                                ts.select(inputExpr, grByX, j), ts.select(inputExpr, grByX, i)),
                                filterWithMask(j),
                                ctx.mkLt(ts.getRealVal(ts.select(inputExpr, haPrX, j)), ts.getRealVal(ts.select(inputExpr, haPrX, i)))
                        ),
                        ts.getRealVal(ts.select(inputExpr, haPrX, j)),
                        t);
            }
            BoolExpr b = ctx.mkEq(
                    ctx.mkSelect(minAggrCol, ctx.mkInt(i)),
                    ctx.mkITE(
                            ts.isStrVal(ts.select(inputExpr, haPrX, i)),
                            ts.mkNilVal(),
                            ts.mkRealVal(t)
                    ));
            ret.add(b);
        }

        BoolExpr b =  ctx.mkEq(
                finalAggrCol,
                (ArrayExpr)ctx.mkITE(
                        ctx.mkEq(haOpX, ts.aggrSort().getConst(0)),  // count
                        cntAggrCol,
                        ctx.mkITE(
                                ctx.mkEq(haOpX, ts.aggrSort().getConst(1)),   // sum
                                sumAggrCol,
                                ctx.mkITE(
                                        ctx.mkEq(haOpX, ts.aggrSort().getConst(2)),  // average
                                        avgAggrCol,
                                        ctx.mkITE(
                                                ctx.mkEq(haOpX, ts.aggrSort().getConst(3)),  // max
                                                maxAggrCol,
                                                ctx.mkITE(
                                                        ctx.mkEq(haOpX, ts.aggrSort().getConst(4)),  // min
                                                        minAggrCol,
                                                        cntAggrCol
                                                ))))));
        ret.add(b);

        // add a new column to inpurExpr
        //        ret.add(ctx.mkEq(ctx.mkSelect(inputExpr, ctx.mkString("aggr_col")), finalAggrCol));
        for (int i = 0; i < input.nrow(); i++) {
            ret.add(ctx.mkEq(
                    ts.select(inputExpr, aggrCol, i),
                    ctx.mkSelect(finalAggrCol, ctx.mkInt(i))
            ));
        }

        return ret;
    }

    private BoolExpr filterClause(SeqExpr col, Expr op, DatatypeExpr val, IntExpr row) {
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
                                        ctx.mkITE(
                                                ctx.mkAnd(
                                                        ctx.mkEq(op, ts.opSort().getConst(4)),  //ge
                                                        ts.isRealVal(val)),
                                                ctx.mkGe(ts.getRealVal(ts.select(inputExpr, col, row)), ts.getRealVal(val)),
                                                ctx.mkITE(
                                                        ctx.mkAnd(
                                                                ctx.mkEq(op, ts.opSort().getConst(5)),  //le
                                                                ts.isRealVal(val)),
                                                        ctx.mkLe(ts.getRealVal(ts.select(inputExpr, col, row)), ts.getRealVal(val)),
                                                        ctx.mkFalse()
                                                )
                                        )
                                )
                        )
                )
        );
    }

    private BoolExpr filterClauseWithMask(BoolExpr mask, BoolExpr clause) {
        return (BoolExpr) ctx.mkITE(
                mask,
                ctx.mkTrue(),
                clause
        );
    }

    protected BoolExpr filterWithMask(int row) {
        return filterWithMask(ctx.mkInt(row));
    }

    protected BoolExpr filterWithMask(IntExpr row) {
        List<BoolExpr> clausesWithMask = new ArrayList<>();
        for (int i = 0; i < nfilter; i++) {
            BoolExpr b = filterClause(fiPrXs[i], fiOpXs[i], fiVaXs[i], row);
            BoolExpr bm = filterClauseWithMask(fiMaXs[i], b);
            clausesWithMask.add(bm);
        }
        BoolExpr ret = clausesWithMask.get(0);
        for (int i = 0; i < fiLopXs.length; i++) {
            ret = (BoolExpr)ctx.mkITE(
                    ctx.mkEq(fiLopXs[i], ts.lopSort().getConst(0)),  //and
                    ctx.mkAnd(ret, clausesWithMask.get(i+1)),
                    ctx.mkOr(ret, clausesWithMask.get(i+1))
            );
        }
        return ret;
    }

    public BoolExpr having (int row) {
        return having(ctx.mkInt(row));
    }

    public BoolExpr having(IntExpr row) {
        //        return filterClause(ctx.mkString("aggr_col"),  haCoX, haVaX, row);
        return (BoolExpr) ctx.mkITE(
                haMaX,
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
                    //                    ctx.mkNot(erio));
                    ctx.mkTrue());
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

            rini = ctx.mkAnd(
                    filterWithMask(inr),
                    having(inr),
                    rini
            );


            BoolExpr erio = ctx.mkExists(inrs, rini, 1, null, null, null, null);
            ret.add(erio);
        }
        return ret;

    }


    public List<BoolExpr> encodeTableAddID(ArrayExpr e, Table t) {
        List<BoolExpr> ret = new ArrayList<>();
        for (int col = 0; col < t.ncol(); col++){
            for (int row = 0; row < t.nrow(); row++) {
                Value value = t.get(col, row);
                String header = t.header(col);
                DatatypeExpr ve = null;
                if (value.getValType() == ValType.NumberVal){
                    ve = ts.mkRealVal(value.toString());
                } else if (value.getValType() == ValType.StringVal) {
                    ve = ts.mkStrVal(value.toString());
                } else {
                    Olog.log.error("Unsupported example table cell type");
                }

                ret.add(
                        ctx.mkEq(ts.select(e, header, row), ve)
                );
            }
        }

        //add ID
        for (int row = 0; row < t.nrow(); row++){
            String header = "_auto_id_";
            String id = header + row;
            ret.add(ctx.mkEq(ts.select(e, header, row), ts.mkStrVal(id)));
        }
        return ret;
    }

    public List<BoolExpr> encodeDomain(){
        // projection variables
        List<BoolExpr> ret  = new ArrayList<>();
        for (int i = 0; i < projXs.length; i++) {
            BoolExpr[] t = new BoolExpr[input.ncol()+1];
            for (int j = 0; j < input.ncol(); j++){
                t[j] = ctx.mkEq(projXs[i], ctx.mkString(input.header(j)));
            }
            t[t.length-1] = ctx.mkEq(projXs[i], aggrCol);
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
        ret.add(ctx.mkOr(isProjection(grByX),
                ctx.mkEq(grByX, autoIDCol)));

        // having variables
        ret.add(isProjection(haPrX));
        //        ret.add(ctx.mkEq(haPrX, ctx.mkString("final")));
        BoolExpr[] ors = new BoolExpr[input.nrow()];
        for (int j = 0; j < ors.length; j++) {
            //            ors[j] = ctx.mkEq(haVaX, ctx.mkSelect(finalAggrCol, ctx.mkInt(j)));
            ors[j] = ctx.mkEq(haVaX, ts.select(inputExpr, aggrCol, ctx.mkInt(j)));
        }
        ret.add(ctx.mkOr(ors));

        return ret;
    }

    public Status solve() {
        popInOutTables();
        addUnknowns();

        List<BoolExpr> inputCons = encodeTableAddID(inputExpr, input);
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
            this.status = 1;
            this.time_usage = (int)(etime- stime);
            Olog.log.debug(input.toString());
            Olog.log.debug(output.toString());
            Olog.log.debug("solver: \n" + solver);
            Olog.log.debug("model: \n" + model.toString());
        } else {
            this.status = 0;
            this.time_usage = (int)(etime- stime);
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

        boolean flag = false;
        String wherePart = "";
        for (int i = 0; i < nfilter - 1; i++) {
            if (!eval(fiMaXs[i]).equals("false")) {
                if (eval(fiLopXs[i]).equals("or")) {  // "true or", no where clause for sql
                    flag = true;
                    break;
                } else {  // "true and" drop
                    continue;
                }
            } else {
                wherePart += eval(fiPrXs[i]) + " " + eval(fiOpXs[i]) + " " + eval((fiVaXs[i])) + " " + eval(fiLopXs[i]) + " ";
            }
        }
        if (!flag) {
            if (eval(fiMaXs[nfilter - 1]).equals("false")) {
                wherePart += eval(fiPrXs[fiLopXs.length]) + " " + eval(fiOpXs[fiLopXs.length]) + " " + eval((fiVaXs[fiLopXs.length]));
            } else {
                flag = true;
            }
        }
        wherePart = flag ? "" : "\nWHERE " + wherePart;
        ret += wherePart;

        ret += "\nGROUP BY ";
        ret += eval(grByX);
        if (eval(haMaX).equals("false")) {
            ret += "\nHAVING ";
            ret += eval(haOpX) + "(" + eval(haPrX) + ") " + eval(haCoX) + " " + eval(ts.getRealVal(haVaX));
        }

        return ret;
    }

    public static void main(String[] args) {
        Olog.log.setLevel(Level.DEBUG);

        String filename = "data/all/joined/dev_002M";
//                String filename = "data/sqlsynthesizer/joined/textbook_5_1_6";
        //        String filename = "data/sqlsynthesizer/joined/textbook_5_2_9";
        SelectWhereGroupby6 solver = new SelectWhereGroupby6(filename, 2);
        solver.solve();

        for (int i = 0; i < ((SelectWhereGroupby6) solver).projXs.length; i++) {
            Olog.log.debug(solver.solver.getModel().evaluate(((SelectWhereGroupby6) solver).projXs[i], false).toString() + ", projX" + i);
        }

        Olog.log.debug("model for logic connectors: ");
        for (int i = 0; i < ((SelectWhereGroupby6) solver).fiLopXs.length; i++) {
            Olog.log.debug(solver.solver.getModel().evaluate(((SelectWhereGroupby6) solver).fiLopXs[i], false).toString());
        }

        Olog.log.debug("model for filters: ");
        for (int i = 0; i < ((SelectWhereGroupby6) solver).fiPrXs.length; i++) {
            Olog.log.debug(solver.solver.getModel().evaluate(((SelectWhereGroupby6) solver).fiMaXs[i], false).toString());
            Olog.log.debug(solver.solver.getModel().evaluate(((SelectWhereGroupby6) solver).fiPrXs[i], false).toString());
            Olog.log.debug(solver.solver.getModel().evaluate(((SelectWhereGroupby6) solver).fiOpXs[i], false).toString());
            Olog.log.debug(solver.solver.getModel().evaluate(((SelectWhereGroupby6) solver).fiVaXs[i], false).toString());
        }
        Olog.log.debug(solver.solver.getModel().evaluate(((SelectWhereGroupby6) solver).grByX, false).toString() + ", grByX");
        Olog.log.debug(solver.solver.getModel().evaluate(((SelectWhereGroupby6) solver).haMaX, false).toString() + ", haMax");
        Olog.log.debug(solver.solver.getModel().evaluate(((SelectWhereGroupby6) solver).haPrX, false).toString() + ", haPrX");
        Olog.log.debug(solver.solver.getModel().evaluate(((SelectWhereGroupby6) solver).haOpX, false).toString() + ", haOpX");
        Olog.log.debug(solver.solver.getModel().evaluate(((SelectWhereGroupby6) solver).haVaX, false).toString() + ", haVaX");
        Olog.log.debug(solver.solver.getModel().evaluate(((SelectWhereGroupby6) solver).haCoX, false).toString() + ", haCoX");

        //        for (int i = 0; i < solver.input.nrow(); i++) {
        //            Olog.log.debug(solver.solver.getModel().evaluate(solver.ts.select(solver.inputExpr, solver.projXs[1], solver.ctx.mkInt(i)), false).toString());
        //        }


        Olog.log.info(solver.input.toString());
        Olog.log.info(solver.output.toString());

        Olog.log.info("\n" + solver.sql());
    }

}
