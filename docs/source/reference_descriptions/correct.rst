Correct action is used to correct errors in barcodes. It collects sequences from a capture group to the barcode tree,
and cluster barcodes by similarity and frequency, creating new cluster for each frequent barcode and for each
barcode that doesn't have matching cluster by similarity. Then it replaces rare barcodes with the most frequent ones
from the same cluster and writes reads with corrected barcodes to the output.

Correct action works in 3 stages:

1. Reading input file and collecting all barcodes from there.
2. Sorting and clustering barcodes.
3. Reading input file again, correcting barcodes and writing output file.

:code:`--input` argument is mandatory, reading data from stdin is not supported because correct action reads input
file twice, on stages 1 and 3. :code:`--output` argument is optional: results will be written to stdout if
:code:`--output` argument is missing. :code:`--groups` argument is mandatory, it must contain space separated list
of groups that will be corrected. Built-in groups R1, R2, R3 etc are not supported in correct action.

Examples for correct action:

.. code-block:: console

   mist correct --groups UMI --input extracted.mif --output corrected.mif --cluster-threshold 0.01
   mist correct --groups G1 G3 G2 --input filtered.mif | xz > corrected.mif.xz
   mist correct --groups SB1 SB2 --max-total-errors 2 --input data.mif --output corrected.mif

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

Command line arguments reference for correct action:
