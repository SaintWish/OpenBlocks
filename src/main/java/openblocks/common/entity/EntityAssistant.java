package openblocks.common.entity;

import io.netty.buffer.ByteBuf;
import java.lang.ref.WeakReference;
import java.util.UUID;
import javax.annotation.Nonnull;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumHand;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fml.common.registry.IEntityAdditionalSpawnData;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import openmods.utils.NbtUtils;

public abstract class EntityAssistant extends EntitySmoothMove implements IEntityAdditionalSpawnData {

	private static final String OWNER_ID_TAG = "OwnerId";
	private UUID ownerId;
	private WeakReference<EntityPlayer> cachedOwner = new WeakReference<>(null);
	protected double ownerOffsetX;
	protected double ownerOffsetY;
	protected double ownerOffsetZ;

	public EntityAssistant(World world, EntityPlayer owner) {
		super(world);
		this.cachedOwner = new WeakReference<>(owner);

		if (owner != null) this.ownerId = owner.getGameProfile().getId();
	}

	public EntityPlayer findOwner() {
		EntityPlayer owner = cachedOwner.get();
		if (owner == null && ownerId != null) {
			owner = world.getPlayerEntityByUUID(ownerId);
			if (owner != null) cachedOwner = new WeakReference<>(owner);
		}

		return owner;
	}

	@Override
	protected void readEntityFromNBT(NBTTagCompound tag) {
		if (tag.hasKey(OWNER_ID_TAG, Constants.NBT.TAG_COMPOUND)) {
			NBTTagCompound ownerTag = tag.getCompoundTag(OWNER_ID_TAG);
			ownerId = NbtUtils.readUuid(ownerTag);
		} else {
			ownerId = null;
		}
	}

	@Override
	protected void writeEntityToNBT(NBTTagCompound tag) {
		if (ownerId != null) tag.setTag(OWNER_ID_TAG, NbtUtils.store(ownerId));
	}

	@Override
	public void onUpdate() {
		if (!world.isRemote) {
			EntityPlayer owner = findOwner();

			if (owner != null) smoother.setTarget(
					owner.posX + ownerOffsetX,
					owner.posY + owner.getEyeHeight() + ownerOffsetY,
					owner.posZ + ownerOffsetZ);

		}

		updatePrevPosition();
		smoother.update();
	}

	@Override
	protected void dealFireDamage(int par1) {}

	@Override
	public boolean attackEntityFrom(DamageSource par1DamageSource, float par2) {
		if (!isDead && !world.isRemote) entityDropItem(toItemStack(), 0.5f);
		setDead();
		return true;
	}

	@Nonnull
	public abstract ItemStack toItemStack();

	@Override
	public boolean processInitialInteract(EntityPlayer player, EnumHand hand) {
		if (player instanceof EntityPlayerMP && player.isSneaking() && getDistance(player) < 3) { return true; }
		return false;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public boolean canRenderOnFire() {
		return false;
	}

	@Override
	public boolean canBeCollidedWith() {
		return true;
	}

	@Override
	public void writeSpawnData(ByteBuf data) {
		if (ownerId != null) {
			data.writeBoolean(true);
			new PacketBuffer(data).writeUniqueId(ownerId);
		} else data.writeBoolean(false);
	}

	@Override
	public void readSpawnData(ByteBuf data) {
		if (data.readBoolean()) ownerId = new PacketBuffer(data).readUniqueId();
		else ownerId = null;
	}

	public void setSpawnPosition(Entity owner) {
		setPosition(owner.posX + 1, owner.posY + owner.getEyeHeight(), owner.posZ);
	}

}
