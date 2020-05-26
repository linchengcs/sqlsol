package solver;

import table.lang.Table;
import util.Olog;

import java.util.Arrays;

public class SelectWhere3 extends SelectWhere {

    public void popInOutTables() {
        input = new Table(
                "input",
                Arrays.asList("id1", "name1"),
                Arrays.asList(
                        Arrays.asList("1", "john"),
                        Arrays.asList("2", "bob"),
                        Arrays.asList("3", "mary"),
                        Arrays.asList("4", "don")
                ));
        output = new Table(
                "output",
                Arrays.asList("id2", "name2"),
                Arrays.asList(
                        Arrays.asList("1", "john"),
                        Arrays.asList("3", "mary")
                ));
    }


    public static void main(String[] args) {
        AbstractSolver solver = new SelectWhere3();
        solver.solve();

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
