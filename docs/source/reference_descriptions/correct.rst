Correct action is used to correct errors in barcodes. It collects sequences from a capture group to the barcode tree,
and cluster barcodes by similarity and frequency, creating new cluster for each frequent barcode and for each
barcode that doesn't have matching cluster by similarity. Then it replaces rare barcodes with the most frequent ones
from the same cluster and writes reads with corrected barcodes to the output.

Correct action works in 4 stages (if :code:`--primary-groups` argument is not present):

#. Reading input file and collecting all barcodes from there.
#. Clustering barcodes by wildcards to find barcodes with wildcards that can be merged with more specific barcodes.
#. Clustering barcodes to find possible mismatches and indels; creating correction table.
#. Reading input file again, correcting barcodes and writing output file.

**Important:** file must be sorted with :ref:`sort` action before using correct action, and :code:`--groups` argument
in sort action must contain the same groups in the same order as in correct action.

:code:`--input` argument is mandatory, reading data from stdin is not supported because correct action reads input
file twice, on stages 1 and 3. :code:`--output` argument is optional: results will be written to stdout if
:code:`--output` argument is missing. :code:`--groups` argument is mandatory, it must contain space separated list
of groups that will be corrected. Built-in groups :code:`R1`, :code:`R2`, :code:`R3` etc are not supported in correct
action.

Examples for correct action:

.. code-block:: text

   minnn correct --groups UMI --input sorted.mif --output corrected.mif --cluster-threshold 0.01
   minnn correct --groups G1 G3 G2 --max-unique-barcodes 7000 --input filtered.mif | xz > corrected.mif.xz
   minnn correct --groups SB1 SB2 --max-errors 2 --max-errors-share -1 --input data.mif --output corrected.mif

:code:`--max-unique-barcodes` and :code:`--min-count` arguments can be used to filter barcode values by their count
*after* correction. They work in the same way as in :ref:`filter-by-count` action.

:code:`--max-errors-share` argument specifies how two barcodes can differ in the same cluster. This share is multiplied
on average barcode length to calculate maximal allowed number of errors (Levenshtein distance) between barcodes;
but if result is less than 1, it rounds up to 1. Barcodes with bigger number of errors will not be corrected.
The maximal allowed number of errors is calculated separately for each group, for example, if there is short
group :code:`CB` and long group :code:`UMI`, more errors will be allowed in :code:`UMI` group. Negative value
after :code:`--max-errors-share` means that :code:`--max-errors-share` argument is disabled and you must set the
:code:`--max-errors` argument.

You can specify the maximal allowed number of errors directly, same value for all groups. :code:`--max-errors`
argument can be used for this. It is disabled (set to :code:`-1`) by default.

**Important:** If both :code:`--max-errors-share` and :code:`max-errors` arguments are enabled, then the lowest value
of max errors from these arguments will be used. If you want, for example, to use only :code:`--max-errors`, then
disable (set to :code:`-1`) the :code:`--max-errors-share` argument.

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

If there are wildcards in the barcodes, then barcodes with wildcards can be merged with more specific barcodes
*before* performing search for mismatches and indels. On this wildcards pre-processing stage, when merging cluster of
barcodes with pure letter in a position and cluster of barcodes with wildcard in that position, clusters will be merged
if pure letter cluster size multiplied on the threshold is greater or equal to wildcard cluster size, otherwise
clusters will be treated as different barcodes. The threshold can be specified with
:code:`--wildcards-collapsing-merge-threshold` argument. If this argument is absent, default threshold value
will be used.

:code:`--primary-groups` argument means that barcodes must be corrected inside clusters that are formed from reads with
the same values of the primary groups. Usage example is correcting UMI separately for each unique cell barcode.
**Important:** if :code:`--primary-groups` argument is used, then input file must be sorted by both primary and
secondary groups, and primary groups must be *first* in :code:`--groups` argument of sort action. For example, if
correct action contains arguments :code:`--primary-groups CB1 CB2` and :code:`--groups UMI1 UMI2`, then sort action
must contain argument :code:`--groups CB1 CB2 UMI1 UMI2`. If primary groups need correction, they must be corrected
before this sorting, with separate correct action.

For example, we have 2 cell barcodes in groups :code:`CB1` and :code:`CB2` and UMI in group :code:`UMI`, and we want
to correct cell barcodes, and correct UMI for each unique combination of cell barcodes separately. Then we can use the
following sequence of commands:

.. code-block:: text

   minnn sort --groups CB1 CB2 --input data.mif --output sorted-primary.mif
   minnn correct --groups CB1 CB2 --input sorted-primary.mif --output corrected-primary.mif
   minnn sort --groups CB1 CB2 UMI --input corrected-primary.mif --output sorted-all.mif
   minnn correct --primary-groups CB1 CB2 --groups UMI --input sorted-all.mif --output corrected-secondary.mif

Note that :code:`--max-unique-barcodes` and :code:`--min-count` are counted separately for each cluster if
:code:`--primary-groups` argument is present, so you may want to set lower values for :code:`--max-unique-barcodes` and
:code:`--min-count` arguments if you use them.

:code:`--threads` argument works only if :code:`--primary-groups` argument is specified. Correction of
secondary barcodes for each combination of primary barcodes can be performed in separate thread.

Command line arguments reference for correct action:
