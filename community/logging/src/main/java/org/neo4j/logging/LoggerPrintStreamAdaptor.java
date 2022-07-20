/*
 * Copyright (c) "Neo4j"
 * Neo4j Sweden AB [http://neo4j.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.logging;

import java.io.PrintStream;
import java.util.Objects;
import org.apache.logging.log4j.core.util.NullOutputStream;

/**
 * Adaptor class to log using {@link InternalLog} instead of {@link PrintStream}.
 */
public class LoggerPrintStreamAdaptor extends PrintStream {
    InternalLog log;

    Level level;

    public LoggerPrintStreamAdaptor(InternalLog log, Level level) {
        super(NullOutputStream.getInstance());
        this.level = Objects.requireNonNull(level);
        this.log = Objects.requireNonNull(log);
    }

    @Override
    public void print(final boolean m) {
        log(String.valueOf(m));
    }

    @Override
    public void print(final char m) {
        log(String.valueOf(m));
    }

    @Override
    public void print(final char[] m) {
        log(String.valueOf(m));
    }

    @Override
    public void print(final double m) {
        log(String.valueOf(m));
    }

    @Override
    public void print(final float m) {
        log(String.valueOf(m));
    }

    @Override
    public void print(final int m) {
        log(String.valueOf(m));
    }

    @Override
    public void print(final long m) {
        log(String.valueOf(m));
    }

    @Override
    public void print(final Object m) {
        log(String.valueOf(m));
    }

    @Override
    public void print(final String m) {
        log(String.valueOf(m));
    }

    @Override
    public void println() {}

    @Override
    public void println(final boolean m) {
        log(String.valueOf(m));
    }

    @Override
    public void println(final char m) {
        log(String.valueOf(m));
    }

    @Override
    public void println(final char[] m) {
        log(String.valueOf(m));
    }

    @Override
    public void println(final double m) {
        log(String.valueOf(m));
    }

    @Override
    public void println(final float m) {
        log(String.valueOf(m));
    }

    @Override
    public void println(final int m) {
        log(String.valueOf(m));
    }

    @Override
    public void println(final long m) {
        log(String.valueOf(m));
    }

    @Override
    public void println(final Object m) {
        log(String.valueOf(m));
    }

    @Override
    public void println(final String m) {
        log(m);
    }

    public PrintStream printf(String s, Object... args) {
        log(s, args);
        return this;
    }

    private void log(String s, Object... args) {
        s = s.strip();
        switch (level) {
            case INFO:
                log.info(s, args);
                break;
            case ERROR:
                log.error(s, args);
                break;
            case WARN:
                log.warn(s, args);
                break;
            case DEBUG:
                log.debug(s, args);
                break;
        }
    }

    private void log(String s) {
        s = s.strip();
        switch (level) {
            case INFO:
                log.info(s);
                break;
            case ERROR:
                log.error(s);
                break;
            case WARN:
                log.warn(s);
                break;
            case DEBUG:
                log.debug(s);
                break;
        }
    }
}
