package com.milaboratory.mist.cli;

import com.milaboratory.cli.JCommanderBasedMain;
import com.milaboratory.util.VersionInfo;
import sun.misc.Signal;

import static com.milaboratory.mist.util.SystemUtils.exitWithError;

public final class Main {
    public static void main(String[] args) throws Exception {
        Signal.handle(new Signal("PIPE"), signal -> exitWithError("Broken pipe!"));

        JCommanderBasedMain jCommanderBasedMain = new JCommanderBasedMain("mist",
                new ExtractAction(),
                new ReportAction(),
                new FilterAction(),
                new DemultiplexAction(),
                new StatGroupsAction(),
                new StatPositionsAction(),
                new SortAction(),
                new CorrectAction(),
                new ConsensusAction(),
                new MifToFastqAction());

        jCommanderBasedMain.setVersionInfoCallback(() -> {
            VersionInfo milibVersionInfo = VersionInfo.getVersionInfoForArtifact("milib");
            VersionInfo mistVersionInfo = VersionInfo.getVersionInfoForArtifact("mist");

            System.out.println(
                    "MiST v" + mistVersionInfo.getVersion() +
                    " (built " + mistVersionInfo.getTimestamp() +
                    "; rev=" + mistVersionInfo.getRevision() +
                    "; branch=" + milibVersionInfo.getBranch() +
                    "; host=" + milibVersionInfo.getHost() +
                    ")\n" +
                    "MiLib v" + milibVersionInfo.getVersion() +
                    " (rev=" + milibVersionInfo.getRevision() +
                    "; branch=" + milibVersionInfo.getBranch() +
                    ")");
        });

        jCommanderBasedMain.main(args);
    }
}
