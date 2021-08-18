package net.axay.fabrik.persistence.mixin.chunk;

import net.axay.fabrik.persistence.CompoundProvider;
import net.axay.fabrik.persistence.PersistentCompoundKt;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructureManager;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.ChunkSerializer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ProtoChunk;
import net.minecraft.world.poi.PointOfInterestStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChunkSerializer.class)
public class ChunkSerializerMixin {
    @Inject(method = "serialize", at = @At("RETURN"))
    private static void onSerialize(ServerWorld world,
                                    Chunk chunk,
                                    CallbackInfoReturnable<NbtCompound> cir) {
        PersistentCompoundKt.putPersistentData(cir.getReturnValue().getCompound("Level"), ((CompoundProvider) chunk).getCompound());
    }

    @Inject(method = "deserialize", at = @At("RETURN"))
    private static void onDeserialize(ServerWorld world,
                                      StructureManager structureManager,
                                      PointOfInterestStorage poiStorage,
                                      ChunkPos pos,
                                      NbtCompound nbt,
                                      CallbackInfoReturnable<ProtoChunk> cir) {
        ((CompoundProvider) cir.getReturnValue()).setCompound(PersistentCompoundKt.getPersistentData(nbt.getCompound("Level")));
    }
}