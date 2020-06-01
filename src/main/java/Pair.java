public class Pair<T, U> {

    protected T first;
    protected U second;

    Pair(T first, U second) {
        this.first = first;
        this.second = second;
    }

    public String toString() {
        return "(" + this.first + ", " + this.second + ")";
    }

}
