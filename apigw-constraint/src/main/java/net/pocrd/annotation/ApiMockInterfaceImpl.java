package net.pocrd.annotation;

import net.pocrd.define.MockApiImplementation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by rendong on 15/7/14.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ApiMockInterfaceImpl {
    Class<? extends MockApiImplementation> value();
}
