Filter action is used to filter data from MIF file with specified query and write only matching reads to the output.
There are 2 general cases of using filter action:

1. Filtering input data by group value or length.
2. Filtering output of :ref:`consensus` action to exclude consensuses assembled from too small number of reads.

Filter action works with queries. Query is mandatory argument and must be enclosed in double quotes :code:`""`. Details
about filter syntax can be found in :ref:`filter_syntax` section. :code:`--input` and :code:`--output` arguments are
optional, and if they are missing, stdin and stdout will be used instead of input and output files. Filter action
always uses MIF format for input and output. Usage examples for filter action can be found in :ref:`filter_syntax`
section.

If pattern filter is used in filter query, then pattern matching algorithm based on bitap and aligner will be used.
This is the same algorithm as in :ref:`extract` action. :code:`--fair-sorting` argument is option for this algorithm,
you can read the details about it in help for :ref:`extract` action. If there are no pattern filters in the filter
query, :code:`--fair-sorting` argument has no effect.

:code:`--threads` option sets the number of threads for filter query matching. It is recommended to set it equal to the
number of CPU cores if at least 1 pattern filter is used in the query, or set :code:`--threads` to :code:`1` if
pattern filters are not used.

Command line arguments reference for filter action:
