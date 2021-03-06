/*
 * The MIT License
 *
 * Copyright 2016 Ahseya.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.github.horrorho.inflatabledonkey.util;

import java.util.ArrayList;
import java.util.List;

/**
 * ListUtils.
 *
 * @author Ahseya
 */
public final class ListUtils {

    public static <T> List<List<T>> partition(List<T> list, int size) {
        if (size <= 0) {
            throw new IllegalArgumentException("bad size: " + size);
        }
        List<List<T>> lists = new ArrayList<>();
        for (int i = 0; i < list.size(); i += size) {
            int fromIndex = i;
            int toIndex = fromIndex + size;
            toIndex = toIndex > list.size()
                    ? list.size()
                    : toIndex;
            List<T> subList = list.subList(fromIndex, toIndex);
            lists.add(subList);
        }
        return lists;
    }
}
