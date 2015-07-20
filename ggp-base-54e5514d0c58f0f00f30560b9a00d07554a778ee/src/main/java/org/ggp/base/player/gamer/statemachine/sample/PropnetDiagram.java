package org.ggp.base.player.gamer.statemachine.sample;
import org.ggp.base.util.game.CloudGameRepository;
import org.ggp.base.util.game.Game;
import org.ggp.base.util.game.GameRepository;
import org.ggp.base.util.propnet.architecture.PropNet;
import org.ggp.base.util.propnet.factory.OptimizingPropNetFactory;

public class PropnetDiagram{
    public static void main(String[] args) throws Exception	{
    	GameRepository repo = new CloudGameRepository("games.ggp.org/base");
    	String gameKey = "onestep";
    	Game theGame = repo.getGame(gameKey);
    	PropNet propnet = OptimizingPropNetFactory.create(theGame.getRules());
    	String gv_file="c:/Users/Lars/Desktop/moves/aha.gv";
    	String svg_file="c:/Users/Lars/Desktop/moves/aha.gv.jpg";
    	propnet.renderToFile(gv_file);
    	RunCmd dot = new RunCmd();
 		dot.runCmd("dot", "-Tjpg", gv_file, "-O");
 		dot.runCmd("C:\\Program Files (x86)\\Google\\Chrome\\Application\\chrome.exe", svg_file, "", "");
    }
}