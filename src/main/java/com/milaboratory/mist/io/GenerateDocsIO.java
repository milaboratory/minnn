package com.milaboratory.mist.io;

import com.beust.jcommander.*;

import java.io.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
            writer.println(title("Command line syntax"));
            for (Class parameterClass : parameterClasses) {
                writer.println(subtitle(getCommandName(parameterClass)));
                writer.println(getAnnotationValue(parameterClass, "commandDescription") + "\n\n::\n");
                for (Field field : parameterClass.getDeclaredFields()) {
                    String names = getAnnotationValue(field, "names");
                    String description = stripQuotes(getAnnotationValue(field, "description"));
                    if (names.length() > 2) {
                        names = names.substring(1, names.length() - 1);
                        writer.println(" " + names + ": " + description);
                    } else {
                        writer.println(" " + description);
                    }
                }
                writer.println();
            }
        } catch (IOException e) {
            throw exitWithError(e.toString());
        }
    }

    private String getAnnotationValue(AnnotatedElement annotatedElement, String parameterName) {
        Annotation annotation = Stream.of(Parameters.class, Parameter.class, DynamicParameter.class)
                .map((Function<Class<? extends Annotation>, Annotation>)annotatedElement::getAnnotation)
                .filter(Objects::nonNull).findFirst().orElse(null);
        if (annotation == null)
            throw exitWithError("Annotation for " + annotatedElement + " not found!");
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

    private String title(String str) {
        String line = Stream.generate(() -> "=").limit(str.length()).collect(Collectors.joining());
        return line + "\n" + str + "\n" + line;
    }

    private String subtitle(String str) {
        return str + "\n" + Stream.generate(() -> "-").limit(str.length()).collect(Collectors.joining());
    }
}
