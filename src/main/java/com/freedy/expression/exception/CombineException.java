package com.freedy.expression.exception;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Freedy
 * @date 2022/10/10 0:23
 */
public class CombineException extends RuntimeException {

    List<Exception> exceptions = new ArrayList<>();

    public CombineException() {
        super("this is a multi exception");
    }

    public void addException(Exception ex){
        exceptions.add(ex);
    }

    @Override
    public void printStackTrace() {
        super.printStackTrace();
        for (int i = 0; i < exceptions.size(); i++) {
            System.err.println("---------------------------------------------------------");
            System.err.println("exception " + i + ":");
            exceptions.get(i).printStackTrace();
        }
    }
}