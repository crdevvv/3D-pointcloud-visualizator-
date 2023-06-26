package com.example.prova3d;

import java.util.Comparator;
import java.util.List;

public class KdTree {

    private final int dimensions;
    private Node root;
    private Node best = null;
    private double bestDistance = 0;
    private int visited = 0;

    public KdTree(int dimensions, List<Node> nodes) {
        this.dimensions = dimensions;
        root = makeTree(nodes, 0, nodes.size(), 0);
    }

    public Node getRoot() {
        return root;
    }

    public Node findNearest(Node target) {
        if (root == null)
            throw new IllegalStateException("Tree is empty!");
        best = null;
        visited = 0;
        bestDistance = 0;
        nearest(root, target, 0);
        return best;
    }

    public int visited() {
        return visited;
    }

    public double distance() {
        return Math.sqrt(bestDistance);
    }

    private void nearest(Node root, Node target, int index) {
        if (root == null)
            return;
        ++visited;
        double d = root.distance(target);
        if (best == null || d < bestDistance) {
            bestDistance = d;
            best = root;
        }
        if (bestDistance == 0)
            return;
        double dx = root.get(index) - target.get(index);
        index = (index + 1) % dimensions;
        nearest(dx > 0 ? root.left : root.right, target, index);
        if (dx * dx >= bestDistance)
            return;
        nearest(dx > 0 ? root.right : root.left, target, index);
    }

    private Node makeTree(List<Node> nodes, int begin, int end, int index) {
        if (end <= begin)
            return null;
        int n = begin + (end - begin)/2;
        Node node = QuickSelect.select(nodes, begin, end - 1, n, new NodeComparator(index));
        index = (index + 1) % dimensions;
        node.left = makeTree(nodes, begin, n, index);
        node.right = makeTree(nodes, n + 1, end, index);
        return node;
    }

    private static class NodeComparator implements Comparator<Node> {
        private int index_;

        private NodeComparator(int index) {
            index_ = index;
        }
        public int compare(Node n1, Node n2) {
            return Double.compare(n1.get(index_), n2.get(index_));
        }
    }

    public static class Node {
        private final float[] cords;
        private Node left = null;
        private Node right = null;

        public Node(float[] cords) {
            this.cords = cords;
        }
        public Node(float x, float y, float z) {
            this(new float[]{x, y, z});
        }
        float get(int index) {
            return cords[index];
        }
        double distance(Node node) {
            double dist = 0;
            for (int i = 0; i < cords.length; ++i) {
                double d = cords[i] - node.cords[i];
                dist += d * d;
            }
            return dist;
        }
        public String toString() {
            StringBuilder s = new StringBuilder("(");
            for (int i = 0; i < cords.length; ++i) {
                if (i > 0)
                    s.append(", ");
                s.append(cords[i]);
            }
            s.append(')');
            return s.toString();
        }
    }
}
