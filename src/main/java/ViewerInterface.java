public interface ViewerInterface {

    public void processUpdate(IndexUpdate update);
    public void resend();
    public long getViewerId();
    public void run();

}
