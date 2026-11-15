import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;

/**
 * ============================================================================
 * PROJECT: SmartFlow 2.0 - Ultimate Autonomous Traffic & Urban Simulation
 * FEATURES:
 *   1. 📸 Automated Speed Camera & E-Challan Engine (Red Light Jumping + Fines)
 *   2. 🧠 Dynamic Driver Personalities (Learner [L], Rash, VIP Convoy, Normal)
 *   3. 🔊 Horn & Impatient Honking Simulation (Speech Bubbles: "BEEP BEEP!")
 *   4. 🚧 Interactive Click-to-Place Roadblocks / Traffic Cones (Mouse Click!)
 *   5. 🚶 Pedestrian Crossing Engine (Zebra crossing button + pedestrians)
 *   6. 🌙 Dynamic Night Mode with Headlight Beams & Street Lamps
 *   7. 🚨 Emergency Green Corridor (V2X priority for Ambulances & VIPs)
 *   8. 🌱 Live Eco-Carbon & Idle Fuel Wastage Telemetry
 *   9. 🤖 Continuous Auto-Traffic Dispatcher
 * ============================================================================
 */

// ==========================================
// 1. ENUMS (Direction, Signal Color, Driver Personality)
// ==========================================
enum Direction {
    NORTH, SOUTH, EAST, WEST
}

enum SignalColor {
    RED, YELLOW, GREEN
}

enum DriverPersonality {
    NORMAL,         // Rule follower
    LEARNER,        // Drives slow, delays on green, marked with [L]
    RASH,           // Fast, tailgates, honks, 25% chance of jumping red light!
    VIP_MINISTER,   // Black tinted convoy with red/blue beacon, doesn't stop
    AMBULANCE       // Emergency vehicle, triggers Green Corridor
}

// ==========================================
// 2. ROADBLOCK / CONE ENTITY (Interactive Mouse Click)
// ==========================================
class CrashParticle {
    double x, y;
    double vx, vy;
    Color color;
    int life;
    int maxLife;
    int size;

    public CrashParticle(double x, double y, Color color, double speed, int maxLife) {
        this.x = x;
        this.y = y;
        this.color = color;
        double angle = Math.random() * Math.PI * 2;
        this.vx = Math.cos(angle) * speed;
        this.vy = Math.sin(angle) * speed;
        this.maxLife = maxLife;
        this.life = maxLife;
        this.size = 3 + new Random().nextInt(5);
    }

    public boolean update() {
        x += vx;
        y += vy;
        vx *= 0.94;
        vy *= 0.94;
        life--;
        return life > 0;
    }

    public void draw(Graphics2D g2) {
        float alpha = Math.max(0.0f, Math.min(1.0f, (float) life / maxLife));
        g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), (int) (alpha * 230)));
        g2.fillOval((int) x - size / 2, (int) y - size / 2, size, size);
    }
}

class RoadCone {
    double x, y;
    int radius = 10;

    public RoadCone(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public void draw(Graphics2D g2) {
        // Shadow
        g2.setColor(new Color(0, 0, 0, 80));
        g2.fillOval((int) x - radius + 2, (int) y - radius + 3, radius * 2, radius * 2);

        // Orange base
        g2.setColor(new Color(230, 126, 34));
        g2.fillOval((int) x - radius, (int) y - radius, radius * 2, radius * 2);

        // White reflective inner ring
        g2.setColor(Color.WHITE);
        g2.fillOval((int) x - radius / 2, (int) y - radius / 2, radius, radius);

        // Center black tip
        g2.setColor(Color.BLACK);
        g2.fillOval((int) x - 2, (int) y - 2, 4, 4);
    }
}

// ==========================================
// 3. PEDESTRIAN ENTITY (Walking on Crosswalk)
// ==========================================
class Pedestrian {
    double x, y;
    double targetX, targetY;
    double speed = 1.2;
    Color shirtColor;
    boolean isCrossing = false;
    boolean finished = false;

    public Pedestrian(double startX, double startY, double targetX, double targetY) {
        this.x = startX;
        this.y = startY;
        this.targetX = targetX;
        this.targetY = targetY;
        Color[] shirts = {new Color(52, 152, 219), new Color(231, 76, 60), new Color(46, 204, 113), new Color(155, 89, 182)};
        this.shirtColor = shirts[new Random().nextInt(shirts.length)];
    }

    public void update() {
        if (!isCrossing || finished) return;

        double dx = targetX - x;
        double dy = targetY - y;
        double dist = Math.hypot(dx, dy);

        if (dist < 2.0) {
            finished = true;
            isCrossing = false;
        } else {
            x += (dx / dist) * speed;
            y += (dy / dist) * speed;
        }
    }

    public void draw(Graphics2D g2, long tick) {
        if (finished) return;

        // Shadow
        g2.setColor(new Color(0, 0, 0, 70));
        g2.fillOval((int) x - 4, (int) y - 2, 8, 4);

        // Body / Shirt
        g2.setColor(shirtColor);
        g2.fillOval((int) x - 5, (int) y - 7, 10, 8);

        // Head (Skin tone)
        g2.setColor(new Color(255, 218, 185));
        g2.fillOval((int) x - 3, (int) y - 11, 6, 6);

        // Backpack / Bag
        g2.setColor(new Color(44, 62, 80));
        g2.fillOval((int) x - 4, (int) y - 5, 4, 4);
    }
}

// ==========================================
// 4. VEHICLE ENTITY CLASS (Multi-Agent Modeling)
// ==========================================
class Vehicle {
    private static int idCounter = 1000;
    private int id;
    private String plateNumber;
    private Direction direction;
    private DriverPersonality personality;
    private Color color;

    // Kinematics coordinates
    private double x, y;
    private double speed;
    private double targetSpeed;
    private double maxSpeed;
    private double acceleration = 0.16;
    private double deceleration = 0.28;

    // Dimensions
    private int length = 42;
    private int width = 22;

    // Behavioral state
    private boolean isBraking = false;
    private boolean hasCrossedIntersection = false;
    private boolean hasJumpingRedLight = false;
    private boolean hasBeenChallaned = false;
    private boolean isCrashed = false;
    private double crashAngle = 0.0;
    private int honkTimer = 0; // Ticks for "BEEP BEEP" bubble
    private int waitTicks = 0;
    private long spawnTime;

    public Vehicle(Direction direction, DriverPersonality personality, double startX, double startY) {
        this.id = idCounter++;
        this.direction = direction;
        this.personality = personality;
        this.x = startX;
        this.y = startY;
        this.spawnTime = System.currentTimeMillis();

        // Generate realistic Indian license plate (e.g. DL-04-AB-1290)
        String[] rto = {"DL-01", "MH-12", "KA-03", "UP-16", "HR-26", "GJ-01"};
        int num = 1000 + new Random().nextInt(9000);
        this.plateNumber = rto[new Random().nextInt(rto.length)] + "-" + (char) ('A' + new Random().nextInt(26)) + (char) ('A' + new Random().nextInt(26)) + "-" + num;

        // Configure physics based on driver personality
        switch (personality) {
            case RASH:
                this.color = new Color(214, 48, 49); // Fierce Red
                this.maxSpeed = 4.5;
                this.speed = 4.5;
                this.targetSpeed = 4.5;
                this.acceleration = 0.35;
                this.deceleration = 0.40;
                // 100% Guaranteed to jump red light for flawless viva demonstration!
                this.hasJumpingRedLight = true;
                break;

            case LEARNER:
                this.color = new Color(241, 196, 15); // Yellow learner
                this.maxSpeed = 1.9; // Runs slow
                this.acceleration = 0.08;
                break;

            case VIP_MINISTER:
                this.color = new Color(20, 20, 20); // Jet Black VIP SUV
                this.maxSpeed = 3.5;
                this.length = 46;
                this.width = 24;
                break;

            case AMBULANCE:
                this.color = new Color(245, 245, 245);
                this.maxSpeed = 3.7;
                this.length = 48;
                this.width = 25;
                break;

            case NORMAL:
            default:
                Color[] normalColors = {new Color(52, 152, 219), new Color(46, 204, 113), new Color(155, 89, 182), new Color(230, 126, 34), new Color(52, 73, 94)};
                this.color = normalColors[new Random().nextInt(normalColors.length)];
                this.maxSpeed = 2.8;
                break;
        }
        this.speed = this.maxSpeed * 0.7;
        this.targetSpeed = this.maxSpeed;
    }

    public void updatePhysics(double simSpeedMultiplier) {
        if (isCrashed) {
            speed = 0;
            targetSpeed = 0;
            return;
        }

        if (speed < targetSpeed) {
            speed = Math.min(targetSpeed, speed + acceleration * simSpeedMultiplier);
        } else if (speed > targetSpeed) {
            speed = Math.max(targetSpeed, speed - deceleration * simSpeedMultiplier);
        }

        isBraking = (targetSpeed < 0.2 && speed > 0.1);

        if (speed < 0.2) {
            waitTicks++;
            // Impatient Rash drivers honk after waiting 1.5 seconds!
            if (personality == DriverPersonality.RASH && waitTicks > 90 && honkTimer == 0) {
                honkTimer = 50; // Show "HONK!" for ~50 frames
            }
        } else {
            waitTicks = 0;
        }

        if (honkTimer > 0) honkTimer--;

        // Move according to heading
        double delta = speed * simSpeedMultiplier;
        switch (direction) {
            case SOUTH: y += delta; break;
            case NORTH: y -= delta; break;
            case EAST:  x += delta; break;
            case WEST:  x -= delta; break;
        }
    }

