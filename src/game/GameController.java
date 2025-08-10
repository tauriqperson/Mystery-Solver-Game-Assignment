package game;

import Model.*;
import data.DatabaseManager;
import gui.GameWindow;
import javax.swing.*;
import java.io.*;
import java.sql.SQLException;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

/*Main controller class for the deteective game
* Manages game logic, player interactions and coordinates between model and view*/
public class GameController {
    private GameWindow view;
    private Case currentCase;
    private Map<String, Boolean> questionedSuspects;
    private DatabaseManager dbManager;
    private Map<String, Room> shipRooms;
    private String currentRoom;
    private Map<String, Set<String>> askedQuestions;

    //Initializes the game controller with database connection and UI setup
    //Loads initial game resources and sets up event handlers
    public GameController(){
       dbManager = new DatabaseManager();
        view = new GameWindow();
        initializeShip();
        loadCase("src/resources/case1.txt");
        loadClues("src/resources/clues.txt");
        loadQuestions("src/resources/questions.txt");
        setupEventHandlers();

        // Disable navigation buttons initially
        view.getNorthBtn().setEnabled(false);
        view.getSouthBtn().setEnabled(false);
        view.getSearchBtn().setEnabled(false);

        //Initialize tracking of asked questions
        askedQuestions = new HashMap<>();

        //Set up question tracking for each suspect
        if (currentCase != null && currentCase.getSuspects() != null){
            for (Suspect suspect : currentCase.getSuspects()){
                askedQuestions.put(suspect.getName(), new HashSet<>());
            }
        }

    }

    //Creates and connects the ship's rooms with their descriptions
    private void initializeShip(){
        shipRooms = new HashMap<>();

        //Create room objects with descriptions
        Room engineRoom = new Room("Engine Room",
                "The ship's engine room hums with activity. Massive reactors dominate the space,\n" +
                        "with pipes and conduits running along every surface. The air smells of oil and\n" +
                        "ozone. This is where Chief Engineer Harris was found dead.");
        Room crewQuarters = new Room("Crews Quarters", "Rows of beds where the crew sleeps. Personal belongings are scattered about.");
        Room bridge = new Room("Bridge", "The ship's control center. Displays and controls over every surface.");


        //Connect rooms
        engineRoom.connectRoom("Crew Quarters");
        crewQuarters.connectRoom("Engine Room");
        crewQuarters.connectRoom("Bridge");
        bridge.connectRoom("Crew Quarters");

        //Add rooms to the ship map
        shipRooms.put("Engine Room", engineRoom);
        shipRooms.put("Crew Quarters", crewQuarters);
        shipRooms.put("Bridge", bridge);

    }

    //Displays the current case information in the game view
    private void displayCaseInfo(){
        StringBuilder displayText = new StringBuilder();

        //Build case title and description
        displayText.append("=== ").append(currentCase.getTitle()).append(" ===\n\n");

        displayText.append(currentCase.getCrimeScene().replace(". ", ".\n"));

        //List all suspects
        displayText.append("\n\n=== SUSPECTS ===\n");
        for(Suspect s : currentCase.getSuspects()){
            displayText.append("• ").append(s.getName()).append(": ")
                    .append(s.getDescription()).append("\n");
        }

        view.displayText((displayText.toString()));
    }

    //Starts a new case by loading resources and initializing game state
    public void startNewCase(){
        view.setVisible(true);
        initializeShip();
        loadCase("src/resources/case1.txt");
        loadClues("src/resources/clues.txt");
        loadQuestions("src/resources/questions.txt");


        //set starting location
        currentRoom = "Engine Room";
        Room engineRoom = shipRooms.get(currentRoom);

        //Build intiial display text
        StringBuilder displayText = new StringBuilder();

        displayText.append("=== ").append(currentCase.getTitle()).append(" ===\n\n")
                .append(currentCase.getCrimeScene().replace(". ", ".\n"))
                .append("\n\n=== SUSPECTS ===\n");

        for(Suspect s : currentCase.getSuspects()) {
            displayText.append("• ").append(s.getName()).append(": ")
                    .append(s.getDescription()).append("\n");
        }

        //Add location and action information
        displayText.append("\n\n=== CURRENT LOCATION ===\n")
                .append("You are in the ").append(currentRoom).append("\n")
                .append(engineRoom.getDescription()).append("\n\n")
                .append("=== What would you like to do? ===\n")
                .append("• Search the room - Look for clues\n")
                .append("• Move to another area (North/South)");

        view.displayText(displayText.toString());

        //Enable interactiion buttons
        view.getNorthBtn().setEnabled(true);
        view.getSouthBtn().setEnabled(true);
        view.getSearchBtn().setEnabled(true);

        updateNavigationButtons();

        //reset question tracking
        askedQuestions.clear();
        for(Suspect suspect : currentCase.getSuspects()){
            askedQuestions.put(suspect.getName(), new HashSet<>());
        }
    }

