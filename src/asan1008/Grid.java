package asan1008;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import javafx.util.Pair;
import spacesettlers.objects.AbstractObject;
import spacesettlers.objects.Asteroid;
import spacesettlers.objects.Beacon;
import spacesettlers.objects.Ship;
import spacesettlers.simulator.Toroidal2DPhysics;
import spacesettlers.utilities.Position;

public class Grid {
	public static final int GRID_SIZE = 40;
	public Map<Pair<Integer, Integer>, GridNode> nodes;
	public Map<GridNode, ArrayList<GridNode>> adjacencyMap;
	public AbstractObject goal;
	
	public Grid(Toroidal2DPhysics space, Ship ship, AbstractObject goal){
		nodes = new HashMap<Pair<Integer,Integer>, GridNode>();
		divideSpace(space, goal);
		markOccupiedNodes(space, goal);
		getNodeByObject(goal).sethValue(0);
	}
	
	public void divideSpace(Toroidal2DPhysics space, AbstractObject goal){
		for(int i = 0; i < space.getWidth(); i+=GRID_SIZE){
			for(int j = 0; j < space.getHeight(); j+=GRID_SIZE){
				GridNode node = new GridNode(i, i+GRID_SIZE, j, j+GRID_SIZE, space, goal);
				nodes.put(new Pair<Integer, Integer>(i, j), node);
			}
		}
	}
	
	public void markOccupiedNodes(Toroidal2DPhysics space, AbstractObject goal) {
		for( AbstractObject object : space.getAllObjects() ) {
			if( object instanceof Beacon || 
					(object instanceof Asteroid && ((Asteroid)object).isMineable()) || 
					object.getId() == goal.getId()){
				continue;
			}
			
			getNodeByObject(object).setFree(false);
		}
	}

	public Pair<Integer, Integer> getKeyPair(Position position) {
		int xPosition = (int) (position.getX() - (position.getX() % GRID_SIZE));
		int yPosition = (int) (position.getY() - (position.getY() % GRID_SIZE));
		return new Pair<Integer, Integer>(xPosition, yPosition);
	}
	
	public GridNode getNodeByObject(AbstractObject object) {
		return nodes.get(getKeyPair(object.getPosition()));
	}
}
