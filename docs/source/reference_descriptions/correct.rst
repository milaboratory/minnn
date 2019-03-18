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
   minnn correct --groups SB1 SB2 --max-total-errors 2 --max-unique-barcodes 0 --input data.mif --output corrected.mif

:code:`--max-unique-barcodes` is an important argument: it sets maximal count of unique barcodes that will be included
in the output file. Only barcodes with highest counts will be included; barcodes with low counts will be filtered out.
This limit is the same for each group and calculated for each group separately. Reads that contain at least 1 filtered
out barcode will not be included in the output. You can use :code:`--excluded-barcodes-output` argument if you want
to write filtered out reads to the separate MIF file, or specify :code:`--max-unique-barcodes 0` to enable any amount
of unique barcodes and disable this filtering feature.

:code:`--min-count` argument allows to specify count threshold for barcodes directly. Barcodes with lower counts will
be filtered out. Reads that contain at least 1 filtered out barcode will not be included in the output.

Arguments :code:`--max-mismatches`, :code:`--max-indels` and :code:`--max-total-errors` specify how two barcodes can
differ in the same cluster. Two barcodes for which at least one of these 3 restrictions is not met will never be
added to the same cluster. Therefore, barcodes with number of mutations that exceed these values will not be corrected.

:code:`--cluster-threshold` is frequency threshold that prevents two unique but similar barcodes from merging into
one barcode. If this barcode count divided to cluster's largest barcode count is below this threshold, this barcode can
be merged to the cluster, otherwise it will form a new cluster.

Barcode clustering algorithm can use multiple layers: there is cluster head (the most frequent barcode in the
cluster), then the layer contains barcodes clustered to the head, and there can be more layers of barcodes clustered
to the previous layer. Maximum number of layers is specified by :code:`--max-cluster-depth` argument. If
:code:`--max-cluster-depth` is :code:`1` then there will be only 1 layer below the head; if
:code:`--max-cluster-depth` is :code:`2`, there will be second layer clustered to the first layer etc.

In addition to :code:`--cluster-threshold`, clustering algorithm also uses probabilities of substitutions and indels
in sequence for extra restrictions when barcode cannot be added to cluster. You can set these probabilities manually
with :code:`--single-substitution-probability` and :code:`--single-indel-probability` arguments. If you don't need this
feature, set both probabilities to :code:`1`.

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
:code:`--min-count` in this case.

Command line arguments reference for correct action:
