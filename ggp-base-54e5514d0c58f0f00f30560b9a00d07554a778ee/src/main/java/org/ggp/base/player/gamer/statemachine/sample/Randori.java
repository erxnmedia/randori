	package org.ggp.base.player.gamer.statemachine.sample;
	import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.ggp.base.player.gamer.event.GamerSelectedMoveEvent;
import org.ggp.base.util.gdl.grammar.GdlTerm;
import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;


	public final class Randori extends SampleGamer
	{
	    public int[] nodeCounter = new int[1];
	    public long time_out;
	    public long finishBy;
	    public long start;
	    public long ponder_time;
	    public Node parent;
	    public int moves;
	    public Map<MachineState,Node> edges;
	    public Set<Node> nodes;
	    public long time_margin;
	    public int max_dag_nodes;
	    public int trace_from;
	    public int max_minimax_passes;
	    public boolean trace_games;
	    public boolean randomize;
	    public boolean metagame;
	    public boolean prune;
	    public boolean monitor_gc;
	    public boolean gc_monitor_installed = false;
	    public boolean show_state;
	    public InstallGCMonitoring gc_monitor = new InstallGCMonitoring();
	    public RunCmd dot = new RunCmd();

	    public void set_player_parameters() {
		max_dag_nodes = 1000000;
		max_minimax_passes = 10000;
		trace_from = 4;
		trace_games = false;
		randomize = true;
		metagame = true;
		prune = false;
		monitor_gc = true;
		show_state = false;
		time_margin = 3000;
	    }

	    public void cleanup() {
		parent = null;
		nodeCounter[0] = 0;
		edges = new HashMap<MachineState,Node>();
		nodes = new HashSet<Node>();
	    }

	    @Override
	    public void stateMachineAbort() {
		cleanup();
		cleanupAfterMatch();
	    }
	    public void dingDing(String where) {
	    	System.out.println(where + ": Time " + (finishBy - System.currentTimeMillis()) + " ms");
	    }

	    public void setFinishBy(String which, long timeout)    {
		time_out = timeout;
		finishBy = Math.max(2000, time_out - time_margin);
		ponder_time = finishBy - System.currentTimeMillis();
		System.out.println(which + " time to ponder is " + ponder_time + "ms, margin is " + (time_out-finishBy) + " ms");
	    }

	    public void minimax() throws GoalDefinitionException, MoveDefinitionException,
	    							TransitionDefinitionException {
	    	for (int i = 1;
		     ((i < max_minimax_passes) &&
		      (nodeCounter[0] < max_dag_nodes) &&
		      (System.currentTimeMillis() < finishBy));
		     i++) {
		    try {
		    	parent.minimax(i);
		    }
		    catch (InterruptedException e) {
		    	dingDing("End MINIMAX: OUT OF TIME");
		    	return;
		    }
		}
	    }

	    @Override
		public void stateMachineMetaGame(long timeout)
		throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException, IOException
	    {
		cleanup();
		System.gc();
		moves = 0;
		set_player_parameters();
		if (monitor_gc && !gc_monitor_installed) {
			gc_monitor.installGCMonitoring();
			gc_monitor_installed=true;
		}
		setFinishBy("Metagame", timeout);
		long start = System.currentTimeMillis();
		parent = new Node(this);
		if (metagame) {
		    playBall();
		    long stop = System.currentTimeMillis();
		    dingDing("Created " + nodeCounter[0] + " new nodes in " + (stop - start) + " milliseconds");
		}
	    }

	    public void saveGV(String fileroot) throws IOException {
	    	String gv_file = fileroot + ".gv";
	    	String svg_file = fileroot + ".gv.svg";
		PrintWriter out = new PrintWriter(gv_file);
		String text = String.format("digraph g {%ngraph [%nrankdir = \"LR\"%n];%n");
		for (Node node : nodes) {
		    node.printed = false;
		}
		text += parent.toGV();
		text += String.format("}%n");
		out.println(text);
		out.close();
 		dot.runCmd("dot", "-Tsvg", gv_file, "-O");
 		dot.runCmd("C:\\Program Files (x86)\\Google\\Chrome\\Application\\chrome.exe", svg_file, "", "");
	    }

	    public JointMove getLastJointMove() {
		List<GdlTerm> lastMoves = getMatch().getMostRecentMoves();
		List<Move> meauves = new ArrayList<Move>();
		for (GdlTerm sentence : lastMoves)
		    meauves.add(stateMachine.getMoveFromTerm(sentence));
		return new JointMove(meauves);
	    }

	    public void playBall() throws GoalDefinitionException, MoveDefinitionException, TransitionDefinitionException, IOException {
		dingDing("minimax started");
		minimax();
		if (trace_games && moves >= trace_from)
		    saveGV("C:/Users/Lars/Desktop/moves/before_pruning_"+moves);
		parent.markFullyDeterminedNodes();
		parent.pruneFullyExploredMoves();
		if (trace_games && moves >= trace_from)
		    saveGV("C:/Users/Lars/Desktop/moves/after_pruning"+moves);
	    }

	    @Override
	    public Move stateMachineSelectMove(long timeout)
		throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException, IOException	{
		long start = System.currentTimeMillis();
		moves += 1;
		System.out.println("BEGIN MOVE " + moves);
		setFinishBy("Player", timeout);
		int startNodes = nodeCounter[0];
		System.out.println("Starting with parent populated to " + parent + " and initial move selection " + parent.selected);
		System.out.println("Live dag nodes " + nodeCounter[0]);
		if (moves > 1) {
		    JointMove lastMove = getLastJointMove();
		    int old = parent.counter;
		    parent = lastMove.getBranch(parent, nodeCounter, getCurrentState());
		    System.out.println("Move from node "+old+" by move "+lastMove+" to node "+parent.counter);
		    System.out.println("Move " + moves + " current state " + parent.state);
		}
		Move selected = parent.selected;
		playBall();
		selected = parent.selectRandomMove();
		long stop = System.currentTimeMillis();
		notifyObservers(new GamerSelectedMoveEvent(parent.moves, parent.selected, stop - start));
		int newNodes = nodeCounter[0] - startNodes;
		System.out.println("Created " + newNodes + " new nodes in " + (stop - start) + " milliseconds");
		System.out.println("Move " + moves + " selected move " + parent.selected);
		System.out.println("Total nodes created " + nodeCounter[0]);
		long finish = System.currentTimeMillis();
		System.out.println("END MOVE " + moves + " elapsed time " + (finish - start));
		return selected;
	    }
	}
