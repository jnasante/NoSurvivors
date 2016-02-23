package asan1008;

import spacesettlers.objects.AbstractObject;
import spacesettlers.simulator.Toroidal2DPhysics;
import spacesettlers.utilities.Position;

public class GridNode {
	private double x1, x2, y1, y2;
	private double hValue;
	private double gValue;
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

	public double getFValue() {
		return hValue + gValue;
	}

	public void setHValue(double hValue) {
		this.hValue = hValue;
	}

	public void setGValue(double gValue) {
		this.gValue = gValue;
	}
	
	public double getGValue() {
		return gValue;
	}

	public boolean isFree() {
		return free;
	}

	public void setFree(boolean free) {
		this.free = free;
	}
}
