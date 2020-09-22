package basics;
import java.util.*;
public class JavaLoops {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		ArrayList<Integer> numbers = new ArrayList<Integer>();
		for (int i = 0; i <= 20; i++) {
			numbers.add(i);
		}
		for (int i = 0; i < numbers.size(); i++) {
			if (numbers.get(i) % 2 == 0) {
				System.out.println(numbers.get(i));
			}
		}

	}

}
