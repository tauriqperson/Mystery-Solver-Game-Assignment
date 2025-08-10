package Model;

public class Clue {
    private String description;
    private boolean isDiscovered;

    //constructor
    public Clue(String description, boolean isDiscovered){
        this.description = description;
        this.isDiscovered = isDiscovered;
    }

    //getters and setters
    public String getDescription(){
        return description;
    }
    public void setDescription(String description){
        this.description = description;
    }
    public boolean isDiscovered(){
        return isDiscovered;
    }
    public void setDiscovered(boolean discovered){
        isDiscovered = discovered;
    }

}
