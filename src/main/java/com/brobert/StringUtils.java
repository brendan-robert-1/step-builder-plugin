package com.brobert;

import java.util.LinkedList;
import java.util.List;

public class StringUtils {

    public static String getPascalCase(String name) {
        LinkedList<String> pascalCaseSplit = splitCamelCaseString(name);
        pascalCaseSplit = pascalCase(pascalCaseSplit);
        return listToDelimitedString(pascalCaseSplit, "");
    }



    public static String getCamelCase(String name) {
        LinkedList<String> camelCaseSplit = splitCamelCaseString(name);
        camelCaseSplit = camelCase(camelCaseSplit);
        return listToDelimitedString(camelCaseSplit, "");
    }



    private static LinkedList<String> camelCase(LinkedList<String> camelCaseSplit) {
        LinkedList<String> pascalCase = new LinkedList<>();
        int i = 0;
        for (String s : camelCaseSplit) {
            String str = s;
            if(i++ != 0){
                str = upperFirstChar(s);
            }
            pascalCase.add(str);
        }
        return pascalCase;
    }



    private static LinkedList<String> pascalCase(LinkedList<String> pascalCaseSplit) {
        LinkedList<String> pascalCase = new LinkedList<>();
        for (String s : pascalCaseSplit) {
            String str = upperFirstChar(s);
            pascalCase.add(str);
        }
        return pascalCase;
    }



    private static LinkedList<String> splitCamelCaseString(String s) {
        LinkedList<String> result = new LinkedList<String>();
        for (String w : s.split("(?<!(^|[A-Z]))(?=[A-Z])|(?<!^)(?=[A-Z][a-z])")) {
            result.add(w);
        }
        return result;
    }



    private static String listToDelimitedString(List<String> strings, String delimiter) {
        StringBuilder sb = new StringBuilder();
        int count = 0;
        for (String s : strings) {
            sb.append(s);
            if (++count != strings.size()) {
                sb.append(delimiter);
            }
        }
        return sb.toString();
    }



    private static String upperFirstChar(String s) {
        String lower = s.toLowerCase();
        return lower.substring(0, 1).toUpperCase() + lower.substring(1);
    }

}
