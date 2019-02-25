===========
Quick Start
===========

Example: we have a pair of FASTQ files :code:`R1.fastq` and :code:`R2.fastq` from experiment where we attached 2 sample
barcodes to sequence. We know that first sample barcode is first 5 nucleotides of sequence and 2nd barcode is
:code:`ATGNNNN`. We want to calculate single consensus for each combination of barcodes, and before this we want to
filter out sequences where first sample barcode is :code:`TTTTT` for which we know that this is garbage. Then we do the
following actions:

#. Extract barcodes from data.

   .. code-block:: text

      minnn extract --input R1.fastq R2.fastq --output extracted.mif --pattern "^(SB1:N{5}) & (SB2:ATGNNNN)\*"

   Note that extract action will search :code:`R1`, :code:`R2` combination and then try the same search with swapped
   reads :code:`R2`, :code:`R1`. Then it will choose the match with better score. This is the default behavior; if you
   want to check only :code:`R1`, :code:`R2` combination without checking reversed order, use :code:`--oriented` flag.
   Details for pattern syntax can be found in :ref:`pattern_syntax` section.
#. Correct mismatches and indels in barcodes.

   .. code-block:: text

      minnn correct --input extracted.mif --output corrected.mif --groups SB1 SB2

#. Filter out garbage reads.

   .. code-block:: text

      minnn filter --input corrected.mif --output filtered.mif "SB1~'~TTTTT'"

   Details for filter syntax can be found in :ref:`filter_syntax` section.
#. (Optionally) check statistics for collected barcodes.

   .. code-block:: text

      minnn stat-groups --input filtered.mif --output stat-groups.txt --groups SB1 SB2
      minnn stat-positions --input filtered.mif --output stat-positions.txt --groups SB2

#. Sort reads by barcode values.

   .. code-block:: text

      minnn sort --input filtered.mif --output sorted.mif --groups SB1 SB2

#. Calculate consensuses.

   .. code-block:: text

      minnn consensus --input sorted.mif --output consensus.mif --max-consensuses-per-cluster 1 --groups SB1 SB2

#. Export consensuses to FASTQ files.

   .. code-block:: text

      minnn mif2fastq --input consensus.mif --group R1=consensus-R1.fastq --group R2=consensus-R2.fastq
