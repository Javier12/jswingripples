package org.incha.core.search;

import java.awt.*;

/**
 * Manages search results highlight colors.
 * Created by fcocl_000 on 09-05-2016.
 */
public class Highlight {

    /**
     * Return a highlight color based on the percentage of search hits.
     * @param fileName the file to be highlighted.
     * @return the highlight color.
     */
    public static Color getColor(String fileName) {
        float hitPercentage = Searcher.getInstance().searchHits(fileName);

        if (hitPercentage == 0) return Color.CYAN;
        return Color.MAGENTA;
    }
}
