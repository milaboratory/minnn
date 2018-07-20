========
Appendix
========

.. _filter_syntax:

Filter Syntax
-------------

Filters are used in :ref:`filter` action to set filtering conditions. There are 2 general cases of using filter action:

1. Filtering input data by group value or length.
2. Filtering output of :ref:`consensus` action to exclude consensuses assembled from too small number of reads.

Filtering query must always be specified as separate argument in double quotes. Examples:

.. code-block:: console

   mist filter --input extracted.mif --output filtered.mif "UMI~'ATTAGACA'"
   mist filter "Len(SB)=11" --input corrected.mif --output filtered.mif
   mist filter --input consensus.mif "MinConsensusReads=150" --output consensus_filtered.mif
   mist filter "Len(G1)=4 & Len(G2)=6 & G2~'TCCA'"

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

.. code-block:: console

   mist filter "UMI~'GCC || ^TCA'"
   mist filter "GROUP1~'ATTA'"
   mist filter "G1~'~ATT$ && ~^GCC'"

Length filter is used for filtering reads by group length. The syntax is :code:`Len(group_name)=value`. Only reads
where length of the specified group equals to the *value* will be passed to the output. Examples:

.. code-block:: console

   mist filter "Len(UMI)=8"
   mist filter "Len(G1) = 3"

Consensus reads filter is used for filtering MIF files written by :ref:`consensus` action. The syntax is
:code:`MinConsensusReads=value`. Only consensuses calculated from *value* or more reads will be passed to the output.
Examples:

.. code-block:: console

   mist filter "MinConsensusReads=10"
   mist filter "MinConsensusReads = 18"

---------------
Logic Operators
---------------

There are logic operators :code:`&` (AND) and :code:`|` (OR) that can be used in filtering query to combine multiple
basic filters. There can be multiple logic operators in 1 query; :code:`&` has higher priority than :code:`|`.
Parentheses :code:`()` can be used to manage operations priority. Examples:

.. code-block:: console

   mist filter "MinConsensusReads=25 & G1~'TCGCC'"
   mist filter "G1~'N{4:8}' & (G2~'ATTA' | G3~'GACA')"
   mist filter "Len(G1)=10 & Len(G2)=8 | Len(G1)=8 & Len(G2)=10"
