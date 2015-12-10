package com.grb.impulse.utils;

import org.apache.sshd.server.Command;
import org.apache.sshd.server.Environment;
import org.apache.sshd.server.ExitCallback;

import java.io.*;

/**
 * Created by gbromfie on 12/4/15.
 */
public class SshServerClient implements Command {

    private InputStream in;
    private OutputStream out;
    private OutputStream err;
    private ExitCallback callback;
    private Environment environment;
    private Thread thread;
    private Runnable r;
    private String threadName;
    private boolean closed = false;

    public SshServerClient() {
    }

    public void setRunnable(Runnable r, String threadName) {
        this.r = r;
        this.threadName = threadName;
    }

    public InputStream getIn() {
        return in;
    }

    public OutputStream getOut() {
        return out;
    }

    public OutputStream getErr() {
        return err;
    }

    public Environment getEnvironment() {
        return environment;
    }

    @Override
    public void setInputStream(InputStream in) {
        this.in = in;
    }

    @Override
    public void setOutputStream(OutputStream out) {
        this.out = out;
    }

    @Override
    public void setErrorStream(OutputStream err) {
        this.err = err;
    }

    @Override
    public void setExitCallback(ExitCallback callback) {
        if (closed) {
            callback.onExit(0);
        }
        this.callback = callback;
    }

    @Override
    public void start(Environment env) throws IOException {
        environment = env;
        thread = new Thread(r, threadName);
        thread.start();
    }

    @Override
    public void destroy() {
        try {
            in.close();
        } catch (IOException e) {
        }
        try {
            out.close();
        } catch (IOException e) {
        }
        try {
            err.close();
        } catch (IOException e) {
        }
    }

    public void close() {
        if (this.callback == null) {
            closed = true;
        } else {
            this.callback.onExit(0);
        }
    }
}
