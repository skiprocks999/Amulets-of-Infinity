/* Skip999
 * 10/29/20
 * Purpose: Contains inventory functionality for AlloyForge
 */

package com.contInf.BlockLib.container;

import com.contInf.BlockLib.init.BlockInit;
import com.contInf.BlockLib.init.ContInfContainerTypes;
import com.contInf.BlockLib.tileentity.AlloyForgeTileEntity;
import com.contInf.BlockLib.util.FunctionalIntReferenceHolder;

import java.util.Objects;

import javax.annotation.Nonnull;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IWorldPosCallable;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.items.SlotItemHandler;

public class AlloyForgeContainer extends Container {

	/* Fields */
	private AlloyForgeTileEntity tileEntity;
	private IWorldPosCallable canInteractWithCallable;
	public FunctionalIntReferenceHolder currentSmeltTime;
	public FunctionalIntReferenceHolder currentBurnTime;
	
	/* Constructors */
	
	//Server Constructor
	public AlloyForgeContainer(final int windowId, final PlayerInventory playerInv, 
			final AlloyForgeTileEntity tile) {
		super(ContInfContainerTypes.ALLOY_FORGE.get(),windowId);
		
		this.tileEntity = tile;
		this.canInteractWithCallable = IWorldPosCallable.of(tile.getWorld(), tile.getPos());
		
		//Hotbar
		int hotBarY = 142;
		int startX = 8;
		final int slotSizePlus2 = 16 + 2;
		
		for(int collumn = 0; collumn < 9; collumn ++) {
			this.addSlot(new Slot(playerInv,collumn, startX + (collumn * slotSizePlus2), hotBarY));
		}
		
		//Main Inventory
		int mainY = 84;
		
		for(int row = 0; row < 3; row++) {
			for(int collumn = 0; collumn < 9; collumn ++) {
				this.addSlot(new Slot(playerInv, 9 + (row*9) + collumn, 
						startX + (collumn * slotSizePlus2), mainY + (row * slotSizePlus2)));
			}
		}
		
		//Furnace Slots
		
		//Input1
		this.addSlot(new SlotItemHandler(tile.getInventory(), 0, 46, 16));
		//Input2
		this.addSlot(new SlotItemHandler(tile.getInventory(), 1, 46, 54));
		//Output
		this.addSlot(new SlotItemHandler(tile.getInventory(), 2, 116, 35));
		//Fuel
		this.addSlot(new SlotItemHandler(tile.getInventory(), 3, 8, 54));
		
		
		//Tracks current smelt time
		this.trackInt(currentSmeltTime = new FunctionalIntReferenceHolder(() -> this.tileEntity.currentSmeltTime,
				value -> this.tileEntity.currentSmeltTime = value));
		
		//Tracks current burn time
		this.trackInt(currentBurnTime = new FunctionalIntReferenceHolder(() -> this.tileEntity.currentBurnTime,
				value -> this.tileEntity.currentBurnTime = value));
	}
	
	//Client Constructor
	public AlloyForgeContainer(final int windowId, final PlayerInventory playerInv, 
			final PacketBuffer data) {
		this(windowId, playerInv, getTileEntity(playerInv,data));
	}

	
	/* Functional Methods */
	
	/* Obtains an instance of the AlloyForgeTileEntity*/
	private static AlloyForgeTileEntity getTileEntity(
			final PlayerInventory playerInv, final PacketBuffer data) {
		Objects.requireNonNull(playerInv, "playerInv cannot be null");
		Objects.requireNonNull(data, "data cannot be null");
		final TileEntity tileAtPos = playerInv.player.world.getTileEntity(data.readBlockPos());
		if (tileAtPos instanceof AlloyForgeTileEntity) {
			return (AlloyForgeTileEntity) tileAtPos;
		}
		throw new IllegalStateException("TileEntity is not correct " + tileAtPos);
	}

	
	@Override
	public boolean canInteractWith(PlayerEntity playerIn) {
		return isWithinUsableDistance(canInteractWithCallable, playerIn, BlockInit.alloy_forge.get());
	}
	
	
	/* Transfers in items for inventory slots*/
	@Nonnull
	@Override
	public ItemStack transferStackInSlot(final PlayerEntity player, final int index) {
		ItemStack returnStack = ItemStack.EMPTY;
		final Slot slot = this.inventorySlots.get(index);
		if (slot != null && slot.getHasStack()) {
			final ItemStack slotStack = slot.getStack();
			returnStack = slotStack.copy();

			final int containerSlots = this.inventorySlots.size() - player.inventory.mainInventory.size();
			if (index < containerSlots) {
				if (!mergeItemStack(slotStack, containerSlots, this.inventorySlots.size(), true)) {
					return ItemStack.EMPTY;
				}
			} else if (!mergeItemStack(slotStack, 0, containerSlots, false)) {
				return ItemStack.EMPTY;
			}
			if (slotStack.getCount() == 0) {
				slot.putStack(ItemStack.EMPTY);
			} else {
				slot.onSlotChanged();
			}
			if (slotStack.getCount() == returnStack.getCount()) {
				return ItemStack.EMPTY;
			}
			slot.onTake(player, slotStack);
		}
		return returnStack;
	}
	
	
	/* Scales smelt time int for progress animation*/
	@OnlyIn(Dist.CLIENT)
	public int getSmeltProgressionScaled() {
		return this.currentSmeltTime.get() != 0 && this.tileEntity.maxSmeltTime != 0
					? this.currentSmeltTime.get() * 36 / this.tileEntity.maxSmeltTime : 0;
				
	}
	
	
	/* Scales smelt time int for progress animation*/
	@OnlyIn(Dist.CLIENT)
	public int getBurnProgressionScaled() {
		return this.currentBurnTime.get() != 0 && this.tileEntity.currentBurnTime != 0
					? 14 - (int)(this.currentBurnTime.get()/this.tileEntity.itemBurnTime) : 14;
				
	}

}
