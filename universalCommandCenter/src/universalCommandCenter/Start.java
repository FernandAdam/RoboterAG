package universalCommandCenter;
//BRICK
import lejos.hardware.Button;
import lejos.hardware.ev3.EV3;
import lejos.hardware.port.Port;
import lejos.hardware.BrickFinder;
import lejos.hardware.Keys;
import lejos.hardware.Sound;
//MOTOR
import lejos.hardware.port.MotorPort;
import lejos.hardware.motor.Motor;
import lejos.hardware.motor.NXTRegulatedMotor;
import lejos.hardware.motor.EV3LargeRegulatedMotor;
//SENSORS
import lejos.hardware.sensor.SensorMode;
import lejos.hardware.port.SensorPort;
import lejos.hardware.sensor.EV3TouchSensor;
import lejos.hardware.sensor.EV3ColorSensor;
import lejos.hardware.sensor.EV3UltrasonicSensor;
import lejos.hardware.sensor.HiTechnicColorSensor;
//TIME
import lejos.utility.Delay;
import lejos.utility.Stopwatch;

public class Start {
		public static void main(String[] args) {
			com bot = new com(5.6, 15, Motor.D, Motor.A, SensorPort.S2,SensorPort.S4);
	
			bot.driveDistance(100);
			Delay.msdelay(200);
			bot.driveDistance(10);
			Sound.twoBeeps();
			System.exit(1);
		}
		public void hello() {
			System.out.println("Hello");
		}

	}


class com {

			//COLOR SENSORS
			public static EV3ColorSensor colorSensorLeft;
			public static EV3ColorSensor colorSensorRight;

			//CONSTANT VALUES
			static double wheelDiameter;
			static double wheelCircumference;
			static double wheelDistance;

			//SPEED SETTINGS
			static int cmPerSecond;
			static int turn90degreeInSeconds;

			//LIGHT SENSOR
			static int checkDriveColorSensor = 2;// 2 is the right sensor and 0 is the left
			public static double actualLight;
			public static double ambientWhite = 0.8;
			public static double ambientBlack = 0.05;

			public static NXTRegulatedMotor motorRight;
			public static NXTRegulatedMotor motorLeft;

			public com(double RadDurchmesser, double RadAbstand, NXTRegulatedMotor motorLeftPort, NXTRegulatedMotor motorRightPort, Port colorSensorLeftPort, Port colorSensorRightPort) {
				// RadDurchmesser 5.6, Radabstand 14.5, motorRightPort =^ Port des rechten Farbsensors

				colorSensorLeft = new EV3ColorSensor(colorSensorLeftPort);
				colorSensorRight = new EV3ColorSensor(colorSensorRightPort);
				motorLeft = motorLeftPort;
				motorRight = motorRightPort;

				wheelDiameter = RadDurchmesser;
				wheelDistance = RadAbstand;
				wheelCircumference = wheelDiameter * Math.PI;
				cmPerSecond = (int) (360 / wheelCircumference);
				turn90degreeInSeconds = (int) ((wheelDistance / wheelDiameter)* 180);

				followLineThread followLineThread = new followLineThread();
				followLineThread.start();
			}

