========
Routines
========

.. _barcode_extraction:

Barcode extraction
------------------
Barcode extraction can be performed with :ref:`extract` action. Typical case is when we have a pair of FASTQ files
with :code:`R1` and :code:`R2` reads that contain barcodes. Main task here is to create pattern query for
:code:`extract` action, and barcodes will be extracted from sequences by this pattern. Patterns are similar to regular
expressions, but with some features specific for nucleotide sequences. Detailed description of pattern syntax is in
:ref:`pattern_syntax` section. There are examples of patterns for some simple cases. In these examples we extract
barcodes from :code:`data-R1.fastq` and :code:`data-R2.fastq` files and write results to :code:`barcodes-R1.fastq`
and :code:`barcodes-R2.fastq` files. :code:`extract` action writes output data in MIF format, so we use
:ref:`mif2fastq` action to convert it to FASTQ format. Extracted barcodes will be in read description lines of output
FASTQ files.

**Example 1.** Barcode is first 8 nucleotides of :code:`R1`:

.. code-block:: text

   minnn extract --pattern "^(barcode:N{8})\*" --input data-R1.fastq data-R2.fastq --output extracted.mif
   minnn mif2fastq --input extracted.mif --group R1=barcodes-R1.fastq --group R2=barcodes-R2.fastq

**Example 2.** There are 2 barcodes, first starting with :code:`ATT` and ending with :code:`AAA`, with length 9,
and second starting with :code:`GCC` and ending with :code:`TTT`, with length 12. Swapping of :code:`R1` and :code:`R2`
is not allowed, first barcode is always in :code:`R1` and second in :code:`R2`:

.. code-block:: text

   minnn extract --pattern "(B1:ATTNNNAAA)\(B2:GCCN{6}TTT)" --input data-R1.fastq data-R2.fastq --output extracted.mif
   minnn mif2fastq --input extracted.mif --group R1=barcodes-R1.fastq --group R2=barcodes-R2.fastq

**Example 3.** Good sequence starts with :code:`ATTAGACA`, and first 5 nucleotides can be possibly cut; and if sequence
starts with something else, we want to skip it. First barcode with length 5 is immediately after :code:`ATTAGACA`,
then there must be :code:`GGC` and any 5 nucleotides, and then the second barcode starting with :code:`TTT` with
length 12. Also, good sequence must end with :code:`TTAGC`, and last 2 nucleotides can be possibly cut. :code:`R1` and
:code:`R2` can be in reverse order in some reads. And we want to allow substitutions and indels (but with score
penalties) inside sequences:

.. code-block:: text

   minnn extract --pattern "^<{5}attagaca(B1:n{5})gccn{5}(B2:tttn{9})+ttagc>>$\*" --try-reverse-order --score-threshold -25 --input data-R1.fastq data-R2.fastq --output extracted.mif
   minnn mif2fastq --input extracted.mif --group R1=barcodes-R1.fastq --group R2=barcodes-R2.fastq

.. _demultiplexing:

Demultiplexing
--------------
Demultiplexing is splitting one dataset into multiple datasets by barcode values. Demultiplexing can be performed with
:ref:`demultiplex` action. It works with MIF files, so if you want to demultiplex data from FASTQ files, you need to
extract barcodes and convert data to MIF format first, see :ref:`barcode_extraction` section. Output MIF files can be
converted to FASTQ with :ref:`mif2fastq` action. There are 2 common demultiplexing tasks: split file by barcode values
and extract samples with specified combinations of barcode values.

**Example 1.** Split data by unique UMI values. We have input data where UMI is first 6 nucleotides, and we want to
perform barcodes correction (see :ref:`correcting_umi_sequence` section) before demultiplexing.

.. code-block:: text

   minnn extract --pattern "^(UMI:N{6})\*" --input data-R1.fastq data-R2.fastq --output extracted.mif
   minnn sort --groups UMI --input extracted.mif --output sorted.mif
   minnn correct --groups UMI --input sorted.mif --output corrected.mif
   minnn demultiplex --by-barcode UMI corrected.mif --demultiplex-log demultiplex.log

Note that splitting data by unique UMI values can result in very big number of output files!

**Example 2.** Input data is like in previous example, but we will extract only data with the following UMI values:
:code:`AATTTT`, :code:`AAAGGG`, :code:`CCCCCC`, :code:`AGACAT`, :code:`TTTTTA`, :code:`TTTTTG`. For this task we will
create the following sample file :code:`umi_samples.txt`:

