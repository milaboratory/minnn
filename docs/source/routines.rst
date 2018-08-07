========
Routines
========
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

Example 1. Barcode is first 8 nucleotides of the sequence:

.. code-block:: console

   mist extract --pattern "^(barcode:N{8})\*" --input data-R1.fastq data-R2.fastq --output extracted.mif
   mist mif2fastq --input extracted.mif --group-R1 barcodes-R1.fastq --group-R2 barcodes-R2.fastq

Example 2. There are 2 barcodes, first starting with :code:`ATT` and ending with :code:`AAA`, with length 9, and second
starting with :code:`GCC` and ending with :code:`TTT`, with length 12. Reads are oriented (swapping of :code:`R1` and
:code:`R2` is not allowed), and first barcode is always in :code:`R1` and second in :code:`R2`:

.. code-block:: console

   mist extract --pattern "(B1:ATTNNNAAA)\(B2:GCCN{6}TTT)" --oriented --input data-R1.fastq data-R2.fastq --output extracted.mif
   mist mif2fastq --input extracted.mif --group-R1 barcodes-R1.fastq --group-R2 barcodes-R2.fastq

Example 3. Good sequence starts with :code:`ATTAGACA`, and first 5 nucleotides can be possibly cut; and if sequence
starts with something else, we want to skip it. First barcode with length 5 is immediately after :code:`ATTAGACA`,
then there must be :code:`GGC` and any 5 nucleotides, and then the second barcode starting with :code:`TTT` with
length 12. Also, good sequence must end with :code:`TTAGC`, and last 2 nucleotides can be possibly cut. And we want
to allow mismatches and indels (but with score penalties) inside sequences:

.. code-block:: console

   mist extract --pattern "^<{5}attagaca(B1:n{5})gccn{5}(B2:tttn{9})+ttagc>>$\*" --score-threshold -25 --input data-R1.fastq data-R2.fastq --output extracted.mif
   mist mif2fastq --input extracted.mif --group-R1 barcodes-R1.fastq --group-R2 barcodes-R2.fastq

Demultiplexing
--------------


Correcting UMI sequence
-----------------------


Consensus assembly
------------------

