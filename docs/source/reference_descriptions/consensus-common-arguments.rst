:code:`--max-consensuses-per-cluster` is important option: it allows to set maximal number of consensuses for
one combination of barcode values. If each molecule is marked by unique set of barcodes, you can set
:code:`--max-consensuses-per-cluster` to :code:`1`.

If enough sequences from cluster were not used in consensus calculations, and :code:`--max-consensuses-per-cluster` is
not exceeded, then new consensus calculation will start from remaining sequences. You can set the threshold with
:code:`--skipped-fraction-to-repeat` option. If number of remaining sequences divided to cluster size is not below
this threshold, new consensus calculation will start, otherwise all reads with remaining sequences will be discarded
and not used in any consensus calculation. Also, if :code:`--max-consensuses-per-cluster` is exceeded, all remaining
reads will be discarded. :code:`--not-used-reads-output` argument allows to write all discarded reads to the separate
MIF file.

Calculated consensuses are processed with quality trimmer to remove low quality tails on the left and right sides.
You can set trim window size (length of subsequence for which average quality is calculated) with
:code:`--trim-window-size` option. Quality threshold is set by :code:`--avg-quality-threshold` option, and minimal
allowed consensus length after trimming - by :code:`--min-good-sequence-length` option. If resulting consensus will be
shorter, it will be discarded.

Input reads are prepared with the same quality trimmer before calculating consensus, but trimming parameters are
configured separately. Use :code:`--reads-trim-window-size` to set trim window size for input reads,
:code:`--reads-avg-quality-threshold` for quality threshold and :code:`--reads-min-good-sequence-length` for minimum
input read length after trimming. Too short reads will not be included in consensus calculation.

:code:`--consensuses-to-separate-groups` parameter changes action behavior significantly. If this parameter is not
specified, output file will contain calculated consensuses. If it is specified, original sequences will be written to
the output files, consensuses will be written as capture groups :code:`CR1`, :code:`CR2` etc, so it will be possible to
cluster original reads by consensuses using :ref:`filter` / :ref:`demultiplex` actions, or export original reads and
corresponding consensuses into separate reads using :ref:`mif2fastq` action. Note that input file must not contain any
groups named :code:`CR1`, :code:`CR2` etc if you use :code:`--consensuses-to-separate-groups` parameter.
