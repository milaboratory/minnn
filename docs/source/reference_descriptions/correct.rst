Correct action is used to correct errors in barcodes. It collects sequences from a capture group to the barcode tree,
and cluster barcodes by similarity and frequency, creating new cluster for each frequent barcode and for each
barcode that doesn't have matching cluster by similarity. Then it replaces rare barcodes with the most frequent ones
from the same cluster and writes reads with corrected barcodes to the output.

Correct action works in 3 stages (if :code:`--primary-groups` argument is not present):

1. Reading input file and collecting all barcodes from there.
2. Sorting and clustering barcodes.
3. Reading input file again, correcting barcodes and writing output file.

:code:`--input` argument is mandatory, reading data from stdin is not supported because correct action reads input
file twice, on stages 1 and 3. :code:`--output` argument is optional: results will be written to stdout if
:code:`--output` argument is missing. :code:`--groups` argument is mandatory, it must contain space separated list
of groups that will be corrected. Built-in groups :code:`R1`, :code:`R2`, :code:`R3` etc are not supported in correct
action.

Examples for correct action:

.. code-block:: text

   minnn correct --groups UMI --input extracted.mif --output corrected.mif --cluster-threshold 0.01
   minnn correct --groups G1 G3 G2 --max-unique-barcodes 7000 --input filtered.mif | xz > corrected.mif.xz
   minnn correct --groups SB1 SB2 --max-errors 2 --max-errors-share -1 --input data.mif --output corrected.mif

:code:`--max-unique-barcodes` argument is useful for correction of cell barcodes in single cell sequencing: it sets
maximal count of unique barcodes that will be included in the output file. Only barcodes with highest counts will be
included; barcodes with low counts will be filtered out. This limit is the same for each group and calculated for each
group separately, so if you want to set different limits for different groups (for example, for cell barcodes and UMI
in single cell sequencing), perform separate :code:`correct` action runs for different groups. Reads that contain at
least 1 filtered out barcode will not be included in the output. You can use :code:`--excluded-barcodes-output`
argument if you want to write filtered out reads to the separate MIF file. If :code:`--max-unique-barcodes` argument
is absent or set to :code:`0`, filtering by maximal number of unique barcodes will be disabled.

:code:`--min-count` argument allows to specify count threshold for barcodes directly. Barcodes with lower counts will
be filtered out. Reads that contain at least 1 filtered out barcode will not be included in the output.

:code:`--max-errors-share` argument specifies how two barcodes can differ in the same cluster. This share is multiplied
on average barcode length to calculate maximal allowed number of errors (Levenshtein distance) between barcodes.
Barcodes with bigger number of errors will not be corrected. The maximal allowed number of errors is calculated
separately for each group, for example, if there is short group :code:`CB` and long group :code:`UMI`, more errors
will be allowed in :code:`UMI` group. Negative value after :code:`--max-errors-share` means that this max errors
calculation method is disabled.

You can also specify the maximal allowed number of errors directly, same value for all groups. :code:`--max-errors`
argument can be used for this. It is disabled (set to :code:`-1`) by default.

**Important:** If multiple methods to specify the maximal allowed number of errors are enabled, then the lowest value
of max errors from these methods will be used. If you want, for example, to use only :code:`--max-errors` method, then
disable (set to :code:`-1`) the :code:`--max-errors-share` argument.

Also there is 3rd method to specify the maximal allowed number of errors in barcode.
:code:`--max-errors-count-multiplier` argument, if enabled (set to positive value), is multiplied on error probability
based on average of minimal barcode qualities from a share of barcodes with worst qualities; then multiplied on average
barcode length. There are 5 steps of calculation max errors value in this case:

1. A share of barcodes with worst qualities is extracted from the current group. Size of the share can be changed with
   :code:`--max-errors-worst-barcodes-share` argument.
2. Quality of the worst letter from each of barcodes in this share is taken.
3. Average quality of these worst letters is calculated.
4. Probability of error corresponding to this quality is calculated.
5. The probability is multiplied on average barcode length and then on :code:`--max-errors-count-multiplier` value.

This max errors calculation method is disabled (set to :code:`-1`) by default. If you want to enable it,
the recommended value for :code:`--max-errors-count-multiplier` is :code:`1`.

Clustering algorithm uses probabilities of substitutions and indels in sequence to check when barcode cannot be
added to cluster; for example, if the barcode's count is big, and there is low probability that this barcode emerged
because of errors. You can change the default values for these probabilities with
:code:`--single-substitution-probability` and :code:`--single-indel-probability` arguments. If you don't need
this feature, set both probabilities to :code:`1`.

In addition to :code:`--single-substitution-probability` and :code:`--single-indel-probability` arguments, clustering
algorithm also allows to directly specify the frequency threshold that prevents two unique but similar barcodes from
merging into one barcode. :code:`--cluster-threshold` argument can be used for it. If the current barcode's count
divided to cluster's largest barcode's count is below this threshold, the current barcode can be merged to the cluster,
otherwise it will form a new cluster. This feature is turned off (set to :code:`1`) by default.

Barcode clustering algorithm can use multiple layers: there is cluster head (the most frequent barcode in the
cluster), then the layer contains barcodes clustered to the head, and there can be more layers of barcodes clustered
to the previous layer. Maximum number of layers is specified by :code:`--max-cluster-depth` argument. If
:code:`--max-cluster-depth` is :code:`1` then there will be only 1 layer below the head; if
:code:`--max-cluster-depth` is :code:`2`, there will be second layer clustered to the first layer etc.

:code:`--primary-groups` argument means that barcodes must be corrected inside clusters that are formed from reads with
the same values of the primary groups. Usage example is correcting UMI separately for each unique cell barcode. It is
highly recommended to sort input MIF file by primary groups before correction because correction with unsorted primary
groups is much slower and memory consuming. If primary groups need correction, they must be corrected before sorting.
For example, we have 2 cell barcodes in groups :code:`CB1` and :code:`CB2` and UMI in group :code:`UMI`, and we want
to correct cell barcodes, and correct UMI for each unique combination of cell barcodes separately. Then we can use the
following sequence of commands:

.. code-block:: text

   minnn correct --groups CB1 CB2 --input data.mif --output corrected-primary.mif
   minnn sort --groups CB1 CB2 --input corrected-primary.mif --output sorted-primary.mif
   minnn correct --primary-groups CB1 CB2 --groups UMI --input sorted-primary.mif --output corrected-secondary.mif

Note that :code:`--max-unique-barcodes` and :code:`--min-count` are counted separately for each cluster if
:code:`--primary-groups` argument is present, so you may want to set lower values for :code:`--max-unique-barcodes` and
:code:`--min-count` arguments if you use them.

Command line arguments reference for correct action:
