package test;


import scr.GamepadHandler;

public class GamepadExample {
    public static void main(String[] args) {
        GamepadHandler gamepad = new GamepadHandler(0); // Usa il primo controller
        
        try {
            while (true) {
                gamepad.update(); // Aggiorna lo stato del controller
                
                if (gamepad.isConnected()) {
                    // Stampa lo stato di tutti i pulsanti
                    printButtonState("A", gamepad.isAPressed());
                    printButtonState("B", gamepad.isBPressed());
                    printButtonState("X", gamepad.isXPressed());
                    printButtonState("Y", gamepad.isYPressed());
                    printButtonState("LB", gamepad.isLeftBumperPressed());
                    printButtonState("RB", gamepad.isRightBumperPressed());
                    printButtonState("Back", gamepad.isBackPressed());
                    printButtonState("Start", gamepad.isStartPressed());
                    
                    // Stampa gli analogici
                    printAnalogStick("Sinistro", 
                                   gamepad.getLeftStickX(), 
                                   gamepad.getLeftStickY());
                    printAnalogStick("Destro", 
                                   gamepad.getRightStickX(), 
                                   gamepad.getRightStickY());
                    
                    // Stampa i trigger
                    printTrigger("LT", gamepad.getLeftTrigger());
                    printTrigger("RT", gamepad.getRightTrigger());
                    
                    // Stampa il D-Pad
                    printDPad(gamepad.isDPadUpPressed(), 
                             gamepad.isDPadDownPressed(),
                             gamepad.isDPadLeftPressed(), 
                             gamepad.isDPadRightPressed());
                    
                    // Piccola pausa per evitare di sovraccaricare la CPU
                    Thread.sleep(16); // ~60fps
                    System.out.println("----------------------"); // Separatore tra i frame
                } else {
                    System.out.println("Controller non connesso");
                    Thread.sleep(1000);
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            gamepad.close(); // Chiudi la connessione quando hai finito
        }
    }
    
    private static void printButtonState(String buttonName, boolean isPressed) {
        if (isPressed) {
            System.out.println("Pulsante " + buttonName + " premuto");
        }
    }
    
    private static void printAnalogStick(String stickName, float x, float y) {
        if (Math.abs(x) > 0.2 || Math.abs(y) > 0.2) {
            System.out.printf("Analogico %s: X=%.2f, Y=%.2f%n", stickName, x, y);
        }
    }
    
    private static void printTrigger(String triggerName, float value) {
        if (value > 0.1) {
            System.out.printf("Trigger %s: %.2f%n", triggerName, value);
        }
    }
    
    private static void printDPad(boolean up, boolean down, boolean left, boolean right) {
        if (up || down || left || right) {
            StringBuilder dpadState = new StringBuilder("D-Pad: ");
            if (up) dpadState.append("Su ");
            if (down) dpadState.append("Giù ");
            if (left) dpadState.append("Sinistra ");
            if (right) dpadState.append("Destra");
            
            System.out.println(dpadState.toString().trim());
        }
    }
}