package com.jonathanedgecombe.dcpu;

import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

public class DCPU implements Runnable {
	private final Queue<Short> interruptQueue = new ArrayBlockingQueue<>(256);
	private Device[] devices;
	private short[] ram;
	private short A, B, C, X, Y, Z, I, J, PC, SP, EX, IA;
	private boolean interruptsEnabled;
	private int connectedDevices;

	public DCPU() {
		reset();
	}

	private void reset() {
		ram = new short[0x10000];
		A = 0;
		B = 0;
		C = 0;
		X = 0;
		Y = 0;
		Z = 0;
		I = 0;
		J = 0;
		PC = 0;
		SP = (short) 0xffff;
		EX = 0;
		IA = 0;
		interruptsEnabled = true;
		interruptQueue.clear();
		devices = new Device[0x10000];
		connectedDevices = 0;
	}

	public void addDevice(Device d) {
		for (int i = 0; i < 0x10000; i++) {
			if (devices[i] == null) {
				devices[i] = d;
				connectedDevices++;
			}
		}
		throw new RuntimeException("Too many devices connected");
	}

	public void removeDevice(Device d) {
		for (int i = 0; i < 0x10000; i++) {
			if (devices[i] == d) {
				devices[i] = null;
				connectedDevices--;
			}
		}
	}

	private short fetch() {
		return ram[PC++];
	}

	private void fetchDecodeExecute() {
		short instruction = fetch();

		int opcode = instruction & 0x1f;

		if (opcode != 0) {
			// Basic instruction
			int a = (instruction >> 10) & 0x3f;
			int b = (instruction >> 5)  & 0x1f;

			switch (opcode) {
			case 0x01:
				set(b, get(a));
				return;
			case 0x02:
				int add = get(b)&0xffff + get(a)&0xffff;
				if (add >= 0xffff) {
					EX = 1;
				} else {
					EX = 0;
				}
				set(b, add);
				return;
			case 0x03:
				int sub = get(b)&0xffff - get(a)&0xffff;
				if (sub < 0) {
					EX = -1;
				} else {
					EX = 0;
				}
				set(b, sub);
				return;
			case 0x04:
				int mul = get(b)&0xffff * get(a)&0xffff;
				EX = (short) (mul>>>16);
				set(b, mul);
				return;
			case 0x05:
				int mli = get(b) * get(a);
				EX = (short) (mli>>>16);
				set(b, mli);
				return;
			case 0x06:
				int getB = get(b)&0xffff;
				int getA = get(a)&0xffff;
				if (getA == 0) {
					set(b, 0);
					EX = 0;
				} else {
					int div = getB/getA;
					EX = (short) ((getB<<16)/getA);
					set(b, div);
				}
				return;
			case 0x07:
				int getBS = get(b);
				int getAS = get(a);
				if (getAS == 0) {
					set(b, 0);
					EX = 0;
				} else {
					int dvi = getBS/getAS;
					EX = (short) ((getBS<<16)/getAS);
					set(b, dvi);
				}
				return;
			case 0x08:
				int getBM = get(b)&0xffff;
				int getAM = get(a)&0xffff;
				if (getAM == 0) {
					set(b, 0);
				} else {
					set(b, getBM%getAM);
				}
				return;
			case 0x09:
				int getBMS = get(b);
				int getAMS = get(a);
				if (getAMS == 0) {
					set(b, 0);
				} else {
					set(b, getBMS%getAMS);
				}
				return;
			case 0x0a:
				set(b, get(b)&0xffff & get(a)&0xffff);
				return;
			case 0x0b:
				set(b, get(b)&0xffff | get(a)&0xffff);
				return;
			case 0x0c:
				set(b, get(b)&0xffff ^ get(a)&0xffff);
				return;
			case 0x0d:
				int getBSHR = get(b)&0xffff;
				int getASHR = get(a)&0xffff;
				EX = (short) ((getBSHR<<16)>>getASHR);
				set(b, getBSHR>>>getASHR);
				return;
			case 0x0e:
				int getBASR = get(b);
				int getAASR = get(a)&0xffff;
				EX = (short) ((getBASR<<16)>>>getAASR);
				set(b, getBASR>>getAASR);
				return;
			case 0x0f:
				int getBSHL = get(b)&0xffff;
				int getASHL = get(a)&0xffff;
				EX = (short) ((getBSHL<<getASHL)>>16);
				set(b, getBSHL<<getASHL);
				return;
			case 0x10:
				if ((get(b)&get(a)) == 0) PC++;
				return;
			case 0x11:
				if ((get(b)&get(a)) == 0) PC++;
				return;
			case 0x12:
				if (get(b) != get(a)) PC++;
				return;
			case 0x13:
				if (get(b) == get(a)) PC++;
				return;
			case 0x14:
				if ((get(b)&0xffff) <= (get(a)&0xffff)) PC++;
				return;
			case 0x15:
				if (get(b) <= get(a)) PC++;
				return;
			case 0x16:
				if ((get(b)&0xffff) >= (get(a)&0xffff)) PC++;
				return;
			case 0x17:
				if (get(b) >= get(a)) PC++;
				return;

			case 0x1a:
				int addX = get(b)&0xffff + get(a)&0xffff + EX;
				if (addX >= 0xffff) {
					EX = 1;
				} else {
					EX = 0;
				}
				set(b, addX);
				return;
			case 0x1b:
				int subX = get(b)&0xffff - get(a)&0xffff + EX;
				if (subX < 0) {
					EX = -1;
				} else {
					EX = 0;
				}
				set(b, subX);
				return;

			case 0x1e:
				set(b, get(a));
				I++;
				J++;
				return;
			case 0x1f:
				set(b, get(a));
				I--;
				J--;
				return;
			}
		} else {
			// Special instruction
			int a = (instruction >> 10) & 0x3f;
			opcode = (instruction >> 5)  & 0x1f;

			switch(opcode) {
			case 0x01:
				push(PC);
				PC = get(a);
				return;

			case 0x08:
				addInterupt(get(a));
				return;
			case 0x09:
				set(a, IA);
				return;
			case 0x0a:
				IA = get(a);
				return;
			case 0x0b:
				A = pop();
				PC = pop();
				return;
			case 0x0c:
				interruptsEnabled = get(a) == 0;
				return;

			case 0x10:
				set(a, connectedDevices);
				return;
			case 0x11:
				short deviceId = get(a);
				Device d = devices[deviceId];
				if (d == null) {
					A = 0;
					B = 0;
					C = 0;
					X = 0;
					Y = 0;
				} else {
					A = (short) d.getHardwareId();
					B = (short) (d.getHardwareId()>>16);
	
					C = d.getHardwareVersion();
	
					X = (short) d.getManufacturerId();
					Y = (short) (d.getManufacturerId()>>16);
				}
				return;
			case 0x12:
				short deviceIdI = get(a);
				Device dI = devices[deviceIdI];
				if (dI != null) {
					dI.interrupt();
				}
				return;
			}
		}
	}

	public void addInterupt(short message) {
		if (!interruptQueue.offer(message)) {
			throw new RuntimeException("Interrupt queue full");
		}
	}

	private void push(short i) {
		ram[--SP & 0xffff] = i;
	}

	private short pop() {
		return ram[SP++ & 0xffff];
	}

	private short get(int a) {
		if (a >= 0x00 && a < 0x08) {
			switch(a) {
			case 0x00:
				return A;
			case 0x01:
				return B;
			case 0x02:
				return C;
			case 0x03:
				return X;
			case 0x04:
				return Y;
			case 0x05:
				return Z;
			case 0x06:
				return I;
			case 0x07:
				return J;
			}
		}
		
		else if (a >= 0x08 && a < 0x10) {
			switch(a - 0x08) {
			case 0x00:
				return ram[A & 0xffff];
			case 0x01:
				return ram[B & 0xffff];
			case 0x02:
				return ram[C & 0xffff];
			case 0x03:
				return ram[X & 0xffff];
			case 0x04:
				return ram[Y & 0xffff];
			case 0x05:
				return ram[Z & 0xffff];
			case 0x06:
				return ram[I & 0xffff];
			case 0x07:
				return ram[J & 0xffff];
			}
		}

		else if (a >= 0x10 && a < 0x18) {
			short offset = fetch();

			switch(a - 0x10) {
			case 0x00:
				return ram[A+offset & 0xffff];
			case 0x01:
				return ram[B+offset & 0xffff];
			case 0x02:
				return ram[C+offset & 0xffff];
			case 0x03:
				return ram[X+offset & 0xffff];
			case 0x04:
				return ram[Y+offset & 0xffff];
			case 0x05:
				return ram[Z+offset & 0xffff];
			case 0x06:
				return ram[I+offset & 0xffff];
			case 0x07:
				return ram[J+offset & 0xffff];
			}
		}

		else if (a >= 0x18 && a < 0x20) {
			switch(a) {
			case 0x18:
				//return ram[--SP & 0xffff];
				return pop();
			case 0x19:
				return ram[SP & 0xffff];
			case 0x1a:
				short offset = fetch();
				return ram[SP+offset & 0xffff];
			case 0x1b:
				return SP;
			case 0x1c:
				return PC;
			case 0x1d:
				return EX;
			case 0x1e:
				offset = fetch();
				return ram[offset & 0xffff];
			case 0x20:
				return fetch();
			}
		}

		else if (a >= 0x20 && a <= 0x40) return (short) a;

		throw new RuntimeException("Cannot get " + Integer.toHexString(a));
	}

