package com.khorn.terraincontrol.customobjects.bo3;

import com.khorn.terraincontrol.TerrainControl;
import com.khorn.terraincontrol.configuration.ConfigFile;
import com.khorn.terraincontrol.configuration.ConfigFunction;
import com.khorn.terraincontrol.configuration.TCDefaultValues;
import com.khorn.terraincontrol.configuration.WorldConfig.ConfigMode;
import com.khorn.terraincontrol.customobjects.CustomObject;
import com.khorn.terraincontrol.customobjects.bo3.BO3Settings.OutsideSourceBlock;
import com.khorn.terraincontrol.customobjects.bo3.BO3Settings.SpawnHeightSetting;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class BO3Config extends ConfigFile
{

    public Map<String, CustomObject> otherObjectsInDirectory;
    public String author;
    public String description;
    public ConfigMode settingsMode;
    public boolean tree;
    public int frequency;
    public double rarity;
    public boolean rotateRandomly;
    public SpawnHeightSetting spawnHeight;
    public int minHeight;
    public int maxHeight;
    public ArrayList<String> excludedBiomes;
    public HashSet<Integer> sourceBlock;
    public int maxPercentageOutsideSourceBlock;
    public OutsideSourceBlock outsideSourceBlock;
    public BlockFunction[][] blocks = new BlockFunction[4][]; // four rotations
    public BO3Check[][] bo3Checks = new BO3Check[4][];
    public int maxBranchDepth;
    public BranchFunction[][] branches = new BranchFunction[4][];

    /**
     * Creates a BO3Config from a file.
     *
     * @param name The name of the BO3 without the extension.
     * @param file The file of the BO3.
     */
    public BO3Config(String name, File file, Map<String, CustomObject> otherObjectsInDirectory)
    {
        super(name, file);

        this.otherObjectsInDirectory = otherObjectsInDirectory;

        readSettingsFile();

        init();
    }

    /**
     * Creates a BO3Config with the specified settings. Ignores the settings in
     * the
     * settings file.
     *
     * @param oldObject The old BO3 object. It's settings will be copied.
     * @param extraSettings The extra settings.
     */
    public BO3Config(BO3 oldObject, Map<String, String> extraSettings)
    {
        super(oldObject.getSettings().name, oldObject.getSettings().file);

        this.settingsCache = oldObject.getSettings().settingsCache;
        this.settingsCache.putAll(extraSettings);

        // Make sure that the BO3 file won't get overwritten
        this.settingsCache.put(TCDefaultValues.SettingsMode.toString().toLowerCase(), ConfigMode.WriteDisable.toString());

        init();
    }

    private void init()
    {
        readConfigSettings();
        correctSettings();
        if (settingsMode != ConfigMode.WriteDisable)
        {
            writeSettingsFile(settingsMode == ConfigMode.WriteAll);
        }

        rotateBlocksAndChecks();
    }

    @Override
    public void logFileNotFound(File file)
    {
        // Ignore
    }

    public Map<String, String> getSettingsCache()
    {
        return settingsCache;
    }

    @Override
    protected void writeConfigSettings() throws IOException
    {
        // The object
        writeBigTitle("BO3 object");
        writeComment("This is the config file of a custom object.");
        writeComment("If you add this object correctly to your BiomeConfigs, it will spawn in the world.");
        writeComment("");
        writeComment("This is the creator of this BO3 object");
        writeValue("Author", author);
        writeNewLine();
        writeComment("A short description of this BO3 object");
        writeValue("Description", description);
        writeNewLine();
        writeComment("The BO3 version, don't change this! It can be used by external applications to do a version check.");
        writeValue("Version", 3);
        writeNewLine();
        writeComment("The settings mode, WriteAll, WriteWithoutComments or WriteDisable. See WorldConfig.");
        writeValue("SettingsMode", settingsMode.toString());

        // Main settings
        writeBigTitle("Main settings");
        writeComment("This needs to be set to true to spawn the object in the Tree and Sapling resources.");
        writeValue("Tree", tree);
        writeNewLine();
        writeComment("The frequency of the BO3 from 1 to 200. Tries this many times to spawn this BO3 when using the CustomObject(...) resource.");
        writeComment("Ignored by Tree(..), Sapling(..) and CustomStructure(..)");
        writeValue("Frequency", frequency);
        writeNewLine();
        writeComment("The rarity of the BO3 from 0 to 100. Each spawn attempt has rarity% chance to succeed when using the CustomObject(...) resource.");
        writeComment("Ignored by Tree(..), Sapling(..) and CustomStructure(..)");
        writeValue("Rarity", rarity);
        writeNewLine();
        writeComment("If you set this to true, the BO3 will be placed with a random rotation.");
        writeValue("RotateRandomly", rotateRandomly);
        writeNewLine();
        writeComment("The spawn height of the BO3 - randomY, highestBlock or highestSolidBlock.");
        writeValue("SpawnHeight", spawnHeight.toString());
        writeNewLine();
        writeComment("The height limits for the BO3.");
        writeValue("MinHeight", minHeight);
        writeValue("MaxHeight", maxHeight);
        writeNewLine();
        writeComment("Objects can have other objects attacthed to it: branches. Branches can also");
        writeComment("have branches attached to it, which can also have branches, etc. This is the");
        writeComment("maximum branch depth for this objects.");
        writeValue("MaxBranchDepth", maxBranchDepth);
        writeNewLine();
        writeComment("When spawned with the UseWorld keyword, this BO3 should NOT spawn in the following biomes.");
        writeComment("If you write the BO3 name directly in the BiomeConfigs, this will be ignored.");
        writeValue("ExcludedBiomes", excludedBiomes);

        // Sourceblock
        writeBigTitle("Source block settings");
        writeComment("The block the BO3 should spawn in");
        writeValue("SourceBlock", sourceBlock);
        writeNewLine();
        writeComment("The maximum percentage of the BO3 that can be outside the SourceBlock.");
        writeComment("The BO3 won't be placed on a location with more blocks outside the SourceBlock than this percentage.");
        writeValue("MaxPercentageOutsideSourceBlock", maxPercentageOutsideSourceBlock);
        writeNewLine();
        writeComment("What to do when a block is about to be placed outside the SourceBlock? (dontPlace, placeAnyway)");
        writeValue("OutsideSourceBlock", outsideSourceBlock.toString());

        // Blocks and other things
        writeResources();
    }

    @Override
    protected void readConfigSettings()
    {
        author = readSettings(BO3Settings.author);
        description = readSettings(BO3Settings.description);
        settingsMode = readSettings(TCDefaultValues.SettingsMode);

        tree = readSettings(BO3Settings.tree);
        frequency = readSettings(BO3Settings.frequency);
        rarity = readSettings(BO3Settings.rarity);
        rotateRandomly = readSettings(BO3Settings.rotateRandomly);
        spawnHeight = readSettings(BO3Settings.spawnHeight);
        minHeight = readSettings(BO3Settings.minHeight);
        maxHeight = readSettings(BO3Settings.maxHeight);
        maxBranchDepth = readSettings(BO3Settings.maxBranchDepth);
        excludedBiomes = readSettings(BO3Settings.excludedBiomes);

        sourceBlock = readSettings(BO3Settings.sourceBlock);
        maxPercentageOutsideSourceBlock = readSettings(BO3Settings.maxPercentageOutsideSourceBlock);
        outsideSourceBlock = readSettings(BO3Settings.outsideSourceBlock);

        // Read the resources
        readResources();
    }

    private void readResources()
    {
        List<BlockFunction> tempBlocksList = new ArrayList<BlockFunction>();
        List<BO3Check> tempChecksList = new ArrayList<BO3Check>();
        List<BranchFunction> tempBranchesList = new ArrayList<BranchFunction>();

        for (Map.Entry<String, String> entry : this.settingsCache.entrySet())
        {
            String key = entry.getKey();
            int start = key.indexOf("(");
            int end = key.lastIndexOf(")");
            if (start != -1 && end != -1)
            {
                String name = key.substring(0, start);
                String[] props = readComplexString(key.substring(start + 1, end));

                ConfigFunction<BO3Config> res = TerrainControl.getConfigFunctionsManager().getConfigFunction(name, this, this.name + " on line " + entry.getValue(), Arrays.asList(props));

                if (res != null && res.isValid())
                {
                    if (res instanceof BlockFunction)
                    {
                        tempBlocksList.add((BlockFunction) res);
                    } else if (res instanceof BO3Check)
                    {
                        tempChecksList.add((BO3Check) res);
                    } else if (res instanceof WeightedBranchFunction)
                    {
                        tempBranchesList.add((WeightedBranchFunction) res);
                    } else if (res instanceof BranchFunction)
                    {
                        tempBranchesList.add((BranchFunction) res);
                    }
                }
            }
        }

        // Store the blocks
        blocks[0] = tempBlocksList.toArray(new BlockFunction[tempBlocksList.size()]);
        bo3Checks[0] = tempChecksList.toArray(new BO3Check[tempChecksList.size()]);
        branches[0] = tempBranchesList.toArray(new BranchFunction[tempBranchesList.size()]);
    }

    public void writeResources() throws IOException
    {
        // Blocks
        writeBigTitle("Blocks");
        writeComment("All the blocks used in the BO3 are listed here. Possible blocks:");
        writeComment("Block(x,y,z,id[.data][,nbtfile.nbt)");
        writeComment("RandomBlock(x,y,z,id[:data][,nbtfile.nbt],chance[,id[:data][,nbtfile.nbt],chance[,...]])");
        writeComment("So RandomBlock(0,0,0,CHEST,chest.nbt,50,CHEST,anotherchest.nbt,100) will spawn a chest at");
        writeComment("the BO3 origin, and give it a 50% chance to have the contents of chest.nbt, or, if that");
        writeComment("fails, a 100% percent chance to have the contents of anotherchest.nbt.");
        for (BlockFunction block : blocks[0])
        {
            writeValue(block.write());
        }

        // BO3Checks
        writeBigTitle("BO3 checks");
        writeComment("Require a condition at a certain location in order for the BO3 to be spawned.");
        writeComment("BlockCheck(x,y,z,id[:data][,id[:data][,...]]) - one of the blocks must be at the location");
        writeComment("BlockCheckNot(x,y,z,id[:data][,id[:data][,...]]) - all the blocks must not be at the location");
        writeComment("LightCheck(x,y,z,minLightLevel,maxLightLevel) - light must be between min and max (inclusive)");
        for (BO3Check check : bo3Checks[0])
        {
            writeValue(check.write());
        }

        // Branches
        writeBigTitle("Branches");
        writeComment("Branches are objects that will spawn when this object spawns when it is used in");
        writeComment("the CustomStructure resource. Branches can also have branches, making complex");
        writeComment("structures possible. See the wiki for more details.");
        writeComment("");
        writeComment("Regular Branches spawn each branch with an independent chance of spawning.");
        writeComment("Branch(x,y,z,branchName,rotation,chance[,anotherBranchName,rotation,chance[,...]][IndividualChance])");
        writeComment("branchName - name of the object to spawn.");
        writeComment("rotation - NORTH, SOUTH, EAST or WEST.");
        writeComment("IndividualChance - The chance each branch has to spawn, assumed to be 100 when left blank");
        writeComment("");
        writeComment("Weighted Branches spawn branches with a dependent chance of spawning.");
        writeComment("WeightedBranch(x,y,z,branchName,rotation,chance[,anotherBranchName,rotation,chance[,...]][MaxChanceOutOf])");
        writeComment("MaxChanceOutOf - The chance all branches have to spawn out of, assumed to be 100 when left blank");
//        writeComment("Example1: WeightedBranch(0,0,0,branch1,NORTH,2,branch2,NORTH,6,10)");
//        writeComment("   branch1 will have a 2 in 10 (20%) chance of spawning, branch2 will have a 6 in 10 (60%) chance to spawn,");
//        writeComment("   and there is a 2 in 10 (20%) chance nothing will spawn");
//        writeComment("Example1A: WeightedBranch(0,0,0,branch1,NORTH,10,branch2,NORTH,30,50)");
//        writeComment("   Same chance as Example1");
//        writeComment("   branch1 will have a 10 in 50 (20%) chance of spawning, branch2 will have a 30 in 50 (60%) chance to spawn,");
//        writeComment("   and there is a 10 in 50 (20%) chance nothing will spawn");
//        writeComment("Example2: WeightedBranch(0,0,0,branch1,NORTH,10,branch2,NORTH,30)");
//        writeComment("   branch1 will have a 10 in 100 (10%) chance of spawning, branch2 will have a 30 in 100 (30%) chance to spawn,");
//        writeComment("   and there is a 60 in 100 (60%) chance nothing will spawn");

        for (BranchFunction branch : branches[0])
        {
            writeValue(branch.makeString());
        }

    }

    @Override
    protected void correctSettings()
    {
        frequency = applyBounds(frequency, 1, 200);
        rarity = applyBounds(rarity, 0.000001, 100.0);
        minHeight = applyBounds(minHeight, TerrainControl.worldDepth, TerrainControl.worldHeight - 1);
        maxHeight = applyBounds(maxHeight, minHeight, TerrainControl.worldHeight);
        maxBranchDepth = applyBounds(maxBranchDepth, 1, Integer.MAX_VALUE);
        sourceBlock = applyBounds(sourceBlock, 0, TerrainControl.supportedBlockIds);
        maxPercentageOutsideSourceBlock = applyBounds(maxPercentageOutsideSourceBlock, 0, 100);
    }

    @Override
    protected void renameOldSettings()
    {
        // Stub method - there are no old setting to convert yet (:
    }

    /**
     * Rotates all the blocks and all the checks
     */
    public void rotateBlocksAndChecks()
    {
        for (int i = 1; i < 4; i++)
        {
            // Blocks
            blocks[i] = new BlockFunction[blocks[i - 1].length];
            for (int j = 0; j < blocks[i].length; j++)
            {
                blocks[i][j] = blocks[i - 1][j].rotate();
            }
            // BO3 checks
            bo3Checks[i] = new BO3Check[bo3Checks[i - 1].length];
            for (int j = 0; j < bo3Checks[i].length; j++)
            {
                bo3Checks[i][j] = bo3Checks[i - 1][j].rotate();
            }
            // Branches
            branches[i] = new BranchFunction[branches[i - 1].length];
            for (int j = 0; j < branches[i].length; j++)
            {
                branches[i][j] = branches[i - 1][j].rotate();
            }
        }
    }

}
