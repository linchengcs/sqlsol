package solver;

import ch.qos.logback.classic.Level;
import com.microsoft.z3.*;
import table.lang.Table;
import table.lang.datatype.ValType;
import table.lang.datatype.Value;
import util.Olog;

import java.util.ArrayList;
import java.util.List;

public class SelectWhereOptionalGroupby extends SelectWhereGroupby3 {


    String benchmarkPath;

    public SelectWhereOptionalGroupby(String benchmarkPath) {
        super(benchmarkPath);
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
        ret.add(ctx.mkOr(isProjection(grByX),
                ctx.mkEq(grByX,ctx.mkString("_auto_id_"))));

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

    public static void main(String[] args) {
        Olog.log.setLevel(Level.INFO);

        String filename = "data/sqlsynthesizer/joined/forum-questions-4";
//        String filename = "data/sqlsynthesizer/joined/textbook_5_1_1";
        SelectWhereOptionalGroupby solver = new SelectWhereOptionalGroupby(filename);
        solver.solve();

        for (int i = 0; i < ((SelectWhereOptionalGroupby) solver).projXs.length; i++) {
            Olog.log.debug(solver.solver.getModel().evaluate(((SelectWhereOptionalGroupby) solver).projXs[i], false).toString() + ", projX" + i);
        }
        Olog.log.debug(solver.solver.getModel().evaluate(((SelectWhereOptionalGroupby) solver).fiMaX, false).toString());

        for (int i = 0; i < ((SelectWhereOptionalGroupby) solver).fiLopXs.length; i++) {
            Olog.log.debug(solver.solver.getModel().evaluate(((SelectWhereOptionalGroupby) solver).fiLopXs[i], false).toString());
        }
        for (int i = 0; i < ((SelectWhereOptionalGroupby) solver).fiPrXs.length; i++) {
            Olog.log.debug(solver.solver.getModel().evaluate(((SelectWhereOptionalGroupby) solver).fiPrXs[i], false).toString());
            Olog.log.debug(solver.solver.getModel().evaluate(((SelectWhereOptionalGroupby) solver).fiOpXs[i], false).toString());
            Olog.log.debug(solver.solver.getModel().evaluate(((SelectWhereOptionalGroupby) solver).fiVaXs[i], false).toString());
        }
        Olog.log.debug(solver.solver.getModel().evaluate(((SelectWhereOptionalGroupby) solver).grByX, false).toString() + ", grByX");
        Olog.log.debug(solver.solver.getModel().evaluate(((SelectWhereOptionalGroupby) solver).haPrX, false).toString() + ", haPrX");
        Olog.log.debug(solver.solver.getModel().evaluate(((SelectWhereOptionalGroupby) solver).haOpX, false).toString() + ", haOpX");
        Olog.log.debug(solver.solver.getModel().evaluate(((SelectWhereOptionalGroupby) solver).haVaX, false).toString() + ", haVaX");
        Olog.log.debug(solver.solver.getModel().evaluate(((SelectWhereOptionalGroupby) solver).haCoX, false).toString() + ", haCoX");

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

        Olog.log.info(solver.input.toString());
        Olog.log.info(solver.output.toString());

        Olog.log.info("\n" + solver.sql());
    }

}
