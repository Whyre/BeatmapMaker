package beatmap;

import java.util.Scanner;

/**
 * Created by William on 12/2/2015.
 */
public class MixedNumber implements Comparable<MixedNumber> {
    private int wholeInt;
    private int numerator;
    private int denominator;

    public MixedNumber(String str) {
        Scanner s = new Scanner(str);
        wholeInt = s.nextInt();
        if (str.trim().contains(" ")) {
            String fraction = s.next();
            numerator = Integer.parseInt(fraction.substring(0, fraction.indexOf("/")));
            denominator = Integer.parseInt(fraction.substring(fraction.indexOf("/") + 1));
        }
    }

    public MixedNumber(int wholeInt, int numerator, int denominator) {
        this.wholeInt = wholeInt;
        this.numerator = numerator;
        this.denominator = denominator;
    }

    public int getWholeInt() {
        return wholeInt;
    }

    public int getDenominator() {
        return denominator;
    }

    public int getNumerator() {
        return numerator;
    }

    public String toString() {
        if (numerator != 0 && denominator != 0)
            return (wholeInt + " " + numerator + "/" + denominator);
        else return Integer.toString(wholeInt);
    }

    @Override
    public int compareTo(MixedNumber o) {
        if (this.wholeInt > o.getWholeInt()) {
            return 1;
        }
        if (this.wholeInt < o.getWholeInt()) {
            return -1;
        }
        if (o.getDenominator() != 0 && this.denominator != 0) {
            if (this.denominator > o.getDenominator()) {
                int multiplier = this.denominator / o.getDenominator();
                if (this.numerator > o.getNumerator() * multiplier) {
                    return 1;
                }
                if (this.numerator < o.getNumerator()) {
                    return -1;
                } else return 0;
            }
            int multiplier = o.getDenominator() / this.denominator;
            if (this.numerator * multiplier > o.getNumerator()) return 1;
            if (this.numerator * multiplier < o.getNumerator()) return -1;
        }
        return 0;
    }
}


