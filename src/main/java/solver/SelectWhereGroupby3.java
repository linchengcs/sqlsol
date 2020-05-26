package solver;

import ch.qos.logback.classic.Level;
import parser.ExampleDS;
import util.Olog;

public class SelectWhereGroupby3 extends SelectWhereGroupby2 {


    String benchmarkPath;

    public SelectWhereGroupby3(String benchmarkPath) {
        super();
        this.benchmarkPath = benchmarkPath;
    }

    public void popInOutTables() {
        ExampleDS ds = ExampleDS.readFromFile(benchmarkPath);
        input = ds.inputs.get(0);
        output = ds.output;
    }

    public static void main(String[] args) {
        Olog.log.setLevel(Level.INFO);

        String filename = "data/sqlsynthesizer/joined/forum-questions-4";
//        String filename = "data/sqlsynthesizer/joined/textbook_5_1_1";
        SelectWhereGroupby3 solver = new SelectWhereGroupby3(filename);
        solver.solve();

        for (int i = 0; i < ((SelectWhereGroupby3) solver).projXs.length; i++) {
            Olog.log.debug(solver.solver.getModel().evaluate(((SelectWhereGroupby3) solver).projXs[i], false).toString() + ", projX" + i);
        }
        Olog.log.debug(solver.solver.getModel().evaluate(((SelectWhereGroupby3) solver).fiMaX, false).toString());

        for (int i = 0; i < ((SelectWhereGroupby3) solver).fiLopXs.length; i++) {
            Olog.log.debug(solver.solver.getModel().evaluate(((SelectWhereGroupby3) solver).fiLopXs[i], false).toString());
        }
        for (int i = 0; i < ((SelectWhereGroupby3) solver).fiPrXs.length; i++) {
            Olog.log.debug(solver.solver.getModel().evaluate(((SelectWhereGroupby3) solver).fiPrXs[i], false).toString());
            Olog.log.debug(solver.solver.getModel().evaluate(((SelectWhereGroupby3) solver).fiOpXs[i], false).toString());
            Olog.log.debug(solver.solver.getModel().evaluate(((SelectWhereGroupby3) solver).fiVaXs[i], false).toString());
        }
        Olog.log.debug(solver.solver.getModel().evaluate(((SelectWhereGroupby3) solver).grByX, false).toString() + ", grByX");
        Olog.log.debug(solver.solver.getModel().evaluate(((SelectWhereGroupby3) solver).haPrX, false).toString() + ", haPrX");
        Olog.log.debug(solver.solver.getModel().evaluate(((SelectWhereGroupby3) solver).haOpX, false).toString() + ", haOpX");
        Olog.log.debug(solver.solver.getModel().evaluate(((SelectWhereGroupby3) solver).haVaX, false).toString() + ", haVaX");
        Olog.log.debug(solver.solver.getModel().evaluate(((SelectWhereGroupby3) solver).haCoX, false).toString() + ", haCoX");

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
