package no.idporten.eudiw.statuslist.util;

public class IntStack {
    private final int[] data;
    private int top;

    public IntStack(int[] array, int top) {
        data = array;
        this.top = top;
    }

    public void push(int val) {
        data[++top] = val;
    }

    public int pop() {
        return data[top--];
    }

    public int peek() {
        return data[top];
    }

    public boolean isEmpty() {
        return top == -1;
    }

    public int size() {
        return top + 1;
    }
}
