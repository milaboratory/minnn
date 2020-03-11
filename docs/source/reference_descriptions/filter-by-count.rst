Filter by count action is used to filter barcode values by their count. It works in 2 stages:

1. Reading input file and counting barcodes.
2. Reading input file again, and writing only matching barcodes (with count above the threshold) to the output file.

:code:`--input` argument is mandatory, reading data from stdin is not supported because filter-by-count action reads
input file twice. :code:`--output` argument is optional: results will be written to stdout if :code:`--output` argument
is missing. :code:`--groups` argument is mandatory, it must contain space separated list of groups that will be
filtered by count. Built-in groups :code:`R1`, :code:`R2`, :code:`R3` etc are not supported in filter-by-count action.

Examples for filter-by-count action:

.. code-block:: text

   minnn filter-by-count --groups UMI --input extracted.mif --output filtered.mif --max-unique-barcodes 7000
   minnn filter-by-count --groups G1 G3 G2 --min-count 100 --input data.mif | xz > filtered.mif.xz
   minnn filter-by-count --groups G1 --min-count 10 --input input.mif --output high-count.mif --excluded-barcodes-output low-count.mif

:code:`--max-unique-barcodes` argument is useful for filtering cell barcodes in single cell sequencing: it sets
maximal count of unique barcodes that will be included in the output file. Only barcodes with highest counts will be
included; barcodes with low counts will be filtered out. This limit is the same for each group and calculated for each
group separately, so if you want to set different limits for different groups (for example, for cell barcodes and UMI
in single cell sequencing), perform separate :code:`filter-by-count` action runs for different groups. Reads that
contain at least 1 filtered out barcode will not be included in the output. You can use
:code:`--excluded-barcodes-output` argument if you want to write filtered out reads to the separate MIF file.
If :code:`--max-unique-barcodes` argument is absent or set to :code:`0`, filtering by maximal number of unique barcodes
will be disabled.

:code:`--min-count` argument allows to specify count threshold for barcodes directly. Barcodes with lower counts will
be filtered out. Reads that contain at least 1 filtered out barcode will not be included in the output.

:code:`--max-unique-barcodes` and :code:`--min-count` arguments can be used simultaneously; in this case, both filters
will be applied to each barcode.

Command line arguments reference for filter-by-count action:
