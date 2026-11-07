package com.onionleaks;

/** Entry point for ./build.sh test — runs every suite and exits non-zero on failure. */
public class AllTests {
    public static void main(String[] args) throws Exception {
        CryptoTests.run();
        ServiceTests.run();
        HttpFlowTests.run();
        ToolTests.run();
        System.exit(T.summary());
    }
}
