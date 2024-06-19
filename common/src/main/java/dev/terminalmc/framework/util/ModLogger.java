package dev.terminalmc.framework.util;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.util.StackLocatorUtil;

public class ModLogger {
    private final Logger logger;

    public ModLogger(Logger logger) {
        this.logger = logger;
    }

    public ModLogger(String name) {
        this(LogManager.getLogger(name));
    }

    private String edit(Level level, String message) {
        if (level == Level.DEBUG) return String.format("[%s/%s]: %s", logger.getName(),
                StackLocatorUtil.getCallerClass(4).getSimpleName(), message);
        return String.format("[%s]: %s", logger.getName(), message);
    }

    private void log(Level level, String message, Object... args) {
        if (!logger.isEnabled(level)) return;
        logger.log(level, edit(level, message), args);
    }

    public void trace(String message, Object... args){
        log(Level.TRACE, message, args);
    }

    public void debug(String message, Object... args){
        log(Level.DEBUG, message, args);
    }

    public void info(String message, Object... args){
        log(Level.INFO, message, args);
    }

    public void warn(String message, Object... args){
        log(Level.WARN, message, args);
    }

    public void error(String message, Object... args){
        log(Level.ERROR, message, args);
    }

    public void fatal(String message, Object... args){
        log(Level.FATAL, message, args);
    }
}
