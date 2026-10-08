package kamkeel.npcdbc.compat;

import org.apache.logging.log4j.Logger;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

/**
 * Static method of an optional mod, looked up once through reflection. Missing
 * class or method leaves it off; a call that throws turns it off for good and
 * logs once. Callers do {@code MethodHandle h = m.get(); if (h != null) ... invokeExact}.
 */
final class SoftMethod {

    private final Logger log;
    private final String owner;
    private final String name;
    private final MethodType type;
    private MethodHandle handle;
    private boolean lookedUp;

    SoftMethod(Logger log, String owner, String name, MethodType type) {
        this.log = log;
        this.owner = owner;
        this.name = name;
        this.type = type;
    }

    MethodHandle get() {
        if (!lookedUp) {
            lookedUp = true;
            try {
                handle = MethodHandles.publicLookup().findStatic(Class.forName(owner), name, type);
            } catch (ClassNotFoundException e) {
                log.info("{} not present: {} is off", owner, name);
            } catch (ReflectiveOperationException e) {
                log.warn("{} has no {}{}: that feature is off", owner, name, type);
            }
        }
        return handle;
    }

    void disable(Throwable t) {
        log.error(owner + "." + name + " failed, turning it off", t);
        handle = null;
    }
}
