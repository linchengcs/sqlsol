package table.lang;

import table.lang.datatype.Value;
import java.util.ArrayList;
import java.util.List;
/**
 * Created by clwang on 12/14/15.
 */
public class TableRow {
    /* the table containing the row */
    List<String> fieldNames = new ArrayList<String>();
    List<Value> values = new ArrayList<Value>();

    private TableRow() {}

    public static TableRow TableRowFromString(String tableName, List<String> names, List<String> rowContent) {
        TableRow newRow = new TableRow();

        for (String s : names)
            newRow.fieldNames.add(s);
        for (String s : rowContent)
            newRow.values.add(Value.parse(s));

        return newRow;
    }

    public static TableRow TableRowFromContent(String tableName, List<String> names, List<Value> rowContent) {
        TableRow newRow = new TableRow();
        newRow.fieldNames.addAll(names);
        newRow.values.addAll(rowContent);
        return newRow;
    }

    public List<Value> getValues() {
        return this.values;
    }

    public Value getValue(int i) {
        if (this.values.size() <= i) {
            System.err.println("[Error@TableRow59] None Exist Value");
        }
        return this.values.get(i);
    }

    @Override
    public String toString() {
        String str = "";
        for (Value i : values) {
            str += i.toString() + " | ";
        }
        // ignore the last "|"
        str = str.substring(0, str.length() - 2);
        return str;
    }


    public int index = -1;

}
