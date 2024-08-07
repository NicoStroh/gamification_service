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

        int roundedAchiever = (int) Math.round(achieverPercentage);
        int roundedExplorer = (int) Math.round(explorerPercentage);
        int roundedSocializer = (int) Math.round(socializerPercentage);
        int roundedKiller = (int) Math.round(killerPercentage);


        int sum = roundedAchiever + roundedExplorer + roundedSocializer + roundedKiller;

        // If sum is not 200, distribute rest
        if (sum != 200) {
            int difference = 200 - sum;

            // Less than 200, distribute rest to smallest value
            if (difference > 0) {
                if (roundedAchiever <= roundedExplorer && roundedAchiever <= roundedSocializer && roundedAchiever <= roundedKiller) {
                    roundedAchiever += difference;
                } else if (roundedExplorer <= roundedAchiever && roundedExplorer <= roundedSocializer && roundedExplorer <= roundedKiller) {
                    roundedExplorer += difference;
                } else if (roundedSocializer <= roundedAchiever && roundedSocializer <= roundedExplorer && roundedSocializer <= roundedKiller) {
                    roundedSocializer += difference;
                } else {
                    roundedKiller += difference;
                }
            } // More than 200, decrease the highest value
            else {
                if (roundedAchiever >= roundedExplorer && roundedAchiever >= roundedSocializer && roundedAchiever >= roundedKiller) {
                    roundedAchiever += difference;
                } else if (roundedExplorer >= roundedAchiever && roundedExplorer >= roundedSocializer && roundedExplorer >= roundedKiller) {
                    roundedExplorer += difference;
                } else if (roundedSocializer >= roundedAchiever && roundedSocializer >= roundedExplorer && roundedSocializer >= roundedKiller) {
                    roundedSocializer += difference;
                } else {
                    roundedKiller += difference;
                }
            }

        }

        return new PlayerTypeEntity(userUUID,
                roundedAchiever,
                roundedExplorer,
                roundedSocializer,
                roundedKiller);

    }

    /**
     * Evaluates the answers for the question and calculates the player types.
     *
     * @param userUUID UUID of the user
     *
     * The questions of the test and its evalutation are the following:
     *
     *                 Question 0:
     *                 - Are you interested in the Bloom's Taxonomy level of other students?
     *                 - Yes -> S, K
     *                 - No -> A, E
     *
     *                 Question 1:
     *                 - Would you like to see which position you have on a leaderboard?
     *                 - Yes -> A, K
     *                 - No ->
     *
     *                 Question 2:
     *                 - Are you interested in who has gathered the most experience points in the month?
     *                 - Yes -> A, K
     *                 - No -> E
     *
     *                 Question 3:
     *                 - Do you like to collect experience points?
     *                 - Yes -> A, K
     *                 - No ->
     *
     *                 Question 4:
     *                 - Is a user profile important for you?
     *                 - Yes -> S
     *                 - No ->
     *
     *                 Question 5:
     *                 - Do you like to display badges or achievements in your user profile?
     *                 - Yes -> A, E, S
     *                 - No ->
     *
     *                 Question 6:
     *                 - Do you like to have a level system?
     *                 - Yes -> A, E, K
     *                 - No ->
     *
     *                 Question 7:
     *                 - Do you like to customize your avatar/user profile with for example clothes, hats, ...?
     *                 - Yes -> S
     *                 - No ->
     *
     *                 Question 8:
     *                 - Do you like to unlock new or hidden content?
     *                 - Yes -> E, S
     *                 - No ->
     *
     *                 Question 9:
     *                 - If you have the choice to beat an end boss in a team or alone, what would you choose?
     *                 - Fighting in a team -> S
     *                 - Fighting alone -> K
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