    public void stopVehicle() { this.targetSpeed = 0.0; }
    public void resumeVehicle() { this.targetSpeed = this.maxSpeed; }
    public void adjustSpeed(double target) { this.targetSpeed = Math.min(this.maxSpeed, target); }

    public double getFrontX() {
        if (direction == Direction.EAST) return x + length / 2.0;
        if (direction == Direction.WEST) return x - length / 2.0;
        return x;
    }

    public double getFrontY() {
        if (direction == Direction.SOUTH) return y + length / 2.0;
        if (direction == Direction.NORTH) return y - length / 2.0;
        return y;
    }

    public double getRearX() {
        if (direction == Direction.EAST) return x - length / 2.0;
        if (direction == Direction.WEST) return x + length / 2.0;
        return x;
    }

    public double getRearY() {
        if (direction == Direction.SOUTH) return y - length / 2.0;
        if (direction == Direction.NORTH) return y + length / 2.0;
        return y;
    }

    public void draw(Graphics2D g2, long tickCount, boolean isNightMode) {
        Graphics2D g = (Graphics2D) g2.create();
        g.translate(x, y);

        switch (direction) {
            case SOUTH: g.rotate(Math.PI / 2); break;
            case NORTH: g.rotate(-Math.PI / 2); break;
            case WEST:  g.rotate(Math.PI); break;
            case EAST:  break;
        }

        if (isCrashed) {
            g.rotate(crashAngle);
        }

        // ==========================================
        // DYNAMIC HEADLIGHT CONES (NIGHT MODE EFFECT)
        // ==========================================
        if (isNightMode) {
            Graphics2D gLight = (Graphics2D) g.create();
            gLight.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.45f));
            // Trapezoid headlight light beam
            int beamLength = 110;
            int beamWidth = 45;
            Polygon beam = new Polygon();
            beam.addPoint(length / 2, -width / 2 + 3);
            beam.addPoint(length / 2 + beamLength, -beamWidth);
            beam.addPoint(length / 2 + beamLength, beamWidth);
            beam.addPoint(length / 2, width / 2 - 3);

            gLight.setPaint(new GradientPaint(length / 2, 0, new Color(255, 255, 200, 200),
                    length / 2 + beamLength, 0, new Color(255, 255, 100, 0)));
            gLight.fillPolygon(beam);
            gLight.dispose();
        }

        // Shadow
        g.setColor(new Color(0, 0, 0, 75));
        g.fillRoundRect(-length / 2 + 2, -width / 2 + 3, length, width, 8, 8);

        // Vehicle Body
        g.setColor(color);
        g.fillRoundRect(-length / 2, -width / 2, length, width, 7, 7);
        g.setColor(new Color(0, 0, 0, 110));
        g.drawRoundRect(-length / 2, -width / 2, length, width, 7, 7);

        // Crash impact damage
        if (isCrashed) {
            g.setColor(new Color(30, 30, 30, 220));
            g.fillOval(length / 2 - 14, -width / 2 + 2, 16, width - 4);
            g.setColor(new Color(230, 126, 34));
            g.fillOval(length / 2 - 10, -width / 2 + 5, 8, width - 10);
        }

        // Windshields
        g.setColor(new Color(41, 128, 185, 220));
        g.fillRoundRect(length / 2 - 16, -width / 2 + 3, 7, width - 6, 3, 3); // Front
        g.fillRoundRect(-length / 2 + 7, -width / 2 + 3, 5, width - 6, 2, 2);  // Rear

        // Roof
        g.setColor(color.darker());
        g.fillRect(-length / 2 + 13, -width / 2 + 4, length - 29, width - 8);

        // Headlights
        g.setColor(new Color(254, 249, 231));
        g.fillRect(length / 2 - 2, -width / 2 + 2, 2, 4);
        g.fillRect(length / 2 - 2, width / 2 - 6, 2, 4);

        // Brake Lights
        if (isBraking || speed < 0.2) {
            g.setColor(new Color(255, 20, 20));
            g.fillRect(-length / 2, -width / 2 + 2, 3, 4);
            g.fillRect(-length / 2, width / 2 - 6, 3, 4);
            // Red glow
            g.setColor(new Color(255, 0, 0, 80));
            g.fillOval(-length / 2 - 6, -width / 2 - 2, 10, 8);
            g.fillOval(-length / 2 - 6, width / 2 - 6, 10, 8);
        } else {
            g.setColor(new Color(150, 20, 20));
            g.fillRect(-length / 2, -width / 2 + 2, 2, 3);
            g.fillRect(-length / 2, width / 2 - 5, 2, 3);
        }

        // SPECIAL IDENTIFIERS ON ROOF
        if (personality == DriverPersonality.LEARNER) {
            // [L] Plate
            g.setColor(Color.WHITE);
            g.fillRoundRect(-6, -6, 12, 12, 2, 2);
            g.setColor(Color.RED);
            g.setFont(new Font("Arial", Font.BOLD, 10));
            g.drawString("L", -3, 4);
        } else if (personality == DriverPersonality.VIP_MINISTER) {
            // Flashing VIP Red & Blue Beacon on roof
            boolean flash = (tickCount / 8) % 2 == 0;
            g.setColor(flash ? new Color(231, 76, 60) : new Color(52, 152, 219));
            g.fillOval(-4, -4, 8, 8);
            g.setColor(Color.WHITE);
            g.drawOval(-4, -4, 8, 8);
        } else if (personality == DriverPersonality.AMBULANCE) {
            // Flashing Siren
            boolean flash = (tickCount / 10) % 2 == 0;
            g.setColor(flash ? Color.RED : Color.BLUE);
            g.fillOval(-3, -4, 6, 8);
            // Red Cross on roof
            g.setColor(Color.RED);
            g.fillRect(-8, -2, 8, 4);
            g.fillRect(-6, -4, 4, 8);
        }

        g.dispose();

        // ==========================================
        // HONK / HORN SPEECH BUBBLE ("BEEP BEEP!")
        // ==========================================
        if (honkTimer > 0) {
            Graphics2D gHonk = (Graphics2D) g2.create();
            gHonk.translate(x, y - 25);
            gHonk.setColor(new Color(255, 234, 167));
            gHonk.fillRoundRect(-28, -14, 56, 18, 6, 6);
            gHonk.setColor(new Color(214, 48, 49));
            gHonk.drawRoundRect(-28, -14, 56, 18, 6, 6);
            gHonk.setFont(new Font("Segoe UI", Font.BOLD, 10));
            gHonk.drawString("🔊 BEEP!!", -22, -1);
            gHonk.dispose();
        }
    }

    // Getters and Setters
    public int getId() { return id; }
    public String getPlateNumber() { return plateNumber; }
    public Direction getDirection() { return direction; }
    public DriverPersonality getPersonality() { return personality; }
    public double getX() { return x; }
    public double getY() { return y; }
    public double getSpeed() { return speed; }
    public int getLength() { return length; }
    public boolean hasCrossedIntersection() { return hasCrossedIntersection; }
    public void setCrossedIntersection(boolean status) { this.hasCrossedIntersection = status; }
    public boolean isJumpingRedLight() { return hasJumpingRedLight; }
    public boolean hasBeenChallaned() { return hasBeenChallaned; }
    public void setHasBeenChallaned(boolean status) { this.hasBeenChallaned = status; }
    public boolean isCrashed() { return isCrashed; }
    public void crash(double offset) {
        this.isCrashed = true;
        this.speed = 0;
        this.targetSpeed = 0;
        this.crashAngle = offset;
    }
    public long getSpawnTime() { return spawnTime; }
    public void forceHonk() { this.honkTimer = 50; }
}

// ==========================================
// 5. REINFORCEMENT LEARNING AGENT (Q-LEARNING ALGORITHM)
// ==========================================
class QLearningAgent {
    public static final int NUM_STATES = 18;
    public static final int NUM_ACTIONS = 2; // 0 = KEEP_PHASE, 1 = SWITCH_PHASE

    // Q-Table mapping States to Actions
    private double[][] qTable = new double[NUM_STATES][NUM_ACTIONS];
    private double alpha = 0.25;   // Learning Rate (Weight given to new TD experiences)
    private double gamma = 0.85;   // Discount Factor (Importance of future rewards)
    private double epsilon = 0.15; // Epsilon-Greedy policy (15% explore, 85% exploit)

    private int lastState = 0;
    private int lastAction = 0;
    private double cumulativeReward = 0.0;
    private int totalDecisions = 0;
    private int explorationCount = 0;
    private int exploitationCount = 0;
    private String lastDecisionExplanation = "RL Model initialized with zero-prior Q-Table.";

    public QLearningAgent() {
        for (int s = 0; s < NUM_STATES; s++) {
            for (int a = 0; a < NUM_ACTIONS; a++) {
                qTable[s][a] = 0.0;
            }
        }
    }

    public int discretizeState(int nsQueue, int ewQueue, int activeGreenPhase) {
        int nsLevel = (nsQueue <= 1) ? 0 : (nsQueue <= 3 ? 1 : 2);
        int ewLevel = (ewQueue <= 1) ? 0 : (ewQueue <= 3 ? 1 : 2);
        int phaseBit = (activeGreenPhase == 0) ? 0 : 1;
        return (nsLevel * 6) + (ewLevel * 2) + phaseBit;
    }

