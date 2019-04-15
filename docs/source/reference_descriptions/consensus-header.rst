Consensus actions are used to calculate consensus sequences for all combinations of barcode values. They also allow to
find multiple consensuses in the same combination of barcodes if there are multiple sequences with the same barcodes
in the data.

**Important:** :ref:`sort` action must be used before any consensus action with the same groups in :code:`--groups`
argument as in consensus action, otherwise consensus calculation will consume much more memory!
