package beatmap;

import javafx.beans.property.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;

/**
 * Created by William on 11/19/2015.
 */
public class HitObject implements Comparable<HitObject> {
    private IntegerProperty hitObject;
    private StringProperty beat;
    private StringProperty time;
    private IntegerProperty key;
    private StringProperty hitDuration;

    public HitObject(int hitObject, String beat, String time, int key, String hitDuration) {
        this.hitObject = new SimpleIntegerProperty(hitObject);
        this.beat = new SimpleStringProperty(beat);
        this.time = new SimpleStringProperty(time);
        this.key = new SimpleIntegerProperty(key);
        this.hitDuration = new SimpleStringProperty(hitDuration);
    }

    public void setHitObject(int value) {
        hitObjectProperty().setValue(value);
    }

    public int getHitObject() {
        return hitObjectProperty().get();
    }

    public IntegerProperty hitObjectProperty() {
        //if (hitObject == null) hitObject = new SimpleIntegerProperty(this, "hitObject");
        return hitObject;
    }

    public void setBeat(String value) {
        beatProperty().setValue(value);
    }

    public String getBeat() {
        return beatProperty().get();
    }

    public StringProperty beatProperty() {
        //if (beat == null) beat = new SimpleDoubleProperty(this, "beat");
        return beat;
    }

    public MixedNumber getBeatMixedNumber() {
        return new MixedNumber(this.getBeat());
    }

    public void setTime(String value) {
        timeProperty().setValue(value);
    }

    public String getTime() {
        return timeProperty().get();
    }

    public StringProperty timeProperty() {
        //if (time == null) time = new SimpleStringProperty(this, "--:--");
        return time;
    }

    public void setKey(int value) {
        keyProperty().setValue(value);
    }

    public int getKey() {
        return keyProperty().get();
    }

    public IntegerProperty keyProperty() {
        //if (key == null) key = new SimpleStringProperty(this, "key");
        return key;
    }

    public void setHitDuration(String value) {
        hitDurationProperty().setValue(value);
    }

    public String getHitDuration() {
        return hitDurationProperty().get();
    }

    public StringProperty hitDurationProperty() {
        //if (hitDuration == null) hitDuration = new SimpleDoubleProperty(this, "hitObjectDuration");
        return hitDuration;
    }

    public MixedNumber getHitDurationMixedNumber() {
        return new MixedNumber(this.getHitDuration());
    }

    public boolean isEqualTo(HitObject ho) {
        return (this.getBeat().equals(ho.getBeat()) &&
                this.getKey() == (ho.getKey()));
    }


    @Override
    public int compareTo(HitObject ho) {
//        if (this.getBeat().contains("/")) {
//            int beat1 = Integer.parseInt(this.getBeat().split(" ")[0]);
//            String beat2 = this.getBeat().split(" ")[1];
//            int numerator1 = Integer.parseInt(beat2.split("/")[0]);
//            int denominator1 = Integer.parseInt(beat2.split("/")[1]);
//            if (ho.getBeat().contains("/")) {
//                int ho1 = Integer.parseInt(ho.getBeat().split(" ")[0]);
//                String ho2 = ho.getBeat().split(" ")[1];
//                int numerator2 = Integer.parseInt(ho2.split("/")[0]);
//                int denominator2 = Integer.parseInt(ho2.split("/")[1]);
//
//                if (beat1 > ho1) return 1;
//                if (beat1 < ho1) return -1;
//                if (beat1 == ho1) {
//                    if (denominator1 >= denominator2) {
//                        int multiplier = denominator1 / denominator2;
//                        numerator2 = numerator2 * multiplier;
//                        if (numerator1 > numerator2) return 1;
//                        else return -1;
//                    }
//                    if (denominator1 < denominator2) {
//                        int multiplier = denominator2 / denominator1;
//                        numerator1 = numerator1 * multiplier;
//                        if (numerator1 > numerator2) return 1;
//                        else return -1;
//                    }
//                }
//            } else {
//                double hoDouble = HitObject.round(Double.parseDouble(ho.getBeat()), 6);
//                double beatDouble = HitObject.round(beat1 + (double) numerator1 / (double) denominator1, 6);
//                if (hoDouble > beatDouble) return 1;
//                else if (hoDouble < beatDouble) return -1;
//                else return 0;
//            }
//        } else {
//            double beatDouble = HitObject.getDoubleFromFraction(this);
//            double hoDouble = HitObject.getDoubleFromFraction(ho);
//            if (beatDouble > hoDouble) return 1;
//            else if (beatDouble < hoDouble) return -1;
//        }
//        return 0;
        MixedNumber myMixedNumber = new MixedNumber(this.getBeat());
        MixedNumber hoMixedNumber = new MixedNumber(ho.getBeat());
        return myMixedNumber.compareTo(hoMixedNumber);
    }

    public static double getDoubleFromFraction(HitObject ho) {
        int int1 = Integer.parseInt(ho.getBeat().split(" ")[0]);
        String ho2 = ho.getBeat().split(" ")[1];
        int numerator1 = Integer.parseInt(ho2.split("/")[0]);
        int denominator1 = Integer.parseInt(ho2.split("/")[1]);
        return HitObject.round(int1 + (double) numerator1 / (double) denominator1, 6);
    }

    public static double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();

        BigDecimal bd = new BigDecimal(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }
}
