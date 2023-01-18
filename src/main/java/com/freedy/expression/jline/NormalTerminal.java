package com.freedy.expression.jline;

import java.util.Scanner;

public class NormalTerminal implements ExpressionTerminal {
    private final static Scanner SCANNER = new Scanner(System.in);
    @Override
    public String stdin(String placeholder) {
        System.out.print(placeholder);
        return SCANNER.nextLine();
    }

    @Override
    public void clearScreen() {
        throw new UnsupportedOperationException();
    }
}
