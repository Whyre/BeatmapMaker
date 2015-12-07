package beatmap;

import javafx.beans.InvalidationListener;
import javafx.beans.binding.Binding;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.util.converter.IntegerStringConverter;
import javafx.util.converter.NumberStringConverter;

import java.awt.event.ActionEvent;
import java.io.*;
import java.net.URL;
import java.util.*;

/**
 * Created by William on 11/17/2015.
 */
public class KeyController implements Initializable {
    private ObservableList<Key> keys;
    private int keyNumber;
    private File f;

    @FXML
    private TableView<Key> keyTable;

    @FXML
    private TableColumn<Key, Integer> keyNumberColumn;

    @FXML
    private TableColumn<Key, String> keyBindingColumn;

    @FXML
    private TextField keyNumberField;

    @FXML
    private Button saveButton;

    @FXML
    private Button resetButton;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        keys = FXCollections.observableArrayList();
        keyNumber = Integer.parseInt(keyNumberField.getText());
        keys.add(new Key(""+ 'D', 1));
        keys.add(new Key(""+ 'F', 2));
        keys.add(new Key(""+ 'J', 3));
        keys.add(new Key("" + 'K', 4));
/*        for (int i = 4; i < keyNumber; i++) {
            keys.add(i, new Key("" + (char) (i + 65), i + 1));
        }*/

        keyTable.setItems(keys);
        keyNumberColumn.setCellValueFactory(new PropertyValueFactory<>("keyNumber"));
        keyBindingColumn.setCellValueFactory(new PropertyValueFactory<>("keyCode"));
        //keyNumberColumn.setCellFactory(TextFieldTableCell.<Key, Integer>forTableColumn(new IntegerStringConverter()));
        keyBindingColumn.setCellFactory(TextFieldTableCell.forTableColumn());

        keyTable.getColumns().setAll(keyNumberColumn, keyBindingColumn);
        keyTable.setEditable(true);

        keyNumberField.setOnAction(event -> {
            if (!KeyController.isNumeric(keyNumberField.getText())) {

            } else {
                keyNumber = Integer.parseInt(keyNumberField.getText());
                if (keys.size() >= keyNumber) {
                    for (int k = keys.size(); k > keyNumber; k--) {
                        keys.remove(k - 1);
                    }
                } else {
                    for (int j = keys.size(); j < keyNumber; j++) {
                        keys.add(new Key("" + (char) 65, j + 1));
                    }
                }
            }
        });

        saveButton.setOnAction(event -> {
            f = new File("keys.txt");
            try (Writer writer = new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream(f), "utf-8"))) {
                System.out.println("File saved to " + f.getAbsolutePath());
                writer.write(keyNumber + " ");
                for (Key key : keys) {
                    writer.write(key.getKeyNumber()+key.getKeyCode()+" ");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        resetButton.setOnAction(event -> {
            keys.clear();
            keyNumberField.setText("4");
            keyNumber = 4;
            keys.add(new Key(""+ 'D', 1));
            keys.add(new Key(""+ 'F', 2));
            keys.add(new Key(""+ 'J', 3));
            keys.add(new Key("" + 'K', 4));

        });

    }

    private static boolean isNumeric(String str)
    {
        try
        {
            double d = Double.parseDouble(str);
        }
        catch(NumberFormatException nfe)
        {
            return false;
        }
        return true;
    }


}
