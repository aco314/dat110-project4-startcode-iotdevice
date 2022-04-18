package no.hvl.dat110.aciotdevice.controller;

import no.hvl.dat110.aciotdevice.client.AccessCode;
import no.hvl.dat110.aciotdevice.client.RestClient;
import no.hvl.dat110.aciotdevice.pins.Wiring;

public class AccessController extends MicroController {

	private RestClient client;

	public AccessController() {
		this.client = new RestClient();
	}

	void setup() {
		Serial.begin(9600);

		pinMode(Wiring.BUTTONA, INPUT);
		pinMode(Wiring.BUTTONB, INPUT);
		pinMode(Wiring.SENSOR, INPUT);
		pinMode(Wiring.GREENLED, OUTPUT);
		pinMode(Wiring.YELLOWLED, OUTPUT);
		pinMode(Wiring.REDLED, OUTPUT);

		changeState(1); // To LOCKED state
	}

	// State of the controller
	final int LOCKED = 1;
	final int WAITING = 2;
	final int UNLOCKED = 3;
	int state = LOCKED;

	// Correct key combination is key1 -> key2
	private int key1 = 1;
	private int key2 = 2;
	int firstPressed = 0;
	int secondPressed = 0;

	// Variables for timeout check
	long changedStateTime = 0; 
	final long interval = 5000;

	// State for the use of networking
	int netmode = 0;

	// Prints the current state
	void printstate() {

		switch (state) {
		case LOCKED:
			Serial.println("LOCKED");
			break;

		case WAITING:
			Serial.println("WAITING");
			break;

		case UNLOCKED:
			Serial.println("UNLOCKED");
			break;

		default:
			Serial.println("ILLEGAL STATE");
			break;
		}
	}

	// Changes the state with the correct LED
	void changeState(int newState) {
	  
	  digitalWrite(Wiring.REDLED, LOW);
	  digitalWrite(Wiring.YELLOWLED, LOW);
	  digitalWrite(Wiring.GREENLED, LOW);
	  state = newState;
	  printstate();
	  
	  switch (newState) {
	    
	    case LOCKED:
	      digitalWrite(Wiring.REDLED, HIGH);
	    break;
	    
	    case WAITING:
	      digitalWrite(Wiring.YELLOWLED, HIGH);
	    break;
	    
	    case UNLOCKED:
	      digitalWrite(Wiring.GREENLED, HIGH);
	    break;
	  }
	  
	  // For fixing a bug where if you press the buttons while its not in WAITING stage,
	  // it will remember the press and mess up when it eventually goes into WAITING
	  digitalRead(Wiring.BUTTONA);
	  digitalRead(Wiring.BUTTONB);
	  digitalRead(Wiring.SENSOR);
	}
	
	// Returns true if state was past interval time 
	// in UNLOCKED or WAITING mode
	private boolean timedOut() {
	  
	  long currentMillis = System.currentTimeMillis();
	      
	  if (currentMillis - changedStateTime >= interval) {
	    return true;
	  }
	  
	  return false;
	}

	void loop() {
		
		// Check if network status should be updated
		if (digitalRead(Wiring.PUSHNET) == HIGH) {

			netmode = 1 - netmode; // Toggle network status

			if (netmode == 1) {
				digitalWrite(Wiring.BLUELED, HIGH);
			} else {
				digitalWrite(Wiring.BLUELED, LOW);
			}
		}

		switch (state) {

		case LOCKED:

			// If motion is detected, change state to WAITING
			if (digitalRead(Wiring.SENSOR) == HIGH) {
				changedStateTime = System.currentTimeMillis();
				changeState(WAITING);
			}

			break;
			
		case WAITING:

			if (timedOut()) {
				firstPressed = 0;
				changeState(LOCKED);
				return;
			}

			// If key B pressed
			if (digitalRead(Wiring.BUTTONB) == HIGH) {

				// If first key hasn't been pressed yet
				if (firstPressed == 0) {
					firstPressed = 2;

				// If first key already pressed
				} else {
					secondPressed = 2;
				}

				// Blink yellow LED
				digitalWrite(Wiring.YELLOWLED, LOW);
				delay(300);
				digitalWrite(Wiring.YELLOWLED, HIGH);
				delay(300);

			// If key A pressed
			} else if (digitalRead(Wiring.BUTTONA) == HIGH) {

				// If first key hasn't been pressed yet
				if (firstPressed == 0) {
					firstPressed = 1;

				// If first key already pressed
				} else {
					secondPressed = 1;
				}

				// Blink yellow LED
				digitalWrite(Wiring.YELLOWLED, LOW);
				delay(300);
				digitalWrite(Wiring.YELLOWLED, HIGH);
				delay(300);
			}

			// If a second key has been pressed
			if (secondPressed != 0) {
				
				digitalWrite(Wiring.YELLOWLED, LOW);
				delay(300);
				
				if (netmode == 1) {
					
					// Get the recent access code before checking
					AccessCode newcode = client.doGetAccessCode();
					
					if (newcode != null) {
						key1 = newcode.getAccesscode()[0];
						key2 = newcode.getAccesscode()[1];
						Serial.println("UPDATING CODE");
					}
				}

				// If correct combination
				if ((firstPressed == key1) && (secondPressed == key2)) {
					changedStateTime = System.currentTimeMillis();
					changeState(UNLOCKED);
					
					if (netmode == 1) {
						client.doPostAccessEntry("UNLOCKED");
					}

				// If wrong combination
				} else {
					digitalWrite(Wiring.REDLED, HIGH);
					delay(300);
					digitalWrite(Wiring.REDLED, LOW);
					delay(300);
					changeState(LOCKED);
					
					if (netmode == 1) {
						client.doPostAccessEntry("ACCESS DENIED");
					}
				}

				// Reset counters for next time
				firstPressed = 0;
				secondPressed = 0;
			}

			break;

		case UNLOCKED:

			// Change state to LOCKED after timeout interval
			if (timedOut()) {
				if (netmode == 1) {
					client.doPostAccessEntry("LOCKED");
				}
				changeState(LOCKED);
			}

			break;

		default:
			break;
		}
	}
}

