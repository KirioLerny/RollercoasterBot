# 🎢 Rollercoaster Discord Bot

The **Rollercoaster Bot** is a fun and lightweight Discord bot that lets authorized users send others on a chaotic "ride" through a sequence of temporary voice channels — just like a rollercoaster!

Perfect for small communities or private servers where fun meets moderation control.

---

## ✨ Features

- Slash command support: `/rollercoaster` and `/rc`
- Moves a target user through several voice channels in sequence
- Automatically cleans up all created channels after the ride
- Access controlled via:
  - `ADMINISTRATOR` permission
  - Custom admin role (`Rollercoaster Admin`)
  - Hardcoded user IDs (should be removed if you going to host it youself)

---

## 🛠️ Usage

### Slash Command

`/rollercoaster target:@username` <br>
`/rc target:@username`

You must have the appropriate permissions or role to use the command.

- The user must be connected to a voice channel
- Only one ride per user at a time
- Command creates 5 temporary channels and cycles the user through them
- Automatically moves the user back to the original voice channel after the ride

---

## 🔐 Permissions

To function properly, the bot needs the following permissions:

- `Manage Channels`
- `Move Members`
- `Connect`
- `View Channels`

These are required to create/delete channels and move users between them.

---

## 📎 Invite the Bot

Use the link below to invite the bot to your server:

[Invite the Rollercoaster Bot to your Discord server!](https://kirio.dev/rollercoaster-bot)

---

## 🧩 Dependencies

- Java 17+
- [JDA (Java Discord API)](https://github.com/DV8FromTheWorld/JDA)

---

## 📄 License

MIT License – use it, fork it, improve it, and have fun 🚀

---

## 💡 Disclaimer

This bot is designed for small-scale community fun. Use responsibly and make sure you have consent when initiating rides 😄

Made by Kirio with ♡
