package com.github.tvbox.osc.util.js;

import androidx.annotation.Keep;

import com.github.tvbox.quickjs.JSMethod;
import com.orhanobut.hawk.Hawk;

public class local {
    @Keep
    @JSMethod
    public void delete(String str, String str2) {
        try {
            Hawk.delete("jsRuntime_" + str + "_" + str2);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Keep
    @JSMethod
    public String get(String str, String str2) {
        try {
            return Hawk.get("jsRuntime_" + str + "_" + str2, "");
        } catch (Exception e) {
            Hawk.delete(str);
            return str2;
        }
    }

    @Keep
    @JSMethod
    public void set(String str, String str2, String str3) {
        try {
            Hawk.put("jsRuntime_" + str + "_" + str2, str3);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
