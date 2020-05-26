package theory;

import com.microsoft.z3.*;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;

import static org.junit.Assert.*;


public class TableSortsTest {

    TableSorts ts;
    Context ctx;
    Solver solver;

    @Before
    public void setup(){
        HashMap<String, String> cfg = new HashMap<String, String>();
        cfg.put("model", "true");
        this.ctx = new Context(cfg);
        this.ts = new TableSorts(ctx);
        this.solver = solver;
    }

    @Test
    public void testSetup(){
        assertNotNull(this.ts);
    }

    @Test
    public void test1(){

        ArraySort tblSort = ts.tblSort();
        ArrayExpr t = (ArrayExpr)ctx.mkConst("t", tblSort);
        SeqExpr s = ts.mkStringExpr("s");
        IntExpr i = ctx.mkIntConst("i");
        DatatypeExpr v = ts.mkValExpr("v");

        BoolExpr b = ctx.mkEq(ts.select(t, s, i), v);
        BoolExpr b1 = ts.isNilVal(v);

//        solver.add(b);
//        solver.add(b1);
//        solver.check();
//        assertEquals (solver.check() , Status.SATISFIABLE);
    }

    @Test
    public void  testDatatypeRecognizer() {
        DatatypeExpr e = ts.mkNilVal();

    }


    @Test
    public void testHello() {
        assertNotNull("no message", "hello");
    }
}
