/*
 * Copyright (c) 2016-2018, MiLaboratory LLC
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
package com.milaboratory.minnn.cli;

import com.milaboratory.cli.*;
import com.milaboratory.util.TempFileManager;
import com.milaboratory.util.VersionInfo;
import picocli.CommandLine;
import picocli.CommandLine.*;
import picocli.CommandLine.Model.*;

import java.util.*;

import static com.milaboratory.minnn.cli.Defaults.APP_NAME;

public final class Main {
    private static boolean initialized = false;
    private static String[] versionInfo;

    public static void main(String... args) {
        handleParseResult(parseArgs(args).getParseResult(), args);
    }

    private static void handleParseResult(ParseResult parseResult, String[] args) {
        ExceptionHandler<Object> exHandler = new ExceptionHandler<>();
        exHandler.andExit(1);
        RunLast runLast = new RunLast() {
            @Override
            protected List<Object> handle(ParseResult parseResult) throws ExecutionException {
                List<CommandLine> parsedCommands = parseResult.asCommandLineList();
                CommandLine commandLine = parsedCommands.get(parsedCommands.size() - 1);
                Object command = commandLine.getCommand();
                if (command instanceof CommandSpec && ((CommandSpec)command).userObject() instanceof Runnable) {
                    try {
                        ((Runnable)((CommandSpec)command).userObject()).run();
                        return new ArrayList<>();
                    } catch (ParameterException | ExecutionException ex) {
                        throw ex;
                    } catch (Exception ex) {
                        throw new ExecutionException(commandLine,
                                "Error while running command (" + command + "): " + ex, ex);
                    }
                }
                return super.handle(parseResult);
            }
        };

        try {
            runLast.handleParseResult(parseResult);
        } catch (ParameterException ex) {
            exHandler.handleParseException(ex, args);
        } catch (ExecutionException ex) {
            exHandler.handleExecutionException(ex, parseResult);
        }
    }

    private static CommandLine mkCmd() {
        System.setProperty("picocli.usage.width", "100");

        // Getting command string if executed from script
        String command = System.getProperty(APP_NAME + ".command", "java -jar " + APP_NAME + ".jar");

        if (!initialized) {
            VersionInfo milibVersionInfo = VersionInfo.getVersionInfoForArtifact("milib");
            VersionInfo minnnVersionInfo = VersionInfo.getVersionInfoForArtifact(APP_NAME);
            // Checking whether we are running a snapshot version
            if (minnnVersionInfo.getVersion().contains("SNAPSHOT")) {
                // If so, enable asserts
                ClassLoader.getSystemClassLoader().setDefaultAssertionStatus(true);
            }

            TempFileManager.setPrefix(APP_NAME + "_");

            versionInfo = new String[] {
                    "MiNNN v" + minnnVersionInfo.getVersion() +
                            " (built " + minnnVersionInfo.getTimestamp() +
                            "; rev=" + minnnVersionInfo.getRevision() +
                            "; branch=" + milibVersionInfo.getBranch() +
                            "; host=" + milibVersionInfo.getHost() +
                            ")",
                    "MiLib v" + milibVersionInfo.getVersion() +
                            " (rev=" + milibVersionInfo.getRevision() +
                            "; branch=" + milibVersionInfo.getBranch() +
                            ")"
            };

            initialized = true;
        }

        CommandLine cmd = new CommandLine(new CommandMain(APP_NAME, versionInfo))
                .setCommandName(command)
                .addSubcommand("help", HelpCommand.class);

        cmd.setSeparator(" ");
        return cmd;
    }

    private static CommandLine parseArgs(String... args) {
        if (args.length == 0)
            args = new String[] {"help"};
        ExceptionHandler exHandler = new ExceptionHandler();
        exHandler.andExit(1);
        CommandLine cmd = mkCmd();
        try {
            cmd.parseArgs(args);
        } catch (ParameterException ex) {
            exHandler.handleParseException(ex, args);
        }
        return cmd;
    }

    private static class ExceptionHandler<R> extends DefaultExceptionHandler<R> {
        @Override
        public R handleParseException(ParameterException ex, String[] args) {
            if (ex instanceof ValidationException && !((ValidationException)ex).printHelp) {
                System.err.println(ex.getMessage());
                return returnResultOrExit(null);
            }
            return super.handleParseException(ex, args);
        }
    }





//    public static void main(String[] args) throws Exception {
//
//        Signal.handle(new Signal("PIPE"), signal -> exitWithError("Broken pipe!"));
//
//        JCommanderBasedMain jCommanderBasedMain = new JCommanderBasedMain("minnn",
//                new ExtractAction(),
//                new ReportAction(),
//                new FilterAction(),
//                new DemultiplexAction(),
//                new StatGroupsAction(),
//                new StatPositionsAction(),
//                new SortAction(),
//                new CorrectAction(),
//                new ConsensusAction(),
//                new MifToFastqAction(),
//                new GenerateDocsAction());
//
//        jCommanderBasedMain.setVersionInfoCallback(() -> {
//            VersionInfo milibVersionInfo = VersionInfo.getVersionInfoForArtifact("milib");
//            VersionInfo minnnVersionInfo = VersionInfo.getVersionInfoForArtifact("minnn");
//
//            System.out.println(
//                    "MiNNN v" + minnnVersionInfo.getVersion() +
//                    " (built " + minnnVersionInfo.getTimestamp() +
//                    "; rev=" + minnnVersionInfo.getRevision() +
//                    "; branch=" + milibVersionInfo.getBranch() +
//                    "; host=" + milibVersionInfo.getHost() +
//                    ")\n" +
//                    "MiLib v" + milibVersionInfo.getVersion() +
//                    " (rev=" + milibVersionInfo.getRevision() +
//                    "; branch=" + milibVersionInfo.getBranch() +
//                    ")");
//        });
//
//        jCommanderBasedMain.main(args);
//    }
}
