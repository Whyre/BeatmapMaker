package beatmap;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaPlayer.Status;
import javafx.scene.media.MediaView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.util.converter.DoubleStringConverter;
import javafx.util.converter.IntegerStringConverter;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import java.io.*;
import java.net.URL;
import java.util.*;

/*
    TO DO list: Add metronome
                snap to beat function

 */

public class Controller implements Initializable {

    private boolean atEndOfMedia = false;
    private boolean stopRequested = false;
    private boolean generateBPMMode = false;
    private boolean beatmapMode = false;
    private boolean audioFileLoaded = false;
    private boolean snapToBeat = false;
    private double oldTime;
    private Duration duration;
    private MediaPlayer mp;
    private Clip[] hitsounds;
    private int offset;
    private int bpm;
    private int keyNumber;
    private int hitObjectNumber;
    private int hitsoundIndex;
    private int snapBeatDenominator;
    private double snapLength;
    private long startTime;
    private static final String timeFormat = "%02d:%02d:%03d";
    private List<KeyCode> keyCodes;
    private ObservableList<HitObject> hitObjects;

    @FXML
    private MediaView mediaView;

    @FXML
    private Button playButton, clearBeatmapButton;

    @FXML
    private Slider timeSlider;

    @FXML
    private Label timeLabel;

    @FXML
    private Slider volumeSlider;

    @FXML
    private MenuItem openMusicMenuItem, keyBindMenuItem, exportBeatmapMenuItem;

    @FXML
    private CheckMenuItem zeroNoteLengthMenuItem, wholeNoteLengthMenuItem, halfNoteLengthMenuItem,
            quarterNoteLengthMenuItem, eighthNoteLengthMenuItem, sixteenNoteLengthMenuItem;

    @FXML
    private TextField bpmField, offsetTextField;

    @FXML
    private ToggleButton generateBPMToggle, beatmapModeToggle;

    @FXML
    private VBox node;

    @FXML
    private TableColumn<HitObject, Integer> hitObjectColumn;

    @FXML
    private TableColumn<HitObject, String> beatNumberColumn;

    @FXML
    private TableColumn<HitObject, String> timeColumn;

    @FXML
    private TableColumn<HitObject, Integer> keyColumn;

    @FXML
    private TableColumn<HitObject, String> hitObjectHoldDurationColumn;

    @FXML
    private TableView<HitObject> hitObjectTable;


