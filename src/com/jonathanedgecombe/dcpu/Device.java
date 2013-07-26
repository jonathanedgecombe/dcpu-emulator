package com.jonathanedgecombe.dcpu;

public abstract class Device implements Runnable {
	private final int hardwareId;
	private final short hardwareVersion;
	private final int manufacturerId;

	public abstract void interrupt();

	public Device(int hardwareId, short hardwareVersion, int manufacturerId) {
		this.hardwareId = hardwareId;
		this.hardwareVersion = hardwareVersion;
		this.manufacturerId = manufacturerId;
	}

	public int getHardwareId() {
		return hardwareId;
	}

	public short getHardwareVersion() {
		return hardwareVersion;
	}

	public int getManufacturerId() {
		return manufacturerId;
	}
}
