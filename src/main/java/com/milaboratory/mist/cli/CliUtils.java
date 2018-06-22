package com.milaboratory.mist.cli;

import com.beust.jcommander.ParameterException;

import java.text.DecimalFormat;

import static com.milaboratory.core.sequence.SequenceQuality.MAX_QUALITY_VALUE;

public final class CliUtils {
    public final static DecimalFormat floatFormat = new DecimalFormat("#.##");

    static void validateQuality(int quality) throws ParameterException {
        if ((quality < 0) || (quality > MAX_QUALITY_VALUE))
            throw new ParameterException(quality + " is invalid value for quality! Valid values are from 0 to "
                    + MAX_QUALITY_VALUE + ".");
    }
}
