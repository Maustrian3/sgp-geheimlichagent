package geheimlichagent;

public class MyPair<A, B> implements at.ac.tuwien.ifs.sge.util.pair.Pair<A, B> {
    private A a;
    private B b;

    public MyPair(A a, B b) {
        this.a = a;
        this.b = b;
    }

    @Override
    public A getA() {
        return a;
    }

    @Override
    public B getB() {
        return b;
    }

    @Override
    public String toString() {
        return "{" +
                "Agent:" + a +
                ", Score=" + b +
                '}';
    }
}
