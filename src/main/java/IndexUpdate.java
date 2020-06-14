/*
Simple game update characterised by an index:
 - 0 for group chat
 - X for player X
with a message, usually a request.
 */


public class IndexUpdate {

    private int index;
    private String message;
    private UpdateType updateType;

    public IndexUpdate(int index, String message, UpdateType updateType) {
        this.index = index;
        this.message = message;
        this.updateType = updateType;
    }

    public int getIndex() {
        return this.index;
    }

    public String getMessage() {
        return this.message;
    }

    public UpdateType getUpdateType() {
        return this.updateType;
    }

    public String toString() {
        return getIndex() + ": " + getMessage() + " Type: " + getUpdateType();
    }

}
