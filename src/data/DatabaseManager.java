package data;

import Model.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


//Manages all database operations for the detective game
//Handles player data, game state, cases, suspects and clues
public class DatabaseManager {
    private Connection connection;
    private static final String DB_URL = "jdbc:sqlite:detective.db";

    //Initializes database connection and creates tables if they don't exist
    public DatabaseManager() {
        try {

            connection = DriverManager.getConnection(DB_URL);
            initializeDatabase();
        } catch (SQLException e) {
            System.err.println("Database connection failed: " + e.getMessage());
        }
    }

    //Creates all required tables if they don't exist
    //Tthrows SQLException if any database operation fails

    private void initializeDatabase() throws SQLException {
        // SQL statements for creating all necessary tables
        String createPlayers = "CREATE TABLE IF NOT EXISTS players (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "name TEXT UNIQUE NOT NULL, " +
                "score INTEGER DEFAULT 0, " +
                "current_case INTEGER DEFAULT 1)";

        String createCases = "CREATE TABLE IF NOT EXISTS cases (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "title TEXT NOT NULL, " +
                "difficulty TEXT NOT NULL, " +
                "is_completed BOOLEAN DEFAULT FALSE)";

        String createSuspects = "CREATE TABLE IF NOT EXISTS suspects (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "case_id INTEGER, " +
                "name TEXT NOT NULL, " +
                "is_guilty BOOLEAN DEFAULT FALSE, " +
                "FOREIGN KEY (case_id) REFERENCES cases(id))";

        String createGameState = "CREATE TABLE IF NOT EXISTS game_state (" +
                "player_id INTEGER PRIMARY KEY," +
                "current_room TEXT NOT NULL," +
                "FOREIGN KEY (player_id) REFERENCES players(id))";

        String createSuspectProgress = "CREATE TABLE IF NOT EXISTS suspect_progress (" +
                "player_id INTEGER," +
                "suspect_name TEXT," +
                "questioned BOOLEAN DEFAULT FALSE," +
                "PRIMARY KEY (player_id, suspect_name)," +
                "FOREIGN KEY (player_id) REFERENCES players(id))";

        String createClueProgress = "CREATE TABLE IF NOT EXISTS clue_progress (" +
                "player_id INTEGER," +
                "clue_description TEXT," +
                "discovered BOOLEAN DEFAULT FALSE," +
                "PRIMARY KEY (player_id, clue_description)," +
                "FOREIGN KEY (player_id) REFERENCES players(id))";

        String createSavedCases = "CREATE TABLE IF NOT EXISTS saved_cases (" +
                "player_id INTEGER PRIMARY KEY," +
                "case_title TEXT NOT NULL," +
                "crime_scene TEXT NOT NULL," +
                "suspects_data TEXT NOT NULL," +
                "FOREIGN KEY (player_id) REFERENCES players(id))";


// Execute all table creation statements
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createPlayers);
            stmt.execute(createCases);
            stmt.execute(createSuspects);
            stmt.execute(createGameState);
            stmt.execute(createSuspectProgress);
            stmt.execute(createClueProgress);
            stmt.execute(createSavedCases);
        }
    }

    /**
     * Gets the database connection, reconnecting if necessary
     * Return active database connection
     * Throws SQLException if connection fails
     */
    public Connection getConnection() throws SQLException{
        if (connection == null || connection.isClosed()){
            connection = DriverManager.getConnection(DB_URL);
        }
        return connection;
    }

    //Creates a new player record in the database
    public void createPlayer(String playerName) throws SQLException {
        String sql = "INSERT INTO players (name) VALUES (?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, playerName);
            pstmt.executeUpdate();
        }
    }

    //Retrieves a player's current score
    public int getPlayerScore(String playerName) throws SQLException {
        String sql = "SELECT score FROM players WHERE name = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, playerName);
            ResultSet rs = pstmt.executeQuery();
            return rs.getInt("score");
        }
    }

    //Updates a player's progress (current case and score)
    public void updatePlayerProgress(String playerName, int caseId, int score) throws SQLException {
        String sql = "UPDATE players SET current_case = ?, score = ? WHERE name = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, caseId);
            pstmt.setInt(2, score);
            pstmt.setString(3, playerName);
            pstmt.executeUpdate();
        }
    }

    //Saves the complete game state for a player
    public void saveGameState(String playerName, String currentRoom,
                              Map<String, Boolean> questionedSuspects, List<Clue> discoveredClues, Case currentCase)
            throws SQLException {

        System.out.println("[DB] Starting save for " + playerName + " in room " + currentRoom);

        try {
            connection.setAutoCommit(false); // Start transaction

            //Get or create player
            int playerId = getPlayerId(playerName);
            if (playerId == -1) {
                System.out.println("[DB] Creating new player record");
                createPlayer(playerName);
                playerId = getPlayerId(playerName);
            }
            System.out.println("[DB] Using player ID: " + playerId);

            //Save room state (using REPLACE to handle existing records)
            String roomSQL = "INSERT OR REPLACE INTO game_state (player_id, current_room) VALUES (?, ?)";
            try (PreparedStatement pstmt = connection.prepareStatement(roomSQL)) {
                pstmt.setInt(1, playerId);
                pstmt.setString(2, currentRoom);
                int rows = pstmt.executeUpdate();
                System.out.println("[DB] Room save affected " + rows + " rows");
            }

            // Save all other game state components
            saveSuspectProgress(playerId, questionedSuspects);
            saveClueProgress(playerId, discoveredClues);
            saveCaseAndSuspects(playerId, currentCase);


            connection.commit();
            System.out.println("[DB] Save completed successfully");
        } catch (SQLException e) {
            System.err.println("[DB ERROR] Save failed: " + e.getMessage());
            connection.rollback();
            throw e;
        } finally {
            connection.setAutoCommit(true);
        }
    }

    //Saves the current case and its suspects for a player
    private void saveCaseAndSuspects(int playerId, Case currentCase) throws SQLException {
        if (currentCase == null) return;

        //Clear any existing case data for this player
        String deleteSQL = " DELETE FROM saved_cases WHERE player_id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(deleteSQL)){
            pstmt.setInt(1, playerId);
            pstmt.executeUpdate();
        }

        //Insert new case data
        String caseSQL = "INSERT INTO saved_cases (player_id, case_title, crime_scene, suspects_data) VALUES (?, ?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(caseSQL)){
            pstmt.setInt(1, playerId);
            pstmt.setString(2, currentCase.getTitle());
            pstmt.setString(3, currentCase.getCrimeScene());
            pstmt.setString(4, serializeSuspects(currentCase.getSuspects()));
            pstmt.executeUpdate();
        }
    }

    //Converts suspect list to serialized string for storage
    private String serializeSuspects(List<Suspect> suspects){
        StringBuilder sb = new StringBuilder();
        for (Suspect s : suspects){
            sb.append(s.getName()).append(";")
                    .append(s.getDescription()).append(";")
                    .append(s.isGuilty()).append("|");
        }
        return sb.toString();
    }

    //Converts serialized string back to suspect list
    private List<Suspect> deserializeSuspects(String data){
        List<Suspect> suspects = new ArrayList<>();
        if (data == null || data.isEmpty()) {
            return suspects;
        }

        //Split serialized data and reconstruct suspects
        String[] suspectEntries = data.split("\\|");
        for (String entry : suspectEntries) {
            if (entry.isEmpty()) continue;
            String[] parts = entry.split(";");
            if (parts.length >= 3) {
                Suspect s = new Suspect(parts[0], parts[1], Boolean.parseBoolean(parts[2]));
                suspects.add(s);
            }
        }
        return suspects;
    }


    //Saves which suspects have been questioned
    private void saveSuspectProgress(int playerId, Map<String, Boolean> questionedSuspects)
        throws SQLException {
            String sql = "INSERT OR REPLACE INTO suspect_progress VALUES (?, ?, ?)";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)){
                for (Map.Entry<String, Boolean> entry : questionedSuspects.entrySet()){
                    pstmt.setInt(1, playerId);
                    pstmt.setString(2, entry.getKey());
                    pstmt.setBoolean(3, entry.getValue());
                    pstmt.addBatch();
                }
                pstmt.executeBatch();
            }
        }


        //Saves which clues have been discovered
    private void saveClueProgress( int playerId, List<Clue> discoveredClues)
    throws SQLException{
        String sql = "INSERT OR REPLACE INTO clue_progress VALUES (?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)){
            for (Clue clue : discoveredClues) {
                pstmt.setInt(1, playerId);
                pstmt.setString (2, clue.getDescription());
                pstmt.setBoolean(3, clue.isDiscovered());
                pstmt.addBatch();
            }
            pstmt.executeBatch();
        }
    }

    //Loads the complete game state for a player
    public GameState loadGameState(String playerName) throws SQLException {
        System.out.println("[LOAD] Attempting to load game state for: " + playerName);

        int playerId = getPlayerId(playerName);
        if (playerId == -1) {
            System.out.println("[LOAD] No saved game found for player: " + playerName);
            return null;
        }

        //Load all components of game state
        String currentRoom = getCurrentRoom(playerId);
        System.out.println("[LOAD] Retrieved current room: " + currentRoom);


        Case currentCase = loadPlayerCase(playerId);
        Map<String, Boolean> questionedSuspects = loadQuestionedSuspects(playerId);
        List<Clue> discoveredClues = loadDiscoveredClues(playerId);

        return new GameState(playerName, currentRoom, currentCase, questionedSuspects, discoveredClues);
    }

    //Loads the current case for a player
    private Case loadPlayerCase (int playerId) throws SQLException{
        String sql = "SELECT case_title, crime_scene, suspects_data FROM saved_cases WHERE player_id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, playerId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                Case currentCase = new Case();
                currentCase.setTitle(rs.getString("case_title"));
                currentCase.setCrimeScene(rs.getString("crime_scene"));
                currentCase.setSuspects(deserializeSuspects(rs.getString("suspects_data")));
                return currentCase;
            }
        }
        return null;
    }

    //Loads which suspects have been questioned
    private Map<String, Boolean> loadQuestionedSuspects(int playerId) throws SQLException{
        Map<String, Boolean> suspects = new HashMap<>();
        String sql = "SELECT suspect_name, questioned FROM suspect_progress WHERE player_id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)){
            pstmt.setInt(1, playerId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()){
                suspects.put(rs.getString("suspect_name"), rs.getBoolean("questioned"));
            }
        }
        return suspects;
    }


    //Loads which clues have been discovered
    private List<Clue> loadDiscoveredClues(int playerId) throws SQLException{
        List<Clue> clues = new ArrayList<>();
        String sql = "SELECT clue_description FROM clue_progress WHERE player_id = ? AND discovered = TRUE";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)){
            pstmt.setInt(1, playerId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()){
                clues.add(new Clue(rs.getString("clue_description"), true));
            }
        }
        return clues;
    }

    //Gets the current room for a player and returns the name of the current room
    private String getCurrentRoom(int playerId) throws SQLException {
        String sql = "SELECT current_room FROM game_state WHERE player_id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, playerId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                String room = rs.getString("current_room");
                System.out.println("[LOAD] Found room in database: " + room);
                return room;
            }
        }
        System.out.println("[LOAD] No room found in database, using default");
        return "Engine Room"; // Fallback
    }

    //Gets the database ID for a player name
    private int getPlayerId(String playerName) throws SQLException {
        String sql = "SELECT id FROM players WHERE name = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, playerName);
            ResultSet rs = pstmt.executeQuery();
            return rs.next() ? rs.getInt("id") : -1;
        }
    }

    //Prints debug information about current database state
    public void debugPrintGameState() throws SQLException {
        System.out.println("\n=== DATABASE DEBUG INFO ===");

        //Print all players
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM players")) {
            System.out.println("Players:");
            while (rs.next()) {
                System.out.println("ID: " + rs.getInt("id") +
                        ", Name: " + rs.getString("name"));
            }
        }

        //Print all game states
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM game_state")) {
            System.out.println("Game States:");
            while (rs.next()) {
                System.out.println("Player ID: " + rs.getInt("player_id") +
                        ", Room: " + rs.getString("current_room"));
            }
        }

        System.out.println("=== END DEBUG INFO ===\n");
    }

    //Verifies that a player's game state was saved correctly
    public void verifySave(String playerName) throws SQLException {
        System.out.println("\n=== DATABASE VERIFICATION ===");

        int playerId = getPlayerId(playerName);
        if (playerId == -1) {
            System.out.println("Player not found!");
            return;
        }

        String sql = "SELECT current_room FROM game_state WHERE player_id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, playerId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                System.out.println("Saved room: " + rs.getString("current_room"));
            } else {
                System.out.println("No room saved for this player");
            }
        }
        System.out.println("=== VERIFICATION COMPLETE ===\n");
    }

//Closes the database connection
    public void closeConnection(){
        try {
            if (connection != null) {
                connection.close();
            }
        }catch (SQLException e){
            System.err.println("Error closing connection: " + e.getMessage());
        }
    }
}
