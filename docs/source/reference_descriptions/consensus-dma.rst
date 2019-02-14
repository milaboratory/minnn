Consensus action is used to calculate consensus sequences for all combinations of barcode values. It also allows to
find multiple consensuses in the same combination of barcodes if there are multiple sequences with the same barcodes
in the data. Consensus action uses multi-sequence alignment to put multiple sequences to the same coordinate system.
Then it calculate consensus letter (or deletion) for each position. Consensus action works in 2 stages: first it aligns
the cluster (group of sequences with same barcodes) to the best sequence from this cluster; best sequence is determined
by length and quality. On 2nd stage, it aligns all sequences from this cluster to the consensus from 1st stage.
After both stages, quality trimming is performed on both sides of the consensus, and if the resulting sequence is too
short, this consensus is discarded. Sequences that have low alignment score with the best sequence are not included in
consensus calculation. But if there are many remaining sequences (both in case that consensus was calculated and in
case it was discarded), next consensus calculation will start with the cluster of remaining sequences.

**Important:** :ref:`sort` action must be used before consensus action with the same groups in :code:`--groups`
argument as in consensus action, otherwise the results of consensus action will be wrong!

:code:`--groups` argument with space separated list of groups can be specified in consensus action to specify which
groups will be used for consensus calculation. If :code:`--groups` argument is missing, all groups from input file
(except built-in groups :code:`R1`, :code:`R2` etc) will be used. :code:`--input` and :code:`--output` arguments are
optional, and if they are missing, stdin and stdout will be used instead of input and output files.

Examples for consensus action:

.. code-block:: text

   minnn consensus --groups SB1 SB2 --input sorted.mif --output consensus.mif --score-threshold -750
   xzcat sorted.mif.xz | minnn consensus --output consensus.mif
   minnn consensus --max-consensuses-per-cluster 1 --consensuses-to-separate-groups --input data.mif --output result.mif

:code:`--score-threshold` is very important option: it is alignment score threshold by which it will be determined
to include sequence to the consensus or don't include. Too low threshold score can result in including garbage data
to consensus or melding multiple molecules to one consensus. Too high threshold score can result in splitting one
molecule to multiple consensuses, and in leaving out sequences with valuable data. Score values for single matches,
mismatches and indels in alignment can be set with :code:`--aligner-match-score`, :code:`--aligner-mismatch-score` and
:code:`--aligner-gap-score` arguments.

Consensus action uses banded aligner, it reduces size of the matrix to increase speed and reduce memory usage.
:code:`--width` option allows to specify width of the banded matrix. Lower values mean faster consensus calculation
and lower memory usage, but less accuracy. It's recommended to set :code:`--width` to maximum allowed length of
single insertion or deletion, multiplied by 1.5.

:code:`--max-consensuses-per-cluster` is important option: it allows to set maximum number of consensuses for
one combination of barcode values. If each molecule is marked by unique set of barcodes, you can set
:code:`--max-consensuses-per-cluster` to :code:`1`.

If enough sequences from cluster were not used in consensus calculations, and :code:`--max-consensuses-per-cluster` is
not exceeded, then new consensus calculation will start from remaining sequences. You can set the threshold with
:code:`--skipped-fraction-to-repeat` option. If number of remaining sequences divided to cluster size is not below
this threshold, new consensus calculation will start.

Calculated consensuses are processed with quality trimmer to remove low quality tails on the left and right sides.
You can set trim window size (length of subsequence for which average quality is calculated) with
:code:`--trim-window-size` option. Quality threshold is set by :code:`--avg-quality-threshold` option, and minimal
allowed consensus length after trimming - by :code:`--min-good-sequence-length` option. If resulting consensus will be
shorter, it will be discarded.

Input reads are prepared with the same quality trimmer before calculating consensus, but trimming parameters are
configured separately. Use :code:`--reads-trim-window-size` to set trim window size for input reads,
:code:`--reads-avg-quality-threshold` for quality threshold and :code:`--reads-min-good-sequence-length` for minimum
input read length after trimming. Too short reads will not be included in consensus calculation.

:code:`--consensuses-to-separate-groups` parameter changes consensus action behavior significantly. If this parameter
is not specified, output file will contain calculated consensuses. If it is specified, original sequences will be
written to the output files, consensuses will be written as capture groups :code:`CR1`, :code:`CR2` etc, so it will be
possible to cluster original reads by consensuses using :ref:`filter` / :ref:`demultiplex` actions, or export original
reads and corresponding consensuses into separate reads using :ref:`mif2fastq` action. Note that input file must
not contain any groups named :code:`CR1`, :code:`CR2` etc if you use :code:`--consensuses-to-separate-groups`
parameter.

Command line arguments reference for consensus action:
