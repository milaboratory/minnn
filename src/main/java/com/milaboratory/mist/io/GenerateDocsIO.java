package com.milaboratory.mist.io;

import com.beust.jcommander.*;

import java.io.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

import static com.milaboratory.mist.util.SystemUtils.*;

public final class GenerateDocsIO {
    private final List<Class> parameterClasses = new ArrayList<>();
    private final String outputFileName;

    public GenerateDocsIO(String outputFileName) {
        for (String actionName : new String[] { "Extract", "Filter", "Demultiplex", "MifToFastq", "Correct", "Sort",
                "Consensus", "StatGroups", "StatPositions", "Report" }) {
            try {
                parameterClasses.add(Class.forName("com.milaboratory.mist.cli." + actionName + "Action$" + actionName
                        + "ActionParameters"));
            } catch (ClassNotFoundException e) {
                throw exitWithError(e.toString());
            }
        }
        this.outputFileName = outputFileName;
    }

    public void go() {
        try (PrintStream writer = new PrintStream(new FileOutputStream(outputFileName))) {
            for (Class parameterClass : parameterClasses) {
                writer.println(parameterClass.getName());
                writer.println(getAnnotationValue(parameterClass.getAnnotation(Parameters.class),
                        "commandDescription") + "\n");
                for (Field field : parameterClass.getDeclaredFields()) {
                    writer.println(getAnnotationValue(field.getAnnotation(Parameter.class), "names"));
                    writer.println(getAnnotationValue(field.getAnnotation(Parameter.class),
                            "description") + "\n");
                }
            }
        } catch (IOException e) {
            throw exitWithError(e.getMessage());
        }
    }

    private String getAnnotationValue(Annotation annotation, String parameterName) {
        try {
            for (Method method : annotation.annotationType().getDeclaredMethods())
                if (method.getName().equals(parameterName)) {
                    Object value = method.invoke(annotation, (Object[])null);
                    return value.getClass().isArray() ? Arrays.toString((Object[])value) : value.toString();
                }
        } catch (InvocationTargetException | IllegalAccessException e) {
            throw exitWithError(e.toString());
        }
        throw exitWithError("Parameter " + parameterName + " not found in annotation " + annotation);
    }
}
