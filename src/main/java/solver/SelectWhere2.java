package solver;

import table.lang.Table;
import util.Olog;

import java.util.Arrays;

public class SelectWhere2 extends SelectWhere {

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
                        Arrays.asList("1", "john"),
                        Arrays.asList("2", "bob")
                ));
    }


    public static void main(String[] args) {
        AbstractSolver solver = new SelectWhere2();
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
