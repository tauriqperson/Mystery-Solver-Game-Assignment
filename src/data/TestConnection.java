package data;

import java.sql.*;
public class TestConnection {


    /**
     * Main test method for database operations
     * Tests database connection, table existence, and basic player operations
     */
    public static void main(String[] args){
        try {
            //Initialize database connection
            DatabaseManager db = new DatabaseManager();
            System.out.println("YES!! Database connection successful!");

            //Test if required tables exist
            testTableExists(db, "players");
            testTableExists(db, "cases");
            testTableExists(db, "suspects");

            //Test basic player operations
            testPlayerOperations(db);

            //Clean up connection
            db.closeConnection();;
        } catch(Exception e){
            System.err.println("NO!! Database error: "+ e.getMessage());
            e.printStackTrace();
        }
    }

    //Tests if a specified table exists in the database
    private static void testTableExists(DatabaseManager db, String tableName) throws SQLException{
        try(Connection conn = db.getConnection();
        ResultSet rs = conn.getMetaData().getTables(null, null, tableName, null)){
            if(rs.next()){
                System.out.println("Table '" + tableName + "' exists");
            }else {
                System.out.println("Table '" + tableName + "' DOES NOT exist");
            }
        }
    }

    //Tests basic player operations including creation, score retrieval, and progress update
    private static void testPlayerOperations(DatabaseManager db) throws SQLException{
        db.createPlayer("TestPlayer");
        System.out.println("Created test player");

        int score = db.getPlayerScore("TestPlayer");
        System.out.println("Retrieved player score: " + score);

        db.updatePlayerProgress("TestPlayer", 1, 100 );
        System.out.println("Updated player progress");
    }

}