    public int selectAction(int state) {
        totalDecisions++;
        // Epsilon-Greedy Exploration vs Exploitation
        if (Math.random() < epsilon) {
            explorationCount++;
            int randomAction = new Random().nextInt(NUM_ACTIONS);
            lastDecisionExplanation = String.format("RL Action: Explore (%s)", randomAction == 0 ? "HOLD" : "SWITCH");
            return randomAction;
        } else {
            exploitationCount++;
            int bestAction = 0;
            double maxQ = qTable[state][0];
            for (int a = 1; a < NUM_ACTIONS; a++) {
                if (qTable[state][a] > maxQ) {
                    maxQ = qTable[state][a];
                    bestAction = a;
                }
            }
            lastDecisionExplanation = String.format("RL Action: Bellman Exploit %s (Q=%.2f)", bestAction == 0 ? "HOLD" : "SWITCH", maxQ);
            return bestAction;
        }
    }

    public void update(int state, int action, double reward, int nextState) {
        // Bellman Optimality Equation:
        // Q(s, a) <- Q(s, a) + alpha * [ Reward + gamma * max_a' Q(s', a') - Q(s, a) ]
        double maxNextQ = Math.max(qTable[nextState][0], qTable[nextState][1]);
        double currentQ = qTable[state][action];
        double tdError = (reward + gamma * maxNextQ) - currentQ;
        qTable[state][action] = currentQ + alpha * tdError;

        cumulativeReward += reward;
    }

    public double calculateReward(int nsWaiting, int ewWaiting, int totalClearedDelta) {
        // Reward function: +8 for each vehicle passing intersection, -1.2 per waiting vehicle in queue
        return (totalClearedDelta * 8.0) - ((nsWaiting + ewWaiting) * 1.2);
    }

    public double getCumulativeReward() { return cumulativeReward; }
    public double getAlpha() { return alpha; }
    public double getGamma() { return gamma; }
    public double getEpsilon() { return epsilon; }
    public int getTotalDecisions() { return totalDecisions; }
    public String getLastDecisionExplanation() { return lastDecisionExplanation; }
    public double getQValue(int state, int action) { return qTable[state][action]; }
    public int getLastState() { return lastState; }
    public int getLastAction() { return lastAction; }
    public void setLastState(int s) { this.lastState = s; }
    public void setLastAction(int a) { this.lastAction = a; }
}

enum ControlMode {
    RL_Q_LEARNING,
    HEURISTIC_ADAPTIVE,
    FIXED_TIMER
}

// ==========================================
// 6. TRAFFIC SIGNAL CONTROLLER CLASS
// ==========================================
class SignalController {
    private int currentPhase = 0; // 0: NS Green, 1: NS Yellow, 2: EW Green, 3: EW Yellow, 4: All-Red Pedestrian Phase
    private int phaseTicks = 0;
    private int greenDuration = 300;
    private int yellowDuration = 90;
    private int pedestrianPhaseTicks = 0;

    private ControlMode mode = ControlMode.RL_Q_LEARNING; // Default to Machine Learning!
    private QLearningAgent rlAgent = new QLearningAgent();
    private int rlCooldown = 0;

    private boolean isGreenCorridorActive = false;
    private String decisionLog = "System active in Q-Learning (Reinforcement Learning) Mode.";

    public void update(int nsWaiting, int ewWaiting, boolean emergencyDetected, boolean pedestrianRequest, int clearedDelta) {
        phaseTicks++;

        // 1. EMERGENCY GREEN CORRIDOR OVERRIDE (Ambulance / VIP Convoy)
        if (emergencyDetected) {
            isGreenCorridorActive = true;
            decisionLog = "🚨 EMERGENCY PROTOCOL: Green Corridor activated for Priority Vehicle!";
            return;
        } else {
            isGreenCorridorActive = false;
        }

        // 2. PEDESTRIAN CROSSING PHASE
        if (pedestrianRequest && currentPhase != 4) {
            currentPhase = 4;
            phaseTicks = 0;
            decisionLog = "🚶 PEDESTRIAN PHASE: All vehicle signals set to RED for crosswalk.";
            return;
        }

        if (currentPhase == 4) {
            pedestrianPhaseTicks++;
            if (pedestrianPhaseTicks > 240) {
                pedestrianPhaseTicks = 0;
                currentPhase = 0;
                phaseTicks = 0;
            }
            return;
        }

        // 3. EXECUTE ACTIVE CONTROL MODE
        if (mode == ControlMode.RL_Q_LEARNING) {
            // Transitions for Yellow lights
            if (currentPhase == 1) {
                if (phaseTicks >= yellowDuration) { currentPhase = 2; phaseTicks = 0; }
                return;
            } else if (currentPhase == 3) {
                if (phaseTicks >= yellowDuration) { currentPhase = 0; phaseTicks = 0; }
                return;
            }

            int minGreenTicks = 120; // 2 seconds minimum green before RL can switch
            rlCooldown++;

            // Every 60 ticks (1s) after min green, RL Agent evaluates state and takes Bellman action
            if (phaseTicks >= minGreenTicks && rlCooldown >= 60) {
                rlCooldown = 0;

                int activePhaseBit = (currentPhase == 0) ? 0 : 1;
                int currentState = rlAgent.discretizeState(nsWaiting, ewWaiting, activePhaseBit);
                double reward = rlAgent.calculateReward(nsWaiting, ewWaiting, clearedDelta);

                // Update Q-Table with previous experience
                rlAgent.update(rlAgent.getLastState(), rlAgent.getLastAction(), reward, currentState);

                // Choose next action via Epsilon-Greedy policy
                int action = rlAgent.selectAction(currentState);
                rlAgent.setLastState(currentState);
                rlAgent.setLastAction(action);

                if (action == 1) {
                    // Action 1: SWITCH_PHASE
                    if (currentPhase == 0) { currentPhase = 1; phaseTicks = 0; }
                    else if (currentPhase == 2) { currentPhase = 3; phaseTicks = 0; }
                    decisionLog = "[RL Agent] " + rlAgent.getLastDecisionExplanation() + " -> Switched Phase";
                } else {
                    // Action 0: KEEP_PHASE
                    decisionLog = "[RL Agent] " + rlAgent.getLastDecisionExplanation() + " -> Holding Green Flow";
                }
            } else if (phaseTicks >= greenDuration + 120) {
                // Max safety timeout
                if (currentPhase == 0) { currentPhase = 1; phaseTicks = 0; }
                else if (currentPhase == 2) { currentPhase = 3; phaseTicks = 0; }
            }

        } else if (mode == ControlMode.HEURISTIC_ADAPTIVE) {
            int minGreenTicks = 120;
            if (currentPhase == 0) {
                if (phaseTicks >= minGreenTicks && nsWaiting == 0 && ewWaiting > 0) {
                    currentPhase = 1; phaseTicks = 0;
                    decisionLog = "Heuristic AI: NS road clear. Early switch to EW.";
                } else if (phaseTicks >= minGreenTicks && ewWaiting >= 4 && phaseTicks >= greenDuration - 90) {
                    currentPhase = 1; phaseTicks = 0;
                    decisionLog = "Heuristic AI: Heavy congestion on EW (" + ewWaiting + " cars).";
                } else if (phaseTicks >= greenDuration) {
                    currentPhase = 1; phaseTicks = 0;
                }
            } else if (currentPhase == 1) {
                if (phaseTicks >= yellowDuration) { currentPhase = 2; phaseTicks = 0; }
            } else if (currentPhase == 2) {
                if (phaseTicks >= minGreenTicks && ewWaiting == 0 && nsWaiting > 0) {
                    currentPhase = 3; phaseTicks = 0;
                    decisionLog = "Heuristic AI: EW road clear. Early switch to NS.";
                } else if (phaseTicks >= minGreenTicks && nsWaiting >= 4 && phaseTicks >= greenDuration - 90) {
                    currentPhase = 3; phaseTicks = 0;
                    decisionLog = "Heuristic AI: Heavy congestion on NS (" + nsWaiting + " cars).";
                } else if (phaseTicks >= greenDuration) {
                    currentPhase = 3; phaseTicks = 0;
                }
            } else if (currentPhase == 3) {
                if (phaseTicks >= yellowDuration) { currentPhase = 0; phaseTicks = 0; }
            }

        } else { // ControlMode.FIXED_TIMER
            int target = (currentPhase == 0 || currentPhase == 2) ? greenDuration : yellowDuration;
            if (phaseTicks >= target) {
                currentPhase = (currentPhase + 1) % 4;
                phaseTicks = 0;
                decisionLog = "Fixed Timer: Switched to Phase " + currentPhase;
            }
        }
    }

    public void manualSwitch() {
        if (currentPhase == 0) currentPhase = 1;
        else if (currentPhase == 2) currentPhase = 3;
        phaseTicks = 0;
        decisionLog = "Manual Operator Light Override.";
    }

    public void forceDirection(boolean forceNorthSouth) {
        if (forceNorthSouth) {
            currentPhase = 0;
        } else {
            currentPhase = 2;
        }
        phaseTicks = 0;
        decisionLog = "Signal Locked for Challan Demo: NS is RED!";
    }

