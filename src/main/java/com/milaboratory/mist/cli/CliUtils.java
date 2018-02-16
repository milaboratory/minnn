package com.milaboratory.mist.cli;

import com.beust.jcommander.ParameterException;

import static com.milaboratory.core.sequence.SequenceQuality.MAX_QUALITY_VALUE;

final class CliUtils {
    static void validateQuality(int quality) throws ParameterException {
        if ((quality < 0) || (quality > MAX_QUALITY_VALUE))
            throw new ParameterException(quality + " is invalid value for quality! Valid values are from 0 to "
                    + MAX_QUALITY_VALUE + ".");
    }
}
