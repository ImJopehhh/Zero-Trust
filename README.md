# Zero Trust 🛡️

> **"Absolute Authority. Zero Apologies."**

**Zero Trust** is a high-performance runtime security plugin for Minecraft. It does not just monitor your server; it **dominates** it. Designed to crush internal threats, rogue admins, and backdoor exploits, Zero Trust ensures that the only ones in control are the ones you chose.

---

## ⚡ Key Features

### 👮 Permission Guard

> *"Power belongs only to the chosen."*

A ruthless background task that enforces your authority.

* **Logic:** Continuously scans for players with `OP` or `*` permission.
* **Enforcement:** If a player is not on the `trusted-admins` list, they are stripped of power, kicked, and logged instantly.
* **Impact:** Stolen accounts and backdoors become powerless.

### 🚨 Self-Defense (Dead Man's Switch)

> *"If I fall, the world will know."*

Zero Trust is rigged with a digital tripwire. It knows when it is being murdered.

* **Logic:** Detects forced disabling via malicious plugins or console sabotage while the server is running.
* **Reaction:** Triggers an immediate **Discord Webhook Alert** to your private channel.
* **Impact:** No sabotage goes unnoticed. Silence is an alarm.

### 🧱 Command Firewall

> *"Your voice has no power here."*

A strict filter that renders unauthorized commands useless.

* **Logic:** Intercepts sensitive commands (`/stop`, `/op`, `/reload`, `/plugman`) before they execute.
* **Enforcement:** Blocks any attempt from non-trusted personnel, preventing accidental or malicious server shutdowns.
* **Impact:** Your server infrastructure is locked behind a steel door.

### 🛡️ Exploit Mitigation

> *"Chaos is denied entry."*

Keeps the server stable against brute-force destruction.

* **Logic:** Monitors block-breaking speeds and invalid packet data.
* **Enforcement:** Patches `Book Ban` crashes and `Nuker` exploits in real-time.
* **Impact:** Unbreakable stability against griefers.

---

## 📥 Installation

1. Download `ZeroTrust.jar`.
2. Drop it into `/plugins`.
3. **Restart** the server.
4. **CRITICAL:** Open `config.yml` and add yourself to `trusted-admins`.
5. Set your `discord-webhook-url`.
6. Run `/zt reload`.

---

## ⚠️ Disclaimer

**Zero Trust** is not a suggestion; it is a rule.

* **Do not** remove yourself from the `trusted-admins` list while holding OP, or the plugin will turn against you.
* Keep your Webhook URL strictly confidential.
