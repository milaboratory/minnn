MifInfo action is used to show information about MIF file: MiNNN version used to create the file, number of targets
(:code:`R1`, :code:`R2` etc), number of reads, original number of reads, group names, corrected and sorted groups.
Original number of reads is number of reads in original data, before Extract action.

Input file in MIF format must be passed as argument for MifInfo action.

:code:`--no-reads-count`, :code:`--quick` or :code:`-q` argument means to display only information from MIF header,
so number of reads will not be displayed (because counting reads requires to read the entire MIF file); original
number of reads will also not be displayed, because it is contained in MIF footer. Running MifInfo with this argument
is much faster for big MIF files.

Examples for mif-info action:

.. code-block:: text

   minnn mif-info --quick data.mif
   minnn mif-info consensus.mif --report consensus-mif-info.txt

Command line arguments reference for mif-info action:
