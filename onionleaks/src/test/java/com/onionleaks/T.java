package com.onionleaks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Minimal test harness: no external dependencies. */
public final class T {
    private T() {}

    public interface Body { void run() throws Exception; }
    record Result(String name, Throwable failure) {}

    private static final List<Result> results = new ArrayList<>();

    public static void test(String name, Body body) {
        try {
            body.run();
            results.add(new Result(name, null));
            System.out.println("  PASS  " + name);
        } catch (Throwable t) {
            results.add(new Result(name, t));
            System.out.println("  FAIL  " + name + "  -> " + t);
        }
    }

    public static void assertTrue(boolean c, String msg) { if (!c) throw new AssertionError(msg); }
    public static void assertFalse(boolean c, String msg) { if (c) throw new AssertionError(msg); }
    public static void assertEquals(Object exp, Object act, String msg) {
        if (exp == null ? act != null : !exp.equals(act)) throw new AssertionError(msg + ": expected <" + exp + "> got <" + act + ">");
    }
    public static void assertArrayEquals(byte[] exp, byte[] act, String msg) {
        if (!Arrays.equals(exp, act)) throw new AssertionError(msg + ": byte arrays differ");
    }
    public static void assertThrows(Class<? extends Throwable> type, Body b, String msg) {
        try { b.run(); } catch (Throwable t) {
            if (type.isInstance(t)) return;
            throw new AssertionError(msg + ": wrong exception " + t);
        }
        throw new AssertionError(msg + ": expected " + type.getSimpleName());
    }

    /** Prints a summary and returns the exit code. */
    public static int summary() {
        long failed = results.stream().filter(r -> r.failure != null).count();
        System.out.println();
        System.out.println(results.size() + " tests, " + (results.size() - failed) + " passed, " + failed + " failed");
        for (Result r : results) if (r.failure != null) { System.out.println("--- " + r.name); r.failure.printStackTrace(System.out); }
        return failed == 0 ? 0 : 1;
    }
}
