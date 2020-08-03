===========
Description
===========

Minnn is a toolset to process genetic data from sequencing machines and assemble sequenced molecules from raw FASTQ
data. Consensus assembly in minnn consists of the following stages:

#. Extract barcodes from raw sequences.
#. Sort sequences by barcode values to group them for further correction.
#. Correct mismatches and indels in barcodes.
#. Sort sequences by barcode values to group them for further consensus assembly.
#. Assembly consensuses for each barcode. There can be one or many consensuses for each barcode, depending on the way
   of obtaining original data.
#. Export calculated consensuses to FASTQ format.

Also minnn has some other functions:

* Filter original data by barcode values.
* Filter calculated consensuses by quantity of reads from which they were assembled.
* Filter barcode values by their count.
* Decontaminate: remove barcodes from one cell that appear in samples from another cell.
* Split (demultiplex) data into separate files by barcode values.
* Collect statistics from data by barcode values and by barcode positions in sequences.

Minnn is free for academic and non-profit use (see :ref:`license`).

===========
Usage Chart
===========

.. image:: _static/usage-chart.svg
    :width: 100%
