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
    private static final HashMap<String, String> replaceTable = new HashMap<>();
    static {
        replaceTable.put("filter_options\n        Filter Options:      ", " Filter Options: ");
        replaceTable.put("group_options\n        Group Options:          ", " Group Options: ");
    }
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
            writer.println(title("Reference", true));
            writer.println(title("Command Line Syntax", false));
            writer.println("\n.. include:: reference_descriptions/header.rst\n");
            for (Class parameterClass : parameterClasses) {
                String actionName = getActionName(parameterClass);
                writer.println(subtitle(actionName));
                writer.println(".. include:: reference_descriptions/" + actionName + ".rst\n\n"
                        + ".. code-block:: text\n");
                TreeSet<OrderedParameter> parameters = new TreeSet<>();
                int i = 0;
                for (Field field : parameterClass.getDeclaredFields()) {
                    final int secondaryOrder = i++;
                    if (!isHidden(field)) {
                        String names = getAnnotationValue(field, "names");
                        String description = getAnnotationValue(field, "description");
                        int primaryOrder = getOrder(field);
                        if (names.length() > 2) {
                            names = names.substring(1, names.length() - 1);
                            parameters.add(new OrderedParameter(" " + names + ": " + description,
                                    primaryOrder, secondaryOrder));
                        } else
                            replaceTable.keySet().stream().filter(description::contains).findFirst()
                                    .ifPresent(s -> parameters.add(new OrderedParameter(description
                                            .replace(s, replaceTable.get(s)) + "\n", primaryOrder, secondaryOrder)));
                    }
                }
                parameters.forEach(p -> writer.println(p.text));
                writer.println();
            }
        } catch (IOException e) {
            throw exitWithError(e.toString());
        }
    }

    private String getAnnotationValue(AnnotatedElement annotatedElement, String parameterName) {
        Object value = getAnnotationValueObject(annotatedElement, parameterName,
                Stream.of(Parameter.class, DynamicParameter.class));
        if (value instanceof RuntimeException)
            throw exitWithError(((RuntimeException)value).getMessage());
        else
            return value.getClass().isArray() ? Arrays.toString((Object[])value) : value.toString();
    }

    private int getOrder(AnnotatedElement annotatedElement) {
        Object value = getAnnotationValueObject(annotatedElement, "order",
                Stream.of(Parameter.class));
        int order = (value instanceof RuntimeException) ? Integer.MAX_VALUE : (int)value;
        return (order == -1) ? Integer.MAX_VALUE - 1 : order;
    }

    private boolean isHidden(AnnotatedElement annotatedElement) {
        Object value = getAnnotationValueObject(annotatedElement, "hidden",
                Stream.of(Parameter.class));
        return !(value instanceof RuntimeException) && (boolean)value;
    }

    private Object getAnnotationValueObject(AnnotatedElement annotatedElement, String parameterName,
                                            Stream<Class<? extends Annotation>> annotationClasses) {
        Annotation annotation = annotationClasses
                .map((Function<Class<? extends Annotation>, Annotation>)annotatedElement::getAnnotation)
                .filter(Objects::nonNull).findFirst().orElse(null);
        if (annotation == null)
            return new RuntimeException("Annotation for " + annotatedElement + " not found!");
        try {
            for (Method method : annotation.annotationType().getDeclaredMethods())
                if (method.getName().equals(parameterName))
                    return method.invoke(annotation, (Object[])null);
        } catch (InvocationTargetException | IllegalAccessException e) {
            throw exitWithError(e.toString());
        }
        return new RuntimeException("Parameter " + parameterName + " not found in annotation " + annotation);
    }

    private String getActionName(Class parameterClass) {
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

    private String title(String str, boolean topLevel) {
        String line = Stream.generate(() -> "=").limit(str.length()).collect(Collectors.joining());
        return (topLevel ? "" : line + "\n") + str + "\n" + line;
    }

    private String subtitle(String str) {
        return ".. _" + str + ":\n\n" + str + "\n"
                + Stream.generate(() -> "-").limit(str.length()).collect(Collectors.joining());
    }

    private class OrderedParameter implements Comparable<OrderedParameter> {
        final String text;
        final int primaryOrder;
        final int secondaryOrder;

        OrderedParameter(String text, int primaryOrder, int secondaryOrder) {
            this.text = text;
            this.primaryOrder = primaryOrder;
            this.secondaryOrder = secondaryOrder;
        }

        @Override
        public int compareTo(OrderedParameter other) {
            int firstCompare = Integer.compare(primaryOrder, other.primaryOrder);
            return (firstCompare != 0) ? firstCompare : Integer.compare(secondaryOrder, other.secondaryOrder);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            OrderedParameter that = (OrderedParameter)o;
            if (primaryOrder != that.primaryOrder) return false;
            if (secondaryOrder != that.secondaryOrder) return false;
            return text.equals(that.text);
        }

        @Override
        public int hashCode() {
            int result = text.hashCode();
            result = 31 * result + primaryOrder;
            result = 31 * result + secondaryOrder;
            return result;
        }
    }
}
