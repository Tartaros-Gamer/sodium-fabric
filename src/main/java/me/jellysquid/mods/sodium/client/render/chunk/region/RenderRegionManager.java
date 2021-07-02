package me.jellysquid.mods.sodium.client.render.chunk.region;

import it.unimi.dsi.fastutil.longs.Long2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.*;
import me.jellysquid.mods.sodium.client.gl.arena.GlBufferSegment;
import me.jellysquid.mods.sodium.client.gl.buffer.IndexedVertexData;
import me.jellysquid.mods.sodium.client.gl.device.CommandList;
import me.jellysquid.mods.sodium.client.gl.device.RenderDevice;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkGraphicsState;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkRenderer;
import me.jellysquid.mods.sodium.client.render.chunk.RenderSection;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBuildResult;
import me.jellysquid.mods.sodium.client.render.chunk.data.ChunkMeshData;
import me.jellysquid.mods.sodium.client.render.chunk.passes.BlockRenderPass;
import me.jellysquid.mods.sodium.client.util.math.FrustumExtended;

import java.util.*;

public class RenderRegionManager {
    private final Long2ReferenceOpenHashMap<RenderRegion> regions = new Long2ReferenceOpenHashMap<>();

    private final ChunkRenderer renderer;

    public RenderRegionManager(ChunkRenderer renderer) {
        this.renderer = renderer;
    }

    public void update(FrustumExtended frustum) {
        Iterator<RenderRegion> it = this.regions.values()
                .iterator();

        while (it.hasNext()) {
            RenderRegion region = it.next();

            if (region.isEmpty()) {
                region.deleteResources();

                it.remove();
            } else {
                region.updateVisibility(frustum);
            }
        }
    }

    public void upload(CommandList commandList, Iterator<ChunkBuildResult> queue) {
        for (Map.Entry<RenderRegion, List<ChunkBuildResult>> entry : this.setupUploadBatches(queue).entrySet()) {
            RenderRegion region = entry.getKey();
            List<ChunkBuildResult> uploadQueue = entry.getValue();

            for (BlockRenderPass pass : BlockRenderPass.VALUES) {
                this.upload(commandList, region, pass, uploadQueue);
            }

            for (ChunkBuildResult result : uploadQueue) {
                result.render.setData(result.data);
                result.render.setLastBuiltTime(result.buildTime);

                result.delete();
            }
        }
    }

    private void upload(CommandList commandList, RenderRegion region, BlockRenderPass pass, List<ChunkBuildResult> results) {
        int vertexBytes = 0;
        int indexBytes = 0;

        for (ChunkBuildResult result : results) {
            ChunkMeshData meshData = result.getMesh(pass);

            if (meshData != null) {
                IndexedVertexData vertexData = meshData.getVertexData();

                vertexBytes += vertexData.vertexBuffer.size();
                indexBytes += vertexData.indexBuffer.size();
            }

            ChunkGraphicsState graphics = result.render.setGraphicsState(pass, null);

            // De-allocate the existing buffer arena for this render
            // This will allow it to be cheaply re-allocated just below
            if (graphics != null) {
                graphics.delete();
            }
        }

        RenderRegion.RenderRegionArenas arenas = region.getArenas(pass);

        if (arenas == null) {
            if (vertexBytes + indexBytes == 0) {
                return;
            }

            arenas = region.createArenas(commandList, pass);
        }

        int vertexStride = this.renderer.getVertexType().getBufferVertexFormat().getStride();

        arenas.vertexBuffers.checkArenaCapacity(commandList, vertexBytes / vertexStride);
        arenas.indexBuffers.checkArenaCapacity(commandList, indexBytes / 4);

        for (ChunkBuildResult result : results) {
            ChunkMeshData meshData = result.getMesh(pass);

            if (meshData != null) {
                IndexedVertexData upload = meshData.getVertexData();

                GlBufferSegment vertexSegment = arenas.vertexBuffers.uploadBuffer(commandList, upload.vertexBuffer.getUnsafeBuffer());
                GlBufferSegment indexSegment = arenas.indexBuffers.uploadBuffer(commandList, upload.indexBuffer.getUnsafeBuffer());

                result.render.setGraphicsState(pass, new ChunkGraphicsState(vertexSegment, indexSegment, meshData));
            }
        }

        if (arenas.getTessellation() != null) {
            commandList.deleteTessellation(arenas.getTessellation());

            arenas.setTessellation(null);
        }

        if (arenas.isEmpty()) {
            region.deleteArenas(commandList, pass);
        }
    }

    private Map<RenderRegion, List<ChunkBuildResult>> setupUploadBatches(Iterator<ChunkBuildResult> renders) {
        Map<RenderRegion, List<ChunkBuildResult>> map = new Reference2ObjectLinkedOpenHashMap<>();

        while (renders.hasNext()) {
            ChunkBuildResult result = renders.next();
            RenderSection render = result.render;

            if (!render.canAcceptBuildResults(result)) {
                result.delete();

                continue;
            }

            RenderRegion region = this.regions.get(RenderRegion.getRegionKeyForChunk(render.getChunkX(), render.getChunkY(), render.getChunkZ()));

            if (region == null) {
                throw new NullPointerException("Couldn't find region for chunk: " + render);
            }

            List<ChunkBuildResult> uploadQueue = map.computeIfAbsent(region, k -> new ArrayList<>());
            uploadQueue.add(result);
        }

        return map;
    }


    public RenderRegion createRegionForChunk(int x, int y, int z) {
        long key = RenderRegion.getRegionKeyForChunk(x, y, z);
        RenderRegion region = this.regions.get(key);

        if (region == null) {
            this.regions.put(key, region = RenderRegion.createRegionForChunk(this.renderer, RenderDevice.INSTANCE, x, y, z));
        }

        return region;
    }

    public void delete() {
        for (RenderRegion region : this.regions.values()) {
            region.deleteResources();
        }

        this.regions.clear();
    }

    public Collection<RenderRegion> getLoadedRegions() {
        return this.regions.values();
    }
}
