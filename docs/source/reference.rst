Reference
=========
===================
Command Line Syntax
===================

.. include:: reference_descriptions/header.rst

.. _extract:

extract
-------
.. include:: reference_descriptions/extract.rst

.. code-block:: text

 --pattern: Query, pattern specified in MiNNN format.
 --input: Input files. Single file means that there is 1 read or multi-read file; multiple files mean that there is 1 file for each read. If not specified, stdin will be used.
 --output: Output file in "mif" format. If not specified, stdout will be used.
 --not-matched-output: Output file for not matched reads in "mif" format. If not specified, not matched reads will not be written anywhere.
 --input-format: Input data format. "fastq" (default) or "mif".
 --oriented: By default, if there are 2 or more reads, 2 last reads are checked in direct and reverse order. With this flag, only in direct order.
 --match-score: Score for perfectly matched nucleotide.
 --mismatch-score: Score for mismatched nucleotide.
 --uppercase-mismatch-score: Score for mismatched uppercase nucleotide.
 --gap-score: Score for gap or insertion.
 --score-threshold: Score threshold, matches with score lower than this will not go to output.
 --good-quality-value: This or better quality value will be considered good quality, without score penalties.
 --bad-quality-value: This or worse quality value will be considered bad quality, with maximal score penalty.
 --max-quality-penalty: Maximal score penalty for bad quality nucleotide in target.
 --single-overlap-penalty: Score penalty for 1 nucleotide overlap between neighbor patterns. Negative value.
 --max-overlap: Max allowed overlap for 2 intersecting operands in +, & and pattern sequences.
 --bitap-max-errors: Maximum allowed number of errors for bitap matcher.
 --fair-sorting: Use fair sorting and fair best match by score for all patterns.
 -n, --number-of-reads: Number of reads to take; 0 value means to take the entire input file.
 --threads: Number of threads for parsing reads.
 --description-group-: Description group names and regular expressions to parse expected nucleotide sequences for that groups from read description. Example: --description-group-CELLID1='ATTA.{2-5}GACA' --description-group-CELLID2='.{11}$'

.. _filter:

filter
------
.. include:: reference_descriptions/filter.rst

.. code-block:: text

 --input: Input file in "mif" format. If not specified, stdin will be used.
 --output: Output file in "mif" format. If not specified, stdout will be used.
 --fair-sorting: Use fair sorting and fair best match by score for all patterns.
 -n, --number-of-reads: Number of reads to take; 0 value means to take the entire input file.
 --threads: Number of threads for parsing reads.

.. _demultiplex:

demultiplex
-----------
.. include:: reference_descriptions/demultiplex.rst

.. code-block:: text

 Filter Options: Barcodes and sample configuration files that specify sequences for demultiplexing. At least 1 barcode or 1 sample file must be specified. Syntax example: minnn demultiplex --by-barcode UID --by-sample samples.txt input.mif

 --output-buffer-size: Write buffer size for each output file.
 -n, --number-of-reads: Number of reads to take; 0 value means to take the entire input file.

.. _mif2fastq:

mif2fastq
---------
.. include:: reference_descriptions/mif2fastq.rst

.. code-block:: text

 Group Options: Groups and their file names for output reads. At least 1 group must be specified. Built-in groups R1, R2, R3... used for input reads. Example: --group-R1 out_R1.fastq --group-R2 out_R2.fastq --group-UMI UMI.fastq

 --input: Input file in "mif" format. If not specified, stdin will be used.
 --copy-original-headers: Copy original comments from initial fastq files to comments of output fastq files.
 -n, --number-of-reads: Number of reads to take; 0 value means to take the entire input file.

.. _correct:

correct
-------
.. include:: reference_descriptions/correct.rst

.. code-block:: text

 --groups: Group names for correction.
 --input: Input file in "mif" format. This argument is required; stdin is not supported.
 --output: Output file in "mif" format. If not specified, stdout will be used.
 --max-mismatches: Maximum number of mismatches between barcodes for which they are considered identical.
 --max-indels: Maximum number of insertions or deletions between barcodes for which they are considered identical.
 --max-total-errors: Maximum Levenshtein distance between barcodes for which they are considered identical.
 --cluster-threshold: Threshold for UMI clustering: if smaller UMI count divided to larger UMI count is below this threshold, UMI will be merged to the cluster.
 --max-cluster-depth: Maximum cluster depth for algorithm of similar barcodes clustering.
 --single-substitution-probability: Single substitution probability for clustering algorithm.
 --single-indel-probability: Single insertion/deletion probability for clustering algorithm.
 -n, --number-of-reads: Number of reads to take; 0 value means to take the entire input file.
 --suppress-warnings: Don't display any warnings.

.. _sort:

sort
----
.. include:: reference_descriptions/sort.rst

.. code-block:: text

 --groups: Group names to use for sorting. Priority is in descending order.
 --input: Input file in "mif" format. If not specified, stdin will be used.
 --output: Output file in "mif" format. If not specified, stdout will be used.
 --chunk-size: Chunk size for sorter.
 --suppress-warnings: Don't display any warnings.

.. _consensus:

consensus
---------
.. include:: reference_descriptions/consensus.rst

