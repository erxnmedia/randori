package org.ggp.base.apps.validator;

import org.ggp.base.util.game.CloudGameRepository;
import org.ggp.base.util.game.GameRepository;

/**
 * BatchValidator does game validation on all of the games in a given game repository.
 * This allows you to quickly determine which games need to be repaired, given a large
 * existing game repository with games of varying quality.
 *
 * @author schreib
 */
public final class PrintGames
{
	public static void main(String[] args)
	{
		GameRepository repo = new CloudGameRepository("games.ggp.org/base");
		for (String gameKey : repo.getGameKeys()) {
			System.out.println(gameKey);
		}
		System.out.println("Done!");
	}
}