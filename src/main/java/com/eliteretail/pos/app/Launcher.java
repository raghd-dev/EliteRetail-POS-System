package com.eliteretail.pos.app;

/**
 * Classpath-friendly entry point for IDEs such as IntelliJ IDEA.
 *
 * <p>The Java launcher treats a main class that directly extends
 * {@code javafx.application.Application} specially. When JavaFX is supplied
 * through Maven dependencies on the classpath, launching that subclass
 * directly can produce "JavaFX runtime components are missing". This wrapper
 * avoids that special-case and delegates to the real JavaFX application.</p>
 */
public final class Launcher {
    private Launcher() {
    }

    public static void main(String[] args) {
        EliteRetailApplication.main(args);
    }
}
