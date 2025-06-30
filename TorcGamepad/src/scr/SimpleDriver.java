package scr;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import scr.GamepadHandler;

public class SimpleDriver extends Controller {
    
    private class Feature {
        private SensorModel sensor;
        private Action action; 

        public Feature(SensorModel sensor, Action action) {
            this.sensor = sensor;
            this.action = action;
        }

        public SensorModel getSensor() {
            return sensor;
        }

        public Action getAction() {
            return action;
        }

      @Override
        public String toString() {
            double[] track = sensor.getTrackEdgeSensors();
            //double[] focus = sensor.getFocusSensors();
            //double[] opponents = sensor.getOpponentSensors();
            //double[] wheels = sensor.getWheelSpinVelocity();
            
            // Riga dati CSV
            StringBuilder csv = new StringBuilder();
            csv.append(String.format(
                Locale.US,
                "%.4f;%.4f;%.4f;%.4f;%.4f;%d;",
                sensor.getSpeed(),
                sensor.getAngleToTrackAxis(),
                sensor.getTrackPosition(),
                sensor.getLateralSpeed(),
                sensor.getRPM(),
                sensor.getGear()
            ));
            
            csv.append(String.format(Locale.US, "%.4f;", track[5])); //-40
            csv.append(String.format(Locale.US, "%.4f;", track[9])); // 0
            csv.append(String.format(Locale.US, "%.4f;", track[13])); // 40
            //for (double val : track) csv.append(String.format(Locale.US, "%.4f;", val));
            //for (double val : focus) csv.append(String.format(Locale.US, "%.4f;", val));
            //for (double val : opponents) csv.append(String.format(Locale.US, "%.4f;", val));
            //for (double val : wheels) csv.append(String.format(Locale.US, "%.4f;", val));
            
            
            csv.append(String.format(
                Locale.US,
                "%.4f;%.4f;%.4f;%d;%d;%.4f",
                action.accelerate,
                action.brake,
                action.clutch,
                action.focus,
                action.gear,
                action.steering
            ));
            
            return csv.toString();
            
            /*return sensor.getMessage()+";"+sensor.getAngleToTrackAxis()+";"
                    + action.accelerate+";"+ action.brake+";" + action.clutch+";"+action.focus+";"+action.gear+";"+action.steering;*/
        }
    }
    
    private List<Feature> l = new ArrayList<>();
    public GamepadHandler gamepad = new GamepadHandler(0);
    private float clutch = 0;
    
    /* Gear change constants */
   /* private final int[] gearUp = {9500, 10000, 12000, 13000, 14000, 0};    // RPM per salire
    private final int[] gearDown = {8500, 9000, 9200, 9500, 10000, 10000};  // RPM per scendere*/
    
    private final int[] gearUp = {6000, 7000, 8000, 12000, 14000, 0};       // Salita ultra-rapida alle marce alte
    private final int[] gearDown = {1000, 2000, 5000, 7000, 10000, 12000}; // Soglie di discesa rialzate
    
    // Parametri configurabili
    private final float STEERING_SMOOTHING = 0.1f;    // Più basso = più fluido
    private final float STEERING_SENSITIVITY = 0.3f;  // Più basso = meno sensibile agli input alti
    private final float STEERING_DEADZONE = 0.02f;
    private final float ACCEL_SMOOTHING = 0.2f;
    private final float ACCEL_CURVE = 0.1f;
    private final float ACCEL_DEADZONE = 0.05f;

    public void reset() {
        System.out.println("Restarting the race!");
    }

    public void shutdown() {
        System.out.println("Bye bye!");
        System.out.println("Inisio Salvataggio");
        this.salvaCSV();
        System.out.println("Fine Salvataggio");
    }

    private int getGear(SensorModel sensors) {
        int gear = sensors.getGear();
        double rpm = sensors.getRPM();

        if (gear < 1) return 1;
        if (gear < 6 && rpm >= gearUp[gear - 1]) return gear + 1;
        if (gear > 1 && rpm <= gearDown[gear - 1]) return gear - 1;
        return gear;
    }

    private float getSteer() {
        float stickX = -gamepad.getLeftStickX();
        
        // Deadzone
        if (Math.abs(stickX) < STEERING_DEADZONE) {
            stickX = 0;
        }       
        // Applica curva di sensibilità
        return applySteeringCurve(stickX, STEERING_SENSITIVITY);
    }

    private float lowPassFilter(float currentInput, float beta, float previousOutput) {
        return previousOutput + beta * (currentInput - previousOutput);
    }

