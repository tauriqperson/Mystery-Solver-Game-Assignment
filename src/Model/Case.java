package Model;

import java.util.ArrayList;
import java.util.List;

public class Case {
    private String title;
    private String crimeScene;
    private List<Suspect> suspects;
    private List<Clue> clues;
    private Difficulty difficulty;

//constructor
 public Case(String title, String crimeScene, List<Suspect> suspects, List<Clue> clues, Difficulty difficulty){
     this.title = title;
     this.crimeScene = crimeScene;
     this.suspects = suspects;
     this.clues = clues;
     this.difficulty = difficulty;
 }

 public Case(){
     this("", "", new ArrayList<>(), new ArrayList<>(), Difficulty.MEDIUM);
 }

 //getters and setters
 public String getTitle(){
     return title;
 }
 public void setTitle(String title){
     this.title = title;
 }
 public String getCrimeScene(){
     return crimeScene;
 }
 public void setCrimeScene(String crimeScene){
     this.crimeScene = crimeScene;
 }
 public List<Suspect> getSuspects(){
     return suspects;
 }
 public void setSuspects(List<Suspect> suspects){
     this.suspects = suspects;
 }
 public List<Clue> getClues(){
     return clues;
 }



}


