package model;

import java.util.ArrayList;
import java.util.List;

public class CustomLinkedList<T> {
    private Node tail;
    private Node head;
    private int size;

    public CustomLinkedList() {
        this.tail = null;
        this.head = null;
        this.size = 0;
    }

    public void linkLast(Node node) {
        if (tail == null) {
            // Если список пустой
            head = node;
        } else {
            // Добавляем в конец
            tail.setNext(node);
            node.setPrev(tail);
        }
        tail = node;
        size++;
    }

    public void removeNode(Node node) {
        if (node == null) {
            return;
        }

        Node prevNode = node.getPrev();
        Node nextNode = node.getNext();

        if (prevNode != null) {
            prevNode.setNext(nextNode);
        } else {
            // Удаляем голову
            head = nextNode;
        }

        if (nextNode != null) {
            nextNode.setPrev(prevNode);
        } else {
            // Удаляем хвост
            tail = prevNode;
        }

        // Очищаем ссылки удаленного узла
        node.setPrev(null);
        node.setNext(null);
    }

    public List<Task> getTasks() {
        List<Task> tasks = new ArrayList<>();
        Node current = head;

        while (current != null) {
            tasks.add(current.getTask());
            current = current.getNext();
        }

        return tasks;
    }
}