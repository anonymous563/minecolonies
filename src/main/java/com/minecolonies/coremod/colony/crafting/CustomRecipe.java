package com.minecolonies.coremod.colony.crafting;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.IColonyManager;
import com.minecolonies.api.colony.buildings.IBuildingWorker;
import com.minecolonies.api.colony.requestsystem.StandardFactoryController;
import com.minecolonies.api.colony.requestsystem.token.IToken;
import com.minecolonies.api.crafting.IRecipeManager;
import com.minecolonies.api.crafting.IRecipeStorage;
import com.minecolonies.api.crafting.ItemStorage;
import com.minecolonies.api.crafting.ModRecipeTypes;
import com.minecolonies.api.crafting.RecipeStorage;
import com.minecolonies.api.research.IGlobalResearchTree;
import com.minecolonies.api.util.ItemStackUtils;
import com.minecolonies.api.util.Log;
import com.minecolonies.api.util.constant.TypeConstants;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import org.jetbrains.annotations.NotNull;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.JsonToNBT;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.ArrayList;
import java.util.Objects;

/**
 * This class represents a recipe loaded from custom data that is available to a crafter
 * but not to a player
 */
public class CustomRecipe
{
    /**
     * The property name that indicates type for the recipe
     */
    public static final String RECIPE_TYPE_PROP = "type";

    /**
     * The recipe type
     */
    public static final String RECIPE_TYPE_RECIPE = "recipe";

    /**
     * The multiple output recipe type
     */
    public static final String RECIPE_TYPE_RECIPE_MULT_OUT = "recipe-multi-out";

    /**
     * The multiple input recipe type
     */
    public static final String RECIPE_TYPE_RECIPE_MULT_IN = "recipe-multi-in";


    /**
     * The remove type
     */
    public static final String RECIPE_TYPE_REMOVE = "remove";

    /**
     * The property name that indicates the recipe to remove
     */
    public static final String RECIPE_ID_TO_REMOVE_PROP = "recipe-id-to-remove";

    /**
     * The property name that indicates crafter type for the recipe
     */
    public static final String RECIPE_CRAFTER_PROP = "crafter";

    /**
     * The property name for the inputs array
     */
    public static final String RECIPE_INPUTS_PROP = "inputs";

    /**
     * The property name for the inputs array
     */
    public static final String RECIPE_SECONDARY_PROP = "additional-output";

    /**
     * The property name for the alternate output array
     */
    public static final String RECIPE_ALTERNATE_PROP = "alternate-output";

    /**
     * The property name for the result item
     */
    public static final String RECIPE_RESULT_PROP = "result";

    /**
     * The property namefor the result loottable
     */
    public static final String RECIPE_LOOTTABLE_PROP = "loot-table";

    /**
     * The property name for Count, used both in inputs array and for result
     */
    public static final String COUNT_PROP = "count";

    /**
     * The property name for the item id in the inputs array
     */
    public static final String ITEM_PROP = "item";

    /**
     * The property name for the intermediate block ID
     */
    public static final String RECIPE_INTERMEDIATE_PROP = "intermediate";

    /**
     * The property name for the required research id
     */
    public static final String RECIPE_RESEARCHID_PROP = "research-id";

    /**
     * The property name for the research id that invalidates this recipe
     */
    public static final String RECIPE_EXCLUDED_RESEARCHID_PROP = "not-research-id";

    /**
     * The property name for the minimum level the building must be
     */
    public static final String RECIPE_BUILDING_MIN_LEVEL_PROP = "min-building-level";

    /**
     * The property name for the maximum level the building can be
     */
    public static final String RECIPE_BUILDING_MAX_LEVEL_PROP = "max-building-level";

    /**
     * The property name for if a recipe of the inputs must exist for the recipe to be valid
     */
    public static final String RECIPE_MUST_EXIST = "must-exist";

    /**
     * The crafter name for this instance, defaults to 'unknown'
     */
    private String crafter = "unknown";

    /**
     * The recipe id for this instance, used for removal and lookup
     */
    private ResourceLocation recipeId = null;

    /**
     * The list of ItemStacks for input to the recipe
     */
    private ArrayList<ItemStorage> inputs = new ArrayList<>();

    /**
     * The list of ItemStacks for alternate (multi-recipe) outputs from the recipe
     */
    private ArrayList<ItemStack> altOutputs = new ArrayList<>();

    /**
     * the result ItemStack
     */
    private ItemStack result = null;

    /**
     * The list of ItemStacks for additional outputs to the recipe
     */
    private ArrayList<ItemStack> secondary = new ArrayList<>();

    /**
     * The Intermediate Block
     */
    private Block intermediate = Blocks.AIR;

