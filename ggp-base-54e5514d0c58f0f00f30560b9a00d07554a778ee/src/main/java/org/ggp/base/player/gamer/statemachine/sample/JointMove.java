package org.ggp.base.player.gamer.statemachine.sample;

import java.util.ArrayList;
import java.util.List;

import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;

public class JointMove extends ArrayList<Move> {

    public Node branch;

    public Node getBranch(Node parent, final int[] nodeCounter, MachineState nextState)
    		throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
	if (branch == null) {
	    Node nextNode = parent.player.edges.get(nextState);
	    if (nextNode != null) {
		branch = nextNode;
	    }
	    else {
		branch = new Node(parent, nextState);
		parent.player.edges.put(nextState, branch);
	    }
	}
	return branch;
    }

    public Node getBranch(Node parent, final int[] nodeCounter)
    		throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
    	if (parent.isTerminal)
    		return null;
    	else
    		return getBranch(parent, nodeCounter, parent.getNextState(this));
    }

    public JointMove(List<Move> jm) {
	super(jm);
	branch = null;
    }
}