	private void set(int b, int value) {
		if (b >= 0x00 && b < 0x08) {
			switch(b) {
			case 0x00:
				A = (short) value;
				return;
			case 0x01:
				B = (short) value;
				return;
			case 0x02:
				C = (short) value;
				return;
			case 0x03:
				X = (short) value;
				return;
			case 0x04:
				Y = (short) value;
				return;
			case 0x05:
				Z = (short) value;
				return;
			case 0x06:
				I = (short) value;
				return;
			case 0x07:
				J = (short) value;
				return;
			}
		}
		
		else if (b >= 0x08 && b < 0x10) {
			switch(b - 0x08) {
			case 0x00:
				ram[A & 0xffff] = (short) value;
				return;
			case 0x01:
				ram[B & 0xffff] = (short) value;
				return;
			case 0x02:
				ram[C & 0xffff] = (short) value;
				return;
			case 0x03:
				ram[X & 0xffff] = (short) value;
				return;
			case 0x04:
				ram[Y & 0xffff] = (short) value;
				return;
			case 0x05:
				ram[Z & 0xffff] = (short) value;
				return;
			case 0x06:
				ram[I & 0xffff] = (short) value;
				return;
			case 0x07:
				ram[J & 0xffff] = (short) value;
				return;
			}
		}

		else if (b >= 0x10 && b < 0x18) {
			short offset = fetch();

			switch(b - 0x10) {
			case 0x00:
				ram[A+offset & 0xffff] = (short) value;
				return;
			case 0x01:
				ram[B+offset & 0xffff] = (short) value;
				return;
			case 0x02:
				ram[C+offset & 0xffff] = (short) value;
				return;
			case 0x03:
				ram[X+offset & 0xffff] = (short) value;
				return;
			case 0x04:
				ram[Y+offset & 0xffff] = (short) value;
				return;
			case 0x05:
				ram[Z+offset & 0xffff] = (short) value;
				return;
			case 0x06:
				ram[I+offset & 0xffff] = (short) value;
				return;
			case 0x07:
				ram[J+offset & 0xffff] = (short) value;
				return;
			}
		}

		else {
			switch(b) {
			case 0x18:
				//ram[--SP & 0xffff] = (short) value;
				push((short) value);
				return;
			case 0x19:
				ram[SP & 0xffff] = (short) value;
				return;
			case 0x1a:
				short offset = fetch();
				ram[SP+offset & 0xffff] = (short) value;
				return;
			case 0x1b:
				SP = (short) value;
				return;
			case 0x1c:
				PC = (short) value;
				return;
			case 0x1d:
				EX = (short) value;
				return;
			case 0x1e:
				offset = fetch();
				ram[offset & 0xffff] = (short) value;
				return;
			}
		}

		throw new RuntimeException("Cannot set " + Integer.toHexString(b));
	}

	private void checkInterrupts() {
		Short message = interruptQueue.poll();
		if (message != null && IA != 0 && interruptsEnabled) {
			push(PC);
			push(A);
			PC = IA;
			A = message;
		}
	}

	@Override
	public void run() {
		while (true) {
			long ns = System.nanoTime();
			checkInterrupts();
			fetchDecodeExecute();
			while (System.nanoTime()-ns < 10000) {
				try {
					Thread.sleep(0, 1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
					throw new RuntimeException();
				}
			}
		}
	}
}