    /**
     * ID of the required research. Null if none required
     */
    private ResourceLocation researchId = null;

    /**
     * ID of the exclusionary research. Null if nothing excludes this recipe
     */
    private ResourceLocation excludedResearchId = null;

    /**
     * The Minimum Level the building has to be for this recipe to be valid
     */
    private int minBldgLevel = 0;

    /**
     * The Maximum Level the building can to be for this recipe to be valid
     */
    private int maxBldgLevel = 5;

    /**
     * If true, the recipe inputs must match an already existing recipe's inputs
     */
    private boolean mustExist = false;

    /**
     * The loottable to use for possible additional outputs
     */
    private ResourceLocation lootTable;

    /**
     * Cache of the recipe storage for performance
     */
    private RecipeStorage cachedRecipeStorage;

    /**
     * This class can only be created by the parse static
     */
    private CustomRecipe()
    {
    }

    /**
     * Convert an Item string with NBT to an ItemStack
     * @param itemData ie: minecraft:potion{Potion=minecraft:water}
     * @return stack with any defined NBT
     */
    private static ItemStack idToItemStack(final String itemData)
    {
        String itemId = itemData;
        final int tagIndex = itemId.indexOf("{");
        final String tag = tagIndex > 0 ? itemId.substring(tagIndex) : null;
        itemId = tagIndex > 0 ? itemId.substring(0, tagIndex) : itemId;
        String[] split = itemId.split(":");
        if(split.length != 2)
        {
            if(split.length == 1)
            {
                final String[] tempArray ={"minecraft", split[0]};
                split = tempArray;
            }
            else
            {
                Log.getLogger().error("Unable to parse item definition: " + itemData);
            }
        }
        final Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(split[0], split[1]));
        final ItemStack stack = new ItemStack(item);
        if (tag != null)
        {
            try
            {
                stack.setTag(JsonToNBT.getTagFromJson(tag));
            }
            catch (CommandSyntaxException e1)
            {
                //Unable to parse tags, drop them.
                Log.getLogger().error("Unable to parse item definition: " + itemData);
            }
        }
        if (stack.isEmpty())
        {
            Log.getLogger().warn("Parsed item definition returned empty: " + itemData);
        }
        return stack;
    }

    /**
     * Parse a Json object into a Custom recipe
     *
     * @param recipeId the recipe id
     * @param recipeJson the json representing the recipe
     * @return new instance of CustomRecipe
     */
    public static CustomRecipe parse(@NotNull final ResourceLocation recipeId, @NotNull final JsonObject recipeJson)
    {
        final CustomRecipe recipe = new CustomRecipe();
        recipe.recipeId = recipeId;

        if (recipeJson.has(RECIPE_CRAFTER_PROP))
        {
            recipe.crafter = recipeJson.get(RECIPE_CRAFTER_PROP).getAsString();
        }
        if (recipeJson.has(RECIPE_INPUTS_PROP))
        {
            for (JsonElement e : recipeJson.get(RECIPE_INPUTS_PROP).getAsJsonArray())
            {
                if (e.isJsonObject())
                {
                    JsonObject ingredient = e.getAsJsonObject();
                    if (ingredient.has(ITEM_PROP))
                    {
                        final ItemStack stack = idToItemStack(ingredient.get(ITEM_PROP).getAsString());
                        if(ingredient.has(COUNT_PROP))
                        {
                            stack.setCount(ingredient.get(COUNT_PROP).getAsInt());
                        }
                        recipe.inputs.add(new ItemStorage(stack));
                    }

                }
            }
        }

        if (recipeJson.has(RECIPE_RESULT_PROP))
        {
            recipe.result = idToItemStack(recipeJson.get(RECIPE_RESULT_PROP).getAsString());
        }
        else
        {
            recipe.result = ItemStack.EMPTY;
        }

        if (recipeJson.has(RECIPE_LOOTTABLE_PROP))
        {
            recipe.lootTable = new ResourceLocation(recipeJson.get(RECIPE_LOOTTABLE_PROP).getAsString());
        }

        if (recipeJson.has(RECIPE_SECONDARY_PROP))
        {
            for (JsonElement e : recipeJson.get(RECIPE_SECONDARY_PROP).getAsJsonArray())
            {
                if (e.isJsonObject())
                {
                    JsonObject ingredient = e.getAsJsonObject();
                    if (ingredient.has(ITEM_PROP))
                    {
                        final ItemStack stack = idToItemStack(ingredient.get(ITEM_PROP).getAsString());
                        if(ingredient.has(COUNT_PROP))
                        {
                            stack.setCount(ingredient.get(COUNT_PROP).getAsInt());
                        }
                        recipe.secondary.add(stack);
                    }

                }
            }
        }


        if (recipeJson.has(RECIPE_ALTERNATE_PROP))
        {
            for (JsonElement e : recipeJson.get(RECIPE_ALTERNATE_PROP).getAsJsonArray())
            {
                if (e.isJsonObject())
                {
                    JsonObject ingredient = e.getAsJsonObject();
                    if (ingredient.has(ITEM_PROP))
                    {
                        final ItemStack stack = idToItemStack(ingredient.get(ITEM_PROP).getAsString());
                        if(ingredient.has(COUNT_PROP))
                        {
                            stack.setCount(ingredient.get(COUNT_PROP).getAsInt());
                        }
                        recipe.altOutputs.add(stack);
                    }

                }
            }
        }

        if (recipeJson.has(COUNT_PROP) && !ItemStackUtils.isEmpty(recipe.result))
        {
            recipe.result.setCount(recipeJson.get(COUNT_PROP).getAsInt());
        }
        if (recipeJson.has(RECIPE_INTERMEDIATE_PROP))
        {
            final String[] split = recipeJson.get(RECIPE_INTERMEDIATE_PROP).getAsString().split(":");
            recipe.intermediate = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(split[0], split[1]));
        }
        else
        {
            recipe.intermediate = Blocks.AIR;
        }
        if (recipeJson.has(RECIPE_RESEARCHID_PROP))
        {
            recipe.researchId = new ResourceLocation(recipeJson.get(RECIPE_RESEARCHID_PROP).getAsString());
        }
        if (recipeJson.has(RECIPE_EXCLUDED_RESEARCHID_PROP))
        {
            recipe.excludedResearchId = new ResourceLocation(recipeJson.get(RECIPE_EXCLUDED_RESEARCHID_PROP).getAsString());
        }
        if(recipeJson.has(RECIPE_BUILDING_MIN_LEVEL_PROP))
        {
            recipe.minBldgLevel= recipeJson.get(RECIPE_BUILDING_MIN_LEVEL_PROP).getAsInt();
        }
        if(recipeJson.has(RECIPE_BUILDING_MAX_LEVEL_PROP))
        {
            recipe.maxBldgLevel= recipeJson.get(RECIPE_BUILDING_MAX_LEVEL_PROP).getAsInt();
        }
        if(recipeJson.has(RECIPE_MUST_EXIST))
        {
            recipe.mustExist = recipeJson.get(RECIPE_MUST_EXIST).getAsBoolean();
        }

        return recipe;
    }

    /**
     * Get the name of the crafter this recipe applies to
     * @return crafter name
     */
    public String getCrafter()
    {
        return crafter;
    }

    /**
     * Get the ID for this recipe
     * @return Recipe Resource Location
     */
    public ResourceLocation getRecipeId()
    {
        return recipeId;
    }

    /**
     * Get the ID of research required before this recipe is valid.
     * @return The research ID or null if there is no such requirement.
     */
    public ResourceLocation getRequiredResearchId() { return this.researchId; }

    /**
     * Get the ID of research after which this recipe is no longer valid.
     * @return The research ID or null if there is no such requirement.
     */
    public ResourceLocation getExcludedResearchId() { return this.excludedResearchId; }

    /**
     * Get the minimum (inclusive) building level required before this recipe is valid.
     * @return The minimum building level (0 means no such requirement).
     */
    public int getMinBuildingLevel() { return this.minBldgLevel; }

    /**
     * Get the maximum (inclusive) building level required to still consider this recipe valid.
     * @return The maximum building level (the recipe is no longer valid at higher levels).
     */
    public int getMaxBuildingLevel() { return this.maxBldgLevel; }

    /**
     * Check to see if the recipe is currently valid for the building
     * This does research checks, to verify that the appropriate researches are in the correct states
     * @param building      Building to check recipe against.
     */
    public boolean isValidForBuilding(IBuildingWorker building)
    {
        final IColony colony = building.getColony();
        final boolean requiredEffectPresent;
        if (researchId != null)
        {
            requiredEffectPresent = isUnlockEffectResearched(researchId, colony);
        }
        else
        {
            requiredEffectPresent = false;
        }
        final boolean excludedEffectPresent;
        if (excludedResearchId != null)
        {
            excludedEffectPresent = isUnlockEffectResearched(excludedResearchId, colony);
        }
        else
        {
            excludedEffectPresent = false;
        }
        if (isPrecursorRecipeMissing(building))
        {
            return false;
        }

        final int bldgLevel = building.getBuildingLevel();

        return (researchId == null || requiredEffectPresent)
                 && (excludedResearchId == null || !excludedEffectPresent)
                 && (bldgLevel >= minBldgLevel)
                 && (bldgLevel <= maxBldgLevel);
    }

    /**
     * Check if a given researchId has been completed and has an unlock ability effect.
     * @param researchId    The id of the research to check for.
     * @param colony        The colony being checked against.
     */
    private boolean isUnlockEffectResearched(ResourceLocation researchId, IColony colony)
    {
        //Check first if the research effect exists.
        if (!IGlobalResearchTree.getInstance().hasResearchEffect(researchId) && !IGlobalResearchTree.getInstance().hasResearch(researchId))
        {
            // If there's nothing registered with this effect, and no research with this key, we'll default to acting as if research is not yet completed.
            return false;
        }
        else
        {
            if (IGlobalResearchTree.getInstance().hasResearchEffect(researchId) && colony.getResearchManager().getResearchEffects().getEffectStrength(researchId) > 0)
            {
                // Research effect queried, present, and set to true.
                return true;
            }
            if (IGlobalResearchTree.getInstance().hasResearch(researchId) && colony.getResearchManager().getResearchTree().hasCompletedResearch(researchId))
            {
                // Research ID queried and present.
                // This will allow simple Recipe-style unlocks to exist without needing an extra effect category.
                return true;
            }
        }
        // Research ID queried and present or present as an effect, but not completed or does not have an unlock effect.
        return false;
    }

    /**
     * Check if a precursor recipe is missing from the building.
     * @param building      The building which would contain the precursor recipe.
     * @return              True if a precusor recipe was required and not present.
     */
    private boolean isPrecursorRecipeMissing(IBuildingWorker building)
    {
        if(mustExist)
        {
            final IRecipeStorage compareStorage = this.getRecipeStorage();
            final ResourceLocation recipeSource = this.getRecipeId();
            for (IToken<?> recipeToken : building.getRecipes())
            {
                final IRecipeStorage storage = IColonyManager.getInstance().getRecipeManager().getRecipes().get(recipeToken);
                if ((storage.getRecipeSource() != null && storage.getRecipeSource().equals(recipeSource)) || (
                  ItemStackUtils.compareItemStacksIgnoreStackSize(storage.getPrimaryOutput(), compareStorage.getPrimaryOutput(), false, true) &&
                  storage.getCleanedInput().containsAll(compareStorage.getCleanedInput())
                    && compareStorage.getCleanedInput()
                         .containsAll(storage.getCleanedInput())))
                {
                    return false;
                }
            }
            return true;
        }
        // if no precursor needed.
        return false;
    }

    /**
     * Get a the recipe storage represented by this recipe
     * @return Recipe Storage
     */
    public IRecipeStorage getRecipeStorage()
    {
        if(cachedRecipeStorage == null)
        {
            if(altOutputs.isEmpty())
            {
                cachedRecipeStorage = StandardFactoryController.getInstance().getNewInstance(
                    TypeConstants.RECIPE,
                    StandardFactoryController.getInstance().getNewInstance(TypeConstants.ITOKEN),
                    inputs,
                    1,
                    result,
                    intermediate,
                    this.getRecipeId(),
                    ModRecipeTypes.CLASSIC_ID,
                    null, //alternate outputs
                    secondary, //secondary output
                    lootTable
                    );
            }
            else
            {
                cachedRecipeStorage = StandardFactoryController.getInstance().getNewInstance(
                    TypeConstants.RECIPE,
                    StandardFactoryController.getInstance().getNewInstance(TypeConstants.ITOKEN),
                    inputs,
                    1,
                    result,
                    intermediate,
                    this.getRecipeId(),
                    ModRecipeTypes.MULTI_OUTPUT_ID,
                    altOutputs, //alternate outputs
                    secondary, //secondary output
                    lootTable
                    );
            }
            IRecipeManager recipeManager = IColonyManager.getInstance().getRecipeManager();
            IToken<?> cachedRecipeToken = recipeManager.getRecipeId(cachedRecipeStorage);
            if(cachedRecipeToken != null && !cachedRecipeToken.equals(cachedRecipeStorage.getToken()))
            {
                cachedRecipeStorage = (RecipeStorage) recipeManager.getRecipes().get(cachedRecipeToken);
            }
            recipeManager.registerUse(cachedRecipeStorage.getToken());
        }
        return cachedRecipeStorage;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(result, researchId, excludedResearchId, inputs);
    }

    @Override
    public boolean equals(final Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (o == null || getClass() != o.getClass())
        {
            return false;
        }

        final CustomRecipe that = (CustomRecipe) o;


        return result.isItemEqual(that.result) 
            && researchId.equals(that.researchId)
            && excludedResearchId.equals(that.excludedResearchId)
            && inputs.equals(that.inputs);
    }

    /**
     * Does this require it to already be there? 
     */
    public boolean getMustExist()
    {
        return mustExist;
    }

}