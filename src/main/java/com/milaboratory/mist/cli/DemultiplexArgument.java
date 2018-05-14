package com.milaboratory.mist.cli;

public final class DemultiplexArgument {
    public final boolean isBarcode;    // true if this is barcode, false if this is sample file name
    public final String argument;

    DemultiplexArgument(boolean isBarcode, String argument) {
        this.isBarcode = isBarcode;
        this.argument = argument;
    }
}
