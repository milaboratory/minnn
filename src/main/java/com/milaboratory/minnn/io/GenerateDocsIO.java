package com.milaboratory.minnn.io;

import picocli.CommandLine.*;

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

import static com.milaboratory.minnn.cli.ConsensusAction.CONSENSUS_ACTION_NAME;
import static com.milaboratory.minnn.cli.CorrectAction.CORRECT_ACTION_NAME;
import static com.milaboratory.minnn.cli.DemultiplexAction.DEMULTIPLEX_ACTION_NAME;
import static com.milaboratory.minnn.cli.ExtractAction.EXTRACT_ACTION_NAME;
import static com.milaboratory.minnn.cli.FilterAction.FILTER_ACTION_NAME;
import static com.milaboratory.minnn.cli.MifToFastqAction.MIF_TO_FASTQ_ACTION_NAME;
import static com.milaboratory.minnn.cli.SortAction.SORT_ACTION_NAME;
import static com.milaboratory.minnn.cli.StatGroupsAction.STAT_GROUPS_ACTION_NAME;
import static com.milaboratory.minnn.cli.StatPositionsAction.STAT_POSITIONS_ACTION_NAME;
import static com.milaboratory.minnn.io.GenerateDocsIO.FieldType.*;
import static com.milaboratory.minnn.util.SystemUtils.*;

public final class GenerateDocsIO {
    private static final List<String> skippedOptions = Arrays.asList("--help", "--verbose", "--force");
    private static final HashMap<String, List<String>> specificOptions = new HashMap<>();
    static {
        List<String> commandsWithForceOverwrite = Arrays.asList(EXTRACT_ACTION_NAME, FILTER_ACTION_NAME,
                DEMULTIPLEX_ACTION_NAME, MIF_TO_FASTQ_ACTION_NAME, CORRECT_ACTION_NAME, SORT_ACTION_NAME,
                CONSENSUS_ACTION_NAME, STAT_GROUPS_ACTION_NAME, STAT_POSITIONS_ACTION_NAME);
        List<String> commandsWithQuietOption = Arrays.asList(CORRECT_ACTION_NAME, SORT_ACTION_NAME,
                CONSENSUS_ACTION_NAME);
        specificOptions.put("--force-overwrite", commandsWithForceOverwrite);
        specificOptions.put("--no-warnings", commandsWithQuietOption);
    }
    private final List<Class> actionClasses = new ArrayList<>();
    private final String outputFileName;

    public GenerateDocsIO(String outputFileName) {
        for (String actionName : new String[] { "Extract", "Filter", "Demultiplex", "MifToFastq", "Correct", "Sort",
                "Consensus", "StatGroups", "StatPositions", "Report" }) {
            try {
                actionClasses.add(Class.forName("com.milaboratory.minnn.cli." + actionName + "Action"));
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
            for (Class actionClass : actionClasses) {
                String actionName = getAnnotationStringParameter(actionClass, "name");
                writer.println(subtitle(actionName));
                writer.println(".. include:: reference_descriptions/" + actionName + ".rst\n\n"
                        + ".. code-block:: text\n");
                ArrayList<String> parameterDescriptions = new ArrayList<>();
                ArrayList<String> parameterDescriptionsLast = new ArrayList<>();
                for (Field field : actionClass.getDeclaredFields()) {
                    FieldType fieldType = getFieldType(field);
                    if ((fieldType != UNKNOWN) && !isHidden(field)) {
                        ArrayList<String> names = getOptionNames(field);
                        String description = getAnnotationStringParameter(field, "description");
                        if (names.stream().noneMatch(skippedOptions::contains)) {
                            if (names.stream().anyMatch(specificOptions.keySet()::contains))
                                for (HashMap.Entry<String, List<String>> specificOption : specificOptions.entrySet()) {
                                    if (names.stream().anyMatch(specificOption.getKey()::contains)
                                            && specificOption.getValue().contains(actionName)) {
                                        parameterDescriptionsLast.add(" " + String.join(", ", names) + ": "
                                                + description);
                                    }
                                }
                            else
                                parameterDescriptions.add(" " + String.join(", ", names) + ": "
                                        + description);
                        }
                    }
                }
                parameterDescriptions.addAll(parameterDescriptionsLast);
                parameterDescriptions.forEach(writer::println);
                writer.println();
            }
        } catch (IOException e) {
            throw exitWithError(e.toString());
        }
    }

    private ArrayList<String> getOptionNames(AnnotatedElement annotatedElement) {
        Object value = getAnnotationValueObject(annotatedElement, "names", Stream.of(Option.class));
        ArrayList<String> optionNames = new ArrayList<>();
        if (value.getClass().isArray()) {
            for (Object name : (Object[])value)
                optionNames.add(name.toString());
        } else
            optionNames.add(value.toString());
        return optionNames;
    }

    private String getAnnotationStringParameter(AnnotatedElement annotatedElement, String parameterName) {
        Object value = getAnnotationValueObject(annotatedElement, parameterName,
                Stream.of(Command.class, Option.class, Parameters.class));
        if (value instanceof RuntimeException)
            throw exitWithError(value.toString());
        else
            return value.toString();
    }

    private FieldType getFieldType(AnnotatedElement annotatedElement) {
        Object value = getAnnotationValueObject(annotatedElement, "description",
                Stream.of(Option.class));
        if (!(value instanceof RuntimeException))
            return OPTION;
        value = getAnnotationValueObject(annotatedElement, "description", Stream.of(Parameters.class));
        if (!(value instanceof RuntimeException))
            return PARAMETERS;
        return UNKNOWN;
    }

    private boolean isHidden(AnnotatedElement annotatedElement) {
        Object value = getAnnotationValueObject(annotatedElement, "hidden",
                Stream.of(Command.class, Option.class, Parameters.class));
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

    private String title(String str, boolean topLevel) {
        String line = Stream.generate(() -> "=").limit(str.length()).collect(Collectors.joining());
        return (topLevel ? "" : line + "\n") + str + "\n" + line;
    }

    private String subtitle(String str) {
        return ".. _" + str + ":\n\n" + str + "\n"
                + Stream.generate(() -> "-").limit(str.length()).collect(Collectors.joining());
    }

    enum FieldType {
        OPTION, PARAMETERS, UNKNOWN
    }
}
