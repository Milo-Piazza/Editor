package editor;

import javafx.application.Application;
import javafx.application.Application.Parameters;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import javafx.stage.Stage;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.VPos;
import javafx.geometry.Orientation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.control.ScrollBar;
import javafx.util.Duration;
import java.util.LinkedList;
import java.util.List;

/**
 * A JavaFX application that opens, edits, and rewrites files
 * using a graphical interface similar to other text editors.
 */
public class Editor extends Application {
    /** An EventHandler to handle keys that get pressed. */
    private static final int INITIALFONTSIZE = 12;
    private int fontSize = INITIALFONTSIZE;
    private static final int FONTSIZEINCREMENT = 4;
    private static final int MARGIN = 5;
    //will probably be changed when file i/o is ready
    private static final String DEFAULTFONT = "Verdana";
    private static double BLINKINTERVAL = .5;
    private static final int INITIALSCREENWIDTH = 500;
    private static final int INITIALSCREENHEIGHT = 500;
    private int windowWidth;
    private int windowHeight;
    private Scene scene;
    private Rectangle cursor;
    private TextList currentText;
    private String filename;
    private boolean debugMode;
    private ScrollBar scrollBar;
    private class KeyEventHandler implements EventHandler<KeyEvent> {
        /** TextList containing the Text to display on the screen. */
        @Override
        public void handle(KeyEvent keyEvent) {
            /*We have 3 update cases for key events:
             *"nothing:" update nothing -- if an unused keyCode is pressed
             *"c&s:" update only the cursor and scroll bar
             *"all:" update the TextList, cursor, and scroll bar*/
            String update = "nothing";
            if (keyEvent.getEventType() == KeyEvent.KEY_PRESSED) {
                KeyCode code = keyEvent.getCode();
                if (keyEvent.isShortcutDown()) {
                    if (code == KeyCode.P) {
                        debugPrint(currentText.getCursorX() + ", " + currentText.getCursorY());
                    }
                    else if (code == KeyCode.S) {
                        currentText.write(filename);
                    }
                    else if (code == KeyCode.Z) {
                        //currentText.undo();
                        //update = "all";
                    }
                    else if (code == KeyCode.Y) {
                        //currentText.redo();
                        //update = "all";
                    }
                    else if ((code == KeyCode.PLUS) || (code == KeyCode.EQUALS)) {
                        fontSize += FONTSIZEINCREMENT;
                        currentText.setSize(fontSize);
                        cursor.setHeight(currentText.getTextHeight());
                        update = "all";
                    }
                    else if (code == KeyCode.MINUS) {
                        fontSize = Math.max(4, fontSize - 4);
                        currentText.setSize(fontSize);
                        cursor.setHeight(currentText.getTextHeight());
                        update = "all";
                    }
                }
                else {
                    if (code == KeyCode.LEFT) {
                        currentText.moveCursorLeft();
                        update = "c&s";
                    }
                    else if (code == KeyCode.RIGHT) {
                        currentText.moveCursorRight();
                        update = "c&s";
                    }
                    else if (code == KeyCode.UP) {
                        currentText.moveCursorUp();
                        update = "c&s";
                    } 
                    else if (code == KeyCode.DOWN) {
                        currentText.moveCursorDown();
                        update = "c&s";
                    }
                    else if (code == KeyCode.BACK_SPACE) {
                        currentText.backspace();
                        update = "all";
                    }
                }
            }
            else if (keyEvent.getEventType() == KeyEvent.KEY_TYPED) {
                /*since all KEY_TYPED events will change the list, we should
                 *always update everything.*/
                String characterTyped = keyEvent.getCharacter();
                if (characterTyped.length() > 0 && characterTyped.charAt(0) != 8 && !(keyEvent.isShortcutDown())) {
                    // Ignore control keys, which have non-zero length
                    // as well as the backspace key, which is
                    // represented as a character of value = 8 on Windows.
                    currentText.type(characterTyped);
                    update = "all";
                    keyEvent.consume();
                }
                //centerText();
            } 
            if (update == "all") {
                currentText.updateList(windowWidth - (int) Math.round(scrollBar.getLayoutBounds().getWidth()));
                updateCursorAndScrollBar();
            }
            else if (update == "c&s") {
                updateCursorAndScrollBar();
            }
        }
    }
    private class ClickHandler implements EventHandler<MouseEvent> {
        @Override
        public void handle(MouseEvent mouseEvent) {
            int clickedX = (int) Math.round(mouseEvent.getX());
            int clickedY = (int) Math.round(mouseEvent.getY() + scrollBar.getValue() * currentText.getTextHeight());
            currentText.moveCursor(clickedX, clickedY);
            updateCursorAndScrollBar();
        }
    }
    private class CursorBlinkHandler implements EventHandler<ActionEvent> {
        private int blinkIndex = 0;
        private Color[] blinkColors = {Color.BLACK, Color.WHITE};
        private void blink() {
            cursor.setFill(blinkColors[blinkIndex]);
            blinkIndex = (blinkIndex + 1) % blinkColors.length;
        }
        @Override
        public void handle(ActionEvent event) {
            blink();
        }
    }
    /*This method has a strange bug: it does not move the scroll bar far enough when
     *moving the cursor down to expose the whole cursor. However it does not deform
     *the text and otherwise works as intended.*/
    public void updateCursorAndScrollBar() {
        cursor.setX(currentText.getCursorX());
        int currentLine = currentText.getCursorY() / currentText.getTextHeight();
        scrollBar.setMax(Math.max(0, currentText.getLineCount() - windowHeight / currentText.getTextHeight()));
        if (currentLine <= scrollBar.getValue()) {
            scrollBar.setValue(currentLine);
        }
        else if (currentLine > scrollBar.getValue() + scene.getHeight() / currentText.getTextHeight()) {
            scrollBar.setValue(currentLine + 1 - scene.getHeight() / currentText.getTextHeight());
        }
        cursor.setY(currentText.getCursorY());
    }
    //To be called in start().
    public void beginCursorBlink(double interval) {
        final Timeline timeline = new Timeline();
        timeline.setCycleCount(Timeline.INDEFINITE);
        CursorBlinkHandler cursorBlink = new CursorBlinkHandler();
        KeyFrame keyframe = new KeyFrame(Duration.seconds(interval), cursorBlink);
        timeline.getKeyFrames().add(keyframe);
        timeline.play();
    }
    //To only be used in debug mode.
    public void debugPrint(String stringToPrint) {
        if (debugMode) {
            System.out.println(stringToPrint);
        }
    }
    @Override
    public void start(Stage primaryStage) {
        // Create a Node that will be the parent of all things displayed on the screen.
        Group root = new Group();
        Group textRoot = new Group();
        root.getChildren().add(textRoot);
        currentText = new TextList(DEFAULTFONT, INITIALFONTSIZE, MARGIN, textRoot);
        cursor = new Rectangle();
        cursor.setX(MARGIN);
        cursor.setY(0);
        cursor.setWidth(1);
        cursor.setHeight(currentText.getTextHeight());
        textRoot.getChildren().add(cursor);
        beginCursorBlink(BLINKINTERVAL);
        List<String> arguments = getParameters().getRaw();
        if (arguments.size() == 1) {
            filename = arguments.get(0);
            debugMode = false;
        }
        else if (arguments.size() >= 2) {
            filename = arguments.get(0);
            debugMode = (arguments.get(1).equals("debug"));
        }
        else {
            System.out.println("Error: expected filename");
            System.exit(1);
        }
        try {
            File inputFile = new File(filename);
            if (inputFile.exists()) {
                FileReader reader = new FileReader(inputFile);
                BufferedReader bufferedReader = new BufferedReader(reader);
                int intRead = -1;
                while ((intRead = bufferedReader.read()) != -1) {
                    char charRead = (char) intRead;
                    if ((charRead == '\r') 
                    || (charRead == '\n') 
                    || (String.valueOf(charRead) == "\n\r")) {
                        currentText.type("\r");
                    }
                    else {
                        currentText.type(String.valueOf(charRead));
                    }
                }
                bufferedReader.close();
            }
        } catch (FileNotFoundException fnfe) {
            System.out.println("File not found! Exception was: " + fnfe);
        } catch (IOException ioe) {
            System.out.println("Error while reading! Exception was: " + ioe);
        }
        // The Scene represents the window: its height and width will be the height and width
        // of the window displayed.
        windowWidth = INITIALSCREENWIDTH;
        windowHeight = INITIALSCREENHEIGHT;
        currentText.updateList(windowWidth);
        currentText.moveCursor(MARGIN, 0);
        //Initialize and configure the scroll bar
        scrollBar = new ScrollBar();
        scrollBar.setMin(0);
        scrollBar.setMax(Math.max(0, Math.round(currentText.getLineCount() - windowHeight / currentText.getTextHeight())));
        scrollBar.setOrientation(Orientation.VERTICAL);
        scrollBar.setLayoutX(windowWidth - scrollBar.getLayoutBounds().getWidth());
        scrollBar.setPrefHeight(windowHeight);
        scrollBar.valueProperty().addListener(new ChangeListener<Number>() {
                public void changed(ObservableValue<? extends Number> observableValue, 
                                    Number oldValue, 
                                    Number newValue) {
                    //Adjust the text upon scrolling down or up
                    textRoot.setLayoutY(-1 * currentText.getTextHeight() * newValue.intValue());
                }
            }
        );
        root.getChildren().add(scrollBar);
        scene = new Scene(root, windowWidth, windowHeight, Color.WHITE);
        scene.widthProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(
                ObservableValue<? extends Number> observableValue,
                Number oldScreenWidth,
                Number newScreenWidth) {
                    windowWidth = newScreenWidth.intValue();
                    currentText.updateList((int) Math.round(windowWidth - scrollBar.getLayoutBounds().getWidth()));
                    scrollBar.setLayoutX(windowWidth - scrollBar.getLayoutBounds().getWidth());
                    scrollBar.setMax(Math.max(0, Math.ceil(currentText.getLineCount() - windowHeight / currentText.getTextHeight())));
                    updateCursorAndScrollBar();
                }
            }
        );
        scene.heightProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(
                ObservableValue<? extends Number> observableValue,
                Number oldScreenHeight,
                Number newScreenHeight) {
                    windowHeight = newScreenHeight.intValue();
                    scrollBar.setMax(Math.max(0, Math.ceil(currentText.getLineCount() - windowHeight / currentText.getTextHeight())));
                    scrollBar.setPrefHeight(windowHeight);
                }
            }
        );
        // To get information about what keys the user is pressing, create an EventHandler.
        // EventHandler subclasses must override the "handle" function, which will be called
        // by javafx.
        EventHandler<KeyEvent> keyEventHandler = new KeyEventHandler();
        EventHandler<MouseEvent> clickHandler = new ClickHandler();
        // Register the event handler to be called for all KEY_PRESSED and KEY_TYPED events.
        scene.setOnKeyTyped(keyEventHandler);
        scene.setOnKeyPressed(keyEventHandler);
        scene.setOnMouseClicked(clickHandler);
        primaryStage.setTitle("Editor");
        // This is boilerplate, necessary to setup the window where things are displayed.
        primaryStage.setScene(scene);
        primaryStage.show();
    }
    public static void main(String[] args) {
        launch(args);
    }
}