    public SignalColor getNorthSouthColor() {
        if (currentPhase == 4) return SignalColor.RED;
        if (currentPhase == 0) return SignalColor.GREEN;
        if (currentPhase == 1) return SignalColor.YELLOW;
        return SignalColor.RED;
    }

    public SignalColor getEastWestColor() {
        if (currentPhase == 4) return SignalColor.RED;
        if (currentPhase == 2) return SignalColor.GREEN;
        if (currentPhase == 3) return SignalColor.YELLOW;
        return SignalColor.RED;
    }

    public ControlMode getMode() { return mode; }
    public void setMode(ControlMode mode) {
        this.mode = mode;
        this.decisionLog = "Active Control Mode Switched to " + mode.name();
    }
    public QLearningAgent getRlAgent() { return rlAgent; }
    public String getDecisionLog() { return decisionLog; }
    public boolean isGreenCorridorActive() { return isGreenCorridorActive; }
    public int getRemainingSeconds() {
        int target = (currentPhase == 0 || currentPhase == 2) ? greenDuration : yellowDuration;
        return Math.max(0, (target - phaseTicks) / 60);
    }
}

// ==============================================================
// 6. SIMULATION CANVAS PANEL (Graphics2D + Physics + Cones + Camera)
// ==============================================================
class SimulationPanel extends JPanel {
    private List<Vehicle> vehicles = new ArrayList<>();
    private List<RoadCone> roadCones = new ArrayList<>();
    private List<Pedestrian> pedestrians = new ArrayList<>();
    private SignalController signalController = new SignalController();

    private long tickCounter = 0;
    private double simSpeedMultiplier = 1.0;
    private boolean isPaused = false;
    private boolean isNightMode = false;
    private boolean isAutoSpawnEnabled = true;
    private boolean pedestrianCrossingActive = false;

    // Camera Flash effect
    private float cameraFlashAlpha = 0.0f;
    private String lastChallanText = null;
    private int challanBannerTimer = 0;

    // Crash and Accident Simulation
    private List<CrashParticle> crashParticles = new ArrayList<>();
    private boolean isAccidentActive = false;
    private int totalAccidents = 0;
    private String accidentBannerText = null;
    private int accidentBannerTimer = 0;

    // Statistics & Telemetry
    private int totalSpawned = 0;
    private int totalPassed = 0;
    private int totalChallansIssued = 0;
    private int totalChallanFineAmount = 0;
    private double totalCo2EmittedGrams = 0;
    private double totalFuelWastedMl = 0;

    // Geometry
    private final int roadWidth = 140;
    private final int laneWidth = 70;

