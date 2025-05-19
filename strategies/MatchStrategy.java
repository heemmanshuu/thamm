// File: MatchStrategy.java
package thamm.strategies;

import thamm.types.Player;
import thamm.types.Match;
import java.util.List;

/**
 * MatchStrategy interface: game developers implement this to define their matchmaking logic.
 */
public interface MatchStrategy {
    List<Match> generateMatches(List<Player> bufferedPlayers);
}