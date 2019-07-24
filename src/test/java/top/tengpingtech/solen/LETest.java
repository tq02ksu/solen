package top.tengpingtech.solen;

public class LETest {
    public static void main(String[] args) {
        short[] input = new short [] { 0x410a, 0x474c };
        for (int in : input) {
            System.out.println(in + " le : " + Integer.reverse(in << 16));
        }
    }
}
