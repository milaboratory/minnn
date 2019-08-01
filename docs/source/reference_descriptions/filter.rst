Filter action is used to filter data from MIF file with specified query and write only matching reads to the output.
There are 2 general cases of using filter action:

1. Filtering input data by group value or length.
2. Filtering output of :ref:`consensus` or :ref:`consensus-dma` action to exclude consensuses assembled from too small
   number of reads.

Filter action works with queries. Query must be enclosed in double quotes :code:`""`. Details about filter syntax can
be found in :ref:`filter_syntax` section. Filter action must contain a query or at least one :code:`--whitelist`
argument.

:code:`--whitelist` argument is used to set whitelist of barcode values in a text file. Whitelist file must contain
barcode values or queries with MiNNN :ref:`pattern_syntax`, one value or query on the line. This is more convenient way
for specifying OR operator (see :ref:`filter_syntax`) when there are many operands. So, for example, instead of using
:code:`"BC1~'AAA' | BC1~'GGG' | BC1~'CCC'"` query, option :code:`--whitelist BC1=options_BC1.txt` can be used, where
:code:`options_BC1.txt` must contain :code:`AAA`, :code:`GGG` and :code:`CCC` lines. There can be multiple whitelist
options, it is useful for specifying whitelists for different barcodes. Also, whitelists and filter query can be used
simultaneously. In this case, only reads that match both the query and the whitelists will pass to the output.
Examples:

.. code-block:: text

   minnn filter --input data.mif --output filtered.mif "MinGroupQuality(G1) = 7" --whitelist G1=whitelist.txt
   minnn filter --whitelist UMI=whitelist_umi.txt --whitelist SB=whitelist_sb.txt --input in.mif --output out.mif

Example contents of whitelist file:

.. code-block:: text

   AAAAAATTCTTNNTTCT
   AAAAGGGGTTTCTCTGT
   <<<AAAATCTGGGCCTGTGCT
   AAAA+GGNAACT
   AAAAtttttttGGGCCT

:code:`--input` and :code:`--output` arguments are optional, and if they are missing, stdin and stdout will be used
instead of input and output files. Filter action always uses MIF format for input and output. Usage examples for filter
action can be found in :ref:`filter_syntax` section.

If :code:`--whitelist` argument or pattern filter is used in the filter query, then pattern matching algorithm based
on bitap and aligner will be used. This is the same algorithm as in :ref:`extract` action. :code:`--fair-sorting`
argument is option for this algorithm, you can read the details about it in help for :ref:`extract` action.
If there are no :code:`--whitelist` arguments and no pattern filters in the filter query, :code:`--fair-sorting`
argument has no effect.

:code:`--threads` option sets the number of threads for filter query matching. It is recommended to set it equal to the
number of CPU cores if there is at least one :code:`--whitelist` argument or pattern filter in the query,
or set :code:`--threads` to :code:`1` if :code:`--whitelist` arguments and pattern filters are not used.

Command line arguments reference for filter action:
