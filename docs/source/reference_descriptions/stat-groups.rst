Stat-groups action is used to collect summary statistics about values of capture groups in MIF file and create table
with sequences, qualities and counts for these capture groups. The table is plain text, space separated. Example:

.. code::

   G1.seq G1.qual.min G1.qual.avg G2.seq G2.qual.min G2.qual.avg G3.seq G3.qual.min G3.qual.avg G4.seq G4.qual.min G4.qual.avg count percent
   TCTCAG 111111 FFFFFF CGA 1// FFF GGAGC ////0 FFFFF CG // FF 1937 7.8%
   TCTCAG 111111 FFFFFF ACA ... EFE GGTGC ../// EEEEF CT ./ FF 1638 6.6%
   TCTCAG 111111 FFFFFF AGA 0// FFE AGTAC ///// FFFFF AA // FE 1598 6.44%
   TCTCAG 111111 FFFFFF AGA ../ FFF AGTAC ///// EFFFF AC // FF 1425 5.74%
   TCTCAG ;11:1: FFFFFF CAA 900 FFF AGTAC 01:>> FFFFF AA 10 FF 1122 4.52%
   TCTCAG 111111 FFFFFF AAA 011 EFF GTCAC 11111 FFFFF AT 11 FF 1050 4.23%
   TCTCAG 111111 FFFFFF AGA /./ FFF GGGGC /.... FEFEF GA .. FE 1025 4.13%

:code:`--groups` argument is mandatory: you must specify space separated list of groups to collect statistics.
Stat-groups action will count occurrences for each unique combination of group values, or for each unique value
if only 1 group is specified. The table is sorted by number of occurrences (:code:`count` column).

This table contains the following columns:

:code:`GROUPNAME.seq` (for each group) - value (nucleotide sequence) of the group

:code:`GROUPNAME.qual.min` (for each group) - minimal quality for each letter from all occurrences of this value

:code:`GROUPNAME.qual.avg` (for each group) - average quality for each letter counted by all occurrences of this value

:code:`count` - number of occurrences of this value

:code:`percent` - percentage of this value occurrences in all checked reads (in the entire input file if
:code:`--number-of-reads` argument is not specified)

Examples for stat-groups action:

.. code-block:: console

   mist stat-groups --groups UMI --input corrected.mif --read-quality-filter 10 --min-frac-filter 0.05
   xzcat extracted.mif.xz | mist stat-groups --groups G1 G2 G3 --output stat-groups.txt --min-count-filter 100 -n 10000

:code:`--input` argument means input file in MIF format, or if this argument is missing, stdin will be used.
:code:`--output` argument means output plain text file where the table will be written. If :code:`--output` argument
is missing, the table will be written to stdout.

:code:`--read-quality-filter` argument allows to filter input reads by **minimal** quality of their letters. If any
letter in any group value will have quality below this threshold, the entire read will be ignored (but still counted as
checked read: number of checked reads is used as total in :code:`percent` column and as stop condition in
:code:`--number-of-reads` argument).

:code:`--min-quality-filter` argument allows to exclude table lines in which minimal quality for at least 1 letter
is below the specified threshold. :code:`--avg-quality-filter` argument allows to exclude table lines in which average
quality of at least 1 group is below the specified threshold. Quality of a group is calculated as average quality of
all letters of this group.

:code:`--min-count-filter` argument allows to exclude table lines with :code:`count` lower than the specified
threshold, and :code:`--min-frac-filter` allows to exclude lines where count divided to total number of checked reads
is below the threshold (this is like value in :code:`percent` column, but in fractions of 1, not percents).

:code:`--number-of-reads` or :code:`-n` argument allows to collect statistics not from the entire file, but from
first specified number of reads.

In the end, stat-groups action will display total percentage of reads included in the table (that passed all filters)
to the total number of checked reads.

Command line arguments reference for stat-groups action:
