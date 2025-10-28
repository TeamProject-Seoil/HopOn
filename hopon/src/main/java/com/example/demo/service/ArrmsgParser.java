// src/main/java/com/example/demo/service/ArrmsgParser.java
package com.example.demo.service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ArrmsgParser {
    private ArrmsgParser() {}

    private static final Pattern P_MIN_SEC = Pattern.compile("(\\d+)\\s*분\\s*(\\d+)\\s*초\\s*후");
    private static final Pattern P_MIN = Pattern.compile("(\\d+)\\s*분\\s*후");
    private static final Pattern P_SEC = Pattern.compile("(\\d+)\\s*초\\s*후");

    /** 서울시 arrmsgX 문자열을 ETA(초)로 변환. 못 파싱하면 null */
    public static Integer toSeconds(String s) {
        if (s == null) return null;
        s = s.trim();
        if (s.isEmpty()) return null;

        if (s.contains("곧 도착") || s.contains("곧도착") || s.contains("도착")) {
            return 0;
        }
        Matcher m;

        m = P_MIN_SEC.matcher(s);
        if (m.find()) {
            int min = parseInt(m.group(1));
            int sec = parseInt(m.group(2));
            return min * 60 + sec;
        }

        m = P_MIN.matcher(s);
        if (m.find()) {
            int min = parseInt(m.group(1));
            return min * 60;
        }

        m = P_SEC.matcher(s);
        if (m.find()) {
            int sec = parseInt(m.group(1));
            return sec;
        }

        return null;
    }

    private static int parseInt(String s) {
        try { return Integer.parseInt(s); } catch (Exception e) { return 0; }
    }
}
