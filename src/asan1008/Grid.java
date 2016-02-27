package asan1008;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import javafx.util.Pair;
import spacesettlers.objects.AbstractObject;
import spacesettlers.objects.Asteroid;
import spacesettlers.objects.Beacon;
import spacesettlers.objects.Ship;
import spacesettlers.simulator.Toroidal2DPhysics;
import spacesettlers.utilities.Position;

public class Grid {
	public static final int GRID_NODE_SIZE = 40;
	public Map<Pair<Integer, Integer>, GridNode> nodes;
	public Map<GridNode, ArrayList<GridNode>> adjacencyMap;
	public AbstractObject goal;
	private Ship ship;
	private final int MAXIMUM_SEARCH_DEPTH = 100;
	
	public Grid(Toroidal2DPhysics space, Ship ship, AbstractObject goal){
		nodes = new HashMap<Pair<Integer,Integer>, GridNode>();
		divideSpace(space, goal);
		markOccupiedNodes(space, goal);
		initializeAdjacencyMap(space.getWidth(), space.getHeight());
		getNodeByObject(goal).setHValue(0);
		this.ship = ship;
	}
	
	public void divideSpace(Toroidal2DPhysics space, AbstractObject goal){
		for(int i = 0; i < space.getWidth(); i+=GRID_NODE_SIZE){
			for(int j = 0; j < space.getHeight(); j+=GRID_NODE_SIZE){
				GridNode node = new GridNode(i, i+GRID_NODE_SIZE, j, j+GRID_NODE_SIZE, space, goal);
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
	
	public void initializeAdjacencyMap(double width, double height) {
		for(GridNode node: nodes.values()) {
			ArrayList<GridNode> neighbors = new ArrayList<GridNode>();
			
			double topPosition = node.getY1()-GRID_NODE_SIZE;
			double leftPosition = node.getX1()-GRID_NODE_SIZE;
			double rightPosition = node.getX1()+GRID_NODE_SIZE;
			double bottomPosition = node.getY1()+GRID_NODE_SIZE;
			
			boolean top = (topPosition < 0) ? false : true;
			boolean left = (leftPosition < 0) ? false : true;
			boolean right = (rightPosition > width) ? false : true;
			boolean bottom = (bottomPosition > height) ? false : true;

			if(left) {
				neighbors.add(nodes.get(getKeyPair(new Position(leftPosition, node.getY1()))));
			}
			if(top){
				neighbors.add(nodes.get(getKeyPair(new Position(node.getX1(), topPosition))));
				if(left) {
					neighbors.add(nodes.get(getKeyPair(new Position(leftPosition, topPosition))));
				}
				if(right) {
					neighbors.add(nodes.get(getKeyPair(new Position(rightPosition, topPosition))));
				}
			}
			if(right) {
				neighbors.add(nodes.get(getKeyPair(new Position(rightPosition, node.getY1()))));
			}
			if(bottom){
				neighbors.add(nodes.get(getKeyPair(new Position(node.getX1(), bottomPosition))));
				if(left) {
					neighbors.add(nodes.get(getKeyPair(new Position(leftPosition, bottomPosition))));
				}
				if(right) {
					neighbors.add(nodes.get(getKeyPair(new Position(rightPosition, bottomPosition))));
				}
			}
			
			adjacencyMap.put(node, neighbors);
		}
	}

	public Pair<Integer, Integer> getKeyPair(Position position) {
		int xPosition = (int) (position.getX() - (position.getX() % GRID_NODE_SIZE));
		int yPosition = (int) (position.getY() - (position.getY() % GRID_NODE_SIZE));
		return new Pair<Integer, Integer>(xPosition, yPosition);
	}
	
	public GridNode getNodeByObject(AbstractObject object) {
		return nodes.get(getKeyPair(object.getPosition()));
	}
	
	public GridNode removeMinFromFringe(HashMap<GridNode, Double> fringe) {		
		GridNode currentMinNode = null;
		
		for (GridNode node : fringe.keySet()) {
			if (currentMinNode == null) {
				currentMinNode = node;
				continue;
			}
			
			if (fringe.get(node) < fringe.get(currentMinNode)) {
				currentMinNode = node;
			}
		}
		
		if (currentMinNode != null) fringe.remove(currentMinNode);
		return currentMinNode;
	}
	
	/**
	 * A* search
	 * 
	 * @return The optimal path of free nodes for us to traverse to get to the goal
	 */
	public LinkedList<GridNode> getPathToGoal(Toroidal2DPhysics space) {
		// starting node
		GridNode start = getNodeByObject(ship);
		
		// Create map of a node to the most efficient node to reach it from the start
		HashMap<GridNode, GridNode> cameFrom = new HashMap<GridNode, GridNode>();
		
		// Create the fringe, which maps nodes to fValues
		HashMap<GridNode, Double> fringe = new HashMap<GridNode, Double>();
		
		// Add start to fringe
		fringe.put(start, start.getHValue());
		
		// Create a map from nodes to gValues
		HashMap<GridNode, Double> gValueMap = new HashMap<GridNode, Double>();
		
		// add current node to gValue map
		gValueMap.put(start, 0.0);
		
		// Create the closed set and add current node
		HashSet<GridNode> closed = new HashSet<GridNode>();		
		
		while (true) {
			if (fringe.isEmpty()) {
				return null; // There is nowhere for us to go
			}
			
			GridNode current = removeMinFromFringe(fringe);
			
			if (current.equals(getNodeByObject(goal))) {
				return constructPath(cameFrom, current); // If the next node is the goal, end A*
			} else if (!closed.contains(current)) {
				closed.add(current);
				for (GridNode node : adjacencyMap.get(current)) {
					if (closed.contains(node)) {
						continue;
					}
					
					double possibleGValue = gValueMap.get(current) + space.findShortestDistance(current.getPosition(), node.getPosition());
					double possibleFValue = node.getHValue() + possibleGValue;
					// TODO: this may not work. Now the two nodes are different because of different F/G values.
					if (!fringe.containsKey(node)){
						fringe.put(node, node.getHValue() + possibleGValue);
					} else if(fringe.get(node) < possibleFValue) {
						fringe.put(node, possibleFValue);
					}
					cameFrom.put(node, current);
					gValueMap.put(node, possibleGValue);
				}
			}
			
			if (closed.size() >= MAXIMUM_SEARCH_DEPTH) {
				return null; // At this point, we most likely cannot access the goal object
			}
		}
	}
	
	public LinkedList<GridNode> constructPath(HashMap<GridNode, GridNode> cameFrom, GridNode current) {
		LinkedList<GridNode> path = new LinkedList<GridNode>();
		path.add(current);
		while(cameFrom.containsKey(current)){
			current = cameFrom.get(current);
			path.add(current);
		}
		return path;
	}
}


