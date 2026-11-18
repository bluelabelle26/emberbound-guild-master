package com.emberbound;

import com.google.gson.Gson;
import com.google.inject.Inject;
import java.awt.BorderLayout;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.Locale;
import javax.swing.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.runelite.api.ChatMessageType;
import net.runelite.api.ItemContainer;
import net.runelite.api.Item;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.util.Text;
import net.runelite.api.Client;
import net.runelite.api.Experience;
import net.runelite.api.GameState;
import net.runelite.api.WorldType;
import net.runelite.api.Skill;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.StatChanged;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.input.KeyManager;
import net.runelite.client.input.MouseManager;
import net.runelite.client.game.SkillIconManager;
import net.runelite.client.audio.AudioPlayer;
import net.runelite.client.game.ItemVariationMapping;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.loottracker.LootReceived;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.PluginPanel;

@PluginDescriptor(name = "Emberbound Guild Master", description = "Local Covenant contracts")
public class GuildMasterPlugin extends Plugin
{
    private static final String GROUP = "emberboundGuildMaster";
    private static final List<String> RANKS = List.of("Adventurer", "D", "C", "B", "A", "S");
    private static final List<OneTimeObjective> DIARY_OBJECTIVES = diaryObjectives();
    private static final List<OneTimeObjective> ONE_TIME_OBJECTIVES = List.of(
        new OneTimeObjective("Bryophyta Novice", "D", "Open Bryophyta's keyed chest once.", VarbitID.CA_TASK_BRYOPHYTA_KILLCOUNT_1_COMPLETED),
        new OneTimeObjective("Bryophyta Champion", "C", "Open Bryophyta's keyed chest five times.", VarbitID.CA_TASK_BRYOPHYTA_KILLCOUNT_2_COMPLETED),
        new OneTimeObjective("Obor Novice", "D", "Open Obor's keyed chest once.", VarbitID.CA_TASK_HILLGIANT_BOSS_KILLCOUNT_1_COMPLETED),
        new OneTimeObjective("Obor Champion", "C", "Open Obor's keyed chest five times.", VarbitID.CA_TASK_HILLGIANT_BOSS_KILLCOUNT_2_COMPLETED),
        new OneTimeObjective("King Black Dragon Novice", "D", "Defeat the King Black Dragon 10 times.", VarbitID.CA_TASK_KBD_KILLCOUNT_1_COMPLETED),
        new OneTimeObjective("King Black Dragon Champion", "C", "Defeat the King Black Dragon 25 times.", VarbitID.CA_TASK_KBD_KILLCOUNT_2_COMPLETED),
        new OneTimeObjective("Giant Mole Novice", "D", "Defeat the Giant Mole 10 times.", VarbitID.CA_TASK_MOLE_KILLCOUNT_1_COMPLETED),
        new OneTimeObjective("Giant Mole Champion", "C", "Defeat the Giant Mole 25 times.", VarbitID.CA_TASK_MOLE_KILLCOUNT_2_COMPLETED),
        new OneTimeObjective("Barrows Novice", "D", "Open the Barrows chest 10 times.", VarbitID.CA_TASK_BARROWS_KILLCOUNT_1_COMPLETED),
        new OneTimeObjective("Barrows Champion", "C", "Open the Barrows chest 25 times.", VarbitID.CA_TASK_BARROWS_KILLCOUNT_2_COMPLETED),
        new OneTimeObjective("Sarachnis Novice", "D", "Defeat Sarachnis 10 times.", VarbitID.CA_TASK_SARACHNIS_KILLCOUNT_1_COMPLETED),
        new OneTimeObjective("Sarachnis Champion", "C", "Defeat Sarachnis 25 times.", VarbitID.CA_TASK_SARACHNIS_KILLCOUNT_2_COMPLETED),
        new OneTimeObjective("Chaos Elemental Adept", "B", "Defeat the Chaos Elemental 10 times.", VarbitID.CA_TASK_CHAOSELE_KILLCOUNT_1_COMPLETED),
        new OneTimeObjective("Chaos Elemental Veteran", "A", "Defeat the Chaos Elemental 25 times.", VarbitID.CA_TASK_CHAOSELE_KILLCOUNT_2_COMPLETED),
        new OneTimeObjective("Coal bag", "Adventurer", "Buy a coal bag from Prospector Percy.", "Motherlode Mine", net.runelite.api.ItemID.COAL_BAG),
        new OneTimeObjective("Gem bag", "D", "Buy a gem bag from Prospector Percy.", "Motherlode Mine", net.runelite.api.ItemID.GEM_BAG),
        new OneTimeObjective("Log basket", "D", "Buy a log basket from the Forestry Shop.", "Forestry", net.runelite.api.ItemID.LOG_BASKET),
        new OneTimeObjective("Seed box", "C", "Claim a seed box from Tithe Farm.", "Tithe Farm", net.runelite.api.ItemID.SEED_BOX),
        new OneTimeObjective("Herb sack", "C", "Claim a herb sack from Tithe Farm.", "Tithe Farm", net.runelite.api.ItemID.HERB_SACK),
        new OneTimeObjective("Soul bearer", "C", "Finish Bear Your Soul to receive the repaired soul bearer.", Quest.BEAR_YOUR_SOUL),
        new OneTimeObjective("Fighter torso", "B", "Claim a fighter torso from Barbarian Assault.", "Barbarian Assault", net.runelite.api.ItemID.FIGHTER_TORSO),
        new OneTimeObjective("Dragon defender", "B", "Obtain a dragon defender from a Warriors' Guild basement cyclops.", "Cyclopes", net.runelite.api.ItemID.DRAGON_DEFENDER),
        new OneTimeObjective("Colossal blade", "A", "Buy a colossal blade from the Giants' Foundry reward shop.", "Giants' Foundry", net.runelite.api.ItemID.COLOSSAL_BLADE));
    private static final java.util.Map<String, Integer> RANK_C_LOGS = java.util.Map.of(
        "Giant Mole", VarbitID.COLLECTION_BOSSES_MOLE_COMPLETED,
        "Sarachnis", VarbitID.COLLECTION_BOSSES_SARACHNIS_COMPLETED,
        "Hespori", VarbitID.COLLECTION_BOSSES_HESPORI_COMPLETED);
    private static final List<Integer> BARROWS_COMPONENTS = List.of(
        ItemID.BARROWS_AHRIM_HEAD, ItemID.BARROWS_AHRIM_WEAPON, ItemID.BARROWS_AHRIM_BODY, ItemID.BARROWS_AHRIM_LEGS,
        ItemID.BARROWS_DHAROK_HEAD, ItemID.BARROWS_DHAROK_WEAPON, ItemID.BARROWS_DHAROK_BODY, ItemID.BARROWS_DHAROK_LEGS,
        ItemID.BARROWS_GUTHAN_HEAD, ItemID.BARROWS_GUTHAN_WEAPON, ItemID.BARROWS_GUTHAN_BODY, ItemID.BARROWS_GUTHAN_LEGS,
        ItemID.BARROWS_KARIL_HEAD, ItemID.BARROWS_KARIL_WEAPON, ItemID.BARROWS_KARIL_BODY, ItemID.BARROWS_KARIL_LEGS,
        ItemID.BARROWS_TORAG_HEAD, ItemID.BARROWS_TORAG_WEAPON, ItemID.BARROWS_TORAG_BODY, ItemID.BARROWS_TORAG_LEGS,
        ItemID.BARROWS_VERAC_HEAD, ItemID.BARROWS_VERAC_WEAPON, ItemID.BARROWS_VERAC_BODY, ItemID.BARROWS_VERAC_LEGS);
    private static final List<String> BOSS_RANKS = List.of(
        "Brutus",
        "Barrows|Scurrius|Deranged archaeologist",
        "Giant Mole|Hespori|Sarachnis",
        "Artio|Calvar'ion|Chaos Elemental|Chaos Fanatic|Crazy archaeologist|King Black Dragon|Kalphite Queen|Revenant maledictus|Scorpia|Skotizo|Spindel",
        "Callisto|Corporeal Beast|Duke Sucellus|Nex|Phantom Muspah|The Gauntlet|The Leviathan|The Whisperer|Vardorvis|Venenatis|Vet'ion|Vorkath|Zulrah",
        "Chambers of Xeric|Theatre of Blood|Tombs of Amascut");
    private static final java.util.Map<String, Quest> ENCOUNTER_QUESTS = java.util.Map.ofEntries(
        java.util.Map.entry("Brutus", Quest.THE_IDES_OF_MILK),
        java.util.Map.entry("Barrows", Quest.PRIEST_IN_PERIL),
        java.util.Map.entry("Deranged archaeologist", Quest.BONE_VOYAGE),
        java.util.Map.entry("Duke Sucellus", Quest.DESERT_TREASURE_II__THE_FALLEN_EMPIRE),
        java.util.Map.entry("The Leviathan", Quest.DESERT_TREASURE_II__THE_FALLEN_EMPIRE),
        java.util.Map.entry("The Whisperer", Quest.DESERT_TREASURE_II__THE_FALLEN_EMPIRE),
        java.util.Map.entry("Vardorvis", Quest.DESERT_TREASURE_II__THE_FALLEN_EMPIRE),
        java.util.Map.entry("Vorkath", Quest.DRAGON_SLAYER_II),
        java.util.Map.entry("Zulrah", Quest.REGICIDE),
        java.util.Map.entry("The Gauntlet", Quest.SONG_OF_THE_ELVES),
        java.util.Map.entry("Theatre of Blood", Quest.PRIEST_IN_PERIL),
        java.util.Map.entry("Tombs of Amascut", Quest.BENEATH_CURSED_SANDS),
        java.util.Map.entry("Guardians of the Rift", Quest.TEMPLE_OF_THE_EYE));
    private static final List<String> LOOT_CLEARS = List.of("Chambers of Xeric", "Theatre of Blood", "Tombs of Amascut");
    private static final java.util.Map<String, List<Integer>> RAID_REWARDS = java.util.Map.of(
        "Chambers of Xeric", List.of(net.runelite.api.ItemID.DEXTEROUS_PRAYER_SCROLL, net.runelite.api.ItemID.ARCANE_PRAYER_SCROLL, net.runelite.api.ItemID.TWISTED_BUCKLER, net.runelite.api.ItemID.DRAGON_HUNTER_CROSSBOW, net.runelite.api.ItemID.DINHS_BULWARK, net.runelite.api.ItemID.ANCESTRAL_HAT, net.runelite.api.ItemID.ANCESTRAL_ROBE_TOP, net.runelite.api.ItemID.ANCESTRAL_ROBE_BOTTOM, net.runelite.api.ItemID.DRAGON_CLAWS, net.runelite.api.ItemID.ELDER_MAUL, net.runelite.api.ItemID.KODAI_INSIGNIA, net.runelite.api.ItemID.TWISTED_BOW),
        "Theatre of Blood", List.of(net.runelite.api.ItemID.AVERNIC_DEFENDER_HILT, net.runelite.api.ItemID.GHRAZI_RAPIER, net.runelite.api.ItemID.SANGUINESTI_STAFF, net.runelite.api.ItemID.JUSTICIAR_FACEGUARD, net.runelite.api.ItemID.JUSTICIAR_CHESTGUARD, net.runelite.api.ItemID.JUSTICIAR_LEGGUARDS, net.runelite.api.ItemID.SCYTHE_OF_VITUR),
        "Tombs of Amascut", List.of(net.runelite.api.ItemID.OSMUMTENS_FANG, net.runelite.api.ItemID.LIGHTBEARER, net.runelite.api.ItemID.ELIDINIS_WARD, net.runelite.api.ItemID.MASORI_MASK, net.runelite.api.ItemID.MASORI_BODY, net.runelite.api.ItemID.MASORI_CHAPS, net.runelite.api.ItemID.TUMEKENS_SHADOW));
    private static final List<Integer> SKILLING_PETS = List.of(
        net.runelite.api.ItemID.ROCK_GOLEM, net.runelite.api.ItemID.BEAVER, net.runelite.api.ItemID.HERON,
        net.runelite.api.ItemID.BABY_CHINCHOMPA, net.runelite.api.ItemID.PHOENIX, net.runelite.api.ItemID.GIANT_SQUIRREL,
        net.runelite.api.ItemID.TANGLEROOT, net.runelite.api.ItemID.ROCKY, net.runelite.api.ItemID.RIFT_GUARDIAN,
        net.runelite.api.ItemID.HERBI, net.runelite.api.ItemID.TINY_TEMPOR, net.runelite.api.ItemID.ABYSSAL_PROTECTOR);
    private static final List<Integer> RANK_S_BOSS_PETS = List.of(
        net.runelite.api.ItemID.PET_CHAOS_ELEMENTAL, net.runelite.api.ItemID.SCURRY,
        net.runelite.api.ItemID.PET_DAGANNOTH_SUPREME, net.runelite.api.ItemID.PET_DAGANNOTH_PRIME,
        net.runelite.api.ItemID.PET_DAGANNOTH_REX, net.runelite.api.ItemID.PET_KREEARRA,
        net.runelite.api.ItemID.PET_GENERAL_GRAARDOR, net.runelite.api.ItemID.PET_ZILYANA,
        net.runelite.api.ItemID.PET_KRIL_TSUTSAROTH, net.runelite.api.ItemID.PET_DARK_CORE,
        net.runelite.api.ItemID.PET_SNAKELING, net.runelite.api.ItemID.TZREKJAD,
        net.runelite.api.ItemID.TZREKZUK, net.runelite.api.ItemID.OLMLET,
        net.runelite.api.ItemID.LIL_ZIK, net.runelite.api.ItemID.TUMEKENS_GUARDIAN);
    private static final List<Integer> MAGE_ARENA_II_CAPES = List.of(
        net.runelite.api.ItemID.IMBUED_SARADOMIN_CAPE, net.runelite.api.ItemID.IMBUED_GUTHIX_CAPE,
        net.runelite.api.ItemID.IMBUED_ZAMORAK_CAPE);
    private static final java.util.Map<Skill, List<Integer>> SKILLCAPES = java.util.Map.ofEntries(
        java.util.Map.entry(Skill.ATTACK, List.of(net.runelite.api.ItemID.ATTACK_CAPE, net.runelite.api.ItemID.ATTACK_CAPET)),
        java.util.Map.entry(Skill.STRENGTH, List.of(net.runelite.api.ItemID.STRENGTH_CAPE, net.runelite.api.ItemID.STRENGTH_CAPET)),
        java.util.Map.entry(Skill.DEFENCE, List.of(net.runelite.api.ItemID.DEFENCE_CAPE, net.runelite.api.ItemID.DEFENCE_CAPET)),
        java.util.Map.entry(Skill.RANGED, List.of(net.runelite.api.ItemID.RANGING_CAPE, net.runelite.api.ItemID.RANGING_CAPET)),
        java.util.Map.entry(Skill.PRAYER, List.of(net.runelite.api.ItemID.PRAYER_CAPE, net.runelite.api.ItemID.PRAYER_CAPET)),
        java.util.Map.entry(Skill.MAGIC, List.of(net.runelite.api.ItemID.MAGIC_CAPE, net.runelite.api.ItemID.MAGIC_CAPET)),
        java.util.Map.entry(Skill.RUNECRAFT, List.of(net.runelite.api.ItemID.RUNECRAFT_CAPE, net.runelite.api.ItemID.RUNECRAFT_CAPET)),
        java.util.Map.entry(Skill.AGILITY, List.of(net.runelite.api.ItemID.AGILITY_CAPE, net.runelite.api.ItemID.AGILITY_CAPET)),
        java.util.Map.entry(Skill.HERBLORE, List.of(net.runelite.api.ItemID.HERBLORE_CAPE, net.runelite.api.ItemID.HERBLORE_CAPET)),
        java.util.Map.entry(Skill.THIEVING, List.of(net.runelite.api.ItemID.THIEVING_CAPE, net.runelite.api.ItemID.THIEVING_CAPET)),
        java.util.Map.entry(Skill.CRAFTING, List.of(net.runelite.api.ItemID.CRAFTING_CAPE, net.runelite.api.ItemID.CRAFTING_CAPET)),
        java.util.Map.entry(Skill.FLETCHING, List.of(net.runelite.api.ItemID.FLETCHING_CAPE, net.runelite.api.ItemID.FLETCHING_CAPET)),
        java.util.Map.entry(Skill.SLAYER, List.of(net.runelite.api.ItemID.SLAYER_CAPE, net.runelite.api.ItemID.SLAYER_CAPET)),
        java.util.Map.entry(Skill.CONSTRUCTION, List.of(net.runelite.api.ItemID.CONSTRUCT_CAPE, net.runelite.api.ItemID.CONSTRUCT_CAPET)),
        java.util.Map.entry(Skill.MINING, List.of(net.runelite.api.ItemID.MINING_CAPE, net.runelite.api.ItemID.MINING_CAPET)),
        java.util.Map.entry(Skill.SMITHING, List.of(net.runelite.api.ItemID.SMITHING_CAPE, net.runelite.api.ItemID.SMITHING_CAPET)),
        java.util.Map.entry(Skill.FISHING, List.of(net.runelite.api.ItemID.FISHING_CAPE, net.runelite.api.ItemID.FISHING_CAPET)),
        java.util.Map.entry(Skill.COOKING, List.of(net.runelite.api.ItemID.COOKING_CAPE, net.runelite.api.ItemID.COOKING_CAPET)),
        java.util.Map.entry(Skill.FIREMAKING, List.of(net.runelite.api.ItemID.FIREMAKING_CAPE, net.runelite.api.ItemID.FIREMAKING_CAPET)),
        java.util.Map.entry(Skill.WOODCUTTING, List.of(net.runelite.api.ItemID.WOODCUTTING_CAPE, net.runelite.api.ItemID.WOODCUT_CAPET)),
        java.util.Map.entry(Skill.FARMING, List.of(net.runelite.api.ItemID.FARMING_CAPE, net.runelite.api.ItemID.FARMING_CAPET)),
        java.util.Map.entry(Skill.HUNTER, List.of(net.runelite.api.ItemID.HUNTER_CAPE, net.runelite.api.ItemID.HUNTER_CAPET)),
        java.util.Map.entry(Skill.SAILING, List.of(net.runelite.api.ItemID.SAILING_CAPE, net.runelite.api.ItemID.SAILING_CAPET)));
    private enum RankSTrial { PET_AND_CLUES, SKILLCAPES, REWARDS }
    private static final List<Integer> MOONS_COMPONENTS = List.of(
        net.runelite.api.ItemID.BLUE_MOON_SPEAR, net.runelite.api.ItemID.BLUE_MOON_HELM,
        net.runelite.api.ItemID.BLUE_MOON_CHESTPLATE, net.runelite.api.ItemID.BLUE_MOON_TASSETS,
        net.runelite.api.ItemID.DUAL_MACUAHUITL, net.runelite.api.ItemID.BLOOD_MOON_HELM,
        net.runelite.api.ItemID.BLOOD_MOON_CHESTPLATE, net.runelite.api.ItemID.BLOOD_MOON_TASSETS,
        net.runelite.api.ItemID.ECLIPSE_ATLATL, net.runelite.api.ItemID.ECLIPSE_MOON_HELM,
        net.runelite.api.ItemID.ECLIPSE_MOON_CHESTPLATE, net.runelite.api.ItemID.ECLIPSE_MOON_TASSETS);
    private static final boolean DEMOTION_TEST = Boolean.getBoolean("emberbound.demotionTest");
    private static final boolean PROMOTION_TEST = Boolean.getBoolean("emberbound.promotionTest");
    private static final boolean COMPLETION_TEST = Boolean.getBoolean("emberbound.completionTest");
    private static final boolean FETCH_TEST = Boolean.getBoolean("emberbound.fetchTest");
    private static final String LEDGER_KEY = FETCH_TEST ? "fetchTestLedger" : COMPLETION_TEST ? "completionTestLedger"
        : PROMOTION_TEST ? "promotionTestLedger" : DEMOTION_TEST ? "demotionTestLedger" : "ledger";
    private static final Pattern BRUTUS_KILL_COUNT =
        Pattern.compile("Your Brutus kill count is:? ([0-9,]+)\\.?", Pattern.CASE_INSENSITIVE);
    private static final Pattern GIANT_CHEST_COUNT = Pattern.compile(
        "Your (Bryophyta|Obor)(?:'s)? chest(?: open(?:ing)?| loot)? count is:? ([0-9,]+)\\.?",
        Pattern.CASE_INSENSITIVE);
    private static final Pattern BRUTUS_LOG_ENTRY = Pattern.compile(
        "(?:New item added to your collection log|Collection log):? (?:Bottomless milk bucket(?: \\(empty\\))?|Cow slippers|Mooleta|Beef)\\.?",
        Pattern.CASE_INSENSITIVE);
    // Inventory form of Beef, the Brutus pet.
    private static final int BRUTUS_PET_ITEM = 33124;
    @Inject private Client client;
    @Inject private ClientThread clientThread;
    @Inject private ConfigManager configuration;
    @Inject private ClientToolbar toolbar;
    @Inject private Gson gson;
    @Inject private OverlayManager overlayManager;
    @Inject private MouseManager mouseManager;
    @Inject private KeyManager keyManager;
    @Inject private SkillIconManager skillIconManager;
    @Inject private AudioPlayer audioPlayer;
    private CovenantScroll scroll;
    private String displayedProfile;
    private int displayedSlate = -1;

