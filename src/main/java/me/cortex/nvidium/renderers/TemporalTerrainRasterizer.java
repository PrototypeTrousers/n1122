package me.cortex.nvidium.renderers;

import me.cortex.nvidium.gl.shader.Shader;
import me.cortex.nvidium.sodiumCompat.ShaderLoader;
import me.cortex.nvidium.mixin.minecraft.LightMapAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util. ResourceLocation;
import org.lwjgl3.opengl.GL45;
import org.lwjgl3.opengl.GL45C;

import static me.cortex.nvidium.RenderPipeline.GL_DRAW_INDIRECT_ADDRESS_NV;
import static me.cortex.nvidium.gl.shader.ShaderType.*;
import static org.lwjgl3.opengl.GL11C.GL_NEAREST;
import static org.lwjgl3.opengl.GL11C.GL_NEAREST_MIPMAP_LINEAR;
import static org.lwjgl3.opengl.GL33.glGenSamplers;
import static org.lwjgl3.opengl.NVMeshShader.glMultiDrawMeshTasksIndirectNV;
import static org.lwjgl3.opengl.NVVertexBufferUnifiedMemory.glBufferAddressRangeNV;

public class TemporalTerrainRasterizer extends Phase {
    private final int blockSampler = glGenSamplers();
    private final int lightSampler = glGenSamplers();
    private final Shader shader = Shader.make()
            .addSource(TASK, ShaderLoader.parse(new  ResourceLocation("nvidium", "terrain/temporal_task.glsl")))
            .addSource(MESH, ShaderLoader.parse(new  ResourceLocation("nvidium", "terrain/mesh.glsl")))
            .addSource(FRAGMENT, ShaderLoader.parse(new  ResourceLocation("nvidium", "terrain/frag.frag"))).compile();

    public TemporalTerrainRasterizer() {
        GL45C.glSamplerParameteri(blockSampler, GL45C.GL_TEXTURE_MIN_FILTER, GL_NEAREST_MIPMAP_LINEAR);
        GL45C.glSamplerParameteri(blockSampler, GL45C.GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        GL45C.glSamplerParameteri(blockSampler, GL45C.GL_TEXTURE_MIN_LOD, 0);
        GL45C.glSamplerParameteri(blockSampler, GL45C.GL_TEXTURE_MAX_LOD, 4);
    }

    public void raster(int regionCount, long commandAddr) {
        shader.bind();

        int blockId = Minecraft.getInstance().getTextureManager().getTexture(new  ResourceLocation("minecraft", "textures/atlas/blocks.png")).getGlId();
        int lightId = ((LightMapAccessor)Minecraft.getInstance().gameRenderer.getLightmapTextureManager()).getTexture().getGlId();

        GL45C.glBindTextureUnit(0, blockId);
        GL45C.glBindSampler(0, blockSampler);

        GL45C.glBindTextureUnit(1, lightId);
        GL45C.glBindSampler(1, lightSampler);



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
