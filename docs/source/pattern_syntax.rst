.. _pattern_syntax:

==============
Pattern Syntax
==============

Patterns are used in :ref:`extract` action to specify which sequences must pass to the output and which sequences
must be filtered out. Also, capture groups in patterns are used for barcode extraction. Patterns must always
be specified after :code:`--pattern` option and must always be in double quotes. Examples:

.. code-block:: text

   minnn extract --pattern "ATTAGACA"
   minnn extract --pattern "*\*" --input R1.fastq R2.fastq
   minnn extract --pattern "^(UMI:N{3:5})attwwAAA\*" --input-format mif

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
:code:`--input-format MIF` parameter is used). There can be many reads, but the most common case is 2 reads:
:code:`R1` and :code:`R2`. By default, extract action will check input reads in order they specified in
:code:`--input` argument, or if input file is MIF, then in order they saved in MIF file. If :code:`--try-reverse-order`
argument is specified, it will also try the combination with 2 swapped last reads (for example, if there are 3 reads,
it will try :code:`R1, R2, R3` and :code:`R1, R3, R2` combinations), and then choose the match with better score.

Another important syntax element is capture group. It looks like :code:`(group_name:query)` where :code:`group_name`
is any sequence of letters and digits (like :code:`UMI` or :code:`SB1`) that you use as group name. Group names are
case sensitive, so :code:`UMI` and :code:`umi` are different group names. :code:`query` is part of query that will be
saved as this capture group. It can contain nested groups and some other syntax elements that are allowed inside
single read (see below).

:code:`R1`, :code:`R2`, :code:`R3` etc are built-in group names that contain full matched reads.
You can override them by specifying manually in the query, and overridden values will go to output instead of full
reads. For example, query like this

.. code-block:: text

   minnn extract --input R1.fastq R2.fastq --pattern "^NNN(R1:(UMI:NNN)ATTAN{*})\^NNN(R2:NNNGACAN{*})"

can be used if you want to strip first 3 characters and override built-in :code:`R1` and :code:`R2` groups to write
output reads without stripped characters. Note that :code:`R1`, :code:`R2`, :code:`R3` etc, like any common groups,
can contain nested groups and can be nested inside other groups.

**Important:** in matches that come from swapped reads (when :code:`--try-reverse-order` argument is specified),
if you don't use built-in group names override, :code:`R1` in input will become :code:`R2` in output and vice versa
(or there can be, for example, swapped :code:`R2` and :code:`R3` in case of 3 reads). If you use the override,
:code:`R1`, :code:`R2`, :code:`R3` etc in output will come from the place where they matched. If you export the output
MIF file from :ref:`extract` action to FASTQ and want to determine whether the match came from straight or swapped
reads, check the comments for :code:`||~` character sequence: it is added to matches that came from swapped reads.
Look at :ref:`mif2fastq` section for detailed information.

:code:`*` character can be used instead of read contents if any contents must match. It can be enclosed in one or
multiple capture groups, but can't be used if there are other query elements in the same read. If there are other
query elements, use :code:`N{*}` instead. For example, the following queries are **valid**:

.. code-block:: text

   minnn extract --input R1.fastq R2.fastq --try-reverse-order --pattern "(G1:ATTA)\(G2:(G3:*))"
   minnn extract --input R1.fastq R2.fastq R3.fastq --pattern "*\*\*"
   minnn extract --input R1.fastq R2.fastq --pattern "(G1:ATTAN{*})\(G2:*)"

and this is **invalid**:

.. code-block:: text

   minnn extract --input R1.fastq R2.fastq --pattern "(G1:ATTA*)\*"

Curly brackets after nucleotide can be used to specify number of repeats for the nucleotide. There can be any
nucleotide letter (uppercase or lowercase, basic or wildcard) and then curly braces with quantity specifier.
The following syntax constructions are allowed:

:code:`a{*}` - any number of repeats, from 1 to the entire sequence

:code:`a{:}` - same as the above

:code:`a{14}` - fixed number of repeats

:code:`a{3:6}` - specified interval of allowed repeats, interval borders are inclusive

:code:`a{:5}` - interval from 1 to specified number, inclusive

:code:`a{4:}` - interval from specified number (inclusive) to the entire sequence

**Special Case:** if :code:`n` or :code:`N` nucleotide is used before curly brackets, indels and pattern overlaps
(see :code:`--max-overlap` parameter below) are disabled, so lowercase :code:`n` and uppercase :code:`N` are
equivalent when used before curly brackets.

