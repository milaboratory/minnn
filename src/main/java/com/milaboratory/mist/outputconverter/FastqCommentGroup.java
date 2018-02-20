package com.milaboratory.mist.outputconverter;

import com.milaboratory.core.Range;
import com.milaboratory.core.sequence.NSequenceWithQuality;

final class FastqCommentGroup implements Comparable<FastqCommentGroup> {
    private final String name;
    private final boolean matched;
    private final boolean insideMain;
    private final String sequence;
    private final String quality;
    private final String from;
    private final String to;

    FastqCommentGroup(String name) {
        this(name, false, false, null, null);
    }

    FastqCommentGroup(String name, NSequenceWithQuality value) {
        this(name, true, false, value, null);
    }

    FastqCommentGroup(String name, boolean matched, boolean insideMain, NSequenceWithQuality value, Range range) {
        this.name = name;
        this.matched = matched;
        this.insideMain = insideMain;
        if (matched) {
            this.sequence = value.getSequence().toString();
            this.quality = value.getQuality().toString();
            if (insideMain) {
                this.from = Integer.toString(range.getLower());
                this.to = Integer.toString(range.getUpper());
            } else {
                this.from = null;
                this.to = null;
            }
        } else {
            this.sequence = null;
            this.quality = null;
            this.from = null;
            this.to = null;
        }
    }

    @Override
    public int compareTo(FastqCommentGroup otherGroup) {
        return name.compareTo(otherGroup.name);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FastqCommentGroup that = (FastqCommentGroup)o;
        return name.equals(that.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    StringBuilder getDescription() {
        StringBuilder description = new StringBuilder();
        description.append(name);
        if (matched) {
            description.append('~');
            description.append(sequence);
            description.append('~');
            description.append(quality);
            if (insideMain) {
                description.append('{');
                description.append(from);
                description.append('~');
                description.append(to);
                description.append('}');
            }
        }
        description.append('|');
        return description;
    }
}
