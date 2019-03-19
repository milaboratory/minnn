========
Appendix
========

.. _filter_syntax:

Filter Syntax
-------------

Filters are used in :ref:`filter` action to set filtering conditions. There are 2 general cases of using filter action:

1. Filtering input data by group value or length.
2. Filtering output of :ref:`consensus` and :ref:`consensus-dma` actions to exclude consensuses assembled from too
   small number of reads.

Filtering query must always be specified as separate argument in double quotes. Examples:

.. code-block:: text

   minnn filter --input extracted.mif --output filtered.mif "UMI~'ATTAGACA'"
   minnn filter "Len(SB)=11" --input corrected.mif --output filtered.mif
   minnn filter --input consensus.mif "MinConsensusReads=150" --output consensus_filtered.mif
   minnn filter "Len(G1)=4 & Len(G2)=6 & G2~'TCCA'"

Filter syntax uses 3 basic filters: pattern filter, length filter and consensus reads filter. Also, there are logic
operators and parentheses that can be used to combine multiple filters in 1 action.

-------------
Basic Filters
-------------

Pattern filter is used for filtering reads by group contents. Only reads where group value matches the pattern will
be passed to the output. Pattern syntax is the same as for :ref:`extract` action, it is described in in
:ref:`pattern_syntax` section. Pattern filter uses the following syntax: :code:`group_name~'pattern_query'`. Pattern
query must always be in single quotes (:code:`''`). Groups (parentheses) and read separators (``\``) are **not**
allowed in pattern query. Examples of pattern filter usage:

.. code-block:: text

   minnn filter "UMI~'GCC || ^TCA'"
   minnn filter "GROUP1~'ATTA'"
   minnn filter "G1~'~ATT$ && ~^GCC'"

Length filter is used for filtering reads by group length. The syntax is :code:`Len(group_name)=value`. Only reads
where length of the specified group equals to the :code:`value` will be passed to the output. Example:

.. code-block:: text

   minnn filter "Len(G1) = 3"

Group quality filters are used to filter out reads with low quality barcodes. The syntax for these filters is
:code:`MinGroupQuality(group_name)=value` and :code:`AvgGroupQuality(group_name)=value`. :code:`MinGroupQuality`
will filter out reads where at least 1 nucleotide in the specified group has quality lower than the specified
value. :code:`AvgGroupQuality` will filter out reads where average quality of all nucleotides in the specified group
is lower than the specified value. Examples:

.. code-block:: text

   minnn filter "MinGroupQuality(G1) = 7"
   minnn filter "AvgGroupQuality(UMI) = 20"

N count filters can be used to filter out matched barcodes with too many :code:`N` letters.
:code:`GroupMaxNCount(group_name)=value` excludes reads where the specified groups contains more :code:`N` letters
than the specified value. :code:`GroupMaxNFraction(group_name)=value` allows to specify the maximal number of
:code:`N` letters as a fraction of group length. Specified value in this filter must be floating point in range from
:code:`0` to :code:`1`. Examples:

.. code-block:: text

   minnn filter "GroupMaxNCount(SB) = 3"
   minnn filter "GroupMaxNFraction(UMI) = 0.1"

All filters that have :code:`group_name` as argument allow to use :code:`*` instead of group name. This option allows
to apply filter to all groups in the input (except built-in groups :code:`R1`, :code:`R2` etc). Examples:

.. code-block:: text

   minnn filter "Len(*) = 5"
   minnn filter "MinGroupQuality(*) = 10"
   minnn filter "AvgGroupQuality(*) = 15"
   minnn filter "GroupMaxNCount(*) = 0"
   minnn filter "GroupMaxNFraction(*) = 0.15"

Consensus reads filter is used for filtering MIF files written by :ref:`consensus` and :ref:`consensus-dma` actions.
The syntax is :code:`MinConsensusReads=value`. Only consensuses calculated from :code:`value` or more reads will be
passed to the output. Example:

.. code-block:: text

   minnn filter "MinConsensusReads = 18"

---------------
Logic Operators
---------------

There are logic operators :code:`&` (AND) and :code:`|` (OR) that can be used in filtering query to combine multiple
basic filters. There can be multiple logic operators in 1 query; :code:`&` has higher priority than :code:`|`.
Parentheses :code:`()` can be used to manage operations priority. Examples:

.. code-block:: text

   minnn filter "MinConsensusReads=25 & G1~'TCGCC'"
   minnn filter "G1~'N{4:8}' & (G2~'ATTA' | G3~'GACA')"
   minnn filter "Len(G1)=10 & Len(G2)=8 | Len(G1)=8 & Len(G2)=10"
