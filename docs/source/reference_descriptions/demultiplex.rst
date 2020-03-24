Demultiplex action is used to filter nucleotide sequences from one MIF file into multiple MIF files, separating
sequences by barcode values or by samples.

:code:`filter_options` argument is mandatory. It must contain one or multiple filter options and input file name.
Stdin is not supported in demultiplex action. Available filter options are:

:code:`--by-barcode GROUPNAME` - split by values (nucleotide sequences) of group :code:`GROUPNAME`, one output file for
each value

:code:`--by-sample SAMPLE_FILENAME` - extract samples specified in file :code:`SAMPLE_FILENAME` (file format is
described below), one output file for each sample; and reads that didn't match any sample will be skipped

You can specify multiple :code:`--by-barcode` and :code:`--by-sample` options, but note that output file will be
created for each combination of barcode values and samples, so this command can create very big number of files!

:code:`--demultiplex-log` argument is mandatory. It specifies the name of output text file where the list of all
output files will be written. You can use the log file from previous run and add :code:`--overwrite-if-required`
argument if you want to use smart overwrite feature: overwrite output files only if the input file had changed.

Examples for demultiplex action:

.. code-block:: text

   minnn demultiplex --output-buffer-size 30000 --by-barcode SB1 corrected.mif --demultiplex-log log.txt
   minnn demultiplex --demultiplex-log 1.log --by-sample samples1.txt --by-sample samples2.txt --by-barcode UMI in.mif

**Sample file format:**

Sample file is a plain-text table with values separated with spaces or tabs. First line contains the keyword
:code:`Sample` and then group names. Other lines start with sample names and then there are values of the groups
for this sample. Multiple lines with the same sample name will be combined into one sample.

Example for sample file with single group:

.. code-block:: text

   Sample UMI
   good_sample_1 AAAA
   good_sample_1 TTTT
   good_sample_2 CCCC
   good_sample_2 AAGG
   error_value_1 GGAA
   error_value_2 AATT
   special_value TTAA

Asterisk wildcards :code:`*` can be used if any value of a group must match. Example for sample file with
multiple groups and wildcards:

.. code-block:: text

   Sample SB1 SB2 SB3
   test_sample_1_1 AAAA CTT CGCGTCT
   test_sample_1_2 * * CGCGGGG
   test_sample_1_3 AACA * CGCGTCT
   test_sample_1_4 AAAA GTT CGCGTCT

Output file names will contain input file name, and then sequence of sample names and barcode values separated with
:code:`_` token, in the same order as these sample files and barcode group names were specified in the
:code:`filter_options` argument. For example, output file names can look like
:code:`data_GGT_test_sample_1_4_TTGG.mif`, :code:`corrected_ATTAGACA.mif` or :code:`extracted_sample4.mif`. One file
will be created for each combination of barcode values and matched samples that contains at least one read.

:code:`--output-buffer-size` argument allows to set the write buffer size (in bytes) manually. This buffer size is used
for each output file, so it's better to set lower values if you intend to create a lot of output files.

Command line arguments reference for demultiplex action:
