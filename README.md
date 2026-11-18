# Emberbound Guild Master

Emberbound Guild Master adds a local contract and favour ledger to RuneLite. Open the sidebar to request a slate, accept a contract, and track progress from Adventurer to Rank S. Use the Freelance Board for repeatable work. Contract and rank data are stored per RuneLite account profile.

## Beta candidate

- Issued and freelance skilling, supported boss encounters, clues, Slayer, and Guild Coffer gathering.
- Achievement Diary tiers for all twelve regions, supported Combat Achievements and Clogging projects, and eligible in-progress quests.
- Promotion trials, refusal and demotion, a completion scroll, and fanfare.
- Wilderness content follows the ordinary rank and access rules.

Open the normal bank before accepting a Coffer contract. The last existing numbered tab is the coffer, and credit is based on the required increase in both that tab and the whole bank. For a project tied to a Collection Log page, opening the page lets the plugin establish whether the slot is already filled.

## Development

Use JDK 11 and run `gradlew build`. The Gradle `run` task starts the RuneLite development launcher. The Windows batch launchers are local development shortcuts. The Fetch, completion, demotion, and promotion test launchers use separate ledgers.
