/*
 * Title: AutomatedOracle.java
 * Project: LIGA
 * Creator: Georgii Mikriukov
 * 2018
 */

/*
 * Title: AutomatedOracle.java
 * Project: LIGA
 * Creator: Georgii Mikriukov
 * 2018
 */

package com.liga.active;

import org.apache.commons.lang3.tuple.MutablePair;

import java.util.List;

/**
 * This oracle just returns the already known labels.
 * To assign labels manually, create new Oracle interface implementation.
 */
public class AutoOracle implements Oracle {

    @Override
    public List<MutablePair<String, String>> getLabels(List<MutablePair<String, String>> list) {
        return list;
    }
}
