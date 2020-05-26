package table.lang;

import table.lang.datatype.ValType;
import table.lang.datatype.Value;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by clwang on 12/12/15.
 * The class representing a Table in a database
 */
public class Table {

    private static int TableCount = 0;
    public static String AssignNewName() {
        TableCount ++;
        return "{Default" + TableCount + "}";
    }

    private String name = "";
    private List<String> schema = new ArrayList<>();
    private List<table.lang.TableRow> rows = new ArrayList<>();

    public Table() {}

    /**
     * Initializer of a table, the content here is only strings
     * @param tableName the name of the table
     * @param schema the meta data of the table
     * @param rawContent all contents in the form of string
     */
    public Table(String tableName, List<String> schema, List<List<String>> rawContent) {
        List<table.lang.TableRow> rows = rawContent
                .stream()
                .map(sList -> table.lang.TableRow.TableRowFromString(tableName, schema, sList))
                .collect(Collectors.toList());
        this.initialize(tableName, schema, rows);
    }

    public void initialize(String tableName, List<String> schema, List<table.lang.TableRow> rows) {
        this.name = tableName;
        this.schema = schema;
        this.rows = rows;
    }

    public List<String> getSchema() { return this.schema; }
    public String getName() { return this.name; }
    public List<table.lang.TableRow> getContent() { return this.rows; }

    @Override
    public String toString() {
        String str = "@" + name + "\r\n";
        int k = 0;
        for (String i : schema) {
            str += i + "(" + this.getSchemaType().get(k) + ")" + " | ";
            k ++;
        }
        str = str.substring(0, str.length() - 2) + "\r\n";
        for (table.lang.TableRow row : rows) {
            str += row.toString() + "\r\n";
        }
        return str;
    }

    public String toStringWithIndent(String indent) {
        String str = indent + "# " + name + "\r\n\r\n";

        str += indent + " | ";
        for (String i : schema) {
            str += i + " | ";
        }
        str += "\r\n";

        str += indent ;
        for (TableRow row : rows) {
            str += "| " + row.toString() + " |\r\n";
            str += indent;
        }
        return str + "\r\n";
    }

    /**
     * Get a column from the table, specified by index
     * @param index the column to be retrieved
     * @return The column represented as a list
     */
    public List<Value> getColumnByIndex(Integer index) {
        if (index >= this.getSchema().size())
            System.err.println("[ERROR@Table107] column index is bigger than the schema size.");

        return this.getContent().stream().map(r -> r.getValue(index)).collect(Collectors.toList());
    }



    public int retrieveIndex(String name) {
        String fieldName = name;
        if (this.name.equals("anonymous"))
            fieldName = name;
        else
            fieldName = name.substring(name.indexOf(".") + 1);

        for (int i = 0; i < this.schema.size(); i ++) {
            if (this.schema.get(i).equals(fieldName))
                return i;
        }
        System.err.println("[Error@Table152]Metadata retrieval fail.");
        return -1;
    }



    public List<ValType> storedSchemaType = null;
    // return the schema type of a table.
    public List<ValType> getSchemaType() {

        if (this.storedSchemaType != null) return this.storedSchemaType;

        List<List<ValType>> typeCollections = new ArrayList<>();
        for (int i = 0; i < this.getSchema().size(); i ++) {
            typeCollections.add(new ArrayList<>());
        }
        for (int i = 0; i < this.getContent().size(); i ++) {
            int j = 0;
            for (Value v : this.getContent().get(i).getValues()) {
                typeCollections.get(j).add(v.getValType());
                j ++;
            }
        }
        return typeCollections.stream().map(l -> typeLowerBound(l)).collect(Collectors.toList());
    }
    public ValType typeLowerBound(List<ValType> types) {
        if (types.isEmpty()) return null;
        ValType currentType = types.get(0);
        for (ValType type : types) {
            if (currentType == type) {
                continue;
            }
            return ValType.StringVal;
        }
        return currentType;
    }

    public boolean isEmpty() {
        if (this.getContent().size() == 0)
            return true;
        return false;
    }


    // added by Lin Cheng
    private Integer ncol = null;
    private Integer nrow = null;
    public int ncol(){
        if (this.ncol == null)
            this.ncol = schema.size();
        return this.ncol;
    }
    public  int nrow(){
        if (this.nrow == null)
            this.nrow = this.rows.size();
        return this.nrow;
    }

    public Value get(int col, int row){
        return this.getContent().get(row).getValue(col);
    }

    public String header(int col){
        return this.schema.get(col);
    }


}
