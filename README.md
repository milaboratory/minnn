[![CircleCI](https://circleci.com/gh/milaboratory/minnn/tree/develop.svg?style=svg)](https://circleci.com/gh/milaboratory/minnn/tree/develop)

# MiNNN

Universal tool for processing molecular-barcoded Next Generation Sequencing data.

### Status

MiNNN is in beta, and is intended only for testing and evaluation.

Feature requests and bug reports are very welcome!

### Applications

- Data with sample barcodes
  - Perform demultiplexing by known barcode sequences
- Single-cell data
  - Correct sequencing/PCR errors in cell barcodes
  - Demultiplex the data
- Process data with Unique Molecular Identifier (UMI)
  - Correct sequencing/PCR errors in UMI
  - Assemble sequences by UMI
- Any combinations of above,

### Main features:

- Powerful syntax for barcode parsing
- Very fast barcode extraction engine (in most cases process data as fast as your file system can feed it)
- Fast and accurate algorithms for barcode clustering/correction with full support of indels
- Comprehensive sequence assemble algorithm
  - corrects both mismatches and indels
  - detects and extracts multiple sequence clusters for single UMI

### Documentation

Documentation is available at [http://minnn.readthedocs.io/](http://minnn.readthedocs.io/)

### License

```
Copyright (c) 2016-2020, MiLaboratory LLC
All Rights Reserved

Permission to use, copy, modify and distribute any part of this program for
educational, research and non-profit purposes, by non-profit institutions
only, without fee, and without a written agreement is hereby granted,
provided that the above copyright notice, this paragraph and the following
three paragraphs appear in all copies.

Those desiring to incorporate this work into commercial products or use for
commercial purposes should contact MiLaboratory LLC, which owns exclusive
rights for distribution of this program for commercial purposes, using the
following email address: licensing@milaboratory.com.

IN NO EVENT SHALL THE INVENTORS BE LIABLE TO ANY PARTY FOR DIRECT, INDIRECT,
SPECIAL, INCIDENTAL, OR CONSEQUENTIAL DAMAGES, INCLUDING LOST PROFITS,
ARISING OUT OF THE USE OF THIS SOFTWARE, EVEN IF THE INVENTORS HAS BEEN
ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

THE SOFTWARE PROVIDED HEREIN IS ON AN "AS IS" BASIS, AND THE INVENTORS HAS
NO OBLIGATION TO PROVIDE MAINTENANCE, SUPPORT, UPDATES, ENHANCEMENTS, OR
MODIFICATIONS. THE INVENTORS MAKES NO REPRESENTATIONS AND EXTENDS NO
WARRANTIES OF ANY KIND, EITHER IMPLIED OR EXPRESS, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY OR FITNESS FOR A
PARTICULAR PURPOSE, OR THAT THE USE OF THE SOFTWARE WILL NOT INFRINGE ANY
PATENT, TRADEMARK OR OTHER RIGHTS.
```