    @Override
    public void initialize(URL location, ResourceBundle resources) {
        List<Double> bpmTimeList = new ArrayList<>();
        hitObjects = FXCollections.observableArrayList();

        hitObjectTable.setItems(hitObjects);
        hitObjectColumn.setCellValueFactory(new PropertyValueFactory<>("hitObject"));

        beatNumberColumn.setCellValueFactory(new PropertyValueFactory<>("beat"));
        beatNumberColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        beatNumberColumn.setOnEditCommit(event -> {
            event.getTableView().getItems().get(event.getTablePosition().getRow()).setBeat(event.getNewValue());

//            double elapsedTime = event.getNewValue() / bpm;
//            double elapsedMinutes = Math.floor(elapsedTime);
//            double elapsedSeconds = Math.floor((elapsedTime - elapsedMinutes) * 60);
//            double elapsedMillis = (elapsedTime - elapsedMinutes - elapsedSeconds) * 3600;
//            event.getTableView().getItems().get(event.getTablePosition().getRow()).setTime(String.format(timeFormat, (int) elapsedMinutes, (int) elapsedSeconds, (int) elapsedMillis));
            refreshBeatmapByBeat(hitObjects);
            System.out.println("edit committed");
            hitObjectTable.requestFocus();
        });

        timeColumn.setCellValueFactory(new PropertyValueFactory<>("time"));
        keyColumn.setCellValueFactory(new PropertyValueFactory<>("key"));
        keyColumn.setCellFactory(TextFieldTableCell.<HitObject, Integer>forTableColumn(new IntegerStringConverter()));
        hitObjectHoldDurationColumn.setCellValueFactory(new PropertyValueFactory<>("hitDuration"));
        hitObjectHoldDurationColumn.setCellFactory(TextFieldTableCell.forTableColumn());


        hitObjectTable.setRowFactory(tableView -> {
            final TableRow<HitObject> row = new TableRow<>();
            final ContextMenu contextMenu = new ContextMenu();
            final MenuItem removeMenuItem = new MenuItem("Remove");
            final MenuItem addRowMenuItem = new MenuItem("Add Row");
            removeMenuItem.setOnAction(event -> {
                hitObjectTable.getItems().remove(row.getItem());
                refreshBeatmapByBeat(hitObjects);
            });
            addRowMenuItem.setOnAction(event -> {
                hitObjectTable.getItems().add(row.getIndex() + 1, new HitObject(row.getIndex() + 1, "0", "--:--:--", 1, ""));
                refreshBeatmapByHitObject(hitObjects);
            });
            contextMenu.getItems().add(removeMenuItem);
            contextMenu.getItems().add(addRowMenuItem);
            // Set context menu on row, but use a binding to make it only show for non-empty rows:
            row.contextMenuProperty().bind(
                    Bindings.when(row.emptyProperty())
                            .then((ContextMenu) null)
                            .otherwise(contextMenu)
            );
            return row;
        });

        hitObjectTable.getColumns().setAll(hitObjectColumn, beatNumberColumn, timeColumn, keyColumn, hitObjectHoldDurationColumn);
        Controller.addAutoScroll(hitObjectTable);
        hitObjectTable.setEditable(true);

        hitsounds = new Clip[32];
        for (int i = 0; i < hitsounds.length; i++) {
            try {
                AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(Controller.class.getResource("/hitsound old.wav"));
                hitsounds[i] = AudioSystem.getClip();
                hitsounds[i].open(audioInputStream);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }


        keyCodes = new ArrayList<>();
        if (new File("keys.txt").isFile()) {
            System.out.println("The key binding file exists");
            InputStream keyStream = null;
            try {
                keyStream = new FileInputStream(new File("keys.txt"));
            } catch (IOException e) {
                System.out.println("Failed to load keys file");
            }
            Scanner scanner = new Scanner(new BufferedReader(new InputStreamReader(keyStream)));
            keyNumber = scanner.nextInt();
            while (scanner.hasNext()) {
                keyCodes.add(KeyCode.getKeyCode(scanner.next().substring(1)));
            }
            System.out.println("The keys are " + keyCodes);
        } else {
            keyNumber = 4;
            keyCodes.add(KeyCode.getKeyCode("D"));
            keyCodes.add(KeyCode.getKeyCode("F"));
            keyCodes.add(KeyCode.getKeyCode("J"));
            keyCodes.add(KeyCode.getKeyCode("K"));
            System.out.println("Using default keys: " + keyCodes);
        }


        generateBPMToggle.setOnAction(event -> {
            if (mp != null) {
                if (!generateBPMMode) {
                    bpmField.setText("--");
                    mp.seek(mp.getStartTime());
                    mp.play();
                } else if (generateBPMMode) {
                    mp.pause();
                    if (snapToBeat)
                        snapLength = HitObject.round((bpm * (mp.getTotalDuration().toMillis()) * (1 / 60000)) / snapBeatDenominator, snapBeatDenominator);
                }
                generateBPMMode = !generateBPMMode;
                bpmTimeList.clear();
            }
        });

        beatmapModeToggle.setOnAction(event -> {
            if (bpm == 0) {
                Alert alert = new Alert(AlertType.WARNING);
                alert.setTitle("BPM Warning");
                alert.setHeaderText(null);
                alert.setContentText("The BPM is currently 0. This will not work.");
                alert.showAndWait();
                beatmapModeToggle.setSelected(false);
                beatmapModeToggle.getParent().requestFocus();
            } else if (mp != null) {
                if (!beatmapMode) {
                    mp.seek(mp.getStartTime());
                    startTime = System.currentTimeMillis() + offset;
                    mp.play();
                } else {
                    mp.pause();
                }
                beatmapMode = !beatmapMode;
            }
        });


        keyBindMenuItem.setOnAction(event -> {
            Parent root;
            try {
                root = FXMLLoader.load(Controller.class.getResource("/keys.fxml"), resources);
                Stage newWindow = new Stage();
                newWindow.setTitle("Set Keybindings");
                newWindow.setScene(new Scene(root));
                newWindow.show();

            } catch (IOException e) {
                e.printStackTrace();
            }
        });


        openMusicMenuItem.setOnAction(event -> {
            FileChooser f = new FileChooser();
            f.setTitle("Choose Audio File");
            f.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("Audio Files", "*.wav", "*.mp3", "*.aac", "*.m4a"));
            File selectedFile = f.showOpenDialog(this.node.getScene().getWindow());
            if (selectedFile == null) {
                return;
            }
            if (audioFileLoaded) {
                mp.stop();
                mp.dispose();
                playButton.setText(">");
            }
            mp = new MediaPlayer(new Media(selectedFile.toURI().toString()));
            mediaView.setMediaPlayer(mp);
            audioFileLoaded = true;

            mp.currentTimeProperty().addListener((ov) -> updateValues());

            mp.setOnPlaying(() -> {
                startTime = System.currentTimeMillis() + offset;
                if (stopRequested) {
                    mp.pause();
                    stopRequested = false;
                } else {
                    playButton.setText("||");
                }
            });

            mp.setOnPaused(() ->
                    playButton.setText(">"));

            mp.setOnReady(() -> {
                duration = mp.getMedia().getDuration();
                updateValues();

            });

            mp.setOnEndOfMedia(() -> {
                        playButton.setText(">");
                        stopRequested = true;
                        atEndOfMedia = true;
                        beatmapMode = false;
                        beatmapModeToggle.setSelected(false);
                    }
            );

            CheckMenuItem[] noteLengths = {zeroNoteLengthMenuItem, wholeNoteLengthMenuItem, halfNoteLengthMenuItem,
                    quarterNoteLengthMenuItem, eighthNoteLengthMenuItem, sixteenNoteLengthMenuItem};

            zeroNoteLengthMenuItem.setSelected(true);

            zeroNoteLengthMenuItem.setOnAction(event1 ->
                    setSnapToBeat(zeroNoteLengthMenuItem, noteLengths));

            wholeNoteLengthMenuItem.setOnAction(event1 -> {
                snapBeatDenominator = 1;
                setSnapToBeat(wholeNoteLengthMenuItem, noteLengths);
            });

            halfNoteLengthMenuItem.setOnAction(event1 -> {
                snapBeatDenominator = 2;
                setSnapToBeat(halfNoteLengthMenuItem, noteLengths);
            });

            quarterNoteLengthMenuItem.setOnAction(event1 -> {
                snapBeatDenominator = 4;
                setSnapToBeat(quarterNoteLengthMenuItem, noteLengths);
            });

            eighthNoteLengthMenuItem.setOnAction(event1 -> {
                snapBeatDenominator = 8;
                setSnapToBeat(eighthNoteLengthMenuItem, noteLengths);
            });

            sixteenNoteLengthMenuItem.setOnAction(event1 -> {
                snapBeatDenominator = 16;
                setSnapToBeat(sixteenNoteLengthMenuItem, noteLengths);
            });

        });

        playButton.setOnAction(event -> {
            if (audioFileLoaded) {
                if (mp.getStatus() == Status.UNKNOWN || mp.getStatus() == Status.HALTED) {
                    return;
                }
                if (mp.getStatus() == Status.PAUSED || mp.getStatus() == Status.READY || mp.getStatus() == Status.STOPPED) {
                    if (atEndOfMedia) {
                        mp.seek(mp.getStartTime());
                        atEndOfMedia = false;
                    }
                    startTime = System.currentTimeMillis() + offset;
                    mp.play();

                } else if (generateBPMMode) {
                    mp.pause();
                }
            }
        });

        timeSlider.valueProperty().addListener(ov -> {
            if (audioFileLoaded && timeSlider.isValueChanging()) {
                    // multiply duration by percentage calculated by slider position
                    mp.seek(duration.multiply(timeSlider.getValue() / 100.0));
                }
        });


        volumeSlider.valueProperty().addListener(ov -> {
            if (audioFileLoaded && volumeSlider.isValueChanging()) {
                    mp.setVolume(volumeSlider.getValue() / 100.0);
                }
        });

        //       Queue<AudioClip> audioClipQueue = new ArrayDeque<>(32);


        node.setOnKeyPressed(event -> {
            if (generateBPMMode) {
                keyCodes.stream().filter(keyCode -> event.getCode() == keyCode).forEach(keyCode -> {
                    updateBPM(bpmTimeList);
                    hitsounds[hitsoundIndex].setFramePosition(0);
                    hitsounds[hitsoundIndex].start();
                    hitsoundIndex = (hitsoundIndex + 1) % hitsounds.length;
                });
            }
            if (mp != null && beatmapMode) {
                    for (int i = 0; i < keyCodes.size(); i++) {
                        if (event.getCode() == keyCodes.get(i)) {
//                            hitsounds[hitsoundIndex].setFramePosition(0);
//                            hitsounds[hitsoundIndex].start();
//                            hitsoundIndex = (hitsoundIndex + 1) % hitsounds.length;
                            hitObjectNumber++;
                            if (snapToBeat) updateBeatmapSnap(hitObjects, i);
                            else updateBeatmap(hitObjects, i);
                        }
                    }
                }
        });


        bpmField.textProperty().addListener(((observable, oldValue, newValue) -> {
            try {
                bpm = (int) Math.round(Double.parseDouble(newValue));
            } catch (NumberFormatException e) {
                bpm = (int) Double.parseDouble(oldValue);
            }
            if (snapToBeat)
                snapLength = HitObject.round((1 / ((double) bpm * snapBeatDenominator)) * 60000, 3);
        }));

        bpmField.setOnAction(event -> {
            //double tempBPM = Double.parseDouble(bpmField.getText());
            //bpm = (int) Math.round(tempBPM);
            //bpm = (int) Math.round(Double.parseDouble(bpmField.getText()));
            bpmField.getParent().requestFocus();
        });

        offsetTextField.textProperty().addListener(((observable, oldValue, newValue) -> {
            try {
                offset = Integer.parseInt(newValue);
            } catch (NumberFormatException e) {
                offset = Integer.parseInt(oldValue);
            }
        }));

        offsetTextField.setOnAction(event -> {
            offsetTextField.getParent().requestFocus();
        });

        clearBeatmapButton.setOnAction(event -> {
            hitObjects.clear();
            hitObjectNumber = 0;
        });


        exportBeatmapMenuItem.setOnAction(event -> {
            if (hitObjects.isEmpty()) {
                Alert alert = new Alert(AlertType.WARNING);
                alert.setTitle("Beatmap Warning");
                alert.setHeaderText(null);
                alert.setContentText("You have no beatmap. Stop being lazy and make one already!");
                alert.showAndWait();
            } else {
                FileChooser fileChooser = new FileChooser();
                fileChooser.setTitle("Choose a location to save");
                fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Text Files", ".txt"));
                File exportFile = fileChooser.showSaveDialog(this.node.getScene().getWindow());
                try {
                    BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(exportFile));
                    bufferedWriter.write(bpm + " o" + offset + " ");
                    if (snapToBeat) {
                        bufferedWriter.write(snapToBeat + " " + snapBeatDenominator);
                        for (int i = 1; i < keyNumber + 1; i++) {
                            bufferedWriter.newLine();
                            bufferedWriter.write(i + " ");
                            for (HitObject hitObject : hitObjects) {
                                if (hitObject.getKey() == i) {
                                    if (!hitObject.getHitDuration().isEmpty() && hitObject.getHitDurationMixedNumber().compareTo(new MixedNumber(0, 0, 0)) != 0)
                                    bufferedWriter.write(hitObject.getBeatMixedNumber().getWholeInt() + "n"
                                            + hitObject.getBeatMixedNumber().getNumerator()
                                            + "d" + hitObject.getHitDurationMixedNumber() + " ");
                                    else bufferedWriter.write(hitObject.getBeatMixedNumber().getWholeInt() + "n"
                                            + hitObject.getBeatMixedNumber().getNumerator() + " ");
                                }
                            }
                        }
                    } else {
                        bufferedWriter.write("" + snapToBeat);
                        for (int i = 1; i < keyNumber + 1; i++) {
                            bufferedWriter.newLine();
                            bufferedWriter.write(i + " ");
                            for (HitObject hitObject : hitObjects) {
                                if (hitObject.getKey() == i) {
                                    if (!hitObject.getHitDuration().isEmpty() &&
                                            hitObject.getHitDurationMixedNumber().compareTo(new MixedNumber(0, 0, 0)) != 0)
                                        bufferedWriter.write(hitObject.getBeat() + hitObject.getHitDuration() + " ");
                                    else bufferedWriter.write(hitObject.getBeat() + " ");
                                }
                            }
                        }
                    }
                    System.out.println("File saved successfully. File path: " + exportFile.getAbsolutePath());
                    bufferedWriter.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        });
    }

    private void setSnapToBeat(CheckMenuItem menuItem, CheckMenuItem[] menuItems) {
        for (CheckMenuItem c : menuItems) {
            if (c.isSelected()) {
                c.setSelected(false);
            }
            menuItem.setSelected(true);
            if (zeroNoteLengthMenuItem.isSelected()) {
                snapToBeat = false;
            } else {
                snapToBeat = true;
                snapLength = HitObject.round((1 / ((double) bpm * snapBeatDenominator)) * 60000, 3);
            }
        }
    }

    private void updateBeatmap(ObservableList<HitObject> ov, int keyNumber) {
        Platform.runLater(() -> {
            long longElapsed = System.currentTimeMillis() - startTime;
            long elapsedMinutes = longElapsed / 60000;
            long elapsedSeconds = (longElapsed - elapsedMinutes * 60000) / 1000;
            double elapsedMilliseconds = longElapsed - elapsedMinutes * 60000 - elapsedSeconds * 1000;
            addAndPlaySound(ov, new HitObject(hitObjectNumber, Double.toString((longElapsed / 60000.0) * bpm),
                    String.format(timeFormat, elapsedMinutes, elapsedSeconds, (int) elapsedMilliseconds),
                    keyNumber + 1, ""));
        });
    }

    private void updateBeatmapSnap(ObservableList<HitObject> ov, int keyNumber) {
        Platform.runLater(() -> {
            long longElapsed = System.currentTimeMillis() - startTime;
//            long elapsedMinutes = longElapsed / 60000;
//            long elapsedSeconds = (longElapsed - elapsedMinutes * 60000) / 1000;
//            double elapsedMilliseconds = longElapsed - elapsedMinutes * 60000 - elapsedSeconds * 1000;
            int beatInt = (int) (longElapsed / snapLength);
            int beatWhole = beatInt / snapBeatDenominator;
            if (wholeNoteLengthMenuItem.isSelected()) {
                long elapsedMinutes = (long) (beatWhole / (double) bpm);
                long elapsedSeconds = (long) ((((beatWhole / (double) bpm)) - elapsedMinutes) * 60);
                double elapsedMilliseconds = ((((beatWhole / (double) bpm) - elapsedMinutes) * 60) - elapsedSeconds) * 1000;
                int beat = beatWhole + (((longElapsed / snapLength) - beatInt) >= (snapLength / 2) ? 1 : 0);
                HitObject ho= new HitObject(hitObjectNumber, "" + beat, String.format(timeFormat,
                        elapsedMinutes, elapsedSeconds, (int) elapsedMilliseconds), keyNumber + 1, "");
                if (ov.isEmpty() || !ho.isEqualTo(ov.get(ov.size() - 1))) addAndPlaySound(ov, ho);
            } else {
                int beatNumerator = beatInt - beatWhole * snapBeatDenominator + (((longElapsed / snapLength) - beatInt) >= (snapLength / 2) ? 1 : 0);
//                System.out.println("Snap Length: " + snapLength);
//                System.out.println("Beat Int: " + beatInt + "\n Beat Whole: " + beatWhole + "\n Beat Numerator: " + beatNumerator);
                long elapsedMinutes = (long) ((beatWhole + (double) beatNumerator / snapBeatDenominator) / (double) bpm);
                long elapsedSeconds = (long) ((((beatWhole + (double) beatNumerator / snapBeatDenominator) / (double) bpm) - elapsedMinutes) * 60);
                double elapsedMilliseconds = ((((beatWhole + (double) beatNumerator / snapBeatDenominator) / (double) bpm) - elapsedMinutes) * 60 - elapsedSeconds) * 1000;
                HitObject ho = new HitObject(hitObjectNumber, beatWhole + " " + beatNumerator + "/" + snapBeatDenominator,
                        String.format(timeFormat, elapsedMinutes, elapsedSeconds, (int) elapsedMilliseconds),
                        keyNumber + 1, "");
                if (ov.isEmpty() || !ho.isEqualTo(ov.get(ov.size() - 1))) addAndPlaySound(ov, ho);
            }
        });
    }

    private void refreshBeatmapByBeat(ObservableList<HitObject> ov) {
        Collections.sort(ov);
        for (HitObject hitObject : ov) {
            hitObject.setHitObject(ov.indexOf(hitObject) + 1);
        }
    }

    private void refreshBeatmapByHitObject(ObservableList<HitObject> ov) {
 /*       Collections.sort(ov, (o1, o2) -> {
            if (o1.getHitObject() > o2.getHitObject()) return 1;
            else if (o1.getHitObject() < o2.getHitObject()) return -1;
            else return 0;
        });*/
        for (HitObject hitObject : ov) {
            hitObject.setHitObject(ov.indexOf(hitObject) + 1);
        }

    }

    private void updateBPM(List<Double> list) {
        if (list.isEmpty()) {
            list.add((double) 0);
        } else {
            list.add((System.currentTimeMillis() - oldTime) / 60000);
        }
        oldTime = System.currentTimeMillis();
        if (list.size() > 16) {
            list.remove(0);
        }
        double totalTime = list.stream().mapToDouble(Double::doubleValue).sum();
        bpm = (int) Math.round((list.size()) / totalTime);
        //bpmField.setText(String.valueOf((list.size() - 1) / totalTime));
        bpmField.setText(String.valueOf(bpm));
    }

    private void updateValues() {
        if (timeLabel != null && timeSlider != null && volumeSlider != null) {
            Platform.runLater(() -> {
                Duration currentTime = mp.getCurrentTime();
                timeLabel.setText(formatTime(currentTime, duration));
                timeSlider.setDisable(duration.isUnknown());
                if (!timeSlider.isDisabled()
                        && duration.greaterThan(Duration.ZERO)
                        && !timeSlider.isValueChanging()) {
                    timeSlider.setValue(currentTime.divide(duration.toMillis()).toMillis()
                            * 100.0);
                }
                if (!volumeSlider.isValueChanging()) {
                    volumeSlider.setValue((int) Math.round(mp.getVolume()
                            * 100));
                }
            });
        }
    }

    private void addAndPlaySound(ObservableList<HitObject> ov, HitObject ho) {
        ov.add(ho);
        hitsounds[hitsoundIndex].setFramePosition(0);
        hitsounds[hitsoundIndex].start();
        hitsoundIndex = (hitsoundIndex + 1) % hitsounds.length;
    }

    private static String formatTime(Duration elapsed, Duration duration) {
        int longElapsed = (int) Math.floor(elapsed.toSeconds());
        int elapsedHours = longElapsed / (60 * 60);
        if (elapsedHours > 0) {
            longElapsed -= elapsedHours * 60 * 60;
        }
        int elapsedMinutes = longElapsed / 60;
        int elapsedSeconds = longElapsed - elapsedHours * 60 * 60
                - elapsedMinutes * 60;

        if (duration.greaterThan(Duration.ZERO)) {
            int intDuration = (int) Math.floor(duration.toSeconds());
            int durationHours = intDuration / (60 * 60);
            if (durationHours > 0) {
                intDuration -= durationHours * 60 * 60;
            }
            int durationMinutes = intDuration / 60;
            int durationSeconds = intDuration - durationHours * 60 * 60 -
                    durationMinutes * 60;
            if (durationHours > 0) {
                return String.format("%d:%02d:%02d/%d:%02d:%02d",
                        elapsedHours, elapsedMinutes, elapsedSeconds,
                        durationHours, durationMinutes, durationSeconds);
            } else {
                return String.format("%02d:%02d/%02d:%02d",
                        elapsedMinutes, elapsedSeconds, durationMinutes,
                        durationSeconds);
            }
        } else {
            if (elapsedHours > 0) {
                return String.format("%d:%02d:%02d", elapsedHours,
                        elapsedMinutes, elapsedSeconds);
            } else {
                return String.format("%02d:%02d", elapsedMinutes,
                        elapsedSeconds);
            }
        }
    }

    private static <S> void addAutoScroll(final TableView<S> view) {
        if (view == null) {
            throw new NullPointerException();
        }

        view.getItems().addListener((ListChangeListener<S>) (c -> {
            c.next();
            final int size = view.getItems().size();
            if (size > 0) {
                view.scrollTo(size - 1);
            }
        }));
    }

}