    private float applySteeringCurve(float input, float sensitivity) {
        float sign = Math.signum(input);
        float absInput = Math.abs(input);
        return sign * (float) Math.pow(absInput, 1.0 / sensitivity);
    }

    private float getAccel() {
        float trigger = gamepad.getRightTrigger();
        
        // Deadzone
        if (trigger < ACCEL_DEADZONE) {
            trigger = 0;
        }
        
        // Filtro passa-basso
        float smoothedAccel;
        if (!l.isEmpty()) {
            smoothedAccel = lowPassFilter(
                trigger, 
                ACCEL_SMOOTHING, 
                (float) l.get(l.size() - 1).getAction().accelerate
            );
        } else {
            smoothedAccel = lowPassFilter(trigger, ACCEL_SMOOTHING, 0.0f);
        }
        
        return logarithmicSmoothing(smoothedAccel, ACCEL_CURVE);
    }

    private static float logarithmicSmoothing(float input, float curveFactor) {
        float clampedInput = Math.max(0.0f, Math.min(1.0f, input));
        float logCurve = (float) Math.log1p(curveFactor * clampedInput);
        float maxOutput = (float) Math.log1p(curveFactor);
        return logCurve / maxOutput;
    }

    private float getBrake() {
        return gamepad.getLeftTrigger();
    }
    
    private float getAntiLockBrake(float rawBrake, SensorModel sensors) {
    double[] wheels = sensors.getWheelSpinVelocity();
    double speed = sensors.getSpeed();
    
    // Se una ruota sta per bloccarsi (velocità < 80% della velocità dell'auto)
    for (double wheelSpeed : wheels) {
        if (wheelSpeed < speed * 0.8 && rawBrake > 0.1f) {
            return rawBrake * 0.3f; // Riduci la frenata del 30%
        }
    }
    return rawBrake;
    }

    public Action control(SensorModel sensors) {
    gamepad.update();
    
    if (!gamepad.isConnected()) {
        Action action = new Action();
        action.brake = 1;
        return action;
    }
    
    float steer = getSteer();
    float accel = getAccel();
    float brake = getBrake();
    
    brake = getAntiLockBrake(brake,sensors);
    
    // Adattamento alla velocità (versione per F1)
    float speed = (float) sensors.getSpeed();
    float speedFactor = 1.0f / (1.0f + speed / 50.0f);
    steer *= Math.max(0.9f, speedFactor);  // Sterzo minimo garantito del 90%
    
    
    // Controllo di trazione (opzionale per F1)
    double[] wheels = sensors.getWheelSpinVelocity();
    double avgWheelSpin = (wheels[0] + wheels[1] + wheels[2] + wheels[3]) / 4.0;
    if (avgWheelSpin > speed * 1.5) {  // Soglia più alta per F1
        accel *= 0.5f;
    }
    
    Action action = new Action();
    action.steering = steer;
    action.accelerate = accel;
    action.brake = brake;
    action.gear = getGear(sensors);
    action.clutch = clutch;
    
    l.add(new Feature(sensors, action));
    return action;
    }

    public void salvaCSV(){
        SensorModel sensor = l.getFirst().getSensor();
        //double[] track = sensor.getTrackEdgeSensors();
        //double[] focus = sensor.getFocusSensors();
        //double[] opponents = sensor.getOpponentSensors();
        //double[] wheels = sensor.getWheelSpinVelocity();
        // Intestazione CSV (una volta sola, all'inizio)
        //String header = "speed;angleToTrack;trackPos;gear;racePos;lateralSpeed;currentLapTime;damage;distanceFromStartLine;distanceRaced;fuel;lastLapTime;rpm;zSpeed;z;message";
        String header = "speed;angleToTrack;trackPos;lateralSpeed;RPM;gear;trackEdge-45°;trackEdge0°;trackEdge45°;accelerate;brake;clutch;focus;gear;steering";

        //for (int i = 0; i < track.length; i++) header += "trackEdge;" + i;
        //for (int i = 0; i < focus.length; i++) header += "focus;" + i;
        //for (int i = 0; i < opponents.length; i++) header += "opponent;" + i;
        //for (int i = 0; i < wheels.length; i++) header += "wheelSpin;" + i;
        StringBuilder corpo = new StringBuilder();
        
        for (Feature f : l){
            corpo.append(f.toString()+"\n");
        }
            
         try(BufferedWriter writer = new BufferedWriter(new FileWriter("../DataSet.csv"))){
            
             writer.write(header+"\n");
             writer.write(corpo.toString());
            
        } catch (IOException ex) {
            System.getLogger(SimpleDriver.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
        } 
    }
}