package Model;

import java.util.List;
import java.util.Map;

public class GameState {
    private String playerName;
    private String currentRoom;
    private Case currentCase;
    private Map<String, Boolean> questionedSuspects;
    private List<Clue> discoveredClues;

    //constructor
    public GameState(String playerName, String currentRoom, Case currentCase, Map<String, Boolean>questionedSuspects,
                     List<Clue> discoveredClues){
        this.playerName = playerName;
        this.currentRoom = currentRoom;
        this.currentCase = currentCase;
        this.questionedSuspects = questionedSuspects;
        this.discoveredClues = discoveredClues;
    }

    //getters and setters
    public String getCurrentRoom() {return currentRoom;}
    public Case getCurrentCase() { return currentCase; }
    public Map<String, Boolean>getQuestionedSuspects() {return questionedSuspects;}
    public List<Clue>getDiscoveredClues() {return discoveredClues;}

}
