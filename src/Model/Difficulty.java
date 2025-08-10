package Model;

//Represents the difficulty levels available for detective cases
public enum Difficulty{

    EASY("Easy"),
    MEDIUM("Medium"), 
    HARD("Hard");
    
    private final String displayName;
    
    Difficulty(String displayName){
        this.displayName = displayName;
    }
    public String getDisplayName(){
        return displayName;
    }
    @Override
    public String toString(){
        return displayName;
    }
}
