package Model;

public class Suspect {
    private String name;
    private String description;
    private boolean isGuilty;

    //Constructor
    public Suspect(String name, String description, boolean isGuilty){
        this.name = name;
        this.description = description;
        this.isGuilty = isGuilty;

    }

    //getters and setters
    public String getName(){
        return name;
    }
    public String getDescription(){
        return description;
    }
    public boolean isGuilty(){
        return isGuilty;
    }
    public void setGuilty(boolean guilty){
        this.isGuilty = guilty;
    }

}
