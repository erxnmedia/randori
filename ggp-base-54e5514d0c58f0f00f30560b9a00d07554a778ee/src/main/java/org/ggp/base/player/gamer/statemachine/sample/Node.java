package org.ggp.base.player.gamer.statemachine.sample;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;

public class Node {
    Randori player;
    MachineState state;
    Node parent;
    Move selected;
    int score;
    boolean isTerminal;
    boolean isFullyDetermined;
    int roleIndex;
    int counter;
    boolean printed;
    List<Move> moves;
    int [] scores;
    Map<Move,List<JointMove>> jointMoves;

    private static int getRoleIndex(StateMachine _machine, Role _role) {
	Map<Role, Integer> roleIndices = _machine.getRoleIndices();
	return roleIndices.get(_role);
    }

    public String stateLabel() {
	return String.format("\"state%03d\"", counter);
    }

    public void set_unprinted() {
	printed = false;
	if (moves == null)
		return;
	for (Move move : moves) {
	    for (JointMove jointMove : jointMoves.get(move)) {
		Node child = jointMove.branch;
		if (child != null)
		    child.set_unprinted();
	    }
	}
    }

    public String toGV() {
	set_unprinted();
	return toGV(1);
    }

    public void set_printed() {
	printed = true;
	if (moves == null)
		return;
	for (Move move : moves) {
	    for (JointMove jointMove : jointMoves.get(move)) {
		Node child = jointMove.branch;
		if (child != null)
		    child.set_printed();
	    }
	}
    }

    public String toGV(int move_index) {
	if (printed)
	    return "";
	String fields = "move " + move_index + "|counter " + counter + "|score " + score;
	if (player.show_state)
	    fields +=  "|state " + state;
	if (isFullyDetermined)
	    fields += "|fullyDetermined";

	String color = "green";
	if ((move_index & 1) == 0) {
	    color = "blue";
	}

	if (isTerminal)
	    fields += "|terminal";
	else
	    fields += "|move " + selected;

	String node = String.format("%s [%nlabel = \"<f0>%s\"%nshape = \"record\" color=%s%n];%n", stateLabel(), fields, color);
	String arcs = "";
	String unexploredLabel = String.format("\"unexplored%03d\"", counter);
	boolean null_branches = false;
	if (!isTerminal) {
	    String recurse = "";
	    for (Move move : moves) {
		for (JointMove jointMove : jointMoves.get(move)) {
		    String arclabel = String.format("label = \"%s\" ", jointMove);
		    String branchLabel = "";
		    String portLabel = ":f0";
		    if (jointMove.branch != null) {
			branchLabel = jointMove.branch.stateLabel();
			recurse += jointMove.branch.toGV(move_index+1);
		    }
		    else {
			branchLabel = unexploredLabel;
			portLabel = "";
			isFullyDetermined = false;
			null_branches = true;
		    }
		    arcs += String.format("{ edge [color=%s] %s:f0 -> %s%s [ %s ]};%n",
					  color, stateLabel(), branchLabel, portLabel, arclabel);
		}
	    }
	    arcs += recurse;
	}
	String unexplored = "";
	if (null_branches)
	    unexplored = String.format(" %s [%nlabel = \"unexplored %d\"%nshape = \"oval\" color=brown%n];%n",
				       unexploredLabel, counter);
	printed = true;
	return String.format("%s%s%s", node, unexplored, arcs);
    }

    public void markFullyDeterminedNodes() {
	if (isTerminal)
	    return;
	for (Move move : moves) {
	    for (JointMove jointMove : jointMoves.get(move)) {
		Node child = jointMove.branch;
		if (child == null)
		    return;
		child.markFullyDeterminedNodes();
		if (!child.isFullyDetermined)
		    return;
	    }
	}
	isFullyDetermined = true;
    }

    public void pruneOpponentsMoves(Move move, int branch_worst_score) {
	// Prune opponent's moves
	List<JointMove> newJointMoves = new ArrayList<JointMove> ();
	for (JointMove jointMove : jointMoves.get(move)) {
	    if (jointMove.branch.score == branch_worst_score) {
		newJointMoves.add(jointMove);
		break;
	    }
	}
	if (newJointMoves.size() == 1)
	    jointMoves.put(move,newJointMoves);
    }

    public void pruneMyMoves() {
    	// Prune my moves
	List<Move> newMoves = new ArrayList<Move>();
	for (int i = 0; i < moves.size(); i++) {
	    Move move = moves.get(i);
	    if (scores[i] == score) {
		newMoves.add(move);
		break;
	    }
	}
	int m = newMoves.size();
	if (m == 1) {
	    moves = newMoves;
	    scores = new int[m];
	    scores[0] = score;
	}
    }

