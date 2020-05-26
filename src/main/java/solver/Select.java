package solver;

import com.microsoft.z3.*;
import table.lang.Table;
import util.Olog;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Select extends AbstractSolver {

    Expr[] projXs;  // projection unknown

    public Select(){
        super();
    }

    public void popInOutTables() {
        this.input = new Table(
                "input",
                Arrays.asList("id1", "name"),
                Arrays.asList(
                        Arrays.asList("1", "john"),
                        Arrays.asList("2", "bob")
                ));
        this.output = new Table(
                "output",
                Arrays.asList("id2"),
                Arrays.asList(
                        Arrays.asList("1"),
                        Arrays.asList("2")
                ));

    }

    public void addUnknowns() {
        this.projXs = new SeqExpr[this.output.ncol()];
        for (int col = 0; col < this.output.ncol(); col++)
                this.projXs[col] = this.ctx.mkConst("projX" + col, this.ctx.getStringSort());
    }

    public List<BoolExpr> encodeDomain(){
        List<BoolExpr> ret  = new ArrayList<>();
        BoolExpr[] ors = new BoolExpr[(this.input.ncol())];
        for (int i = 0; i < this.projXs.length; i++) {
            for (int col = 0; col < this.input.ncol(); col++) {
                ors[col] = this.ctx.mkEq(this.projXs[i], this.ctx.mkString(this.input.header(col)));
            }
            BoolExpr b = this.ctx.mkOr(ors);
            ret.add(b);
        }
        return ret;
    }

    public List<BoolExpr> encodeRelation(){
        List<BoolExpr> ret = new ArrayList<>();
        for (int col = 0; col < this.output.ncol(); col++) {
            for (int row = 0; row < this.input.nrow(); row++) {
                BoolExpr b = this.ctx.mkEq(
                        this.ts.select(this.inputExpr, (SeqExpr) this.projXs[col], row),
                        this.ts.select(this.outputExpr, this.ctx.mkString(this.output.header(col)), row)
                );
                ret.add(b);
            }
        }
        return ret;
    }


    public static void main(String[] args) {
        Select select = new Select();
        select.popInOutTables();

        Olog.log.debug(select.input.toString());
        Olog.log.debug(select.output.toString());
        Olog.log.debug(select.input.ncol()+"");
        Olog.log.debug(select.input.nrow()+"");
        Olog.log.debug(select.output.ncol()+"");
        Olog.log.debug(select.output.nrow()+"");
        select.solve();
    }
}
