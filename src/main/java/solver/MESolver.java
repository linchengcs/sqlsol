package solver;

import ch.qos.logback.classic.Level;
import com.microsoft.z3.*;
import parser.ExampleDS;
import table.lang.Table;
import table.lang.datatype.ValType;
import table.lang.datatype.Value;
import theory.TableSorts;
import util.Helper;
import util.Olog;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class MESolver {


    Context ctx;
    public Solver solver;
    TableSorts ts;

    public List<Table> inputs, outputs;
    public List<ArrayExpr> inputExprs, outputExprs;


    SeqExpr[] projXs;
    SeqExpr[] fiPrXs;
    Expr[] fiOpXs;
    DatatypeExpr[] fiVaXs;
    Expr[] fiLopXs;
    BoolExpr[] fiMaXs;
    public int nfilter = 1;

    //group by
    SeqExpr grByX;   //group by column, support one column
    SeqExpr haPrX;
    Expr haOpX;      // aggregation function in having
    DatatypeExpr haVaX;
    Expr haCoX;  // comparator operator in having
    BoolExpr haMaX;

    SeqExpr aggrCol;  // header for assistant aggregation column
    SeqExpr autoIDCol;  // header for assistant auto id column

    public int status = -1;  //0 for unsat, 1 for sat
    public int time_usage = 0;
    public int time_check = 0;
    public static int Z3TIMEOUT = 120000;

    public MESolver() {
        HashMap<String, String> cfg = new HashMap<String, String>();
        cfg.put("model", "true");
        this.ctx = new Context(cfg);
        this.solver = ctx.mkSolver();
        this.ts = new TableSorts(ctx);
        this.inputs = new ArrayList<>();
        this.outputs = new ArrayList<>();
        this.inputExprs = new ArrayList<>();
        this.outputExprs = new ArrayList<>();

        if (Z3TIMEOUT > 0) {
            Params p = ctx.mkParams();
            p.add("timeout", Z3TIMEOUT);
            solver.setParameters(p);
        }
    }

    public void setNfilter(int nfilter) {
        this.nfilter = nfilter;
    }

    public void popInOutTables() {
        inputs.add(new Table(
                "input_1",
                Arrays.asList("id1", "name1"),
                Arrays.asList(
                        Arrays.asList("1", "john"),
                        Arrays.asList("2", "bob")
                )));
        outputs.add(new Table(
                "output_1",
                Arrays.asList("id2", "name2"),
                Arrays.asList(
                        Arrays.asList("1", "john")
                )));
//        inputs.add(new Table(
//                "input_2",
//                Arrays.asList("id1", "name1"),
//                Arrays.asList(
//                        Arrays.asList("1", "rose"),
//                        Arrays.asList("2", "mary")
//                )));
//        outputs.add(new Table(
//                "output_2",
//                Arrays.asList("id2", "name2"),
//                Arrays.asList(
//                        Arrays.asList("1", "rose")
//                )));

    }

    public void setInOutTables(List<Table> inputs, List<Table> outputs) {
        this.inputs = inputs;
        this.outputs = outputs;
    }

    public void setInOutTablesFromFile(String file) {
        ExampleDS ds = ExampleDS.readFromFile(file);
        Table input = ds.inputs.get(0);
        Table output = ds.output;
        this.inputs.add(input);
        this.outputs.add(output);
    }

    public void initInOutExprs(){
        assert inputs.size() == outputs.size();
        for (int i = 0; i < inputs.size(); i++) {
            inputExprs.add(ts.mkTblExpr("input_" + i));
            outputExprs.add(ts.mkTblExpr("output_" + i));
        }
    }

    public void addUnknowns() {
        // projection unknowns
        Table output = outputs.get(0);
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

        if (nfilter > 0) {
            fiLopXs = new Expr[nfilter - 1];
        } else {
            fiLopXs = new Expr[0];
        }
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
    public List<BoolExpr> addAggrCol(Table input, ArrayExpr inputExpr) {
        ArrayExpr cntAggrCol = ctx.mkArrayConst("cntCol" + Helper.get_global_id(), ctx.getIntSort(), ts.valSort());
        ArrayExpr sumAggrCol = ctx.mkArrayConst("sumCol" + Helper.get_global_id(), ctx.getIntSort(), ts.valSort());
        ArrayExpr avgAggrCol = ctx.mkArrayConst("avgCol" + Helper.get_global_id(), ctx.getIntSort(), ts.valSort());
        ArrayExpr maxAggrCol = ctx.mkArrayConst("maxCol" + Helper.get_global_id(), ctx.getIntSort(), ts.valSort());
        ArrayExpr minAggrCol = ctx.mkArrayConst("minCol" + Helper.get_global_id(), ctx.getIntSort(), ts.valSort());
        ArrayExpr finalAggrCol = ctx.mkArrayConst("final" + Helper.get_global_id(), ctx.getIntSort(), ts.valSort());
        List<BoolExpr> ret = new ArrayList<>();

        //cntAggrCol
        for (int i = 0; i < input.nrow(); i++) {
            RealExpr[] t = new RealExpr[input.nrow()];
            for(int j = 0; j < input.nrow(); j++) {
                t[j] = (RealExpr) ctx.mkITE(
                        ctx.mkAnd(
                                ctx.mkEq(ts.select(inputExpr, grByX, j), ts.select(inputExpr, grByX, i)),
                                filterWithMask(j, inputExpr)),
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
                                filterWithMask(j, inputExpr),
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
                                filterWithMask(j, inputExpr),
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
                                filterWithMask(j, inputExpr),
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

    private BoolExpr filterClause(SeqExpr col, Expr op, DatatypeExpr val, IntExpr row, ArrayExpr inputExpr) {
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

    protected BoolExpr filterWithMask(int row, ArrayExpr inputExpr) {
        return filterWithMask(ctx.mkInt(row), inputExpr);
    }

    protected BoolExpr filterWithMask(IntExpr row, ArrayExpr inputExpr) {
        if (nfilter == 0) return ctx.mkTrue();
        List<BoolExpr> clausesWithMask = new ArrayList<>();
        for (int i = 0; i < nfilter; i++) {
            BoolExpr b = filterClause(fiPrXs[i], fiOpXs[i], fiVaXs[i], row, inputExpr);
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

    public BoolExpr having (int row, ArrayExpr inputExpr) {
        return having(ctx.mkInt(row), inputExpr);
    }

    public BoolExpr having(IntExpr row, ArrayExpr inputExpr) {
        //        return filterClause(ctx.mkString("aggr_col"),  haCoX, haVaX, row);
        return (BoolExpr) ctx.mkITE(
                haMaX,
                ctx.mkTrue(),
                filterClause(ctx.mkString("aggr_col"),  haCoX, haVaX, row, inputExpr));
    }

    public List<BoolExpr> encodeRelation(Table input, ArrayExpr inputExpr, Table output, ArrayExpr outputExpr) {
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
                    ctx.mkAnd(having(row, inputExpr), filterWithMask(row, inputExpr)),
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
                    filterWithMask(inr, inputExpr),
                    having(inr, inputExpr),
                    rini
            );


            BoolExpr erio = ctx.mkExists(inrs, rini, 1, null, null, null, null);
            ret.add(erio);
        }
        return ret;

    }


    public List<BoolExpr> encodeTableAddID(ArrayExpr e, Table t, String tblName) {
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

        //add ID
        for (int row = 0; row < t.nrow(); row++){
            String header =  "_auto_id_";
            String id = "id" + Helper.get_global_id();
            ret.add(ctx.mkEq(ts.select(e, header, row), ts.mkStrVal(id)));
        }
        return ret;
    }

    public List<BoolExpr> encodeDomain(){
        Table input = inputs.get(0);
        ArrayExpr inputExpr = inputExprs.get(0);  // all consts comes from table 1 ??
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
//                ret.add(ctx.mkEq(haPrX, aggrCol));
        BoolExpr[] ors = new BoolExpr[input.nrow()];
        for (int j = 0; j < ors.length; j++) {
            //            ors[j] = ctx.mkEq(haVaX, ctx.mkSelect(finalAggrCol, ctx.mkInt(j)));
            ors[j] = ctx.mkEq(haVaX, ts.select(inputExpr, aggrCol, ctx.mkInt(j)));
        }
        ret.add(ctx.mkOr(ors));

        return ret;
    }

    
    public Status solve() {
        boolean flag = true;

        long time0 = System.currentTimeMillis();
//        popInOutTables();
        initInOutExprs();
        addUnknowns();

        for (int i = 0; i < inputs.size(); i++) {
            Table input = inputs.get(i);
            Table output = outputs.get(i);
            ArrayExpr inputExpr = inputExprs.get(i);
            ArrayExpr outputExpr = outputExprs.get(i);
            List<BoolExpr> inputCons = encodeTableAddID(inputExpr, input, "tbl_" + i);
            List<BoolExpr> outputCons = encodeTable(outputExpr, output);
            addConstrainsToSolver(inputCons);
            addConstrainsToSolver(outputCons);

            if (flag) {
                flag = false;  // do only once;
            }

            List<BoolExpr> aggrColCons = addAggrCol(input, inputExpr);
            addConstrainsToSolver(aggrColCons);


            List<BoolExpr> relationCons = encodeRelation(input, inputExpr, output, outputExpr);
            addConstrainsToSolver(relationCons);
        }

        List<BoolExpr> domainCons = encodeDomain();
        addConstrainsToSolver(domainCons);

//        Olog.log.debug("solver: \n" + solver);

        long stime = System.currentTimeMillis();
        Status status = solver.check();
        long etime = System.currentTimeMillis();

        if (status == Status.SATISFIABLE) {
            Model model = solver.getModel();
//            Olog.log.debug("model: \n" + model.toString());
            this.status = 1;
        } else {
            Olog.log.info("unsat");
            this.status = 0;
            for (Expr c : solver.getUnsatCore())
            {
//                Olog.log.debug(c.toString());
            }
        }

        this.time_usage = (int)(etime- time0);
        this.time_check = (int)(etime- stime);

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
        if (!flag && nfilter > 0) {
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



    public String eval(Expr expr) {
        String s = solver.getModel().eval(expr, false).toString();
        if (s.startsWith("\""))
            return  s.substring(1, s.length()-1);
        else
            return s;
    }

    protected void addConstrainsToSolver(List<BoolExpr> bs){
        for (BoolExpr b : bs)
            solver.add(b);
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

        return ret;
    }

    protected  BoolExpr isProjection(Expr expr) {
        Table input = inputs.get(0);
        BoolExpr[] ors = new BoolExpr[(input.ncol())];
        for (int col = 0; col < input.ncol(); col++) {
            ors[col] = ctx.mkEq(expr, ctx.mkString(input.header(col)));
        }
        return ctx.mkOr(ors);
    }

    public String getStatistics(String key) {
        return this.solver.getStatistics().get(key).getValueString();
    }

    public static void main(String[] args) {
        Olog.log.setLevel(Level.DEBUG);

//        String filename = "data/sqlsynthesizer/joined/forum-questions-4";
        //        String filename = "data/sqlsynthesizer/joined/textbook_5_1_10";
        //        String filename = "data/sqlsynthesizer/joined/textbook_5_2_5";
        MESolver solver = new MESolver();
        solver.popInOutTables();
        solver.solve();

        for (int i = 0; i < solver.projXs.length; i++) {
            Olog.log.debug(solver.solver.getModel().evaluate(solver.projXs[i], false).toString() + ", projX" + i);
        }

        Olog.log.debug("model for logic connectors: ");
        for (int i = 0; i < solver.fiLopXs.length; i++) {
            Olog.log.debug(solver.solver.getModel().evaluate(solver.fiLopXs[i], false).toString());
        }

        Olog.log.debug("model for filters: ");
        for (int i = 0; i < solver.fiPrXs.length; i++) {
            Olog.log.debug(solver.solver.getModel().evaluate(solver.fiMaXs[i], false).toString());
            Olog.log.debug(solver.solver.getModel().evaluate(solver.fiPrXs[i], false).toString());
            Olog.log.debug(solver.solver.getModel().evaluate(solver.fiOpXs[i], false).toString());
            Olog.log.debug(solver.solver.getModel().evaluate(solver.fiVaXs[i], false).toString());
        }
        Olog.log.debug(solver.solver.getModel().evaluate(solver.grByX, false).toString() + ", grByX");
        Olog.log.debug(solver.solver.getModel().evaluate(solver.haMaX, false).toString() + ", haMax");
        Olog.log.debug(solver.solver.getModel().evaluate(solver.haPrX, false).toString() + ", haPrX");
        Olog.log.debug(solver.solver.getModel().evaluate(solver.haOpX, false).toString() + ", haOpX");
        Olog.log.debug(solver.solver.getModel().evaluate(solver.haVaX, false).toString() + ", haVaX");
        Olog.log.debug(solver.solver.getModel().evaluate(solver.haCoX, false).toString() + ", haCoX");

        //        for (int i = 0; i < solver.input.nrow(); i++) {
        //            Olog.log.debug(solver.solver.getModel().evaluate(solver.ts.select(solver.inputExpr, solver.projXs[1], solver.ctx.mkInt(i)), false).toString());
        //        }



        Olog.log.info("\n" + solver.sql());
    }


}