    // TODO: Need to change this to optimize utility for other on branch in case of cooperative games
    public void pruneFullyExploredMoves() {
	if (isTerminal)
	    return;
	if (!isFullyDetermined)
	    return;
	score = 0;
	for (int i = 0; i < moves.size(); i++) {
	    Move move = moves.get(i);
	    int branch_worst_score = 100;
	    for (JointMove jointMove : jointMoves.get(move)) {
		Node child = jointMove.branch;
		if (child == null)	// Not fully explored
		    return;
		child.pruneFullyExploredMoves();	// Still not fully explored
		if (!child.isFullyDetermined)
		    return;
		branch_worst_score = Math.min(branch_worst_score, child.score);
	    }
	    isFullyDetermined = true;
 	    scores[i] = Math.max(score, branch_worst_score);
	    score = Math.max(score, scores[i]);
	    pruneOpponentsMoves(move, branch_worst_score);
	}
	pruneMyMoves();
    }

    public Move selectRandomMove() {
	selected = moves.get((int)(Math.random()*moves.size()));
	return selected;
    }

    public int minimax(int depth)
	throws GoalDefinitionException, MoveDefinitionException, TransitionDefinitionException, InterruptedException {
	if (System.currentTimeMillis() >= player.finishBy) {
	    player.dingDing("TIMEOUT depth " + depth + " score " + score);
	    throw new InterruptedException();
	}
	if (isFullyDetermined || isTerminal || depth <= 0) {
	    return score;
	}

	int selector = 0;
	List<Move> unexploredMoves = new ArrayList<Move>();
	for (Move move : moves) {
	    for (JointMove jointMove : jointMoves.get(move)) {
		Node child = jointMove.branch;
		if (child == null) {
		    unexploredMoves.add(move);
		    break;
		}
		else if (!child.isFullyDetermined) {
		    unexploredMoves.add(move);
		    break;
		}
	    }
	}

	if (unexploredMoves.size() > 0) {
		Move move = unexploredMoves.get((int)(Math.random()*unexploredMoves.size()));
		List<JointMove> jmoves = jointMoves.get(move);
		JointMove jointMove = jmoves.get((int)(Math.random()*jmoves.size()));
	  	Node child = jointMove.getBranch(this, player.nodeCounter);
	  	child.minimax(depth - 1);
	}
	return score;
    }

    public StateMachine machine() {
	return player.getStateMachine();
    }

    public Role role() {
	return player.getRole();
    }

    public MachineState getNextState(JointMove jointMove) throws TransitionDefinitionException {
    	if (isTerminal)
    		return null;
    	else
    		return machine().getNextState(state, jointMove);
    }

    @Override
    protected void finalize() throws Throwable {} {
    	if (player != null) {
	    player.nodeCounter[0]--;
	    System.out.println("Decrementing node count for node " + counter);
    	}
    }

    public Move ourMoveOf(JointMove jointMove) {
	return jointMove.get(roleIndex);
    }

    public Node(Randori _player, MachineState _state, int _roleIndex)
	throws MoveDefinitionException, GoalDefinitionException {
	counter = _player.nodeCounter[0]++;
	player = _player;
	state = _state;
	roleIndex = _roleIndex;
	isTerminal = machine().isTerminal(state);
	isFullyDetermined = isTerminal;
	player.edges.put(state, this);
	player.nodes.add(this);
	printed = false;
	if (isTerminal)
	    score = machine().getGoal(state, role());
	else {
  	    score = 50;
	    moves = machine().getLegalMoves(state, role());
	    List<JointMove> legalMoves = new ArrayList<JointMove> ();
	    for (List<Move> jm : machine().getLegalJointMoves(state))
		legalMoves.add(new JointMove(jm));
	    jointMoves = new HashMap<Move,List<JointMove>>();
	    scores = new int[moves.size()];
	    for (Move ourMove : moves) {
		jointMoves.put(ourMove, new ArrayList<JointMove>());
		for (JointMove jointMove : legalMoves) {
		    if (ourMoveOf(jointMove).equals(ourMove)) {
			jointMoves.get(ourMove).add(jointMove);
		    }
		}
	    }
	    selected = moves.get(0);
	}
    }

    public Node(Node _parent, MachineState state)
	throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
	this(_parent.player, state, _parent.roleIndex);
	parent = _parent;
    }

    public Node(Randori player)
	throws MoveDefinitionException, GoalDefinitionException {
	this(player, player.getCurrentState(), getRoleIndex(player.getStateMachine(), player.getRole()));
    }
}