Symbols :code:`^` and :code:`$` can be used to restrict matched sequence to start or end of the target sequence.
:code:`^` mark must be in the start of the query for the read, and it means that the query match must start from
the beginning of the read sequence. :code:`$` mark must be in the end, and it means that the query match must be in the
end of the read. Examples:

.. code-block:: text

   minnn extract --pattern "^ATTA"
   minnn extract --input R1.fastq R2.fastq --pattern "TCCNNWW$\^(G1:ATTAGACA)N{3:18}(G2:ssttggca)$"

Advanced Syntax Elements
------------------------

There are operators :code:`&`, :code:`+` and :code:`||` that can be used inside the read query.

:code:`&` operator is logical AND, it means that 2 sequences must match in any order and gap between them.
Examples:

.. code-block:: text

   minnn extract --pattern "ATTA & GACA"
   minnn extract --input R1.fastq R2.fastq --pattern "AAAA & TTTT & CCCC \ *"
   minnn extract --input R1.fastq R2.fastq --pattern "(G1:AAAA) & TTTT & CCCC \ ATTA & (G2:GACA)"

Note that :code:`AAAA`, :code:`TTTT` and :code:`CCCC` sequences can be in any order in the target to consider that the
entire query is matching. :code:`&` operator is not allowed within groups, so this example is **invalid**:

.. code-block:: text

   minnn extract --pattern "(G1:ATTA & GACA)"

:code:`+` operator is also logical AND but with order restriction. Nucleotide sequences can be matched only in
the specified order. Also, :code:`+` operator can be used within groups. Note that in this case the matched group will
also include all nucleotides between matched operands. Examples:

.. code-block:: text

   minnn extract --pattern "(G1:ATTA + GACA)"
   minnn extract --input R1.fastq R2.fastq --pattern "(G1:AAAA + TTTT) + CCCC \ ATTA + (G2:GACA)"

:code:`||` operator is logical OR. It is not allowed within groups, but groups with the same name are allowed
inside operands of :code:`||` operator. Note that if a group is present in one operand of :code:`||` operator and
missing in another operand, this group may appear not matched in the output while the entire query is matched.
Examples:

.. code-block:: text

   minnn extract --pattern "^AAANNN(G1:ATTA) || ^TTTNNN(G1:GACA)"
   minnn extract --input R1.fastq R2.fastq --pattern "(G1:AAAA) || TTTT || (G1:CCCC) \ ATTA || (G2:GACA)"

:code:`+`, :code:`&` and :code:`||` operators can be combined in single query. :code:`+` operator has the highest
priority, then :code:`&`, and :code:`||` has the lowest. Read separator (``\``) has lower priority than all these
3 operators. To change the priority, square brackets :code:`[]` can be used. Examples:

.. code-block:: text

   minnn extract --pattern "^[AAA & TTT] + [GGG || CCC]$"
   minnn extract --input R1.fastq R2.fastq --pattern "[(G1:ATTA+GACA)&TTT]+CCC\(G2:AT+AC)"

Square brackets can be used to create sequences of patterns. Sequence is special pattern that works like :code:`+`
but with penalty for gaps between patterns. Examples of sequence pattern:

.. code-block:: text

   minnn extract --pattern "[AAA & TTT]CCC"
   minnn extract --input R1.fastq R2.fastq --pattern "[(G1:ATTA+GACA)][(G2:TTT)&ATT]\*"

Also square brackets allow to set separate score threshold for the query inside brackets. This can be done by writing
score threshold value followed by :code:`:` after opening bracket. Examples:

.. code-block:: text

   minnn extract --pattern "[-14:AAA & TTT]CCC"
   minnn extract --input R1.fastq R2.fastq --pattern "[0:(G1:ATTA+GACA)][(G2:TTT)&ATT]\[-25:c{*}]"

Matched operands of :code:`&`, :code:`+` and sequence patterns can overlap, but overlaps add penalty to match score.
You can control maximum overlap size and overlapping letter penalty by :code:`--max-overlap` and
:code:`--single-overlap-penalty` parameters. :code:`-1` value for :code:`--max-overlap` parameters means no restriction
on maximum overlap size.

**Important:** parentheses that used for groups are not treated as square brackets; instead, they treated as group
edges attached to nucleotide sequences. So, the following examples are different: first example creates sequence
pattern and second example adds end of :code:`G1` and start of :code:`G2` to the middle of sequence :code:`TTTCCC`.

