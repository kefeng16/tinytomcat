package com.wkf.util;

import com.wkf.annotation.AutoCompleteEnable;
import com.wkf.annotation.RequestParameter;

@RequestParameter
public class Cat {
    @AutoCompleteEnable(id="food")
    public String food;
    @AutoCompleteEnable(id="master")
    public String master;

    @Override
    public String toString() {
        return "Cat{" +
                "food='" + food + '\'' +
                ", master='" + master + '\'' +
                '}';
    }
}
