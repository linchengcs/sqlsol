package solver;

import com.microsoft.z3.*;
import table.lang.Table;
import util.Olog;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SelectWhereFuncCond extends AbstractSolver {

    Expr[] projXs;
    FuncDecl filterX;

    @Override
    public void popInOutTables() {
        this.input = new Table(
                "input",
                Arrays.asList("id1", "name1"),
                Arrays.asList(
                        Arrays.asList("1", "john"),
                        Arrays.asList("2", "bob")
                ));
        this.output = new Table(
                "output",
                Arrays.asList("id2", "name2"),
                Arrays.asList(
                        Arrays.asList("1", "john")
                ));
    }

    @Override
    public void addUnknowns() {
        this.projXs = new SeqExpr[this.output.ncol()];
        for (int col = 0; col < this.output.ncol(); col++)
            this.projXs[col] = this.ctx.mkConst("projX" + col, this.ctx.getStringSort());

        Sort[] sorts = new Sort[this.input.ncol()];
        for (int col = 0; col < this.input.ncol(); col++)
            sorts[col] = this.ts.valSort();
        this.filterX = this.ctx.mkFuncDecl("filter", sorts, this.ctx.mkBoolSort());
    }

    public List<BoolExpr> encodeDomain() {
        return new ArrayList<>();
    }

    public List<BoolExpr> encodeRelation() {
        List<BoolExpr> ret = new ArrayList<>();
        ret.add(
                (BoolExpr)this.ctx.mkApp(this.filterX, this.ts.select(this.inputExpr, "id1", 0), this.ts.select(this.inputExpr, "name1", 0))
        );
        ret.add(
                this.ctx.mkNot(
                (BoolExpr)this.ctx.mkApp(this.filterX, this.ts.select(this.inputExpr, "id1", 1), this.ts.select(this.inputExpr, "name1", 1))
        ));
        return ret;
    }

    public static void main(String[] args) {
        SelectWhereFuncCond select = new SelectWhereFuncCond();
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