			public void testMethods() { // Mit dieser Methode kann man die Funktionalität von den Methoden testen
				while(true) {
					if(Button.DOWN.isDown()) { // fährt 10cm
						Delay.msDelay(500);
						driveDistance(10);
					}else if(Button.ENTER.isDown()) { // dreht sich ein Mal im Kreis
						Delay.msDelay(500);
						rotate(360,100);
					}else if(Button.UP.isDown()) { // dreht sich mit der halben Geschwindigkeit im Kreis
						Delay.msDelay(500);
						rotate(360, 1, 0.5);
					}else if(Button.LEFT.isDown()) { // dreht sich mit dem linken Motor um 180°
						Delay.msDelay(500);
						rotate(180, 0, 1);
					}else if(Button.RIGHT.isDown()) { // dreht sich mit dem rechten Motor um 180°
						Delay.msDelay(500);
						rotate(180, 2, 1);
					}
				}
			}
			public void waitForStartSignal() { // Warte bis Schiedsrichter Start freigibt
				Sound.twoBeeps();
				while(!Button.ENTER.isDown()) {
					if(Button.ESCAPE.isDown()) {
						break;
					}
				}
			}
			public void followLineThreadTest(int seconds) {// folgt der Linie für die angegebenen Sekunden
				followLineThread.forunning=true;
				Delay.msDelay(1000*seconds);
				followLineThread.forunning=false;
			}
			public void initAmbientLight() {// setzt die Lichtumgebungswerte

				// Notfallswerte zur Orientierung
				ambientWhite = 0.4;
				ambientBlack = 0.3;
				actualLight = 1;

				//ZUR WEIßEN STELLE FAHREN
				motorRight.setSpeed(200);
				motorLeft.setSpeed(200);
				motorLeft.forward();
				motorRight.forward();


				for (int i = 100; i != 0; i--) { // Scanne 100 mal
					actualLight = com.colorRed("R");
					if (actualLight < ambientBlack) { // Wenn aktuelles Licht < gesetztes Schwarz, update gesetztes Schwarz
						ambientBlack = actualLight;
					}if (actualLight > ambientWhite) { // Wenn aktuelles Licht > gesetztes Weiß, update gesetztes Weiß
						ambientWhite = actualLight;
					}
					Delay.msDelay(10);
				}
				//AUSGABE ZU TESTZWECKEN
				System.out.println("ambientBlack: " + ambientBlack);
				System.out.println("ambientWhite: " + ambientWhite);
				System.out.println("bestLight: " + (ambientBlack*2+ambientWhite)/3);
				MotorStop();
			}
			public void driveDistance(double cm) {// fährt um die angegebenen cm vorwärts
				motorRight.setSpeed(cmPerSecond);
				motorLeft.setSpeed(cmPerSecond); // Geschwindigkeit zur oben gesetzten cm/s setzen
				//VORWÄRTS ODER RÜCKWÄRTS
				if (cm > 0) { //Vorwärts
					motorLeft.forward();
					motorRight.forward();
				} else { //Rückwärts
					motorLeft.backward();
					motorRight.backward();
				}
				Delay.msDelay((long) (Math.abs(cm) * 1000)); // Delaye solange bis er die DIstanz gefahren ist
				MotorStop(); // Anhalten nach erreichter Distanz
			}
			public void driveFork(int lines, int direction, int adjustTurnSpeedInPercent) { //adjustTurnSpeedInPercent gibt die Drehgeschwindigkeit in Prozent zu 90°/Seconds
				/*
				RICHTUNGEN:
				links: 0
				vorwärts: 1
				rechts: 2
				rückwärts: 3
				oder Wert in °
				*/
				Stopwatch measureTime = new Stopwatch();
				followLineThread.forunning = true; // Folge der Linie

				while (lines != 0) {
					if (measureTime.elapsed() > 400 && linke_Linie() == true) {
							lines--;
						}
				}
				//ABZWEIG GEFUNDEN -> DREHEN
				followLineThread.forunning=false;
				Delay.msDelay(100);
				switch(direction)
				{
					case 0: //LINKS
						rotate(-90,adjustTurnSpeedInPercent);
					case 1: //VORWÄRTS
		 			 	break;
					case 2: //RECHTS
						rotate(90,adjustTurnSpeedInPercent);
					case 3: //RÜCKWÄRTS
						rotate(180,adjustTurnSpeedInPercent);
					default: //WERT in °
						rotate(direction,adjustTurnSpeedInPercent);
				}
			}
			public void rotate(int degree, int adjustTurnSpeedInPercent) { //Der Roboter dreht sich um den angegebenen Grad, bei 100% Prozent 90 Grad pro Sekunde
				//GESCHWINDIGKEIT IN PROZENT VON 'turn90degreeInSeconds'
				motorRight.setSpeed(turn90degreeInSeconds/(100/adjustTurnSpeedInPercent));
				motorLeft.setSpeed(turn90degreeInSeconds/(100/adjustTurnSpeedInPercent));
				if (degree < 0) {
					motorRight.forward();
					motorLeft.backward();
				} else {
					motorRight.backward();
					motorLeft.forward();
				}
				long delay = (long) ((100/adjustTurnSpeedInPercent)*Math.abs(degree) * 5.55556);// degree / 180 * 1000
				Delay.msDelay(delay);
				MotorStop();
			}
			public void rotateToLine(int degree) { //NOT WORKING!!!
				motorRight.setSpeed(turn90degreeInSeconds/2);// drives with 180 degrees per second
				motorLeft.setSpeed(turn90degreeInSeconds/2);
				if (degree < 0) {
					motorRight.forward();
					motorLeft.backward();
				} else {
					motorRight.backward();
					motorLeft.forward();
				}
				long delay = (long) (Math.abs(degree) * 5.55556*2);// degree / 180 * 1000
				Delay.msDelay(delay-200);
				Sound.beep();
				while(colorRed(checkDriveColorSensor)>followLineThread.bestLight) {
					Delay.msDelay(10);
				}
				MotorStop();
			}
			public void rotate(int degree,int motor, double speed) {
				if(motor==0) 		{motorLeft.setSpeed((float)(3*turn90degreeInSeconds*speed));
				}else if (motor==1) {motorRight.setSpeed((float)(turn90degreeInSeconds*speed));
									 motorLeft.setSpeed((float)(turn90degreeInSeconds*speed));
				}else if (motor==2) {motorRight.setSpeed((float)(3*turn90degreeInSeconds*speed));}
				if (degree < 0) {
					if(motor==0) {	motorLeft.backward();
					}else if (motor==1) {motorLeft.forward();motorRight.backward();
					}else if (motor==2) {motorRight.backward();}
				} else {
					if(motor==0) 		{motorLeft.forward();
					}else if (motor==1) {motorLeft.backward();motorRight.forward();
					}else if (motor==2) {motorRight.forward();}
				}
				long delay = (long) ((Math.abs(degree) * 5.55556)/speed);// degree / 180 * 1000
				Delay.msDelay(delay);
				if(motor==0) 		{motorLeft.stop();
				}else if (motor==1) {MotorStop();
				}else if (motor==2) {motorRight.stop();	}
				}
			public static void MotorStop() {
				motorLeft.startSynchronization();
				motorRight.stop(true);
				motorLeft.stop(true);
				motorLeft.endSynchronization();
			}
			public void start() {
				motorLeft.startSynchronization();
				motorRight.forward();
				motorLeft.forward();
				motorLeft.endSynchronization();
			}
			public boolean linke_Linie() {
				float color = 0;
				colorSensorLeft.setCurrentMode("Red");
				colorSensorRight.setCurrentMode("Red");
				if (checkDriveColorSensor == 2) {
					float[] cL = new float[colorSensorLeft.sampleSize()];
					colorSensorLeft.fetchSample(cL, 0);
					color = (float) cL[0];
				} else if (checkDriveColorSensor == 0) {
					float[] cR = new float[colorSensorRight.sampleSize()];
					colorSensorRight.fetchSample(cR, 0);
					color = (float) cR[0];
				}
				if (color < ambientBlack + 0.1) {
					Sound.beep();
					return (true);
				} else {
					return (false);
				}

			}
			public static float colorRed(String x) {
				colorSensorLeft.setCurrentMode("Red");
				colorSensorRight.setCurrentMode("Red");

				float[] cR = new float[colorSensorRight.sampleSize()];
				float[] cL = new float[colorSensorLeft.sampleSize()];
				if (x == "L") {
					colorSensorLeft.fetchSample(cL, 0);
					Float color = (float) cL[0];
					return (color);
				} else {
					colorSensorRight.fetchSample(cR, 0);
					Float color = (float) cR[0];
					return (color);
				}
			}
			public static float colorRed(int x) {
				colorSensorLeft.setCurrentMode("Red");
				colorSensorRight.setCurrentMode("Red");

				float[] cR = new float[colorSensorRight.sampleSize()];
				float[] cL = new float[colorSensorLeft.sampleSize()];
				if (x == 0) {
					colorSensorLeft.fetchSample(cL, 0);
					Float color = (float) cL[0];
					return (color);
				} else {
					colorSensorRight.fetchSample(cR, 0);
					Float color = (float) cR[0];
					return (color);
				}
			}
		}

		class followLineThread extends Thread {
			public static boolean forunning = false;
			private static boolean fostopped;
			static int maxV = 500;
			private double lightnow;
			static double bestLight = (com.ambientBlack*2 + com.ambientWhite)/3;
			double Lenkstärke2 = 0.09;
			public static double Lenkstaerke = 0.14*10;
			public void run() {// followLineThread
				while(true) {
					while (forunning == true) {
						fostopped = true;
						lightnow = com.colorRed(com.checkDriveColorSensor);
						if (lightnow < com.ambientBlack) {
							com.ambientBlack = lightnow;
							bestLight = (com.ambientBlack*2 + com.ambientWhite)/3;
						}if (lightnow > com.ambientWhite) {
							com.ambientWhite = lightnow;
							bestLight = (com.ambientBlack*2 + com.ambientWhite)/3;
						}
						if (lightnow < bestLight) {
							if (com.checkDriveColorSensor == 2) {
									com.motorRight.setSpeed((int) (maxV-(bestLight - lightnow) * 500*Lenkstaerke));//400 war die 500 vorher
								com.motorRight.forward();
								com.motorLeft.setSpeed(maxV);
								com.motorLeft.forward();
							} else if (com.checkDriveColorSensor == 0) {
								com.motorRight.setSpeed(maxV);
								com.motorRight.forward();
									com.motorLeft.setSpeed((int) (maxV-(bestLight - lightnow) * 500*Lenkstaerke));
							com.motorLeft.forward();
							}
						} else if (lightnow > bestLight) {
							if (com.checkDriveColorSensor == 2) {
									com.motorLeft.setSpeed((int) (maxV-(lightnow-bestLight) * 275*Lenkstaerke));// 175 dito
								com.motorLeft.forward();
								com.motorRight.setSpeed(maxV);
								com.motorRight.forward();
							} else if (com.checkDriveColorSensor == 0) {
								com.motorLeft.setSpeed(maxV);
								com.motorLeft.forward();
								com.motorRight.setSpeed((int) (maxV-(lightnow-bestLight) * 275*Lenkstaerke));
								com.motorRight.forward();
							}
						} else {
							com.motorRight.setSpeed(maxV);
							com.motorRight.forward();
							com.motorLeft.setSpeed(maxV);
							com.motorLeft.forward();
						}

					}
					if (fostopped == true) {
						com.MotorStop();
						fostopped = false;
					}

				}
				}
			}




