package com.minecolonies.entity.ai.citizen.guard;

import com.blockout.Log;
import com.minecolonies.colony.Colony;
import com.minecolonies.colony.ColonyManager;
import com.minecolonies.entity.EntityCitizen;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.projectile.EntityTippedArrow;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

/**
 * Arrow class for arrows shot by guards.
 */
public class GuardArrow extends EntityTippedArrow
{
    private static final String TAG_COLONY = "colony";
    private Colony colony;

    /**
     * Constructor for forge.
     *
     * @param worldin the world this is in.
     */
    public GuardArrow(final World worldin)
    {
        super(worldin);
    }

    /**
     * Create a new Arrow.
     *
     * @param worldIn the world this is shot in.
     * @param shooter the guard shooting
     */
    public GuardArrow(final World worldIn, final EntityCitizen shooter)
    {
        super(worldIn, shooter);
        this.colony = shooter.getColony();
    }

    @Override
    public void writeEntityToNBT(final NBTTagCompound compound)
    {
        super.writeEntityToNBT(compound);
        compound.setInteger(TAG_COLONY, colony.getID());
    }

    @Override
    public void readEntityFromNBT(final NBTTagCompound compound)
    {
        super.readEntityFromNBT(compound);
        int colonyID = compound.getInteger(TAG_COLONY);
        colony = ColonyManager.getColony(colonyID);
    }

    @Override
    protected void arrowHit(final EntityLivingBase targetEntity)
    {
        super.arrowHit(targetEntity);
        Log.getLogger().info("Arrow hit " + targetEntity + " with " + targetEntity.getHealth());
        if (targetEntity.getHealth() <= 0.0F)
        {
            colony.incrementMobsKilled();
        }
    }
}
