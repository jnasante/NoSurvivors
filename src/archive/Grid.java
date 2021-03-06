package archive;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import javafx.util.Pair;
import spacesettlers.graphics.LineGraphics;
import spacesettlers.graphics.SpacewarGraphics;
import spacesettlers.graphics.StarGraphics;
import spacesettlers.objects.AbstractObject;
import spacesettlers.objects.Asteroid;
import spacesettlers.objects.Beacon;
import spacesettlers.objects.Ship;
import spacesettlers.objects.weapons.AbstractWeapon;
import spacesettlers.simulator.Toroidal2DPhysics;
import spacesettlers.utilities.Position;

/** 
 * Grid representation of the map (for A* search)
 *
 */
public class Grid {
	public static final int GRID_NODE_SIZE = 20;
	public HashMap<Pair<Integer, Integer>, GridNode> nodes;
	public HashMap<GridNode, ArrayList<GridNode>> adjacencyMap;
	public AbstractObject goal;
	private Ship ship;
	private final int MAXIMUM_SEARCH_SIZE = 300;
	public ArrayList<SpacewarGraphics> graphicsToAdd;
	private boolean shouldIgnoreMineableAsteroids = true;
	
	public Grid(Toroidal2DPhysics space, Ship ship, AbstractObject goal) {
		this(space, ship, goal, true);
	}
	
	public Grid(Toroidal2DPhysics space, Ship ship, AbstractObject goal, boolean shouldIgnoreMineableAsteroids) {
		this.goal = goal;
		this.ship = ship;
		this.shouldIgnoreMineableAsteroids = shouldIgnoreMineableAsteroids;		
		graphicsToAdd = new ArrayList<SpacewarGraphics>();
		nodes = new HashMap<Pair<Integer,Integer>, GridNode>();
		adjacencyMap = new HashMap<GridNode, ArrayList<GridNode>>();
		divideSpace(space, goal);
		markOccupiedNodes(space, goal);
		initializeAdjacencyMap(space.getWidth(), space.getHeight());
		getNodeByObject(goal).setHValue(0);

	}
	
	/**
	 * Divide the space into grid nodes
	 */
	private void divideSpace(Toroidal2DPhysics space, AbstractObject goal){
		for(int i = 0; i < space.getWidth(); i+=GRID_NODE_SIZE){
			for(int j = 0; j < space.getHeight(); j+=GRID_NODE_SIZE){
				GridNode node = new GridNode(i, i+GRID_NODE_SIZE, j, j+GRID_NODE_SIZE, space, goal);
				nodes.put(new Pair<Integer, Integer>(i, j), node);
			}
		}
	}
	
	/**
	 * Mark nodes containing objects as occupied
	 * 
	 * @param space Space
	 * @param goal our goal object
	 */
	private void markOccupiedNodes(Toroidal2DPhysics space, AbstractObject goal) {
		for( AbstractObject object : space.getAllObjects() ) {
			if( object instanceof Beacon || 
					object instanceof AbstractWeapon ||
					(object instanceof Asteroid && ((Asteroid)object).isMineable() && shouldIgnoreMineableAsteroids) || 
					object.getId() == goal.getId() || 
					object.getId() == ship.getId()) {
				continue;
			}
			
			getNodeByObject(object).setOccupied();
			
			// Calculate positions of corners of "box" surrounding object
			Position topLeftPosition = new Position(getLeftBorder(space, object), getTopBorder(space, object));
			Position topRightPosition = new Position(getRightBorder(space, object), getTopBorder(space, object));
			Position bottomLeftPosition = new Position(getLeftBorder(space, object), getBottomBorder(space, object));
			Position bottomRightPosition = new Position(getRightBorder(space, object), getBottomBorder(space, object));
			
			// Mark all nodes the object touches as occupied
			getNodeByPosition(topLeftPosition).setOccupied();
			getNodeByPosition(topRightPosition).setOccupied();
			getNodeByPosition(bottomLeftPosition).setOccupied();
			getNodeByPosition(bottomRightPosition).setOccupied();
		}
		
	}
	
	/**
	 * Helper method to get top border of square around an object
	 */
	private double getTopBorder(Toroidal2DPhysics space, AbstractObject object) {
		return (object.getPosition().getY() - ship.getRadius()) % space.getHeight();
	}

	/**
	 * Helper method to get bottom border of square around an object
	 */
	private double getBottomBorder(Toroidal2DPhysics space, AbstractObject object) {
		return (object.getPosition().getY() + object.getRadius()) % space.getHeight();
	}

	/**
	 * Helper method to get left border of square around an object
	 */
	private double getLeftBorder(Toroidal2DPhysics space, AbstractObject object) {
		return (object.getPosition().getX() - object.getRadius()) % space.getWidth();
	}
	