.. code-block:: text

   Sample UMI
   value_AATTTT AATTTT
   value_AAAGGG AAAGGG
   value_CCCCCC CCCCCC
   value_AGACAT AGACAT
   value_TTTTTA TTTTTA
   value_TTTTTG TTTTTG

And then issue the following commands:

.. code-block:: text

   minnn extract --pattern "^(UMI:N{6})\*" --input data-R1.fastq data-R2.fastq --output extracted.mif
   minnn sort --groups UMI --input extracted.mif --output sorted.mif
   minnn correct --groups UMI --input sorted.mif --output corrected.mif
   minnn demultiplex --by-sample umi_samples.txt corrected.mif --demultiplex-log demultiplex.log

**Example 3.** We extracted sequence barcodes with :ref:`extract` action into :code:`extracted.mif` file, and we named
these barcodes :code:`SB1` and :code:`SB2`. Now we want to put sequences with specified combinations of :code:`SB1`
and :code:`SB2` into separate MIF files. There we will use sample file :code:`samples.txt` with multiple barcodes:

.. code-block:: text

   Sample SB1 SB2
   sample1 ATTAGACA CCCCCC
   sample2 ATTAGACA GGGGGG
   sample3 ATTACCCC TTTTTT

And then issue the following command:

.. code-block:: text

   minnn demultiplex --by-sample samples.txt extracted.mif --demultiplex-log demultiplex.log

.. _correcting_umi_sequence:

Correcting UMI sequence
-----------------------
UMI sequences in input data often contain substitutions and indels, and we want to correct such errors to cluster
sequences by UMI without creating extra clusters for variants with errors. Barcodes correction is performed with
:ref:`correct` action. It is performed after barcode extraction, see :ref:`barcode_extraction` section. **Important:**
file must be sorted with :ref:`sort` action before using :code:`correct` action, and :code:`--groups` argument in
:code:`sort` action must contain the same groups in the same order as in :code:`correct` action. In common cases
you can use the default settings for :code:`sort` and :code:`correct` actions and specify only input and output files
and list of barcode names in :code:`--groups` argument:

.. code-block:: text

   minnn sort --groups UMI --input extracted.mif --output sorted.mif
   minnn correct --groups UMI --input sorted.mif --output corrected.mif

You can convert output MIF file into FASTQ with :ref:`mif2fastq` action, or watch statistics for barcode values
and positions with :ref:`stat-groups` and :ref:`stat-positions` actions. If you want to specify custom settings for
barcode correction, see the description of available options on :ref:`correct` action page.

**Example.** We want to extract and correct UMI in pair of FASTQ files that contain :code:`R1` and :code:`R2`.
We know that UMI is first 6 nucleotides of the read, and it starts with :code:`ATT`. Then we use the following
commands:

.. code-block:: text

   minnn extract --pattern "^(UMI:ATTNNN)\*" --input R1.fastq R2.fastq --output extracted.mif
   minnn sort --groups UMI --input extracted.mif --output sorted-UMI.mif
   minnn correct --groups UMI --input sorted-UMI.mif --output corrected-UMI.mif
   minnn mif2fastq --input corrected-UMI.mif --group R1=corrected-UMI-R1.fastq --group R2=corrected-UMI-R2.fastq

.. _consensus_assembly:

Consensus assembly
------------------
Consensus assembly consists of 6 stages:

#. Extract barcodes from raw sequences.
#. Sort sequences by barcode values to group them for further correction.
#. Correct mismatches and indels in barcodes.
#. Sort sequences by barcode values to group them for further consensus assembly.
#. Assembly consensuses for each barcode. There can be one or many consensuses for each barcode, depending on the way
   of obtaining original data.
#. Export calculated consensuses to FASTQ format.

**Example.** We have 2 FASTQ files with :code:`R1` and :code:`R2`. We want to assemble consensuses by UMI that is 8
nucleotides after first 3 nucleotides :code:`TTT`. And we know that there must be only 1 consensus for each UMI.
Then we use the following commands:

.. code-block:: text

   minnn extract --pattern "^TTT(UMI:N{8})\*" --input R1.fastq R2.fastq --output extracted.mif
   minnn sort --groups UMI --input extracted.mif --output sorted-1.mif
   minnn correct --groups UMI --input sorted-1.mif --output corrected.mif
   minnn sort --groups UMI --input corrected.mif --output sorted-2.mif
   minnn consensus --groups UMI --max-consensuses-per-cluster 1 --input sorted-2.mif --output consensus.mif
   minnn mif2fastq --input consensus.mif --group R1=consensus-R1.fastq --group R2=consensus-R2.fastq

To configure settings for consensus assembly, see the description of available options on :ref:`consensus` action page.
