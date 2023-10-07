package com.github.tvbox.quickjs;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The annotation QUJS function is for auto-registering JS function interception.
 *
 * @since 0.8.1
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Function {
    /**
     * JS function name.
     *
     * @return the name of the JS function to be injected
     * @since 0.8.1
     */
    String name() default "";

}
