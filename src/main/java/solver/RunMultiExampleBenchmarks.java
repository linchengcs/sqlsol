package solver;

import ch.qos.logback.classic.Level;
import parser.ExampleDS;
import table.lang.Table;
import util.Olog;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RunMultiExampleBenchmarks {

    List<Table> inputs = new ArrayList<>();
    List<Table> outputs = new ArrayList<>();

    public void runTest1() {
        inputs = new ArrayList<>();
        outputs = new ArrayList<>();

        inputs.add(new Table(
                "input_1",
                Arrays.asList("id1", "name1"),
                Arrays.asList(
                        Arrays.asList("1", "john"),
                        Arrays.asList("2", "bob")
                )));
        outputs.add(new Table(
                "output_1",
                Arrays.asList("id2", "name2"),
                Arrays.asList(
                        Arrays.asList("1", "john")
                )));
        inputs.add(new Table(
                "input_2",
                Arrays.asList("id1", "name1"),
                Arrays.asList(
                        Arrays.asList("2", "mary"),
                        Arrays.asList("1", "rose")
                )));
        outputs.add(new Table(
                "output_2",
                Arrays.asList("id2", "name2"),
                Arrays.asList(
                        Arrays.asList("1", "rose")
                )));

    }
    public void runTest0() {
        inputs = new ArrayList<>();
        outputs = new ArrayList<>();

        inputs.add(new Table(
                "input_1",
                Arrays.asList("id1", "name1"),
                Arrays.asList(
                        Arrays.asList("1", "john"),
                        Arrays.asList("2", "bob")
                )));
        outputs.add(new Table(
                "output_1",
                Arrays.asList("id2", "name2"),
                Arrays.asList(
                        Arrays.asList("1", "john")
                )));
        inputs.add(new Table(
                "input_2",
                Arrays.asList("id1", "name1"),
                Arrays.asList(
                        Arrays.asList("2", "mary"),
                        Arrays.asList("1", "rose")
                )));
        outputs.add(new Table(
                "output_2",
                Arrays.asList("id2", "name2"),
                Arrays.asList(
                        Arrays.asList("1", "rose")
                )));

    }

    public void runTest2() {
        inputs = new ArrayList<>();
        outputs = new ArrayList<>();

        inputs.add(new Table(
                "input_1",
                Arrays.asList("id1", "name1"),
                Arrays.asList(
                        Arrays.asList("1", "john"),
                        Arrays.asList("2", "bob")
                )));
        outputs.add(new Table(
                "output_1",
                Arrays.asList("id2", "name2"),
                Arrays.asList(
                        Arrays.asList("1", "john")
                )));
        inputs.add(new Table(
                "input_2",
                Arrays.asList("id1", "name1"),
                Arrays.asList(
                        Arrays.asList("1", "john"),
                        Arrays.asList("1", "mary")
                )));
        outputs.add(new Table(
                "output_2",
                Arrays.asList("id2", "name2"),
                Arrays.asList(
                        Arrays.asList("1", "john")
                )));
        inputs.add(new Table(
                "input_3",
                Arrays.asList("id1", "name1"),
                Arrays.asList(
                        Arrays.asList("1", "john"),
                        Arrays.asList("2", "john")
                )));
        outputs.add(new Table(
                "output_3",
                Arrays.asList("id2", "name2"),
                Arrays.asList(
                        Arrays.asList("1", "john")
                )));

    }


    public void runTest4() {
        inputs = new ArrayList<>();
        outputs = new ArrayList<>();

        inputs.add(new Table(
                "input_1",
                Arrays.asList("id1", "name1"),
                Arrays.asList(
                        Arrays.asList("stu1", "JR"),
                        Arrays.asList("stu2", "SR")
                )));
        outputs.add(new Table(
                "output_1",
                Arrays.asList("id2", "name2"),
                Arrays.asList(
                        Arrays.asList("stu1", "JR")
                )));
        inputs.add(new Table(
                "input_2",
                Arrays.asList("id1", "name1"),
                Arrays.asList(
                        Arrays.asList("stu1", "JR"),
                        Arrays.asList("stu1", "SR")
                )));
        outputs.add(new Table(
                "output_2",
                Arrays.asList("id2", "name2"),
                Arrays.asList(
                        Arrays.asList("stu1", "JR")
                )));

    }

    public void runTest3() {
        inputs = new ArrayList<>();
        outputs = new ArrayList<>();

        inputs.add(new Table(
                "input_1",
                Arrays.asList( "name", "level"),
                Arrays.asList(
                        Arrays.asList( "stu1", "JR"),
                        Arrays.asList( "stu2", "SR")
                )));
        outputs.add(new Table(
                "output_1",
                Arrays.asList("name", "level"),
                Arrays.asList(
                        Arrays.asList("stu1", "JR")
                )));
        inputs.add(new Table(
                "input_2",
                Arrays.asList("name", "level"),
                Arrays.asList(
                        Arrays.asList( "stu1", "SR"),
                        Arrays.asList( "stu2", "JR")
                )));
        outputs.add(new Table(
                "output_2",
                Arrays.asList("name", "level"),
                Arrays.asList(
                        Arrays.asList("stu2", "JR")
                )));
    }

    public void runTB111() {
        inputs = new ArrayList<>();
        outputs = new ArrayList<>();

        inputs.add(new Table(
                "input_1",
                Arrays.asList("C_name", "F_key", "S_key", "F_name", "S_name", "level"),
                Arrays.asList(
                        Arrays.asList("class1", "f1", "s1", "faculty1", "stu1", "JR"),
                        Arrays.asList("class2", "f2", "s2", "faculty2", "stu2", "SR")
                )));
        outputs.add(new Table(
                "output_1",
                Arrays.asList("S_name"),
                Arrays.asList(
                        Arrays.asList("stu1")
                )));
        inputs.add(new Table(
                "input_2",
                Arrays.asList("C_name", "F_key", "S_key", "F_name", "S_name", "level"),
                Arrays.asList(

                        Arrays.asList("class2", "f1", "s2", "faculty1", "stu2", "JR"),
                        Arrays.asList("class1", "f1", "s1", "faculty1", "stu1", "SR")
                )));
        outputs.add(new Table(
                "output_2",
                Arrays.asList("S_name"),
                Arrays.asList(
                        Arrays.asList("stu2")
                )));
        inputs.add(new Table(
                "input_3",
                Arrays.asList("C_name", "F_key", "S_key", "F_name", "S_name", "level"),
                Arrays.asList(

                        Arrays.asList("class2", "f1", "s2", "faculty1", "stu2", "JR"),
                        Arrays.asList("class1", "f1", "s1", "faculty3", "stu1", "JR")
                )));
        outputs.add(new Table(
                "output_3",
                Arrays.asList("S_name"),
                Arrays.asList(
                        Arrays.asList("stu2")
                )));
    }


    public void runTB113() {
        inputs = new ArrayList<>();
        outputs = new ArrayList<>();

        inputs.add(new Table(
                "input_1",
                Arrays.asList("class", "room", "student"),
                Arrays.asList(
                        Arrays.asList("c1", "r1", "s1"),
                        Arrays.asList("c1", "r2", "s2"),
                        Arrays.asList("c1", "r1", "s3"),
                        Arrays.asList("c1", "r2", "s4"),
                        Arrays.asList("c1", "r1", "s5"),
                        Arrays.asList("c11", "r11", "s11"),
                        Arrays.asList("c11", "r12", "s12"),
                        Arrays.asList("c11", "r13", "s13"),
                        Arrays.asList("c11", "r14", "s14"),
                        Arrays.asList("c11", "r15", "s15"),
                        Arrays.asList("c2", "r128", "s6"),
                        Arrays.asList("c3", "r128", "s7"),
                        Arrays.asList("c4", "r21", "s8"),
                        Arrays.asList("c5", "r22", "s9"),
                        Arrays.asList("c7", "r22", "s11"),
                        Arrays.asList("c6", "r22", "s9"),
                        Arrays.asList("c6", "r23", "s10"),
                        Arrays.asList("c8", "r24", "s21")
                )));
        outputs.add(new Table(
                "output_1",
                Arrays.asList("class"),
                Arrays.asList(
                        Arrays.asList("c1"),
                        Arrays.asList("c11"),
                        Arrays.asList("c2"),
                        Arrays.asList("c3")
                )));
    }


    public void runTB114() {
        inputs = new ArrayList<>();
        outputs = new ArrayList<>();

        inputs.add(new Table(
                "input_1",
                Arrays.asList("class", "time", "student"),
                Arrays.asList(
                        Arrays.asList("c1", "t1", "s1"),
                        Arrays.asList("c2", "t1", "s1"),
                        Arrays.asList("c3", "t2", "s2"),
                        Arrays.asList("c4", "t2", "s2"),
                        Arrays.asList("c5", "t3", "s3"),
                        Arrays.asList("c6", "t3", "s3"),
                        Arrays.asList("c10", "t3", "s3"),
                        Arrays.asList("c7", "t7", "s7")
                )));
        outputs.add(new Table(
                "output_1",
                Arrays.asList("student"),
                Arrays.asList(
                        Arrays.asList("s1"),
                        Arrays.asList("s2"),
                        Arrays.asList("s3")
                )));

        inputs.add(new Table(
                "input_2",
                Arrays.asList("class", "time", "student"),
                Arrays.asList(
                        Arrays.asList("c1", "t1", "s1"),
                        Arrays.asList("c2", "t1", "s1"),
                        Arrays.asList("c3", "t2", "s2"),
                        Arrays.asList("c4", "t2", "s2"),
                        Arrays.asList("c5", "t3", "s3"),
                        Arrays.asList("c6", "t3", "s3"),
                        Arrays.asList("c10", "t3", "s3"),
                        Arrays.asList("c17", "t17", "s71")
                )));
        outputs.add(new Table(
                "output_2",
                Arrays.asList("student"),
                Arrays.asList(
                        Arrays.asList("s1"),
                        Arrays.asList("s2"),
                        Arrays.asList("s3")
                )));
    }

    public void runTB116() {
	//"select faculty from input group by faculty having count(1) > 3"

        inputs = new ArrayList<>();
        outputs = new ArrayList<>();

        inputs.add(new Table(
                "input_1",
                Arrays.asList("class",  "student", "faculty"),
                Arrays.asList(
                        Arrays.asList("c1", "t1", "f1"),
                        Arrays.asList("c2", "t2", "f1"),
                        Arrays.asList("c1", "t1", "f1"),
                        Arrays.asList("c2", "t2", "f1"),
                        Arrays.asList("c1", "t5", "f2"),
                        Arrays.asList("c2", "t6", "f3"),
                        Arrays.asList("c3", "t7", "f3"),
                        Arrays.asList("c3", "t7", "f3"),
                        Arrays.asList("c3", "t7", "f3"),
                        Arrays.asList("c2", "t8", "f4"),
                        Arrays.asList("c1", "t9", "f4"),
                        Arrays.asList("c2", "t10", "f4"),
                        Arrays.asList("c1", "t11", "f4"),
                        Arrays.asList("c2", "t12", "f4"),
                        Arrays.asList("c5", "t6", "f5"),
                        Arrays.asList("c4", "t7", "f5"),
                        Arrays.asList("c6", "t7", "f5")
                )));
        outputs.add(new Table(
                "output_1",
                Arrays.asList("student"),
                Arrays.asList(
                        Arrays.asList("f1"),
                        Arrays.asList("f3"),
                        Arrays.asList("f4")
                )));
    }

    public void runTB117() {
        //"select level, average(age) from input group by level"

        inputs = new ArrayList<>();
        outputs = new ArrayList<>();

        inputs.add(new Table(
                "input_1",
                Arrays.asList("student",  "level", "age"),
                Arrays.asList(
                        Arrays.asList("s1", "JR", "18"),
                        Arrays.asList("s2", "SR", "24"),
                        Arrays.asList("s3", "JR", "21"),
                        Arrays.asList("s4", "SR", "22"),
                        Arrays.asList("s5", "JR", "18"),
                        Arrays.asList("s6", "SO", "20"),
                        Arrays.asList("s7", "SO", "22")
                )));
        outputs.add(new Table(
                "output_1",
                Arrays.asList("level", "avg"),
                Arrays.asList(
                        Arrays.asList("JR", "19"),
                        Arrays.asList("SR", "23"),
                        Arrays.asList("SO", "21")
                )));
    }

    public void runTB118() {
        //"select level, average(age) from input where level=JR group by level"

        inputs = new ArrayList<>();
        outputs = new ArrayList<>();

        inputs.add(new Table(
                "input_1",
                Arrays.asList("student",  "level", "age"),
                Arrays.asList(
                        Arrays.asList("s1", "JR", "23"),
                        Arrays.asList("s2", "SR", "24"),
                        Arrays.asList("s3", "JR", "23"),
                        Arrays.asList("s4", "SR", "22"),
                        Arrays.asList("s5", "JR", "23"),
                        Arrays.asList("s6", "SO", "20"),
                        Arrays.asList("s7", "SO", "22")
                )));
        outputs.add(new Table(
                "output_1",
                Arrays.asList("level", "avg"),
                Arrays.asList(
                        Arrays.asList("SR", "23"),
                        Arrays.asList("SO", "21")
                )));
    }

    public void runTB119() {
        //"select level, average(age) from input group by level"

        inputs = new ArrayList<>();
        outputs = new ArrayList<>();

        inputs.add(new Table(
                "input_1",
                Arrays.asList("student",  "level", "age"),
                Arrays.asList(
                        Arrays.asList("s1", "JR", "23"),
                        Arrays.asList("s2", "SR", "24"),
                        Arrays.asList("s3", "JR", "23"),
                        Arrays.asList("s4", "SR", "22"),
                        Arrays.asList("s5", "JR", "23"),
                        Arrays.asList("s6", "SO", "20"),
                        Arrays.asList("s7", "SO", "22")
                )));
        outputs.add(new Table(
                "output_1",
                Arrays.asList("level", "avg"),
                Arrays.asList(
                        Arrays.asList("SR", "23"),
                        Arrays.asList("SO", "21")
                )));
    }

    public void runDemo() {
        //"select course, avg(score) from input where level = SR group by course having avg(score) > 20"
        inputs = new ArrayList<>();
        outputs = new ArrayList<>();

        inputs.add(new Table (
                "input_1",
                Arrays.asList("student", "level", "course", "score"),
                Arrays.asList(
                        Arrays.asList("s1", "SR", "math", "70"),
                        Arrays.asList("s1", "SR", "english", "80"),
                        Arrays.asList("s6", "SR", "english", "60"),
                        Arrays.asList("s7", "SR", "english", "90"),
                        Arrays.asList("s2", "SR", "math", "59ver"),
                        Arrays.asList("s3", "SR", "english", "40"),
                        Arrays.asList("s4", "JR", "math", "70"),
                        Arrays.asList("s5", "SO", "english", "85")
                )
        ));

        outputs.add(new Table(
                "output_1",
                Arrays.asList("student", "avg"),
                Arrays.asList(
                        Arrays.asList("s1", "75"),
                        Arrays.asList("s6", "60"),
                        Arrays.asList("s7", "90")
                )
        ));


    }

    public void runFile(String filename) {
        ExampleDS ds = ExampleDS.readFromFile(filename);
        Table input = ds.inputs.get(0);
        Table output = ds.output;
        this.inputs.add(input);
        this.outputs.add(output);
    }

    // run a benchmark with multi IO examples in dir
    public void runFiles(String dir) {
        File dirFile = new File(dir);
        File[] files = dirFile.listFiles();
        Arrays.sort(files);
        for (File f : files) {
            if (f.isFile() && (f.getName().startsWith("forum") || f.getName().startsWith("textbook"))) {
                Olog.log.info("\n\n ------> Start processing benchmark: " + f.getPath() + " <------");
                runFile(f.getPath());
            }
        }
    }

    public static void main(String[] args) {
        Olog.log.setLevel(Level.DEBUG);
        RunMultiExampleBenchmarks a = new RunMultiExampleBenchmarks();

//        a.runTest0();  //?
//        a.runTest1();
//        a.runTest2();
//        a.runTest3();  //?
//        a.runTest4();
//        a.runTB111();   // sat
//        a.runTB113();    // unsat   'where' 'having' can not do 'or'
//        a.runTB114();    // sat
//  115 unsat   'not exists'
//        a.runTB116();
//        a.runTB117();
//        a.runTB118();
//        a.runTB119();
//        a.runDemo();


//        String filename = "data/sqlsynthesizer/joined/forum-questions-4";
//        String filename = "data/all/joined/dev_002M/";
//        a.runFile(filename);
        //        String filename = "data/sqlsynthesizer/joined/textbook_5_1_10";
        //        String filename = "data/sqlsynthesizer/joined/textbook_5_2_9";

//        String mfilename = "data/sqlsynthesizer/multi_example/textbook_5_1_1/";
//        String mfilename = "data/sqlsynthesizer/multi_example/textbook_5_1_6/";  // need run with SelectWhereGroup6
//        String mfilename = "data/sqlsynthesizer/multi_example/textbook_5_1_7/";
//        String mfilename = "data/sqlsynthesizer/multi_example/textbook_5_1_8/";

//        String mfilename = "data/multi_example/test/";
//        a.runFiles(mfilename);

        String filename = "data/multi_example/test_combined/forum_ex1";
        a.runFile(filename);


        long stime = System.currentTimeMillis();
//        for (int i = 0; i < 100; i++) {
            MESolver solver = new MESolver();
            solver.setInOutTables(a.inputs, a.outputs);
            solver.solve();
//        }
        long timeuse = System.currentTimeMillis() - stime;
        Olog.log.info(timeuse + "");

        Olog.log.info(solver.solver.getStatistics().toString());


        for (int i = 0; i < solver.projXs.length; i++) {
            Olog.log.debug(solver.solver.getModel().evaluate(solver.projXs[i], false).toString() + ", projX" + i);
        }

        Olog.log.debug("model for logic connectors: ");
        for (int i = 0; i < solver.fiLopXs.length; i++) {
            Olog.log.debug(solver.solver.getModel().evaluate(solver.fiLopXs[i], false).toString());
        }

        Olog.log.debug("model for filters: ");
        for (int i = 0; i < solver.fiPrXs.length; i++) {
            Olog.log.debug(solver.solver.getModel().evaluate(solver.fiMaXs[i], false).toString());
            Olog.log.debug(solver.solver.getModel().evaluate(solver.fiPrXs[i], false).toString());
            Olog.log.debug(solver.solver.getModel().evaluate(solver.fiOpXs[i], false).toString());
            Olog.log.debug(solver.solver.getModel().evaluate(solver.fiVaXs[i], false).toString());
        }
        Olog.log.debug(solver.solver.getModel().evaluate(solver.grByX, false).toString() + ", grByX");
        Olog.log.debug(solver.solver.getModel().evaluate(solver.haMaX, false).toString() + ", haMax");
        Olog.log.debug(solver.solver.getModel().evaluate(solver.haPrX, false).toString() + ", haPrX");
        Olog.log.debug(solver.solver.getModel().evaluate(solver.haOpX, false).toString() + ", haOpX");
        Olog.log.debug(solver.solver.getModel().evaluate(solver.haVaX, false).toString() + ", haVaX");
        Olog.log.debug(solver.solver.getModel().evaluate(solver.haCoX, false).toString() + ", haCoX");

        //        for (int i = 0; i < solver.input.nrow(); i++) {
        //            Olog.log.debug(solver.solver.getModel().evaluate(solver.ts.select(solver.inputExpr, solver.projXs[1], solver.ctx.mkInt(i)), false).toString());
        //        }


        Olog.log.info(solver.solver.getStatistics().toString());
        Olog.log.info(solver.getStatistics("max memory"));
        Olog.log.info(solver.getStatistics("memory"));

        Olog.log.info("\n" + solver.sql());
    }
    }