    //Loads case from a text file
    private void loadCase(String filePath){

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))){
            currentCase = new Case();

            //read case title and crime scene
            currentCase.setTitle(reader.readLine());
            currentCase.setCrimeScene(reader.readLine());

            //load suspects
           List<Suspect> suspects = new ArrayList<>();
            for (int i = 0; i < 3; i++){
                String line = reader.readLine();
                if (line!= null){
                    String[] parts = line.split(":");
                    Suspect suspect = new Suspect(parts[0], parts[1], false);

                    //guilty suspect hardcoded
                    if (parts[0].equals("Samantha")) {
                        suspect.setGuilty(true);
                    }

                    suspects.add(suspect);
                }
            }

            currentCase.setSuspects(suspects);

            //Initialize questioning tracking
            questionedSuspects= new HashMap<>();
            for(Suspect s: suspects){
                questionedSuspects.put(s.getName(), false);
            }

            displayCaseInfo();
        }catch (IOException e){
            view.displayText("Error loading case file: " + e.getMessage());
            e.printStackTrace();
        }
    }

    //Saves the current game state to the database
    public void saveGame() {
        try {
            System.out.println("[SAVE] Attempting to save game from room: " + currentRoom);
            System.out.println("[SAVE] Current player: Player1");

            //Collect all discovered clues from all rooms
            List<Clue> discoveredClues = new ArrayList<>();
            for (Room room : shipRooms.values()) {
                for (Clue clue : room.getClues()) {
                    if (clue.isDiscovered()) {
                        discoveredClues.add(clue);
                    }
                }
            }
            System.out.println("[SAVE] Found " + discoveredClues.size() + " discovered clues");

            // Save game state to database
            dbManager.saveGameState(
                    "Player1",
                    currentRoom,
                    questionedSuspects,
                    discoveredClues,
                    currentCase
            );

            view.displayText("Game Saved Successfully from " + currentRoom + "!");
        } catch (SQLException e) {
            System.err.println("[SAVE ERROR] " + e.getMessage());
            e.printStackTrace();
            view.displayText("Error saving game: " + e.getMessage());
        }
    }

    //Loads a saved game state from the database
    public void loadGame() {
        try {
            System.out.println("\n[LOAD] Starting load process");

            //debug print current database state
            dbManager.debugPrintGameState();

            //load saved state from the database
            GameState savedState = dbManager.loadGameState("Player1");
            if (savedState != null) {
                //restore game state
                this.currentRoom = savedState.getCurrentRoom();
                System.out.println("[LOAD] Successfully loaded room: " + currentRoom);


                this.currentCase = savedState.getCurrentCase();
                this.questionedSuspects = savedState.getQuestionedSuspects();
                restoreClueState(savedState.getDiscoveredClues());

                //Debug print after loading
                dbManager.debugPrintGameState();

                displayCurrentGameState();

                //initialize asked questions
                if (askedQuestions == null) {
                    askedQuestions = new HashMap<>();
                }

                //enable interaction buttons
                view.getNorthBtn().setEnabled(true);
                view.getSouthBtn().setEnabled(true);
                view.getSearchBtn().setEnabled(true);

                //Initialize question tracking for suspects
                if (currentCase != null && currentCase.getSuspects() != null) {
                    for (Suspect suspect : currentCase.getSuspects()) {
                        if (!askedQuestions.containsKey(suspect.getName())) {
                            askedQuestions.put(suspect.getName(), new HashSet<>());
                        }
                    }
                }
                 view.displayText("Game Loaded Successfully!\n" +
                        "You're in the " + currentRoom);
                 updateNavigationButtons();
            } else {
                view.displayText("No saved game found.");
            }
        } catch (SQLException e) {
            System.err.println("[LOAD] Error during load: " + e.getMessage());
            view.displayText("Error loading game: " + e.getMessage());
        }
    }

    //displays the current game state in the view
    private void displayCurrentGameState(){
        if(currentCase == null) {
            view.displayText("Error: No case loaded");
            return;
        }

        StringBuilder displayText = new StringBuilder();
        displayText.append("=== ").append(currentCase.getTitle()).append(" ===\n\n")
                .append(currentCase.getCrimeScene().replace(". ", ".\n"))
                .append("\n\n=== SUSPECTS ===\n");

        for (Suspect s : currentCase.getSuspects()) {
            displayText.append("- ").append(s.getName()).append(": ")
                    .append(s.getDescription()).append("\n");
        }

        displayText.append("\n\n=== CURRENT LOCATION ===\n")
                .append("You are in the ").append(currentRoom).append("\n")
                .append(shipRooms.get(currentRoom).getDescription()).append("\n\n")
                .append("=== What would you like to do? ===\n")
                .append("- Search the room - Look for clues\n")
                .append("- Move to another area (North/South)");

        view.displayText(displayText.toString());

    }

    //Restores clue discovery state from saved game
    private void restoreClueState(List<Clue> discoveredClues){
        Set<String> discoveredClueDescriptions = discoveredClues.stream()
                .map(Clue::getDescription)
                .collect(Collectors.toSet());

        //Mark clues as discovered iun each room
        for (Room room : shipRooms.values()){
            for (Clue clue : room.getClues()){
                if (discoveredClueDescriptions.contains(clue.getDescription())){
                    clue.setDiscovered(true);
                }
            }
        }
    }

    //Loads clues from a text file and distributes them to rooms
    private void loadClues(String filePath){
        try (Scanner scanner = new Scanner(new File(filePath))){
            List<Clue> clues = new ArrayList<>();

            //Read all clues from file
            while (scanner.hasNextLine()){
                String clueDescription = scanner.nextLine().trim();
                if (!clueDescription.isEmpty()) {
                    clues.add(new Clue(clueDescription, false));
                }
            }

            //Distribute clues to specific rooms
            if (shipRooms != null) {
                shipRooms.get("Engine Room").addClue(clues.get(0));
                shipRooms.get("Engine Room").addClue(clues.get(1));
                shipRooms.get("Crew Quarters").addClue(clues.get(2));
                shipRooms.get("Bridge").addClue(clues.get(3));
            }
        }catch (FileNotFoundException e) {
            view.displayText("Error loading clues file: " + e.getMessage());
        }
    }

    //Displays all discovered clues in the game view
    private void displayClues(){
       if (currentCase == null){
           view.displayText("No case loaded");
           return;
       }

       StringBuilder sb = new StringBuilder();
       sb.append("=== DISCOVERED CLUES ===\n");

       boolean anyCluesFound = false;

       //check all rooms for discovered clues
       for(Room room : shipRooms.values()){
           for (Clue clue : room.getClues()){
               if(clue.isDiscovered()){
                   sb.append("- ").append(clue.getDescription()).append("\n");
                   anyCluesFound = true;
               }
           }
       }

       if(!anyCluesFound){
           sb.append("You haven't found any clues yet! \n");
           sb.append("Search rooms to discover clues");
       }

       view.displayText(sb.toString());
    }

    //Loads interrogation questions from a text file
    private List<String> loadQuestions(String filePath) {
        List<String> questions = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))){
            String line;
            while ((line = reader.readLine()) != null){
                if(!line.trim().isEmpty()){
                    questions.add(line.trim());
                }
            }
        }catch (IOException e){
            System.err.println("Error loading questions: " + e.getMessage());

            //Default questions if file can't be loaded
            questions.add("What were you doing at the time of the incident?");
            questions.add("Do you have an alibi?");
            questions.add("Did you know the victim well?");
        }
        return questions;
    }

    //Handles the suspect questioning process
    private void questionSuspects() {
        if (currentCase == null || currentCase.getSuspects() == null || currentCase.getSuspects().isEmpty()) {
            view.displayText("No suspects available. Load a case first.");
            return;
        }

        List<String> allQuestions = loadQuestions("src/resources/questions.txt");

        //Get suspect names for dialog
        String[] suspectNames = currentCase.getSuspects().stream()
                .map(Suspect::getName)
                .toArray(String[]::new);

        //Show suspect selection dialog
        String suspectName = (String) JOptionPane.showInputDialog(
                view,
                "Who do you want to question?",
                "Select Suspect",
                JOptionPane.PLAIN_MESSAGE,
                null,
                suspectNames,
                suspectNames[0]);


        if (suspectName == null) return;

        //Get question not yet asked to this suspect
        Set<String> alreadyAsked = askedQuestions.getOrDefault(suspectName, new HashSet<>());
        List<String> availableQuestions = allQuestions.stream()
                .filter(q -> !alreadyAsked.contains(q))
                .collect(Collectors.toList());

        if (availableQuestions.isEmpty()) {
            view.displayText("You've already asked " + suspectName + " all available questions!");
            return;
        }

        //Select 3 random questions
        Collections.shuffle(availableQuestions);
        int questionsToShow = Math.min(3, availableQuestions.size());
        List<String> selectedQuestions = availableQuestions.subList(0, questionsToShow);

//Show question selection dialog (pop up window)
        String question = (String) JOptionPane.showInputDialog(
                view,
                "Select a question to ask " + suspectName + ":",
                "Question Suspect",
                JOptionPane.PLAIN_MESSAGE,
                null,
                selectedQuestions.toArray(),
                selectedQuestions.get(0));

        if (question != null) {
            //Track asked question and generate response
            askedQuestions.get(suspectName).add(question);
            questionedSuspects.put(suspectName, true);

            Suspect suspect = currentCase.getSuspects().stream()
                    .filter(s -> s.getName().equals(suspectName))
                    .findFirst()
                    .orElse(null);

            if (suspect != null) {
                String response = generateResponse(suspect, question);
                view.displayText("Question to " + suspectName + ":\n" +
                        "\"" + question + "\"\n\n" +
                        "Response:\n" +
                        "\"" + response + "\"");
            }
        }

    }

    //Generates a response based on the suspect and question
    private String generateResponse(Suspect suspect, String question){
        String questionLower = question.toLowerCase();

        if (suspect.getName().equals("Samantha")) {  //GUILTY SUSPECT
            if (questionLower.contains("what were you doing") || questionLower.contains("alibi")) {
                return "I was... um... reorganizing medical supplies alone. Nobody saw me!";
            }
            else if (questionLower.contains("fingerprints") || questionLower.contains("oxygen canister")) {
                return "*gulps* I-I check equipment regularly! Maybe I touched it then?";
            }
            else if (questionLower.contains("conflict") || questionLower.contains("victim")) {
                return "He was threatening to report my... I mean, we got along fine!";
            }
            else if (questionLower.contains("vent cover")) {
                return "That was already broken when I... uh, I know nothing about it!";
            }
            else if (questionLower.contains("suspicious")) {
                return "Why are you looking at ME like that? I'm not the suspicious one!";
            }
            return "I don't recall anything about that!"; // Default for unexpected questions
        }
        else {  //INNOCENT SUSPECTS
            if (questionLower.contains("what were you doing") || questionLower.contains("alibi")) {
                return "I was at my regular station. Others can confirm.";
            }
            else if (questionLower.contains("fingerprints") || questionLower.contains("oxygen canister")) {
                return "I perform routine maintenance on that equipment weekly.";
            }
            else if (questionLower.contains("conflict") || questionLower.contains("victim")) {
                return "We had standard workplace relations. No major issues.";
            }
            else if (questionLower.contains("vent cover")) {
                return "I reported that damage during last week's inspection.";
            }
            else if (questionLower.contains("suspicious")) {
                return "Nothing unusual comes to mind. Everyone seemed normal.";
            }
            return "I don't have information about that."; //Default
        }
    }

    //Handles the accusation process
    private void makeAccusation(){

        if (currentCase == null || currentCase.getSuspects() == null || currentCase.getSuspects().isEmpty()) {
            view.displayText("No suspects available. Load a case first.");
            return;
        }

        //Get suspects names for dialog
        String[] options = currentCase.getSuspects().stream()
                .map(Suspect::getName)
                .toArray(String[]::new);

        //Show accusation dialog
        String accused = (String) JOptionPane.showInputDialog(
                view,
                "Who do you accuse?",
                "Make Accusation",
                JOptionPane.PLAIN_MESSAGE,
                null,
                options,
                options[0]);

        if (accused != null){
            Suspect suspect = currentCase.getSuspects().stream()
                    .filter(s -> s.getName().equals(accused))
                    .findFirst()
                    .orElse(null);

            //Check if accusation is correct
            if(suspect != null && suspect.isGuilty()){
                view.displayText("Correct! " + accused + " was the killer!!");
            } else {
                view.displayText("Wrong :(( the legendary killer still roams free...");
            }
        }
    }

    //Gets the room name to the north
    private String getNorthRoomName() {
        switch (currentRoom) {
            case "Engine Room":
                return "Crew Quarters";
            case "Crew Quarters":
                return "Bridge";
            default:
                return currentRoom;
        }
    }

    //Gets the room name to the south
    private String getSouthRoomName(){
        switch (currentRoom) {
            case "Bridge":
                return "Crew Quarters";
            case "Crew Quarters":
                return "Engine Room";
            default:
                return currentRoom;
        }
    }

    //Returns player to the engine room from the starting location
    public void returnToEngineRoom(){
        currentRoom = "Engine Room";
        Room engineRoom = shipRooms.get(currentRoom);

        String displayText = "You return to the Engine Room\n\n" +
                engineRoom.getDescription() + "\n\n" +
                "=== What would you like to do? ===\n" +
                "• Search the room - Look for clues\n" +
                "• Move to another area (North/South)";

        view.displayText(displayText);

        updateNavigationButtons();
    }

    //Moves player to specified room if connected
    public void moveToRoom(String roomName){
        if (shipRooms.get(currentRoom).getConnectedRooms().contains(roomName)){
            currentRoom = roomName;
            view.displayText("You enter the " + roomName + ".\n\n" +
                    shipRooms.get(roomName).getDescription());
            updateNavigationButtons();
        }else{
            view.displayText("You can't go that way from here!");
        }
    }

    //Searches current room for clues
    public void searchRoom() {
        Room room = shipRooms.get(currentRoom);
        StringBuilder sb = new StringBuilder();
        sb.append("Searching ").append(currentRoom).append("...\n\n");

        //Check all clues in room
        boolean foundNew = false;
        for (Clue clue : room.getClues()) {
            if (!clue.isDiscovered()) {
                clue.setDiscovered(true);
                sb.append("You found: ").append(clue.getDescription()).append("\n");
                foundNew = true;

                currentCase.getClues().add(clue);
            }
        }

        if (!foundNew){
            sb.append("You did not find anything new.");
        }

        view.displayText(sb.toString());
    }

