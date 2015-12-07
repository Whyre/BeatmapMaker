package beatmap;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.input.KeyCode;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by William on 11/17/2015.
 */
public class Key {
    private StringProperty keyCode;
    private IntegerProperty keyNumber;

    public Key(String keyCode, int keyNumber) {
        this.keyCode = new SimpleStringProperty(keyCode);
        this.keyNumber = new SimpleIntegerProperty(keyNumber);
    }

    public Key(String str) {
        keyCode = new SimpleStringProperty(str.substring(1));
        keyNumber = new SimpleIntegerProperty(Integer.parseInt(str.substring(0, 1)));
    }

    public final void setKeyCode(String value) {
        keyCodeProperty().setValue(value);
    }

    public final String getKeyCode() {
        return keyCodeProperty().get();
    }

    public StringProperty keyCodeProperty() {
        //if (keyCode == null) keyCode = new SimpleStringProperty(this, "keyCode");
        return keyCode;
    }

    public final void setKeyNumber(int value) {
        keyNumberProperty().setValue(value);
    }

    public final int getKeyNumber() {
        return keyNumberProperty().get();
    }

    public IntegerProperty keyNumberProperty() {
       // if (keyNumber == null) keyNumber = new SimpleIntegerProperty(this, "keyNumber");
        return keyNumber;
    }


}
