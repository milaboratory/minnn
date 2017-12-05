package com.milaboratory.mist.cli;

import com.beust.jcommander.Parameters;
import com.milaboratory.cli.Action;
import com.milaboratory.cli.ActionHelper;
import com.milaboratory.cli.ActionParameters;

public final class FilterAction implements Action {
    private final FilterActionParameters filterActionParameters = new FilterActionParameters();

    @Override
    public void go(ActionHelper helper) {

    }

    @Override
    public String command() {
        return "filter";
    }

    @Override
    public ActionParameters params() {
        return filterActionParameters;
    }

    @Parameters(commandDescription =
            "Filter target nucleotide sequences, pass only sequences matching the query.")
    private static final class FilterActionParameters extends ActionParameters {

    }
}
