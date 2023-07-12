package com.wkf.util;

import com.wkf.annotation.RequestParameter;

@RequestParameter
public class Query {
    public int page;
    public int size;
    public Dog dog;

    @Override
    public String toString() {
        return "Query{" +
                "page=" + page +
                ", size=" + size +
                ", dog=" + dog +
                '}';
    }
}
