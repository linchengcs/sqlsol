package parser;

import table.lang.Table;
import util.Olog;
import util.TableUtil;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.List;

public class PreJoin {
//    private String benchmarkDir = "data/sqlsynthesizer/";
//    private String benchmarkDir = "data/top_rated_posts/";
private String benchmarkDir = "data/all/";
//    private String benchmarkDir = "data/dev_set/";

    public static void main(String[] args) {
        PreJoin ase = new PreJoin();
        ase.writeJoinedTablesToFiles();
//        ase.test();
    }


    public void writeJoinedTablesToFiles(){
        File dir = new File(this.benchmarkDir);
        File[] files = dir.listFiles();
        Arrays.sort(files);
        Olog.log.info(benchmarkDir + " has " + files.length + " files");
        for (File f : files) {
            if (f.isDirectory())
                continue;
            System.out.println(f.getName());
            ExampleDS ds = ExampleDS.readFromFile(f.getPath());
            if (ds.inputs.size() > 0) {
                Table input = computeJoinedTables(ds.inputs);
                Table output = ds.output;
                try {
                    Path path = Paths.get(benchmarkDir + "/joined/" + f.getName());
                    Files.write(path, input.toStringWithIndent("").getBytes());
                    Files.write(path, output.toStringWithIndent("").getBytes(), StandardOpenOption.APPEND);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    public Table computeJoinedTables(List<Table> inputs) {
        Table ret = TableUtil.joinInputs(inputs);
        return ret;
    }

    public void test(){
//        String b = "data/sqlsynthesizer/forum-questions-5";
//        String b = "data/sqlsynthesizer/textbook_5_2_2";
        String b = "data/recent_posts/016M";
        ExampleDS  e = ExampleDS.readFromFile(b);
        List<Table> inputs = e.inputs;
        Table j = computeJoinedTables(inputs);
        Olog.log.info(j.toString());
        Olog.log.info(e.output.toString());
    }

}
