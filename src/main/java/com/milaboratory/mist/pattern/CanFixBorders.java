package com.milaboratory.mist.pattern;

public interface CanFixBorders {
    /**
     * Set fixed left or right border for this pattern.
     *
     * @param left true for left border, false for right border
     * @param position coordinate for fixed border
     * @return copy of this pattern with fixed border
     */
    SinglePattern fixBorder(boolean left, int position);
}