.. code-block:: text

 --input: Input file in "mif" format. If not specified, stdin will be used.
 --output: Output file in "mif" format. If not specified, stdout will be used.
 --groups: List of groups that represent barcodes. If not specified, all groups will be used.
 --width: Window width (maximum allowed number of indels) for banded aligner.
 --aligner-match-score: Score for perfectly matched nucleotide, used in sequences alignment.
 --aligner-mismatch-score: Score for mismatched nucleotide, used in sequences alignment.
 --aligner-gap-score: Score for gap or insertion, used in sequences alignment.
 --good-quality-mismatch-penalty: Extra score penalty for mismatch when both sequences have good quality.
 --good-quality-mismatch-threshold: Quality that will be considered good for applying extra mismatch penalty.
 --score-threshold: Score threshold that used to filter reads for calculating consensus.
 --skipped-fraction-to-repeat: Fraction of reads skipped by score threshold that must start the search for another consensus in skipped reads. Value 1 means always get only 1 consensus from one set of reads with identical barcodes.
 --max-consensuses-per-cluster: Maximal number of consensuses generated from 1 cluster. Every time this threshold is applied to stop searching for new consensuses, warning will be displayed. Too many consensuses per cluster indicate that score threshold, aligner width or skipped fraction to repeat is too low.
 --reads-min-good-sequence-length: Minimal length of good sequence that will be still considered good after trimming bad quality tails. This parameter is for trimming input reads.
 --reads-avg-quality-threshold: Minimal average quality for bad quality tails trimmer. This parameter is for trimming input reads.
 --reads-trim-window-size: Window size for bad quality tails trimmer. This parameter is for trimming input reads.
 --min-good-sequence-length: Minimal length of good sequence that will be still considered good after trimming bad quality tails. This parameter is for trimming output consensuses.
 --avg-quality-threshold: Minimal average quality for bad quality tails trimmer. This parameter is for trimming output consensuses.
 --trim-window-size: Window size for bad quality tails trimmer. This parameter is for trimming output consensuses.
 --original-read-stats: Save extra statistics for each original read into separate file.
 --consensuses-to-separate-groups: If this parameter is specified, consensuses will not be written as reads R1, R2 etc to output file. Instead, original sequences will be written as R1, R2 etc and consensuses will be written as CR1, CR2 etc, so it will be possible to cluster original reads by consensuses using filter / demultiplex actions, or export original reads and corresponding consensuses into separate reads using mif2fastq action.
 -n, --number-of-reads: Number of reads to take; 0 value means to take the entire input file.
 --max-warnings: Maximum allowed number of warnings; -1 means no limit.
 --threads: Number of threads for calculating consensus sequences.

.. _stat-groups:

stat-groups
-----------
.. include:: reference_descriptions/stat-groups.rst

.. code-block:: text

 --groups: Space separated list of groups to output, determines the keys by which the output table will be aggregated.
 --input: Input file in "mif" format. If not specified, stdin will be used.
 --output: Output text file. If not specified, stdout will be used.
 --read-quality-filter: Filter group values with a min (non-aggregated) quality below a given threshold, applied on by-read basis, should be applied prior to any aggregation. 0 value means no threshold.
 --min-quality-filter: Filter group values based on min aggregated quality. 0 value means no filtering.
 --avg-quality-filter: Filter group values based on average aggregated quality. 0 value means no filtering.
 --min-count-filter: Filter unique group values represented by less than specified number of reads.
 --min-frac-filter: Filter unique group values represented by less than specified fraction of reads.
 -n, --number-of-reads: Number of reads to take; 0 value means to take the entire input file.

.. _stat-positions:

stat-positions
--------------
.. include:: reference_descriptions/stat-positions.rst

.. code-block:: text

 --groups: Space separated list of groups to output, determines IDs allowed in group.id column.
 --reads: Space separated list of original read IDs to output (R1, R2 etc), determines IDs allowed in read column. If not specified, all reads will be used.
 --output-with-seq: Also output matched sequences. If specified, key columns are group.id + read + seq + pos; if not specified, key columns are group.id + read + pos.
 --input: Input file in "mif" format. If not specified, stdin will be used.
 --output: Output text file. If not specified, stdout will be used.
 --min-count-filter: Filter unique group values represented by less than specified number of reads.
 --min-frac-filter: Filter unique group values represented by less than specified fraction of reads.
 -n, --number-of-reads: Number of reads to take; 0 value means to take the entire input file.

.. _report:

report
------
.. include:: reference_descriptions/report.rst

.. code-block:: text

 --pattern: Query, pattern specified in MiNNN format.
 --target: Target nucleotide sequence, where to search.
 --match-score: Score for perfectly matched nucleotide.
 --mismatch-score: Score for mismatched nucleotide.
 --uppercase-mismatch-score: Score for mismatched uppercase nucleotide.
 --gap-score: Score for gap or insertion.
 --score-threshold: Score threshold, matches with score lower than this will not go to output.
 --good-quality-value: This or better quality value will be considered good quality, without score penalties.
 --bad-quality-value: This or worse quality value will be considered bad quality, with maximal score penalty.
 --max-quality-penalty: Maximal score penalty for bad quality nucleotide in target.
 --single-overlap-penalty: Score penalty for 1 nucleotide overlap between neighbor patterns. Negative value.
 --max-overlap: Max allowed overlap for 2 intersecting operands in +, & and pattern sequences.
 --bitap-max-errors: Maximum allowed number of errors for bitap matcher.
 --fair-sorting: Use fair sorting and fair best match by score for all patterns.

