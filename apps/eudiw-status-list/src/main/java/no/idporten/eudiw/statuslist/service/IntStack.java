package no.idporten.eudiw.statuslist.service;

public class IntStack {
    private final int[] data;
    private int top;

    public IntStack(int capacity) {
        data = new int[capacity];
        top = -1;
    }

    public IntStack(int[] array, int size) {
        data = array;
        this.top = size - 1;
    }

    public void push(int val) { data[++top] = val; }
    public int pop()          { return data[top--]; }
    public int peek()         { return data[top]; }
    public boolean isEmpty()  { return top == -1; }
    public int size()         { return top + 1; }
}
