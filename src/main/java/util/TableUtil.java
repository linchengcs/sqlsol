package util;

import table.lang.Table;
import table.lang.TableRow;
import table.lang.datatype.Value;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TableUtil {

    public static Map<String, String> computeForeignKey(Table t1, Table t2) {
        List<String> t1ks = t1.getSchema();
        List<String> t2ks = t2.getSchema();
        String k = null;
        for (String t : t1ks) {
            if (t2ks.contains(t)) {
                k = t;
                break;
            }
        }
//        assert k != null;
        Map<String, String> ret = new HashMap<>();
        if (k != null)
            ret.put(k, k);
        return ret;

    }

    public static boolean hasForeignKey(Table t1, Table t2) {
        return !computeForeignKey(t1, t2).isEmpty();
    }

    public static List<Table> computeJoinOrder(List<Table> inputs) {
        List<Table> ret = new ArrayList<>();
        ret.add(inputs.get(0));
        while (ret.size() < inputs. size()) {
            List<Table> tmp = new ArrayList<>(inputs);
            tmp.removeAll(ret);
            for (Table t : tmp) {
                for (Table x : ret) {
                    boolean flag = false;
                    if (hasForeignKey(x, t)) {
                        ret.add(t);
                        flag = true;
                    }
                    if (flag)
                        break;

                }
            }
        }
        return ret;
    }

    public static Table joinInputs(List<Table> inputs1) {
        List<Table> inputs = computeJoinOrder(inputs1);
        Table ret = inputs.get(0);
        for (int i = 1; i < inputs.size(); i++) {
            Map<String, String> fk = computeForeignKey(ret, inputs.get(i));
            ret = innerJoin(ret, inputs.get(i), fk);
        }
        assert !ret.getContent().isEmpty();
        return ret;
    }

    public static Table innerJoin(Table tbl1, Table tbl2, Map<String, String> fk){
        String retName = tbl1.getName() + "_" + tbl2.getName() + "_join";
        List<String> retSchema = new ArrayList<>();
        List<TableRow> retRows = new ArrayList<>();
        String t1k = null, t2k = null;
        int t1ki, t2ki;
        for (Map.Entry<String, String> entry : fk.entrySet()) {
            t1k = entry.getKey();
            t2k = entry.getKey();
            break;
        }
        t1ki = tbl1.retrieveIndex(t1k);
        t2ki = tbl2.retrieveIndex(t2k);
        retSchema.addAll(tbl1.getSchema());
        for (int i = 0; i < tbl2.getSchema().size(); i++) {
            if (i != t2ki)
                retSchema.add(tbl2.getSchema().get(i));
        }

        for (TableRow r : tbl1.getContent()) {
            Value value = r.getValue(t1ki);
            for (TableRow r2 : tbl2.getContent()) {
                Value value2 = r2.getValue(t2ki);
                if (value.equals(value2)) {
                    List<String> names = new ArrayList<>(retSchema);
                    List<Value> content = new ArrayList<>(r.getValues());
                    for (int i = 0; i < tbl2.getSchema().size(); i++) {
                        if (i != t2ki) {
                            content.add(r2.getValue(i));
                        }
                    }
                    TableRow nr = TableRow.TableRowFromContent(retName, names, content);
                    retRows.add(nr);
                }
            }
        }
        Table ret = new Table();
        ret.initialize(retName, retSchema, retRows);
        return ret;

    }
}