.. code-block:: text

   minnn extract --pattern "[(G1:AAA+TTT)][(G2:CCC+GGG)]"
   minnn extract --pattern "(G1:AAA+TTT)(G2:CCC+GGG)"

If some of nucleotides on the edge of nucleotide sequence can be cut without gap penalty, tail cut pattern can be used.
It looks like repeated :code:`<` characters in the beginning of the sequence, or repeated :code:`>` characters in
the end of the read, or single :code:`<` or :code:`>` character followed by curly braces with number of
repeats. It is often used with :code:`^`/:code:`$` marks. Examples:

.. code-block:: text

   minnn extract --input R1.fastq R2.fastq --pattern "^<<<ATTAGACA>>$\[^<TTTT || ^<<CCCC]"
   minnn extract --input R1.fastq R2.fastq --pattern "<{6}ACTCACTCGC + GGCTCGC>{2}$\<<AATCC>"

**Important:** :code:`<` and :code:`>` marks belong to nucleotide sequences and not to complex patterns, so square
brackets between :code:`<` / :code:`>` and nucleotide sequences are **not** allowed. Also, the following examples are
different: in first example edge cut applied only to the first operand, and in second example - to both operands.

.. code-block:: text

   minnn extract --pattern "<{3}ATTA & GACA"
   minnn extract --pattern "<{3}ATTA & <{3}GACA"

High Level Logical Operators
----------------------------

There are operators :code:`~`, :code:`&&` and :code:`||` that can be used with full multi-read queries. Note that
:code:`||` operator have the same symbol as read-level OR operator, so square brackets must be used to use
high level :code:`||`.

:code:`||` operator is high-level OR. Groups with the same name are allowed in different operands of this operator,
and if a group is present in one operand of :code:`||` operator and missing in another operand, this group may appear
not matched in the output while the entire query is matched. Examples:

.. code-block:: text

   minnn extract --pattern "[AA\*\TT] || [*\GG\CG]" --input R1.fastq R2.fastq R3.fastq
   minnn extract --pattern "[^(G1:AA) + [ATTA || GACA]$ \ *] || [AT(G1:N{:8})\(G2:AATGC)]" --input R1.fastq R2.fastq

:code:`&&` operator is high-level AND. For AND operator it is not necessary to enclose multi-read query in square
brackets because there is no ambiguity. Groups with the same name are **not** allowed in different operands of
:code:`&&` operator. Examples:

.. code-block:: text

   minnn extract --pattern "AA\*\TT && *\GG\CG" --input R1.fastq R2.fastq R3.fastq
   minnn extract --pattern "^(G1:AA) + [ATTA || GACA]$ \ * && AT(G2:N{:8})\(G3:AATGC)" --input R1.fastq R2.fastq

:code:`~` is high-level NOT operator with single operand. It can sometimes be useful with single-read queries to
filter out wrong data. Groups are **not** allowed in operand of :code:`~` operator.

.. code-block:: text

   minnn extract --pattern "~ATTAGACA"
   minnn extract --pattern "~[TT \ GC]" --input R1.fastq R2.fastq

**Important:** :code:`~` operator always belongs to multi-read query that includes all input reads, so this example
is **invalid**:

.. code-block:: text

   minnn extract --pattern "[~ATTAGACA] \ TTC" --input R1.fastq R2.fastq

Instead, this query can be used:

.. code-block:: text

   minnn extract --pattern "~[ATTAGACA \ *] && * \ TTC" --input R1.fastq R2.fastq

Note that if :code:`--try-reverse-order` argument is specified, reads will be swapped synchronously for all multi-read
queries that appear as operands in the entire query, so this query will never match:

.. code-block:: text

   minnn extract --pattern "~[ATTA \ *] && ATTA \ *" --input R1.fastq R2.fastq

Square brackets are not required for :code:`~` operator, but recommended for clarity if input contains more than
1 read. :code:`~` operator have lower priority than ``\``; :code:`&&` has lower priority than :code:`~`, and
high-level :code:`||` has lower priority than :code:`&&`. But remember that high-level :code:`||` requires to enclose
operands or multi-read blocks inside operands into square brackets to avoid ambiguity with read-level OR operator.

Square brackets with score thresholds can be used with high-level queries too:

.. code-block:: text

   minnn extract --pattern "~[0: ATTA \ GACA && * \ TTT] || [-18: CCC \ GGG]" --input R1.fastq R2.fastq
