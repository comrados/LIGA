/*
 * Title: Oracle.java
 * Project: LIGA
 * Creator: Georgii Mikriukov
 * 2018
 */

/*
 * Title: Oracle.java
 * Project: LIGA
 * Creator: Georgii Mikriukov
 * 2018
 */

package com.liga.active;

import org.apache.commons.lang3.tuple.MutablePair;

import java.util.List;

public interface Oracle {

    /**
     * returns labeled list
     *
     * @param list list to lable
     */
    List<MutablePair<String, String>> getLabels(List<MutablePair<String, String>> list);

}
