.. include:: reference_descriptions/consensus-header.rst

Consensus action aligns multiple sequences by searching the most frequent K-mer in all of these sequences and then
placing all of these sequences to the same coordinate system by offset of the most frequent K-mer in each of
the sequences. Sequences where the most frequent K-mer (with allowed number of errors specified by
:code:`--kmer-max-errors` argument) is not found are marked as remaining sequences and not included in current
consensus calculation. Then consensus action calculates consensus from the current cluster of sequences that are
already positioned in the same coordinate system. After this, quality trimming is performed on both sides of the
consensus, and if the resulting sequence is too short, this consensus is discarded. But if there are many remaining
sequences (both in case that consensus was calculated and in case it was discarded), next consensus calculation will
start with the cluster of remaining sequences.

:code:`--groups` argument with space separated list of groups must be specified in consensus action to specify which
groups will be used for consensus calculation. This argument is mandatory. :code:`--input` and :code:`--output`
arguments are optional, and if they are missing, stdin and stdout will be used instead of input and output files.

Examples for consensus action:

.. code-block:: text

   minnn consensus --groups SB1 SB2 --input sorted.mif --output consensus.mif --kmer-max-errors 2
   xzcat sorted.mif.xz | minnn consensus --output consensus.mif
   minnn consensus --max-consensuses-per-cluster 1 --kmer-length 10 --input data.mif --output consensus.mif

:code:`--kmer-max-errors` argument specifies the maximal number of mismatches when the most frequent K-mer will be
still considered found. :code:`--kmer-length` argument allows to set K-mer length, and :code:`--kmer-offset` can be
used to set maximal allowed offset calculated from the middle of found K-mer to the middle of the sequence.

.. include:: reference_descriptions/consensus-common-arguments.rst

:code:`--original-read-stats` parameter allows to write consensus calculation stats for each original read into
separate file. This is text file in space separated format, and it contains the following information for each read:

#. Original read ID: number of read, starting from 0, in the original FASTQ data that was the input of :ref:`extract`
   action.
#. Consensus ID (number of consensus in the output) or -1 if this read was discarded.
#. Read status. Possible values are:

   * :code:`NOT_MATCHED` - read was not matched in :ref:`extract` action,
   * :code:`READ_DISCARDED_TRIM` - read was discarded by length after quality trimming,
   * :code:`KMERS_NOT_FOUND` - read was in group of reads that was fully discarded because there were no reads in the
     group that have the most frequent K-mer found for all targets (:code:`R1`, :code:`R2` etc),
   * :code:`NOT_USED_IN_CONSENSUS` - read was discarded and not used in any consensus calculation,
   * :code:`USED_IN_CONSENSUS` - read used in consensus,
   * :code:`CONSENSUS_DISCARDED_TRIM` - consensus was calculated and discarded after quality trimming.

#. Number of reads in this consensus, or 0 if this read was not used in consensus.
#. Sequences and qualities for each target of this read.
#. Sequences and qualities for each target of this consensus, or "-" sign if this read was not used in consensus.
#. Levenshtein distances between this read and this consensus, separate values for all targets
   (:code:`R1`, :code:`R2` etc), or -1 values if this read is not used in any consensus.
#. Number of nucleotides that were removed from this read on quality trimming, separate values for all targets
   (:code:`R1`, :code:`R2` etc).
#. Number of nucleotides that were removed from this consensus on quality trimming, separate values for all targets
   (:code:`R1`, :code:`R2` etc).

Command line arguments reference for consensus action:
