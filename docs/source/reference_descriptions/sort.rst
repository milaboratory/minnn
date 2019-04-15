Sort action is used to sort reads by contents (nucleotide sequences) of specified groups. Merged sorting algorithm is
used for faster processing of large amounts of data. :code:`--groups` argument is mandatory, there must be space
separated list of groups, and sorting will be performed by contents of these groups. Order of groups in this list
determines the priority: reads will be compared by contents of 1st group in the list, if they are equal, then by 2nd
etc. The information about groups by which the sorting was performed is saved in output file, so warnings can be
displayed if :ref:`consensus` or :ref:`consensus-dma` action is used with unsorted groups.

**Important:** sort action must be used before :ref:`consensus` or :ref:`consensus-dma` action with the same groups in
:code:`--groups` argument as in consensus action, otherwise consensus calculation will consume much more memory!

Sort action must be used after :ref:`correct` action, and not before it, because correcting barcodes will
cause groups to be unsorted again. However, if correcting barcodes is not needed, sort action can be used right
after :ref:`extract` or :ref:`filter` action. Also, if :ref:`correct` action is used with :code:`--primary-groups`
argument, it is recommended to make additional sorting by primary groups before correction because correction by
unsorted primary groups is much slower and memory consuming.

:code:`--input` and :code:`--output` arguments are optional, and if they are missing, stdin and stdout will be used
instead of input and output files.

Examples for sort action:

.. code-block:: text

   minnn sort --groups UMI --input corrected.mif --output sorted.mif
   xzcat data.mif.xz | minnn sort --groups G1 G3 G2 | xz > sorted_data.mif.xz

:code:`--chunk-size` argument sets the chunk size in bytes for merged sorting. Too large chunks can cause out of
memory errors, and too small chunks can lead to poor performance. Default value :code:`-1` means automatically
calculate chunk size by input file size.

Command line arguments reference for sort action:
