package basics;

public class RecursionButNot {
		public static int sum(int num) {
			int counter = 0;
			while (num > 0) {
				counter += num;
				num--;
			}
			return counter;
		}

		public static void main(String[] args) {
			System.out.println(sum(10));
		}
}