    public SimulationPanel() {
        setBackground(new Color(46, 125, 50));

        // Interactive Mouse Click: Add / Remove Roadblock Cones!
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                // Check if user clicked near an existing cone to remove it
                RoadCone toRemove = null;
                for (RoadCone c : roadCones) {
                    if (Math.hypot(c.x - e.getX(), c.y - e.getY()) < 18) {
                        toRemove = c;
                        break;
                    }
                }

                if (toRemove != null) {
                    roadCones.remove(toRemove);
                } else {
                    // Place new roadblock cone
                    roadCones.add(new RoadCone(e.getX(), e.getY()));
                }
                repaint();
            }
        });
    }

    public void tick() {
        if (isPaused) return;
        tickCounter++;

        // Camera flash decay
        if (cameraFlashAlpha > 0.05f) {
            cameraFlashAlpha -= 0.08f;
        } else {
            cameraFlashAlpha = 0.0f;
        }

        if (challanBannerTimer > 0) challanBannerTimer--;
        if (accidentBannerTimer > 0) accidentBannerTimer--;

        // Update Crash Particles & Smoke
        crashParticles.removeIf(p -> !p.update());
        if (isAccidentActive && tickCounter % 5 == 0) {
            for (Vehicle v : vehicles) {
                if (v.isCrashed()) {
                    crashParticles.add(new CrashParticle(v.getX() + (Math.random() * 8 - 4),
                            v.getY() + (Math.random() * 8 - 4), new Color(110, 110, 110), 0.8, 35));
                }
            }
        }

        int cx = getWidth() / 2;
        int cy = getHeight() / 2;

        int stopLineNorth = cy - roadWidth / 2;
        int stopLineSouth = cy + roadWidth / 2;
        int stopLineWest = cx - roadWidth / 2;
        int stopLineEast = cx + roadWidth / 2;

        SignalColor nsColor = signalController.getNorthSouthColor();
        SignalColor ewColor = signalController.getEastWestColor();

        // 1. AUTO-SPAWN TRAFFIC (Every ~90 frames = 1.5 seconds)
        if (isAutoSpawnEnabled && tickCounter % 90 == 0 && vehicles.size() < 16) {
            spawnRandomVehicle();
        }

        // 2. CHECK EMERGENCY VEHICLES FOR GREEN CORRIDOR
        boolean emergencyActive = false;
        for (Vehicle v : vehicles) {
            if ((v.getPersonality() == DriverPersonality.AMBULANCE || v.getPersonality() == DriverPersonality.VIP_MINISTER)
                    && !v.hasCrossedIntersection()) {
                emergencyActive = true;
                break;
            }
        }

        // 3. UPDATE PEDESTRIANS
        boolean pStillCrossing = false;
        for (Pedestrian p : pedestrians) {
            p.update();
            if (p.isCrossing && !p.finished) pStillCrossing = true;
        }
        pedestrianCrossingActive = pStillCrossing;

        int nsWaiting = 0;
        int ewWaiting = 0;
        int clearedThisTick = 0;

        // 4. VEHICLES UPDATE & COLLISION LOGIC
        for (int i = 0; i < vehicles.size(); i++) {
            Vehicle v = vehicles.get(i);

            // A. Check if vehicle crossed intersection
            if (!v.hasCrossedIntersection()) {
                boolean crossed = false;
                if (v.getDirection() == Direction.SOUTH && v.getRearY() > stopLineSouth) crossed = true;
                if (v.getDirection() == Direction.NORTH && v.getRearY() < stopLineNorth) crossed = true;
                if (v.getDirection() == Direction.EAST && v.getRearX() > stopLineEast) crossed = true;
                if (v.getDirection() == Direction.WEST && v.getRearX() < stopLineWest) crossed = true;

                if (crossed) {
                    v.setCrossedIntersection(true);
                    totalPassed++;
                    clearedThisTick++;
                }
            }

            // B. SPEED CAMERA & E-CHALLAN CHECK (Red light jumping)
            if (!v.hasCrossedIntersection() && !v.hasBeenChallaned()
                    && v.getPersonality() != DriverPersonality.AMBULANCE
                    && v.getPersonality() != DriverPersonality.VIP_MINISTER) {

                boolean jumpedLight = false;
                if (v.getDirection() == Direction.SOUTH && nsColor != SignalColor.GREEN && v.getFrontY() >= stopLineNorth && v.getFrontY() <= stopLineSouth) jumpedLight = true;
                if (v.getDirection() == Direction.NORTH && nsColor != SignalColor.GREEN && v.getFrontY() <= stopLineSouth && v.getFrontY() >= stopLineNorth) jumpedLight = true;
                if (v.getDirection() == Direction.EAST && ewColor != SignalColor.GREEN && v.getFrontX() >= stopLineWest && v.getFrontX() <= stopLineEast) jumpedLight = true;
                if (v.getDirection() == Direction.WEST && ewColor != SignalColor.GREEN && v.getFrontX() <= stopLineEast && v.getFrontX() >= stopLineWest) jumpedLight = true;

                if (jumpedLight) {
                    issueEChallan(v);
                }
            }

            // C. Check Roadblock Cones Ahead (Interactive mouse roadblocks)
            boolean obstacleAhead = false;
            for (RoadCone cone : roadCones) {
                double dist = Math.hypot(cone.x - v.getX(), cone.y - v.getY());
                if (dist < 45) {
                    obstacleAhead = true;
                    v.stopVehicle();
                    v.forceHonk();
                    break;
                }
            }

            // D. Check Leading Vehicle Ahead
            Vehicle leading = findLeadingVehicle(v);
            if (leading != null && !obstacleAhead) {
                double distance = calculateDistance(v, leading);
                double safeGap = 20.0;

                if (distance < safeGap + 35) {
                    obstacleAhead = true;
                    if (distance < safeGap) {
                        v.stopVehicle();
                    } else {
                        v.adjustSpeed(leading.getSpeed() * 0.85);
                    }
                }
            }

            // E. Pedestrian Safety Stop
            if (pedestrianCrossingActive && !obstacleAhead && !v.hasCrossedIntersection()) {
                double distToCross = getDistanceToStopLine(v, stopLineNorth, stopLineSouth, stopLineWest, stopLineEast);
                if (distToCross > -5 && distToCross < 75) {
                    v.stopVehicle();
                    obstacleAhead = true;
                }
            }

            // F. Traffic Light Stop Logic
            if (!obstacleAhead && !v.hasCrossedIntersection()) {
                boolean mustStop = false;

                // VIP Minister, Ambulances, and Rash Violators ignore red light!
                boolean canIgnoreLight = (v.getPersonality() == DriverPersonality.AMBULANCE
                        || v.getPersonality() == DriverPersonality.VIP_MINISTER
                        || v.isJumpingRedLight());

                if (!canIgnoreLight) {
                    switch (v.getDirection()) {
                        case SOUTH:
                            if (nsColor != SignalColor.GREEN) {
                                double d = stopLineNorth - v.getFrontY();
                                if (d > -5 && d < 75) mustStop = true;
                            }
                            break;
                        case NORTH:
                            if (nsColor != SignalColor.GREEN) {
                                double d = v.getFrontY() - stopLineSouth;
                                if (d > -5 && d < 75) mustStop = true;
                            }
                            break;
                        case EAST:
                            if (ewColor != SignalColor.GREEN) {
                                double d = stopLineWest - v.getFrontX();
                                if (d > -5 && d < 75) mustStop = true;
                            }
                            break;
                        case WEST:
                            if (ewColor != SignalColor.GREEN) {
                                double d = v.getFrontX() - stopLineEast;
                                if (d > -5 && d < 75) mustStop = true;
                            }
                            break;
                    }
                }

                if (mustStop) v.stopVehicle();
                else v.resumeVehicle();
            } else if (!obstacleAhead) {
                v.resumeVehicle();
            }

            // G. Queue & Eco-Emission Telemetry (Cars idling at 0 speed)
            if (v.getSpeed() < 0.3 && !v.hasCrossedIntersection()) {
                if (v.getDirection() == Direction.NORTH || v.getDirection() == Direction.SOUTH) nsWaiting++;
                else ewWaiting++;

                // Idle engine wastes ~0.25 ml fuel & 0.6g CO2 per second
                totalFuelWastedMl += (0.25 / 60.0);
                totalCo2EmittedGrams += (0.6 / 60.0);
            }

            v.updatePhysics(simSpeedMultiplier);
        }

        // ========================================================
        // INTERSECTION COLLISION DETECTION (T-BONE ACCIDENT CHECK)
        // ========================================================
        for (int i = 0; i < vehicles.size(); i++) {
            Vehicle v1 = vehicles.get(i);
            if (v1.isCrashed()) continue;

            for (int j = i + 1; j < vehicles.size(); j++) {
                Vehicle v2 = vehicles.get(j);
                if (v2.isCrashed()) continue;

                boolean isV1NS = (v1.getDirection() == Direction.NORTH || v1.getDirection() == Direction.SOUTH);
                boolean isV2NS = (v2.getDirection() == Direction.NORTH || v2.getDirection() == Direction.SOUTH);

                // Perpendicular collision inside intersection box
                if (isV1NS != isV2NS) {
                    if (Math.abs(v1.getX() - cx) < roadWidth / 2 && Math.abs(v1.getY() - cy) < roadWidth / 2
                            && Math.abs(v2.getX() - cx) < roadWidth / 2 && Math.abs(v2.getY() - cy) < roadWidth / 2) {

                        double dist = Math.hypot(v1.getX() - v2.getX(), v1.getY() - v2.getY());
                        if (dist < 32.0) {
                            triggerCollisionImpact(v1, v2);
                        }
                    }
                }
            }
        }

        // Clean out of bounds
        vehicles.removeIf(v -> v.getX() < -80 || v.getX() > getWidth() + 80 || v.getY() < -80 || v.getY() > getHeight() + 80);

        // Update Controller with real-time cleared vehicle rewards for Q-Learning
        signalController.update(nsWaiting, ewWaiting, emergencyActive, false, clearedThisTick);
    }

    private void issueEChallan(Vehicle v) {
        v.setHasBeenChallaned(true);
        totalChallansIssued++;
        int fine = 2000;
        totalChallanFineAmount += fine;

        // Sound alert!
        try {
            Toolkit.getDefaultToolkit().beep();
        } catch (Exception ignored) {}

        // Trigger intense camera strobe flash!
        cameraFlashAlpha = 0.95f;

        lastChallanText = String.format("📸 E-CHALLAN ISSUED: [%s] ₹%d Fine for Jumping Red Light!", v.getPlateNumber(), fine);
        challanBannerTimer = 200; // ~3.5 seconds visible
    }

    private double getDistanceToStopLine(Vehicle v, int n, int s, int w, int e) {
        switch (v.getDirection()) {
            case SOUTH: return n - v.getFrontY();
            case NORTH: return v.getFrontY() - s;
            case EAST:  return w - v.getFrontX();
            case WEST:  return v.getFrontX() - e;
        }
        return 999.0;
    }

    private Vehicle findLeadingVehicle(Vehicle current) {
        Vehicle closest = null;
        double minDistance = Double.MAX_VALUE;

        for (Vehicle other : vehicles) {
            if (other == current || other.getDirection() != current.getDirection()) continue;

            double dist = -1;
            switch (current.getDirection()) {
                case SOUTH: if (other.getY() > current.getY()) dist = other.getRearY() - current.getFrontY(); break;
                case NORTH: if (other.getY() < current.getY()) dist = current.getFrontY() - other.getRearY(); break;
                case EAST:  if (other.getX() > current.getX()) dist = other.getRearX() - current.getFrontX(); break;
                case WEST:  if (other.getX() < current.getX()) dist = current.getFrontX() - other.getRearX(); break;
            }

            if (dist >= 0 && dist < minDistance) {
                minDistance = dist;
                closest = other;
            }
        }
        return closest;
    }

    private double calculateDistance(Vehicle follower, Vehicle leader) {
        switch (follower.getDirection()) {
            case SOUTH: return leader.getRearY() - follower.getFrontY();
            case NORTH: return follower.getFrontY() - leader.getRearY();
            case EAST:  return leader.getRearX() - follower.getFrontX();
            case WEST:  return follower.getFrontX() - leader.getRearX();
        }
        return 999.0;
    }

    public void triggerPedestrianCrossing() {
        int cx = getWidth() / 2;
        int cy = getHeight() / 2;
        if (cx == 0) { cx = 400; cy = 350; }

        pedestrians.clear();
        // Spawn 3 pedestrians to cross North & South zebra crossings
        pedestrians.add(new Pedestrian(cx - roadWidth / 2 - 15, cy - roadWidth / 2 - 12, cx + roadWidth / 2 + 15, cy - roadWidth / 2 - 12));
        pedestrians.add(new Pedestrian(cx + roadWidth / 2 + 15, cy + roadWidth / 2 + 12, cx - roadWidth / 2 - 15, cy + roadWidth / 2 + 12));
        pedestrians.add(new Pedestrian(cx - roadWidth / 2 - 15, cy + roadWidth / 2 + 12, cx + roadWidth / 2 + 15, cy + roadWidth / 2 + 12));

        for (Pedestrian p : pedestrians) p.isCrossing = true;
        signalController.update(0, 0, false, true, 0);
    }

    public void spawnVehicle(Direction dir, DriverPersonality personality) {
        int cx = getWidth() / 2;
        int cy = getHeight() / 2;
        if (cx == 0) { cx = 410; cy = 360; }

        double spawnX = 0, spawnY = 0;
        switch (dir) {
            case SOUTH: spawnX = cx - laneWidth / 2.0; spawnY = -40; break;
            case NORTH: spawnX = cx + laneWidth / 2.0; spawnY = getHeight() + 40; break;
            case EAST:  spawnX = -40; spawnY = cy + laneWidth / 2.0; break;
            case WEST:  spawnX = getWidth() + 40; spawnY = cy - laneWidth / 2.0; break;
        }

        for (Vehicle v : vehicles) {
            if (v.getDirection() == dir && Math.hypot(v.getX() - spawnX, v.getY() - spawnY) < 65) {
                return;
            }
        }

        vehicles.add(new Vehicle(dir, personality, spawnX, spawnY));
        totalSpawned++;
    }

    public void spawnRandomVehicle() {
        Direction[] dirs = Direction.values();
        Direction dir = dirs[new Random().nextInt(dirs.length)];

        // Random personality weighted distribution
        int roll = new Random().nextInt(100);
        DriverPersonality p;
        if (roll < 55) p = DriverPersonality.NORMAL;
        else if (roll < 75) p = DriverPersonality.LEARNER;
        else if (roll < 93) p = DriverPersonality.RASH;
        else p = DriverPersonality.VIP_MINISTER;

        spawnVehicle(dir, p);
    }

    public void spawnSurge(Direction dir, int count) {
        for (int i = 0; i < count; i++) {
            spawnVehicle(dir, DriverPersonality.NORMAL);
        }
    }

    /**
     * 100% Deterministic Instant Challan Demo:
     * - Locks North-South light to RED
     * - Clears any obstacle cars in that lane
     * - Spawns Rash Red Car right near the stop line at high speed
     * - Within 0.5s it crosses the red light, camera flashes & cuts challan!
     */
    public void spawnRashChallanDemo() {
        int cx = getWidth() / 2;
        int cy = getHeight() / 2;
        if (cx == 0) { cx = 410; cy = 360; }

        int stopLineNorth = cy - roadWidth / 2;

        // 1. Force North-South to RED
        signalController.forceDirection(false);

        // 2. Clear Southbound entrance lane so path is 100% free
        vehicles.removeIf(v -> v.getDirection() == Direction.SOUTH && v.getY() < stopLineNorth + 100);

        // 3. Spawn 80px before the stop line, full speed
        double spawnX = cx - laneWidth / 2.0;
        double spawnY = stopLineNorth - 80;

        Vehicle rashCar = new Vehicle(Direction.SOUTH, DriverPersonality.RASH, spawnX, spawnY);
        vehicles.add(rashCar);
        totalSpawned++;
    }

    public void clearAll() {
        vehicles.clear();
        roadCones.clear();
        pedestrians.clear();
        crashParticles.clear();
        isAccidentActive = false;
        accidentBannerTimer = 0;
        totalSpawned = 0;
        totalPassed = 0;
        totalChallansIssued = 0;
        totalChallanFineAmount = 0;
        totalAccidents = 0;
    }

    public void triggerCollisionImpact(Vehicle v1, Vehicle v2) {
        v1.crash(0.32);
        v2.crash(-0.32);
        isAccidentActive = true;
        totalAccidents++;

        double impactX = (v1.getX() + v2.getX()) / 2.0;
        double impactY = (v1.getY() + v2.getY()) / 2.0;

        // Explosion sparks & flame burst
        for (int k = 0; k < 45; k++) {
            Color c = (k % 3 == 0) ? Color.ORANGE : (k % 3 == 1 ? Color.RED : Color.YELLOW);
            crashParticles.add(new CrashParticle(impactX, impactY, c, 4.0, 35));
        }

        cameraFlashAlpha = 0.95f;
        try {
            Toolkit.getDefaultToolkit().beep();
        } catch (Exception ignored) {}

        accidentBannerText = String.format("💥 T-BONE COLLISION! [%s] crashed with [%s] | Intersection Blocked!",
                v1.getPlateNumber(), v2.getPlateNumber());
        accidentBannerTimer = 260; // ~4.5 seconds
    }

    public void triggerCrashDemo() {
        final int centerX = (getWidth() == 0) ? 410 : getWidth() / 2;
        final int centerY = (getHeight() == 0) ? 360 : getHeight() / 2;

        // 1. Force EW to GREEN (so NS is locked to RED)
        signalController.forceDirection(false);

        // 2. Clear intersection
        vehicles.removeIf(v -> Math.hypot(v.getX() - centerX, v.getY() - centerY) < 180);

        // 3. Victim car moving East on Green
        double car1X = centerX - 110;
        double car1Y = centerY + laneWidth / 2.0;
        Vehicle victimCar = new Vehicle(Direction.EAST, DriverPersonality.NORMAL, car1X, car1Y);

        // 4. Rash car moving South jumping Red light
        double car2X = centerX - laneWidth / 2.0;
        double car2Y = centerY - 110;
        Vehicle violatorCar = new Vehicle(Direction.SOUTH, DriverPersonality.RASH, car2X, car2Y);

        vehicles.add(victimCar);
        vehicles.add(violatorCar);
        totalSpawned += 2;
    }

    public void clearAccident() {
        vehicles.removeIf(Vehicle::isCrashed);
        isAccidentActive = false;
        accidentBannerTimer = 0;
        crashParticles.clear();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();
        int cx = w / 2;
        int cy = h / 2;

        // 1. Terrain Grass Background (Darker in Night Mode)
        if (isNightMode) {
            g2.setColor(new Color(15, 30, 20)); // Midnight grass
        } else {
            g2.setColor(new Color(46, 125, 50)); // Fresh green
        }
        g2.fillRect(0, 0, w, h);

        // 2. Concrete Sidewalks / Curbs
        g2.setColor(isNightMode ? new Color(60, 64, 67) : new Color(189, 195, 199));
        int curbOffset = 8;
        g2.fillRect(cx - roadWidth / 2 - curbOffset, 0, roadWidth + curbOffset * 2, h);
        g2.fillRect(0, cy - roadWidth / 2 - curbOffset, w, roadWidth + curbOffset * 2);

        // 3. Asphalt Roads
        g2.setColor(isNightMode ? new Color(25, 30, 36) : new Color(44, 62, 80));
        g2.fillRect(cx - roadWidth / 2, 0, roadWidth, h);
        g2.fillRect(0, cy - roadWidth / 2, w, roadWidth);

        // 4. White Zebra Crossings
        g2.setColor(new Color(236, 240, 241, isNightMode ? 140 : 220));
        drawZebraCrossing(g2, cx - roadWidth / 2, cy - roadWidth / 2 - 24, roadWidth, 20, true);
        drawZebraCrossing(g2, cx - roadWidth / 2, cy + roadWidth / 2 + 4, roadWidth, 20, true);
        drawZebraCrossing(g2, cx - roadWidth / 2 - 24, cy - roadWidth / 2, 20, roadWidth, false);
        drawZebraCrossing(g2, cx + roadWidth / 2 + 4, cy - roadWidth / 2, 20, roadWidth, false);

        // 5. Solid White Stop Lines
        g2.setColor(Color.WHITE);
        g2.setStroke(new BasicStroke(3));
        g2.drawLine(cx - roadWidth / 2, cy - roadWidth / 2, cx, cy - roadWidth / 2);
        g2.drawLine(cx, cy + roadWidth / 2, cx + roadWidth / 2, cy + roadWidth / 2);
        g2.drawLine(cx - roadWidth / 2, cy, cx - roadWidth / 2, cy + roadWidth / 2);
        g2.drawLine(cx + roadWidth / 2, cy - roadWidth / 2, cx + roadWidth / 2, cy);

        // 6. Yellow Dashed Centerlines
        g2.setColor(new Color(241, 196, 15));
        float[] dash = {14, 14};
        g2.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, dash, 0.0f));
        g2.drawLine(cx, 0, cx, cy - roadWidth / 2 - 25);
        g2.drawLine(cx, cy + roadWidth / 2 + 25, cx, h);
        g2.drawLine(0, cy, cx - roadWidth / 2 - 25, cy);
        g2.drawLine(cx + roadWidth / 2 + 25, cy, w, cy);

        // 7. SPEED CAMERA POLE WITH SENSORS 📸
        drawSpeedCamera(g2, cx + roadWidth / 2 + 15, cy - roadWidth / 2 - 45);

        // 8. Traffic Signals at Corners
        drawSignalPole(g2, cx - roadWidth / 2 - 20, cy - roadWidth / 2 - 45, signalController.getNorthSouthColor(), "N");
        drawSignalPole(g2, cx + roadWidth / 2 + 8, cy + roadWidth / 2 + 10, signalController.getNorthSouthColor(), "S");
        drawSignalPole(g2, cx - roadWidth / 2 - 45, cy + roadWidth / 2 + 8, signalController.getEastWestColor(), "W");
        drawSignalPole(g2, cx + roadWidth / 2 + 10, cy - roadWidth / 2 - 25, signalController.getEastWestColor(), "E");

        // 9. Interactive Roadblock Cones (Mouse Placed)
        for (RoadCone cone : roadCones) {
            cone.draw(g2);
        }

        // 10. Render Pedestrians
        for (Pedestrian p : pedestrians) {
            p.draw(g2, tickCounter);
        }

        // 11. Render Vehicles
        for (Vehicle v : vehicles) {
            v.draw(g2, tickCounter, isNightMode);
        }

        // Render Crash & Explosion Sparks
        for (CrashParticle cp : crashParticles) {
            cp.draw(g2);
        }

        // 12. CAMERA FLASH STROBE EFFECT (White burst on E-Challan)
        if (cameraFlashAlpha > 0.0f) {
            g2.setColor(new Color(1.0f, 1.0f, 1.0f, cameraFlashAlpha));
            g2.fillRect(0, 0, w, h);
        }

        // 13. E-CHALLAN NOTIFICATION BANNER
        if (challanBannerTimer > 0 && lastChallanText != null) {
            g2.setColor(new Color(192, 57, 43, 230));
            g2.fillRoundRect(w / 2 - 240, 15, 480, 36, 10, 10);
            g2.setColor(Color.WHITE);
            g2.setStroke(new BasicStroke(2));
            g2.drawRoundRect(w / 2 - 240, 15, 480, 36, 10, 10);
            g2.setFont(new Font("Segoe UI", Font.BOLD, 13));
            g2.drawString(lastChallanText, w / 2 - 225, 38);
        }

        // 13.5. ACCIDENT IMPACT ALERT BANNER
        if (accidentBannerTimer > 0 && accidentBannerText != null) {
            g2.setColor(new Color(192, 57, 43, 245));
            g2.fillRoundRect(w / 2 - 270, 56, 540, 36, 10, 10);
            g2.setColor(Color.WHITE);
            g2.setStroke(new BasicStroke(2));
            g2.drawRoundRect(w / 2 - 270, 56, 540, 36, 10, 10);
            g2.setFont(new Font("Segoe UI", Font.BOLD, 12));
            g2.drawString(accidentBannerText, w / 2 - 255, 78);
        }

        // 14. EMERGENCY GREEN CORRIDOR BANNER
        if (signalController.isGreenCorridorActive()) {
            g2.setColor(new Color(39, 174, 96, 220));
            g2.fillRoundRect(w / 2 - 220, 58, 440, 32, 8, 8);
            g2.setColor(Color.WHITE);
            g2.drawString("🚨 EMERGENCY GREEN CORRIDOR ACTIVE - ALL TRAFFIC HALTED", w / 2 - 205, 79);
        }

        // 15. On-Screen Mini Instructions Overlay
        g2.setColor(new Color(20, 20, 20, 190));
        g2.fillRoundRect(15, h - 80, 280, 65, 8, 8);
        g2.setColor(new Color(241, 196, 15));
        g2.setFont(new Font("Segoe UI", Font.BOLD, 11));
        g2.drawString("💡 INTERACTIVE CONTROLS:", 25, h - 62);
        g2.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        g2.setColor(Color.WHITE);
        g2.drawString("• Left-Click on road: Place Barricade 🚧", 25, h - 45);
        g2.drawString("• Click existing cone to remove it", 25, h - 28);
    }

    private void drawSpeedCamera(Graphics2D g, int x, int y) {
        // Camera Pole & Mount
        g.setColor(new Color(40, 40, 40));
        g.fillRect(x + 6, y + 10, 5, 25);
        // Camera Box
        g.setColor(new Color(220, 220, 220));
        g.fillRoundRect(x, y, 18, 14, 4, 4);
        g.setColor(new Color(50, 50, 50));
        g.drawRoundRect(x, y, 18, 14, 4, 4);
        // Lens
        g.setColor(new Color(41, 128, 185));
        g.fillOval(x + 4, y + 3, 7, 7);
        // Flash bulb
        g.setColor(Color.YELLOW);
        g.fillOval(x + 12, y + 3, 3, 3);
    }

    private void drawZebraCrossing(Graphics2D g, int x, int y, int width, int height, boolean isHorizontal) {
        int stripes = 8;
        if (isHorizontal) {
            int stripeW = width / stripes;
            for (int i = 0; i < stripes; i += 2) g.fillRect(x + i * stripeW, y, stripeW, height);
        } else {
            int stripeH = height / stripes;
            for (int i = 0; i < stripes; i += 2) g.fillRect(x, y + i * stripeH, width, stripeH);
        }
    }

    private void drawSignalPole(Graphics2D g, int x, int y, SignalColor activeColor, String label) {
        g.setColor(new Color(20, 20, 20));
        g.fillRoundRect(x, y, 16, 36, 4, 4);
        g.setColor(new Color(70, 70, 70));
        g.drawRoundRect(x, y, 16, 36, 4, 4);

        int bulbR = 8;
        // RED
        if (activeColor == SignalColor.RED) {
            g.setColor(new Color(255, 40, 40));
            g.fillOval(x + 4, y + 4, bulbR, bulbR);
            g.setColor(new Color(255, 0, 0, 120));
            g.drawOval(x + 2, y + 2, bulbR + 4, bulbR + 4);
        } else {
            g.setColor(new Color(80, 20, 20));
            g.fillOval(x + 4, y + 4, bulbR, bulbR);
        }

        // YELLOW
        if (activeColor == SignalColor.YELLOW) {
            g.setColor(new Color(255, 220, 0));
            g.fillOval(x + 4, y + 14, bulbR, bulbR);
            g.setColor(new Color(255, 220, 0, 120));
            g.drawOval(x + 2, y + 12, bulbR + 4, bulbR + 4);
        } else {
            g.setColor(new Color(80, 70, 10));
            g.fillOval(x + 4, y + 14, bulbR, bulbR);
        }

        // GREEN
        if (activeColor == SignalColor.GREEN) {
            g.setColor(new Color(46, 204, 113));
            g.fillOval(x + 4, y + 24, bulbR, bulbR);
            g.setColor(new Color(46, 204, 113, 120));
            g.drawOval(x + 2, y + 22, bulbR + 4, bulbR + 4);
        } else {
            g.setColor(new Color(15, 60, 30));
            g.fillOval(x + 4, y + 24, bulbR, bulbR);
        }
    }

    public SignalController getSignalController() { return signalController; }
    public int getTotalSpawned() { return totalSpawned; }
    public int getTotalPassed() { return totalPassed; }
    public int getTotalChallans() { return totalChallansIssued; }
    public int getTotalFineRevenue() { return totalChallanFineAmount; }
    public int getTotalAccidents() { return totalAccidents; }
    public double getTotalCo2() { return totalCo2EmittedGrams; }
    public double getTotalFuel() { return totalFuelWastedMl; }
    public int getQueueCount() {
        int count = 0;
        for (Vehicle v : vehicles) if (v.getSpeed() < 0.3 && !v.hasCrossedIntersection()) count++;
        return count;
    }
    public void setSimSpeedMultiplier(double mult) { this.simSpeedMultiplier = mult; }
    public boolean isPaused() { return isPaused; }
    public void togglePause() { this.isPaused = !this.isPaused; }
    public boolean isNightMode() { return isNightMode; }
    public void toggleNightMode() { this.isNightMode = !this.isNightMode; }
    public boolean isAutoSpawnEnabled() { return isAutoSpawnEnabled; }
    public void setAutoSpawnEnabled(boolean enabled) { this.isAutoSpawnEnabled = enabled; }
}

