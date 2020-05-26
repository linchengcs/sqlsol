package util;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Helper {

    private static int GLOBAL_ID = 0;

    public static String printAllFields (Object o) {
        String ret = "";
        try {
            for (Field field : o.getClass().getDeclaredFields()) {
                ret += field.getName() + " : " + field.get(o).toString() + "\n";
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return ret;
    }

    public static int get_global_id () {
        return ++GLOBAL_ID;
    }
}
