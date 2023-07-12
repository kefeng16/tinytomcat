package com.wkf.util;

import com.wkf.annotation.AutoCompleteEnable;
import com.wkf.annotation.RequestParameter;

@RequestParameter
public class Dog {
    @AutoCompleteEnable(id="name")
    public String name;
    @AutoCompleteEnable(id="age")
    public int age;



    @Override
    public String toString() {
        return "Dog{" +
                "name='" + name + '\'' +
                ", age=" + age +
                '}';
    }
}
