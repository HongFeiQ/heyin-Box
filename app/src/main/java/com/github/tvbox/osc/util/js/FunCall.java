package com.github.tvbox.osc.util.js;

import com.github.tvbox.quickjs.JSCallFunction;
import com.github.tvbox.quickjs.JSFunction;
import com.github.tvbox.quickjs.JSObject;

import java.util.concurrent.Callable;

public class FunCall implements Callable<Object> {

    private final JSObject jsObject;
    private final Object[] args;
    private final String name;
    private Object result;

    public static FunCall call(JSObject jsObject, String name, Object... args) {
        return new FunCall(jsObject, name, args);
    }

    private FunCall(JSObject jsObject, String name, Object... args) {
        this.jsObject = jsObject;
        this.name = name;
        this.args = args;
    }

    @Override
    public Object call() throws Exception {
        result = jsObject.getJSFunction(name).call(args);
        if (result instanceof JSObject) {
            JSFunction then = ((JSObject) result).getJSFunction("then");
            if (then != null) then.call(jsCallFunction);
        }
        return result;
    }

    private final JSCallFunction jsCallFunction = new JSCallFunction() {
        @Override
        public Object call(Object... args) {
            return result = args[0];
        }
    };
}
