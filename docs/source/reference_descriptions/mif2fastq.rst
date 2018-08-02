Mif2fastq action is used to convert MIF file to FASTQ format. It writes information about capture groups to read
headers, and optionally allows to add original headers (that were in FASTQ files passed to :ref:`extract` action)
to the start of output header comments. Also mif2fastq action allows to save capture groups contents as separate reads.

Group options are mandatory, and they specify what output files will be created. Group names may be built-in groups
:code:`R1`, :code:`R2`, :code:`R3` etc that contain the entire reads, also group names may be names of capture
groups created by :ref:`extract` action, and also there may be built-in groups :code:`CR1`, :code:`CR2`, :code:`CR3`
etc if the MIF file was created by :ref:`consensus` action with :code:`--consensuses-to-separate-groups` parameter.
Group options format is
:code:`--group-GROUPNAME1 filename1.fastq --group-GROUPNAME2 filename2.fastq --group-GROUPNAME3 filename3.fastq`, and
there can be any number of pairs of groups and corresponding file names, but at least 1 pair must be specified.

:code:`--input` argument is optional, and if it's missing, stdin will be used instead of input file.

Examples for mif2fastq action:

.. code-block:: console

   mist mif2fastq --input corrected.mif --copy-original-headers --group-R1 R1.fastq --group-R2 R2.fastq
   xzcat data.mif.xz | mist mif2fastq --group-R1 data-R1.fastq --group-R2 data-R2.fastq --group-UMI data-UMI.fastq
   mist mif2fastq --input consensus.mif --group-R1 consensus-R1.fastq --group-R2 consensus-R2.fastq
   mist mif2fastq --input consensus-separate.mif --group-R1 data-R1.fastq --group-CR1 consensus-R1.fastq

:code:`--copy-original-headers` parameter specifies to copy original headers (that were in FASTQ files passed to
:ref:`extract` action) to the start of output header comments. If it isn't specified, only mist comments will be in
output FASTQ files.

**Output FASTQ files comments format:**

:code:`@[original_headers]~group_descriptions~[||~]`

:code:`~` symbol is used as separator between sections, :code:`|` symbol is used as separator between groups
inside :code:`group_descriptions` section. :code:`original_headers` section is present only if
:code:`--copy-original-headers` parameter is specified. :code:`||~` token is present only if this was a reversed match
(with swapped :code:`R1` and :code:`R2`) in :ref:`extract` action. In reversed matches :code:`R1` read in extract
action input file becomes :code:`R2` in mif2fastq output file and vice versa, so :code:`||~` token is used as
notification for reversed matches.

:code:`group_descriptions` section contains descriptions of all capture groups except built-in groups :code:`R1`,
:code:`R2`, :code:`R3` etc. Groups are separated by :code:`|` token. There can be 3 types of groups in this section:

1. Group that is inside *current target*. Format: :code:`GROUPNAME~SEQ~QUAL~{FROM~TO}`. Example:
   :code:`G1~ATTAGGG~111BFF1~{10~17}`. :code:`GROUPNAME` is capture group name, :code:`SEQ` is target sequence where
   this group matched, :code:`QUAL` is quality of this target fragment, :code:`FROM` is start coordinate in the current
   target (inclusive), :code:`TO` is end coordinate in the current target (exclusive). *Current target* is read
   corresponding to the current output FASTQ file. It can be built-in group :code:`R1`, :code:`R2` etc that represents
   the entire input read; it can be overridden :code:`R1`, :code:`R2` etc if there was override for these built-in
   groups in :ref:`extract` action query; and it can be capture group used as output read in group options of mif2fastq
   action.
2. Group that is matched but not inside current target. Format: :code:`GROUPNAME~SEQ~QUAL`. Example:
   :code:`UMI~AAAGGCCC~\\\111C`. This format is used for groups that matched somewhere in another read or not in bounds
   of current target.
3. Not matched group. Format: :code:`GROUPNAME`. Example: :code:`SB1`. Used for not matched groups.
   :ref:`pattern_syntax` page contains the information where such groups can appear.

Command line arguments reference for mif2fastq action:
