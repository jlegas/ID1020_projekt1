import com.sun.org.apache.xpath.internal.operations.Or;
import se.kth.id1020.TinySearchEngineBase;
import se.kth.id1020.util.Attributes;
import se.kth.id1020.util.Document;
import se.kth.id1020.util.Word;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by justi on 2016-12-10.
 */
public class TinySearchEngine implements TinySearchEngineBase {
    public List<IndexEntry> indexedWords = new ArrayList<>();

    public class IndexEntry implements Comparable<IndexEntry> {
        public String word;
        public ArrayList<Attributes> attributes;

        public IndexEntry(Word word, Attributes attr) {
            this.word = word.word;
            this.attributes = new ArrayList<>();
            this.attributes.add(attr);
        }

        public IndexEntry(String word) {
            this.word = word;
        }

        @Override
        public int compareTo(IndexEntry entry) {
            return this.word.compareTo(entry.word);
        }
    }

    @Override
    public void insert(Word word, Attributes attr) {
        IndexEntry entry = new IndexEntry(word, attr);
        int index = Collections.binarySearch(indexedWords, entry);

        if (index >= 0) { //exists in the List
            indexedWords.get(index).attributes.add(attr);
        } else {
            indexedWords.add(-index - 1, entry);
        }
    }

    @Override
    public List<Document> search(String query) {
        List<Document> results = new ArrayList<>();
        String[] search = query.split(" ");

        if (search.length >= 4 && search[search.length - 3].equals("orderby")) { //orderby
            String[] unionSearch = new String[search.length - 3];
            for (int i = 0; i < (search.length - 3); i++) {
                unionSearch[i] = search[i];
            }
            List<SearchResult> tmp = searchArray(unionSearch);


            //orderby(property, direction, list)
            results = orderBy(search[search.length - 2], search[search.length - 1], tmp);
        } else { //simple unordered union search
            List<SearchResult> tmp = searchArray(search);
            for (SearchResult result : tmp) {
                for (Attributes attr : result.attributes) {
                    if (!results.contains(attr.document)) {
                        results.add(attr.document);
                    }
                }
            }
        }

        return results;
    }

    private List<SearchResult> searchArray(String[] queries) {
        List<SearchResult> results = new ArrayList<>();
        for (int i = 0; i < queries.length; i++) {
            List<SearchResult> temp = searchString(queries[i]);
            for (SearchResult result : temp) {
                results.add(result);
            }
        }
        return results;
    }

    private List<Document> orderBy(String by, String direction, List<SearchResult> toOrder) {
        List<OrderableSearchResult> resultToOrder = new ArrayList<>();
        List<Document> finalResults = new ArrayList<>();

        //order by popularity
        if (by.equals("popularity")) {
            //build orderable set
            for (SearchResult searchResult : toOrder) {
                for (Attributes attr : searchResult.attributes) {
                    OrderableSearchResult check = new OrderableSearchResult(attr.document.popularity, attr.document);
                    if (!resultToOrder.contains(check)) {
                        resultToOrder.add(check);
                    }
                }
            }
        }

        //order by occurence
        else if (by.equals("count")) {
            //build orderable set
            Boolean increasedCount;
            for (SearchResult searchResult : toOrder) {
                for (Attributes attr : searchResult.attributes) {
                    increasedCount = false;
                    OrderableSearchResult check = new OrderableSearchResult(1, attr.document);
                    for (OrderableSearchResult increaseCount : resultToOrder) {
                        if (check.document.name.equals(increaseCount.document.name)) {
                            increaseCount.order++;
                            increasedCount = true;
                        }
                    }
                    if (!increasedCount) {
                        resultToOrder.add(check);
                    }
                }
            }
        }

        //order by count
        else if (by.equals("occurence")) {
            //build orderable set
            for (SearchResult searchResult : toOrder) {
                for (Attributes attr : searchResult.attributes) {
                    OrderableSearchResult check = new OrderableSearchResult(attr.occurrence, attr.document);
                    if (!resultToOrder.contains(check)) {
                        resultToOrder.add(check);
                    }
                }
            }
        }

        //order the orderable result set
        if (direction.equals("asc")) {
            BubbleSort.sort(resultToOrder, 1);
        } else if (direction.equals("desc")) {
            BubbleSort.sort(resultToOrder, 2);
        }

        //construct final deliverable result list containing all the documents
        for (OrderableSearchResult orderedResult : resultToOrder) {
            if (!finalResults.contains(orderedResult.document)) {
                finalResults.add(orderedResult.document);
            }
        }

        return finalResults;
    }

    private List<SearchResult> searchString(String query) {
        //use binary search
        String[] indexArray = new String[(indexedWords.size())];
        int i = 0;
        for (IndexEntry entry : indexedWords) {
            indexArray[i] = entry.word;
            i++;
        }
        int index = BinarySearch.search(query, indexArray);

        //use Java's binary search
//        int index = Collections.binarySearch(indexedWords, new IndexEntry(query));

        if (index >= 0) { //exists in the List
            List<SearchResult> list = new ArrayList<>();
            //return unique doc list
//            for (Attributes attr : indexedWords.get(index).attributes) {
//                if (!list.contains(attr.document)) {
//                    list.add(attr.document);
//                }
//            }
            //return all doc list
            SearchResult searchResult = new SearchResult(query);
            for (Attributes attr : indexedWords.get(index).attributes) {
                searchResult.attributes.add(attr);
            }
            list.add(searchResult);
            return list;
        } else {
            return null;
        }
    }

    public class SearchResult {
        //public String word;
        public List<Attributes> attributes;

        public SearchResult(String word) {
            //this.word = word;
            this.attributes = new ArrayList<>();
        }
    }

    public class OrderableSearchResult {
        public int order;
        public Document document;

        public OrderableSearchResult(int order, Document document) {
            this.order = order;
            this.document = document;
        }
    }

}
