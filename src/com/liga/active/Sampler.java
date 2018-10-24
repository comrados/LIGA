/*
 * Title: Sampler.java
 * Project: LIGA
 * Creator: Georgii Mikriukov
 * 2018
 */

/*
 * Title: Sampler.java
 * Project: LIGA
 * Creator: Georgii Mikriukov
 * 2018
 */

package com.liga.active;

import org.apache.commons.lang3.tuple.MutablePair;

import java.util.List;
import java.util.Map;

public interface Sampler {

    /**
     * get n samples from train data, depending on strategy and scores
     *
     * @param train training data
     * @param scores scores
     */
    List<MutablePair<String, String>> getSamples(List<MutablePair<String, String>> train,
                                                 List<Map<String, Double>> scores);

}
