import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

public class FixedMap<T, U> {

    private List<T> tList;
    private List<U> uList;
    private int size;

    public FixedMap(int size) {
        this.size = size;
        this.tList = new ArrayList<>();
        this.uList = new ArrayList<>();
    }

    public boolean containsKey(T key) {
        return tList.contains(key);
    }

    private int getIndex(T key) {
        return tList.indexOf(key);
    }

    public U get(T key) {
        int index = getIndex(key);
        return index == -1 ? null : uList.get(index);
    }

    public void put(T t, U u) {
        if (containsKey(t)) {
            int index = getIndex(t);
            tList.remove(index);
            uList.remove(index);
        } else if (tList.size() == size) {
                tList.remove(0);
                uList.remove(0);
        }
        tList.add(t);
        uList.add(u);
    }


    public static void main(String[] args) {
        FixedMap<Integer, String> list = new FixedMap<>(5);
        Scanner s = new Scanner(System.in);
/*
        while (true) {
            int number = s.nextInt();
            String currentString = s.next();
            list.put(number, currentString);
            System.out.println(list);
        }*/

///*
        list.put(5, "abc");
        list.put(6, "abc");
        System.out.println(list.get(4));//*/
    }

    public String toString() {
        return "T: " + tList + "\nU: " + uList;
    }


}
