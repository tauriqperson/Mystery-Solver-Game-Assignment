package Model;

import java.util.ArrayList;
import java.util.List;

public class Room {
    private String name;
    private String description;
    private List<Clue> clues;
    private List<String> connectedRooms;

//Constructor
    public Room(String name, String description){
        this.name = name;
        this.description = description;
        this.clues = new ArrayList<>();
        this.connectedRooms = new ArrayList<>();
    }

    //getters and setters
    public String getName(){
        return name;
    }
    public String getDescription(){
        return description;
    }
    public List<Clue> getClues(){
        return clues;
    }
    public List<String> getConnectedRooms(){
        return connectedRooms;
    }

    public void addClue(Clue clue){
        clues.add(clue);
    }
    public void connectRoom(String roomName){
        connectedRooms.add(roomName);
    }
}
