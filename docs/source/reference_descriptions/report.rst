Report action is used to find match and groups in query and display report on the screen. Most of this action
arguments are the same that of :ref:`extract` action, and it uses the same pattern syntax (see :ref:`pattern_syntax`
page). The difference from extract is that report action doesn't work with files; instead it uses pattern with
single target, which can be single-read or multi-read, and displays result on the screen immediately. It can be useful
to quickly test complex patterns before using them on big files in extract action.

Mandatory command line arguments are :code:`--pattern` and :code:`--target`. :code:`--pattern` argument has the same
syntax as in :ref:`extract` action. :code:`--target` is used to specify target nucleotide sequence, or multiple
space separated sequences for multi-read targets. Space separated multi-targets must be enclosed in double quotes
:code:`""`. Nucleotide sequences are without quality, case insensitive (:code:`ATTAGACA` and :code:`attagaca` means
the same in the target), and can contain wildcards (N, W, S, M etc). Examples:

.. code-block:: text

   minnn report --pattern "(SB1:NNN)atta \ (SB2:NNN)gaca" --target "CCTCCCCACCA ATTAGACA"
   minnn report --pattern "NCCN" --target WCNTDTBCDATD
   minnn report --pattern "[TTT \ CCC] || [AAA \ GGG && AGC \ GTT]" --score-threshold 0 --target "aaagc gggtt"

Example output:

.. code-block:: text

   $ minnn report --pattern "(SB1:NNN)atta \ (SB2:NNN)gaca" --target "CCTCCCCACCA ATTAGACA"
   Found match in target 1 (CCTCCCCACCA): CCCACCA
   Range in this target: (4->11)

   Found match in target 1 (ATTAGACA): TTAGACA
   Range in this target: (1->8)

   Found matched group SB1 in target 1 (CCTCCCCACCA): CCC
   Range in this target: (4->7)

   Found matched group R1 in target 1 (CCTCCCCACCA): CCTCCCCACCA
   Range in this target: (0->11)

   Found matched group SB2 in target 1 (ATTAGACA): TTA
   Range in this target: (1->4)

   Found matched group R2 in target 2 (ATTAGACA): ATTAGACA
   Range in this target: (0->8)

Command line arguments reference for report action:
