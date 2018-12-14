/*
 * Title: LIGA.java
 * Project: LIGA
 * Creator: Georgii Mikriukov
 * 2018
 */

package com.liga;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;

import com.fasterxml.jackson.core.*;
import org.apache.commons.lang3.tuple.MutablePair;

/**
 * Main class of language identification for short texts (LIGA and logLIGA).
 * Use it to train and classify texts, load and save models
 */
public class LIGA {

    private boolean debug = false;

    // confidence threshold (if lower - language is still UNKNOWN)
    private double threshold = 0.0125;
    // max recursive search depth
    private int maxSearchDepth = 1000;
    // flag of model, if true - logLIGA, false - LIGA
    private boolean logLIGA = true;
    // tree maps - we don't know how many items, natural order -> more readable, adequate memory distribution, O(log(n))
    private TreeMap<String, TreeMap<String, Integer>> nodes = new TreeMap<>();
    private TreeMap<String, TreeMap<String, TreeMap<String, Integer>>> edges = new TreeMap<>();
    private TreeMap<String, MutablePair<Integer, Integer>> counter = new TreeMap<>();

    public double getThreshold() {
        return threshold;
    }

    public void setThreshold(double threshold) {
        this.threshold = threshold;
    }

    public int getMaxSearchDepth() {
        return maxSearchDepth;
    }

    public void setMaxSearchDepth(int maxSearchDepth) {
        this.maxSearchDepth = maxSearchDepth;
    }

    public boolean isLogLIGA() {
        return logLIGA;
    }

    public void setLogLIGA(boolean logLIGA) {
        this.logLIGA = logLIGA;
    }

    public TreeMap<String, TreeMap<String, Integer>> getNodes() {
        return nodes;
    }

    public TreeMap<String, TreeMap<String, TreeMap<String, Integer>>> getEdges() {
        return edges;
    }

    public TreeMap<String, MutablePair<Integer, Integer>> getCounter() {
        return counter;
    }

    public LIGA(LIGABuilder builder){
        this.threshold = builder.threshold;
        this.maxSearchDepth = builder.maxSearchDepth;
        this.logLIGA = builder.logLIGA;
    }

    /**
     * Adds the loaded dataset to model
     *
     * @param dataset
     */
    public void addDataset(List<MutablePair<String, String>> dataset, int ngramLength){
        for (MutablePair<String, String> entry: dataset){
            addDocument(entry.right, entry.left, ngramLength);
        }
    }

    /**
     * Adds a document to the model
     *
     * @param doc         document
     * @param language    language
     * @param ngramLength length of ngram
     */
    public void addDocument(String doc, String language, int ngramLength) {
        // Minor pre-processing
        doc = Tokenizer.preprocess(doc);

        // get ngrams
        List<String> ngrams = getNgrams(doc, ngramLength);

        // adds dock if only ngrams exist
        if (!ngrams.isEmpty()) {

            // Initialize counter for language
            if (!counter.containsKey(language))
                counter.put(language, new MutablePair<Integer, Integer>(0, 0));

            String previousNgram = null;
            for (String ngram : ngrams) {
                // Add node
                addNode(ngram, language);

                // See if we have to add an edge
                if (previousNgram != null) {
                    // Add edge
                    addEdge(previousNgram, ngram, language);
                }

                previousNgram = ngram;
            }
        }
    }

    /**
     * gets ngrams of the string
     *
     * @param doc         original doc
     * @param ngramLength ngram length
     */
    private List<String> getNgrams(String doc, int ngramLength) {
        List<String> out = new LinkedList<>();
        // number of ngrams
        int num = doc.length() - (ngramLength - 1);
        // if ngrams available
        if (num > 0) {
            for (int i = 0; i < num; i++)
                out.add(doc.substring(i, i + ngramLength));
            // if ngrams anavailable, but document is not empty
        } else if (doc.length() > 0) {
            StringBuilder ngram = new StringBuilder(doc);
            while (ngram.length() < ngramLength)
                ngram.append(" ");
            out.add(ngram.toString());
        }
        return out;
    }

    /**
     * Adds a node (N-gram) to the model
     *
     * @param node     ngram label
     * @param language language
     */
    private void addNode(String node, String language) {
        // Check if this node exists
        if (!nodes.containsKey(node)) {
            // Add the node as it doesn't exist yet
            nodes.put(node, new TreeMap<String, Integer>());
            edges.put(node, new TreeMap<String, TreeMap<String, Integer>>());
        }
        // Initialize the count for this node for this language
        if (!nodes.get(node).containsKey(language))
            nodes.get(node).put(language, 0);
        // Increase the counter for this language
        nodes.get(node).put(language, nodes.get(node).get(language) + 1);

        // Update the total counter
        if (!counter.containsKey(language))
            counter.put(language, new MutablePair<Integer, Integer>(0, 0));
        counter.get(language).setLeft(counter.get(language).getLeft() + 1);
    }

    /**
     * Adds an edge between two existing nodes
     *
     * @param source   node from
     * @param target   node to
     * @param language language
     */
    private void addEdge(String source, String target, String language) {
        // Check if there is already an edge
        if (!edges.get(source).containsKey(target))
            edges.get(source).put(target, new TreeMap<String, Integer>());

        // Check if there is an edge for this language
        if (!edges.get(source).get(target).containsKey(language))
            edges.get(source).get(target).put(language, 0);

        // Increase the count
        edges.get(source).get(target).put(language, edges.get(source).get(target).get(language) + 1);

        // Update the total counter, source node and target node should exist for this language
        counter.get(language).setRight(counter.get(language).getRight() + 1);
    }

    /**
     * Recursive path matching function
     *
     * @param path     path of ngrams
     * @param counts   map with current scores
     * @param depth    current depth of recursion
     * @param maxDepth max allowed depth of recursion
     */
    private List<Map<String, MutablePair<Integer, Integer>>> recPathMatching(List<String> path,
                                                                                 List<Map<String, MutablePair<Integer, Integer>>> counts,
                                                                                 Integer depth, Integer maxDepth) {
        if (depth > maxDepth || path.size() == 0)
            return counts; // Done traversing, return accumulator
        else if (path.size() == 1) {

            HashMap<String, MutablePair<Integer, Integer>> ngramCounts = new HashMap<>();

            String ngram = path.get(0);
            // There is just one node left, just count node and disregard edges
            if (nodes.containsKey(ngram))
                countNodes(ngram, ngramCounts);

            if (!ngramCounts.isEmpty())
                counts.add(ngramCounts);

            return counts;
        } else {

            HashMap<String, MutablePair<Integer, Integer>> ngramCounts = new HashMap<>();

            // Get source and target
            String source = path.get(0);
            String target = path.get(1);

            // First we update scores for the source node
            if (nodes.containsKey(source)) {
                countNodes(source, ngramCounts);
                countEdges(source, target, ngramCounts);
            }

            if (!ngramCounts.isEmpty())
                counts.add(ngramCounts);

            // Recurse with the trailing path
            path.remove(0);
            return recPathMatching(path, counts, ++depth, maxDepth);
        }
    }

    /**
     * Counts nodes
     *
     * @param ngram  node
     * @param counts map for counts
     */
    private void countNodes(String ngram, HashMap<String, MutablePair<Integer, Integer>> counts) {
        for (Entry<String, Integer> langCounts : nodes.get(ngram).entrySet()) {
            String language = langCounts.getKey();
            Integer cnt = langCounts.getValue();
            // Update score
            if (!counts.containsKey(language))
                counts.put(language, new MutablePair<Integer, Integer>(0, 0));
            counts.put(language, MutablePair.of(counts.get(language).getLeft() + cnt, counts.get(language).getRight()));
        }
    }

    /**
     * Counts edges
     *
     * @param source source ngram
     * @param target target ngram
     * @param counts map for counts
     */
    private void countEdges(String source, String target, HashMap<String, MutablePair<Integer, Integer>> counts) {
        if (edges.containsKey(source)) {
            if (edges.get(source).containsKey(target)) {
                // Add up scores
                for (Entry<String, Integer> langCounts : edges.get(source).get(target).entrySet()) {
                    String language = langCounts.getKey();
                    Integer cnt = langCounts.getValue();
                    // Update score
                    if (!counts.containsKey(language))
                        counts.put(language, new MutablePair<Integer, Integer>(0, 0));
                    counts.put(language, MutablePair.of(counts.get(language).getLeft(), counts.get(language).getRight() + cnt));
                }
            }
        }
    }

    /**
     * Classifies a message and returns the most probable language
     *
     * @param doc original document
     */
    public String classifyMostProbable(String doc, int ngramLength) {

        if (debug) System.out.println(doc);

        Double bestScore = -1.0;
        String bestLang = "UNKNOWN";

        if (modelIsNotEmpty()) {
            // Get all N-grams into a list
            List<String> ngrams = getNgrams(doc, ngramLength);

            // Get counts
            List<Map<String, MutablePair<Integer, Integer>>> counts = recPathMatching(ngrams, new ArrayList<>(), 0, maxSearchDepth);

            // Calculate scores
            Map<String, Double> scores = calcScores(counts);

            // Get the best score or return unknown
            for (Entry<String, Double> score : scores.entrySet()) {
                if (score.getValue() > bestScore && score.getValue() > threshold) {
                    bestScore = score.getValue();
                    bestLang = score.getKey();
                }
            }
        } else {
            bestLang = "EMPTY MODEL";
        }

        if (debug) System.out.println("BEST: " + bestLang);
        if (debug) System.out.println();

        // Return best scoring language
        return bestLang;
    }

    /**
     * Gets scores for a given document
     *
     * @param doc document
     * @param ngramLength n
     */
    public Map<String, Double> classifyAll(String doc, int ngramLength){
        Map<String, Double> scores = new HashMap<>();
        if (modelIsNotEmpty()) {
            // Get all N-grams into a list
            List<String> ngrams = getNgrams(doc, ngramLength);

            // Get counts
            List<Map<String, MutablePair<Integer, Integer>>> counts = recPathMatching(ngrams, new ArrayList<>(), 0, maxSearchDepth);

            // Calculate scores
            scores = calcScores(counts);
        }
        return scores;
    }

    /**
     * Calculates scores for LIGA or logLIGA
     *
     * @param counts counts
     */
    private Map<String, Double> calcScores(List<Map<String, MutablePair<Integer, Integer>>> counts) {
        if (logLIGA){
            return calcScoresLogLIGA(counts);
        } else {
            return calcScoresLIGA(counts);
        }
        //Map<String, Double> scores = new HashMap<>();
    }

    /**
     * Calculates scores for default LIGA
     *
     * @param counts list ngram and ngram transitions counts for map of languages
     * @return
     */
    private Map<String, Double> calcScoresLIGA(List<Map<String, MutablePair<Integer, Integer>>> counts) {
        Map<String, Double> total = new HashMap<>();

        for (Map<String, MutablePair<Integer, Integer>> countMap: counts){
            for (Entry<String, MutablePair<Integer, Integer>> count : countMap.entrySet()){
                String lang = count.getKey(); // language
                Integer nodes = count.getValue().getLeft(); // nodes count for language
                Integer edges = count.getValue().getRight(); // edges count for language
                Integer nodesTotal = counter.get(lang).getLeft(); // total number of nodes for language
                Integer edgesTotal = counter.get(lang).getRight(); // total number of edges for language

                double nodesNormalized = (double) nodes / (double) nodesTotal;
                double edgesNormalized = (double) edges / (double) edgesTotal;

                if (!total.containsKey(lang))
                    total.put(lang, 0d);
                total.put(lang, total.get(lang) + nodesNormalized + edgesNormalized);
            }
        }
        return total;
    }

    /**
     * Calculates scores for logLIGA. Linearizes exponentially distrubuted (according to the Zipf's law) ngrams
     *
     * @param counts list ngram and ngram transitions counts for map of languages
     * @return
     */
    private Map<String, Double> calcScoresLogLIGA(List<Map<String, MutablePair<Integer, Integer>>> counts) {
        Map<String, Double> total = new HashMap<>();

        for (Map<String, MutablePair<Integer, Integer>> countMap: counts){
            for (Entry<String, MutablePair<Integer, Integer>> count : countMap.entrySet()){
                String lang = count.getKey(); // language
                Integer nodes = count.getValue().getLeft(); // nodes count for language
                Integer edges = count.getValue().getRight(); // edges count for language
                Integer nodesTotal = counter.get(lang).getLeft(); // total number of nodes for language
                Integer edgesTotal = counter.get(lang).getRight(); // total number of edges for language

                double nodesLogNormalized = 0d;
                double edgesLogNormalized = 0d;

                if ((nodes > 0) && (nodesTotal > 0))
                    nodesLogNormalized = Math.log(nodes) / Math.log(nodesTotal); // linearized ngram frequencies
                if ((edges > 0) && (edgesTotal > 0))
                    edgesLogNormalized = Math.log(edges) / Math.log(edgesTotal); // linearized ngram transition frequencies

                if (!total.containsKey(lang))
                    total.put(lang, 0d);
                total.put(lang, total.get(lang) + nodesLogNormalized + edgesLogNormalized);
            }
        }
        return total;
    }

    /**
     * loads model from the file (drops old graph and counter)
     *
     * @param path path to the model
     */
    public void loadModel(String path) {
        try {
            dropModel();
            JsonFactory jFactory = new JsonFactory();
            JsonParser jParser = jFactory.createParser(new File(path));
            // Continue until we find the end object
            while (!jParser.nextToken().equals(JsonToken.END_OBJECT)) {
                // Get the field name
                String fieldname = jParser.getCurrentName();
                if (fieldname != null) {
                    // We look for graph and counter field names
                    if (fieldname.equals("graph")) {
                        jParser.nextToken();
                        readGraph(jParser);
                    } else if (fieldname.equals("counter")) {
                        jParser.nextToken();
                        readCounter(jParser);
                    }
                }
            }
            jParser.close();
        } catch (Exception e) {
            System.out.println("Unable to load model");
            e.printStackTrace();
        }
    }

    /**
     * reads model graph
     * @param jParser parser instance
     */
    private void readGraph(JsonParser jParser) throws IOException {
        while (!jParser.nextToken().equals(JsonToken.END_OBJECT)) {
            String ngram = jParser.getCurrentName();
            // Next we again have a JSON object
            while (!jParser.nextToken().equals(JsonToken.END_OBJECT)) {
                // We can have 2 fields: nodes and edges
                String nodeOrEdge = jParser.getCurrentName();
                if (nodeOrEdge.equals("nodes")) {
                    jParser.nextToken();
                    // Now we have an object containing languages and counts
                    while (!jParser.nextToken().equals(JsonToken.END_OBJECT)) {
                        String language = jParser.getCurrentName();
                        jParser.nextToken();
                        Integer count = jParser.getIntValue();
                        // read node
                        readGraphNode(ngram, language, count);
                    }
                } else if (nodeOrEdge.equals("edges")) {
                    jParser.nextToken();
                    // Edges contain more trigrams
                    while (!jParser.nextToken().equals(JsonToken.END_OBJECT)) {
                        String target = jParser.getCurrentName();
                        jParser.nextToken();
                        // More JSON Objects, containing languages and counts
                        while (!jParser.nextToken().equals(JsonToken.END_OBJECT)) {
                            String language = jParser.getCurrentName();
                            jParser.nextToken();
                            Integer count = jParser.getIntValue();
                            // reads edge
                            readGraphEdge(ngram, target, language, count);
                        }
                    }
                }
            }
        }
    }

    /**
     * reads graph node from model
     *
     * @param ngram    node name
     * @param language language
     * @param count    count (weight)
     */
    private void readGraphNode(String ngram, String language, Integer count) {
        // Add the ngram if required
        if (!nodes.containsKey(ngram)) {
            nodes.put(ngram, new TreeMap<String, Integer>());
            edges.put(ngram, new TreeMap<String, TreeMap<String, Integer>>());
        }
        if (!nodes.get(ngram).containsKey(language))
            nodes.get(ngram).put(language, 0);
        // Add the counts
        nodes.get(ngram).put(language, count);
    }

    /**
     * reads graph edge from model
     *
     * @param ngram    from node
     * @param target   to node
     * @param language language
     * @param count    count (weight)
     */
    private void readGraphEdge(String ngram, String target, String language, Integer count) {
        // Add the ngram if required
        if (!nodes.containsKey(ngram)) {
            nodes.put(ngram, new TreeMap<String, Integer>());
            edges.put(ngram, new TreeMap<String, TreeMap<String, Integer>>());
        }

        if (!nodes.get(ngram).containsKey(language))
            nodes.get(ngram).put(language, 0);

        // Add the target if required
        if (!nodes.containsKey(target)) {
            nodes.put(target, new TreeMap<String, Integer>());
            edges.put(target, new TreeMap<String, TreeMap<String, Integer>>());
        }
        if (!nodes.get(target).containsKey(language))
            nodes.get(target).put(language, 0);

        // Add the edge
        if (!edges.containsKey(ngram))
            edges.put(ngram, new TreeMap<String, TreeMap<String, Integer>>());
        if (!edges.get(ngram).containsKey(target))
            edges.get(ngram).put(target, new TreeMap<String, Integer>());
        if (!edges.get(ngram).get(target).containsKey(language))
            edges.get(ngram).get(target).put(language, 0);
        edges.get(ngram).get(target).put(language, count);
    }

    /**
     * reads model counter
     * @param jParser parser instance
     */
    private void readCounter(JsonParser jParser) throws IOException {
        // We have language that contain objects which are counters for nodes and edges
        while (!jParser.nextToken().equals(JsonToken.END_OBJECT)) {
            String language = jParser.getCurrentName();
            jParser.nextToken();

            // Get the nodes/edges object
            while (!jParser.nextToken().equals(JsonToken.END_OBJECT)) {
                String nodeEdge = jParser.getCurrentName();

                // Check if its nodes or edges
                if (nodeEdge.equals("nodes")) {
                    jParser.nextToken();
                    Integer count = jParser.getIntValue();
                    // read node
                    readCounterNode(language, count);

                } else if (nodeEdge.equals("edges")) {
                    jParser.nextToken();
                    Integer count = jParser.getIntValue();
                    // read edge
                    readCounterEdge(language, count);
                }
            }
        }
    }

    /**
     * reads counter node from model
     *
     * @param language language
     * @param count    total count
     */
    private void readCounterNode(String language, Integer count) {
        // Add the language if it didn't exist yet
        if (!counter.containsKey(language))
            counter.put(language, new MutablePair<Integer, Integer>(0, 0));

        // Add to the mapping
        counter.get(language).setLeft(count);
    }

    /**
     * reads counter edge from model
     *
     * @param language language
     * @param count    total count
     */
    private void readCounterEdge(String language, Integer count) {
        // Add the language if it didn't exist yet
        if (!counter.containsKey(language))
            counter.put(language, new MutablePair<Integer, Integer>(0, 0));

        // Add it
        counter.get(language).setRight(count);
    }

    /**
     * cleans hash maps and sets all the values default
     */
    public void dropModel() {
        nodes.clear();
        edges.clear();
        counter.clear();
    }


    private boolean modelIsNotEmpty() {
        return !nodes.isEmpty() && !edges.isEmpty() && !counter.isEmpty();
    }

    /**
     * saves model to file
     *
     * @param path filepath
     */
    public void saveModel(String path) {
        if (modelIsNotEmpty())
            try {
                JsonFactory jFactory = new JsonFactory();
                JsonGenerator jGenerator = jFactory.createGenerator(new File(path), JsonEncoding.UTF8);
                jGenerator.writeStartObject();

                writeGraph(jGenerator);
                writeCounter(jGenerator);

                jGenerator.writeEndObject();
                jGenerator.close();
            } catch (IOException e) {
                System.out.println("Unable to save model");
                e.printStackTrace();
            }
    }

    /**
     * writes graph structure to model
     *
     * @param jGenerator generator instance
     */
    private void writeGraph(JsonGenerator jGenerator) throws IOException {
        jGenerator.writeFieldName("graph");
        jGenerator.writeStartObject();
        for (Entry<String, TreeMap<String, Integer>> node: nodes.entrySet())
            writeNgram(jGenerator, node);
        jGenerator.writeEndObject();
    }

    /**
     * writes ngram info (node and adjacent edges)
     *
     * @param jGenerator generator instance
     * @param node ngram
     */
    private void writeNgram(JsonGenerator jGenerator, Entry<String, TreeMap<String, Integer>> node) throws IOException {
        jGenerator.writeFieldName(node.getKey());
        jGenerator.writeStartObject();
        // nodes
        jGenerator.writeFieldName("nodes");
        jGenerator.writeStartObject();
        for (Entry<String, Integer> entry: node.getValue().entrySet())
            jGenerator.writeNumberField(entry.getKey(), entry.getValue());
        jGenerator.writeEndObject();
        // edges
        jGenerator.writeFieldName("edges");
        jGenerator.writeStartObject();
        if (edges.containsKey(node.getKey())){
            TreeMap<String, TreeMap<String, Integer>> edgesTree = edges.get(node.getKey());
            for (Entry<String, TreeMap<String, Integer>> edge: edgesTree.entrySet()){
                jGenerator.writeFieldName(edge.getKey());
                jGenerator.writeStartObject();
                for (Entry<String, Integer> entry: edge.getValue().entrySet()){
                    jGenerator.writeNumberField(entry.getKey(), entry.getValue());
                }
                jGenerator.writeEndObject();
            }
        }
        jGenerator.writeEndObject();
        jGenerator.writeEndObject();
    }

    /**
     * writes counter to model
     *
     * @param jGenerator generator instance
     */
    private void writeCounter(JsonGenerator jGenerator) throws IOException {
        jGenerator.writeFieldName("counter");
        jGenerator.writeStartObject();
        for (Entry<String, MutablePair<Integer, Integer>> entry: counter.entrySet()){
            jGenerator.writeFieldName(entry.getKey());
            jGenerator.writeStartObject();
            jGenerator.writeNumberField("nodes", entry.getValue().getLeft());
            jGenerator.writeNumberField("edges", entry.getValue().getRight());
            jGenerator.writeEndObject();
        }
        jGenerator.writeEndObject();
    }

    public static class LIGABuilder {

        // confidence threshold (if lower - language is still UNKNOWN)
        private double threshold;
        // max recursive search depth
        private int maxSearchDepth = 1000;
        // flag of model, if true - logLIGA, false - LIGA
        private boolean logLIGA = true;

        public LIGABuilder setMaxSearchDepth(int maxSearchDepth) {
            this.maxSearchDepth = maxSearchDepth;
            return this;
        }

        public LIGABuilder setLogLIGA(boolean logLIGA) {
            this.logLIGA = logLIGA;
            return this;
        }

        /**
         * builder
         */
        public LIGABuilder(double threshold) {
            this.threshold = threshold;
        }

        public LIGA build() {
            return new LIGA(this);
        }
    }

}
