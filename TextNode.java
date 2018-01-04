package editor;

import javafx.scene.text.Text;
/* TextNode
 * Author: Milo Piazza
 * A doubly linked list that stores JavaFX Text objects, with various
 * utility methods for adding and removing elements.
 */
public class TextNode {
    private TextNode prev = null;
    private TextNode next = null;
    private Text value;
    private boolean isEndOfLine;
    public TextNode() {
        value = null;
        isEndOfLine = false;
    }
    public TextNode(String myString) {
        value = new Text(0, 0, myString);
        isEndOfLine = myString.equals("\r");
    }
    public TextNode(Text myText) {
        value = myText;
        isEndOfLine = myText.getText().equals("\r");
    }
    public TextNode(int x, int y, String myString) {
        value = new Text(x, y, myString);
        isEndOfLine = myString.equals("\r");
    }
    //Helper method that initializes a new list to be used by TextList
    public static TextNode makeNewList() {
        TextNode sof = new TextNode();
        TextNode eof = new TextNode();
        sof.next = eof;
        eof.prev = sof;
        return sof;
    }
    public void changeEndOfLine(boolean newEndOfLine) {
        isEndOfLine = newEndOfLine;
    }
    public boolean isEndOfLine() {
        return isEndOfLine;
    }
    public TextNode getPrev() {
        return prev;
    }
    public TextNode getNext() {
        return next;
    }
    public Text getValue() {
        return value;
    }
    //Places a new node after the one calling this method
    public void insert(TextNode newNode) {
        this.next.prev = newNode;
        newNode.next = this.next;
        this.next = newNode;
        newNode.prev = this;
    }
    //Removes the node calling this method
    public void remove() {
        this.prev.next = this.next;
        this.next.prev = this.prev;
        this.prev = null;
        this.next = null;
    }
}
