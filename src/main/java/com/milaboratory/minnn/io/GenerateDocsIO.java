/*
 * Copyright (c) 2016-2020, MiLaboratory LLC
 * All Rights Reserved
 *
 * Permission to use, copy, modify and distribute any part of this program for
 * educational, research and non-profit purposes, by non-profit institutions
 * only, without fee, and without a written agreement is hereby granted,
 * provided that the above copyright notice, this paragraph and the following
 * three paragraphs appear in all copies.
 *
 * Those desiring to incorporate this work into commercial products or use for
 * commercial purposes should contact MiLaboratory LLC, which owns exclusive
 * rights for distribution of this program for commercial purposes, using the
 * following email address: licensing@milaboratory.com.
 *
 * IN NO EVENT SHALL THE INVENTORS BE LIABLE TO ANY PARTY FOR DIRECT, INDIRECT,
 * SPECIAL, INCIDENTAL, OR CONSEQUENTIAL DAMAGES, INCLUDING LOST PROFITS,
 * ARISING OUT OF THE USE OF THIS SOFTWARE, EVEN IF THE INVENTORS HAS BEEN
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * THE SOFTWARE PROVIDED HEREIN IS ON AN "AS IS" BASIS, AND THE INVENTORS HAS
 * NO OBLIGATION TO PROVIDE MAINTENANCE, SUPPORT, UPDATES, ENHANCEMENTS, OR
 * MODIFICATIONS. THE INVENTORS MAKES NO REPRESENTATIONS AND EXTENDS NO
 * WARRANTIES OF ANY KIND, EITHER IMPLIED OR EXPRESS, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY OR FITNESS FOR A
 * PARTICULAR PURPOSE, OR THAT THE USE OF THE SOFTWARE WILL NOT INFRINGE ANY
 * PATENT, TRADEMARK OR OTHER RIGHTS.
 */
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

import static com.milaboratory.minnn.cli.ConsensusDoubleMultiAlignAction.CONSENSUS_DOUBLE_MULTI_ALIGN_ACTION_NAME;
import static com.milaboratory.minnn.cli.ConsensusSingleCellAction.CONSENSUS_SINGLE_CELL_ACTION_NAME;
import static com.milaboratory.minnn.cli.CorrectAction.CORRECT_ACTION_NAME;
import static com.milaboratory.minnn.cli.DecontaminateAction.DECONTAMINATE_ACTION_NAME;
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
    private static final List<String> commandsWithParameters = Collections.singletonList(DEMULTIPLEX_ACTION_NAME);
    private static final HashMap<String, List<String>> specificOptions = new HashMap<>();
    static {
        List<String> commandsWithForceOverwrite = Arrays.asList(EXTRACT_ACTION_NAME, FILTER_ACTION_NAME,
                DEMULTIPLEX_ACTION_NAME, MIF_TO_FASTQ_ACTION_NAME, CORRECT_ACTION_NAME, SORT_ACTION_NAME,
                CONSENSUS_SINGLE_CELL_ACTION_NAME, CONSENSUS_DOUBLE_MULTI_ALIGN_ACTION_NAME, STAT_GROUPS_ACTION_NAME,
                STAT_POSITIONS_ACTION_NAME, DECONTAMINATE_ACTION_NAME);
        List<String> commandsWithQuietOption = Arrays.asList(CORRECT_ACTION_NAME, SORT_ACTION_NAME,
                CONSENSUS_SINGLE_CELL_ACTION_NAME, CONSENSUS_DOUBLE_MULTI_ALIGN_ACTION_NAME);
        specificOptions.put("--force-overwrite", commandsWithForceOverwrite);
        specificOptions.put("--no-warnings", commandsWithQuietOption);
    }
    private final List<Class> actionClasses = new ArrayList<>();
    private final String outputFileName;

    public GenerateDocsIO(String outputFileName) {
        for (String actionName : new String[] { "Extract", "Filter", "Demultiplex", "MifToFastq", "Correct", "Sort",
                "ConsensusSingleCell", "ConsensusDoubleMultiAlign", "StatGroups", "StatPositions", "Decontaminate",
                "Report" }) {
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
                for (Field field : getAllDeclaredFields(actionClass)) {
                    FieldType fieldType = getFieldType(field);
                    if ((fieldType != UNKNOWN) && !isHidden(field)) {
                        if (fieldType == PARAMETERS) {
                            if (commandsWithParameters.contains(actionName))
                                parameterDescriptions.add(" " + getAnnotationStringParameter(field,
                                        "description"));
                        } else {
                            ArrayList<String> names = getOptionNames(field);
                            String description = getAnnotationStringParameter(field, "description");
                            if (names.stream().noneMatch(skippedOptions::contains)) {
                                if (names.stream().anyMatch(specificOptions.keySet()::contains))
                                    for (HashMap.Entry<String, List<String>> specificOption
                                            : specificOptions.entrySet()) {
                                        if (names.stream().anyMatch(specificOption.getKey()::contains)
                                                && specificOption.getValue().contains(actionName)) {
                                            parameterDescriptionsLast.add(" " + String.join(", ", names)
                                                    + ": " + description);
                                        }
                                    }
                                else
                                    parameterDescriptions.add(" " + String.join(", ", names) + ": "
                                            + description);
                            }
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

    private static ArrayList<String> getOptionNames(AnnotatedElement annotatedElement) {
        Object value = getAnnotationValueObject(annotatedElement, "names", Stream.of(Option.class));
        ArrayList<String> optionNames = new ArrayList<>();
        if (value.getClass().isArray()) {
            for (Object name : (Object[])value)
                optionNames.add(name.toString());
        } else
            optionNames.add(value.toString());
        return optionNames;
    }

    private static String getAnnotationStringParameter(AnnotatedElement annotatedElement, String parameterName) {
        Object value = getAnnotationValueObject(annotatedElement, parameterName,
                Stream.of(Command.class, Option.class, Parameters.class));
        if (value instanceof RuntimeException)
            throw exitWithError(value.toString());
        else {
            if (value.getClass().isArray())
                return (String)(((Object[])value)[0]);
            else
                return (String)value;
        }
    }

    private static FieldType getFieldType(AnnotatedElement annotatedElement) {
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

    private static Object getAnnotationValueObject(
            AnnotatedElement annotatedElement, String parameterName,
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

    private static List<Field> getAllDeclaredFields(Class actionClass) {
        List<Field> allDeclaredFields = new ArrayList<>(Arrays.asList(actionClass.getDeclaredFields()));
        Class superClass = actionClass.getSuperclass();
        while (superClass != null) {
            allDeclaredFields.addAll(Arrays.asList(superClass.getDeclaredFields()));
            superClass = superClass.getSuperclass();
        }
        return allDeclaredFields;
    }

    private static String title(String str, boolean topLevel) {
        String line = Stream.generate(() -> "=").limit(str.length()).collect(Collectors.joining());
        return (topLevel ? "" : line + "\n") + str + "\n" + line;
    }

    private static String subtitle(String str) {
        return ".. _" + str + ":\n\n" + str + "\n"
                + Stream.generate(() -> "-").limit(str.length()).collect(Collectors.joining());
    }

    enum FieldType {
        OPTION, PARAMETERS, UNKNOWN
    }
}