    private final Properties catalog = new Properties();
    private final java.util.Map<Quest, String> questRanks = new java.util.EnumMap<>(Quest.class);
    private boolean observedUnfinishedQuest;
    private volatile boolean cofferReady;
    private boolean observedUnfinishedAchievement;
    private final java.util.Set<String> unfinishedFlagObjectives = new java.util.HashSet<>();
    private SlayerAssignment lastSlayerAssignment;
    private boolean slayerCompletionNotice;
    private boolean slayerCancellationNotice;
    private volatile boolean bankReady;
    private final java.util.Set<Skill> rankSBelow99 = java.util.EnumSet.noneOf(Skill.class);
    private PluginPanel panel;
    private NavigationButton navigation;
    private String profile;
    private Ledger ledger;
    private Integer lastObservedXp;
    private boolean freelanceBoardOpen;

    private static final class Offer
    {
        String skill;
        String encounter;
        String rank;
        int lastEncounterCount;
        int target;
        int reward;
        boolean freelance;
        boolean brutusTrial;
        boolean giantChestTrial;
        String oneTimeObjective;
        String slayerMaster;
        SlayerAssignment slayerAssignment;
        boolean slayerUnverified;
        boolean rankAStandard;
        boolean rankARaid;
        RankSTrial rankSTrial;
        List<Skill> rankSAllowedSkills;
        List<Skill> rankSVerifiedSkills;
        List<Skill> rankSReachedSkills;
        List<Integer> rankSNotifiedItems;
        List<Integer> rankSVerifiedItems;
        List<Integer> rankSStartingGodCapes;
        boolean rankSGodCape;
        int rankAPendingItem;
        boolean rankAPetVerified;
        int promotionPetId;
        int promotionLogVarbit;
        boolean promotionPetAnnounced;
        String capeTrial;
        boolean fireCapeVerified;
        boolean godCapeVerified;
        List<ArmoryPiece> armoryPieces;
        int oborChests;
        int lastOborChestCount;
        int lastBryophytaChestCount;
        String quest;
        boolean cofferFetch;
        int cofferTab;
        long bankBaseline;
        long cofferBaseline;
    }

    private static final class OneTimeObjective
    {
        final String name;
        final String rank;
        final String detail;
        final int completionVarbit;
        final String logPage;
        final int itemId;
        Quest rewardQuest;
        String diaryLastNormalRank;

        OneTimeObjective(String name, String rank, String detail, int completionVarbit)
        {
            this(name, rank, detail, completionVarbit, null, 0);
        }

        OneTimeObjective(String name, String rank, String detail, String logPage, int itemId)
        {
            this(name, rank, detail, 0, logPage, itemId);
        }

        OneTimeObjective(String name, String rank, String detail, Quest rewardQuest)
        {
            this(name, rank, detail, 0, null, 0);
            this.rewardQuest = rewardQuest;
        }

        private OneTimeObjective(String name, String rank, String detail, int completionVarbit, String logPage, int itemId)
        {
            this.name = name;
            this.rank = rank;
            this.detail = detail;
            this.completionVarbit = completionVarbit;
            this.logPage = logPage;
            this.itemId = itemId;
        }
    }

    private static final class SlayerAssignment
    {
        int monster;
        int bossVariant;
        String monsterName;
        int area;
        int master;
        int original;
        int remaining;
        int streak;
        String issuer;

        boolean sameTask(SlayerAssignment other)
        {
            return monster == other.monster && bossVariant == other.bossVariant && area == other.area
                && master == other.master && original == other.original;
        }
    }

    private static final class ArmoryPiece
    {
        int itemId;
        String name;
        long bankBaseline;
        long cofferBaseline;
        boolean logged;
        boolean deposited;
    }

    private static final class Ledger
    {
        List<Offer> offers = new ArrayList<>();
        Offer active;
        java.util.Map<String, Boolean> observedClogs = new java.util.HashMap<>();
        List<Integer> armoryRequest;
        String rankAObjective;
        String collectionTrialBoss;
        List<String> freelanceCooldown = new ArrayList<>();
        List<Offer> freelanceOffers = new ArrayList<>();
        String freelanceBoardRank;
        int boardCompletions;
        int freelanceBoardVersion;
        boolean rerollPending;
        boolean freelanceLocked;
        int recordedXp;
        int favour;
        int contractStreak;
        int lastBrutusKillCount;
        boolean brutusLogUpdate;
        String rank = "Adventurer";
        int refusals;
        int slateVersion;
        String message = "Welcome to the Emberbound Covenant.";
    }

    @Override
    protected void startUp() throws IOException
    {
        try (InputStream source = getClass().getResourceAsStream("/guildmaster.properties"))
        {
            if (source == null)
            {
                throw new IOException("Missing bundled contract catalog");
            }
            catalog.load(source);
        }
        loadQuestRanks();
        panel = new PluginPanel() {};
        panel.setLayout(new BorderLayout());
        BufferedImage icon = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D graphics = icon.createGraphics();
        graphics.setColor(new java.awt.Color(232, 177, 91));
        graphics.fillRect(2, 3, 12, 10);
        graphics.setColor(new java.awt.Color(95, 61, 26));
        graphics.drawLine(2, 3, 8, 8);
        graphics.drawLine(14, 3, 8, 8);
        graphics.dispose();
        navigation = NavigationButton.builder().tooltip("Guild Master").icon(icon).panel(panel).build();
        toolbar.addNavigation(navigation);
        scroll = new CovenantScroll(client);
        overlayManager.add(scroll);
        overlayManager.add(scroll.envelope);
        mouseManager.registerMouseListener(scroll.input);
        keyManager.registerKeyListener(scroll.keyboard);
        clientThread.invokeLater(this::loadProfile);
    }

    private void loadQuestRanks() throws IOException
    {
        java.util.Map<String, Quest> byName = new java.util.HashMap<>();
        for (Quest quest : Quest.values())
        {
            byName.put(questNameKey(quest.getName()), quest);
        }
        questRanks.clear();
        try (InputStream source = getClass().getResourceAsStream("/ranked-quests.txt"))
        {
            if (source == null)
            {
                throw new IOException("Missing bundled quest roster");
            }
            BufferedReader roster = new BufferedReader(new InputStreamReader(source, StandardCharsets.UTF_8));
            String line;
            while ((line = roster.readLine()) != null)
            {
                String[] entry = line.split("\\|", 2);
                Quest quest = byName.get(questNameKey(entry[1]));
                if (quest != null)
                {
                    questRanks.put(quest, entry[0]);
                }
            }
        }
    }

