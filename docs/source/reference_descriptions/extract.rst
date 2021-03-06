Extract action is used to process reads from FASTQ or MIF files and extract information about barcodes. It works
with *patterns*: query strings that specify which information we want to extract. Extract action patterns are similar
to regular expressions, but with specific elements for nucleotide sequence processing and barcode extraction tasks.
Details about pattern syntax can be found in :ref:`pattern_syntax` section.

:code:`--pattern` is the required argument for extract action; pattern query must be specified after it in double
quotes :code:`""`. :code:`--input` and :code:`--output` arguments are optional, but in most cases they must be
specified. Missing :code:`--input` argument means that input data will come from stdin, which is useful when working
with pipes. Note that only single-read FASTQ file can be passed from stdin. Also, MIF file (single-read or multi-read)
can be passed from stdin, but note that :code:`--input-format MIF` argument must always be present when using extract
action with input from MIF. If :code:`--output` argument is missing, data will be written to stdout. Examples:

.. code-block:: text

   minnn extract --input R1.fastq R2.fastq --output extracted.mif --pattern "(SB1:NNN)atta \ (SB2:NNN)gaca"
   xzcat data.mif.xz | minnn extract --input-format MIF --pattern "ATCC\*" | xz > extracted.mif.xz
   minnn extract --input test.mif --input-format MIF --pattern "(UMI:^N{:8})" --output test_umi.mif

**Important:** number of reads in the specified pattern must be equal to number of reads in the input data.
For example, if there are 2 FASTQ files in the input, there must be a read separator (``\``) in the pattern with
queries for :code:`R1` and :code:`R2`.

By default, if there is more than 1 read in the input, extract action will check input reads in order they specified in
:code:`--input` argument, or if input file is MIF, then in order they saved in MIF file. If :code:`--try-reverse-order`
argument is specified, it will also try the combination with 2 swapped last reads (for example, if there are 3 reads,
it will try :code:`R1, R2, R3` and :code:`R1, R3, R2` combinations), and then choose the match with better score.
This will be done for each multi-read sequence from the input.

Extract action uses bitap algorithm to quickly search nucleotide sequences from pattern in the target, and then
it uses aligner to align pattern sequence with found section of the target and calculate match score. You can set
maximum number of errors for bitap matcher with :code:`--bitap-max-errors` argument. Arguments :code:`--match-score`,
:code:`--mismatch-score`, :code:`--uppercase-mismatch-score` and :code:`--gap-score` set scoring parameters for
the aligner. You can read details about the difference between uppercase and lowercase letters in :ref:`pattern_syntax`
section.

There are other arguments that affect match scoring: :code:`--good-quality-value`, :code:`--bad-quality-value` and
:code:`--max-quality-penalty` set penalties for bad quality nucleotides in the target. Good and better quality letter
has no penalty, bad and worse quality letter has maximum specified penalty. Also, there are
:code:`--single-overlap-penalty` and :code:`--max-overlap` arguments, you can read about them in :ref:`pattern_syntax`
section.

:code:`--score-threshold` argument is used to set the score threshold: matches with lower score will not go to the
output. If both :code:`--score-threshold` and :code:`--match-score` are :code:`0`, only perfect matches without
penalties will go to the output.

Extract action uses internal heuristics to speed up the search and reduce the number of combinations to check when
using pattern sequences and logical operators. This can produce wrong results in rare cases. There is the option
:code:`--fair-sorting` to always do full search, but this is much slower than default unfair sorting.

:code:`--threads` option sets the number of threads for pattern matching. By default, it is equal to available
number of CPU cores.

:code:`--not-matched-output` argument allows to write not matched reads to the separate MIF file. By default (if this
argument is not present) not matched reads will not be written anywhere.

Sometimes read description contain barcodes or other nucleotide information. Extract action allows to parse that
information from description and save it as groups in the output. Syntax for description groups parsing:
:code:`--description-group GROUPNAME='regular_expression'`. :code:`GROUPNAME` is a group name where nucleotide sequence
will be saved. It must **not** duplicate any group name from the pattern or built-in groups :code:`R1`, :code:`R2` etc.
Multiple :code:`description-group` arguments can be specified. :code:`regular_expression` is common java regular
expression (**not** the pattern syntax), and it must always be in single quotes :code:`''`. Regular expressions will
be applied to read descriptions of all reads (first :code:`R1`, then :code:`R2`, :code:`R3` etc), and then there will
be attempt to parse match as nucleotide sequence. If valid nucleotide sequence will not be found by this regexp in any
read description, extract action will stop with error. Usage examples for :code:`--description-group` arguments:

.. code-block:: text

   minnn extract --input R1.fastq R2.fastq --output extracted.mif --pattern "*\*" --description-group G1='ATG.{10}'
   minnn extract --pattern "(G1:ATTN{10})" --description-group G2='^.{12}' --description-group G3='(?<=\=)ATTA.*(?=\;)'

:code:`--description-group` arguments also support parsing sequences with qualities. If you specify 2 named groups
:code:`seq` and :code:`qual` in regexp query, contents of these matched groups will be parsed as sequence and quality
for this description group. Usage examples:

.. code-block:: text

   minnn extract --pattern "*" --description-group G1='UMI~(?<seq>.*?)~(?<qual>.*?)\{'
   minnn extract --pattern "(G1:NNTTA)" --description-group G2='^(?<qual>.*?)~{5}(?<seq>[a-zA-Z]*)'

Command line arguments reference for extract action:
