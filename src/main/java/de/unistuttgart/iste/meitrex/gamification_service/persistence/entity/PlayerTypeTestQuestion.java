package de.unistuttgart.iste.meitrex.gamification_service.persistence.entity;

import java.io.FileReader;
import java.io.IOException;
import com.google.gson.Gson;
import lombok.Getter;

@Getter
public class PlayerTypeTestQuestion {

    private final int id;
    private final String text;
    private final String option0;
    private final String option1;
    private boolean selectedOption;

    public PlayerTypeTestQuestion(String text) {
        this.id = 0;
        this.text = text;
        this.option0 = "Yes";
        this.option1 = "No";
    }

    public PlayerTypeTestQuestion(int id, String text, String option0, String option1) {
        this.id = id;
        this.text = text;
        this.option0 = option0;
        this.option1 = option1;
    }

    // Parst Json-Datei zu PlayerTypeTestQuestion-Objekt
    public static PlayerTypeTestQuestion ParseJsonFile(String questionPath) {
        try (FileReader reader = new FileReader(questionPath)) {

            Gson gson = new Gson();
            return gson.fromJson(reader, PlayerTypeTestQuestion.class);

        } catch (IOException e) {

        }
        return null;
    }

    public boolean getSelectedOption() {
        return this.selectedOption;
    }

    public void setSelectedOption(boolean selectedOption) {
        this.selectedOption = selectedOption;
    }

    public void print() {
        System.out.println(this.text);
        System.out.println("[0]: " + this.option0);
        System.out.println("[1]: " + this.option1);
    }

}
