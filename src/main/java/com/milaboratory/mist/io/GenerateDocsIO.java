package com.milaboratory.mist.io;

import com.beust.jcommander.*;

import java.io.*;
import java.lang.reflect.Field;
import java.util.*;

import static com.milaboratory.mist.util.SystemUtils.*;

public final class GenerateDocsIO {
    private final List<Class> parameterClasses = new ArrayList<>();
    private final String outputFileName;

    public GenerateDocsIO(String outputFileName) {
        for (String actionName : new String[] { "Extract", "Filter", "Demultiplex", "MifToFastq", "Correct", "Sort",
                "Consensus", "StatGroups", "StatPositions", "Report" }) {
            try {
                parameterClasses.add(Class.forName("com.milaboratory.mist.cli." + actionName + "Action." + actionName
                        + "ActionParameters"));
            } catch (ClassNotFoundException e) {
                throw exitWithError(e.getMessage());
            }
        }
        this.outputFileName = outputFileName;
    }

    public void go() {
        try (PrintStream writer = new PrintStream(new FileOutputStream(outputFileName))) {
            for (Class parameterClass : parameterClasses) {
                writer.println(parameterClass.getName());
                writer.println(parameterClass.getAnnotation(Parameters.class));
                for (Field field : parameterClass.getDeclaredFields()) {
                    writer.println(field.getName());
                    writer.println(field.getAnnotation(Parameter.class));
                }
            }
        } catch (IOException e) {
            throw exitWithError(e.getMessage());
        }
    }
}
