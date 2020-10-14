package com.minecolonies.coremod.colony.requestsystem.resolvers;

import com.minecolonies.api.colony.buildings.IBuildingWorkerView;
import com.minecolonies.api.colony.requestsystem.location.ILocation;
import com.minecolonies.api.colony.requestsystem.manager.IRequestManager;
import com.minecolonies.api.colony.requestsystem.request.IRequest;
import com.minecolonies.api.colony.requestsystem.requestable.IDeliverable;
import com.minecolonies.api.colony.requestsystem.requestable.IRequestable;
import com.minecolonies.api.colony.requestsystem.requestable.crafting.PublicCrafting;
import com.minecolonies.api.colony.requestsystem.requester.IRequester;
import com.minecolonies.api.colony.requestsystem.token.IToken;
import com.minecolonies.coremod.colony.buildings.AbstractBuildingWorker;
import com.minecolonies.coremod.colony.requestsystem.resolvers.core.AbstractCraftingRequestResolver;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.IFormattableTextComponent;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Predicate;

import static com.minecolonies.api.util.RSConstants.CONST_CRAFTING_RESOLVER_PRIORITY;

/**
 * A crafting resolver which takes care of 3x3 crafts which are crafted by a crafter worker.
 */
public class PublicWorkerCraftingRequestResolver extends AbstractCraftingRequestResolver
{
    /**
     * Initializing constructor.
     *
     * @param location the location of the resolver.
     * @param token    its id.
     */
    public PublicWorkerCraftingRequestResolver(@NotNull final ILocation location, @NotNull final IToken<?> token)
    {
        super(location, token, true);
    }

    @Nullable
    @Override
    public List<IRequest<?>> getFollowupRequestForCompletion(@NotNull final IRequestManager manager, @NotNull final IRequest<? extends IDeliverable> completedRequest)
    {
        //Noop. The production resolver already took care of that.
        return null;
    }

    @Override
    public void onAssignedRequestBeingCancelled(@NotNull final IRequestManager manager, @NotNull final IRequest<? extends IDeliverable> request)
    {
    }

    @Override
    public void onAssignedRequestCancelled(@NotNull final IRequestManager manager, @NotNull final IRequest<? extends IDeliverable> request)
    {

    }

    @Override
    public void onRequestedRequestComplete(@NotNull final IRequestManager manager, @NotNull final IRequest<?> request)
    {
        /*
         * Nothing to be done.
         */
    }

    @Override
    public void onRequestedRequestCancelled(@NotNull final IRequestManager manager, @NotNull final IRequest<?> request)
    {
        //NOOP
    }

    @NotNull
    @Override
    public IFormattableTextComponent getRequesterDisplayName(@NotNull final IRequestManager manager, @NotNull final IRequest<?> request)
    {
        final IRequester requester = manager.getColony().getRequesterBuildingForPosition(getLocation().getInDimensionLocation());
        if (requester instanceof IBuildingWorkerView)
        {
            final IBuildingWorkerView bwv = (IBuildingWorkerView) requester;
            return new TranslationTextComponent(bwv.getJobDisplayName().toLowerCase());
        }
        return super.getRequesterDisplayName(manager, request);
    }

    @Override
    public int getPriority()
    {
        return CONST_CRAFTING_RESOLVER_PRIORITY;
    }

    @Override
    public boolean canBuildingCraftStack(@NotNull final AbstractBuildingWorker building, final Predicate<ItemStack> stackPredicate)
    {
        return building.getFirstRecipe(stackPredicate) != null;
    }

    @Override
    protected IRequestable createNewRequestableForStack(final ItemStack stack, final int count, final int minCount)
    {
        return new PublicCrafting(stack, count);
    }
}
