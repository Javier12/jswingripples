package org.incha.core.search;

/**
 * Created by fcocl_000 on 07-05-2016.
 * Class containing constants for the Indexer and Searcher classes.
 */
class LuceneConstants {
    static final String CONTENTS = "contents";
    static final String FILE_NAME = "filename";
    static final String FILE_PATH = "filepath";
    static final String INDEX_DIRECTORY_PATH = System.getProperty("user.dir") + "\\.SearchIndexes";
    static final int MAX_RESULTS_ITEMS = 10000;
}
