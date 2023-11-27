/*
 * Copyright 2021 Rundeck, Inc. (http://rundeck.com)
 */
package com.dtolabs.rundeck.app.internal.logging;

import com.dtolabs.rundeck.core.execution.Contextual;
import com.dtolabs.rundeck.core.logging.LogEvent;
import com.dtolabs.rundeck.core.logging.LogLevel;
import com.dtolabs.rundeck.core.logging.LogUtil;
import com.dtolabs.rundeck.core.utils.LogBuffer;

import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public class LogEventBuffer implements Comparable, LogBuffer<LogEvent> {
    Date                  time;
    Map<String, String>   context;
    Contextual            listener;
    ByteArrayOutputStream baos;
    Long                  serial;
    final static AtomicLong counter = new AtomicLong(0);
    private      Charset    charset;
    private      LogLevel   level;

    public LogEventBuffer(LogLevel level, final Contextual listener, Charset charset) {
        this.level = level;
        serial = counter.incrementAndGet();
        this.listener = listener;
        reset(listener.getContext());
        this.charset = charset;
    }

    public LogEventBuffer(LogLevel level, final Contextual listener) {
        this.level = level;
        serial = counter.incrementAndGet();
        this.listener = listener;
        reset(listener.getContext());
        this.charset = null;
    }

    public boolean isEmpty() {
        return time == null;
    }

    public void clear() {
        this.time = null;
        this.context = null;
        this.baos = new ByteArrayOutputStream();
    }

    public void reset() {
        reset(listener.getContext());
    }
    void reset(final Map<String, String> context) {
        this.time = new Date();
        this.context = context;
        this.baos = new ByteArrayOutputStream();
    }

    @Override
    public void write(final byte b) {
        baos.write(b);
    }


    public LogEvent get() {
        String string = baos != null ? (charset != null ? baos.toString(charset) : baos.toString()):"";
        return new DefaultLogEvent(
                level,
                time != null ? time : new Date(),
                string,
                LogUtil.EVENT_TYPE_LOG,
                context != null ? context : new HashMap<>()
        );
    }

    @Override
    public int compareTo(final Object o) {
        if (!(o instanceof LogEventBuffer)) {
            return -1;
        }
        LogEventBuffer other = (LogEventBuffer) o;
        int td = compareDates(time, other.time);
        if (td != 0) {
            return td;
        }
        return serial.compareTo(other.serial);
    }

    static int compareDates(Date a, Date b) {
        if (a == null && b == null) {
            return 0;
        }
        if (a != null && b != null) {
            return a.compareTo(b);
        }

        return a != null ? -1 : 1;

    }
}
