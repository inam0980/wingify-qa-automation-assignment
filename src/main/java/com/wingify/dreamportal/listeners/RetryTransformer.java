package com.wingify.dreamportal.listeners;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import org.testng.IAnnotationTransformer;
import org.testng.IRetryAnalyzer;
import org.testng.annotations.ITestAnnotation;
import org.testng.internal.annotations.DisabledRetryAnalyzer;

/**
 * Applies {@link RetryAnalyzer} to every test, so no test method has to remember
 * {@code @Test(retryAnalyzer = ...)}.
 *
 * <p>A test that names its own analyzer keeps it; only the TestNG default is replaced.
 */
public class RetryTransformer implements IAnnotationTransformer {

    // TestNG declares this method with raw Class/Constructor/Method parameters. Using the
    // generic forms compiles but does not override the interface method, so the transformer
    // is silently never called.
    @Override
    @SuppressWarnings("rawtypes")
    public void transform(ITestAnnotation annotation, Class testClass,
                          Constructor testConstructor, Method testMethod) {
        Class<? extends IRetryAnalyzer> declared = annotation.getRetryAnalyzerClass();
        if (declared == null || declared == DisabledRetryAnalyzer.class) {
            annotation.setRetryAnalyzer(RetryAnalyzer.class);
        }
    }
}
