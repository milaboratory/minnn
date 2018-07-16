.. _pattern_syntax:

==============
Pattern Syntax
==============

Patterns are used in :code:`extract` action to specify which sequences must pass to the output and which sequences
must be filtered out. Also, capture groups in patterns are used for barcode extraction. Patterns must always
be specified after :code:`--pattern` option and must always be in double quotes. Examples:

.. code-block:: console

   mist extract --pattern "ATTAGACA"
   mist extract --pattern "*\*"
   mist extract --pattern "^(UMI:N{3:5})attwwAAA\*"

Basic Syntax Elements
---------------------

Many syntax elements in patterns are similar to regular expressions, but there are differences. Uppercase
and lowercase letters are used to specify the sequence that must be matched, but uppercase letters don't allow
indels between them and lowercase letters allow indels. Indels on left and right borders of uppercase letters are
also not allowed. Also, score penalty for mismatches in uppercase and lowercase letters can be different:
:code:`--mismatch-score` parameter used for lowercase mismatches and :code:`--uppercase-mismatch-score` for
uppercase mismatches. Standard IUPAC wildcards (N, W, S, M etc) are also allowed in both uppercase and lowercase
sequences.

``\`` character is very important syntax element: it used as read separator. There can be single-read input
files, in this case ``\`` character must not be used. In multi-read inputs ``\`` must be used, and number
of reads in pattern must be equal to number of input FASTQ files (or to number of reads in input MIF file if
:code:`--input-format mif` parameter is used). There can be many reads, but the most common case is 2 reads: R1 and R2.
By default, extract action will search R1, R2 combination and then try the same search with swapped reads R2, R1.
Then it will choose the match with better score. This is the default behavior; if you want to check only R1, R2
combination without checking reversed order, use :code:`--oriented` flag. R3, R4, R5 and further reads are never
swapped and not affected by :code:`--oriented` flag.

Another important syntax element is capture group. It looks like :code:`(group_name:query)` where :code:`group_name`
is any sequence of letters and digits (like :code:`UMI` or :code:`SB1`) that you use as group name. Group names are
case sensitive, so :code:`UMI` and :code:`umi` are different group names. :code:`query` is part of query that will be
saved as this capture group. It can contain nested groups and any other syntax elements that are allowed inside
single read.

:code:`R1`, :code:`R2`, :code:`R3` etc are built-in group names that contain full matched reads.
You can override them by specifying manually in the query, and overridden values will go to output instead of full
reads. For example, query like this

.. code-block:: console

   mist extract --input R1.fastq R2.fastq --pattern "^NNN(R1:(UMI:NNN)ATTAN{*})\^NNN(R2:NNNGACAN{*})"

can be used if you want to strip first 3 characters and override built-in R1 and R2 groups to write output reads
without stripped characters. Note that R1, R2, R3 etc, like any common groups, can contain nested groups and can be
nested inside other groups.

**Important:** in matches that come from swapped R1 and R2 reads, R1 in input will become R2 in output and vice versa,
if you don't use built-in group names override. If you use the override, R1, R2, R3 etc in output will come from
the place where they matched. If you export the output MIF file from :code:`extract` action to FASTQ and want
to determine whether the match came from straight or swapped R1, R2, check the comments for :code:`||~` character
sequence: it is added to matches that came from swapped reads. Look at :ref:`mif2fastq` section for
detailed information.

:code:`*` character can be used instead of read contents if any contents must match. It can be enclosed in one or
multiple capture groups, but can't be used if there are other query elements in the same read. If there are other
query elements, use :code:`N{*}` instead. For example, the following queries are **valid**:

.. code-block:: console

   mist extract --input R1.fastq R2.fastq --oriented --pattern "(G1:ATTA)\(G2:(G3:*))"
   mist extract --input R1.fastq R2.fastq R3.fastq --pattern "*\*\*"
   mist extract --input R1.fastq R2.fastq --pattern "(G1:ATTAN{*})\(G2:*)"

and this is **invalid**:

.. code-block:: console

   mist extract --input R1.fastq R2.fastq --pattern "(G1:ATTA*)\*"

Curly brackets after nucleotide can be used to specify number of repeats for the nucleotide. There can be any
nucleotide letter (uppercase or lowercase, basic or wildcard) and then curly braces with quantity specifier.
The following syntax constructions are allowed:

:code:`a{*}` - any number of repeats, from 1 to the entire sequence

:code:`a{:}` - same as the above

:code:`a{14}` - fixed number of repeats

:code:`a{3:6}` - specified interval of allowed repeats, interval borders are inclusive

:code:`a{:5}` - interval from 1 to specified number, inclusive

:code:`a{4:}` - interval from specified number (inclusive) to the entire sequence

Symbols :code:`^` and :code:`$` can be used to restrict matched sequence to start or end of the target sequence.
:code:`^` mark must be in the start of the query for the read, and it means that the query match must start from
the beginning of the read sequence. :code:`$` mark must be in the end, and it means that the query match must be in the
end of the read. Examples:

.. code-block:: console

   mist extract --pattern "^ATTA"
   mist extract --input R1.fastq R2.fastq --pattern "TCCNNWW$\^(G1:ATTAGACA)N{3:18}(G2:ssttggca)$"

Advanced Syntax Elements
------------------------
