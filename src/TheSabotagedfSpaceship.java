import game.GameController;

/**
 * The Sabotaged Spaceship - Main Game Launcher
 *
 * This class serves as the entry point for the detective mystery game "The Sabotaged Spaceship".
 * It initializes the game controller and starts a new case when launched.
 *
 * Game Description:
 * A sci-fi detective game where players investigate a murder aboard a spaceship,
 * gathering clues, questioning suspects, and ultimately determining who committed the crime.
 */

public class TheSabotagedfSpaceship {

/**
 * Main entry point for the application
 *
 * Creates the game controller and starts a new case scenario.
 * All game operations are managed through the GameController instance.
 */

 public static void main(String[] args) {
     //Initialize the game controller which handles all game logic
        GameController controller = new GameController();

        //Start the first/new case scenario
        controller.startNewCase();
    }
}
