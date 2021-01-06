package com.contInf.ringsOfInfinity.gui;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.contInf.ringsOfInfinity.RingsOfInfinity;
import com.contInf.ringsOfInfinity.container.HellfireFurnaceContainer;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;

public class HellfireFurnaceScreen extends ContainerScreen<HellfireFurnaceContainer>{
	/* Fields */
	private static final ResourceLocation TEXTURE = new ResourceLocation(RingsOfInfinity.modID,
			"textures/gui/hellfire_furnace.png");
	
	private static final Logger logger = LogManager.getLogger(RingsOfInfinity.modID);
	
	/* Constructors */

	public HellfireFurnaceScreen(HellfireFurnaceContainer screenContainer, PlayerInventory inv,
			ITextComponent titleIn) {
		
		super(screenContainer, inv, titleIn);

		this.guiLeft = 0;
		this.guiTop = 0;
		this.xSize = 176;
		this.ySize = 166;
	}

	
	/* Functional Methods */
	
	/*Draws gui image*/
	@Override
	protected void drawGuiContainerBackgroundLayer(MatrixStack matrixStack,float partialTicks, int mouseX, int mouseY) {
		RenderSystem.color4f(1.0f, 1.0f, 1.0f, 1.0f);
		this.minecraft.getTextureManager().bindTexture(TEXTURE);
		HellfireFurnaceScreen.blit(matrixStack,this.guiLeft, this.guiTop, 0, 0, this.xSize, this.ySize,256,256);
		
		//blit(int x, int y, int textureX,
		//					int textureY, int width, int height, int textureWidth, int textureHeight);
		
		

		//Burn Time
		double burnTimeFraction = this.container.getBurnProgressionScaled();
		//logger.debug("burn time fraciton: " + burnTimeFraction);
		int yOffset = (int)(burnTimeFraction * 16);
		//logger.debug("current y offset: " + yOffset);
		
		this.blit(matrixStack,this.guiLeft + 55, this.guiTop + 36 + yOffset, 176, yOffset, 16, (16 - yOffset));

		
		//Progress Bar
		this.blit(matrixStack,this.guiLeft + 76, this.guiTop + 33, 176, 16, 
				this.container.getSmeltProgressionScaled(), 18);
		
	}

	/*Draws mouse and gui text*/
	@Override
	protected void drawGuiContainerForegroundLayer(MatrixStack matrixStack, int mouseX, int mouseY) {
		super.drawGuiContainerForegroundLayer(matrixStack,mouseX, mouseY);
		this.font.drawString(matrixStack, this.title.getString(), 8.0f, 5.0f, 0x404040);
		this.font.drawString(matrixStack, this.playerInventory.getDisplayName().getString(), 8.0f, 73.0f, 0x404040);
	}

	
	/*Renders gui*/
	@Override
	public void render(MatrixStack matrixStack,int mouseX, int mouseY, float partialTicks) {
		this.renderBackground(matrixStack);
		super.render(matrixStack, mouseX, mouseY, partialTicks);
		this.renderHoveredTooltip(matrixStack,mouseX, mouseY);
	}
}
