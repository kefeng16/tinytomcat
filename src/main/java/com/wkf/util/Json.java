package com.wkf.util;

import com.fasterxml.jackson.databind.ObjectMapper;

public class Json {

    public static String marshal(Object o) throws Exception {
        return new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(o);
    }

    public static <T> T unmarshal(String json, Class<T> clazz) throws Exception {
        T t = new ObjectMapper().readValue(json, clazz);
        return t;
    }

}