// ==============================================================
// 7. MAIN DASHBOARD APPLICATION (Java Swing)
// ==============================================================
public class TrafficSimulator extends JFrame {
    private SimulationPanel simPanel;
    private javax.swing.Timer animationTimer;

    // Telemetry labels
    private JLabel lblTotalSpawned;
    private JLabel lblTotalPassed;
    private JLabel lblCurrentQueue;
    private JLabel lblChallans;
    private JLabel lblAccidents;
    private JLabel lblMLReward;
    private JLabel lblEcoStats;
    private JTextArea txtDecisionLog;
    private JCheckBox cbAutoSpawn;
    private JButton btnNightMode;
    private JButton btnPause;

    public TrafficSimulator() {
        setTitle("SmartFlow 2.0 - Futuristic Urban Traffic Simulation Engine");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1240, 820);
        setMinimumSize(new Dimension(1060, 720));
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        simPanel = new SimulationPanel();
        add(simPanel, BorderLayout.CENTER);

        JPanel controlPanel = createDashboardPanel();
        add(controlPanel, BorderLayout.EAST);

        JPanel headerPanel = createHeaderPanel();
        add(headerPanel, BorderLayout.NORTH);

        animationTimer = new javax.swing.Timer(16, e -> {
            simPanel.tick();
            simPanel.repaint();
            updateDashboardStats();
        });
        animationTimer.start();

