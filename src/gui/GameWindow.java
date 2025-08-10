package gui;

import javax.swing.*;
import java.awt.*;

/**
 * Main game window for the Java Detective game
 * Handles all UI components and provides access to interactive elements
 */
public class GameWindow extends JFrame {
    private JButton startCaseBtn, viewCluesBtn, questionSuspectsBtn, makeAccusationBtn, exitBtn,
                    northBtn, southBtn, searchBtn, returnToEngineBtn, saveBtn, loadBtn;
    private JTextArea displayArea;

    //Constructs and initializes the game window with all UI components
    public GameWindow(){
        //Window Setup
        setTitle("Java Detective");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        //Initialize action buttons
        startCaseBtn = new JButton("Start Case");
        viewCluesBtn = new JButton("View Clues");
        questionSuspectsBtn = new JButton("Question Suspects");
        makeAccusationBtn = new JButton("Make Accusation");
        saveBtn = new JButton("Save Game");
        loadBtn = new JButton("Load Game");
        exitBtn = new JButton("Exit");

        //Initialize Navigation buttons
        northBtn = new JButton("North");
        southBtn = new JButton("South");
        searchBtn = new JButton("Search Room");
        returnToEngineBtn = new JButton("Return to Engine Room");
        returnToEngineBtn.setEnabled(false); //disabled by default

        //Configure main text display area
        displayArea = new JTextArea();
        displayArea.setEditable(false);
        displayArea.setLineWrap(true);
        displayArea.setWrapStyleWord(true);
        displayArea.setFont(new Font("Monospaced", Font.PLAIN, 14));

        //set my preferred colour scheme
        displayArea.setForeground(new Color(0, 255, 0));
        displayArea.setBackground(Color.BLACK);

        //Configure scroll pane for text area
        JScrollPane scrollPane = new JScrollPane(displayArea);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.getViewport().setBackground(Color.BLACK);

        //Style scroll bar to match theme
        JScrollBar vertical = scrollPane.getVerticalScrollBar();


        vertical.setBackground(Color.BLACK);
        vertical.setForeground(new Color(0, 150, 0));


        //create button panel for action buttons
        JPanel buttonPanel = new JPanel(new GridLayout(1, 6));
        buttonPanel.add(startCaseBtn);
        buttonPanel.add(viewCluesBtn);
        buttonPanel.add(questionSuspectsBtn);
        buttonPanel.add(makeAccusationBtn);
        buttonPanel.add(saveBtn);
        buttonPanel.add(loadBtn);
        buttonPanel.add(exitBtn);

        //create panel for navigation controls
        JPanel navPanel = new JPanel(new GridLayout(1, 3));
        navPanel.add(northBtn);
        navPanel.add(southBtn);
        navPanel.add(returnToEngineBtn);
        navPanel.add(searchBtn);


        //Apply special styling to search button
        styleSearchButton(searchBtn);

        //add components to window
        add(buttonPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        add(navPanel, BorderLayout.SOUTH);

    }

    //Applies custom styling to the search button
    public void styleSearchButton(JButton button){
        button.setForeground(Color.YELLOW);
        button.setBackground(Color.BLACK);
        button.setBorder(BorderFactory.createLineBorder(new Color(0, 180, 0)));
    }


    public void displayText(String text){
        displayArea.setText(text);
    }

    //getters
    public JButton getStartCaseBtn(){
        return startCaseBtn;
    }
    public JButton getViewCluesBtn(){
        return viewCluesBtn;
    }
    public JButton getQuestionSuspectsBtn(){
        return questionSuspectsBtn;
    }
    public JButton getMakeAccusationBtn(){
        return makeAccusationBtn;
    }
    public JButton getSaveBtn(){
        return saveBtn;
    }
    public JButton getLoadBtn(){
        return loadBtn;
    }
    public JButton getExitBtn(){
        return exitBtn;
    }
    public JButton getNorthBtn(){
        return northBtn;
    }
    public JButton getSouthBtn(){
        return southBtn;
    }
    public JButton getSearchBtn(){
        return searchBtn;
    }
    public JButton getReturnToEngineBtn(){
        return returnToEngineBtn;
    }


}
