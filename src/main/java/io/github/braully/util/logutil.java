/*
Copyright 2019 Braully Rocha

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */
package io.github.braully.util;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.builder.api.AppenderComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilderFactory;
import org.apache.logging.log4j.core.config.builder.api.RootLoggerComponentBuilder;
import org.apache.logging.log4j.core.config.builder.impl.BuiltConfiguration;

/**
 *
 * @author strike
 */
public class logutil {

    static {
        ConfigurationBuilder<BuiltConfiguration> builder = ConfigurationBuilderFactory.newConfigurationBuilder();
        builder.setStatusLevel(Level.INFO);
        builder.setConfigurationName("DefaultLogger");
        // create a console appender
        AppenderComponentBuilder appenderBuilder = builder.newAppender("Console", "CONSOLE")
                .addAttribute("target", ConsoleAppender.Target.SYSTEM_OUT);
        // add a layout like pattern, json etc
        appenderBuilder.add(builder.newLayout("PatternLayout")
                .addAttribute("pattern", "%d %p %c [%t] %m%n"));

        RootLoggerComponentBuilder rootLogger = builder.newRootLogger(Level.DEBUG);
        rootLogger.add(builder.newAppenderRef("Console"));

        builder.add(appenderBuilder);
        builder.add(rootLogger);
        Configurator.reconfigure(builder.build());
    }

    public static Logger log = LogManager.getLogger("io.github.braully");

    public static void setInfoLevel() {
        Configurator.setLevel(log.getName(), Level.INFO);
    }

    public static void setDebugLevel() {
        Configurator.setLevel(log.getName(), Level.DEBUG);
    }

    public static void info(String strmsg) {
        log.info(strmsg);
    }

    public static void info(String strmsg, Throwable e) {
        log.info(strmsg, e);
    }

    public static void error(String strmsg) {
        log.error(strmsg);
    }

    public static void error(String strmsg, Throwable ex) {
        log.error(strmsg, ex);
    }

    public static void error(Throwable ex) {
        log.error(ex);
    }

    public static void debug(String fail, Exception e) {
        log.debug(fail, e);
    }

    public static void debug(String msg) {
        log.debug(msg);
    }

    public static void warn(String msg) {
        log.warn(msg);
    }

    public static void warn(String msg, Exception e) {
        log.warn(msg, e);
    }
}
