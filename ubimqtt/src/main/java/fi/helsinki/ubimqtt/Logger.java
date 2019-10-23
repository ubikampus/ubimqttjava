package fi.helsinki.ubimqtt;

public class Logger {

    public static void log(String s) {
        StackTraceElement l = new Exception().getStackTrace()[0];
        System.out.println(
                l.getClassName() + "/" + l.getMethodName() + ":" + l.getLineNumber() + ": " + s);
    }

}
