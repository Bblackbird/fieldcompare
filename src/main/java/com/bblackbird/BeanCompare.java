package com.bblackbird;

import java.lang.reflect.Field;
import java.util.Deque;

public class BeanCompare extends FieldCompare {

    protected void log(Deque<Field> pf, Deque<String> prefix, Field f, Object left, Object right) {
        System.out.println("Field=" + getFullName(pf, prefix, f) + "Left=" + left + ", Right=" + right);
    }

    protected void log(Deque<Field> pf, Deque<String> prefix, Field f, Object left, Object right, Exception e) {
        System.err.println("Field=" + getFullName(pf, prefix, f) + "Left=" + left + ", Right=" + right + ", Exception=" + e);
    }

}
