package asan1008;

import spacesettlers.objects.AbstractObject;
import spacesettlers.simulator.Toroidal2DPhysics;
import spacesettlers.utilities.Position;
import spacesettlers.utilities.Vector2D;

/**
 * Calls MoveAction for the actual movements but allows you to aim for a spacewar object
 * and to stop when the object dies (e.g. someone (maybe you) reached it)
 * 
 * Based on code for MoveToObjectAction
 * 
 * Provides extra support for A* movement (moving along path rather than straight line) and 
 * more flexible target velocity
 * 
 */
public class FasterMoveToObjectAction extends OrientedMoveAction {
	AbstractObject goalObject;
	Position originalLocation;
	
	/**
	 * Initialize with your location and the goal object 
	 * 
	 * @param space
	 * @param currentLocation
	 * @param goalObject
	 */
	public FasterMoveToObjectAction(Toroidal2DPhysics space, Position currentPosition, AbstractObject goalObject, Position targetPosition, Vector2D targetVelocity, double targetOrientation) {
		super(space, currentPosition, targetPosition, targetVelocity, targetOrientation);
		this.goalObject = goalObject;
		this.originalLocation = goalObject.getPosition().deepCopy();
	}
	
	/**
	 * Return the goal object (and remember it is a clone so use its UUID!)
	 * @return
	 */
	public AbstractObject getGoalObject() {
		return goalObject;
	}

	/**
	 * Returns true if the movement finished or the goal object died or moved
	 * 
	 */
	public boolean isMovementFinished(Toroidal2DPhysics space) {
		if (super.isMovementFinished(space)) {
			return true;
		}
		
		AbstractObject newGoalObj = space.getObjectById(goalObject.getId());
		
		if (newGoalObj == null) {
			return true;
		}
		
		if (!newGoalObj.isAlive()) {
			return true;
		} 
		
		if (!newGoalObj.getPosition().equals(originalLocation)) {
			return true;
		}
		
		return false;
	}
	

}
