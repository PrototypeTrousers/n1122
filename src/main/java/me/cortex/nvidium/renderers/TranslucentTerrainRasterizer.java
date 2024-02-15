package me.cortex.nvidium.renderers;

import me.cortex.nvidium.gl.shader.Shader;
import me.cortex.nvidium.sodiumCompat.ShaderLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.util.ResourceLocation;
import org.lwjgl3.opengl.GL45;
import org.lwjgl3.opengl.GL45C;

import static me.cortex.nvidium.RenderPipeline.GL_DRAW_INDIRECT_ADDRESS_NV;
import static me.cortex.nvidium.gl.shader.ShaderType.*;
import static org.lwjgl3.opengl.GL11C.GL_NEAREST;
import static org.lwjgl3.opengl.GL11C.GL_NEAREST_MIPMAP_LINEAR;
import static org.lwjgl3.opengl.GL33.glGenSamplers;
import static org.lwjgl3.opengl.NVMeshShader.glMultiDrawMeshTasksIndirectNV;
import static org.lwjgl3.opengl.NVVertexBufferUnifiedMemory.glBufferAddressRangeNV;

public class TranslucentTerrainRasterizer extends Phase {
    private final int blockSampler = glGenSamplers();
    private final int lightSampler = glGenSamplers();

    private final Shader shader = Shader.make()
            .addSource(TASK, ShaderLoader.parse(new  ResourceLocation("nvidium", "terrain/translucent/task.glsl")))
            .addSource(MESH, ShaderLoader.parse(new  ResourceLocation("nvidium", "terrain/translucent/mesh.glsl")))
            .addSource(FRAGMENT, ShaderLoader.parse(new  ResourceLocation("nvidium", "terrain/translucent/frag.frag")))
            .compile();

    public TranslucentTerrainRasterizer() {
        GL45C.glSamplerParameteri(blockSampler, GL45C.GL_TEXTURE_MIN_FILTER, GL_NEAREST_MIPMAP_LINEAR);
        GL45C.glSamplerParameteri(blockSampler, GL45C.GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        GL45C.glSamplerParameteri(blockSampler, GL45C.GL_TEXTURE_MIN_LOD, 0);
        GL45C.glSamplerParameteri(blockSampler, GL45C.GL_TEXTURE_MAX_LOD, 4);
    }


    private static void setTexture(int textureId, int bindingPoint) {
        GlStateManager.setActiveTexture(33984 + bindingPoint);
        GlStateManager.bindTexture(textureId);
    }

    //Translucency is rendered in a very cursed and incorrect way
    // it hijacks the unassigned indirect command dispatch and uses that to dispatch the translucent chunks as well
    public void raster(int regionCount, long commandAddr) {
        shader.bind();

        int blockId = Minecraft.getMinecraft().getTextureManager().getTexture(TextureMap.LOCATION_BLOCKS_TEXTURE).getGlTextureId();
        int lightId = OpenGlHelper.lightmapTexUnit;

        //GL45C.glBindTextureUnit(0, blockId);
        //GL45C.glBindSampler(0, blockSampler);

        //GL45C.glBindTextureUnit(1, lightId);
        //GL45C.glBindSampler(1, lightSampler);
        setTexture(blockId, 0);
        setTexture(lightId, 1);

        //the +8*6 is to offset to the unassigned dispatch
        glBufferAddressRangeNV(GL_DRAW_INDIRECT_ADDRESS_NV, 0, commandAddr, regionCount*8L);//Bind the command buffer
        glMultiDrawMeshTasksIndirectNV( 0, regionCount, 0);
        GL45C.glBindSampler(0, 0);
        GL45C.glBindSampler(1, 0);
    }

    public void delete() {
        GL45.glDeleteSamplers(blockSampler);
        GL45.glDeleteSamplers(lightSampler);
        shader.delete();
    }
}
