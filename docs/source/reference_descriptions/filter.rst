Filter action is used to filter data from MIF file with specified query and write only matching reads to the output.
There are 3 general cases of using filter action:

1. Filter input or corrected data by barcodes whitelist.
2. Filtering input data by group value or length.
3. Filtering output of :ref:`consensus` or :ref:`consensus-dma` action to exclude consensuses assembled from too small
   number of reads.

Filter action works with queries. Query must be enclosed in double quotes :code:`""`. Details about filter syntax can
be found in :ref:`filter_syntax` section. Filter action must contain a query or at least one :code:`--whitelist` or
:code:`--whitelist-patterns` argument.

:code:`--whitelist` argument is used to set whitelist of barcode values from a text file. Whitelist file must contain
exact barcode values, one value on the line. **Important:** :code:`--whitelist` filtering doesn't support matching by
wildcards. Entries with wildcards in whitelist file will be transformed to all corresponding combinations of entries
with basic letters. Also, whitelist entries with wildcards allow exact matches: for example, for entry :code:`NNGT`
value :code:`NNGT` in the input will match, but value :code:`NAGT` will be filtered out. If you need full wildcards
support, use :code:`--whitelist-patterns` argument.

:code:`--whitelist-patterns` argument is much slower than :code:`--whitelist`, but it allows to use many advanced
features in whitelist entries, and has full wildcards support. Whitelist file for :code:`--whitelist-patterns` argument
must contain queries with MiNNN :ref:`pattern_syntax`, one query on the line. Exact barcode values are also possible
with this syntax: to use exact barcode value in the whitelist, start it with :code:`^` (it means that value must start
from the beginning of the capture group) and end it with :code:`$` (end is exactly at the end of the capture group);
for example, :code:`^TTTGGCAGC$`.

There can be multiple :code:`--whitelist` and :code:`--whitelist-patterns` options, it is useful for specifying
whitelists for different barcodes. Also, whitelists and filter query can be used simultaneously. In this case, only
reads that match both the query and the whitelists will pass to the output.

Examples:

.. code-block:: text

   minnn filter --input data.mif --output filtered.mif "MinGroupQuality(G1) = 7" --whitelist-patterns G1=whitelist.txt
   minnn filter --whitelist UMI=whitelist_umi.txt --whitelist SB=whitelist_sb.txt --input in.mif --output out.mif

Example contents of whitelist file for :code:`--whitelist` argument:

.. code-block:: text

   GGTCCTTCAGC
   GGTAATTCAGC
   GGTCCGTCAGC
   GGTCCTTCTTT
   GGTCCTTCTTAA
   GGTCCTTCTGCA

Example contents of whitelist file for :code:`--whitelist-patterns` argument:

.. code-block:: text

   ^AAAAAATTCTTNNTTCT$
   AAGGGGTTTCTCTGT$
   ^<<<AAAATCTGGGCCTGTGCT$
   ^AAAA + GGNAACT
   AAAATTTT & TTTGGGCCT

:code:`--input` and :code:`--output` arguments are optional, and if they are missing, stdin and stdout will be used
instead of input and output files. Filter action always uses MIF format for input and output. Usage examples for filter
action can be found in :ref:`filter_syntax` section.

If :code:`--whitelist-patterns` argument or pattern filter is used in the filter query, then pattern matching
algorithm will be used. This is the same algorithm as in :ref:`extract` action. :code:`--fair-sorting` argument is
option for this algorithm, you can read the details about it in help for :ref:`extract` action. If there are no
:code:`--whitelist-patterns` arguments and no pattern filters in the filter query, :code:`--fair-sorting` argument
has no effect.

:code:`--threads` option sets the number of threads for filter query matching. By default, it is equal to available
number of CPU cores.

Command line arguments reference for filter action:
