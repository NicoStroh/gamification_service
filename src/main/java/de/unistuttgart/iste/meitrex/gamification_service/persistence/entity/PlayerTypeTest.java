package de.unistuttgart.iste.meitrex.gamification_service.persistence.entity;

import lombok.Getter;

import java.io.File;
import java.net.URL;
import java.util.Enumeration;
import java.util.UUID;

@Getter
public class PlayerTypeTest {

    /**
     * Indicator function for a condition.
     *
     * @param condition the condition of the indicator function
     * @return (int)1 if condition, (int)0 else
     */
    public static int IA(boolean condition) {

        if (condition) {
            return 1;
        }
        return 0;

    }

    public boolean justCreated;

    private PlayerTypeTestQuestion[] questions = new PlayerTypeTestQuestion[]{};

    public PlayerTypeTest() {

        justCreated = true;
        String questionsPath = getQuestionsPath();

        if (null != questionsPath) {

            File[] questionFiles = new File(questionsPath).listFiles();

            if (null != questionFiles) {

                this.questions = new PlayerTypeTestQuestion[questionFiles.length];

                for (File questionFile : questionFiles) {
                    String questionPath = questionFile.getPath();
                    PlayerTypeTestQuestion question = PlayerTypeTestQuestion.ParseJsonFile(questionPath);

                    if (null != question) {
                        questions[question.getId()] = question;
                    }

                }

            }

        }

    }

    private String getQuestionsPath() {

        String questionsPath = null;
        try {

            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            Enumeration<URL> resources = classLoader.getResources("questions");

            while (resources.hasMoreElements()) {
                URL url = resources.nextElement();
                String fileName = url.getFile();
                if (fileName.endsWith("questions")) {
                    questionsPath = fileName;
                }
            }

        } catch (Exception ignored) {

        }
        return questionsPath;

    }

    public void setAnswer(int questionId, boolean selectedAnswer) {
        this.questions[questionId].setSelectedOption(selectedAnswer);
        justCreated = false;
    }

    private double calculateAchieverPercentage() {
        return (double) (100 * (IA(this.questions[0].getSelectedOption())
                + IA(! this.questions[1].getSelectedOption())
                + IA(! this.questions[2].getSelectedOption())
                + IA(! this.questions[3].getSelectedOption())
                + IA(! this.questions[5].getSelectedOption())
                + IA(! this.questions[6].getSelectedOption()))) / 6;
    }

    private double calculateExplorerPercentage() {
        return (double) (100 * (IA(this.questions[0].getSelectedOption())
                + IA(this.questions[2].getSelectedOption())
                + IA(! this.questions[5].getSelectedOption())
                + IA(! this.questions[6].getSelectedOption())
                + IA(! this.questions[8].getSelectedOption()))) / 5;
    }

    private double calculateSocializerPercentage() {
        return (double) (100 * (IA(! this.questions[0].getSelectedOption()))
                + IA(! this.questions[4].getSelectedOption())
                + IA(! this.questions[5].getSelectedOption())
                + IA(! this.questions[7].getSelectedOption())
                + IA(! this.questions[8].getSelectedOption())
                + IA(! this.questions[9].getSelectedOption())) / 6;
    }

    private double calculateKillerPercentage() {
        return (double) (100 * (IA(! this.questions[0].getSelectedOption())
                + IA(! this.questions[1].getSelectedOption())
                + IA(! this.questions[2].getSelectedOption())
                + IA(! this.questions[3].getSelectedOption())
                + IA(! this.questions[6].getSelectedOption())
                + IA(this.questions[9].getSelectedOption()))) / 6;
    }

    /**
     * Normalizes the percentageValues if there is at least one > 100
     *
     * @param achieverPercentage Achiever
     * @param explorerPercentage Explorer
     * @param socializerPercentage Socializer
     * @param killerPercentage Killer
     *
     * @return a PlayerTypeEntity with normalized values
     */
    private static PlayerTypeEntity normalizeValues(UUID userUUID,
                                                    double achieverPercentage,
                                                    double explorerPercentage,
                                                    double socializerPercentage,
                                                    double killerPercentage) {

        // Sum up to 200
        double coefficient = 200 / (achieverPercentage + explorerPercentage
                + socializerPercentage + killerPercentage);

        achieverPercentage *= coefficient;
        explorerPercentage *= coefficient;
        socializerPercentage *= coefficient;
        killerPercentage *= coefficient;


        // Do any values exceed 100?
        if (achieverPercentage > 100) {
            double excess = achieverPercentage - 100;
            double distribution = 1 + (excess / (200 - achieverPercentage));
            explorerPercentage *= distribution;
            socializerPercentage *= distribution;
            killerPercentage *= distribution;
            achieverPercentage = 100;
        }
        if (explorerPercentage > 100) {
            double excess = explorerPercentage - 100;
            double distribution = 1 + (excess / (200 - explorerPercentage));
            achieverPercentage *= distribution;
            socializerPercentage *= distribution;
            killerPercentage *= distribution;
            explorerPercentage = 100;
        }
        if (socializerPercentage > 100) {
            double excess = socializerPercentage - 100;
            double distribution = 1 + (excess / (200 - socializerPercentage));
            achieverPercentage *= distribution;
            explorerPercentage *= distribution;
            killerPercentage *= distribution;
            socializerPercentage = 100;
        }
        if (killerPercentage > 100) {
            double excess = killerPercentage - 100;
            double distribution = 1 + (excess / (200 - killerPercentage));
            achieverPercentage *= distribution;
            explorerPercentage *= distribution;
            socializerPercentage *= distribution;
            killerPercentage = 100;
        }

        return new PlayerTypeEntity(userUUID,
                (int) Math.round(achieverPercentage),
                (int) Math.round(explorerPercentage),
                (int) Math.round(socializerPercentage),
                (int) Math.round(killerPercentage));

    }

    /**
     * Evaluates the answers for the question and calculates the player types.
     *
     * @param userUUID UUID of the user
     *
     * @return a PlayerTypeEntity, representing the player types of the user
     */
    public PlayerTypeEntity evaluateTest(UUID userUUID) {

        double achieverPercentage = this.calculateAchieverPercentage();
        double explorerPercentage = this.calculateExplorerPercentage();
        double socializerPercentage = this.calculateSocializerPercentage();
        double killerPercentage = this.calculateKillerPercentage();

        return normalizeValues(userUUID,
                achieverPercentage,
                explorerPercentage,
                socializerPercentage,
                killerPercentage);

    }

}