/*package no.hvl.dat110.aciotdevice.controller;

import no.hvl.dat110.aciotdevice.client.AccessCode;
import no.hvl.dat110.aciotdevice.client.RestClient;
import no.hvl.dat110.aciotdevice.pins.Wiring;

public class AccessController extends MicroController {

	private RestClient client;

	public AccessController() {
		this.client = new RestClient();
	}

	void setup() {
		Serial.begin(9600);

		pinMode(Wiring.PIR, INPUT);
		pinMode(Wiring.PUSHBTN1, INPUT);
		pinMode(Wiring.PUSHBTN2, INPUT);

		pinMode(Wiring.GREENLED, OUTPUT);
		pinMode(Wiring.YELLOWLED, OUTPUT);
		pinMode(Wiring.REDLED, OUTPUT);

		for (int i = 0; i < 3; i++) {

			setleds(HIGH, HIGH, HIGH);
			delay(500);
			setleds(LOW, LOW, LOW);
			delay(500);

		}

		setleds(HIGH, LOW, LOW);
		printstate();
	}

	void setleds(int vred, int vyellow, int vgreen) {

		digitalWrite(Wiring.GREENLED, vgreen);
		digitalWrite(Wiring.YELLOWLED, vyellow);
		digitalWrite(Wiring.REDLED, vred);

	}

	void blink(int pin) {

		for (int i = 0; i < 5; i++) {

			digitalWrite(pin, LOW);
			delay(250);
			digitalWrite(pin, HIGH);
			delay(250);

		}
	}

	// state of the controller
	final int LOCKED = 0; // C -> Java: const -> final
	final int WAIT1P = 1;
	final int WAIT2P = 2;
	final int CHECKING = 3;
	final int UNLOCKED = 4;

	int state = LOCKED;

	// keep track of the order in which buttons are pressed
	int firstpressed = 0;
	int secondpressed = 0;

	// current access code - default is 1 -> 2
	private int[] code = { 1, 2 };

	// state for the use of networking
	int netmode = 0;

	void printstate() {

		switch (state) {
		case LOCKED:
			Serial.println("LOCKED");
			break;

		case WAIT1P:
			Serial.println("WAIT1P");
			break;

		case WAIT2P:
			Serial.println("WAIT2P");
			break;

		case CHECKING:
			Serial.println("CHECKING");
			break;

		case UNLOCKED:
			Serial.println("UNLOCKED");
			break;

		default:
			Serial.println("ILLEGAL STATE");
			break;
		}
	}

	void setstate(int newstate) {

		state = newstate;
		printstate();
	}

	void loop() {

		int pirsensor = digitalRead(Wiring.PIR);
		int btn2 = digitalRead(Wiring.PUSHBTN2);
		int btn1 = digitalRead(Wiring.PUSHBTN1);
		int nbtn = digitalRead(Wiring.PUSHNET);

		// check if network status should be updated
		if (nbtn == HIGH) {

			netmode = 1 - netmode; // toggle network status

			if (netmode == 1) {
				digitalWrite(Wiring.BLUELED, HIGH);
			} else {
				digitalWrite(Wiring.BLUELED, LOW);
			}
		}

		switch (state) {

		case LOCKED:

			if (pirsensor == HIGH) {
				setstate(WAIT1P);
				setleds(LOW, HIGH, LOW);

			}
			break;

		case WAIT1P:
			if ((btn1 == HIGH) || (btn2 == HIGH)) {
				blink(Wiring.YELLOWLED);

				if (btn1 == HIGH) {
					firstpressed = 1;
				}

				if (btn2 == HIGH) {
					firstpressed = 2;
				}

				setstate(WAIT2P);

			}

			break;

		case WAIT2P:
			if ((btn1 == HIGH) || (btn2 == HIGH)) {
				blink(Wiring.YELLOWLED);

				if (btn1 == HIGH) {
					secondpressed = 1;
				}

				if (btn2 == HIGH) {
					secondpressed = 2;
				}

				setstate(CHECKING);

			}
			break;

		case CHECKING:

			if (netmode == 1) {

				// get the recent access code before checking
				AccessCode newcode = client.doGetAccessCode();

				if (newcode != null) {
					code = newcode.getAccesscode();
					Serial.println("UPDATING CODE");
				}
			}

			if ((firstpressed == code[0]) && (secondpressed == code[1])) {
				setstate(UNLOCKED);
			} else {
				blink(Wiring.REDLED);
				setleds(HIGH, LOW, LOW);
				setstate(LOCKED);

				if (netmode == 1) {
					client.doPostAccessEntry("ACCESS DENIED");
				}

			}

			firstpressed = 0;
			secondpressed = 0;

			break;

		case UNLOCKED:
			blink(Wiring.GREENLED);
			setleds(LOW, LOW, HIGH);

			if (netmode == 1) {
				client.doPostAccessEntry("UNLOCKED");
			}

			delay(5000);

			if (netmode == 1) {
				client.doPostAccessEntry("LOCKED");
			}

			blink(Wiring.REDLED);
			setleds(HIGH, LOW, LOW);
			setstate(LOCKED);
			break;

		default:
			break;
		}
	}
}*/
