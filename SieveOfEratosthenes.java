//CC0815 Demo
//Alexander Gruschina - Mario Kotoy

public class SieveOfEratosthenes {

	class PrimeMap {
		boolean[] notPrime;
		int max;
		int countPrimes;
	}

	PrimeMap map;

	public void initialize(int maxValue) {
		int i;

		System.out.println("Determining primes up to " + maxValue);
		map = new PrimeMap();
		map.max = maxValue;
		map.notPrime = new boolean[100];
		i = 2;
		while (i < map.max) {
			map.notPrime[i] = false;
			i = i + 1;
		}
	}

	public void determineMultiples() {
		int i;
		int j;
		int helper;

		i = 2;
		while (i < map.max) {
			if (map.notPrime[i] == false) {
				helper = 2 * i;
				j = 3;
				while (helper < map.max) {
					map.notPrime[helper] = true;
					helper = j * i;
					j = j + 1;
				}
			}
			i = i + 1;
		}
	}

	public int calcNumOfPrimes() {
		int i;
		int count;

		count = 0;
		i = 2;
		while (i < map.max) {
			if (map.notPrime[i] == false)
				count = count + 1;
			i = i + 1;
		}
		return count;
	}

	public void outputResult(boolean requestPrimes) {
		int i;

		if (!requestPrimes) {
			System.out.println("ALL NON-PRIMES:");
			System.out.println("1");
		} else {
			System.out.println("ALL PRIMES:");
		}
		i = 2;
		while (i < map.max) {
			if (map.notPrime[i] != requestPrimes) {
				System.out.println(i);
			}
			i = i + 1;
		}
	}

	public static void main(String args) {
		System.out.println("--------------------------------------");
		System.out.println("COMPILER CONSTRUCTION SS 2013");
		System.out.println("Alexander Gruschina - Mario Kotoy");
		System.out.println("CC0815 Demo");
		System.out.println("The Sieve of Eratosthenes");
		System.out.println("--------------------------------------");

		initialize(50);
		determineMultiples();
		map.countPrimes = calcNumOfPrimes();
		System.out.println("Total number of primes: " + map.countPrimes);
		outputResult(true);
		System.out.println("------------ Done ------------");
	}
}
