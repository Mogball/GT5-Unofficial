package gregtech.common.render;

import cpw.mods.fml.relauncher.SideOnly;
import gregtech.api.enums.ItemList;
import gregtech.common.items.GT_FluidDisplayItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraftforge.client.IItemRenderer;
import net.minecraftforge.client.MinecraftForgeClient;
import org.lwjgl.opengl.GL11;

@SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
public class GT_FluidDisplayStackRenderer implements IItemRenderer {
    private static final float smallTextScale = 0.5f;

    public GT_FluidDisplayStackRenderer() {
        MinecraftForgeClient.registerItemRenderer(ItemList.Display_Fluid.getItem(), this);
    }

    @Override
    public boolean handleRenderType (ItemStack item, ItemRenderType type)
    {
        if(!item.hasTagCompound())
            return false;
        return type == ItemRenderType.INVENTORY;
    }

    @Override
    public boolean shouldUseRenderHelper (ItemRenderType type, ItemStack item, ItemRendererHelper helper)
    {
        //not sure what this does.
        return false;
    }

    @Override
    public void renderItem (ItemRenderType type, ItemStack item, Object... data) {
        if (item == null || item.getItem() == null || !(item.getItem() instanceof GT_FluidDisplayItem))
            return;

        Tessellator tess = Tessellator.instance;
        GL11.glPushMatrix();
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glEnable(GL11.GL_BLEND);
        OpenGlHelper.glBlendFunc(770, 771, 1, 0);

        int l = item.getItem().getColorFromItemStack(item, 0);
        float f3 = (float)(l >> 16 & 255) / 255.0F;
        float f4 = (float)(l >> 8 & 255) / 255.0F;
        float f = (float)(l & 255) / 255.0F;
        GL11.glColor4f(f3, f4, f, 1.0F);

        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glEnable(GL11.GL_ALPHA_TEST);

        IIcon icon = item.getItem().getIconFromDamage(item.getItemDamage());
        tess.startDrawingQuads();

        // draw a simple rectangle for the inventory icon
        final float x_min = icon.getMinU();
        final float x_max = icon.getMaxU();
        final float y_min = icon.getMinV();
        final float y_max = icon.getMaxV();
        tess.addVertexWithUV( 0, 16, 0, x_min, y_max);
        tess.addVertexWithUV(16, 16, 0, x_max, y_max);
        tess.addVertexWithUV(16,  0, 0, x_max, y_min);
        tess.addVertexWithUV( 0,  0, 0, x_min, y_min);
        tess.draw();

        GL11.glEnable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_ALPHA_TEST);
        GL11.glDisable(GL11.GL_BLEND);

        GL11.glPopMatrix();

        if(item.getTagCompound() == null)
            return;

        // Render Fluid amount text
        long fluidAmount = item.getTagCompound().getLong("mFluidDisplayAmount");
        if (fluidAmount > 0L) {
            String amountString;

            if (fluidAmount < 10000) {
                amountString = "" + fluidAmount + "L";
            } else {
                int exp = (int) (Math.log(fluidAmount) / Math.log(1000));
                double shortAmount = fluidAmount / Math.pow(1000, exp);
                if ( shortAmount >= 100) {
                    amountString = String.format("%.0f%cL", shortAmount, "kMGT".charAt(exp - 1));
                } else if ( shortAmount >= 10) {
                    amountString = String.format("%.1f%cL", shortAmount, "kMGT".charAt(exp - 1));
                } else {
                    amountString = String.format("%.2f%cL", shortAmount, "kMGT".charAt(exp - 1));
                }
            }

            FontRenderer fontRender = Minecraft.getMinecraft().fontRenderer;
            GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            GL11.glDisable(GL11.GL_BLEND);
            GL11.glScalef(smallTextScale, smallTextScale, smallTextScale); //TODO: how to make this pretty at all scales?
            fontRender.drawString( amountString, 0, 16*2 - fontRender.FONT_HEIGHT + 1, 0xFFFFFF, true);
            GL11.glEnable(GL11.GL_LIGHTING);
            GL11.glEnable(GL11.GL_DEPTH_TEST);
        }
    }
}
