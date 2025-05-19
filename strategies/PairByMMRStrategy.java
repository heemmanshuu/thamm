// File: PairByMMRStrategy.java
package thamm.strategies;

import thamm.types.Player;
import thamm.types.Match;
import java.util.*;

/**
 * Default strategy: sort players by MMR and pair them up.
 */
public class PairByMMRStrategy implements MatchStrategy {
    @Override
    public List<Match> generateMatches(List<Player> bufferedPlayers) {
        List<Match> matches = new ArrayList<>();
        bufferedPlayers.sort(Comparator.comparingInt(p -> p.mmr));
        for (int i = 0; i + 1 < bufferedPlayers.size(); i += 2) {
            List<Player> pair = List.of(bufferedPlayers.get(i), bufferedPlayers.get(i + 1));
            matches.add(new Match(pair, System.currentTimeMillis()));
        }
        return matches;
    }
}