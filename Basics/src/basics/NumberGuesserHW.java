package basics;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;
import java.util.Scanner;
import java.util.ArrayList;


public class NumberGuesserHW {
	private int level = 1;
	private int strikes = 0;
	private int maxStrikes = 5;
	private int number = 0;
	private boolean isRunning = false;
	final String saveFile = "numberGuesserSave.txt";

	/***
	 * Gets a random number between 1 and level.
	 * 
	 * @param level (level to use as upper bounds)
	 * @return number between bounds
	 */
	public static int getNumber(int level) {
		int range = 9 + ((level - 1) * 5);
		System.out.println("I picked a random number between 1-" + (range + 1) + ", let's see if you can guess.");
		return new Random().nextInt(range) + 1;
	}

	private void win() {
		System.out.println("That's right!");
		level++;// level up!
		strikes = 0;
		augmentMaxStrikes();
		saveLevel();
		System.out.println("Welcome to level " + level);
		System.out.println("You now have " + (maxStrikes) + " strikes.");
		number = getNumber(level);
	}

	private void lose() {
		System.out.println("Uh oh, looks like you need to get some more practice.");
		System.out.println("The correct number was " + number);
		strikes = 0;
		level--;
		if (level < 1) {
			level = 1;
		}
		augmentMaxStrikes();
		saveLevel();
		number = getNumber(level);
	}

	private void processCommands(String message) {
		if (message.equalsIgnoreCase("quit")) {
			System.out.println("Tired of playing? No problem, see you next time.");
			isRunning = false;
		}
	}

	private void processGuess(int guess) {
		if (guess < 0) {
			return;
		}
		System.out.println("You guessed " + guess);
		if (guess == number) {
			win();
		} else {
			System.out.println("That's wrong");
			strikes++;
			saveLevel();
			if (strikes >= maxStrikes) {
				lose();
			} else {
				int remainder = maxStrikes - strikes;
				System.out.println("You have " + remainder + "/" + maxStrikes + " attempts remaining");
				if (guess > number) {
					System.out.println("Lower");
				} else if (guess < number) {
					System.out.println("Higher");
				}
			}
		}
	}

	private int getGuess(String message) {
		int guess = -1;
		try {
			guess = Integer.parseInt(message);
		} catch (NumberFormatException e) {
			System.out.println("You didn't enter a number, please try again");

		}
		return guess;
	}

	private void saveLevel() {
		try (FileWriter fw = new FileWriter(saveFile)) {
			fw.write("" + level + "\n");// here we need to convert it to a String to record correctly
			fw.write("" + strikes + "\n");
			fw.write("" + number + "\n");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private boolean loadLevel() {
		File file = new File(saveFile);
		if (!file.exists()) {
			return false;
		}
		ArrayList<Integer> numbers = new ArrayList<Integer>();
		try (Scanner reader = new Scanner(file)) {
			while (reader.hasNextInt()) {
				numbers.add(reader.nextInt());
				/*int _level = reader.nextInt();
				if (_level > 1) {
					level = _level;
				}
				int _strikes = reader.nextInt();
				strikes = _strikes;
				int _number = reader.nextInt();
				number = _number;*/
			}
			level = numbers.get(0);
			strikes = numbers.get(1);
			number = numbers.get(2);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return false;
		} catch (Exception e2) {
			e2.printStackTrace();
			return false;
		}
		return level > 1;
	}

	void run() {
		try (Scanner input = new Scanner(System.in);) {
			if (loadLevel()) {
				System.out.println("Welcome to Number Guesser 4.0!");
				augmentMaxStrikes();
				System.out.println("I'll ask you to guess a number between a range, and you'll have " + (maxStrikes)
						+ " attempts to guess.");
				System.out.println("Successfully loaded level " + level + " let's continue then! You currently have " + (maxStrikes - strikes) + " strikes left");
			}
			else {
				System.out.println("Welcome to Number Guesser 4.0!");
				System.out.println("I'll ask you to guess a number between a range, and you'll have " + (maxStrikes)
						+ " attempts to guess.");
				number = getNumber(level);
			}
			
			isRunning = true;
			while (input.hasNext()) {
				String message = input.nextLine();
				processCommands(message);
				if (!isRunning) {
					break;
				}
				int guess = getGuess(message);
				processGuess(guess);
			}

		} catch (Exception e) {
			e.printStackTrace();
			System.out.println(e.getMessage());
		}
	}

	public static void main(String[] args) {
		NumberGuesserHW guesser = new NumberGuesserHW();
		guesser.run();
	}
	
	public void augmentMaxStrikes() {
		if (level % 4 == 0) {
			maxStrikes = 5 + (level / 4);
		}
		else if (level % 4 == 1) {
			maxStrikes = 5 + ((level - 1) / 4);
		}
		else if (level % 4 == 2) {
			maxStrikes = 5 + ((level - 2) / 4);
		}
		else if (level % 4 == 3) {
			maxStrikes = 5 + ((level - 3) / 4);
		}
	}
}