	/**
	 * Helper method to get right border of square around an object
	 */
	private double getRightBorder(Toroidal2DPhysics space, AbstractObject object) {
		return (object.getPosition().getX() + object.getRadius()) % space.getHeight();
	}
	
	/**
	 * Determine neighbors of each node
	 * 
	 * @param width
	 * @param height
	 */
	private void initializeAdjacencyMap(double width, double height) {
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
			
			GridNode neighbor;
			
			if(left) {
				neighbor = nodes.get(getKeyPair(new Position(leftPosition, node.getY1())));
				if (neighbor != null && neighbor.isFree()) neighbors.add(neighbor);
			}
			
			if(top) {
				neighbor = nodes.get(getKeyPair(new Position(node.getX1(), topPosition)));
				if (neighbor != null && neighbor.isFree()) neighbors.add(neighbor);
				
				if(left) {
					neighbor = nodes.get(getKeyPair(new Position(leftPosition, topPosition)));
					if (neighbor != null && neighbor.isFree()) neighbors.add(neighbor);
				}
				
				if(right) {
					neighbor = nodes.get(getKeyPair(new Position(rightPosition, topPosition)));
					if (neighbor != null && neighbor.isFree()) neighbors.add(neighbor);
				}
			}
			
			if(right) {
				neighbor = nodes.get(getKeyPair(new Position(rightPosition, node.getY1())));
				if (neighbor != null && neighbor.isFree()) neighbors.add(neighbor);
			}
			
			if(bottom){
				neighbor = nodes.get(getKeyPair(new Position(node.getX1(), bottomPosition)));
				if (neighbor != null && neighbor.isFree()) neighbors.add(neighbor);
				
				if(left) {
					neighbor = nodes.get(getKeyPair(new Position(leftPosition, bottomPosition)));
					if (neighbor != null && neighbor.isFree()) neighbors.add(neighbor);
				}
				
				if(right) {
					neighbor = nodes.get(getKeyPair(new Position(rightPosition, bottomPosition)));
					if (neighbor != null && neighbor.isFree()) neighbors.add(neighbor);
				}
			}
			
			adjacencyMap.put(node, neighbors);

		}
	}

	/**
	 * Get pre-defined key-pair for the grid node based on position
	 * 
	 * @param position
	 * @return
	 */
	private Pair<Integer, Integer> getKeyPair(Position position) {
		int xPosition = (int) (position.getX() - (position.getX() % GRID_NODE_SIZE));
		int yPosition = (int) (position.getY() - (position.getY() % GRID_NODE_SIZE));
		return new Pair<Integer, Integer>(xPosition, yPosition);
	}
	
	/**
	 * Get grid node containing object
	 * 
	 * @param object
	 * @return
	 */
	protected GridNode getNodeByObject(AbstractObject object) {
		return getNodeByPosition(object.getPosition());
	}
	
	/**
	 * Get grid node containing position
	 * 
	 * @param object
	 * @return
	 */
	protected GridNode getNodeByPosition(Position position) {
		return nodes.get(getKeyPair(position));
	}
	
	/**
	 * Custom priority queue created from Hashmap
	 * 
	 * @param fringe
	 * @return
	 */
	private GridNode removeMinFromFringe(HashMap<GridNode, Double> fringe) {		
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
				int count = 0;
				for (GridNode node : adjacencyMap.get(current)) {
					if (node == null || !node.isFree() || closed.contains(node)) {
						continue;
					}
										
					double possibleGValue = gValueMap.get(current) + space.findShortestDistance(current.getPosition(), node.getPosition());
					double possibleFValue = node.getHValue() + possibleGValue;
					
					if (!fringe.containsKey(node)){
						fringe.put(node, node.getHValue() + possibleGValue);
					} else if(fringe.get(node) < possibleFValue) {
						fringe.put(node, possibleFValue);
					}
					cameFrom.put(node, current);
					gValueMap.put(node, possibleGValue);
				}
			}
			
			if (closed.size() >= MAXIMUM_SEARCH_SIZE) {
				return null; // At this point, we most likely cannot access the goal object
			}
		}
	}
	
	/**
	 * Helper method for A*
	 * Construct the final path based on information gathered from A* search
	 * 
	 * @param cameFrom
	 * @param current
	 * @return
	 */
	private LinkedList<GridNode> constructPath(HashMap<GridNode, GridNode> cameFrom, GridNode current) {
		LinkedList<GridNode> path = new LinkedList<GridNode>();
		path.add(current);
		while(cameFrom.containsKey(current)){
			current = cameFrom.get(current);
			path.add(current);
		}
		return path;
	}
	
}


