# 🚦 SmartFlow 2.0: Autonomous Urban Traffic Control & Surveillance Engine

[![Java](https://img.shields.io/badge/Language-Java%208%2B-orange.svg)](https://www.java.com/)
[![Machine Learning](https://img.shields.io/badge/Algorithm-Q--Learning%20(Reinforcement%20Learning)-blue.svg)]()
[![Rendering](https://img.shields.io/badge/Graphics-Java%20Swing%20%26%20AWT%20(60%20FPS)-green.svg)]()
[![Build](https://img.shields.io/badge/Dependencies-Zero%20(100%25%20Pure%20Java)-brightgreen.svg)]()
[![License](https://img.shields.io/badge/License-MIT-purple.svg)]()

> **SmartFlow 2.0** is an advanced, autonomous intersection management and traffic surveillance simulator built completely from scratch in Core Java. It features an online **Reinforcement Learning (Q-Learning) Agent** that optimizes signal phases using the **Bellman Optimality Equation**, integrated with real-time speed camera E-Challan enforcement, kinetic collision mechanics, multi-agent driver personalities, and environmental telemetry.

---

## 📸 Key Features

- **🧠 Reinforcement Learning (Q-Learning) Engine:** Implements tabular Q-learning with state discretization across 18 states, $\epsilon$-greedy exploration policy, and real-time Bellman equation weight updates.
- **📸 Speed Camera & Automatic E-Challan:** Optical sensor detects red-light jump infractions, triggers an intense white camera flash, issues authentic Indian license plate citations (e.g. `DL-04-AB-1290`), and tracks municipal revenue.
- **💥 T-Bone Collision & Kinetic Crash Dynamics:** Real-time spatial conflict detection modeling orthogonal vehicular crashes with 45 flame/smoke particles and tow-truck clearance.
- **🚗 Multi-Agent Driver Behavioral Profiles:**
  - `LEARNER`: Roof-mounted `[L]` badge, capped velocity, delayed green reactions.
  - `RASH`: Red sports car, tailgates close, impatient honking (`🔊 BEEP!!`), jumps red lights.
  - `VIP CONVOY`: Pitch-black tinted SUV with flashing red/blue beacons; absolute right-of-way.
  - `AMBULANCE`: Triggers intersection-wide **Emergency Green Corridor** (freezes civilian traffic).
  - `NORMAL`: Standard compliance with following distances and smooth deceleration.
- **🌙 Dynamic Night Mode:** Ray-cast headlight illumination cones projected onto dark asphalt using `GradientPaint` and coordinate transforms.
- **🚧 Interactive Roadblocks:** Left-click anywhere on the road canvas to deploy or remove traffic cones.
- **🚶 Pedestrian Zebra Walk:** Push-button pedestrian crosswalk request with animated foot traffic.
- **🌱 Eco-Emissions Telemetry:** Real-time accumulation of idle fuel wastage ($0.25\text{ mL/s}$) and $CO_2$ footprint ($0.60\text{ g/s}$).

---

## 📐 Mathematical Formulation: Tabular Q-Learning

The signal controller functions as an autonomous RL agent optimizing the Bellman Optimality Equation:

$$Q(s, a) \leftarrow Q(s, a) + \alpha \Big[ R + \gamma \max_{a'} Q(s', a') - Q(s, a) \Big]$$

### Parameters:
- **Learning Rate ($\alpha$):** `0.25`
- **Discount Factor ($\gamma$):** `0.85`
- **Exploration Rate ($\epsilon$):** `0.15` ($\epsilon$-Greedy Policy)
- **Reward Function ($R$):** $R = (8.0 \times \Delta\text{Cleared}) - (1.2 \times \text{Waiting Queue})$

---

## 🚀 How to Run

### Prerequisites
- Java Development Kit (JDK 8 or higher)
- No external libraries or Python required!

### Compilation & Execution
```bash
# Clone the repository
git clone https://github.com/<your-username>/smartflow-traffic-ai-simulation.git
cd smartflow-traffic-ai-simulation

# Compile the Java application
javac TrafficSimulator.java

# Run the simulation
java TrafficSimulator
```

---

## 📁 Project Structure

```text
smartflow-traffic-ai-simulation/
├── TrafficSimulator.java          # Complete source code (OOP Architecture & Swing Engine)
├── SmartFlow_Project_Report.pdf   # 4-page official submission & defense report
├── report.html                    # Print-ready HTML documentation
├── VIVA_GUIDE.md                  # Comprehensive Viva Voce defense Q&A
├── GeneratePDF.java               # Standalone binary PDF builder utility
└── README.md                      # GitHub documentation
```

---

## 👨‍🏫 Viva Voce Quick Cheatsheet

| Question | Short Answer |
| :--- | :--- |
| **Why Q-Learning over Supervised?** | Traffic lacks pre-labeled optimal actions; RL learns directly via dynamic reward feedback. |
| **Why is rendering smooth at 60 FPS?** | `javax.swing.Timer` at 16ms + Swing off-screen **Double Buffering** completely eliminates flicker. |
| **How does collision avoidance work?** | Scanning leading vehicles in lane; if gap $< 20\text{ px}$, follower velocity matches leader ($v \times 0.85$). |
| **What does Epsilon ($\epsilon = 0.15$) do?** | Balances 85% greedy exploitation ($\arg\max Q$) with 15% random exploration. |

---

## 📄 License
This project is licensed under the MIT License - feel free to use and modify for educational purposes.