    private static String questNameKey(String title)
    {
        return title.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    @Override
    protected void shutDown()
    {
        toolbar.removeNavigation(navigation);
        mouseManager.unregisterMouseListener(scroll.input);
        keyManager.unregisterKeyListener(scroll.keyboard);
        overlayManager.remove(scroll);
        overlayManager.remove(scroll.envelope);
        scroll.clear();
        displayedSlate = -1;
        displayedProfile = null;
        profile = null;
        freelanceBoardOpen = false;
        ledger = null;
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged event)
    {
        if (event.getGameState() == GameState.LOGIN_SCREEN || event.getGameState() == GameState.HOPPING)
        {
            profile = null;
            freelanceBoardOpen = false;
            ledger = null;
            render();
        }
        if (event.getGameState() != GameState.LOGGED_IN)
        {
            lastObservedXp = null;
            observedUnfinishedAchievement = false;
            unfinishedFlagObjectives.clear();
            lastSlayerAssignment = null;
            slayerCompletionNotice = false;
            slayerCancellationNotice = false;
            cofferReady = false;
            bankReady = false;
            rankSBelow99.clear();
            observedUnfinishedQuest = false;
        }
        else
        {
            clientThread.invokeLater(this::loadProfile);
        }
    }

    private void loadProfile()
    {
        if (client.getGameState() != GameState.LOGGED_IN)
        {
            render();
            return;
        }
        String current = configuration.getRSProfileKey();
        if (current != null && !current.equals(profile))
        {
            scroll.clearCompletion();
            profile = current;
            freelanceBoardOpen = false;
            String data = configuration.getRSProfileConfiguration(GROUP, LEDGER_KEY);
            Ledger saved = data == null ? null : gson.fromJson(data, Ledger.class);
            ledger = saved == null ? new Ledger() : saved;
            if (DEMOTION_TEST && ledger.active == null && !ledger.offers.isEmpty()
                && ledger.refusals == 0 && ledger.favour == 20000
                && ("You have been demoted to Rank " + ledger.rank + " at 20.00% favour.").equals(ledger.message))
            {
                ledger.offers.clear();
                configuration.setRSProfileConfiguration(GROUP, LEDGER_KEY, gson.toJson(ledger));
            }
            if (FETCH_TEST)
            {
                ledger = new Ledger();
                Offer contract = new Offer();
                contract.encounter = "Oak logs";
                contract.rank = ledger.rank;
                contract.target = 20;
                contract.reward = 2000;
                contract.cofferFetch = true;
                ledger.offers.add(contract);
                ledger.message = "Fetch Alpha: open the bank and accept the 20 oak logs offer.";
                configuration.setRSProfileConfiguration(GROUP, LEDGER_KEY, gson.toJson(ledger));
            }
            else if (COMPLETION_TEST)
            {
                ledger = new Ledger();
                Offer contract = new Offer();
                contract.skill = Skill.DEFENCE.name();
                contract.rank = ledger.rank;
                contract.target = 2000;
                contract.reward = 2000;
                ledger.active = contract;
                ledger.recordedXp = contract.target - 1;
                lastObservedXp = null;
                ledger.message = "Completion test: earn 1 Defence XP.";
            }
            else if (saved == null && (PROMOTION_TEST || DEMOTION_TEST))
            {
                ledger.rank = PROMOTION_TEST ? "Adventurer" : "C";
                ledger.favour = PROMOTION_TEST ? 100000 : 1000;
                configuration.setRSProfileConfiguration(GROUP, LEDGER_KEY, gson.toJson(ledger));
            }
        }
        render();
    }

    @Subscribe
    public void onStatChanged(StatChanged event)
    {
        if (!hasActiveProfile() || ledger.active == null || ledger.active.brutusTrial || ledger.active.encounter != null
            || !event.getSkill().name().equalsIgnoreCase(ledger.active.skill)
            || (COMPLETION_TEST && lastObservedXp == null))
        {
            return;
        }
        recordXp(event.getXp());
    }

    @Subscribe
    public void onGameTick(GameTick event)
    {
        if (hasActiveProfile())
        {
            updateClogObservations();
            updateOfferedFlagObjectives();
        }
        bankReady = hasActiveProfile() && openBank() != null;
        cofferReady = hasActiveProfile() && ledger.active == null && readCoffer(null) != null;
        if (hasActiveProfile() && ledger.active != null)
        {
            if (ledger.active.oneTimeObjective != null)
            {
                trackOneTimeObjective(ledger.active);
            }
            else if ("Slayer's Gauntlet".equals(ledger.active.encounter))
            {
                trackSlayer(ledger.active);
            }
            else if (ledger.active.brutusTrial)
            {
                checkBrutusLog();
            }
            else if (ledger.active.rankSTrial != null)
            {
                trackRankSTrial(ledger.active);
            }
            else if (ledger.active.rankARaid || ledger.active.rankAStandard)
            {
                trackRankATrial(ledger.active);
            }
            else if (ledger.active.armoryPieces != null)
            {
                trackArmory(ledger.active);
            }
            else if (ledger.active.capeTrial != null)
            {
                trackCapeTrial(ledger.active);
            }
            else if (ledger.active.promotionLogVarbit > 0)
            {
                if (client.getVarbitValue(ledger.active.promotionLogVarbit) == 1)
                {
                    ledger.recordedXp = 1;
                    verifyCompletion(() -> finishPromotion("B"));
                }
            }
            else if (ledger.active.promotionPetAnnounced)
            {
                ItemContainer log = client.getItemContainer(InventoryID.COLLECTION_TRANSMIT);
                if (log != null && log.count(ledger.active.promotionPetId) > 0)
                {
                    verifyCompletion(() -> finishPromotion("B"));
                }
            }
            else if (ledger.active.cofferFetch)
            {
                trackCoffer(ledger.active);
            }
            else if (ledger.active.quest != null)
            {
                if (Quest.valueOf(ledger.active.quest).getState(client) != QuestState.FINISHED)
                {
                    observedUnfinishedQuest = true;
                }
                else if (observedUnfinishedQuest)
                {
                    observedUnfinishedQuest = false;
                    advanceEncounter();
                }
            }
            else if (ledger.active.encounter == null)
            {
                recordXp(client.getSkillExperience(Skill.valueOf(ledger.active.skill)));
            }
        }
    }

    private void recordXp(int xp)
    {
        if (lastObservedXp == null)
        {
            lastObservedXp = xp;
            return;
        }
        if (xp <= lastObservedXp)
        {
            return;
        }
        int earned = xp - lastObservedXp;
        lastObservedXp = xp;
        ledger.recordedXp = Math.min(ledger.active.target, ledger.recordedXp + earned);
        if (ledger.recordedXp == ledger.active.target)
        {
            verifyCompletion(ledger.active.freelance ? this::finishFreelance : this::finishIssuedContract);
            return;
        }
        saveAndRender();
    }

    private void removeFreelance(Offer offer)
    {
        String key = freelanceKey(offer);
        ledger.freelanceCooldown.add(key);
        ledger.freelanceOffers.removeIf(entry -> freelanceKey(entry).equals(key));
    }

    private void finishFreelance()
    {
        ledger.favour += Math.max(0, Math.min(ledger.active.reward, 99000 - ledger.favour));
        removeFreelance(ledger.active);
        ledger.active = null;
        countBoardCompletion();
        ledger.message = "Freelance work verified.";
    }

    private void finishIssuedContract()
    {
        ledger.freelanceLocked = false;
        ledger.favour = Math.min(100000, ledger.favour + ledger.active.reward);
        ledger.contractStreak++;
        ledger.active = null;
        countBoardCompletion();
        if (ledger.refusals == 2)
        {
            ledger.refusals = 3;
            drawOffers();
            ledger.message = "Recovery work verified. Your favour is unchanged. The return slate is ready.";
        }
        else
        {
            ledger.refusals = 0;
            ledger.message = catalog.getProperty("completed");
        }
    }

    private static String offerTitle(Offer offer)
    {
        if (offer.giantChestTrial)
        {
            return "Giant chest trial";
        }
        if (offer.brutusTrial)
        {
            return "Brutus promotion trial";
        }
        if (isFetch(offer.encounter))
        {
            String action = offer.cofferFetch ? "Fetch " : "Yew logs".equals(offer.encounter) ? "Chop " : "Mine ";
            return action + offer.encounter.toLowerCase(Locale.ROOT);
        }
        return offer.encounter == null ? skillName(offer.skill) : offer.encounter;
    }

    private static String offerUnit(Offer offer)
    {
        if (offer.oneTimeObjective != null)
        {
            return "unlock";
        }
        if ("Slayer's Gauntlet".equals(offer.encounter))
        {
            return "assignments";
        }
        if (offer.rankSTrial != null)
        {
            switch (offer.rankSTrial)
            {
                case PET_AND_CLUES: return "Master caskets";
                case SKILLCAPES: return "skillcapes";
                default: return "rewards";
            }
        }
        if (offer.rankARaid) return "raid unique";
        if (offer.rankAStandard && "Clue caskets".equals(offer.encounter)) return "caskets";
        if (offer.rankAStandard && "Perilous Moons".equals(offer.encounter)) return "full clears";
        if (offer.promotionLogVarbit > 0)
        {
            return "completed log";
        }
        if (offer.capeTrial != null)
        {
            return "capes";
        }
        if (offer.armoryPieces != null)
        {
            return "armory pieces";
        }
        if (offer.quest != null)
        {
            return "quest";
        }
        if ("Guardians of the Rift".equals(offer.encounter))
        {
            return "rifts";
        }
        if ("Iron ore".equals(offer.encounter) || "Runite ore".equals(offer.encounter))
        {
            return "ore";
        }
        if ("Yew logs".equals(offer.encounter) || "Oak logs".equals(offer.encounter))
        {
            return "logs";
        }
        if (LOOT_CLEARS.contains(offer.encounter == null ? "" : offer.encounter))
        {
            return "clears";
        }
        if (offer.encounter != null && offer.encounter.startsWith("Clue Scroll ("))
        {
            return "caskets";
        }
        return offer.brutusTrial || offer.encounter != null ? encounterUnit(offer.encounter) : "XP";
    }

    private static boolean canReroll(Ledger state)
    {
        return state.active != null && !state.active.freelance && !isPromotion(state.active)
            && (long) state.recordedXp * 5 >= (long) state.active.target * 4;
    }

    private static boolean isPromotion(Offer offer)
    {
        return offer.brutusTrial || offer.giantChestTrial || offer.armoryPieces != null || offer.capeTrial != null
            || offer.promotionPetId > 0 || offer.promotionLogVarbit > 0 || offer.rankAStandard || offer.rankARaid
            || offer.rankSTrial != null;
    }

    private void reroll()
    {
        if (!hasActiveProfile() || !canReroll(ledger))
        {
            return;
        }
        Offer replaced = ledger.active;
        String rank = ledger.active.rank == null
            ? (ledger.refusals == 2 ? lowerRank(ledger.rank) : ledger.rank) : ledger.active.rank;
        ledger.active = null;
        ledger.recordedXp = 0;
        ledger.rerollPending = true;
        ledger.freelanceLocked = true;
        freelanceBoardOpen = false;
        drawOffers(rank, replaced);
        ledger.message = "Your replacement slate is ready. Refusing it starts the refusal sequence.";
        saveAndRender();
    }

    private void resetFreelanceBoard()
    {
        ledger.freelanceOffers.clear();
        ledger.freelanceCooldown.clear();
        ledger.freelanceBoardRank = null;
        ledger.boardCompletions = 0;
    }

    private void countBoardCompletion()
    {
        if (++ledger.boardCompletions >= 5)
        {
            resetFreelanceBoard();
        }
    }

    private void verifyCompletion(Runnable apply)
    {
        Offer completed = ledger.active;
        int progress = ledger.recordedXp;
        int priorFavour = ledger.favour;
        apply.run();
        String rank = completed.rank == null ? ledger.rank : completed.rank;
        String parameters = String.format(Locale.US, "Rank %s  /  %,d %s", rank, completed.target, offerUnit(completed));
        String result = completed.brutusTrial
            ? (progress >= completed.target ? "Kill target met. Promoted to Rank D." : "Collection log complete. Promoted to Rank D.")
            : "+" + formatFavour(ledger.favour - priorFavour) + " favour";
        if (completed.giantChestTrial)
        {
            parameters = completed.oborChests >= 15 ? "15 Bryophyta + 15 Obor chests" : "30 Bryophyta chests";
            result = "Promoted to Rank C.";
        }
        if (completed.armoryPieces != null || completed.capeTrial != null || completed.promotionPetId > 0
            || completed.promotionLogVarbit > 0)
        {
            result = "Promoted to Rank B.";
        }
        if (completed.promotionPetId > 0 && progress < completed.target)
        {
            parameters = "New boss pet verified in the Collection Log.";
        }
        if (completed.rankAStandard || completed.rankARaid)
        {
            result = "Promoted to Rank A.";
        }
        if (completed.rankSTrial != null)
        {
            result = "Promoted to Rank S.";
        }
        saveAndRender();
        scroll.showCompletion(offerTitle(completed), parameters, result);
        java.util.concurrent.CompletableFuture.runAsync(() ->
        {
            try
            {
                audioPlayer.play(GuildMasterPlugin.class, "/completion-fanfare.wav", 0f);
            }
            catch (Exception ex)
            {
                java.util.logging.Logger.getLogger(GuildMasterPlugin.class.getName())
                    .log(java.util.logging.Level.WARNING, "Unable to play completion fanfare", ex);
            }
        });
    }

    private boolean hasActiveProfile()
    {
        return ledger != null && profile != null && client.getGameState() == GameState.LOGGED_IN
            && profile.equals(configuration.getRSProfileKey());
    }

    private void saveAndRender()
    {
        configuration.setRSProfileConfiguration(GROUP, LEDGER_KEY, gson.toJson(ledger));
        render();
    }

    private void request()
    {
        if (!hasActiveProfile() || ledger.active != null)
        {
            return;
        }
        if (ledger.offers.isEmpty())
        {
            drawOffers();
            ledger.message = ledger.offers.isEmpty() ? "No eligible contracts are available." : catalog.getProperty("request");
        }
        else
        {
            displayedSlate = -1;
            syncScroll();
            scroll.reopenSlate();
        }
        saveAndRender();
    }

    private void drawOffers()
    {
        drawOffers(null, null);
    }

    private void drawOffers(String replacementRank, Offer replaced)
    {
        ledger.slateVersion++;
        String skillingRank = replacementRank != null ? replacementRank
            : ledger.refusals == 2 ? lowerRank(ledger.rank) : ledger.rank;
        List<Offer> pool = skillingOffers(skillingRank, 0);
        addRankedEncounters(pool, skillingRank, false);
        addQuestOffers(pool, skillingRank);
        addOneTimeOffers(pool, skillingRank);
        if (client.getWorldType().contains(WorldType.MEMBERS))
        {
            Offer slayer = new Offer();
            slayer.encounter = "Slayer's Gauntlet";
            slayer.rank = skillingRank;
            pool.add(slayer);
        }
        if (replaced != null)
        {
            pool.removeIf(offer -> freelanceKey(offer).equals(freelanceKey(replaced)));
        }
        Collections.shuffle(pool);
        for (int scope = 0; scope < 3; scope++)
        {
            for (java.util.Iterator<Offer> candidates = pool.iterator(); candidates.hasNext();)
            {
                Offer offer = candidates.next();
                int target = contractTarget(offer, scope);
                if (target == 0)
                {
                    continue;
                }
                if (offer.encounter == null && remainingSkillXp(Skill.valueOf(offer.skill)) < target)
                {
                    continue;
                }
                offer.target = target;
                if ("Slayer's Gauntlet".equals(offer.encounter))
                {
                    offer.slayerMaster = chooseSlayerMaster(offer.rank, scope);
                }
                offer.reward = ledger.refusals == 2 ? 0
                    : Integer.parseInt(catalog.getProperty("rewards." + offer.rank).split(",")[
                        offer.quest == null && offer.oneTimeObjective == null ? scope : 1]);
                if (offer.oneTimeObjective != null && ledger.refusals != 2)
                {
                    OneTimeObjective objective = findOneTimeObjective(offer.oneTimeObjective);
                    if (objective.diaryLastNormalRank != null)
                    {
                        int base = Integer.parseInt(catalog.getProperty("rewards." + objective.rank).split(",")[1]);
                        offer.reward = base * (rankOrder(ledger.rank) <= rankOrder(objective.diaryLastNormalRank) ? 3 : 1);
                    }
                    else
                    {
                        int base = Integer.parseInt(catalog.getProperty("rewards." + ledger.rank).split(",")[1]);
                        int rankDifference = Integer.compare(rankOrder(offer.rank), rankOrder(ledger.rank));
                        offer.reward = base * (2 + rankDifference) / 2;
                    }
                }
                ledger.offers.add(offer);
                candidates.remove();
                break;
            }
        }
    }

    private int remainingSkillXp(Skill skill)
    {
        return Experience.MAX_SKILL_XP - client.getSkillExperience(skill);
    }

    private List<Offer> skillingOffers(String rank, int scope)
    {
        int target = Integer.parseInt(catalog.getProperty("targets." + rank).split(",")[scope]);
        boolean membersWorld = client.getWorldType().contains(WorldType.MEMBERS);
        List<Offer> pool = new ArrayList<>();
        for (Skill skill : Skill.values())
        {
            if (skill == Skill.OVERALL || (skill.isMembers() && !membersWorld)
                || (skill == Skill.HERBLORE && Quest.DRUIDIC_RITUAL.getState(client) != QuestState.FINISHED)
                || (skill == Skill.SAILING && client.getSkillExperience(skill) <= 0)
                || remainingSkillXp(skill) < target)
            {
                continue;
            }
            Offer offer = new Offer();
            offer.skill = skill.name();
            offer.rank = rank;
            pool.add(offer);
        }
        return pool;
    }

    private int contractTarget(Offer offer, int scope)
    {
        if (offer.quest != null || offer.oneTimeObjective != null)
        {
            return 1;
        }
        String key = offer.encounter == null ? offer.rank : offer.encounter;
        String ladder = catalog.getProperty("targets." + key + "." + offer.rank,
            catalog.getProperty("targets." + key));
        return Integer.parseInt(ladder.split(",")[scope]);
    }

    private static List<OneTimeObjective> diaryObjectives()
    {
        List<OneTimeObjective> diaries = new ArrayList<>();
        addDiaryRegion(diaries, "Ardougne",
            VarbitID.ARDOUGNE_DIARY_EASY_COMPLETE, VarbitID.ARDOUGNE_DIARY_MEDIUM_COMPLETE,
            VarbitID.ARDOUGNE_DIARY_HARD_COMPLETE, VarbitID.ARDOUGNE_DIARY_ELITE_COMPLETE);
        addDiaryRegion(diaries, "Falador",
            VarbitID.FALADOR_DIARY_EASY_COMPLETE, VarbitID.FALADOR_DIARY_MEDIUM_COMPLETE,
            VarbitID.FALADOR_DIARY_HARD_COMPLETE, VarbitID.FALADOR_DIARY_ELITE_COMPLETE);
        addDiaryRegion(diaries, "Wilderness",
            VarbitID.WILDERNESS_DIARY_EASY_COMPLETE, VarbitID.WILDERNESS_DIARY_MEDIUM_COMPLETE,
            VarbitID.WILDERNESS_DIARY_HARD_COMPLETE, VarbitID.WILDERNESS_DIARY_ELITE_COMPLETE);
        addDiaryRegion(diaries, "Western Provinces",
            VarbitID.WESTERN_DIARY_EASY_COMPLETE, VarbitID.WESTERN_DIARY_MEDIUM_COMPLETE,
            VarbitID.WESTERN_DIARY_HARD_COMPLETE, VarbitID.WESTERN_DIARY_ELITE_COMPLETE);
        addDiaryRegion(diaries, "Kandarin",
            VarbitID.KANDARIN_DIARY_EASY_COMPLETE, VarbitID.KANDARIN_DIARY_MEDIUM_COMPLETE,
            VarbitID.KANDARIN_DIARY_HARD_COMPLETE, VarbitID.KANDARIN_DIARY_ELITE_COMPLETE);
        addDiaryRegion(diaries, "Varrock",
            VarbitID.VARROCK_DIARY_EASY_COMPLETE, VarbitID.VARROCK_DIARY_MEDIUM_COMPLETE,
            VarbitID.VARROCK_DIARY_HARD_COMPLETE, VarbitID.VARROCK_DIARY_ELITE_COMPLETE);
        addDiaryRegion(diaries, "Desert",
            VarbitID.DESERT_DIARY_EASY_COMPLETE, VarbitID.DESERT_DIARY_MEDIUM_COMPLETE,
            VarbitID.DESERT_DIARY_HARD_COMPLETE, VarbitID.DESERT_DIARY_ELITE_COMPLETE);
        addDiaryRegion(diaries, "Morytania",
            VarbitID.MORYTANIA_DIARY_EASY_COMPLETE, VarbitID.MORYTANIA_DIARY_MEDIUM_COMPLETE,
            VarbitID.MORYTANIA_DIARY_HARD_COMPLETE, VarbitID.MORYTANIA_DIARY_ELITE_COMPLETE);
        addDiaryRegion(diaries, "Fremennik",
            VarbitID.FREMENNIK_DIARY_EASY_COMPLETE, VarbitID.FREMENNIK_DIARY_MEDIUM_COMPLETE,
            VarbitID.FREMENNIK_DIARY_HARD_COMPLETE, VarbitID.FREMENNIK_DIARY_ELITE_COMPLETE);
        addDiaryRegion(diaries, "Lumbridge & Draynor",
            VarbitID.LUMBRIDGE_DIARY_EASY_COMPLETE, VarbitID.LUMBRIDGE_DIARY_MEDIUM_COMPLETE,
            VarbitID.LUMBRIDGE_DIARY_HARD_COMPLETE, VarbitID.LUMBRIDGE_DIARY_ELITE_COMPLETE);
        addDiaryRegion(diaries, "Kourend & Kebos",
            VarbitID.KOUREND_DIARY_EASY_COMPLETE, VarbitID.KOUREND_DIARY_MEDIUM_COMPLETE,
            VarbitID.KOUREND_DIARY_HARD_COMPLETE, VarbitID.KOUREND_DIARY_ELITE_COMPLETE);
        addDiaryRegion(diaries, "Karamja",
            VarbitID.ATJUN_EASY_DONE, VarbitID.ATJUN_MED_DONE,
            VarbitID.ATJUN_HARD_DONE, VarbitID.KARAMJA_DIARY_ELITE_COMPLETE);
        return List.copyOf(diaries);
    }

    private static void addDiaryRegion(List<OneTimeObjective> diaries, String region, int... completionFlags)
    {
        String[] tiers = {"Easy", "Medium", "Hard", "Elite"};
        String[] firstRanks = {"Adventurer", "C", "B", "A"};
        String[] lastNormalRanks = {"D", "C", "A", "S"};
        for (int tier = 0; tier < tiers.length; tier++)
        {
            OneTimeObjective objective = new OneTimeObjective(region + " " + tiers[tier] + " Diary", firstRanks[tier],
                "Finish the remaining " + tiers[tier] + " tasks in the " + region + " Achievement Diary.", completionFlags[tier]);
            objective.diaryLastNormalRank = lastNormalRanks[tier];
            diaries.add(objective);
        }
    }

    private static OneTimeObjective findOneTimeObjective(String name)
    {
        return java.util.stream.Stream.concat(ONE_TIME_OBJECTIVES.stream(), DIARY_OBJECTIVES.stream())
            .filter(objective -> objective.name.equals(name)).findFirst().orElseThrow();
    }

    private boolean oneTimeEligible(OneTimeObjective objective)
    {
        if (objective.rewardQuest != null)
        {
            return client.getWorldType().contains(WorldType.MEMBERS)
                && objective.rewardQuest.getState(client) == QuestState.IN_PROGRESS;
        }
        if (objective.diaryLastNormalRank != null)
        {
            return client.getWorldType().contains(WorldType.MEMBERS) && client.getVarbitValue(objective.completionVarbit) == 0;
        }
        if (objective.completionVarbit > 0)
        {
            if (objective.name.startsWith("Barrows ") && Quest.PRIEST_IN_PERIL.getState(client) != QuestState.FINISHED)
            {
                return false;
            }
            return client.getVarbitValue(objective.completionVarbit) == 0
                && (client.getWorldType().contains(WorldType.MEMBERS)
                    || objective.name.startsWith("Obor ") || objective.name.startsWith("Bryophyta "));
        }
        if (!client.getWorldType().contains(WorldType.MEMBERS)
            || !Boolean.FALSE.equals(ledger.observedClogs.get(objective.name)))
        {
            return false;
        }
        switch (objective.logPage)
        {
            case "Motherlode Mine":
                return client.getRealSkillLevel(Skill.MINING) >= 30;
            case "Tithe Farm":
                return client.getRealSkillLevel(Skill.FARMING) >= 34
                    && (!"Herb sack".equals(objective.name) || client.getRealSkillLevel(Skill.HERBLORE) >= 58);
            case "Cyclopes":
                int attack = client.getRealSkillLevel(Skill.ATTACK);
                int strength = client.getRealSkillLevel(Skill.STRENGTH);
                return attack + strength >= 130 || attack == 99 || strength == 99;
            case "Giants' Foundry":
                return Quest.SLEEPING_GIANTS.getState(client) == QuestState.FINISHED;
            default:
                return true;
        }
    }

    private void addOneTimeOffers(List<Offer> pool, String rank)
    {
        boolean exactRank = ledger.rerollPending || ledger.refusals == 2 || ledger.refusals == 4;
        int rankIndex = rankOrder(rank);
        int minimum = !exactRank && ledger.favour <= 50000 ? Math.max(0, rankIndex - 1) : rankIndex;
        int maximum = !exactRank && ledger.favour >= 75000 ? Math.min(RANKS.size() - 1, rankIndex + 1) : rankIndex;
        for (OneTimeObjective objective : ONE_TIME_OBJECTIVES)
        {
            int objectiveRank = rankOrder(objective.rank);
            if (objectiveRank >= minimum && objectiveRank <= maximum && oneTimeEligible(objective))
            {
                Offer offer = new Offer();
                offer.encounter = objective.name;
                offer.oneTimeObjective = objective.name;
                offer.rank = objective.rank;
                pool.add(offer);
            }
        }
        for (OneTimeObjective diary : DIARY_OBJECTIVES)
        {
            int firstRank = rankOrder(diary.rank);
            if ((exactRank ? rankIndex >= firstRank && rankIndex <= rankOrder(diary.diaryLastNormalRank)
                : rankIndex >= firstRank) && oneTimeEligible(diary))
            {
                Offer offer = new Offer();
                offer.encounter = diary.name;
                offer.oneTimeObjective = diary.name;
                offer.rank = exactRank ? rank : diary.rank;
                pool.add(offer);
            }
        }
    }

    private void updateClogObservations()
    {
        net.runelite.api.widgets.Widget title = client.getWidget(InterfaceID.Collection.HEADER_TEXT);
        ItemContainer log = client.getItemContainer(InventoryID.COLLECTION_TRANSMIT);
        if (title == null || title.isHidden() || log == null)
        {
            return;
        }
        String page = title.getText() == null ? "" : Text.removeTags(title.getText()).trim();
        boolean changed = false;
        for (OneTimeObjective objective : ONE_TIME_OBJECTIVES)
        {
            if (objective.logPage != null && (objective.logPage.equalsIgnoreCase(page) || log.count(objective.itemId) > 0)
                && !Boolean.TRUE.equals(ledger.observedClogs.get(objective.name)))
            {
                boolean complete = log.count(objective.itemId) > 0;
                Boolean previous = ledger.observedClogs.put(objective.name, complete);
                changed |= previous == null || previous != complete;
                if (complete)
                {
                    recordClogCompletion(objective, Boolean.FALSE.equals(previous));
                }
            }
        }
        if (changed)
        {
            saveAndRender();
        }
    }

    private void recordClogCompletion(OneTimeObjective objective, boolean knownPreviouslyMissing)
    {
        if (knownPreviouslyMissing)
        {
            creditUnassignedObjective(objective);
        }
        if (ledger.offers.removeIf(offer -> objective.name.equals(offer.oneTimeObjective)))
        {
            ledger.slateVersion++;
        }
    }

    private void useOneTimeCompletionNotice(String message)
    {
        String clogPrefix = "New item added to your collection log: ";
        if (message.regionMatches(true, 0, clogPrefix, 0, clogPrefix.length()))
        {
            String item = message.substring(clogPrefix.length()).replaceFirst("\\.$", "").trim();
            for (OneTimeObjective objective : ONE_TIME_OBJECTIVES)
            {
                if (objective.logPage != null && item.equalsIgnoreCase(objective.name))
                {
                    Boolean previous = ledger.observedClogs.put(objective.name, true);
                    if (!Boolean.TRUE.equals(previous))
                    {
                        recordClogCompletion(objective, Boolean.FALSE.equals(previous));
                        saveAndRender();
                    }
                    return;
                }
            }
        }
        else if (message.matches("(?i)Congratulations, you(?:'ve|’ve| have) completed .*combat task:.*"))
        {
            // The message prompts a recheck. The game's completion flag still supplies the result.
            updateOfferedFlagObjectives();
            if (ledger.active != null && ledger.active.oneTimeObjective != null
                && findOneTimeObjective(ledger.active.oneTimeObjective).completionVarbit > 0)
            {
                trackOneTimeObjective(ledger.active);
            }
        }
    }

    private void updateOfferedFlagObjectives()
    {
        boolean changed = false;
        for (java.util.Iterator<Offer> offers = ledger.offers.iterator(); offers.hasNext();)
        {
            Offer offer = offers.next();
            if (offer.oneTimeObjective == null)
            {
                continue;
            }
            OneTimeObjective objective = findOneTimeObjective(offer.oneTimeObjective);
            if (objective.completionVarbit == 0)
            {
                continue;
            }
            if (client.getVarbitValue(objective.completionVarbit) == 0)
            {
                unfinishedFlagObjectives.add(objective.name);
            }
            else
            {
                if (unfinishedFlagObjectives.remove(objective.name))
                {
                    creditUnassignedObjective(objective);
                }
                offers.remove();
                changed = true;
            }
        }
        if (changed)
        {
            ledger.slateVersion++;
            saveAndRender();
        }
    }

    private void creditUnassignedObjective(OneTimeObjective objective)
    {
        if ((ledger.active != null && objective.name.equals(ledger.active.oneTimeObjective))
            || (rankOrder(objective.rank) > rankOrder(ledger.rank)
                && ledger.offers.stream().noneMatch(offer -> objective.name.equals(offer.oneTimeObjective))))
        {
            return;
        }
        int standard = Integer.parseInt(catalog.getProperty("rewards." + objective.rank).split(",")[1]);
        int credit = Math.max(0, Math.min(standard / 10, 99000 - ledger.favour));
        if (credit > 0)
        {
            ledger.favour += credit;
            ledger.message = "Freelance objective verified: " + objective.name + ".";
        }
    }

    private void trackOneTimeObjective(Offer offer)
    {
        OneTimeObjective objective = findOneTimeObjective(offer.oneTimeObjective);
        if (objective.logPage != null)
        {
            if (Boolean.TRUE.equals(ledger.observedClogs.get(objective.name)))
            {
                advanceEncounter();
            }
        }
        else if (objective.rewardQuest != null
            ? objective.rewardQuest.getState(client) != QuestState.FINISHED
            : client.getVarbitValue(objective.completionVarbit) == 0)
        {
            observedUnfinishedAchievement = true;
        }
        else if (observedUnfinishedAchievement)
        {
            advanceEncounter();
        }
    }

    private void addQuestOffers(List<Offer> pool, String rank)
    {
        if (!client.getWorldType().contains(WorldType.MEMBERS))
        {
            return;
        }
        boolean exactRank = ledger.rerollPending || ledger.refusals == 2 || ledger.refusals == 4;
        questRanks.forEach((quest, questRank) ->
        {
            boolean eligibleRank = exactRank ? questRank.equals(rank) : rankOrder(questRank) <= rankOrder(rank);
            // An in-progress quest provides a client-side access check for this offer.
            if (eligibleRank && quest.getState(client) == QuestState.IN_PROGRESS)
            {
                Offer offer = new Offer();
                offer.quest = quest.name();
                offer.encounter = quest.getName();
                offer.rank = questRank;
                pool.add(offer);
            }
        });
    }

    private void addRankedEncounters(List<Offer> pool, String rank, boolean freelance)
    {
        boolean exactRank = freelance || ledger.rerollPending || ledger.refusals == 2 || ledger.refusals == 4;
        for (int i = exactRank ? rankOrder(rank) : 0; i <= rankOrder(rank); i++)
        {
            for (String encounter : BOSS_RANKS.get(i).split("\\|"))
            {
                addEncounter(pool, encounter, RANKS.get(i));
            }
        }
        addEncounter(pool, "Guardians of the Rift", rank);
        if (client.getRealSkillLevel(Skill.MINING) >= 15)
        {
            addEncounter(pool, "Iron ore", rank);
        }
        if (client.getRealSkillLevel(Skill.MINING) >= 85)
        {
            addEncounter(pool, "Runite ore", rank);
        }
        if (client.getRealSkillLevel(Skill.WOODCUTTING) >= 60)
        {
            addEncounter(pool, "Yew logs", rank);
        }
        if (!freelance)
        {
            for (String tier : List.of("Beginner", "Easy", "Medium", "Hard", "Elite", "Master"))
            {
                String clue = "Clue Scroll (" + tier + ")";
                if (catalog.containsKey("targets." + clue + "." + rank))
                {
                    addEncounter(pool, clue, rank);
                }
            }
        }
    }

    private void addEncounter(List<Offer> pool, String encounter, String rank)
    {
        boolean members = client.getWorldType().contains(WorldType.MEMBERS);
        Quest quest = ENCOUNTER_QUESTS.get(encounter);
        if (!members && !List.of("Iron ore", "Runite ore", "Yew logs", "Clue Scroll (Beginner)").contains(encounter))
        {
            return;
        }
        // Keep access-gated encounters out until the client's prerequisites can be verified.
        if (List.of("Nex", "Phantom Muspah", "Hespori", "Skotizo", "The Gauntlet").contains(encounter))
        {
            return;
        }
        if (quest == null || quest.getState(client) == QuestState.FINISHED)
        {
            Offer offer = new Offer();
            offer.encounter = encounter;
            offer.rank = rank;
            offer.cofferFetch = isFetch(encounter);
            pool.add(offer);
        }
    }

    private static String lowerRank(String rank)
    {
        return RANKS.get(Math.max(0, rankOrder(rank) - 1));
    }

    private String declineWarning(int stage, String rank)
    {
        switch (stage)
        {
            case 0: return "Decline this slate? Lose up to 1.00 favour and draw another.";
            case 1: return "Decline again? Lose up to 5.00 favour and draw a zero-favour recovery slate.";
            case 3: return "Decline the return slate? The next slate is your last chance.";
            default: return rank.equals("Adventurer")
                ? "Decline this slate? Your Adventurer favour resets to 0.00%."
                : "Decline this slate? You will be demoted to Rank " + lowerRank(rank) + " at 20.00% favour.";
        }
    }

    private void decline(int slateVersion)
    {
        if (!hasActiveProfile() || ledger.active != null || ledger.offers.isEmpty()
            || ledger.slateVersion != slateVersion)
        {
            return;
        }

        if (ledger.rerollPending)
        {
            ledger.rerollPending = false;
            ledger.refusals = 0;
        }
        ledger.contractStreak = 0;
        switch (ledger.refusals)
        {
            case 0:
                ledger.favour = Math.max(0, ledger.favour - 1000);
                ledger.refusals = 1;
                ledger.message = "I've drawn a fresh slate.";
                break;
            case 1:
                ledger.favour = Math.max(0, ledger.favour - 5000);
                ledger.refusals = 2;
                ledger.message = "Finish one recovery contract. This work pays no favour.";
                break;
            case 3:
                ledger.refusals = 4;
                ledger.message = "One last offer of work. Choose carefully.";
                break;
            default:
                boolean demoted = !ledger.rank.equals("Adventurer");
                ledger.rank = lowerRank(ledger.rank);
                ledger.favour = demoted ? 20000 : 0;
                ledger.refusals = 0;
                ledger.message = demoted ? "You have been demoted to Rank " + ledger.rank + " at 20.00% favour."
                    : "You remain an Adventurer. Your favour has been reset to 0.00%.";
                if (demoted)
                {
                    ledger.offers.clear();
                    resetFreelanceBoard();
                    saveAndRender();
                    return;
                }
                break;
        }
        String message = ledger.message;
        ledger.offers.clear();
        drawOffers();
        ledger.message = message;
        saveAndRender();
    }

    private void accept(int index, int slateVersion)
    {
        if (!hasActiveProfile() || ledger.active != null)
        {
            return;
        }
        if (ledger.slateVersion != slateVersion || index < 0 || index >= ledger.offers.size())
        {
            return;
        }
        Offer selected = ledger.offers.get(index);
        if (selected != null)
        {
            if (selected.oneTimeObjective != null && !oneTimeEligible(findOneTimeObjective(selected.oneTimeObjective)))
            {
                ledger.offers.remove(index);
                ledger.slateVersion++;
                ledger.message = "That objective is no longer eligible.";
                saveAndRender();
                return;
            }
            if (selected.quest != null && Quest.valueOf(selected.quest).getState(client) == QuestState.FINISHED)
            {
                ledger.offers.remove(index);
                ledger.slateVersion++;
                ledger.message = "This quest is already complete.";
                saveAndRender();
                return;
            }
            if (!prepareCoffer(selected))
            {
                return;
            }
            ledger.active = selected;
            observedUnfinishedAchievement = selected.oneTimeObjective != null;
            if ("Slayer's Gauntlet".equals(selected.encounter))
            {
                lastSlayerAssignment = readSlayerAssignment();
                slayerCompletionNotice = false;
                slayerCancellationNotice = false;
            }
            observedUnfinishedQuest = selected.quest != null;
            ledger.rerollPending = false;
            lastObservedXp = selected.encounter == null ? client.getSkillExperience(Skill.valueOf(selected.skill)) : null;
            ledger.offers.clear();
            ledger.recordedXp = 0;
            ledger.message = catalog.getProperty("accepted");
            saveAndRender();
        }
    }

    private void abandon()
    {
        if (hasActiveProfile() && ledger.active != null)
        {
            if (isPromotion(ledger.active))
            {
                ledger.active = null;
                ledger.recordedXp = 0;
                ledger.favour = 99000;
                ledger.message = "The trial was abandoned. Earn back 1.00% favour to retry.";
                saveAndRender();
                return;
            }
            if (ledger.active.freelance)
            {
                removeFreelance(ledger.active);
            }
            ledger.active = null;
            ledger.recordedXp = 0;
            ledger.message = "I've closed the contract. No favour was awarded.";
            saveAndRender();
        }
    }

    private void render()
    {
        if (panel == null)
        {
            return;
        }
        syncScroll();
        final String viewProfile = profile;
        final boolean showFreelance = freelanceBoardOpen;
        final Ledger view = ledger == null ? null : gson.fromJson(gson.toJson(ledger), Ledger.class);
        SwingUtilities.invokeLater(() ->
        {
            panel.removeAll();
            JPanel content = new JPanel();
            content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
            content.setBackground(OAK);
            panel.setBackground(OAK);
            content.setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 8, 8, 8));
            panel.add(content, BorderLayout.NORTH);
            if (view == null)
            {
                content.add(ledgerLabel("Log in to open the ledger.", 13f));
            }
            else
            {
                content.add(ledgerLabel(view.rank.equals("Adventurer") ? view.rank : "Rank " + view.rank, 20f));
                content.add(ledgerLabel("Favour  " + formatFavour(view.favour) + "%", 13f));
                content.add(ledgerLabel("Contract streak  " + view.contractStreak, 13f));
                content.add(Box.createVerticalStrut(9));
                JTextArea message = new JTextArea(view.message);
                message.setEditable(false);
                message.setOpaque(true);
                message.setBackground(LEATHER);
                message.setForeground(PARCHMENT_INK);
                message.setFont(CovenantScroll.calligraphy().deriveFont(13f));
                message.setBorder(BorderFactory.createEmptyBorder(7, 8, 7, 8));
                message.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
                message.setLineWrap(true);
                message.setWrapStyleWord(true);
                content.add(message);
                content.add(Box.createVerticalStrut(10));
                if (view.active != null)
                {
                    Offer active = view.active;
                    JProgressBar progress = new JProgressBar(0, active.target);
                    progress.setValue(view.recordedXp);
                    progress.setStringPainted(true);
                    progress.setString(String.format(Locale.US, "%,d / %,d %s", view.recordedXp, active.target,
                        active.giantChestTrial ? "Bryophyta" : offerUnit(active)));
                    progress.setForeground(BRASS);
                    progress.setBackground(LEATHER);
                    progress.setFont(CovenantScroll.calligraphy().deriveFont(12f));
                    progress.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
                    content.add(ledgerLabel(offerTitle(active), 16f));
                    if ("Slayer's Gauntlet".equals(active.encounter))
                    {
                        content.add(ledgerLabel(active.slayerMaster == null ? "Any Slayer master" : active.slayerMaster, 13f));
                        if (active.slayerAssignment != null)
                        {
                            content.add(ledgerLabel(active.slayerAssignment.monsterName, 12f));
                            content.add(ledgerLabel(active.slayerAssignment.remaining + " / "
                                + active.slayerAssignment.original + " kills remaining", 12f));
                        }
                    }
                    if (active.rankSTrial == RankSTrial.PET_AND_CLUES)
                    {
                        addTrialProgress(content, "New eligible pet", active.rankSVerifiedItems.isEmpty() ? 0 : 1);
                    }
                    else if (active.rankSTrial == RankSTrial.REWARDS)
                    {
                        addTrialProgress(content, "Dizana's Quiver",
                            active.rankSVerifiedItems.contains(net.runelite.api.ItemID.DIZANAS_QUIVER) ? 1 : 0);
                        addTrialProgress(content, "Infernal Cape",
                            active.rankSVerifiedItems.contains(net.runelite.api.ItemID.INFERNAL_CAPE) ? 1 : 0);
                        addTrialProgress(content, "Mage Arena II cape", active.rankSGodCape ? 1 : 0);
                    }
                    if (active.rankAStandard)
                    {
                        content.add(ledgerLabel(active.rankAPetVerified ? "Pet verified" : "New skilling pet needed", 13f));
                    }
                    if (active.capeTrial != null)
                    {
                        content.add(ledgerLabel((active.fireCapeVerified ? "Verified: " : "Needed: ") + "Fire Cape log", 12f));
                        content.add(ledgerLabel((active.godCapeVerified ? "Verified: " : "Needed: ") + "Mage Arena I cape", 12f));
                    }
                    if (active.armoryPieces != null)
                    {
                        content.add(ledgerLabel("Guild Coffer: bank tab " + active.cofferTab, 13f));
                        for (ArmoryPiece piece : active.armoryPieces)
                        {
                            content.add(ledgerLabel((piece.logged && piece.deposited ? "Done: " : "Needed: ")
                                + piece.name, 12f));
                        }
                    }
                    if (active.cofferFetch && active.cofferTab > 0)
                    {
                        content.add(ledgerLabel(String.format(Locale.US, "Guild Coffer: bank tab %d", active.cofferTab), 13f));
                        content.add(ledgerLabel(String.format(Locale.US, "Bank total needed: %,d",
                            active.bankBaseline + active.target), 13f));
                    }
                    progress.setBorder(BorderFactory.createLineBorder(BRASS));
                    progress.setPreferredSize(new java.awt.Dimension(180, 24));
                    content.add(Box.createVerticalStrut(5));
                    content.add(progress);
                    if (active.giantChestTrial)
                    {
                        JProgressBar obor = new JProgressBar(0, 15);
                        obor.setValue(active.oborChests);
                        obor.setStringPainted(true);
                        obor.setString(active.oborChests + " / 15 Obor");
                        obor.setForeground(BRASS);
                        obor.setBackground(LEATHER);
                        obor.setFont(progress.getFont());
                        obor.setBorder(progress.getBorder());
                        obor.setPreferredSize(progress.getPreferredSize());
                        obor.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
                        content.add(Box.createVerticalStrut(5));
                        content.add(obor);
                    }
                    content.add(ledgerLabel((int) (100L * view.recordedXp / active.target) + "% completed", 13f));
                    content.add(Box.createVerticalStrut(15));
                    if (canReroll(view))
                    {
                        button(content, "Reroll contract", () ->
                        {
                            if (JOptionPane.showConfirmDialog(panel, "Replace this contract and give up its progress?",
                                "Reroll contract", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION)
                            {
                                forProfile(viewProfile, this::reroll);
                            }
                        });
                    }
                    JButton abandonButton = ledgerButton("Abandon");
                    abandonButton.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(BRASS, 1, true), BorderFactory.createEmptyBorder(4, 12, 4, 12)));
                    abandonButton.setMaximumSize(abandonButton.getPreferredSize());
                    abandonButton.addActionListener(event ->
                    {
                        if (JOptionPane.showConfirmDialog(panel, isPromotion(active)
                            ? "Abandon the trial? Favour will fall to 99.00%." : "Abandon the active contract?", "Guild Master",
                            JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION)
                        {
                            forProfile(viewProfile, this::abandon);
                        }
                    });
                    content.add(abandonButton);
                }
                else
                {
                    button(content, "Contract Request", () -> forProfile(viewProfile, this::request));
                    JButton freelance = ledgerButton("Freelance Board");
                    freelance.addActionListener(event -> forProfile(viewProfile, () ->
                    {
                        freelanceBoardOpen = !freelanceBoardOpen;
                        if (freelanceBoardOpen)
                        {
                            ensureFreelanceBoard();
                            saveAndRender();
                            return;
                        }
                        render();
                    }));
                    content.add(freelance);
                    content.add(Box.createVerticalStrut(8));
                    button(content, "Promotion", () -> forProfile(viewProfile, this::showPromotion));
                }
            }
            if (showFreelance && view != null && view.active == null)
            {
                JScrollPane board = new JScrollPane();
                board.setBorder(BorderFactory.createEmptyBorder());
                board.setPreferredSize(new java.awt.Dimension(200, 260));
                board.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
                board.getVerticalScrollBar().setUnitIncrement(18);
                populateFreelance(board, view, viewProfile);
                panel.add(board, BorderLayout.CENTER);
            }
            panel.revalidate();
            panel.repaint();
        });
    }

    private void acceptBrutusTrial()
    {
        if (!hasActiveProfile() || ledger.active != null || ledger.favour < 100000 || !ledger.rank.equals("Adventurer"))
        {
            return;
        }
        Offer offer = new Offer();
        offer.brutusTrial = true;
        offer.target = 25;
        ledger.active = offer;
        ledger.recordedXp = 0;
        ledger.lastBrutusKillCount = 0;
        ledger.brutusLogUpdate = false;
        ledger.offers.clear();
        ledger.message = "Your Rank D trial has begun.";
        freelanceBoardOpen = false;
        saveAndRender();
    }

    private void acceptGiantChestTrial()
    {
        if (!hasActiveProfile() || ledger.active != null || ledger.favour < 100000 || !"D".equals(ledger.rank))
        {
            return;
        }
        Offer offer = new Offer();
        offer.giantChestTrial = true;
        offer.encounter = "Giant chests";
        offer.rank = "C";
        offer.target = 30;
        ledger.active = offer;
        ledger.recordedXp = 0;
        ledger.offers.clear();
        ledger.message = "Your Rank C trial has begun.";
        freelanceBoardOpen = false;
        saveAndRender();
    }

    @Subscribe
    public void onChatMessage(ChatMessage event)
    {
        if (!hasActiveProfile() || (event.getType() != ChatMessageType.GAMEMESSAGE
            && event.getType() != ChatMessageType.SPAM))
        {
            return;
        }
        String message = Text.removeTags(event.getMessage()).trim();
        useOneTimeCompletionNotice(message);
        if (ledger.active == null)
        {
            return;
        }
        if ("Slayer's Gauntlet".equals(ledger.active.encounter))
        {
            String lower = message.toLowerCase(Locale.ROOT).replace('’', '\'');
            slayerCompletionNotice |= lower.matches(".*you(?:'ve| have) (?:now )?completed (?:your (?:slayer )?task|[0-9,]+ (?:wilderness )?(?:slayer )?tasks?).*");
            slayerCancellationNotice |= lower.matches(".*(?:slayer (?:task|assignment)|your (?:task|assignment)) (?:has been |was |is )?(?:cancelled|canceled|skipped).*");
            return;
        }
        if (ledger.active.rankSTrial != null)
        {
            recordRankSNotice(ledger.active, message);
            return;
        }
        if (ledger.active.rankARaid)
        {
            return;
        }
        if (ledger.active.rankAStandard)
        {
            recordRankAPetNotice(message);
            if (!"King Black Dragon".equals(ledger.active.encounter))
            {
                return;
            }
        }
        if (ledger.active.promotionPetId > 0)
        {
            String petName = ledger.active.promotionPetId == net.runelite.api.ItemID.SCURRY
                ? "Scurry" : "Pet chaos elemental";
            if (message.equalsIgnoreCase("New item added to your collection log: " + petName)
                || message.equalsIgnoreCase("New item added to your collection log: " + petName + "."))
            {
                ledger.active.promotionPetAnnounced = true;
                saveAndRender();
                return;
            }
        }
        if (ledger.active.giantChestTrial)
        {
            trackGiantChest(message);
            return;
        }
        if (ledger.active.encounter != null)
        {
            trackEncounter(message);
            return;
        }
        if (!ledger.active.brutusTrial)
        {
            return;
        }
        Matcher kill = BRUTUS_KILL_COUNT.matcher(message);
        if (kill.matches())
        {
            int count = Integer.parseInt(kill.group(1).replace(",", ""));
            if (count > ledger.lastBrutusKillCount)
            {
                ledger.lastBrutusKillCount = count;
                ledger.recordedXp++;
                if (ledger.recordedXp >= ledger.active.target)
                {
                    verifyCompletion(this::finishBrutusTrial);
                }
                else
                {
                    saveAndRender();
                }
            }
        }
        else if (BRUTUS_LOG_ENTRY.matcher(message).matches())
        {
            ledger.brutusLogUpdate = true;
            saveAndRender();
        }
    }

    private void trackGiantChest(String message)
    {
        Matcher chest = GIANT_CHEST_COUNT.matcher(message);
        if (!chest.matches())
        {
            return;
        }
        Offer trial = ledger.active;
        int count = Integer.parseInt(chest.group(2).replace(",", ""));
        if ("Obor".equalsIgnoreCase(chest.group(1)))
        {
            if (count <= trial.lastOborChestCount || trial.oborChests == 15)
            {
                return;
            }
            trial.lastOborChestCount = count;
            if (++trial.oborChests == 15)
            {
                trial.target = 15;
            }
        }
        else
        {
            if (count <= trial.lastBryophytaChestCount)
            {
                return;
            }
            trial.lastBryophytaChestCount = count;
            ledger.recordedXp++;
        }
        if (ledger.recordedXp >= trial.target)
        {
            verifyCompletion(() -> finishPromotion("C"));
        }
        else
        {
            saveAndRender();
        }
    }

    private String chooseSlayerMaster(String rank, int scope)
    {
        if (rankOrder(rank) < rankOrder("C") || scope == 2)
        {
            return null;
        }
        List<String> masters = new ArrayList<>();
        int combat = client.getLocalPlayer().getCombatLevel();
        if (combat >= 20 && Quest.PRIEST_IN_PERIL.getState(client) == QuestState.FINISHED) masters.add("Mazchna");
        if (combat >= 40) masters.add("Vannaka");
        if (rankOrder(rank) >= rankOrder("B") && combat >= 70 && Quest.LOST_CITY.getState(client) == QuestState.FINISHED)
        {
            masters.add("Chaeldar");
        }
        if (rankOrder(rank) >= rankOrder("A"))
        {
            masters.add("Krystilia");
            if (combat >= 75) masters.add("Konar");
            if (combat >= 85) masters.add("Nieve/Steve");
            if (combat >= 100 && client.getRealSkillLevel(Skill.SLAYER) >= 50
                && Quest.SHILO_VILLAGE.getState(client) == QuestState.FINISHED)
            {
                masters.add("Duradel/Kuradal");
            }
        }
        Collections.shuffle(masters);
        return masters.isEmpty() || java.util.concurrent.ThreadLocalRandom.current().nextBoolean() ? null : masters.get(0);
    }

    private SlayerAssignment readSlayerAssignment()
    {
        SlayerAssignment assignment = new SlayerAssignment();
        assignment.remaining = client.getVarpValue(VarPlayerID.SLAYER_COUNT);
        assignment.original = client.getVarpValue(VarPlayerID.SLAYER_COUNT_ORIGINAL);
        assignment.monster = client.getVarpValue(VarPlayerID.SLAYER_TARGET);
        assignment.bossVariant = assignment.monster == 98 ? client.getVarbitValue(4723) : 0;
        assignment.area = client.getVarpValue(VarPlayerID.SLAYER_AREA);
        assignment.master = client.getVarbitValue(VarbitID.SLAYER_MASTER);
        assignment.streak = slayerStreak(assignment.master);
        net.runelite.api.widgets.Widget name = client.getWidget(InterfaceID.ChatLeft.NAME);
        if (name != null && !name.isHidden())
        {
            assignment.issuer = Text.removeTags(name.getText()).trim();
        }
        return assignment;
    }

    private int slayerStreak(int master)
    {
        switch (master)
        {
            case 7: return client.getVarbitValue(VarbitID.SLAYER_WILDERNESS_TASKS_COMPLETED);
            case 10: return client.getVarpValue(VarPlayerID.SLAYER_MORTIMER_TASKS_COMPLETED);
            default: return client.getVarbitValue(VarbitID.SLAYER_TASKS_COMPLETED);
        }
    }

    private String slayerMonsterName(SlayerAssignment assignment)
    {
        int table = assignment.monster == 98 ? 116 : 113;
        int column = assignment.monster == 98 ? 1 : 0;
        int value = assignment.monster == 98 ? assignment.bossVariant : assignment.monster;
        List<Integer> rows = client.getDBRowsByValue(table, column, 0, value);
        if (!rows.isEmpty())
        {
            int row = rows.get(0);
            if (assignment.monster == 98)
            {
                Object[] target = client.getDBTableField(row, 4, 0);
                if (target.length == 0 || !(target[0] instanceof Integer))
                {
                    return "Slayer target " + assignment.monster;
                }
                row = (Integer) target[0];
            }
            Object[] names = client.getDBTableField(row, 10, 0);
            if (names.length > 0 && names[0] instanceof String)
            {
                return (String) names[0];
            }
        }
        return "Slayer target " + assignment.monster;
    }

    private static boolean slayerIssuerMatches(String required, String actual)
    {
        if (required == null)
        {
            return true;
        }
        if (actual == null)
        {
            return false;
        }
        for (String alias : required.split("/"))
        {
            if (actual.equalsIgnoreCase(alias) || actual.toLowerCase(Locale.ROOT).startsWith(alias.toLowerCase(Locale.ROOT) + " "))
            {
                return true;
            }
        }
        return false;
    }

    private void trackSlayer(Offer offer)
    {
        SlayerAssignment current = readSlayerAssignment();
        SlayerAssignment previous = lastSlayerAssignment;
        lastSlayerAssignment = current;
        boolean confirmedComplete = slayerCompletionNotice;
        boolean confirmedCancelled = slayerCancellationNotice;
        slayerCompletionNotice = false;
        slayerCancellationNotice = false;
        SlayerAssignment tracked = offer.slayerAssignment;
        if (tracked == null)
        {
            boolean newlyIssued = previous != null && current.remaining > 0 && current.original >= current.remaining
                && (previous.remaining == 0 || !current.sameTask(previous) || current.remaining > previous.remaining);
            if (newlyIssued && slayerIssuerMatches(offer.slayerMaster, current.issuer))
            {
                current.monsterName = slayerMonsterName(current);
                offer.slayerAssignment = current;
                offer.slayerUnverified = false;
                ledger.message = "Tracking " + current.original + " " + current.monsterName + ".";
                saveAndRender();
            }
            return;
        }
        if (confirmedCancelled || (current.remaining > 0
            && (!current.sameTask(tracked) || current.remaining > tracked.remaining)))
        {
            ledger.active = null;
            ledger.recordedXp = 0;
            ledger.message = "The tracked Slayer assignment was cancelled or replaced. No favour awarded.";
            saveAndRender();
            return;
        }
        if (current.remaining == 0 && previous != null && (confirmedComplete
            || slayerStreak(tracked.master) == tracked.streak + 1))
        {
            offer.slayerAssignment = null;
            if (!offer.slayerUnverified)
            {
                ledger.message = "Slayer assignment verified. Ask for a new assignment when ready.";
                advanceEncounter();
            }
            else
            {
                ledger.message = "Assignment finished without contract credit after a tracking gap. Ask for a new assignment.";
                saveAndRender();
            }
            return;
        }
        if (previous == null && current.remaining != tracked.remaining
            && !offer.slayerUnverified)
        {
            offer.slayerUnverified = true;
            ledger.message = "Slayer assignment progress changed without verification.";
            saveAndRender();
        }
        else if (current.remaining == 0 && previous != null && previous.remaining > 0)
        {
            ledger.message = "Waiting for confirmation of the Slayer assignment outcome.";
            saveAndRender();
        }
        if (current.remaining > 0 && current.remaining != tracked.remaining)
        {
            tracked.remaining = current.remaining;
            saveAndRender();
        }
    }

    private void trackEncounter(String message)
    {
        Offer offer = ledger.active;
        if (offer.cofferFetch)
        {
            return;
        }
        if (offer.quest != null)
        {
            return;
        }
        if (isFetch(offer.encounter))
        {
            String expected = "Iron ore".equals(offer.encounter) ? "You manage to mine some iron."
                : "Runite ore".equals(offer.encounter) ? "You manage to mine some runite."
                : "You get some yew logs.";
            if (message.equalsIgnoreCase(expected))
            {
                advanceEncounter();
            }
            return;
        }
        if (LOOT_CLEARS.contains(offer.encounter) || offer.encounter.startsWith("Clue Scroll ("))
        {
            return;
        }
        if ("Guardians of the Rift".equals(offer.encounter)
            && message.matches("(?i)(?:The )?Great Guardian has been defeated[.!]?"))
        {
            removeFreelance(offer);
            ledger.active = null;
            ledger.recordedXp = 0;
            ledger.message = "The Great Guardian fell. No favour was awarded.";
            saveAndRender();
            return;
        }
        String prefix = "Guardians of the Rift".equals(offer.encounter)
            ? "Amount of Rifts you have closed"
            : "Your " + offer.encounter + " " + (offer.encounter.equals("Barrows") ? "chest" : "kill") + " count is";
        if (!message.regionMatches(true, 0, prefix, 0, prefix.length()))
        {
            return;
        }
        String countText = message.substring(prefix.length()).replaceFirst("^:?[ ]+", "")
            .replaceFirst("\\.$", "").replace(",", "");
        if (!countText.matches("[0-9]{1,9}"))
        {
            return;
        }
        int count = Integer.parseInt(countText);
        if (count <= offer.lastEncounterCount)
        {
            return;
        }
        offer.lastEncounterCount = count;
        advanceEncounter();
    }

    @Subscribe
    public void onLootReceived(LootReceived event)
    {
        if (!hasActiveProfile() || ledger.active == null || ledger.active.encounter == null)
        {
            return;
        }
        String encounter = ledger.active.encounter;
        if (ledger.active.rankSTrial != null)
        {
            Offer trial = ledger.active;
            if (trial.rankSTrial == RankSTrial.PET_AND_CLUES && "Clue Scroll (Master)".equals(event.getName())
                && ledger.recordedXp < trial.target)
            {
                ledger.recordedXp++;
                trackRankSTrial(trial);
                saveAndRender();
            }
            else if (trial.rankSTrial == RankSTrial.REWARDS && "Fortis Colosseum".equals(event.getName())
                && event.getItems().stream().anyMatch(item -> item.getId() == net.runelite.api.ItemID.DIZANAS_QUIVER)
                && !trial.rankSVerifiedItems.contains(net.runelite.api.ItemID.DIZANAS_QUIVER))
            {
                trial.rankSVerifiedItems.add(net.runelite.api.ItemID.DIZANAS_QUIVER);
                trackRankSTrial(trial);
                saveAndRender();
            }
            return;
        }
        if (ledger.active.rankARaid || ledger.active.rankAStandard)
        {
            trackRankALoot(event);
            return;
        }
        if ((LOOT_CLEARS.contains(encounter) || encounter.startsWith("Clue Scroll ("))
            && encounter.equalsIgnoreCase(event.getName()))
        {
            advanceEncounter();
        }
    }

    private static boolean isFetch(String encounter)
    {
        return "Iron ore".equals(encounter) || "Runite ore".equals(encounter)
            || "Yew logs".equals(encounter) || "Oak logs".equals(encounter);
    }

    private static final class CofferSnapshot
    {
        int tab;
        long bankQuantity;
        long tabQuantity;
    }

    private CofferSnapshot readCoffer(String material)
    {
        int itemId = material == null ? -1 : "Iron ore".equals(material) ? ItemID.IRON_ORE
            : "Runite ore".equals(material) ? ItemID.RUNITE_ORE
            : "Oak logs".equals(material) ? ItemID.OAK_LOGS : ItemID.YEW_LOGS;
        return readCofferItem(itemId, false);
    }

    private CofferSnapshot readCofferItem(int itemId, boolean includeVariants)
    {
        ItemContainer bank = client.getItemContainer(InventoryID.BANK);
        net.runelite.api.widgets.Widget bankItems = client.getWidget(InterfaceID.Bankmain.ITEMS);
        if (bank == null || bankItems == null || bankItems.isHidden())
        {
            return null;
        }
        Item[] items = bank.getItems();
        int start = 0;
        int end = 0;
        CofferSnapshot snapshot = new CofferSnapshot();
        for (int varbit = VarbitID.BANK_TAB_1; varbit <= VarbitID.BANK_TAB_9; varbit++)
        {
            int size = client.getVarbitValue(varbit);
            if (size > 0)
            {
                snapshot.tab = varbit - VarbitID.BANK_TAB_1 + 1;
                start = end;
                end += size;
            }
        }
        if (snapshot.tab == 0 || end > items.length)
        {
            return null;
        }
        if (itemId == -1)
        {
            return snapshot;
        }
        int canonicalId = includeVariants ? ItemVariationMapping.map(itemId) : itemId;
        for (int slot = 0; slot < items.length; slot++)
        {
            Item item = items[slot];
            if (item != null && (includeVariants ? ItemVariationMapping.map(item.getId()) : item.getId()) == canonicalId)
            {
                snapshot.bankQuantity += item.getQuantity();
                if (slot >= start && slot < end)
                {
                    snapshot.tabQuantity += item.getQuantity();
                }
            }
        }
        return snapshot;
    }

    private boolean prepareCoffer(Offer offer)
    {
        if (!offer.cofferFetch)
        {
            return true;
        }
        CofferSnapshot snapshot = readCoffer(offer.encounter);
        if (snapshot == null)
        {
            cofferReady = false;
            ledger.message = "Open your bank with an existing end tab to confirm this Fetch contract.";
            render();
            return false;
        }
        setCofferBaseline(offer, snapshot);
        return true;
    }

    private static void setCofferBaseline(Offer offer, CofferSnapshot snapshot)
    {
        offer.cofferTab = snapshot.tab;
        offer.bankBaseline = snapshot.bankQuantity;
        offer.cofferBaseline = snapshot.tabQuantity;
    }

    private void trackCoffer(Offer offer)
    {
        CofferSnapshot snapshot = readCoffer(offer.encounter);
        if (snapshot == null)
        {
            return;
        }
        if (offer.cofferTab == 0)
        {
            setCofferBaseline(offer, snapshot);
            ledger.message = "Starting stock recorded. Deposit new materials in the Guild Coffer.";
            saveAndRender();
            return;
        }
        if (snapshot.tab != offer.cofferTab)
        {
            return;
        }
        long added = Math.min(snapshot.bankQuantity - offer.bankBaseline, snapshot.tabQuantity - offer.cofferBaseline);
        int progress = (int) Math.max(0, Math.min(offer.target, added));
        if (progress == ledger.recordedXp)
        {
            return;
        }
        ledger.recordedXp = progress;
        if (progress == offer.target)
        {
            verifyCompletion(offer.freelance ? this::finishFreelance : this::finishIssuedContract);
        }
        else
        {
            saveAndRender();
        }
    }

    private void advanceEncounter()
    {
        if (ledger.active.rankAStandard)
        {
            if (ledger.recordedXp < ledger.active.target)
            {
                ledger.recordedXp++;
                trackRankATrial(ledger.active);
                saveAndRender();
            }
            return;
        }
        ledger.recordedXp++;
        if (ledger.recordedXp == ledger.active.target)
        {
            if (ledger.active.promotionPetId > 0)
            {
                verifyCompletion(() -> finishPromotion("B"));
            }
            else
            {
                verifyCompletion(ledger.active.freelance ? this::finishFreelance : this::finishIssuedContract);
            }
            return;
        }
        saveAndRender();
    }

    private static String encounterUnit(String encounter)
    {
        return "Barrows".equals(encounter) ? "chests" : "kills";
    }

    private boolean activeBrutusTrial()
    {
        return hasActiveProfile() && ledger.active != null && ledger.active.brutusTrial;
    }

    private void checkBrutusLog()
    {
        if (!ledger.brutusLogUpdate)
        {
            return;
        }
        ItemContainer log = client.getItemContainer(InventoryID.COLLECTION_TRANSMIT);
        if (log != null && log.count(ItemID.BOTTOMLESS_MILK_BUCKET) > 0
            && log.count(ItemID.COW_SLIPPERS) > 0 && log.count(ItemID.MOOLETA) > 0
            && log.count(BRUTUS_PET_ITEM) > 0)
        {
            verifyCompletion(this::finishBrutusTrial);
        }
    }

    private void finishBrutusTrial()
    {
        finishPromotion("D");
    }

    private void finishPromotion(String rank)
    {
        ledger.active = null;
        ledger.recordedXp = 0;
        ledger.favour = 0;
        ledger.rank = rank;
        ledger.armoryRequest = null;
        ledger.rankAObjective = null;
        ledger.collectionTrialBoss = null;
        ledger.refusals = 0;
        ledger.message = "Well done. Your trial is complete, and the Covenant salutes you as Rank " + rank + ".";
        resetFreelanceBoard();
        ledger.freelanceLocked = false;
    }

    private CovenantScroll.Card armoryCard(String expectedProfile)
    {
        if (ledger.armoryRequest == null)
        {
            List<Integer> pieces = new ArrayList<>(BARROWS_COMPONENTS);
            Collections.shuffle(pieces);
            ledger.armoryRequest = new ArrayList<>(pieces.subList(0, 3));
            configuration.setRSProfileConfiguration(GROUP, LEDGER_KEY, gson.toJson(ledger));
        }
        List<Integer> request = List.copyOf(ledger.armoryRequest);
        List<String> names = new ArrayList<>();
        for (int id : request)
        {
            names.add(client.getItemDefinition(id).getName());
        }
        String detail = "The armory is missing " + String.join(", ", names)
            + ". Please restore them to the coffer. Open the bank to accept and check the Barrows collection log.";
        return new CovenantScroll.Card("Armory", "Rank B", request.size() + " Barrows pieces", "Promotion",
            detail, null, () -> forProfile(expectedProfile, () -> acceptArmory(request)), () -> cofferReady);
    }

    private void acceptArmory(List<Integer> request)
    {
        if (!hasActiveProfile() || ledger.active != null || ledger.favour < 100000 || !"C".equals(ledger.rank)
            || !request.equals(ledger.armoryRequest))
        {
            return;
        }
        Offer trial = new Offer();
        trial.encounter = "Armory";
        trial.rank = "B";
        trial.target = request.size();
        trial.armoryPieces = new ArrayList<>();
        for (int id : request)
        {
            CofferSnapshot snapshot = readCofferItem(id, true);
            if (snapshot == null)
            {
                cofferReady = false;
                return;
            }
            trial.cofferTab = snapshot.tab;
            ArmoryPiece piece = new ArmoryPiece();
            piece.itemId = id;
            piece.name = client.getItemDefinition(id).getName();
            piece.bankBaseline = snapshot.bankQuantity;
            piece.cofferBaseline = snapshot.tabQuantity;
            trial.armoryPieces.add(piece);
        }
        ledger.active = trial;
        ledger.recordedXp = 0;
        ledger.offers.clear();
        freelanceBoardOpen = false;
        ledger.message = "Your Rank B armory trial has begun. Open the Barrows collection log to verify your pieces.";
        saveAndRender();
    }

    private void trackArmory(Offer trial)
    {
        ItemContainer log = client.getItemContainer(InventoryID.COLLECTION_TRANSMIT);
        int progress = 0;
        boolean changed = false;
        boolean bankVerified = true;
        for (ArmoryPiece piece : trial.armoryPieces)
        {
            if (!piece.logged && log != null && log.count(piece.itemId) > 0)
            {
                piece.logged = true;
                changed = true;
            }
            CofferSnapshot snapshot = readCofferItem(piece.itemId, true);
            if (snapshot == null || snapshot.tab != trial.cofferTab)
            {
                bankVerified = false;
            }
            else
            {
                boolean deposited = snapshot.bankQuantity > piece.bankBaseline
                    && snapshot.tabQuantity > piece.cofferBaseline;
                changed |= piece.deposited != deposited;
                piece.deposited = deposited;
            }
            if (piece.logged && piece.deposited)
            {
                progress++;
            }
        }
        ledger.recordedXp = progress;
        if (bankVerified && progress == trial.target && !trial.rankAStandard)
        {
            verifyCompletion(() -> finishPromotion("B"));
        }
        else if (changed)
        {
            saveAndRender();
        }
    }

    private List<CovenantScroll.Card> rankATrialCards(String expectedProfile)
    {
        if (!client.getWorldType().contains(WorldType.MEMBERS))
        {
            return Collections.emptyList();
        }
        if (ledger.rankAObjective == null)
        {
            List<String> objectives = new ArrayList<>(List.of("King Black Dragon", "Clue caskets"));
            if (Quest.PERILOUS_MOONS.getState(client) == QuestState.FINISHED)
            {
                objectives.add("Perilous Moons");
                objectives.add("Moons armory");
            }
            Collections.shuffle(objectives);
            ledger.rankAObjective = objectives.get(0);
            if ("Moons armory".equals(ledger.rankAObjective))
            {
                List<Integer> pieces = new ArrayList<>(MOONS_COMPONENTS);
                Collections.shuffle(pieces);
                ledger.armoryRequest = new ArrayList<>(pieces.subList(0, 3));
            }
            configuration.setRSProfileConfiguration(GROUP, LEDGER_KEY, gson.toJson(ledger));
        }
        String objective = ledger.rankAObjective;
        boolean armory = "Moons armory".equals(objective);
        String requirement;
        switch (objective)
        {
            case "King Black Dragon": requirement = "Defeat King Black Dragon 100 times."; break;
            case "Clue caskets": requirement = "Open 50 Medium, Hard, Elite, or Master clue caskets."; break;
            case "Perilous Moons": requirement = "Defeat all three Moons and loot the Lunar Chest on 25 runs."; break;
            default:
                List<String> names = new ArrayList<>();
                for (int id : ledger.armoryRequest)
                {
                    names.add(client.getItemDefinition(id).getName());
                }
                requirement = "The armory is missing " + String.join(", ", names)
                    + ". Please restore them to the coffer. Open your bank to confirm.";
                break;
        }
        return List.of(
            new CovenantScroll.Card(objective, "Rank A", "Objective + new pet", "Promotion",
                requirement + " Also obtain a new eligible skilling pet and open its Collection Log.",
                null, () -> forProfile(expectedProfile, () -> acceptRankATrial(false)), () -> !armory || cofferReady),
            new CovenantScroll.Card("Raid trial", "Rank A", "1 personal raid unique", "Promotion",
                "Receive a unique reward in your own raid loot from Chambers of Xeric, Theatre of Blood, or Tombs of Amascut. Open that raid's Collection Log to verify it.",
                null, () -> forProfile(expectedProfile, () -> acceptRankATrial(true))));
    }

    private void acceptRankATrial(boolean raid)
    {
        if (!hasActiveProfile() || ledger.active != null || ledger.favour < 100000 || !"B".equals(ledger.rank)
            || !client.getWorldType().contains(WorldType.MEMBERS) || ledger.rankAObjective == null)
        {
            return;
        }
        Offer trial = new Offer();
        trial.rankARaid = raid;
        trial.rankAStandard = !raid;
        trial.encounter = raid ? "Raid trial" : ledger.rankAObjective;
        trial.rank = "A";
        switch (trial.encounter)
        {
            case "Raid trial": trial.target = 1; break;
            case "King Black Dragon": trial.target = 100; break;
            case "Clue caskets": trial.target = 50; break;
            case "Perilous Moons": trial.target = 25; break;
            case "Moons armory":
                trial.armoryPieces = new ArrayList<>();
                for (int id : ledger.armoryRequest)
                {
                    CofferSnapshot snapshot = readCofferItem(id, true);
                    if (snapshot == null)
                    {
                        cofferReady = false;
                        return;
                    }
                    trial.cofferTab = snapshot.tab;
                    ArmoryPiece piece = new ArmoryPiece();
                    piece.itemId = id;
                    piece.name = client.getItemDefinition(id).getName();
                    piece.bankBaseline = snapshot.bankQuantity;
                    piece.cofferBaseline = snapshot.tabQuantity;
                    trial.armoryPieces.add(piece);
                }
                trial.target = trial.armoryPieces.size();
                break;
            default: return;
        }
        ledger.active = trial;
        ledger.recordedXp = 0;
        ledger.offers.clear();
        freelanceBoardOpen = false;
        ledger.message = "Your Rank A trial has begun.";
        saveAndRender();
    }

    private void recordRankAPetNotice(String message)
    {
        Offer trial = ledger.active;
        String prefix = "New item added to your collection log: ";
        if (trial.rankAPetVerified || !message.regionMatches(true, 0, prefix, 0, prefix.length()))
        {
            return;
        }
        for (int id : SKILLING_PETS)
        {
            String name = client.getItemDefinition(id).getName();
            if (message.equalsIgnoreCase(prefix + name) || message.equalsIgnoreCase(prefix + name + "."))
            {
                trial.rankAPendingItem = id;
                saveAndRender();
                return;
            }
        }
    }

    private void trackRankALoot(LootReceived event)
    {
        Offer trial = ledger.active;
        if (trial.rankARaid)
        {
            List<Integer> rewards = RAID_REWARDS.get(event.getName());
            if (rewards != null && trial.rankAPendingItem == 0)
            {
                for (net.runelite.client.game.ItemStack item : event.getItems())
                {
                    if (rewards.contains(item.getId()))
                    {
                        trial.rankAPendingItem = item.getId();
                        saveAndRender();
                        break;
                    }
                }
            }
            return;
        }
        if (("Clue caskets".equals(trial.encounter)
                && event.getName().matches("Clue Scroll \\((Medium|Hard|Elite|Master)\\)"))
            || ("Perilous Moons".equals(trial.encounter) && "Lunar Chest".equals(event.getName())
                && client.getVarbitValue(VarbitID.PMOON_BOSS_BLOOD_DEAD) == 1
                && client.getVarbitValue(VarbitID.PMOON_BOSS_BLUE_DEAD) == 1
                && client.getVarbitValue(VarbitID.PMOON_BOSS_ECLIPSE_DEAD) == 1))
        {
            advanceEncounter();
        }
    }

    private void trackRankATrial(Offer trial)
    {
        ItemContainer log = client.getItemContainer(InventoryID.COLLECTION_TRANSMIT);
        if (trial.rankAPendingItem > 0 && log != null && log.count(trial.rankAPendingItem) > 0)
        {
            if (trial.rankARaid)
            {
                ledger.recordedXp = 1;
                verifyCompletion(() -> finishPromotion("A"));
                return;
            }
            trial.rankAPetVerified = true;
            trial.rankAPendingItem = 0;
            saveAndRender();
        }
        if (trial.armoryPieces != null)
        {
            trackArmory(trial);
            CofferSnapshot coffer = readCoffer(null);
            if (coffer == null || coffer.tab != trial.cofferTab)
            {
                return;
            }
        }
        if (trial.rankAStandard && trial.rankAPetVerified && ledger.recordedXp >= trial.target)
        {
            verifyCompletion(() -> finishPromotion("A"));
        }
    }

    private CovenantScroll.Card capeCard(String expectedProfile)
    {
        return new CovenantScroll.Card("Cape trial", "Rank B", "Fire Cape + 1 god cape", "Promotion",
            "Show a Fire Cape entry in TzTok-Jad's Collection Log and any one Mage Arena I god cape in your bank. Existing entries and capes count.",
            null, () -> forProfile(expectedProfile, this::acceptCapeTrial));
    }

    private void acceptCapeTrial()
    {
        if (!hasActiveProfile() || ledger.active != null || ledger.favour < 100000 || !"C".equals(ledger.rank)
            || !client.getWorldType().contains(WorldType.MEMBERS))
        {
            return;
        }
        Offer trial = new Offer();
        trial.encounter = "Cape trial";
        trial.capeTrial = "Cape set";
        trial.rank = "B";
        trial.target = 2;
        ledger.active = trial;
        ledger.recordedXp = 0;
        ledger.offers.clear();
        freelanceBoardOpen = false;
        ledger.message = "Open TzTok-Jad's Collection Log and your bank to verify both capes.";
        saveAndRender();
    }

    private void trackCapeTrial(Offer trial)
    {
        if (trial.target != 2)
        {
            trial.encounter = "Cape trial";
            trial.target = 2;
            ledger.recordedXp = 0;
            ledger.message = "Open TzTok-Jad's Collection Log and your bank to verify both capes.";
            saveAndRender();
        }
        ItemContainer log = client.getItemContainer(InventoryID.COLLECTION_TRANSMIT);
        trial.fireCapeVerified |= log != null && log.count(ItemID.TZHAAR_CAPE_FIRE) > 0;
        ItemContainer bank = client.getItemContainer(InventoryID.BANK);
        net.runelite.api.widgets.Widget bankItems = client.getWidget(InterfaceID.Bankmain.ITEMS);
        if (bank != null && bankItems != null && !bankItems.isHidden())
        {
            trial.godCapeVerified = bank.count(ItemID.SARADOMIN_CAPE) > 0 || bank.count(ItemID.GUTHIX_CAPE) > 0
                || bank.count(ItemID.ZAMORAK_CAPE) > 0;
        }
        int progress = (trial.fireCapeVerified ? 1 : 0) + (trial.godCapeVerified ? 1 : 0);
        if (progress == 2)
        {
            ledger.recordedXp = progress;
            verifyCompletion(() -> finishPromotion("B"));
        }
        else if (progress != ledger.recordedXp)
        {
            ledger.recordedXp = progress;
            saveAndRender();
        }
    }

    private CovenantScroll.Card bossTrialCard(String expectedProfile, String boss, int target, int petId)
    {
        return new CovenantScroll.Card(boss, "Rank B", target + " kills or new pet", "Promotion",
            "Defeat " + boss + " " + target + " times after accepting. Keep kill count and Collection Log chat notifications enabled. For a new pet, open the boss's Collection Log to verify the entry.",
            null, () -> forProfile(expectedProfile, () ->
            {
                if (!hasActiveProfile() || ledger.active != null || ledger.favour < 100000 || !"C".equals(ledger.rank)
                    || !client.getWorldType().contains(WorldType.MEMBERS))
                {
                    return;
                }
                Offer trial = new Offer();
                trial.encounter = boss;
                trial.target = target;
                trial.rank = "B";
                trial.promotionPetId = petId;
                ledger.active = trial;
                ledger.recordedXp = 0;
                ledger.offers.clear();
                freelanceBoardOpen = false;
                ledger.message = "Your Rank B boss trial has begun.";
                saveAndRender();
            }));
    }

    private boolean isCollectionTrialEligible(String boss)
    {
        Integer completionFlag = RANK_C_LOGS.get(boss);
        return completionFlag != null && client.getVarbitValue(completionFlag) != 1
            && (!"Hespori".equals(boss) || client.getRealSkillLevel(Skill.FARMING) >= 65);
    }

    private void addCollectionTrialCard(List<CovenantScroll.Card> trials, String expectedProfile)
    {
        if (ledger.collectionTrialBoss == null || !isCollectionTrialEligible(ledger.collectionTrialBoss))
        {
            List<String> eligible = new ArrayList<>(RANK_C_LOGS.keySet());
            eligible.removeIf(boss -> !isCollectionTrialEligible(boss));
            Collections.shuffle(eligible);
            ledger.collectionTrialBoss = eligible.isEmpty() ? null : eligible.get(0);
            configuration.setRSProfileConfiguration(GROUP, LEDGER_KEY, gson.toJson(ledger));
            if (eligible.isEmpty())
            {
                return;
            }
        }
        String boss = ledger.collectionTrialBoss;
        trials.add(new CovenantScroll.Card(boss + " log", "Rank B", "Complete the log", "Promotion",
            "Complete the " + boss + " Collection Log. Your existing entries count toward the full log.",
            null, () -> forProfile(expectedProfile, () ->
            {
                if (!hasActiveProfile() || ledger.active != null || ledger.favour < 100000 || !"C".equals(ledger.rank)
                    || !client.getWorldType().contains(WorldType.MEMBERS) || !boss.equals(ledger.collectionTrialBoss))
                {
                    return;
                }
                if (!isCollectionTrialEligible(boss))
                {
                    showPromotion();
                    return;
                }
                Offer trial = new Offer();
                trial.encounter = boss + " Collection Log";
                trial.rank = "B";
                trial.target = 1;
                trial.promotionLogVarbit = RANK_C_LOGS.get(boss);
                ledger.active = trial;
                ledger.recordedXp = 0;
                ledger.offers.clear();
                freelanceBoardOpen = false;
                ledger.message = "Your Rank B Collection Log trial has begun.";
                saveAndRender();
            })));
    }

    private static void addTrialProgress(JPanel content, String label, int amount)
    {
        JProgressBar progress = new JProgressBar(0, 1);
        progress.setValue(amount);
        progress.setStringPainted(true);
        progress.setString(label + "  " + amount + "/1");
        progress.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        content.add(progress);
    }

    private List<CovenantScroll.Card> rankSTrialCards(String expectedProfile)
    {
        if (!client.getWorldType().contains(WorldType.MEMBERS))
        {
            return Collections.emptyList();
        }
        List<CovenantScroll.Card> trials = new ArrayList<>();
        trials.add(rankSTrialCard(expectedProfile, RankSTrial.PET_AND_CLUES, "Pet and clues", "New pet + 10 Master caskets",
            "Open Master caskets after accepting and unlock an eligible pet. Open its Collection Log page to verify the pet."));
        if (SKILLCAPES.keySet().stream().filter(skill -> client.getRealSkillLevel(skill) < 99).count() >= 2)
        {
            trials.add(rankSTrialCard(expectedProfile, RankSTrial.SKILLCAPES, "Skillcapes", "2 new skillcapes",
                "Reach 99 in two skills below 99 at acceptance and obtain their capes. Hitpoints is excluded. Either trim status counts."));
        }
        trials.add(rankSTrialCard(expectedProfile, RankSTrial.REWARDS, "Three rewards", "Quiver + two capes",
            "Open the bank to accept. Earn Dizana's Quiver and the Infernal Cape. Bank a new Mage Arena II cape, or show all three original imbued god capes already banked."));
        return trials;
    }

    private CovenantScroll.Card rankSTrialCard(String expectedProfile, RankSTrial route, String title, String target, String detail)
    {
        return new CovenantScroll.Card(title, "Rank S", target, "Promotion", detail, null,
            () -> forProfile(expectedProfile, () -> acceptRankSTrial(route, title)),
            () -> route != RankSTrial.REWARDS || bankReady);
    }

    private ItemContainer openBank()
    {
        net.runelite.api.widgets.Widget bankItems = client.getWidget(InterfaceID.Bankmain.ITEMS);
        return bankItems == null || bankItems.isHidden() ? null : client.getItemContainer(InventoryID.BANK);
    }

    private int heldQuantity(int id)
    {
        ItemContainer inventory = client.getItemContainer(InventoryID.INV);
        ItemContainer equipment = client.getItemContainer(InventoryID.WORN);
        return (inventory == null ? 0 : inventory.count(id)) + (equipment == null ? 0 : equipment.count(id));
    }

    private void acceptRankSTrial(RankSTrial route, String title)
    {
        if (!hasActiveProfile() || ledger.active != null || ledger.favour < 100000 || !"A".equals(ledger.rank)
            || !client.getWorldType().contains(WorldType.MEMBERS))
        {
            return;
        }
        Offer trial = new Offer();
        trial.encounter = title;
        trial.rank = "S";
        trial.rankSTrial = route;
        trial.rankSNotifiedItems = new ArrayList<>();
        trial.rankSVerifiedItems = new ArrayList<>();
        switch (route)
        {
            case PET_AND_CLUES:
                trial.target = 10;
                break;
            case SKILLCAPES:
                trial.rankSAllowedSkills = new ArrayList<>();
                for (Skill skill : SKILLCAPES.keySet())
                {
                    if (client.getRealSkillLevel(skill) < 99)
                    {
                        trial.rankSAllowedSkills.add(skill);
                    }
                }
                if (trial.rankSAllowedSkills.size() < 2)
                {
                    return;
                }
                trial.rankSVerifiedSkills = new ArrayList<>();
                trial.rankSReachedSkills = new ArrayList<>();
                rankSBelow99.clear();
                rankSBelow99.addAll(trial.rankSAllowedSkills);
                trial.target = 2;
                break;
            case REWARDS:
                ItemContainer bank = openBank();
                if (bank == null)
                {
                    return;
                }
                trial.rankSStartingGodCapes = new ArrayList<>();
                for (int id : MAGE_ARENA_II_CAPES)
                {
                    trial.rankSStartingGodCapes.add(bank.count(id) + heldQuantity(id));
                }
                trial.target = 3;
                break;
        }
        ledger.active = trial;
        ledger.recordedXp = 0;
        ledger.offers.clear();
        freelanceBoardOpen = false;
        ledger.message = "Your Rank S trial has begun.";
        saveAndRender();
    }

    private void recordRankSNotice(Offer trial, String message)
    {
        String prefix = "New item added to your collection log: ";
        if (!message.regionMatches(true, 0, prefix, 0, prefix.length()))
        {
            return;
        }
        List<Integer> eligible = new ArrayList<>();
        if (trial.rankSTrial == RankSTrial.PET_AND_CLUES)
        {
            eligible.addAll(SKILLING_PETS);
            eligible.addAll(RANK_S_BOSS_PETS);
        }
        else if (trial.rankSTrial == RankSTrial.REWARDS)
        {
            eligible.add(net.runelite.api.ItemID.DIZANAS_QUIVER);
            eligible.add(net.runelite.api.ItemID.INFERNAL_CAPE);
        }
        for (int id : eligible)
        {
            String notice = prefix + client.getItemDefinition(id).getName();
            if ((message.equalsIgnoreCase(notice) || message.equalsIgnoreCase(notice + "."))
                && !trial.rankSNotifiedItems.contains(id))
            {
                trial.rankSNotifiedItems.add(id);
                saveAndRender();
                return;
            }
        }
    }

    private void trackRankSTrial(Offer trial)
    {
        ItemContainer bank = openBank();
        int previousProgress = ledger.recordedXp;
        boolean changed = false;
        if (trial.rankSTrial == RankSTrial.SKILLCAPES)
        {
            for (Skill skill : trial.rankSAllowedSkills)
            {
                if (client.getRealSkillLevel(skill) < 99)
                {
                    rankSBelow99.add(skill);
                    continue;
                }
                if (rankSBelow99.remove(skill) && !trial.rankSReachedSkills.contains(skill))
                {
                    trial.rankSReachedSkills.add(skill);
                    changed = true;
                }
                if (trial.rankSReachedSkills.contains(skill) && !trial.rankSVerifiedSkills.contains(skill)
                    && SKILLCAPES.get(skill).stream().anyMatch(id -> heldQuantity(id) > 0 || (bank != null && bank.count(id) > 0)))
                {
                    trial.rankSVerifiedSkills.add(skill);
                }
            }
            ledger.recordedXp = Math.min(trial.target, trial.rankSVerifiedSkills.size());
        }
        else
        {
            ItemContainer log = client.getItemContainer(InventoryID.COLLECTION_TRANSMIT);
            for (int id : trial.rankSNotifiedItems)
            {
                if (!trial.rankSVerifiedItems.contains(id) && log != null && log.count(id) > 0)
                {
                    trial.rankSVerifiedItems.add(id);
                    changed = true;
                }
            }
            if (trial.rankSTrial == RankSTrial.REWARDS)
            {
                if (bank != null)
                {
                    boolean allBanked = true;
                    boolean newBanked = false;
                    for (int i = 0; i < MAGE_ARENA_II_CAPES.size(); i++)
                    {
                        int id = MAGE_ARENA_II_CAPES.get(i);
                        int count = bank.count(id);
                        allBanked &= count > 0;
                        newBanked |= count > 0 && count + heldQuantity(id) > trial.rankSStartingGodCapes.get(i);
                    }
                    trial.rankSGodCape = allBanked || newBanked;
                }
                ledger.recordedXp = trial.rankSVerifiedItems.size() + (trial.rankSGodCape ? 1 : 0);
            }
        }
        boolean hasRequiredPet = trial.rankSTrial != RankSTrial.PET_AND_CLUES || !trial.rankSVerifiedItems.isEmpty();
        if (ledger.recordedXp >= trial.target && hasRequiredPet)
        {
            verifyCompletion(() -> finishPromotion("S"));
        }
        else if (changed || previousProgress != ledger.recordedXp)
        {
            saveAndRender();
        }
    }

    private void showPromotion()
    {
        if (!hasActiveProfile() || ledger.active != null)
        {
            return;
        }
        String expectedProfile = profile;
        Runnable onClose = () -> forProfile(expectedProfile, () ->
        {
            displayedSlate = -1;
            syncScroll();
        });
        String nextRank;
        List<CovenantScroll.Card> trials;
        switch (ledger.rank)
        {
            case "S":
                scroll.showAudience(ledger.favour >= 100000
                    ? "Your ledger stands complete. The Covenant honours your service at Rank S. The Guild Master rank has yet to open."
                    : "You stand at Rank S. There is still work to enter in your ledger.", onClose);
                return;
            case "A":
                nextRank = "S";
                trials = rankSTrialCards(expectedProfile);
                break;
            case "B":
                nextRank = "A";
                trials = ledger.favour >= 100000 ? rankATrialCards(expectedProfile) : Collections.emptyList();
                break;
            case "C":
                nextRank = "B";
                trials = new ArrayList<>(List.of(
                    ledger.favour >= 100000 && Quest.PRIEST_IN_PERIL.getState(client) == QuestState.FINISHED
                        ? armoryCard(expectedProfile)
                        : promotionCard("Armory", nextRank, "Barrows components", "Requires access to Barrows.")));
                if (client.getWorldType().contains(WorldType.MEMBERS))
                {
                    trials.add(0, bossTrialCard(expectedProfile, "Scurrius", 100, net.runelite.api.ItemID.SCURRY));
                    trials.add(0, bossTrialCard(expectedProfile, "Chaos Elemental", 50, net.runelite.api.ItemID.PET_CHAOS_ELEMENTAL));
                    trials.add(capeCard(expectedProfile));
                    if (ledger.favour >= 100000)
                    {
                        addCollectionTrialCard(trials, expectedProfile);
                    }
                }
                break;
            case "D":
                nextRank = "C";
                trials = List.of(new CovenantScroll.Card("Giant chests", "Rank C", "30 Bryophyta or 15 of each", "Promotion",
                    "Loot 30 keyed Bryophyta chests. Once you loot 15 keyed Obor chests, the Bryophyta target drops to 15. Both counters start at acceptance.",
                    null, () -> forProfile(expectedProfile, this::acceptGiantChestTrial)));
                break;
            default:
                nextRank = "D";
                trials = List.of(new CovenantScroll.Card("Brutus", "Rank D", "25 kills or full log", "Promotion",
                    "Kill Brutus 25 times after accepting, or finish his collection log during the trial. Keep kill-count messages enabled. If you finish the log early, open Brutus in the Collection Log.",
                    null, () -> forProfile(expectedProfile, this::acceptBrutusTrial)));
                break;
        }
        if (ledger.favour < 100000)
        {
            scroll.showAudience("The Covenant still has work for you. Earn full favour at your present rank, "
                + "and I will open the trial for Rank " + nextRank + ". Your favour stands at "
                + formatFavour(ledger.favour) + "%.", onClose);
            return;
        }
        if (trials.isEmpty())
        {
            scroll.showAudience("The Rank " + nextRank + " trials require a members world.", onClose);
            return;
        }
        scroll.showPromotion(trials, "Your ledger stands at full favour. I offer you a trial for Rank "
            + nextRank + ". Read the terms and consider the work before you.", onClose);
    }

    private static CovenantScroll.Card promotionCard(String title, String rank, String objective, String detail)
    {
        return new CovenantScroll.Card(title, "Rank " + rank, objective, "Promotion",
            detail + " Trial tracking is not available in this build.", null, null);
    }

    private void ensureFreelanceBoard()
    {
        if (ledger.rank.equals(ledger.freelanceBoardRank))
        {
            return;
        }
        ledger.freelanceOffers.clear();
        ledger.freelanceBoardRank = ledger.rank;
        List<Offer> pool = skillingOffers(ledger.rank, 1);
        addRankedEncounters(pool, ledger.rank, true);
        Collections.shuffle(pool);
        List<Offer> selected = new ArrayList<>();
        java.util.Set<String> families = new java.util.HashSet<>();
        pool.removeIf(offer -> ledger.freelanceCooldown.contains(freelanceKey(offer)));
        for (Offer offer : pool)
        {
            String family = offer.encounter == null ? "skilling"
                : isFetch(offer.encounter) ? "gathering"
                : "Guardians of the Rift".equals(offer.encounter) ? "minigame" : "boss";
            if (families.add(family))
            {
                selected.add(offer);
            }
        }
        for (Offer offer : pool)
        {
            if (selected.size() == 10)
            {
                break;
            }
            if (!selected.contains(offer))
            {
                selected.add(offer);
            }
        }
        Collections.shuffle(selected);
        for (Offer offer : selected)
        {
            offer.target = contractTarget(offer, 1);
            offer.reward = Integer.parseInt(catalog.getProperty("rewards." + offer.rank).split(",")[1]) / 10;
            offer.freelance = true;
            ledger.freelanceOffers.add(offer);
        }
        ledger.freelanceBoardVersion++;
    }

    private static String freelanceKey(Offer offer)
    {
        return offer.encounter == null ? offer.skill : offer.encounter;
    }

    private void acceptFreelance(String key, int version)
    {
        if (!hasActiveProfile() || !freelanceBoardOpen || ledger.active != null || !ledger.offers.isEmpty()
            || ledger.freelanceLocked || version != ledger.freelanceBoardVersion)
        {
            return;
        }
        for (Offer offer : ledger.freelanceOffers)
        {
            if (freelanceKey(offer).equals(key))
            {
                if (!prepareCoffer(offer))
                {
                    return;
                }
                ledger.active = offer;
                ledger.recordedXp = 0;
                lastObservedXp = offer.encounter == null ? client.getSkillExperience(Skill.valueOf(offer.skill)) : null;
                ledger.message = "Freelance work entered in the ledger.";
                freelanceBoardOpen = false;
                saveAndRender();
                return;
            }
        }
    }

    private void populateFreelance(JScrollPane area, Ledger view, String expectedProfile)
    {
        JPanel list = new JPanel();
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        list.setBorder(BorderFactory.createEmptyBorder(6, 8, 8, 8));
        list.setBackground(OAK);
        list.add(ledgerLabel("Freelance contracts", 15f));
        list.add(Box.createVerticalStrut(8));
        if (view.freelanceLocked || !view.offers.isEmpty())
        {
            list.add(ledgerLabel(view.freelanceLocked ? "Finish issued work first" : "Review slate to accept", 11f));
            list.add(Box.createVerticalStrut(8));
        }
        for (Offer offer : view.freelanceOffers)
        {
            String key = freelanceKey(offer);
            JButton task = ledgerButton(offerTitle(offer) + "  |  " + offer.rank);
            task.setFont(task.getFont().deriveFont(11f));
            task.addActionListener(event -> forProfile(expectedProfile, () ->
            {
                if (!freelanceBoardOpen || ledger.active != null || view.freelanceBoardVersion != ledger.freelanceBoardVersion
                    || ledger.freelanceOffers.stream().noneMatch(entry -> freelanceKey(entry).equals(key)))
                {
                    return;
                }
                boolean canAccept = ledger.offers.isEmpty() && !ledger.freelanceLocked;
                String target = String.format(Locale.US, "%,d %s", offer.target, offerUnit(offer));
                int payout = Math.max(0, Math.min(offer.reward, 99000 - ledger.favour));
                String detail = freelanceDetail(offer);
                if (!canAccept)
                {
                    detail += " Finish or resolve issued work before accepting freelance work.";
                }
                CovenantScroll.Card card = new CovenantScroll.Card(offerTitle(offer), "Rank: " + offer.rank,
                    target, "+" + formatFavour(payout), detail,
                    offer.skill == null ? null : skillIconManager.getSkillImage(Skill.valueOf(offer.skill)),
                    canAccept ? () -> forProfile(expectedProfile, () -> acceptFreelance(key, view.freelanceBoardVersion)) : null,
                    () -> !offer.cofferFetch || cofferReady);
                scroll.showFreelance(card, () -> forProfile(expectedProfile, () ->
                {
                    displayedSlate = -1;
                    syncScroll();
                }));
            }));
            list.add(task);
            list.add(Box.createVerticalStrut(5));
        }
        if (view.freelanceOffers.isEmpty())
        {
            list.add(ledgerLabel("No freelance work remaining.", 11f));
            list.add(ledgerLabel("Request a Guild Master contract.", 10f));
        }
        area.setViewportView(list);
    }

    private static String freelanceDetail(Offer offer)
    {
        String action;
        if (isFetch(offer.encounter))
        {
            action = offer.cofferFetch
                ? "Open your bank before confirming. Add the required amount to your last bank tab, the Guild Coffer. Any source counts."
                : "Yew logs".equals(offer.encounter) ? "Chop yew logs from trees."
                : "Mine " + offer.encounter.toLowerCase(Locale.ROOT) + " from rocks.";
        }
        else if ("Guardians of the Rift".equals(offer.encounter))
        {
            action = "Close rifts successfully. A failed round ends the contract. Keep the rift count messages enabled.";
        }
        else if (offer.encounter != null)
        {
            action = "Record " + offerUnit(offer) + " at " + offer.encounter + ". Keep the encounter count messages enabled.";
        }
        else
        {
            action = "Gain " + offerTitle(offer) + " XP.";
        }
        return action + " Completing or abandoning this task removes it until the board refreshes.";
    }

    private static int rankOrder(String rank)
    {
        return Math.max(0, RANKS.indexOf(rank));
    }

    private void syncScroll()
    {
        if (scroll == null)
        {
            return;
        }
        if (!hasActiveProfile() || ledger.active != null || ledger.offers.isEmpty())
        {
            scroll.clear();
            displayedSlate = -1;
            displayedProfile = null;
            return;
        }
        if (profile.equals(displayedProfile) && ledger.slateVersion == displayedSlate)
        {
            return;
        }
        displayedProfile = profile;
        displayedSlate = ledger.slateVersion;
        String expectedProfile = profile;
        int version = ledger.slateVersion;
        String rank = ledger.refusals == 2 ? lowerRank(ledger.rank) : ledger.rank;
        List<CovenantScroll.Card> cards = new ArrayList<>();
        for (int index = 0; index < ledger.offers.size(); index++)
        {
            Offer offer = ledger.offers.get(index);
            int slot = index;
            boolean boss = offer.encounter != null;
            String title = offerTitle(offer);
            String unit = offerUnit(offer);
            String target = String.format(Locale.US, "%,d %s", offer.target, unit);
            String detail = boss
                ? String.format(Locale.US, "Record %,d %s at %s after accepting. Keep the encounter count messages enabled.",
                    offer.target, unit, title)
                : String.format(Locale.US, "Gain %,d %s XP after accepting. There is no time limit.", offer.target, title);
            if (isFetch(offer.encounter) || "Guardians of the Rift".equals(offer.encounter))
            {
                detail = freelanceDetail(offer).replace(" Completing or abandoning this task removes it until the board refreshes.", "");
            }
            else if (offer.encounter != null && offer.encounter.startsWith("Clue Scroll ("))
            {
                detail = "Open reward caskets of the named tier. Stockpiled caskets count.";
            }
            else if (offer.quest != null)
            {
                detail = "Finish the named quest with Guild Master active.";
            }
            else if (offer.oneTimeObjective != null)
            {
                OneTimeObjective objective = findOneTimeObjective(offer.oneTimeObjective);
                detail = objective.detail + (objective.logPage == null ? " The game verifies completion."
                    : " Reopen the " + objective.logPage + " Collection Log page to verify your unlock.");
            }
            else if ("Slayer's Gauntlet".equals(offer.encounter))
            {
                detail = (offer.slayerMaster == null ? "Any Slayer master. " : "Master: " + offer.slayerMaster + ". ")
                    + "Finish each tracked assignment before taking another. Cancelling, skipping, or replacing it fails the contract. Pre-existing assignments do not count.";
            }
            else if (offer.encounter != null && LOOT_CLEARS.contains(offer.encounter))
            {
                detail = "Complete the encounter and claim your reward. Any difficulty counts.";
            }
            cards.add(new CovenantScroll.Card(title, "Rank: " + (offer.rank == null ? rank : offer.rank), target,
                "+" + formatFavour(offer.reward), detail,
                boss ? null : skillIconManager.getSkillImage(Skill.valueOf(offer.skill)),
                () -> forProfile(expectedProfile, () -> accept(slot, version)),
                () -> !offer.cofferFetch || cofferReady));
        }
        String letter;
        switch (ledger.refusals)
        {
            case 2:
                letter = "Finish one recovery contract to reopen the ordinary board. This work earns no favour.";
                break;
            case 3:
                letter = "Your recovery work is entered in the ledger. Read the new offers and choose your work.";
                break;
            case 4:
                letter = "This is the last slate I can offer at your present standing. Choose carefully.";
                break;
            default:
                letter = FETCH_TEST && cards.size() == 1
                    ? "The Guild Coffer needs oak logs. Open your bank before confirming this contract."
                    : "The Covenant has three contracts for your consideration. Read the terms and choose your work.";
                break;
        }
        scroll.showSlate(cards, letter, declineWarning(ledger.rerollPending ? 0 : ledger.refusals, ledger.rank),
            () -> forProfile(expectedProfile, () -> decline(version)));
    }

    private void forProfile(String expected, Runnable action)
    {
        clientThread.invokeLater(() ->
        {
            if (hasActiveProfile() && profile.equals(expected))
            {
                action.run();
            }
        });
    }

    private static String skillName(String skill)
    {
        return skill.substring(0, 1) + skill.substring(1).toLowerCase(Locale.ROOT);
    }

    private static String formatFavour(int amount)
    {
        return String.format(Locale.ROOT, amount % 10 == 0 ? "%.2f" : "%.3f", amount / 1000.0);
    }

    private static final java.awt.Color OAK = new java.awt.Color(48, 33, 26);
    private static final java.awt.Color LEATHER = new java.awt.Color(78, 52, 35);
    private static final java.awt.Color BRASS = new java.awt.Color(174, 128, 71);
    private static final java.awt.Color PARCHMENT_INK = new java.awt.Color(239, 215, 172);

    private static JLabel ledgerLabel(String text, float size)
    {
        JLabel label = new JLabel(text);
        label.setFont(CovenantScroll.calligraphy().deriveFont(size));
        label.setForeground(PARCHMENT_INK);
        label.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        return label;
    }

    private static JButton ledgerButton(String label)
    {
        JButton button = new JButton(label);
        button.setFont(CovenantScroll.calligraphy().deriveFont(13f));
        button.setForeground(PARCHMENT_INK);
        button.setBackground(LEATHER);
        button.setOpaque(true);
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        button.setMaximumSize(new java.awt.Dimension(Integer.MAX_VALUE, 42));
        button.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BRASS),
            BorderFactory.createEmptyBorder(9, 11, 9, 10)));
        return button;
    }

    private static void button(JPanel panel, String label, Runnable action)
    {
        JButton button = ledgerButton(label);
        button.addActionListener(event -> action.run());
        panel.add(button);
        panel.add(Box.createVerticalStrut(8));
    }
}
