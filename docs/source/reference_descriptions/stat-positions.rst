Stat-positions action is used to collect summary statistics about positions of group matches in MIF file and create
table with group names, reads where they matched, their match positions, and counts for each combination occurrences.
Example:

.. code::

   group.id read pos count percent
   G1 R1 15 4240 4.27%
   G1 R2 15 3936 3.96%
   G1 R1 16 3490 3.51%
   G1 R2 16 3387 3.41%
   G1 R1 17 2203 2.22%
   G1 R2 17 1967 1.98%
   G1 R2 18 724 0.73%
   G1 R1 18 702 0.71%
   G1 R2 19 627 0.63%
   G1 R1 19 579 0.58%
   G4 R1 10 446 0.45%
   G4 R2 10 426 0.43%
   G2 R1 1 407 0.41%
   G2 R1 4 406 0.41%
   G3 R1 4 406 0.41%

:code:`--groups` argument is mandatory: you must specify space separated list of groups to collect statistics.
Stat-positions action will count occurrences for each group separately; occurrences are counted for unique combinations
*group + read + position*. The table is sorted by number of occurrences (:code:`count` column).

This table contains the following columns:

:code:`group.id` - group name

:code:`read` - read (:code:`R1`, :code:`R2`, :code:`R3` etc) where this group matched

:code:`pos` - position of this match inside read, starting from 0

:code:`count` - number of occurrences

:code:`percent` - number of occurrences in percentage to number of all checked reads (to number of lines the entire
input file if :code:`--number-of-reads` argument is not specified)

Examples for stat-positions action:

.. code-block:: console

   mist stat-positions --groups UMI --reads R2 R3 --input corrected.mif -n 10000
   xzcat extracted.mif.xz | mist stat-positions --groups G1 G2 G3 --output stat-positions.txt --output-with-seq

:code:`--input` argument means input file in MIF format, or if this argument is missing, stdin will be used.
:code:`--output` argument means output plain text file where the table will be written. If :code:`--output` argument
is missing, the table will be written to stdout.

:code:`--reads` argument allows to include only matches in specified reads (:code:`R1`, :code:`R2`, :code:`R3` etc) in
the output table. Allowed reads specified as space separated list. If :code:`--reads` argument is missing, all reads
will be included.

:code:`--output-with-seq` argument changes behavior of stat-positions action. New column :code:`seq` is added to the
table, and it contains nucleotide sequence for this match. With this argument occurrences are counted not by
*group + read + position* (default), but by *group + read + position + sequence*. Output table example with
:code:`--output-with-seq` argument:

.. code::

   group.id read pos count percent seq
   G1 R1 15 4231 4.26% TCTCAG
   G1 R2 15 3927 3.95% TCTCAG
   G1 R1 16 3484 3.51% TCTCAG
   G1 R2 16 3379 3.4% TCTCAG
   G1 R1 17 2200 2.22% TCTCAG
   G1 R2 17 1964 1.98% TCTCAG
   G1 R2 18 715 0.72% TCTCAG
   G1 R1 18 694 0.7% TCTCAG
   G1 R2 19 626 0.63% TCTCAG
   G1 R1 19 579 0.58% TCTCAG
   G4 R1 10 437 0.44% AC
   G4 R2 10 422 0.42% AC
   G4 R2 13 351 0.35% AT
   G3 R2 8 349 0.35% GTCAC
   G2 R2 5 348 0.35% AAA
   G4 R1 13 344 0.35% AT
   G4 R1 40 342 0.34% CG
   G1 R1 15 342 0.34% TCTCAA

:code:`--min-count-filter` argument allows to exclude table lines with :code:`count` lower than the specified
threshold, and :code:`--min-frac-filter` allows to exclude lines where count divided to total number of checked reads
is below the threshold (this is like value in :code:`percent` column, but in fractions of 1, not percents).

:code:`--number-of-reads` or :code:`-n` argument allows to collect statistics not from the entire file, but from
first specified number of reads.

In the end, stat-positions action will display total percentage of keys (*group + read + position* or
*group + read + position + sequence* with :code:`--output-with-seq` argument) that were included in the table
(passed all filters) to the total number of checked keys.

Command line arguments reference for stat-positions action:
