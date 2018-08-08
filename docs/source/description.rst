===========
Description
===========

Mist is a toolset to process genetic data from sequencing machines and assemble sequenced molecules from raw FASTQ data.
Consensus assembly in mist consists of the following stages:

1. Extract barcodes from raw sequences.
2. Correct mismatches and indels in barcodes.
3. Sort sequences by barcode values to group them for further consensus assembly.
4. Assembly consensuses for each barcode. There can be one or many consensuses for each barcode, depending on the way
   of obtaining original data.
5. Export calculated consensuses to FASTQ format.

Also mist has some other functions:

* Filter original data by barcode values.
* Filter calculated consensuses by quantity of reads from which they were assembled.
* Split (demultiplex) data into separate files by barcode values.
* Collect statistics from data by barcode values and by barcode positions in sequences.

===========
Usage Chart
===========

.. image:: _static/usage-chart.svg
    :width: 100%
