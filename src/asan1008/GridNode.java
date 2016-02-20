package asan1008;

public class GridNode {
	private double x1, x2, y1, y2;
	private double hValue;
	private boolean free;
	
	public GridNode(double x1, double x2, double y1, double y2, double hValue) {
		this.x1 = x1;
		this.x2 = x2;
		this.y1 = y1;
		this.y2 = y2;
		this.hValue = hValue;
		this.free = true;
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

	public double gethValue() {
		return hValue;
	}

	public void sethValue(double hValue) {
		this.hValue = hValue;
	}

	public boolean isFree() {
		return free;
	}

	public void setFree(boolean free) {
		this.free = free;
	}
}
