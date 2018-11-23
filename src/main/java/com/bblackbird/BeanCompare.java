package com.bblackbird;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.Deque;

public class BeanCompare extends FieldCompare {

    Logger logger = LoggerFactory.getLogger(BeanCompare.class);

    protected void log(Deque<Field> pf, Deque<String> prefix, Field f, Object left, Object right) {
        //System.out.println("Field=" + getFullName(pf, prefix, f) + " Left=" + left + ", Right=" + right);
        logger.info("Field=" + getFullName(pf, prefix, f) + " Left=" + left + ", Right=" + right);
    }

    protected void log(Deque<Field> pf, Deque<String> prefix, Field f, Object left, Object right, Exception e) {
        //System.err.println("Field=" + getFullName(pf, prefix, f) + " Left=" + left + ", Right=" + right + ", Exception=" + e);
        logger.info("Field=" + getFullName(pf, prefix, f) + " Left=" + left + ", Right=" + right + ", Exception=" + e);
    }

}
