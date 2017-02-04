package opr;

import com.vegetarianbaconite.blueapi.SynchronousBlueAPI;
import com.vegetarianbaconite.blueapi.beans.Alliance;
import com.vegetarianbaconite.blueapi.beans.Match;
import com.vegetarianbaconite.blueapi.beans.Team;
import org.apache.commons.math3.linear.CholeskyDecomposition;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;

import java.util.*;

public class Main {

    public static void main(String[] args) {
        SynchronousBlueAPI api = new SynchronousBlueAPI("1902", "OPRCalculator", "0.1");
        List<Team> teams = api.getEventTeams(2016, "flor");
        Collections.sort(teams, (o1, o2) -> o1.getTeamNumber() - o2.getTeamNumber());
        //List<Match> matches = api.getEventMatches(2016, "flor");
        double[][] matrix = new double[teams.size()][teams.size()];
        double[][] scores = new double[teams.size()][1];
        HashMap<Integer, Integer> teamToIndex = new HashMap<>();
        int index = 0;
        for (Team t : teams) {
            teamToIndex.put(t.getTeamNumber(), index);
            index++;
        }

        index = 0;
        for (Team t : teams) {
            double scoreSum = 0;
            System.out.println("Processing " + t.getTeamNumber() + "...");
            HashMap<Integer, Double> playedWith = new HashMap<>(); //key=team index, value=amount of times with that team
            for (Team other : teams) {
                playedWith.put(teamToIndex.get(other.getTeamNumber()), 0d);
            }
            for (Match m : api.getTeamEventMatches(t.getTeamNumber(), "2016flor")) {
                if (m.getCompLevel().equalsIgnoreCase("qm")) {
                    Alliance[] alliances = new Alliance[2];
                    alliances[0] = m.getAlliances().getBlue();
                    alliances[1] = m.getAlliances().getRed();
                    for (Alliance a : alliances) {
                        ArrayList<String> allianceTeams = new ArrayList<>(Arrays.asList(a.getTeams()));
                        if (allianceTeams.contains("frc" + t.getTeamNumber())) {
                            scoreSum += a.getScore();
                            for (String otherTeamString : allianceTeams) {
                                int otherTeam = teamToIndex.get(Integer.parseInt(otherTeamString.replace("frc", "")));
                                playedWith.put(otherTeam, playedWith.get(otherTeam) + 1);
                            }
                        }
                    }
                }
            }

            double[] array = new double[playedWith.values().size()];
            int dIndex = 0;
            for (Double d : playedWith.values()) {
                array[dIndex] = d;
                dIndex++;
            }

            matrix[index] = array;
            scores[index] = new double[]{scoreSum};
            index++;
        }

        System.out.println("Calculating OPRs...");
        long millis = System.currentTimeMillis();
        RealMatrix matchData = MatrixUtils.createRealMatrix(matrix);
        CholeskyDecomposition decomp = new CholeskyDecomposition(matchData);
        RealMatrix oprs = decomp.getSolver().solve(MatrixUtils.createRealMatrix(scores));
        System.out.println("Time Taken: " + (System.currentTimeMillis() - millis) + "ms");
        System.out.println("===========");
        for (int i=0;i<teams.size();i++) {
            double opr = oprs.getData()[i][0];
            Team t = teams.get(i);
            System.out.println(t.getTeamNumber() + ": " + opr);
        }
        /*
        index = 0;
        for (double[] array : matrix) {
            Team t = teams.get(index);
            String nums = "";
            for (double d : array) {
                nums = nums + d + ", ";
            }
            System.out.println(t.getNickname() + " (# " + index + ") play array: " + nums);
            index++;
        }*/
    }

}