//Updates navigation buttons based on current location
    private void updateNavigationButtons(){
        view.getNorthBtn().setEnabled(!currentRoom.equals("Bridge"));
        view.getSouthBtn().setEnabled(!currentRoom.equals("Engine Room"));
        view.getReturnToEngineBtn().setEnabled(!currentRoom.equals("Engine Room"));
    }

    //Sets up all UI event handlers
    private void setupEventHandlers(){

        //Game action buttons
        view.getStartCaseBtn().addActionListener(e -> startNewCase());
        view.getViewCluesBtn().addActionListener(e -> displayClues());
        view.getQuestionSuspectsBtn().addActionListener(e -> questionSuspects());
        view.getMakeAccusationBtn().addActionListener(e -> makeAccusation());

        //Navigation buttons
        view.getNorthBtn().addActionListener(e -> moveToRoom(getNorthRoomName()));
        view.getSouthBtn().addActionListener(e -> moveToRoom(getSouthRoomName()));
        view.getSearchBtn().addActionListener(e -> searchRoom());
        view.getReturnToEngineBtn().addActionListener(e -> returnToEngineRoom());
        view.getLoadBtn().addActionListener(e -> loadGame());

        //Save button with verification
        view.getSaveBtn().addActionListener(e -> {
            saveGame();
            try {
                dbManager.verifySave("Player1"); // Immediately verify the save
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        });

        //Exit button with save prompt
        view.getExitBtn().addActionListener(e -> {
            int choice = JOptionPane.showConfirmDialog(
                    view,
                    "Save before exiting?",
                    "Exit Game",
                    JOptionPane.YES_NO_OPTION);

            switch (choice){
                case JOptionPane.YES_OPTION:
                    saveGame();
                    cleanup();
                    System.exit(0);
                    break;
                case JOptionPane.NO_OPTION:
                    cleanup();
                    System.exit(0);
                    break;

            }
        });

    }

    //Cleans up resources before exiting
    public void cleanup(){
        dbManager.closeConnection();
    }

}
