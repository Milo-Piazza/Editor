package editor;

import java.util.List;
import java.util.ArrayList;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.Group;
import javafx.geometry.VPos;
import java.io.IOException;
import java.io.FileWriter;
/* TextList
 * Author: Milo Piazza
 * A container class for TextNode that includes a pointer
 * representing a cursor and a list of nodes representing
 * the start of each line.
 */
public class TextList {
    private int margin;
    private int size;
    private String font;
    private TextNode sof; //Start of File
    private TextNode eof; //End of file -- the cursor should never reach this node
    private TextNode cursor;
    private Group group;
    private List<TextNode> lineStarts; //The start of each line, whether automatically
    //from the editor or from an actual newline
    public TextList(String font, int size, int margin, Group group) {
        sof = TextNode.makeNewList();
        eof = sof.getNext();
        cursor = sof;
        lineStarts = new ArrayList<TextNode>();
        lineStarts.add(sof);
        this.margin = margin;
        this.font = font; //default Verdana
        this.size = size; //default 12?
        this.group = group;
    }
    public void moveCursorLeft() {
        if (cursor != sof) {
            cursor = cursor.getPrev();
        }
    }
    public void moveCursorRight() {
        if (cursor.getNext() != eof) {
            cursor = cursor.getNext();
        }
    }
    public void moveCursor(int x, int y) {
        int lineToMoveTo = y / getTextHeight();
        if (lineToMoveTo < 0) {
            cursor = sof;
        }
        else if (lineToMoveTo > lineStarts.size() - 1) {
            cursor = eof.getPrev();
        }
        else {
            TextNode ptr = lineStarts.get(lineToMoveTo);
            while ((ptr.getNext() != eof)
               && ((ptr.getNext().getValue().getX() < x) 
               && (!(ptr.getNext().isEndOfLine())))) {
                ptr = ptr.getNext();
            }
            if (ptr.getNext() == eof) {
                cursor = ptr;
            }
            else {
                int distanceFromLeft;
                int distanceFromRight;
                if (ptr == sof) {
                    distanceFromLeft = margin;
                }
                else {distanceFromLeft = Math.abs((int) (Math.round(ptr.getValue().getX())) - x);}
                distanceFromRight = Math.abs((int) (Math.round(ptr.getNext().getValue().getX())) - x);
                if (distanceFromLeft <= distanceFromRight) {
                    cursor = ptr.getPrev();
                }
                else {
                    cursor = ptr;
                }
            }
        }
    }
    public void moveCursorUp() {
        moveCursor(getCursorX(), getCursorY() - getTextHeight());
    }
    public void moveCursorDown() {
        moveCursor(getCursorX(), getCursorY() + getTextHeight());
    }
    public void backspace() {
        if (cursor != sof) {
            TextNode temp = cursor;
            moveCursorLeft();
            group.getChildren().remove(temp.getValue());
            temp.remove();
        }
    }
    public void type(String character) {
        TextNode newNode = new TextNode(character);
        newNode.getValue().setTextOrigin(VPos.TOP);
        group.getChildren().add(newNode.getValue());
        cursor.insert(newNode);
        moveCursorRight();
    }
    public void setFont(String newFont) {
        font = newFont;
    }
    public void setSize(int newSize) {
        size = newSize;
    }
    public String getFont() {
        return font;
    }
    public int getSize() {
        return size;
    }
    public int getCursorX() {
        if ((cursor == sof) || (cursor.isEndOfLine())) {
            return margin;
        }
        return (int) (cursor.getValue().getX() + cursor.getValue().getLayoutBounds().getWidth());
    }
    public int getCursorY() {
        if (cursor == sof) {
            return 0;
        }
        else if (cursor.isEndOfLine()) {
            return (int) cursor.getValue().getY() + getTextHeight();
        }
        return (int) cursor.getValue().getY();
    }
    public int getTextHeight() {
        Text testText = new Text("a");
        testText.setFont(Font.font(this.font, size));
        return (int) Math.round(testText.getLayoutBounds().getHeight());
    }
    public int getLineCount() {
        return lineStarts.size();
    }
    public void updateList(int windowWidth) {
        int currentX = margin;
        int currentY = 0;
        TextNode lastSpace = sof; //this should never actually set the pointer to sof.
        boolean currentLineHasSpace = false;
        lineStarts.clear();
        lineStarts.add(sof);
        TextNode ptr = sof.getNext();
        Text currentNode;
        while (ptr != eof) {
            currentNode = ptr.getValue();
            currentNode.setFont(Font.font(font, size));
            currentNode.setX(currentX);
            currentNode.setY(currentY);
            if (currentX + Math.round(currentNode.getLayoutBounds().getWidth()) > windowWidth) {
                if (currentNode.getText().equals(" ")) {
                    ptr = ptr.getNext();
                    continue;
                }
                else if (currentLineHasSpace) {
                    ptr = lastSpace;
                    ptr.changeEndOfLine(true);
                    ptr = ptr.getNext();
                }
                else {
                    ptr.changeEndOfLine(true);
                }
                lineStarts.add(ptr);
                currentX = margin;
                currentY += getTextHeight();
                currentLineHasSpace = false;
                continue;
            }
            else if (currentNode.getText().equals("\r")) {
                lineStarts.add(ptr);
                currentX = margin;
                currentY += getTextHeight();
            }
            else if ((currentNode.getText().equals(" ")) && (currentX == margin)) {
                ptr = ptr.getNext();
                continue;
            }
            else {
                currentX += (int) Math.round(currentNode.getLayoutBounds().getWidth());
                ptr.changeEndOfLine(false);
            }
            if (currentNode.getText().equals(" ")) {
                lastSpace = ptr;
                currentLineHasSpace = true;
            }
            ptr = ptr.getNext();
        }
    }
    public void write(String filename) {
        try {
            FileWriter writer = new FileWriter(filename);
            char charToWrite;
            TextNode ptr = sof;
            while (ptr.getNext() != eof) {
                ptr = ptr.getNext();
                charToWrite = ptr.getValue().getText().charAt(0);
                if (charToWrite == '\r') {
                    writer.write('\n');
                }
                else {
                    writer.write(charToWrite);
                }
            }
            writer.close();
            System.out.println("The exception-free terminal window fills you with determination.");
            System.out.println("Saved to " + filename);
        } catch (IOException ioe) {
            System.out.println("Error while writing! Exception was: " + ioe);
        }
    }
}
    