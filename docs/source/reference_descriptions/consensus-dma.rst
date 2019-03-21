.. include:: reference_descriptions/consensus-header.rst

Consensus algorithm "Double multi-align" (:code:`consensus-dma` action) uses multi-sequence alignment to put multiple
sequences to the same coordinate system. Then it calculate consensus letter (or deletion) for each position.
:code:`consensus-dma` action works in 2 stages: first it aligns the cluster (group of sequences with same barcodes) to
the best sequence from this cluster; best sequence is determined by length and quality. On 2nd stage, it aligns all
sequences from this cluster to the consensus from 1st stage. After both stages, quality trimming is performed on both
sides of the consensus, and if the resulting sequence is too short, this consensus is discarded. Sequences that have
low alignment score with the best sequence are not included in consensus calculation. But if there are many remaining
sequences (both in case that consensus was calculated and in case it was discarded), next consensus calculation will
start with the cluster of remaining sequences.

:code:`--groups` argument with space separated list of groups must be specified in :code:`consensus-dma` action to
specify which groups will be used for consensus calculation. This argument is mandatory. :code:`--input` and
:code:`--output` arguments are optional, and if they are missing, stdin and stdout will be used instead of input and
output files.

Examples for :code:`consensus-dma` action:

.. code-block:: text

   minnn consensus-dma --groups SB1 SB2 --input sorted.mif --output consensus.mif --score-threshold -750
   xzcat sorted.mif.xz | minnn consensus-dma --max-consensuses-per-cluster 1 --output consensus.mif
   minnn consensus-dma --consensuses-to-separate-groups --width 10 --input data.mif --output result.mif

:code:`--score-threshold` is very important option: it is alignment score threshold by which it will be determined
to include sequence to the consensus or don't include. Too low threshold score can result in including garbage data
to consensus or melding multiple molecules to one consensus. Too high threshold score can result in splitting one
molecule to multiple consensuses, and in leaving out sequences with valuable data. Score values for single matches,
mismatches and indels in alignment can be set with :code:`--aligner-match-score`, :code:`--aligner-mismatch-score` and
:code:`--aligner-gap-score` arguments.

:code:`consensus-dma` action uses banded aligner, it reduces size of the matrix to increase speed and reduce memory
usage. :code:`--width` option allows to specify width of the banded matrix. Lower values mean faster consensus
calculation and lower memory usage, but less accuracy. It's recommended to set :code:`--width` to maximum allowed
length of single insertion or deletion, multiplied by 1.5.

.. include:: reference_descriptions/consensus-common-arguments.rst

:code:`--original-read-stats` parameter allows to write consensus calculation stats for each original read into
separate file. This is text file in space separated format, and it contains the following information for each read:

#. Original read ID: number of read, starting from 0, in the original FASTQ data that was the input of :ref:`extract`
   action.
#. Consensus ID (number of consensus in the output) or -1 if this read was discarded.
#. Read status. Possible values are:

   * :code:`NOT_MATCHED` - read was not matched in :ref:`extract` action,
   * :code:`READ_DISCARDED_TRIM` - read was discarded by length after quality trimming,
   * :code:`NOT_USED_IN_CONSENSUS` - read was discarded and not used in any consensus calculation,
   * :code:`USED_IN_CONSENSUS` - read used in consensus,
   * :code:`CONSENSUS_DISCARDED_TRIM_STAGE1` - consensus was calculated and discarded after quality trimming on the
     1st stage,
   * :code:`CONSENSUS_DISCARDED_TRIM_STAGE2` - consensus was calculated and discarded after quality trimming on the
     2nd stage.

#. ID of the best read of this consensus that was determined on stage 1, or -1 if this read was not used in consensus.
#. Number of reads in this consensus, or 0 if this read was not used in consensus.
#. Sequences and qualities for each target of this read.
#. Sequences and qualities for each target of this consensus, or "-" sign if this read was not used in consensus.
#. Alignment scores for 1st and 2nd stages of consensus calculation, or large negative value if this read was not
   used in consensus.

Command line arguments reference for consensus-dma action:
