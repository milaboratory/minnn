========
Routines
========

.. _barcode_extraction:

Barcode extraction
------------------
Barcode extraction can be performed with :ref:`extract` action. Typical case is when we have a pair of FASTQ files
with :code:`R1` and :code:`R2` reads that contain barcodes. Main task here is to create pattern query for extract
action, and barcodes will be extracted from sequences by this pattern. Patterns are similar to regular expressions,
but with some features specific for nucleotide sequences. Detailed description of pattern syntax is in
:ref:`pattern_syntax` section. There are examples of patterns for some simple cases. In these examples we extract
barcodes from :code:`data-R1.fastq` and :code:`data-R2.fastq` files and write results to :code:`barcodes-R1.fastq`
and :code:`barcodes-R2.fastq` files. Extract action writes output data in MIF format, so we use :ref:`mif2fastq`
action to convert it to FASTQ format. Extracted barcodes will be in read description lines of output FASTQ files.

**Example 1.** Barcode is first 8 nucleotides of the sequence:

.. code-block:: console

   mist extract --pattern "^(barcode:N{8})\*" --input data-R1.fastq data-R2.fastq --output extracted.mif
   mist mif2fastq --input extracted.mif --group-R1 barcodes-R1.fastq --group-R2 barcodes-R2.fastq

**Example 2.** There are 2 barcodes, first starting with :code:`ATT` and ending with :code:`AAA`, with length 9,
and second starting with :code:`GCC` and ending with :code:`TTT`, with length 12. Reads are oriented (swapping of
:code:`R1` and :code:`R2` is not allowed), and first barcode is always in :code:`R1` and second in :code:`R2`:

.. code-block:: console

   mist extract --pattern "(B1:ATTNNNAAA)\(B2:GCCN{6}TTT)" --oriented --input data-R1.fastq data-R2.fastq --output extracted.mif
   mist mif2fastq --input extracted.mif --group-R1 barcodes-R1.fastq --group-R2 barcodes-R2.fastq

**Example 3.** Good sequence starts with :code:`ATTAGACA`, and first 5 nucleotides can be possibly cut; and if sequence
starts with something else, we want to skip it. First barcode with length 5 is immediately after :code:`ATTAGACA`,
then there must be :code:`GGC` and any 5 nucleotides, and then the second barcode starting with :code:`TTT` with
length 12. Also, good sequence must end with :code:`TTAGC`, and last 2 nucleotides can be possibly cut. And we want
to allow mismatches and indels (but with score penalties) inside sequences:

.. code-block:: console

   mist extract --pattern "^<{5}attagaca(B1:n{5})gccn{5}(B2:tttn{9})+ttagc>>$\*" --score-threshold -25 --input data-R1.fastq data-R2.fastq --output extracted.mif
   mist mif2fastq --input extracted.mif --group-R1 barcodes-R1.fastq --group-R2 barcodes-R2.fastq

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

.. code-block:: console

   mist extract --pattern "^(UMI:N{6})\*" --input data-R1.fastq data-R2.fastq --output extracted.mif
   mist correct --groups UMI --input extracted.mif --output corrected.mif
   mist demultiplex --by-barcode UMI corrected.mif

Note that splitting data by unique UMI values can result in very big number of output files!

**Example 2.** Input data is like in previous example, but we will extract only data with the following UMI values:
:code:`AATTTT`, :code:`AAAGGG`, :code:`CCCCCC`, :code:`AGACAT`, :code:`TTTTTA`, :code:`TTTTTG`. For this task we will
create the following sample file :code:`umi_samples.txt`:

.. code::

   Sample UMI
   value_AATTTT AATTTT
   value_AAAGGG AAAGGG
   value_CCCCCC CCCCCC
   value_AGACAT AGACAT
   value_TTTTTA TTTTTA
   value_TTTTTG TTTTTG

And then issue the following commands:

.. code-block:: console

   mist extract --pattern "^(UMI:N{6})\*" --input data-R1.fastq data-R2.fastq --output extracted.mif
   mist correct --groups UMI --input extracted.mif --output corrected.mif
   mist demultiplex --by-sample umi_samples.txt corrected.mif

**Example 3.** We extracted sequence barcodes with :ref:`extract` action into :code:`extracted.mif` file, and we named
these barcodes :code:`SB1` and :code:`SB2`. Now we want to put sequences with specified combinations of :code:`SB1`
and :code:`SB2` into separate MIF files. There we will use sample file :code:`samples.txt` with multiple barcodes:

.. code::

   Sample SB1 SB2
   sample1 ATTAGACA CCCCCC
   sample2 ATTAGACA GGGGGG
   sample3 ATTACCCC TTTTTT

And then issue the following command:

.. code-block:: console

   mist demultiplex --by-sample samples.txt extracted.mif

.. _correcting_umi_sequence:

Correcting UMI sequence
-----------------------

.. _consensus_assembly:

Consensus assembly
------------------

