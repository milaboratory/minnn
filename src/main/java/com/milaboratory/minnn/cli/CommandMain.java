package com.milaboratory.minnn.cli;


import com.milaboratory.cli.ABaseCommand;
import picocli.CommandLine;

@CommandLine.Command(
        name = "main",
        versionProvider = CommandMain.VersionProvider.class,
        separator = " "
)
public class CommandMain extends ABaseCommand {
    private static String[] versionInfo = null;
    @CommandLine.Option(
            names = {"-v", "--version"},
            versionHelp = true,
            description = {"print version information and exit"}
    )
    boolean versionRequested;

    public CommandMain(String appName) {
        super(appName);
    }

    public static void init(String[] versionInfoArg) {
        versionInfo = versionInfoArg;
    }

    public static final class VersionProvider implements CommandLine.IVersionProvider {
        public VersionProvider() {
        }

        public String[] getVersion() {
            if (CommandMain.versionInfo == null) {
                throw new RuntimeException("getVersion() called while versionInfo is not initialized!");
            } else {
                return CommandMain.versionInfo;
            }
        }
    }
}
