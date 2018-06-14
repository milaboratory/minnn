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
                writer.println(getCommandName(parameterClass));
                writer.println(getAnnotationValue(parameterClass.getAnnotation(Parameters.class),
                        "commandDescription") + "\n");
                for (Field field : parameterClass.getDeclaredFields()) {
                    String names = getAnnotationValue(field.getAnnotation(Parameter.class), "names");
                    String description = stripQuotes(getAnnotationValue(field.getAnnotation(Parameter.class),
                            "description"));
                    if (names.length() > 2) {
                        names = names.substring(1, names.length() - 1);
                        writer.println(names + ": " + description);
                    } else {
                        writer.println(description);
                    }
                }
            }
        } catch (IOException e) {
            throw exitWithError(e.toString());
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

    private String getCommandName(Class parameterClass) {
        Field nameField;
        try {
            nameField = parameterClass.getEnclosingClass().getField("commandName");
        } catch (NoSuchFieldException e) {
            throw exitWithError(e.toString());
        }
        try {
            return (String)nameField.get(null);
        } catch (IllegalAccessException e) {
            throw exitWithError(e.toString());
        }
    }

    private String stripQuotes(String str) {
        return str.replace("/(^\"|\')|(\"|\'$)/g", "");
    }
}
