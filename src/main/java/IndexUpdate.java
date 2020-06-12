/*
Simple game update characterised by an index:
 - 0 for group chat
 - X for player X
with a message, usually a request.
 */


public class IndexUpdate {

    private int index;
    private String message;

    public IndexUpdate(int index, String message) {
        this.index = index;
        this.message = message;
    }

    public int getIndex() {
        return this.index;
    }

    public String getMessage() {
        return this.message;
    }

    public String toString() {
        return "(" + getIndex() + ": " + getMessage() + ")";
    }

}
