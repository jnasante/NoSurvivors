package archive;

import spacesettlers.objects.AbstractObject;
import spacesettlers.simulator.Toroidal2DPhysics;
import spacesettlers.utilities.Position;

/**
 * Class for our nodes in the grid
 *
 */
public class GridNode {
	private double x1, x2, y1, y2;
	private double hValue;
	private boolean free;
	
	public GridNode(double x1, double x2, double y1, double y2, Toroidal2DPhysics space, AbstractObject goal) {
		this.x1 = x1;
		this.x2 = x2;
		this.y1 = y1;
		this.y2 = y2;
		this.free = true;
		this.hValue = space.findShortestDistance(getPosition(), goal.getPosition());
	}
	
	public Position getPosition(){
		return new Position((x1+x2)/2, (y1+y2)/2);
	}

	public double getX1() {
		return x1;
	}

	public double getX2() {
		return x2;
	}

	public double getY1() {
		return y1;
	}
	public double getY2() {
		return y2;
	}

	public double getHValue() {
		return hValue;
	}

	public void setHValue(double hValue) {
		this.hValue = hValue;
	}

	public boolean isFree() {
		return free;
	}

	public void setOccupied() {
		this.free = false;
	}
	
	/* Helper methods */
	public double getTopPosition() {
		return y1-Math.abs(y1-y2);
	}
	
	public double getBottomPosition() {
		return y1+Math.abs(y1-y2);
	}
	
	public double getLeftPosition(GridNode node) {
		return x1-Math.abs(x1-x2);
	}

	public double getRightPosition(GridNode node) {
		return x1+Math.abs(x1-x2);
	}
}
