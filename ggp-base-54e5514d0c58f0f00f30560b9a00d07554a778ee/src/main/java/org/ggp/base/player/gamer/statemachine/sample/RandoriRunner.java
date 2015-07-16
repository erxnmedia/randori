package org.ggp.base.player.gamer.statemachine.sample;
import org.ggp.base.player.GamePlayer;
import org.ggp.base.player.gamer.Gamer;

public class RandoriRunner{
    public static final int DEFAULT_PORT = 9147;
    public static void main(String[] args) throws Exception	{
        int port = (args.length >= 1 ? Integer.parseInt(args[0]) : DEFAULT_PORT);
        Gamer gamer = new Randori();
        GamePlayer player = new GamePlayer(port, gamer);
        player.start();
    }
}