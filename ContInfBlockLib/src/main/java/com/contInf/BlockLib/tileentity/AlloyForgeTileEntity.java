/* Skip999
 * 10/28/20
 * purpose: contain functionality for AlloyForgeTileEntity
 */

package com.contInf.BlockLib.tileentity;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.contInf.BlockLib.ContInfBlockLib;
import com.contInf.BlockLib.container.AlloyForgeContainer;
import com.contInf.BlockLib.init.BlockTileEntityTypes;
import com.contInf.BlockLib.init.RecipeSerializerInit;
import com.contInf.BlockLib.objects.blocks.AlloyForge;
import com.contInf.BlockLib.recipes.AlloyForgeRecipe;
import com.contInf.BlockLib.util.AlloyForgeItemHandler;

import net.minecraft.client.Minecraft;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.ItemStackHelper;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.IRecipeType;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SUpdateTileEntityPacket;
import net.minecraft.tileentity.AbstractFurnaceTileEntity;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.Direction;
import net.minecraft.util.NonNullList;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.wrapper.RecipeWrapper;

public class AlloyForgeTileEntity extends TileEntity 
						implements ITickableTileEntity, INamedContainerProvider{

	
	/* Fields */
	
	
	private ITextComponent customName;
	public int currentSmeltTime;
	public final int maxSmeltTime = 199;
	private AlloyForgeItemHandler inventory; 
	public int currentBurnTime;
	public static int itemBurnTime = 1;
	private static final Logger logger = LogManager.getLogger(ContInfBlockLib.modID);
	
	/* Constructors */
	
	
	//Creates a new instance of an AlloyForge entity
	public AlloyForgeTileEntity(TileEntityType<?> tileEntityTypeIn) {
		super(tileEntityTypeIn);
		
		this.inventory = new AlloyForgeItemHandler(4);
	}

	
	//Creates instance for existing AlloyForge entity
	public AlloyForgeTileEntity() {
		this(BlockTileEntityTypes.ALLOY_FORGE.get());
	}

	
	/* Functional Methods */
	
	
	//Creates a GUI for the AlloyForge
	@Override
	public Container createMenu(
			final int windowID, final PlayerInventory playerInv, final PlayerEntity playerIn) {
		return new AlloyForgeContainer(windowID, playerInv, this);
	}

	
	/* WHAT THE BLOCK DOES*/
	@Override
	public void tick() {
		//Tells game to read and write from disk
		boolean dirty = false;
		
		if(this.world != null && !this.world.isRemote) {
			
			//Burn Time Functionality
			
			if(!isLit()) {
				ItemStack[] ingredients = new ItemStack[2];
				ingredients[0] = this.inventory.getStackInSlot(0);
				ingredients[1] = this.inventory.getStackInSlot(1);
				if(this.getRecipe(ingredients) != null) {
					//logger.debug(this.inventory.getStackInSlot(3));
					if(this.inventory.getStackInSlot(3) != ItemStack.EMPTY 
							&& this.inventory.getStackInSlot(2).getCount() < 64) {
						this.currentBurnTime = ForgeHooks.getBurnTime(this.inventory.getStackInSlot(3));
						if(isLit()) {
							itemBurnTime = ForgeHooks.getBurnTime(this.inventory.getStackInSlot(3));
							this.inventory.decrStackSize(3,1);
							//logger.debug("burn time set to " + itemBurnTime);
						}else {
							itemBurnTime = 1;
						}
					}
				}
			}
			
			//Recipe Functionality 
			
			if(isLit()) {
				ItemStack[] ingredients = new ItemStack[2];
				ingredients[0] = this.inventory.getStackInSlot(0);
				ingredients[1] = this.inventory.getStackInSlot(1);
				if(this.getRecipe(ingredients) != null){
					if(this.inventory.getStackInSlot(2).getCount() < 64) {
						if(this.currentBurnTime - 1 > 0) {
							if(this.currentSmeltTime != this.maxSmeltTime) {
															
								this.currentSmeltTime ++;
								this.world.setBlockState(this.getPos(), this.getBlockState().with(AlloyForge.LIT, true));
								if(this.currentBurnTime - 1 == 0) {
									this.currentSmeltTime = 0;
								}
								dirty = true;
								
							}else {
								
								this.currentSmeltTime = 0;
								ItemStack output = this.getRecipe(ingredients).getRecipeOutput();
								this.inventory.insertItem(2, output.copy(),false);
								this.inventory.decrStackSize(0,1);
								this.inventory.decrStackSize(1,1);
								
							}
						}
					}
				}
				//logger.debug("current burn time :" + this.currentBurnTime);
				
				if(this.currentBurnTime - 1 == 0) {
					this.world.setBlockState(this.getPos(), this.getBlockState().with(AlloyForge.LIT, false));
					dirty = true;
				}
				
				this.currentBurnTime--;
				
			}
		}
		
		if(dirty) {
			this.markDirty();
			this.world.notifyBlockUpdate(this.getPos(), this.getBlockState(), this.getBlockState(), Constants.BlockFlags.BLOCK_UPDATE);
		}
	}
	
	
	//Retrieves current name of block for GUI
	@Override
	public ITextComponent getDisplayName() {
		return this.getName();
	}
	
	
	//Sets custom name for block
	@Override
	public CompoundNBT write(CompoundNBT compound) {
		super.write(compound);
		if(this.customName != null) {
			compound.putString("CustomName", ITextComponent.Serializer.toJson(this.customName));
		}
		
		ItemStackHelper.saveAllItems(compound, this.inventory.toNonNullList());
		
		compound.putInt("CurrentSmeltTime", this.currentSmeltTime);
		
		return compound;
	}
	
	
	//Retrieves the current name
	@Override
	public void read(CompoundNBT compound) {
		super.read(compound);
		if(compound.contains("CustomName",Constants.NBT.TAG_STRING)) {
			this.customName = ITextComponent.Serializer.fromJson(compound.getString("CustomName"));
		}
		
		NonNullList<ItemStack> inv = NonNullList.withSize(this.inventory.getSlots(), ItemStack.EMPTY);
		ItemStackHelper.loadAllItems(compound, inv);
		this.inventory.setNonNullList(inv);
		
		this.currentSmeltTime = compound.getInt("CurrentSmeltTime");
	}
	
	
	/* CUSTOM NAME */
	
	public void setCustomName(ITextComponent name) {
		this.customName = name;
	}
	
	public ITextComponent getName() {
		return this.customName != null ? this.customName : this.getDefaultName();
	}
	
	private ITextComponent getDefaultName() {
		return new TranslationTextComponent("container" + ContInfBlockLib.modID + ".alloy_forge");
	}
	
	@Nullable
	public ITextComponent getCustomName() {
		return this.customName;
	}
	
	
	/* Looks for Recipe based on inputs */
	private AlloyForgeRecipe getRecipe(ItemStack[] stack) {
		
		if(stack == null) {
			return null;
		}
		
		Set<IRecipe<?>> recipes = findRecipesbyType(RecipeSerializerInit.ALLOY_FORGE_TYPE, this.world);
		for(IRecipe<?> IRecipes: recipes) {
			AlloyForgeRecipe recipe = (AlloyForgeRecipe) IRecipes;
			if(recipe.matches(new RecipeWrapper(this.inventory), this.world)) {
				return recipe;
			}
		}
		
		return null;
	}

	
	//Returns all recipes for AlloyForge
	public static Set<IRecipe<?>> findRecipesbyType(IRecipeType<?> typeIn, World world) {
		return world != null ? 
				world.getRecipeManager().getRecipes().
					stream().filter(recipe -> recipe.getType() == typeIn).collect(Collectors.toSet())
				:Collections.emptySet();
	}
	
	
	//Determines recipe for client-side
	@SuppressWarnings("resource")
	@OnlyIn(Dist.CLIENT)
	public static Set<IRecipe<?>> findRecipesbyType(IRecipeType<?> typeIn) {
		ClientWorld world = Minecraft.getInstance().world;
		return world != null ? 
				world.getRecipeManager().getRecipes().
					stream().filter(recipe -> recipe.getType() == typeIn).collect(Collectors.toSet())
				:Collections.emptySet();
	}
	
	
	//Determines inputs for particular recipe
	public static Set<ItemStack> getAllRecipeInputs(IRecipeType<?> typeIn, World world){
		Set<ItemStack> inputs = new HashSet<ItemStack>();
		Set<IRecipe<?>> recipes = findRecipesbyType(typeIn, world);
		
		for(IRecipe<?> recipe:recipes) {
			NonNullList<Ingredient> ingredients = recipe.getIngredients();
			
			ingredients.forEach(ingredient -> {
				for(ItemStack stack: ingredient.getMatchingStacks()) {
					inputs.add(stack);
				}
				
			}
			);
		}
		return inputs;
	}
	
	
	//Retireves Inventory for GUI
	public final IItemHandlerModifiable getInventory() {
		return this.inventory;
	}
	
	
	/* Tick Synchronization */
	
	
	@Nullable
	@Override
	public SUpdateTileEntityPacket getUpdatePacket() {
		CompoundNBT nbt = new CompoundNBT();
		this.write(nbt);
		return new SUpdateTileEntityPacket(this.pos, 0, nbt);
	}

	@Override
	public void onDataPacket(NetworkManager net, SUpdateTileEntityPacket pkt) {
		this.read(pkt.getNbtCompound());
	}

	@Override
	public CompoundNBT getUpdateTag() {
		CompoundNBT nbt = new CompoundNBT();
		this.write(nbt);
		return nbt;
	}

	@Override
	public void handleUpdateTag(CompoundNBT nbt) {
		this.read(nbt);
	}
	
	@Override
	public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
		return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.orEmpty(cap, LazyOptional.of(() -> this.inventory));
	}
	
	
	private boolean isLit() {
		return currentBurnTime > 0;
	}
	
	
	
	
	
	
	
}