        // Seed 4 initial vehicles
        for (int i = 0; i < 4; i++) simPanel.spawnRandomVehicle();
    }

    private JPanel createHeaderPanel() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(24, 32, 40));
        header.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

        JLabel title = new JLabel("🚦 SMARTFLOW 2.0: Autonomous Traffic & E-Challan Surveillance Engine");
        title.setFont(new Font("Segoe UI", Font.BOLD, 17));
        title.setForeground(Color.WHITE);

        JLabel sub = new JLabel("Reinforcement Learning (Q-Learning) • Bellman Optimization • Speed Camera • Night Mode");
        sub.setFont(new Font("Segoe UI", Font.BOLD, 12));
        sub.setForeground(new Color(46, 204, 113));

        header.add(title, BorderLayout.WEST);
        header.add(sub, BorderLayout.EAST);
        return header;
    }

    private JPanel createDashboardPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setPreferredSize(new Dimension(370, 750));
        panel.setBackground(new Color(30, 39, 46));
        panel.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));

        // Section 1: Vehicle & Driver Injection
        panel.add(createSectionTitle("🚗 MULTI-AGENT INJECTION"));
        panel.add(Box.createRigidArea(new Dimension(0, 6)));

        cbAutoSpawn = new JCheckBox("Continuous Auto-Traffic Dispatcher", true);
        cbAutoSpawn.setOpaque(false);
        cbAutoSpawn.setForeground(new Color(46, 204, 113));
        cbAutoSpawn.setFont(new Font("Segoe UI", Font.BOLD, 11));
        cbAutoSpawn.addActionListener(e -> simPanel.setAutoSpawnEnabled(cbAutoSpawn.isSelected()));
        panel.add(cbAutoSpawn);
        panel.add(Box.createRigidArea(new Dimension(0, 6)));

        // Vehicle personality buttons grid
        JPanel spawnGrid = new JPanel(new GridLayout(2, 2, 4, 4));
        spawnGrid.setOpaque(false);
        spawnGrid.setMaximumSize(new Dimension(340, 64));

        JButton btnNormal = createStyledButton("➕ Random Car", new Color(41, 128, 185));
        btnNormal.addActionListener(e -> simPanel.spawnRandomVehicle());

        JButton btnRash = createStyledButton("⚡ Rash Driver [Jump Light]", new Color(214, 48, 49));
        btnRash.addActionListener(e -> simPanel.spawnRashChallanDemo());

        JButton btnVIP = createStyledButton("🚨 VIP Minister Convoy", new Color(44, 62, 80));
        btnVIP.addActionListener(e -> {
            Direction[] dirs = Direction.values();
            simPanel.spawnVehicle(dirs[new Random().nextInt(dirs.length)], DriverPersonality.VIP_MINISTER);
        });

        JButton btnAmbulance = createStyledButton("🚑 Green Corridor Ambulance", new Color(192, 57, 43));
        btnAmbulance.addActionListener(e -> {
            Direction[] dirs = Direction.values();
            simPanel.spawnVehicle(dirs[new Random().nextInt(dirs.length)], DriverPersonality.AMBULANCE);
        });

        spawnGrid.add(btnNormal);
        spawnGrid.add(btnRash);
        spawnGrid.add(btnVIP);
        spawnGrid.add(btnAmbulance);
        panel.add(spawnGrid);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));

        // Section 2: Special Event Triggers
        panel.add(createSectionTitle("⚡ URBAN EVENT TRIGGERS"));
        panel.add(Box.createRigidArea(new Dimension(0, 6)));

        JPanel eventGrid = new JPanel(new GridLayout(2, 2, 4, 4));
        eventGrid.setOpaque(false);
        eventGrid.setMaximumSize(new Dimension(340, 64));

        JButton btnCrash = createStyledButton("💥 T-Bone Crash Demo", new Color(192, 57, 43));
        btnCrash.addActionListener(e -> simPanel.triggerCrashDemo());

        JButton btnTow = createStyledButton("🚜 Clear Crash (Tow)", new Color(39, 174, 96));
        btnTow.addActionListener(e -> simPanel.clearAccident());

        JButton btnPedestrian = createStyledButton("🚶 Zebra Walk Request", new Color(142, 68, 173));
        btnPedestrian.addActionListener(e -> simPanel.triggerPedestrianCrossing());

        btnNightMode = createStyledButton("🌙 Toggle Night Mode", new Color(52, 73, 94));
        btnNightMode.addActionListener(e -> {
            simPanel.toggleNightMode();
            btnNightMode.setText(simPanel.isNightMode() ? "☀️ Toggle Day Mode" : "🌙 Toggle Night Mode");
        });

        eventGrid.add(btnCrash);
        eventGrid.add(btnTow);
        eventGrid.add(btnPedestrian);
        eventGrid.add(btnNightMode);
        panel.add(eventGrid);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));

        // Section 3: Signal Control & Machine Learning Modes
        panel.add(createSectionTitle("🤖 AI & SIGNAL INTELLIGENCE"));
        panel.add(Box.createRigidArea(new Dimension(0, 4)));

        JRadioButton rbRL = new JRadioButton("🧠 Q-Learning Agent (Reinforcement Learning)", true);
        JRadioButton rbAdaptive = new JRadioButton("⚡ Smart Adaptive Mode (Heuristic Rules)", false);
        JRadioButton rbFixed = new JRadioButton("⏱️ Fixed Timer Mode (Classic Non-AI)", false);
        ButtonGroup bg = new ButtonGroup();
        bg.add(rbRL); bg.add(rbAdaptive); bg.add(rbFixed);
        configureRadio(rbRL); configureRadio(rbAdaptive); configureRadio(rbFixed);

        rbRL.addActionListener(e -> simPanel.getSignalController().setMode(ControlMode.RL_Q_LEARNING));
        rbAdaptive.addActionListener(e -> simPanel.getSignalController().setMode(ControlMode.HEURISTIC_ADAPTIVE));
        rbFixed.addActionListener(e -> simPanel.getSignalController().setMode(ControlMode.FIXED_TIMER));

        panel.add(rbRL);
        panel.add(rbAdaptive);
        panel.add(rbFixed);
        panel.add(Box.createRigidArea(new Dimension(0, 6)));

        JButton btnManualSwitch = createStyledButton("⚡ Manual Switch Light", new Color(230, 126, 34));
        btnManualSwitch.addActionListener(e -> simPanel.getSignalController().manualSwitch());
        panel.add(btnManualSwitch);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));

        // Section 4: Live Surveillance & Telemetry Box
        panel.add(createSectionTitle("📸 LIVE SURVEILLANCE & ML TELEMETRY"));
        panel.add(Box.createRigidArea(new Dimension(0, 6)));

        JPanel telemetryBox = new JPanel(new GridLayout(7, 1, 3, 3));
        telemetryBox.setBackground(new Color(20, 26, 31));
        telemetryBox.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
        telemetryBox.setMaximumSize(new Dimension(340, 160));

        lblTotalSpawned = createStatLabel("Total Spawned: 0");
        lblTotalPassed = createStatLabel("Cleared Intersection: 0");
        lblCurrentQueue = createStatLabel("Current Waiting Queue: 0");
        lblChallans = createStatLabel("📸 Challans: 0 (Revenue: ₹0)");
        lblChallans.setForeground(new Color(241, 196, 15));
        lblAccidents = createStatLabel("💥 Accidents: 0 (Normal Flow)");
        lblAccidents.setForeground(new Color(231, 76, 60));
        lblMLReward = createStatLabel("🧠 RL Reward: +0.0 (Bellman Model)");
        lblMLReward.setForeground(new Color(52, 152, 219));
        lblEcoStats = createStatLabel("🌱 Idle CO2: 0.0g | Fuel: 0.0ml");
        lblEcoStats.setForeground(new Color(46, 204, 113));

        telemetryBox.add(lblTotalSpawned);
        telemetryBox.add(lblTotalPassed);
        telemetryBox.add(lblCurrentQueue);
        telemetryBox.add(lblChallans);
        telemetryBox.add(lblAccidents);
        telemetryBox.add(lblMLReward);
        telemetryBox.add(lblEcoStats);
        panel.add(telemetryBox);
        panel.add(Box.createRigidArea(new Dimension(0, 8)));

        // Decision Log
        txtDecisionLog = new JTextArea(2, 20);
        txtDecisionLog.setEditable(false);
        txtDecisionLog.setLineWrap(true);
        txtDecisionLog.setWrapStyleWord(true);
        txtDecisionLog.setBackground(new Color(20, 26, 31));
        txtDecisionLog.setForeground(new Color(46, 204, 113));
        txtDecisionLog.setFont(new Font("Consolas", Font.PLAIN, 11));
        txtDecisionLog.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));
        JScrollPane scrollLog = new JScrollPane(txtDecisionLog);
        scrollLog.setMaximumSize(new Dimension(340, 48));
        panel.add(scrollLog);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));

        // Section 5: Engine Speed & Pause
        panel.add(createSectionTitle("⚙️ SIMULATION ENGINE"));
        panel.add(Box.createRigidArea(new Dimension(0, 6)));

        JSlider speedSlider = new JSlider(5, 30, 10);
        speedSlider.setOpaque(false);
        speedSlider.setForeground(Color.WHITE);
        speedSlider.addChangeListener(e -> simPanel.setSimSpeedMultiplier(speedSlider.getValue() / 10.0));
        panel.add(speedSlider);
        panel.add(Box.createRigidArea(new Dimension(0, 6)));

        JPanel simBtnRow = new JPanel(new GridLayout(1, 2, 6, 0));
        simBtnRow.setOpaque(false);
        simBtnRow.setMaximumSize(new Dimension(340, 32));

        btnPause = createStyledButton("⏸️ Pause", new Color(142, 68, 173));
        btnPause.addActionListener(e -> {
            simPanel.togglePause();
            btnPause.setText(simPanel.isPaused() ? "▶️ Resume" : "⏸️ Pause");
        });

        JButton btnClear = createStyledButton("🗑️ Clear Canvas", new Color(192, 57, 43));
        btnClear.addActionListener(e -> simPanel.clearAll());

        simBtnRow.add(btnPause);
        simBtnRow.add(btnClear);
        panel.add(simBtnRow);

        panel.add(Box.createVerticalGlue());
        return panel;
    }

    private void updateDashboardStats() {
        lblTotalSpawned.setText("Total Spawned: " + simPanel.getTotalSpawned());
        lblTotalPassed.setText("Cleared Intersection: " + simPanel.getTotalPassed());

        int queue = simPanel.getQueueCount();
        lblCurrentQueue.setText("Waiting Queue: " + queue + (queue > 4 ? " (Congested!)" : " (Fluid)"));
        lblCurrentQueue.setForeground(queue > 4 ? new Color(231, 76, 60) : Color.WHITE);

        lblChallans.setText(String.format("📸 Challans: %d (Revenue: ₹%,d)", simPanel.getTotalChallans(), simPanel.getTotalFineRevenue()));

        int acc = simPanel.getTotalAccidents();
        lblAccidents.setText("💥 Accidents: " + acc + (acc > 0 ? " (Tow Dispatched!)" : " (Safe)"));
        lblAccidents.setForeground(acc > 0 ? new Color(231, 76, 60) : Color.WHITE);

        QLearningAgent agent = simPanel.getSignalController().getRlAgent();
        lblMLReward.setText(String.format("🧠 RL Reward: %+.1f (ε=%.2f | Dec: %d)",
                agent.getCumulativeReward(), agent.getEpsilon(), agent.getTotalDecisions()));

        lblEcoStats.setText(String.format("🌱 Idle CO2: %.1fg | Fuel: %.1fml", simPanel.getTotalCo2(), simPanel.getTotalFuel()));

        txtDecisionLog.setText(simPanel.getSignalController().getDecisionLog());
    }

    private JLabel createSectionTitle(String title) {
        JLabel lbl = new JLabel(title);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lbl.setForeground(new Color(241, 196, 15));
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        return lbl;
    }

    private JLabel createStatLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lbl.setForeground(Color.WHITE);
        return lbl;
    }

    private JButton createStyledButton(String text, Color bg) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 11));
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
        btn.setMaximumSize(new Dimension(340, 32));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setAlignmentX(Component.LEFT_ALIGNMENT);
        return btn;
    }

    private void configureRadio(JRadioButton rb) {
        rb.setOpaque(false);
        rb.setForeground(Color.WHITE);
        rb.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        rb.setFocusPainted(false);
        rb.setAlignmentX(Component.LEFT_ALIGNMENT);
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        SwingUtilities.invokeLater(() -> {
            TrafficSimulator app = new TrafficSimulator();
            app.setVisible(true);
        });
    }
}
