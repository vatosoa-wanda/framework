package url;

import java.util.HashMap;
import java.util.Map;

public class UrlPatternMatcher {

    public static boolean matches(String pattern, String path) {
        String[] pat = pattern.split("/");
        String[] act = path.split("/");
        if (pat.length != act.length) return false;

        for (int i = 0; i < pat.length; i++) {
            if (!pat[i].startsWith("{") && !pat[i].endsWith("}")) {
                if (!pat[i].equals(act[i])) {
                    return false;
                }
            }
        }
        return true;
    }

    public static Map<String, String> extractVariables(String pattern, String path) {
        Map<String, String> vars = new HashMap<>();
        String[] pat = pattern.split("/");
        String[] act = path.split("/");
        if (pat.length != act.length) return vars;

        for (int i = 0; i < pat.length; i++) {
            if (pat[i].startsWith("{") && pat[i].endsWith("}")) {
                String name = pat[i].substring(1, pat[i].length() - 1);
                vars.put(name, act[i]);
            }
        }
        return vars;
    }
}
